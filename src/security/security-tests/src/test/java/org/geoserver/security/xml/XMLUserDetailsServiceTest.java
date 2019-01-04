/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.geoserver.data.test.LiveSystemTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.AbstractUserDetailsServiceTest;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.password.GeoServerMultiplexingPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class XMLUserDetailsServiceTest extends AbstractUserDetailsServiceTest {

    @Override
    protected SystemTestData createTestData() throws Exception {
        return new LiveSystemTestData(new File("./src/test/resources/data_dir/legacy"));
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do nothing here, we want the live test data info and run its migration
    }

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
        getSecurityManager().saveRoleService(gaConfig /*,isNewRoleService(serviceName)*/);

        GeoServerRoleService service = getSecurityManager().loadRoleService(serviceName);
        service.initializeFromConfig(gaConfig);
        return service;
    }

    @Test
    public void testMigration() throws IOException {

        //        GeoserverUserGroupService userService = createUserGroupService(
        //                XMLUserGroupService.DEFAULT_NAME);
        //        GeoserverRoleService roleService = createRoleService(
        //                XMLRoleService.DEFAULT_NAME);
        //        getSecurityManager().setActiveRoleService(roleService);
        //        getSecurityManager().setActiveUserGroupService(userService);
        GeoServerUserGroupService userService =
                getSecurityManager().loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);
        GeoServerRoleService roleService =
                getSecurityManager().loadRoleService(XMLRoleService.DEFAULT_NAME);

        assertEquals(3, userService.getUsers().size());
        assertEquals(3, userService.getUserCount());
        assertEquals(0, userService.getUserGroups().size());
        assertEquals(0, userService.getGroupCount());

        assertEquals(9, roleService.getRoles().size());

        GeoServerUser admin = (GeoServerUser) userService.loadUserByUsername("admin");
        assertNotNull(admin);
        GeoServerMultiplexingPasswordEncoder enc = getEncoder(userService);
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

        GeoServerRole role_admin =
                roleService.getRoleByName(XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE);
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

        assertEquals(2, admin.getAuthorities().size());
        assertTrue(admin.getAuthorities().contains(role_admin));
        assertTrue(admin.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));

        assertEquals(2, wfs.getAuthorities().size());
        assertTrue(wfs.getAuthorities().contains(role_wfs_read));
        assertTrue(wfs.getAuthorities().contains(role_wfs_write));

        assertEquals(1, disabledUser.getAuthorities().size());
        assertTrue(disabledUser.getAuthorities().contains(role_test));

        GeoServerSecurityManager securityManager = getSecurityManager();
        File userfile = new File(securityManager.get("security").dir(), "users.properties");
        assertFalse(userfile.exists());
        File userfileOld = new File(securityManager.get("security").dir(), "users.properties.old");
        assertTrue(userfileOld.exists());

        File roleXSD =
                new File(
                        new File(securityManager.get("security/role").dir(), roleService.getName()),
                        XMLConstants.FILE_RR_SCHEMA);
        assertTrue(roleXSD.exists());

        File userXSD =
                new File(
                        new File(
                                securityManager.get("security/usergroup").dir(),
                                userService.getName()),
                        XMLConstants.FILE_UR_SCHEMA);
        assertTrue(userXSD.exists());

        /* does not work from the command line
         *
        ServiceAccessRuleDAO sdao = GeoServerExtensions.bean(ServiceAccessRuleDAO.class);
        assertTrue(sdao.getRulesAssociatedWithRole(XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE).isEmpty()==false);
        assertTrue(sdao.getRulesAssociatedWithRole(GeoServerRole.ADMIN_ROLE.getAuthority()).isEmpty());

        DataAccessRuleDAO ddao = GeoServerExtensions.bean(DataAccessRuleDAO.class);
        assertTrue(ddao.getRulesAssociatedWithRole(XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE).isEmpty()==false);
        assertTrue(ddao.getRulesAssociatedWithRole(GeoServerRole.ADMIN_ROLE.getAuthority()).isEmpty());

        RESTAccessRuleDAO rdao = GeoServerExtensions.bean(RESTAccessRuleDAO.class);
        List<String> rules = rdao.getRules();

        boolean found = false;
        for (String rule : rules) {
        	if (rule.contains(XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE))
        		found=true;
        	if (rule.contains(GeoServerRole.ADMIN_ROLE.getAuthority()))
        		Assert.fail("Migration of admin role not successful");
        }
        assertTrue(found);
        */
    }
}
