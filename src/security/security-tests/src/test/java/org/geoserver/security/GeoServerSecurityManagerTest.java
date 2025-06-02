/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Properties;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
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
        assertTrue(masterPWInfoFileContains(String.valueOf(generatedPW)));
        // dumpPWInfoFile();

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
        assertTrue(masterPWInfoFileContains(String.valueOf(generatedPW)));
        assertFalse(masterPWInfoFileContains(GeoServerUser.ADMIN_USERNAME));
        assertFalse(masterPWInfoFileContains(adminUser));
        assertFalse(masterPWInfoFileContains(noAdminUser));
        // dumpPWInfoFile();

        // valid master password for noadminuser
        props.put(noAdminUser, "validPassword" + ",ROLE_WFS");
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);
        assertTrue(masterPWInfoFileContains(String.valueOf(generatedPW)));

        // password to short  for adminuser
        props.put(adminUser, "abc" + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(8, generatedPW.length);
        assertTrue(masterPWInfoFileContains(String.valueOf(generatedPW)));

        // valid password for user having admin role

        String validPassword = "validPassword";
        props.put(adminUser, validPassword + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, String.valueOf(generatedPW));
        assertFalse(masterPWInfoFileContains(validPassword));
        assertTrue(masterPWInfoFileContains(adminUser));
        // dumpPWInfoFile();

        // valid password for "admin" user
        props.put(GeoServerUser.ADMIN_USERNAME, validPassword + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, String.valueOf(generatedPW));
        assertFalse(masterPWInfoFileContains(validPassword));
        assertTrue(masterPWInfoFileContains(GeoServerUser.ADMIN_USERNAME));
        // dumpPWInfoFile();

        // assert configuration reload works properly
        secMgr.reload();
    }

    @SuppressWarnings("PMD.SystemPrintln")
    void dumpPWInfoFile(File infoFile) throws Exception {

        try (BufferedReader bf = new BufferedReader(new FileReader(infoFile))) {
            String line;
            while ((line = bf.readLine()) != null) {
                System.out.println(line);
            }
        }
    }

    void dumpPWInfoFile() throws Exception {
        dumpPWInfoFile(new File(
                getSecurityManager().get("security").dir(), GeoServerSecurityManager.MASTER_PASSWD_INFO_FILENAME));
    }

    boolean masterPWInfoFileContains(File infoFile, String searchString) throws Exception {

        try (BufferedReader bf = new BufferedReader(new FileReader(infoFile))) {
            String line;
            while ((line = bf.readLine()) != null) {
                if (line.contains(searchString)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean masterPWInfoFileContains(String searchString) throws Exception {
        return masterPWInfoFileContains(
                new File(
                        getSecurityManager().get("security").dir(),
                        GeoServerSecurityManager.MASTER_PASSWD_INFO_FILENAME),
                searchString);
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
