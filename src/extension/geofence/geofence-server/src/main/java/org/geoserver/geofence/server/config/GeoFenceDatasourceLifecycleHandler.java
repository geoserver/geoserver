/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.config;

import org.geofence.core.db.config.GeofencePersistenceConfig;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.geofence.services.RuleReaderServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Reconnects the embedded engine's datasource on GeoServer reload, mirroring how GeoServer's own DataStores reconnect -
 * lives here rather than in {@code gs-geofence} since only this module (the embedded-engine one) depends on the
 * persistence layer.
 */
@Component
public class GeoFenceDatasourceLifecycleHandler implements GeoServerLifecycleHandler {

    private final GeofencePersistenceConfig persistenceConfig;
    private final RuleReaderServiceFactory ruleReaderBackendFactory;

    @Autowired
    public GeoFenceDatasourceLifecycleHandler(
            GeofencePersistenceConfig persistenceConfig,
            @Qualifier("ruleReaderBackendFactory") RuleReaderServiceFactory ruleReaderBackendFactory) {
        this.persistenceConfig = persistenceConfig;
        this.ruleReaderBackendFactory = ruleReaderBackendFactory;
    }

    @Override
    public void onReset() {
        // nothing to do - the connection pool itself isn't a cache
    }

    @Override
    public void onDispose() {
        // nothing to do - the datasource bean is destroyed by Spring's own context shutdown
    }

    @Override
    public void beforeReload() {
        // nothing needed before reload starts
    }

    @Override
    public void onReload() {
        if (!persistenceConfig.reloadDatasource()) {
            // fail closed, like core disables a store whose password won't decrypt
            ruleReaderBackendFactory.denyUntilRecovered(
                    "geofence-datasource.properties", persistenceConfig::reloadDatasource);
        }
    }
}
