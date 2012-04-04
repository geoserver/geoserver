package org.geoserver.security.jdbc;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.jdbc.config.JDBCRoleServiceConfig;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidatorTest;
import org.geotools.util.logging.Logging;

import static org.geoserver.security.jdbc.JDBCSecurityConfigException.*;

public class JdbcSecurityConfigValidatorTest extends SecurityConfigValidatorTest  {

    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");
    
    
    @Override
    protected SecurityUserGroupServiceConfig getUGConfig(String name, Class<?> aClass,
            String encoder, String policyName) {
        JDBCUserGroupServiceConfig config = new JDBCUserGroupServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setPasswordEncoderName(encoder);
        config.setPasswordPolicyName(policyName);
        config.setCreatingTables(false);
        return config;
    }
    
    @Override
    protected SecurityRoleServiceConfig getRoleConfig(String name, Class<?> aClass,String adminRole) {
        JDBCRoleServiceConfig config = new JDBCRoleServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setAdminRoleName(adminRole);
        config.setCreatingTables(false);
        return config;
    }
    
    @Override
    protected SecurityAuthProviderConfig getAuthConfig(String name, Class<?> aClass,String userGroupServiceName) {
        JDBCConnectAuthProviderConfig config = new JDBCConnectAuthProviderConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setUserGroupServiceName(userGroupServiceName);        
        return config;
    }

    @Override
    public void testRoleConfig() throws IOException {
        
        super.testRoleConfig();
        
        JDBCRoleServiceConfig  config = 
                (JDBCRoleServiceConfig)getRoleConfig("jdbc", JDBCRoleService.class, 
                GeoServerRole.ADMIN_ROLE.getAuthority());
        
        config.setDriverClassName("a.b.c");
        config.setUserName("user");
        config.setConnectURL("jdbc:connect");
        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
        
        JDBCRoleServiceConfig  configJNDI = (JDBCRoleServiceConfig) 
                getRoleConfig("jndi", JDBCRoleService.class, 
                GeoServerRole.ADMIN_ROLE.getAuthority());
        configJNDI.setJndi(true);
        configJNDI.setJndiName("jndi:connect");
        configJNDI.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        configJNDI.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);

        
        boolean fail;

        
        
