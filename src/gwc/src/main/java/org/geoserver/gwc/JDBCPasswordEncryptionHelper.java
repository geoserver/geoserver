/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.ConfigurationPasswordEncryptionHelper;
import org.geotools.util.logging.Logging;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration.ConnectionPoolConfiguration;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

/**
 * Helper class that encodes and decodes the JDBC connection pool password on demand
 *
 * @author Andrea Aime - GeoSolutions
 */
class JDBCPasswordEncryptionHelper {

    static final Logger LOGGER = Logging.getLogger(JDBCPasswordEncryptionHelper.class);

    private ConfigurationPasswordEncryptionHelper passwords;

    public JDBCPasswordEncryptionHelper(GeoServerSecurityManager securityManager) {
        this.passwords = securityManager.getConfigPasswordEncryptionHelper();
    }

    /**
     * If the connection pool password is encrypted, this method will un-encrypt it using the
     * GeoServer password encoders
     *
     * @param configuration A deep copy of the configuration with the unencrypted password, if the
     *     password was encrypted, or the original one, if the password was plaintext
     */
    public JDBCConfiguration unencryptPassword(JDBCConfiguration configuration) {
        if (configuration.getConnectionPool() != null
                && configuration.getConnectionPool().getPassword() != null) {
            String password = configuration.getConnectionPool().getPassword();
            try {
                String decoded = passwords.decode(password);

                configuration = cloneAndSetPassword(configuration, decoded);
            } catch (EncryptionOperationNotPossibleException e) {
                // fine, it must have been a plain text password
                LOGGER.log(
                        Level.FINE,
                        "Unencrypting the password failed, assuming it is a plain text one",
                        e);
            }
        }

        return configuration;
    }

    /**
     * Encrypts the connection pool password, if not null, using the GeoServer password encoders.
     *
     * @param configuration A deep copy ofthe configuration, with the password encoded
     */
    public JDBCConfiguration encryptPassword(JDBCConfiguration configuration) {
        ConnectionPoolConfiguration pool = configuration.getConnectionPool();
        if (pool != null && pool.getPassword() != null) {
            String password = pool.getPassword();
            String encoded = passwords.encode(password);

            configuration = cloneAndSetPassword(configuration, encoded);
        }

        return configuration;
    }

    private JDBCConfiguration cloneAndSetPassword(JDBCConfiguration configuration, String encoded) {
        // do a deep clone of the config to avoid altering its contents
        ConnectionPoolConfiguration original = configuration.getConnectionPool();
        ConnectionPoolConfiguration clone = new ConnectionPoolConfiguration();
        clone.setConnectionTimeout(original.getConnectionTimeout());
        clone.setDriver(original.getDriver());
        clone.setMaxConnections(original.getMaxConnections());
        clone.setMaxOpenPreparedStatements(original.getMaxOpenPreparedStatements());
        clone.setMinConnections(original.getMinConnections());
        clone.setPassword(encoded);
        clone.setUrl(original.getUrl());
        clone.setUsername(original.getUsername());
        clone.setValidationQuery(original.getValidationQuery());

        JDBCConfiguration result = new JDBCConfiguration();
        result.setConnectionPool(clone);
        result.setDialect(configuration.getDialect());
        result.setJNDISource(configuration.getJNDISource());
        return result;
    }
}
