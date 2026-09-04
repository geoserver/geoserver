/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import java.io.IOException;
import org.geoserver.geofence.cache.CacheConfiguration;
import org.geoserver.geofence.cache.CacheManager;
import org.geoserver.geofence.services.RestRuleReaderService;
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

    private final RestRuleReaderService restRuleReaderService;

    @Autowired
    public GeoFenceConfigurationController(
            GeoFenceConfigurationManager configurationManager,
            CacheManager cacheManager,
            @Qualifier("ruleReaderBackendFactory") RuleReaderServiceFactory ruleReaderBackendFactory,
            @Qualifier("ruleReaderFrontendFactory") RuleReaderServiceFactory ruleReaderFrontendFactory,
            RestRuleReaderService restRuleReaderService) {
        this.configurationManager = configurationManager;
        this.cacheManager = cacheManager;
        this.ruleReaderBackendFactory = ruleReaderBackendFactory;
        this.ruleReaderFrontendFactory = ruleReaderFrontendFactory;
        this.restRuleReaderService = restRuleReaderService;
    }

    /** Updates the configuration, refreshes the classes that need it, then stores it to disk. */
    public void storeConfiguration(GeoFenceConfiguration gfConfig, CacheConfiguration cacheConfig) throws IOException {
        configurationManager.setConfiguration(gfConfig);

        configurationManager.setCacheConfiguration(cacheConfig);
        cacheManager.init();

        ruleReaderBackendFactory.setActiveServiceName(gfConfig.getRuleReaderBackend());
        ruleReaderFrontendFactory.setActiveServiceName(gfConfig.getRuleReaderFrontend());

        // so a URL edit takes effect without a GeoServer restart
        restRuleReaderService.setServiceUrl(gfConfig.getServicesUrl());

        configurationManager.storeConfiguration();
    }
}
