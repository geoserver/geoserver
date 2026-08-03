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
 * Decrypts the GeoFence datasource password using GeoServer's own config-password encryption, so the credential in
 * {@code geofence-datasource.properties} can be stored encrypted (same scheme GeoServer uses for store connection
 * passwords) rather than in clear text. Only present when GeoFence runs embedded in GeoServer; the standalone webapp
 * has no such bean, so its password is used as-is.
 *
 * <p>Mirrors GeoServer's store-password handling:
 *
 * <ul>
 *   <li>An already-encrypted value is decrypted; a bare plaintext value (no recognized prefix) is left as-is - so
 *       existing unencrypted configurations keep working and are not silently rewritten.
 *   <li>A value the operator wrote in clear behind the {@code plain:} marker is decrypted for use <em>and</em>
 *       re-encrypted so the loader can persist the encrypted form back to the file - the same "type it once in clear,
 *       it gets encrypted at rest" flow GeoServer offers for store passwords.
 * </ul>
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
        if (isPlainMarked(storedPassword)) {
            // operator typed the password in clear behind the plain: marker - hand back the plaintext to connect
            // with, plus an encrypted form so the loader can rewrite the file and not leave it readable
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
