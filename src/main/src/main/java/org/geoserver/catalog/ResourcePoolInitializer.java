/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.List;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerReinitializer;
import org.geoserver.util.EntityResolverProvider;

/**
 * Initializes parameters of the {@link ResourcePool} class from configuration.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ResourcePoolInitializer implements GeoServerReinitializer {

    GeoServer gs;
    EntityResolverProvider resolverProvider;

    public ResourcePoolInitializer(EntityResolverProvider resolverProvider) {
        this.resolverProvider = resolverProvider;
    }

    public void initialize(GeoServer geoServer) throws Exception {
        this.gs = geoServer;

        final GeoServerInfo global = geoServer.getGlobal();
        final int cacheSize = global.getFeatureTypeCacheSize();
        if (cacheSize > 0) {
            gs.getCatalog().getResourcePool().setFeatureTypeCacheSize(cacheSize);
        }

        geoServer.addListener(
                new ConfigurationListenerAdapter() {
                    @Override
                    public void handleGlobalChange(
                            GeoServerInfo global,
                            List<String> propertyNames,
                            List<Object> oldValues,
                            List<Object> newValues) {
                        int i = propertyNames.indexOf("featureTypeCacheSize");
                        if (i > -1) {
                            Number featureTypeCacheSize = (Number) newValues.get(i);
                            gs.getCatalog()
                                    .getResourcePool()
                                    .setFeatureTypeCacheSize(featureTypeCacheSize.intValue());
                        }
                        gs.getCatalog()
                                .getResourcePool()
                                .setCoverageExecutor(
                                        global.getCoverageAccess().getThreadPoolExecutor());
                    }
                });

        gs.getCatalog().getResourcePool().setEntityResolverProvider(resolverProvider);
    }
}
