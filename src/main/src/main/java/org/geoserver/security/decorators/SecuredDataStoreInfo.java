/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.util.ProgressListener;

/**
 * Given a {@link DataStoreInfo} makes sure no write operations can be performed through it
 *
 * @author Andrea Aime - TOPP
 * @param <T>
 * @param <F>
 */
@SuppressWarnings("serial")
public class SecuredDataStoreInfo extends DecoratingDataStoreInfo {

    WrapperPolicy policy;

    public SecuredDataStoreInfo(DataStoreInfo delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public DataAccess<? extends FeatureType, ? extends Feature> getDataStore(
            ProgressListener listener) throws IOException {
        final DataAccess<? extends FeatureType, ? extends Feature> ds =
                super.getDataStore(listener);
        if (ds == null) return null;
        else if (policy.level == AccessLevel.METADATA)
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        else
            return (DataAccess<? extends FeatureType, ? extends Feature>)
                    SecuredObjects.secure(ds, policy);
    }
}
