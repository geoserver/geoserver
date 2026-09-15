/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import org.geoserver.geofence.cache.CacheConfiguration;
import org.geoserver.geofence.cache.CacheManager;
import org.geoserver.geofence.services.RestRuleReaderService;
import org.geoserver.geofence.services.RuleReaderServiceFactory;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;

public class GeoFenceConfigurationControllerTest {

    private GenericApplicationContext context;

    @After
    public void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    /** An unusable rule reader name must be rejected before any part of the configuration is applied. */
    @Test
    public void testInvalidBackendLeavesConfigurationUntouched() {
        context = new GenericApplicationContext();
        context.refresh();

        RuleReaderServiceFactory backendFactory = new RuleReaderServiceFactory("noSuchBean", false);
        backendFactory.setApplicationContext(context);
        RuleReaderServiceFactory frontendFactory = new RuleReaderServiceFactory("noSuchBean", true);
        frontendFactory.setApplicationContext(context);

        RecordingConfigurationManager configManager = new RecordingConfigurationManager();
        GeoFenceConfigurationController controller = new GeoFenceConfigurationController(
                configManager,
                new CacheManager(configManager),
                backendFactory,
                frontendFactory,
                new RestRuleReaderService());

        GeoFenceConfiguration cfg = new GeoFenceConfiguration();
        cfg.setRuleReaderBackend("noSuchBean");
        cfg.setRuleReaderFrontend("noSuchBean");

        assertThrows(
                IllegalArgumentException.class, () -> controller.storeConfiguration(cfg, new CacheConfiguration()));

        assertFalse("the rejected configuration must not be applied", configManager.configurationApplied);
        assertFalse("the rejected configuration must not be persisted", configManager.configurationStored);
    }

    private static class RecordingConfigurationManager extends GeoFenceConfigurationManager {

        boolean configurationApplied;
        boolean configurationStored;

        @Override
        public void setConfiguration(GeoFenceConfiguration cfg) {
            configurationApplied = true;
        }

        @Override
        public void storeConfiguration() {
            configurationStored = true;
        }
    }
}
