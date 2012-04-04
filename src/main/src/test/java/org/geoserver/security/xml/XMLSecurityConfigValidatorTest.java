package org.geoserver.security.xml;

import static org.geoserver.security.xml.XMLSecurityConfigException.*;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidatorTest;
import org.geotools.util.logging.Logging;

public class XMLSecurityConfigValidatorTest extends SecurityConfigValidatorTest {

    
    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");
    
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
    }
        
    protected SecurityUserGroupServiceConfig getUGConfig(String name, Class<?> aClass,
            String encoder, String policyName, String fileName) {
        XMLUserGroupServiceConfig config = new XMLUserGroupServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setPasswordEncoderName(encoder);
        config.setPasswordPolicyName(policyName);
        config.setCheckInterval(0);
        config.setFileName(fileName);
        return config;
    }
    
    protected SecurityRoleServiceConfig getRoleConfig(String name, Class<?> aClass,String adminRole,String fileName) {
        XMLRoleServiceConfig config = new XMLRoleServiceConfig();
        config.setName(name);
        config.setClassName(aClass.getName());
        config.setAdminRoleName(adminRole);
        config.setCheckInterval(0);
        config.setFileName(fileName);
        return config;
    }

    public void testRoleConfig() throws IOException{
        
        super.testRoleConfig();
        
        XMLRoleServiceConfig  config = 
                (XMLRoleServiceConfig )getRoleConfig(XMLRoleService.DEFAULT_NAME, XMLRoleService.class, 
                GeoServerRole.ADMIN_ROLE.getAuthority(),XMLConstants.FILE_RR);
        boolean fail;

        
        fail=false;
        try {
            config.setName("default2");
            config.setCheckInterval(-1l);
            getSecurityManager().saveRoleService(config);                                     
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_100,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);
        

                
        fail=false;
        try {
            config.setCheckInterval(999l);
            getSecurityManager().saveRoleService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_100,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setCheckInterval(0);
        
        XMLRoleServiceConfig xmlConfig = (XMLRoleServiceConfig) 
                getRoleConfig("test1",XMLRoleService.class,GeoServerRole.ADMIN_ROLE.getAuthority(),"test1.xml");
        
        try {
            getSecurityManager().saveRoleService(xmlConfig);
            getSecurityManager().removeRoleService(xmlConfig);
        } catch (SecurityConfigException ex) {
            Assert.fail("Should work");
        }
        
        fail=false;
        xmlConfig = (XMLRoleServiceConfig) 
                getRoleConfig("test2",XMLRoleService.class,GeoServerRole.ADMIN_ROLE.getAuthority(),"test2.xml");
        try {
            getSecurityManager().saveRoleService(xmlConfig);
            GeoServerRoleStore store = getSecurityManager().loadRoleService("test2").createStore();
            store.addRole(GeoServerRole.ADMIN_ROLE);
            store.store();
            getSecurityManager().removeRoleService(xmlConfig);
        } catch (SecurityConfigException ex) {
            assertEquals(SEC_ERR_102, ex.getErrorId());
            assertEquals("test2", ex.getArgs()[0]);
            fail=true;
        }
        assertTrue(fail);

        xmlConfig = (XMLRoleServiceConfig) 
                getRoleConfig("test3",XMLRoleService.class,GeoServerRole.ADMIN_ROLE.getAuthority(),                        
                        new File(getSecurityManager().getRoleRoot(),"test3.xml").getAbsolutePath());
        try {
            getSecurityManager().saveRoleService(xmlConfig);
            GeoServerRoleStore store = getSecurityManager().loadRoleService("test3").createStore();
            store.addRole(GeoServerRole.ADMIN_ROLE);
            store.store();
            getSecurityManager().removeRoleService(xmlConfig);
        } catch (SecurityConfigException ex) {
            Assert.fail("Should work");
        }

        // run only if a temp dir is availbale
        if (new XMLSecurityConfigValidator(getSecurityManager()).getTempDir()!=null) {
            String invalidPath="abc"+File.separator+"def.xml";
            xmlConfig = (XMLRoleServiceConfig) 
                    getRoleConfig("test4",XMLRoleService.class,GeoServerRole.ADMIN_ROLE.getAuthority(),                        
                            invalidPath);
            
            fail=false;
            try {
                getSecurityManager().saveRoleService(xmlConfig);
            } catch (SecurityConfigException ex) {
                assertEquals(SEC_ERR_101, ex.getErrorId());
                assertEquals(invalidPath, ex.getArgs()[0]);
                fail=true;
            }
            assertTrue(fail);
        }
        /////////////// test modify
        xmlConfig = (XMLRoleServiceConfig)
                getRoleConfig("test4",XMLRoleService.class,GeoServerRole.ADMIN_ROLE.getAuthority(),                        
                        "testModify.xml");

        try {
            getSecurityManager().saveRoleService(xmlConfig);
            xmlConfig.setValidating(true);
            getSecurityManager().saveRoleService(xmlConfig);
        } catch (SecurityConfigException ex) {
            Assert.fail("Should work");
        }

        fail=false;
        try {
            xmlConfig.setFileName("xyz.xml");
            getSecurityManager().saveRoleService(xmlConfig);
        } catch (SecurityConfigException ex) {
            assertEquals(SEC_ERR_105, ex.getErrorId());
            assertEquals("testModify.xml", ex.getArgs()[0]);
            assertEquals("xyz.xml", ex.getArgs()[1]);
            fail=true;
        }
        assertTrue(fail);

                
    }

    
    public void testUserGroupConfig() throws IOException{

        super.testUserGroupConfig();
        XMLUserGroupServiceConfig config = (XMLUserGroupServiceConfig) 
                getUGConfig(XMLUserGroupService.DEFAULT_NAME, XMLUserGroupService.class, 
                getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME,XMLConstants.FILE_UR);
        boolean fail;
        
        fail=false;
        try {
            config.setName("default2");
            config.setCheckInterval(-1l);
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_100,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        fail=false;
        try {
            config.setCheckInterval(999l);
            getSecurityManager().saveUserGroupService(config);                         
        } catch (SecurityConfigException ex) {
            assertEquals( SEC_ERR_100,ex.getErrorId());
            assertEquals(0,ex.getArgs().length);
            LOGGER.info(ex.getMessage());
            fail=true;
        }
        assertTrue(fail);

        config.setCheckInterval(0);

        XMLUserGroupServiceConfig xmlConfig = (XMLUserGroupServiceConfig) 
                getUGConfig("test1", XMLUserGroupService.class, 
                getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME,"test1.xml");

        GeoServerUserGroup group=new GeoServerUserGroup("testgroup");
        
        try {
            getSecurityManager().saveUserGroupService(xmlConfig);
            getSecurityManager().removeUserGroupService(xmlConfig);
        } catch (SecurityConfigException ex) {
            Assert.fail("Should work");
        }
        
        
        
        fail=false;
        xmlConfig = (XMLUserGroupServiceConfig) 
                getUGConfig("test2", XMLUserGroupService.class, 
                getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME,"test2.xml");
        try {
            getSecurityManager().saveUserGroupService(xmlConfig);
            GeoServerUserGroupStore store = getSecurityManager().loadUserGroupService("test2").createStore();
            store.addGroup(group);
            store.store();
            getSecurityManager().removeUserGroupService(xmlConfig);
        } catch (SecurityConfigException ex) {
            assertEquals(SEC_ERR_103, ex.getErrorId());
            assertEquals("test2", ex.getArgs()[0]);
            fail=true;
        }
        assertTrue(fail);

        xmlConfig = (XMLUserGroupServiceConfig) 
                getUGConfig("test3", XMLUserGroupService.class, 
                getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME,
                new File(getSecurityManager().getUserGroupRoot(),"test3.xml").getAbsolutePath());

        try {
            getSecurityManager().saveUserGroupService(xmlConfig);
            GeoServerUserGroupStore store = getSecurityManager().loadUserGroupService("test3").createStore();
            store.addGroup(group);
            store.store();
            getSecurityManager().removeUserGroupService(xmlConfig);
        } catch (SecurityConfigException ex) {
            Assert.fail("Should work");
        }

        // run only if a temp dir is availbale
        if (new XMLSecurityConfigValidator(getSecurityManager()).getTempDir()!=null) {
            String invalidPath="abc"+File.separator+"def.xml";
            xmlConfig = (XMLUserGroupServiceConfig) 
                    getUGConfig("test4", XMLUserGroupService.class, 
                    getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME,
                    invalidPath);
            
            fail=false;
            try {
                getSecurityManager().saveUserGroupService(xmlConfig);
            } catch (SecurityConfigException ex) {
                assertEquals(SEC_ERR_101, ex.getErrorId());
                assertEquals(invalidPath, ex.getArgs()[0]);
                fail=true;
            }
            assertTrue(fail);
        }
        
        
        xmlConfig = (XMLUserGroupServiceConfig) 
                getUGConfig("test5", XMLUserGroupService.class, 
                getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME,
                "abc.xml");
        try {
            getSecurityManager().saveUserGroupService(xmlConfig);
        } catch (SecurityConfigException ex) {
            Assert.fail("Should work");
        }
        

        fail=false;
        try {
            xmlConfig.setFileName("");
            getSecurityManager().saveUserGroupService(xmlConfig);
        } catch (SecurityConfigException ex) {
            assertEquals(SEC_ERR_104, ex.getErrorId());
            assertEquals(0, ex.getArgs().length);
            fail=true;
        }
        assertTrue(fail);

        /////////////// test modify
        xmlConfig = (XMLUserGroupServiceConfig) 
                getUGConfig("testModify", XMLUserGroupService.class, 
                getPlainTextPasswordEncoder().getName(),PasswordValidator.DEFAULT_NAME,"testModify.xml");
        try {
            getSecurityManager().saveUserGroupService(xmlConfig);
            xmlConfig.setValidating(true);
            getSecurityManager().saveUserGroupService(xmlConfig);
        } catch (SecurityConfigException ex) {
            Assert.fail("Should work");
        }

        fail=false;
        try {
            xmlConfig.setFileName("xyz.xml");
            getSecurityManager().saveUserGroupService(xmlConfig);
        } catch (SecurityConfigException ex) {
            assertEquals(SEC_ERR_105, ex.getErrorId());
            assertEquals("testModify.xml", ex.getArgs()[0]);
            assertEquals("xyz.xml", ex.getArgs()[1]);
            fail=true;
        }
        assertTrue(fail);
    }

    @Override
    public void testAuthenticationProvider() throws IOException {
        super.testAuthenticationProvider();
        
        SecurityAuthProviderConfig config = getAuthConfig("default2", 
                UsernamePasswordAuthenticationProvider.class, null);
        
        boolean fail=false;
        try {
            getSecurityManager().saveAuthenticationProvider(config/*, false*/);
        } catch (SecurityConfigException ex) {
            assertEquals(SEC_ERR_106, ex.getErrorId());
            assertEquals(0, ex.getArgs().length);
            fail=true;
        }
        assertTrue(fail);

    }

}
