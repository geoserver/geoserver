/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import java.io.File;

import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.jdbc.config.JDBCSecurityServiceConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;

import static org.geoserver.security.jdbc.JDBCSecurityConfigException.*;

public class JdbcSecurityConfigValidator extends SecurityConfigValidator {

    public JdbcSecurityConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public void validate(SecurityRoleServiceConfig config) throws SecurityConfigException {
        super.validate(config);
        JDBCSecurityServiceConfig jdbcConfig = (JDBCSecurityServiceConfig) config;
        
        validateFileNames(jdbcConfig,JDBCRoleService.DEFAULT_DDL_FILE,JDBCRoleService.DEFAULT_DML_FILE);
        checkAutomaticTableCreation(jdbcConfig);
        
        if (jdbcConfig.isJndi())
            validateJNDI(jdbcConfig);
        else 
            validateJDBC(jdbcConfig);
    }
    
    @Override
    public void validate(SecurityUserGroupServiceConfig config)
            throws SecurityConfigException {
        super.validate(config);
                        
        JDBCSecurityServiceConfig jdbcConfig = (JDBCSecurityServiceConfig) config;
        
        validateFileNames(jdbcConfig,JDBCUserGroupService.DEFAULT_DDL_FILE,JDBCUserGroupService.DEFAULT_DML_FILE);
        checkAutomaticTableCreation(jdbcConfig);
        
        if (jdbcConfig.isJndi())
            validateJNDI(jdbcConfig);
        else
            validateJDBC(jdbcConfig);
    }
    
   protected void checkAutomaticTableCreation (JDBCSecurityServiceConfig config) throws SecurityConfigException {
        if (config.isCreatingTables()) {
            if (isNotEmpty(config.getPropertyFileNameDDL())==false)
                throw createSecurityException(SEC_ERR_204);
        }
    }
    
    protected void validateFileNames(JDBCSecurityServiceConfig config, String defaultDDL, String defaultDML) throws SecurityConfigException    
    {
        
        String fileName = config.getPropertyFileNameDDL();        
        // ddl may be null
        if (isNotEmpty(fileName)) {
            if (defaultDDL.equals(fileName)==false) {
                // not the default property file
                File file = new File(fileName);
                if (checkFile(file)==false) {
                    throw createSecurityException(SEC_ERR_211, fileName);
                }
            }
        }
        
        fileName = config.getPropertyFileNameDML();
        if (isNotEmpty(fileName)==false) {
            // dml file is required
            throw createSecurityException(SEC_ERR_212);
        }
        
        if (defaultDML.equals(fileName)==false) {
            // not the default property file
            File file = new File(fileName);
            if (checkFile(file)==false) {
                throw createSecurityException(SEC_ERR_213, fileName);
            }
        }
    }
    
    protected void validateJNDI(JDBCSecurityServiceConfig config) throws SecurityConfigException {
        if (isNotEmpty(config.getJndiName())==false)
            throw createSecurityException(SEC_ERR_210);
    }
    
    protected void validateJDBC(JDBCSecurityServiceConfig config) throws SecurityConfigException {
        if (isNotEmpty(config.getDriverClassName())==false)
            throw createSecurityException(SEC_ERR_200);
        if (isNotEmpty(config.getUserName())==false)
            throw createSecurityException(SEC_ERR_201);
        if (isNotEmpty(config.getConnectURL())==false)
            throw createSecurityException(SEC_ERR_202);

        try {
            Class.forName(config.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw createSecurityException(SEC_ERR_203,
                    config.getDriverClassName());
        }

    }

    @Override
    public void validate(SecurityAuthProviderConfig config) throws SecurityConfigException {
        super.validate(config);
        JDBCConnectAuthProviderConfig jdbcConfig = (JDBCConnectAuthProviderConfig) config;
        if (isNotEmpty(jdbcConfig.getDriverClassName())==false)
            throw createSecurityException(SEC_ERR_200);
        if (isNotEmpty(jdbcConfig.getConnectURL())==false)
            throw createSecurityException(SEC_ERR_202);

        try {
            Class.forName(jdbcConfig.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw createSecurityException(SEC_ERR_203,
                    jdbcConfig.getDriverClassName());
        }

        
    }
}
