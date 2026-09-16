/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.config;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geofence.core.db.config.DatasourcePasswordDecoder;
import org.geofence.core.db.config.DatasourcePropertiesLoader;
import org.geofence.core.db.config.GeoFenceConfigDirectoryProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecurityManagerListener;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Encrypts a {@code plain:}-marked datasource password at rest, once the security manager has loaded its config.
 *
 * <p>The datasource beans are often built during startup, before an encoder is available, so the read path alone can't
 * be relied on to perform the upgrade - and it only ever reads the file once per JVM.
 */
@Component
public class GeoFenceDatasourcePasswordEncryptor implements SecurityManagerListener {

    private static final Logger LOGGER = Logging.getLogger(GeoFenceDatasourcePasswordEncryptor.class);

    private final GeoServerSecurityManager securityManager;
    private final DatasourcePasswordDecoder passwordDecoder;
    private final Optional<GeoFenceConfigDirectoryProvider> configDirProvider;

    @Autowired
    public GeoFenceDatasourcePasswordEncryptor(
            GeoServerSecurityManager securityManager,
            DatasourcePasswordDecoder passwordDecoder,
            Optional<GeoFenceConfigDirectoryProvider> configDirProvider) {
        this.securityManager = securityManager;
        this.passwordDecoder = passwordDecoder;
        this.configDirProvider = configDirProvider;
    }

    @PostConstruct
    void register() {
        securityManager.addListener(this);
    }

    @Override
    public void handlePostChanged(GeoServerSecurityManager securityManager) {
        try {
            new DatasourcePropertiesLoader().encryptStoredPassword(configDirProvider, passwordDecoder);
        } catch (RuntimeException e) {
            // this runs inside the security manager's own init: propagating would fail GeoServer startup outright
            LOGGER.log(Level.WARNING, "Could not encrypt the GeoFence datasource password at rest", e);
        }
    }
}
