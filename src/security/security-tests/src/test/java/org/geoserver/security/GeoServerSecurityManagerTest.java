/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.security.authentication.TestingAuthenticationToken;

@Category(SystemTest.class)
public class GeoServerSecurityManagerTest extends GeoServerSecurityTestSupport {

    @Test
    public void testAdminRole() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        TestingAuthenticationToken auth =
                new TestingAuthenticationToken("admin", "geoserver", List.of(GeoServerRole.ADMIN_ROLE));
        auth.setAuthenticated(true);
        assertTrue(secMgr.checkAuthenticationForAdminRole(auth));
    }

    @Test
    public void testMasterPasswordForMigration() throws Exception {

        // simulate no user.properties file
        GeoServerSecurityManager secMgr = getSecurityManager();
        char[] generatedPW = secMgr.extractMasterPasswordForMigration(null);
        assertEquals(8, generatedPW.length);

        Properties props = new Properties();
        String adminUser = "user1";
        String noAdminUser = "user2";

        // check all users with default password
        String defaultMasterePassword = String.valueOf(GeoServerSecurityManager.MASTER_PASSWD_DEFAULT);
        props.put(GeoServerUser.ADMIN_USERNAME, defaultMasterePassword + "," + GeoServerRole.ADMIN_ROLE);
        props.put(adminUser, defaultMasterePassword + "," + GeoServerRole.ADMIN_ROLE);
        props.put(noAdminUser, defaultMasterePassword + ",ROLE_WFS");

        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);

        // valid master password for noadminuser
        props.put(noAdminUser, "validPassword" + ",ROLE_WFS");
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);

        // password to short  for adminuser
        props.put(adminUser, "abc" + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);

        // valid password for user having admin role

        String validPassword = "validPassword";
        props.put(adminUser, validPassword + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, String.valueOf(generatedPW));

        // valid password for "admin" user
        props.put(GeoServerUser.ADMIN_USERNAME, validPassword + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, String.valueOf(generatedPW));

        // assert configuration reload works properly
        secMgr.reload();
    }

    @Test
    public void testWebLoginChainSessionCreation() throws Exception {
        // GEOS-6077
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();

        RequestFilterChain chain =
                config.getFilterChain().getRequestChainByName(GeoServerSecurityFilterChain.WEB_LOGIN_CHAIN_NAME);
        assertTrue(chain.isAllowSessionCreation());
    }

    @Test
    public void testReloadClearsCaches() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        // populate caches by loading default services
        secMgr.loadRoleService("default");
        secMgr.loadUserGroupService("default");
        secMgr.loadPasswordValidator("default");

        // verify caches are populated (precondition)
        assertFalse("roleServices cache should not be empty", secMgr.roleServices.isEmpty());
        assertFalse("userGroupServices cache should not be empty", secMgr.userGroupServices.isEmpty());
        assertFalse("passwordValidators cache should not be empty", secMgr.passwordValidators.isEmpty());

        // capture cached instances by identity
        GeoServerRoleService oldRoleService = secMgr.roleServices.get("default");
        GeoServerUserGroupService oldUgService = secMgr.userGroupServices.get("default");
        PasswordValidator oldPwValidator = secMgr.passwordValidators.get("default");

        assertNotNull(oldRoleService);
        assertNotNull(oldUgService);
        assertNotNull(oldPwValidator);

        // reload calls init() which calls clearCaches(), then re-initializes
        secMgr.reload();

        // after reload, loading services again must produce fresh instances
        secMgr.loadRoleService("default");
        secMgr.loadUserGroupService("default");
        secMgr.loadPasswordValidator("default");

        // check the cache maps directly to avoid wrapper interference from the public load methods
        assertNotSame(
                "roleService should be a fresh instance after reload",
                oldRoleService,
                secMgr.roleServices.get("default"));
        assertNotSame(
                "userGroupService should be a fresh instance after reload",
                oldUgService,
                secMgr.userGroupServices.get("default"));
        assertNotSame(
                "passwordValidator should be a fresh instance after reload",
                oldPwValidator,
                secMgr.passwordValidators.get("default"));
    }

    @Test
    public void testGeoServerEnvParametrization() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();
        String oldRoleServiceName = config.getRoleServiceName();

        try {
            if (GeoServerEnvironment.allowEnvParametrization()) {
                System.setProperty("TEST_SYS_PROPERTY", oldRoleServiceName);

                config.setRoleServiceName("${TEST_SYS_PROPERTY}");
                secMgr.saveSecurityConfig(config);

                SecurityManagerConfig config1 = secMgr.loadSecurityConfig();
                assertEquals(config1.getRoleServiceName(), oldRoleServiceName);
            }
        } finally {
            config.setRoleServiceName(oldRoleServiceName);
            secMgr.saveSecurityConfig(config);
            System.clearProperty("TEST_SYS_PROPERTY");
        }
    }
}
