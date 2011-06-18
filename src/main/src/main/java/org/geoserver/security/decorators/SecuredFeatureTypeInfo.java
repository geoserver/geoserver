/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.AccessLevel;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.FeatureSource;
import org.geotools.factory.Hints;
import org.opengis.util.ProgressListener;

/**
 * Wraps a {@link FeatureTypeInfo} so that it will return a secured
 * FeatureSource
 * 
 * @author Andrea Aime - TOPP
 */
public class SecuredFeatureTypeInfo extends DecoratingFeatureTypeInfo {

    WrapperPolicy policy;

    public SecuredFeatureTypeInfo(FeatureTypeInfo info, WrapperPolicy policy) {
        super(info);
        this.policy = policy;
    }

    //--------------------------------------------------------------------------
    // WRAPPED METHODS TO ENFORCE SECURITY POLICY
    //--------------------------------------------------------------------------

    public FeatureSource getFeatureSource(ProgressListener listener, Hints hints)
            throws IOException {
        final FeatureSource fs = delegate.getFeatureSource(listener, hints);
        
        if(policy.level == AccessLevel.METADATA) {
            throw SecureCatalogImpl.unauthorizedAccess(this.getName());
        } else {
            return (FeatureSource) SecuredObjects.secure(fs, policy);
        }
    }

    public DataStoreInfo getStore() {
        return (DataStoreInfo) SecuredObjects.secure(delegate.getStore(), policy);
    }
}
