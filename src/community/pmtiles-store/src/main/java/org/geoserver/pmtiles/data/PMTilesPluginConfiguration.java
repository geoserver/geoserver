/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.data;

import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geotools.pmtiles.store.PMTilesDataStore;
import org.geotools.pmtiles.store.PMTilesDataStoreFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the GeoServer PMTiles plugin.
 *
 * <p>This configuration class defines the beans required for PMTiles integration with GeoServer:
 *
 * <ul>
 *   <li>{@link PMTilesCacheInvalidator} - Clears caches on GeoServer reset
 * </ul>
 *
 * @see PMTilesCacheInvalidator
 * @see PMTilesWmsIntegrationConfiguration
 */
@Configuration(proxyBeanMethods = false)
public class PMTilesPluginConfiguration {
    /**
     * Creates the lifecycle handler for cache invalidation on GeoServer reset.
     *
     * @return the cache invalidator bean
     */
    @Bean
    PMTilesCacheInvalidator pmTilesCacheInvalidator() {
        return new PMTilesCacheInvalidator();
    }

    /**
     * GeoServer lifecycle handler that invalidates PMTiles caches when the server configuration is reset.
     *
     * <p>This handler ensures that PMTiles caches (tile data, directory entries, etc.) are cleared when GeoServer's
     * configuration is reset through the admin UI or REST API. This is important for:
     *
     * <ul>
     *   <li>Ensuring updated PMTiles files are re-read after replacement
     *   <li>Freeing memory when the server configuration is reset
     *   <li>Maintaining consistency between GeoServer's catalog and cached tile data
     * </ul>
     *
     * @see PMTilesDataStoreFactory#clearCaches()
     * @see GeoServerLifecycleHandler
     */
    static class PMTilesCacheInvalidator implements GeoServerLifecycleHandler {

        /**
         * Clears the caches used by {@link PMTilesDataStore} and its related classes.
         *
         * @see PMTilesDataStoreFactory#clearCaches()
         */
        @Override
        public void onReset() {
            PMTilesDataStoreFactory.clearCaches();
        }

        /** {@inheritDoc} */
        @Override
        public void onDispose() {
            // no-op
        }

        /** {@inheritDoc} */
        @Override
        public void beforeReload() {
            // no-op
        }

        /** {@inheritDoc} */
        @Override
        public void onReload() {
            // no-op
        }
    }
}
