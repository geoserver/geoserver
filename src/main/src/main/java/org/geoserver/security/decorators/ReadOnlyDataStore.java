/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.Response;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * Given a {@link DataStore} subclass makes sure no write operations can be performed through it.
 * Regardless of the policy the store is kept read only as services are supposed to perform writes
 * via {@link FeatureStore} instances returned by {@link FeatureTypeInfo} and not via direct data
 * store access.
 *
 * @author Andrea Aime - TOPP
 */
public class ReadOnlyDataStore extends org.geotools.data.store.DecoratingDataStore {

    WrapperPolicy policy;

    protected ReadOnlyDataStore(DataStore delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public SimpleFeatureSource getFeatureSource(Name typeName) throws IOException {
        final SimpleFeatureSource fs = super.getFeatureSource(typeName);
        return wrapFeatureSource(fs);
    }

    @Override
    public SimpleFeatureSource getFeatureSource(String typeName) throws IOException {
        final SimpleFeatureSource fs = super.getFeatureSource(typeName);
        return wrapFeatureSource(fs);
    }

    @SuppressWarnings("unchecked")
    SimpleFeatureSource wrapFeatureSource(final SimpleFeatureSource fs) {
        if (fs == null) return null;

        WrapperPolicy childPolicy = buildPolicyForFeatureSource();
        return DataUtilities.simple((FeatureSource) SecuredObjects.secure(fs, childPolicy));
    }

    private WrapperPolicy buildPolicyForFeatureSource() {
        WrapperPolicy childPolicy;
        if (policy.getLimits() instanceof VectorAccessLimits) {
            childPolicy = policy;
        } else {
            final AccessLimits limits = policy.getLimits();
            VectorAccessLimits vectorLimits =
                    new VectorAccessLimits(
                            limits.getMode(), null, Filter.INCLUDE, null, Filter.EXCLUDE);
            childPolicy = this.policy.derive(vectorLimits);
        }
        return childPolicy;
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Filter filter, Transaction transaction) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(
            String typeName, Transaction transaction) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(
            String typeName, Transaction transaction) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void updateSchema(String typeName, SimpleFeatureType featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void createSchema(SimpleFeatureType featureType) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw notifyUnsupportedOperation();
    }

    @Override
    public void removeSchema(String typeName) throws IOException {
        throw notifyUnsupportedOperation();
    }

    /**
     * Notifies the caller the requested operation is not supported, using a plain {@link
     * UnsupportedOperationException} in case we have to conceal the fact the data is actually
     * writable, using an Spring security exception otherwise to force an authentication from the
     * user
     */
    protected RuntimeException notifyUnsupportedOperation() {
        if (policy.response == Response.CHALLENGE) {
            return SecureCatalogImpl.unauthorizedAccess();
        } else
            return new UnsupportedOperationException(
                    "This datastore is read only, service code is supposed to perform writes via FeatureStore instead");
    }
}