        fail=false;
        try {            
            configJNDI.setJndiName("");
            getSecurityManager().saveRoleService(configJNDI);
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_210,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        fail=false;
        try {            
            config.setDriverClassName("");
            getSecurityManager().saveRoleService(config);
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_200,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setDriverClassName("a.b.c");
        fail=false;
        try {            
            config.setUserName("");
            getSecurityManager().saveRoleService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_201,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setUserName("user");
        fail=false;
        try {            
            config.setConnectURL(null);
            getSecurityManager().saveRoleService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_202,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        config.setConnectURL("jdbc:connect");
        try {            
            getSecurityManager().saveRoleService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_203,ex.getErrorId());
            assertEquals("a.b.c",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        config.setDriverClassName("java.lang.String");
                
        config.setPropertyFileNameDDL(null);
        try {
            getSecurityManager().saveRoleService(config);
        } catch (SecurityConfigException ex) {
            throw new IOException(ex);
        }
        config.setPropertyFileNameDML(null);
        try {            
            getSecurityManager().saveRoleService(config);
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_212,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
        
        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir()!=null) {
            String invalidPath="abc"+File.separator+"def.properties";
            config.setPropertyFileNameDDL(invalidPath);
            fail=false;
            try {
                getSecurityManager().saveRoleService(config);
            } catch (SecurityConfigException ex) {
                assertEquals(SEC_ERR_211, ex.getErrorId());
                assertEquals(invalidPath, ex.getArgs()[0]);
                fail=true;
            }
            assertTrue(fail);
        }
 
        config.setPropertyFileNameDDL(JDBCRoleService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
 
        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir()!=null) {
            String invalidPath="abc"+File.separator+"def.properties";
            config.setPropertyFileNameDML(invalidPath);
            fail=false;
            try {
                getSecurityManager().saveRoleService(config);
            } catch (SecurityConfigException ex) {
                assertEquals(SEC_ERR_213, ex.getErrorId());
                assertEquals(invalidPath, ex.getArgs()[0]);
                fail=true;
            }
            assertTrue(fail);
        }

        config.setPropertyFileNameDDL(null);
        config.setCreatingTables(true);
        config.setPropertyFileNameDML(JDBCRoleService.DEFAULT_DML_FILE);
        
        try {
            getSecurityManager().saveRoleService(config);
        } catch (SecurityConfigException ex) {
            assertEquals(SEC_ERR_204, ex.getErrorId());
            assertEquals(0, ex.getArgs().length);
            fail=true;
        }
        assertTrue(fail);


    }

    @Override
    public void testUserGroupConfig() throws IOException {

        super.testUserGroupConfig();
        
        JDBCUserGroupServiceConfig  config = 
                (JDBCUserGroupServiceConfig)getUGConfig("jdbc", JDBCUserGroupService.class,
                getPlainTextPasswordEncoder().getName() ,PasswordValidator.DEFAULT_NAME);

        config.setDriverClassName("a.b.c");
        config.setUserName("user");
        config.setConnectURL("jdbc:connect");
        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);


        JDBCUserGroupServiceConfig  configJNDI = (JDBCUserGroupServiceConfig) 
                getUGConfig("jdbc", JDBCUserGroupService.class, 
                getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME);
        configJNDI.setJndi(true);                        
        configJNDI.setJndiName("jndi:connect");
        configJNDI.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        configJNDI.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
        
        boolean fail;

        
        
        fail=false;
        try {            
            configJNDI.setJndiName("");
            getSecurityManager().saveUserGroupService(configJNDI);                                     
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_210,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        fail=false;
        try {            
            config.setDriverClassName("");
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_200,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setDriverClassName("a.b.c");
        fail=false;
        try {            
            config.setUserName("");
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_201,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setUserName("user");
        fail=false;
        try {            
            config.setConnectURL(null);
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_202,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        config.setConnectURL("jdbc:connect");
        try {            
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_203,ex.getErrorId());
            assertEquals("a.b.c",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }

        config.setDriverClassName("java.lang.String");
        
        config.setPropertyFileNameDDL(null);
        try {
            getSecurityManager().saveUserGroupService(config);
        } catch (SecurityConfigException ex) {
            throw new IOException(ex);
        }
        config.setPropertyFileNameDML(null);
        try {            
            getSecurityManager().saveUserGroupService(config);
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_212,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
        
        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir()!=null) {
            String invalidPath="abc"+File.separator+"def.properties";
            config.setPropertyFileNameDDL(invalidPath);
            fail=false;
            try {
                getSecurityManager().saveUserGroupService(config);
            } catch (SecurityConfigException ex) {
                assertEquals(SEC_ERR_211, ex.getErrorId());
                assertEquals(invalidPath, ex.getArgs()[0]);
                fail=true;
            }
            assertTrue(fail);
        }
 
        config.setPropertyFileNameDDL(JDBCUserGroupService.DEFAULT_DDL_FILE);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
 
        // run only if a temp dir is availbale
        if (new JdbcSecurityConfigValidator(getSecurityManager()).getTempDir()!=null) {
            String invalidPath="abc"+File.separator+"def.properties";
            config.setPropertyFileNameDML(invalidPath);
            fail=false;
            try {
                getSecurityManager().saveUserGroupService(config);
            } catch (SecurityConfigException ex) {
                assertEquals(SEC_ERR_213, ex.getErrorId());
                assertEquals(invalidPath, ex.getArgs()[0]);
                fail=true;
            }
            assertTrue(fail);
        }

        
        config.setPropertyFileNameDDL(null);
        config.setCreatingTables(true);
        config.setPropertyFileNameDML(JDBCUserGroupService.DEFAULT_DML_FILE);
        
        try {
            getSecurityManager().saveUserGroupService(config);
        } catch (SecurityConfigException ex) {
            assertEquals(SEC_ERR_204, ex.getErrorId());
            assertEquals(0, ex.getArgs().length);
            fail=true;
        }
        assertTrue(fail);        
    }

    @Override
    public void testAuthenticationProvider() throws IOException {
        super.testAuthenticationProvider();
        JDBCConnectAuthProviderConfig config = 
                (JDBCConnectAuthProviderConfig) getAuthConfig("jdbcprov", JDBCConnectAuthProvider.class, "default");
        
        config.setConnectURL("jdbc:connect");
        
        boolean fail=false;
        try {            
            config.setDriverClassName("");
            getSecurityManager().saveAuthenticationProvider(config);
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_200,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setDriverClassName("a.b.c");
        fail=false;
        try {            
            getSecurityManager().saveAuthenticationProvider(config);
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_203,ex.getErrorId());
            assertEquals("a.b.c",ex.getArgs()[0]);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        
        fail=false;
        try {            
            config.setConnectURL(null);
            getSecurityManager().saveAuthenticationProvider(config);
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_202,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

    }

}
