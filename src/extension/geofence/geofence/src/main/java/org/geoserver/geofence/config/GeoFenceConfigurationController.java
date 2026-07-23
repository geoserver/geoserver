/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import java.io.IOException;
import org.geoserver.geofence.cache.CacheConfiguration;
import org.geoserver.geofence.cache.CacheManager;
import org.geoserver.geofence.services.RuleReaderServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** @author Emanuele Tajariol (etj at geo-solutions.it) */
@Component
public class GeoFenceConfigurationController {

    private final GeoFenceConfigurationManager configurationManager;

    private final CacheManager cacheManager;

    private final RuleReaderServiceFactory ruleReaderBackendFactory;

    private final RuleReaderServiceFactory ruleReaderFrontendFactory;

    @Autowired
    public GeoFenceConfigurationController(
            GeoFenceConfigurationManager configurationManager,
            CacheManager cacheManager,
            @Qualifier("ruleReaderBackendFactory") RuleReaderServiceFactory ruleReaderBackendFactory,
            @Qualifier("ruleReaderFrontendFactory") RuleReaderServiceFactory ruleReaderFrontendFactory) {
        this.configurationManager = configurationManager;
        this.cacheManager = cacheManager;
        this.ruleReaderBackendFactory = ruleReaderBackendFactory;
        this.ruleReaderFrontendFactory = ruleReaderFrontendFactory;
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

        // switch the active RuleReaderService backend/frontend, if they changed
        ruleReaderBackendFactory.setActiveServiceName(gfConfig.getRuleReaderBackend());
        ruleReaderFrontendFactory.setActiveServiceName(gfConfig.getRuleReaderFrontend());

        // write the config to disk
        configurationManager.storeConfiguration();
    }
}
