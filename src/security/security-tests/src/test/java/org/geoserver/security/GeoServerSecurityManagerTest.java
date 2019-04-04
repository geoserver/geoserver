/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Category(SystemTest.class)
public class GeoServerSecurityManagerTest extends GeoServerSecurityTestSupport {

    @Test
    public void testAdminRole() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        TestingAuthenticationToken auth =
                new TestingAuthenticationToken(
                        "admin", "geoserver", (List) Arrays.asList(GeoServerRole.ADMIN_ROLE));
        auth.setAuthenticated(true);
        assertTrue(secMgr.checkAuthenticationForAdminRole(auth));
    }

    @Test
    public void testMasterPasswordForMigration() throws Exception {

        // simulate no user.properties file
        GeoServerSecurityManager secMgr = getSecurityManager();
        char[] generatedPW = secMgr.extractMasterPasswordForMigration(null);
        assertTrue(generatedPW.length == 8);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));
        // dumpPWInfoFile();

        Properties props = new Properties();
        String adminUser = "user1";
        String noAdminUser = "user2";

        // check all users with default password
        String defaultMasterePassword = new String(GeoServerSecurityManager.MASTER_PASSWD_DEFAULT);
        props.put(
                GeoServerUser.ADMIN_USERNAME,
                defaultMasterePassword + "," + GeoServerRole.ADMIN_ROLE);
        props.put(adminUser, defaultMasterePassword + "," + GeoServerRole.ADMIN_ROLE);
        props.put(noAdminUser, defaultMasterePassword + ",ROLE_WFS");

        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertTrue(generatedPW.length == 8);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));
        assertFalse(masterPWInfoFileContains(GeoServerUser.ADMIN_USERNAME));
        assertFalse(masterPWInfoFileContains(adminUser));
        assertFalse(masterPWInfoFileContains(noAdminUser));
        // dumpPWInfoFile();

        // valid master password for noadminuser
        props.put(noAdminUser, "validPassword" + ",ROLE_WFS");
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertTrue(generatedPW.length == 8);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));

        // password to short  for adminuser
        props.put(adminUser, "abc" + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertTrue(generatedPW.length == 8);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));

        // valid password for user having admin role

        String validPassword = "validPassword";
        props.put(adminUser, validPassword + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, new String(generatedPW));
        assertFalse(masterPWInfoFileContains(validPassword));
        assertTrue(masterPWInfoFileContains(adminUser));
        // dumpPWInfoFile();

        // valid password for "admin" user
        props.put(GeoServerUser.ADMIN_USERNAME, validPassword + "," + GeoServerRole.ADMIN_ROLE);
        generatedPW = secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, new String(generatedPW));
        assertFalse(masterPWInfoFileContains(validPassword));
        assertTrue(masterPWInfoFileContains(GeoServerUser.ADMIN_USERNAME));
        // dumpPWInfoFile();

        // assert configuration reload works properly
        secMgr.reload();
    }

    @Test
    public void testMasterPasswordDump() throws Exception {

        GeoServerSecurityManager secMgr = getSecurityManager();
        File f = File.createTempFile("masterpw", "info");
        f.delete();
        try {
            assertFalse(secMgr.dumpMasterPassword(Files.asResource(f)));

            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken(
                            "admin", "geoserver", (List) Arrays.asList(GeoServerRole.ADMIN_ROLE));
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertTrue(secMgr.dumpMasterPassword(Files.asResource(f)));
            dumpPWInfoFile(f);
            assertTrue(masterPWInfoFileContains(f, new String(secMgr.getMasterPassword())));
        } finally {
            f.delete();
        }
    }

    @Test
    public void testMasterPasswordDumpNotAuthorized() throws Exception {

        GeoServerSecurityManager secMgr = getSecurityManager();
        File f = File.createTempFile("masterpw", "info");
        try {
            assertFalse(secMgr.dumpMasterPassword(Files.asResource(f)));

            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken(
                            "admin", "geoserver", (List) Arrays.asList(GeoServerRole.ADMIN_ROLE));
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertFalse(secMgr.dumpMasterPassword(Files.asResource(f)));
        } finally {
            f.delete();
        }
    }

    @Test
    public void testMasterPasswordDumpNotOverwrite() throws Exception {

        GeoServerSecurityManager secMgr = getSecurityManager();
        File f = File.createTempFile("masterpw", "info");
        try (FileOutputStream os = new FileOutputStream(f)) {
            os.write("This should not be overwritten!".getBytes(StandardCharsets.UTF_8));
        }
        try {
            assertFalse(secMgr.dumpMasterPassword(Files.asResource(f)));

            TestingAuthenticationToken auth =
                    new TestingAuthenticationToken(
                            "admin", "geoserver", (List) Arrays.asList(GeoServerRole.ADMIN_ROLE));
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertFalse(secMgr.dumpMasterPassword(Files.asResource(f)));
            dumpPWInfoFile(f);
            assertTrue(masterPWInfoFileContains(f, "This should not be overwritten!"));
            assertFalse(masterPWInfoFileContains(f, new String(secMgr.getMasterPassword())));
        } finally {
            f.delete();
        }
    }

    void dumpPWInfoFile(File infoFile) throws Exception {

        BufferedReader bf = new BufferedReader(new FileReader(infoFile));
        String line;
        while ((line = bf.readLine()) != null) {
            System.out.println(line);
        }
        bf.close();
    }

    void dumpPWInfoFile() throws Exception {
        dumpPWInfoFile(
                new File(
                        getSecurityManager().get("security").dir(),
                        GeoServerSecurityManager.MASTER_PASSWD_INFO_FILENAME));
    }

    boolean masterPWInfoFileContains(File infoFile, String searchString) throws Exception {

        BufferedReader bf = new BufferedReader(new FileReader(infoFile));
        String line;
        while ((line = bf.readLine()) != null) {
            if (line.indexOf(searchString) != -1) {
                bf.close();
                return true;
            }
        }
        bf.close();
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
                config.getFilterChain()
                        .getRequestChainByName(GeoServerSecurityFilterChain.WEB_LOGIN_CHAIN_NAME);
        assertTrue(chain.isAllowSessionCreation());
    }

    @Test
    public void testGeoServerEnvParametrization() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();
        String oldRoleServiceName = config.getRoleServiceName();

        try {
            if (GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION) {
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
