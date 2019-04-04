/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import static org.geoserver.security.jdbc.JDBCSecurityConfigException.*;

import java.io.File;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;

public class JdbcSecurityConfigValidator extends SecurityConfigValidator {

    public JdbcSecurityConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public void validate(SecurityRoleServiceConfig config) throws SecurityConfigException {
        super.validate(config);
        JDBCSecurityServiceConfig jdbcConfig = (JDBCSecurityServiceConfig) config;

        validateFileNames(
                jdbcConfig, JDBCRoleService.DEFAULT_DDL_FILE, JDBCRoleService.DEFAULT_DML_FILE);
        checkAutomaticTableCreation(jdbcConfig);

        if (jdbcConfig.isJndi()) validateJNDI(jdbcConfig);
        else validateJDBC(jdbcConfig);
    }

    @Override
    public void validate(SecurityUserGroupServiceConfig config) throws SecurityConfigException {
        super.validate(config);

        JDBCSecurityServiceConfig jdbcConfig = (JDBCSecurityServiceConfig) config;

        validateFileNames(
                jdbcConfig,
                JDBCUserGroupService.DEFAULT_DDL_FILE,
                JDBCUserGroupService.DEFAULT_DML_FILE);
        checkAutomaticTableCreation(jdbcConfig);

        if (jdbcConfig.isJndi()) validateJNDI(jdbcConfig);
        else validateJDBC(jdbcConfig);
    }

    protected void checkAutomaticTableCreation(JDBCSecurityServiceConfig config)
            throws SecurityConfigException {
        if (config.isCreatingTables()) {
            if (isNotEmpty(config.getPropertyFileNameDDL()) == false)
                throw createSecurityException(DDL_FILE_REQUIRED);
        }
    }

    protected void validateFileNames(
            JDBCSecurityServiceConfig config, String defaultDDL, String defaultDML)
            throws SecurityConfigException {

        String fileName = config.getPropertyFileNameDDL();
        // ddl may be null
        if (isNotEmpty(fileName)) {
            if (defaultDDL.equals(fileName) == false) {
                // not the default property file
                File file = new File(fileName);
                if (checkFile(file) == false) {
                    throw createSecurityException(DDL_FILE_INVALID, fileName);
                }
            }
        }

        fileName = config.getPropertyFileNameDML();
        if (isNotEmpty(fileName) == false) {
            // dml file is required
            throw createSecurityException(DML_FILE_REQUIRED);
        }

        if (defaultDML.equals(fileName) == false) {
            // not the default property file
            File file = new File(fileName);
            if (checkFile(file) == false) {
                throw createSecurityException(DML_FILE_INVALID, fileName);
            }
        }
    }

    protected void validateJNDI(JDBCSecurityServiceConfig config) throws SecurityConfigException {
        if (isNotEmpty(config.getJndiName()) == false)
            throw createSecurityException(JNDINAME_REQUIRED);
    }

    protected void validateJDBC(JDBCSecurityServiceConfig config) throws SecurityConfigException {
        if (isNotEmpty(config.getDriverClassName()) == false)
            throw createSecurityException(DRIVER_CLASSNAME_REQUIRED);
        if (isNotEmpty(config.getUserName()) == false)
            throw createSecurityException(USERNAME_REQUIRED);
        if (isNotEmpty(config.getConnectURL()) == false)
            throw createSecurityException(JDBCURL_REQUIRED);

        try {
            Class.forName(config.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw createSecurityException(DRIVER_CLASS_NOT_FOUND_$1, config.getDriverClassName());
        }
    }

    @Override
    public void validate(SecurityAuthProviderConfig config) throws SecurityConfigException {
        super.validate(config);
        JDBCConnectAuthProviderConfig jdbcConfig = (JDBCConnectAuthProviderConfig) config;
        if (isNotEmpty(jdbcConfig.getDriverClassName()) == false)
            throw createSecurityException(DRIVER_CLASSNAME_REQUIRED);
        if (isNotEmpty(jdbcConfig.getConnectURL()) == false)
            throw createSecurityException(JDBCURL_REQUIRED);

        try {
            Class.forName(jdbcConfig.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw createSecurityException(
                    DRIVER_CLASS_NOT_FOUND_$1, jdbcConfig.getDriverClassName());
        }
    }
}
