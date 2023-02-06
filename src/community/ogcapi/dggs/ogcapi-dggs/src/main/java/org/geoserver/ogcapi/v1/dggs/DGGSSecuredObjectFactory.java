/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import org.geoserver.platform.ExtensionPriority;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredObjectFactory;
import org.geotools.data.DataStore;
import org.geotools.dggs.gstore.DGGSStore;
import org.springframework.stereotype.Component;

/**
 * Custom security wrapper, for the moment used mostly to make the DGGS specific methods visible to
 * the API, otherwise the core secure wrappers would hide them.
 */
@Component
public class DGGSSecuredObjectFactory implements SecuredObjectFactory {
    @Override
    public boolean canSecure(Class<?> clazz) {
        // TODO: handle DGGSFeatureSource methods providing data access
        return DGGSStore.class.isAssignableFrom(clazz);
    }

    @Override
    public Object secure(Object object, WrapperPolicy policy) {
        // mimicking DefaultSecureDataFactory
        if (object instanceof DGGSStore) {
            return new ReadOnlyDGGSStore((DataStore) object, policy);
        }

        return null;
    }

    @Override
    public int getPriority() {
        // go above the
        return ExtensionPriority.HIGHEST;
    }
}
