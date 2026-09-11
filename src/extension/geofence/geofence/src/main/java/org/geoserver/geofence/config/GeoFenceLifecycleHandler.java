/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.geofence.cache.CacheManager;
import org.geoserver.geofence.services.RestRuleReaderService;
import org.geoserver.geofence.services.RuleReaderServiceFactory;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Ties GeoFence's config/cache refresh into GeoServer's reload, mirroring what
 * {@link GeoFenceConfigurationController#storeConfiguration} already does on the admin page's Save button - so editing
 * {@code geofence.properties} on disk and clicking "Reload Configuration" (or {@code POST /rest/reload}) takes effect
 * without a restart. Embedded-engine datasource reload is handled separately, in {@code geofence-server} (this module
 * must not depend on the embedded engine's persistence layer - it also backs REST-only deployments).
 */
@Component
public class GeoFenceLifecycleHandler implements GeoServerLifecycleHandler {

    private static final Logger LOGGER = Logging.getLogger(GeoFenceLifecycleHandler.class);

    private final GeoFenceConfigurationManager configurationManager;
    private final CacheManager cacheManager;
    private final RuleReaderServiceFactory ruleReaderBackendFactory;
    private final RuleReaderServiceFactory ruleReaderFrontendFactory;
    private final RestRuleReaderService restRuleReaderService;

    @Autowired
    public GeoFenceLifecycleHandler(
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

    @Override
    public void onReset() {
        cacheManager.invalidateAll();
    }

    @Override
    public void onDispose() {
        // nothing to release - no pooled/closeable resource is owned here
    }

    @Override
    public void beforeReload() {
        // nothing needed before reload starts - the real work happens once the new config is in place
    }

    @Override
    public void onReload() {
        if (!applyConfiguration()) {
            // fail closed rather than keep serving the superseded configuration
            ruleReaderBackendFactory.denyUntilRecovered("geofence.properties", this::applyConfiguration);
        }
    }

    /**
     * Re-reads {@code geofence.properties} and applies it. A missing file is not a failure - defaults stay in force, as
     * at startup; only a file that is present but unusable returns false.
     */
    private boolean applyConfiguration() {
        try {
            configurationManager.loadConfiguration();
            GeoFenceConfiguration cfg = configurationManager.getConfiguration();
            cacheManager.init();
            ruleReaderBackendFactory.setActiveServiceName(cfg.getRuleReaderBackend());
            ruleReaderFrontendFactory.setActiveServiceName(cfg.getRuleReaderFrontend());
            restRuleReaderService.setServiceUrl(cfg.getServicesUrl());
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Could not apply the new GeoFence configuration", e);
            return false;
        }
    }
}
