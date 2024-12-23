/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import java.io.IOException;
import org.geoserver.geofence.cache.CacheConfiguration;
import org.geoserver.geofence.cache.CacheManager;

/** @author Emanuele Tajariol (etj at geo-solutions.it) */
public class GeoFenceConfigurationController {

    private GeoFenceConfigurationManager configurationManager;

    private CacheManager cacheManager;

    public void setConfigurationManager(GeoFenceConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Updates the configuration.
     *
     * <p>Sets the config into the manager and forces the classes needing to refresh to do so. Then stores the config to
     * disk.
     */
    public void storeConfiguration(GeoFenceConfiguration gfConfig, CacheConfiguration cacheConfig) throws IOException {

        // set the probe configuration. the access manager performs a getCOnfiguration wheneven
        // needed
        configurationManager.setConfiguration(gfConfig);

        // set config and recreates the cache
        configurationManager.setCacheConfiguration(cacheConfig);
        cacheManager.init();

        // write the config to disk
        configurationManager.storeConfiguration();
    }
}
