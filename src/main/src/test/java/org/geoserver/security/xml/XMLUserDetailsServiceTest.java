/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.xml;

import java.io.File;
import java.io.IOException;

import org.geoserver.data.test.LiveData;
import org.geoserver.data.test.TestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.AbstractUserDetailsServiceTest;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;

public class XMLUserDetailsServiceTest extends AbstractUserDetailsServiceTest {

    @Override
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {
//        KeyStoreProvider.get().setUserGroupKey(serviceName,
//                RandomPasswordProvider.get().getRandomPassword(32));

        
        XMLUserGroupServiceConfig ugConfig = new XMLUserGroupServiceConfig();                 
        ugConfig.setName(serviceName);
        ugConfig.setClassName(XMLUserGroupService.class.getName());
        ugConfig.setCheckInterval(1000); 
        ugConfig.setFileName(XMLConstants.FILE_UR);        
        ugConfig.setValidating(true);
//        ugConfig.setPasswordEncoderName(GeoserverUserPBEPasswordEncoder.PrototypeName);
        ugConfig.setPasswordEncoderName(getDigestPasswordEncoder().getName());
        ugConfig.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        getSecurityManager().saveUserGroupService(ugConfig);

        GeoServerUserGroupService service = getSecurityManager().loadUserGroupService(serviceName);
        service.initializeFromConfig(ugConfig);
        return service;                
    }

    public GeoServerRoleService createRoleService(String serviceName) throws Exception {
        
        XMLRoleServiceConfig gaConfig = new XMLRoleServiceConfig();                 
        gaConfig.setName(serviceName);
        gaConfig.setClassName(XMLRoleService.class.getName());
        gaConfig.setCheckInterval(1000); 
        gaConfig.setFileName(XMLConstants.FILE_RR);
        gaConfig.setValidating(true);
        gaConfig.setAdminRoleName(GeoServerRole.ADMIN_ROLE.getAuthority());
        getSecurityManager().saveRoleService(gaConfig/*,isNewRoleService(serviceName)*/);

        GeoServerRoleService service = 
            getSecurityManager().loadRoleService(serviceName);
        service.initializeFromConfig(gaConfig);
        return service;
    }
                    

    public void testMigration() throws IOException {
        
//        GeoserverUserGroupService userService = createUserGroupService(
//                XMLUserGroupService.DEFAULT_NAME);
//        GeoserverRoleService roleService = createRoleService(
//                XMLRoleService.DEFAULT_NAME);
//        getSecurityManager().setActiveRoleService(roleService);
//        getSecurityManager().setActiveUserGroupService(userService);
        GeoServerUserGroupService userService = getSecurityManager().loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);
        GeoServerRoleService roleService =getSecurityManager().loadRoleService(XMLRoleService.DEFAULT_NAME);
        
        assertEquals(3,userService.getUsers().size());
        assertEquals(3,userService.getUserCount());
        assertEquals(0,userService.getUserGroups().size());
        assertEquals(0,userService.getGroupCount());
        
        assertEquals(9,roleService.getRoles().size());
        
        GeoServerUser admin = (GeoServerUser) userService.loadUserByUsername("admin");
        assertNotNull(admin);
        GeoServerPasswordEncoder enc= getEncoder(userService);
        assertTrue(enc.isPasswordValid(admin.getPassword(), "gs", null));
        
        assertTrue(admin.isEnabled());
        
        GeoServerUser wfs = (GeoServerUser) userService.loadUserByUsername("wfs");
        assertNotNull(wfs);
        assertTrue(enc.isPasswordValid(wfs.getPassword(), "webFeatureService", null));
        assertTrue(wfs.isEnabled());

        GeoServerUser disabledUser = (GeoServerUser) userService.loadUserByUsername("disabledUser");
        assertNotNull(disabledUser);
        assertTrue(enc.isPasswordValid(disabledUser.getPassword(), "nah", null));
        assertFalse(disabledUser.isEnabled());
        
        GeoServerRole role_admin = roleService.getRoleByName("ROLE_ADMINISTRATOR");
        assertNotNull(role_admin);
        GeoServerRole role_wfs_read = roleService.getRoleByName("ROLE_WFS_READ");
        assertNotNull(role_wfs_read);
        GeoServerRole role_wfs_write = roleService.getRoleByName("ROLE_WFS_WRITE");
        assertNotNull(role_wfs_write);
        GeoServerRole role_test = roleService.getRoleByName("ROLE_TEST");
        assertNotNull(role_test);
        assertNotNull(roleService.getRoleByName("NO_ONE"));
        assertNotNull(roleService.getRoleByName("TRUSTED_ROLE"));
        assertNotNull(roleService.getRoleByName("ROLE_SERVICE_1"));
        assertNotNull(roleService.getRoleByName("ROLE_SERVICE_2"));

        

        assertEquals(1,admin.getAuthorities().size());
        assertTrue(admin.getAuthorities().contains(role_admin));
        
        assertEquals(2,wfs.getAuthorities().size());
        assertTrue(wfs.getAuthorities().contains(role_wfs_read));
        assertTrue(wfs.getAuthorities().contains(role_wfs_write));

        assertEquals(1,disabledUser.getAuthorities().size());
        assertTrue(disabledUser.getAuthorities().contains(role_test));
        
        GeoServerSecurityManager securityManager = getSecurityManager();
        File userfile = new File(securityManager.getSecurityRoot(),"users.properties");
        assertFalse(userfile.exists());
        File userfileOld = new File(securityManager.getSecurityRoot(),"users.properties.old");
        assertTrue(userfileOld.exists());

        File roleXSD = new File(new File(securityManager.getRoleRoot(), roleService.getName()),
            XMLConstants.FILE_RR_SCHEMA);
        assertTrue(roleXSD.exists());

        File userXSD = new File (new File(securityManager.getUserGroupRoot(), userService.getName()), 
            XMLConstants.FILE_UR_SCHEMA);
        assertTrue(userXSD.exists());


    }

    @Override
    protected TestData buildTestData() throws Exception {
            
        File data = new File("./src/test/resources/migratedatadir");        
        return new LiveData(data);
    }
}
