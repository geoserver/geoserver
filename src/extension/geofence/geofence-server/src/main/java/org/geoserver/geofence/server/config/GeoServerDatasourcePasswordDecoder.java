/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.config;

import org.geofence.core.db.config.DatasourcePasswordDecoder;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.ConfigurationPasswordEncryptionHelper;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Decrypts the GeoFence datasource password via GeoServer's config-password encryption. A {@code plain:}-marked value
 * is decrypted for use and re-encrypted for the loader to persist back, mirroring GeoServer's store-password handling.
 */
@Component
public class GeoServerDatasourcePasswordDecoder implements DatasourcePasswordDecoder {

    private final GeoServerSecurityManager securityManager;
    private final ConfigurationPasswordEncryptionHelper passwordHelper;

    public GeoServerDatasourcePasswordDecoder(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
        this.passwordHelper = new ConfigurationPasswordEncryptionHelper(securityManager);
    }

    @Override
    public Result decode(String storedPassword) {
        if (storedPassword == null) {
            return Result.asIs(null);
        }
        String plaintext = passwordHelper.decode(storedPassword);
        // Same guard XStreamPersister applies to store passwords: before init, encode() silently wouldn't encrypt.
        if (securityManager.isInitialized() && isPlainMarked(storedPassword)) {
            return Result.persist(plaintext, passwordHelper.encode(plaintext));
        }
        return Result.asIs(plaintext);
    }

    private boolean isPlainMarked(String value) {
        GeoServerPlainTextPasswordEncoder plain =
                securityManager.loadPasswordEncoder(GeoServerPlainTextPasswordEncoder.class);
        return plain != null && plain.isResponsibleForEncoding(value);
    }
}
