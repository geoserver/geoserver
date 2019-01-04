/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.Response;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Given a {@link DataAccess} subclass makes sure no write operations can be performed through it.
 * Regardless of the policy the data access is kept read only as services are supposed to perform
 * writes via {@link FeatureStore} instances returned by {@link FeatureTypeInfo} and not via direct
 * calls to data access.
 *
 * @author Andrea Aime - TOPP
 * @param <T>
 * @param <F>
 */
public class ReadOnlyDataAccess<T extends FeatureType, F extends Feature>
        extends DecoratingDataAccess<T, F> {

    static final String READ_ONLY = "This data access is read only";
    private WrapperPolicy policy;

    ReadOnlyDataAccess(DataAccess<T, F> delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public FeatureSource<T, F> getFeatureSource(Name typeName) throws IOException {
        final FeatureSource<T, F> fs = super.getFeatureSource(typeName);
        if (fs == null) return null;

        return (FeatureSource) SecuredObjects.secure(fs, policy);
    }

    @Override
    public void createSchema(T featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void updateSchema(Name typeName, T featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw notifyUnsupportedOperation();
    }
    /**
     * Notifies the caller the requested operation is not supported, using a plain {@link
     * UnsupportedOperationException} in case we have to conceal the fact the data is actually
     * writable, using an Spring Security security exception otherwise to force an authentication
     * from the user
     */
    RuntimeException notifyUnsupportedOperation() {
        if (policy.response == Response.CHALLENGE) {
            return SecureCatalogImpl.unauthorizedAccess();
        } else
            return new UnsupportedOperationException(
                    "This data access is read only, service code is supposed to perform writes via FeatureStore instead");
    }
}
