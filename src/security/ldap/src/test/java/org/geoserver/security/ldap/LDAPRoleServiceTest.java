/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.SortedSet;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

public class LDAPRoleServiceTest extends LDAPBaseTest {

    GeoServerRoleService service;

    public void createRoleService(boolean userFilter, Boolean convertToUpperCase, String rolePrefix)
            throws IOException {
        service = new LDAPRoleService();
        if (userFilter) {
            config.setGroupSearchFilter("member={1},dc=example,dc=com");
            config.setUserFilter("uid={0}");
        } else {
            config.setGroupSearchFilter("member=cn={0}");
        }
        if (convertToUpperCase != null) {
            config.setConvertToUpperCase(convertToUpperCase);
        }
        if (rolePrefix != null) {
            config.setRolePrefix(rolePrefix);
        }
        service.initializeFromConfig(config);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    protected void configureAuthentication() {
        config.setUser(
                "uid=admin,ou=People,dc=example,dc=com"); // ("uid=admin,ou=People,dc=example,dc=com");
        config.setPassword("admin");
        config.setBindBeforeGroupSearch(true);
    }

    protected void checkAdminRoles() throws IOException {
        config.setAdminGroup("admin");
        config.setGroupAdminGroup("other");
        createRoleService(false, null, null);

        assertNotNull(service.getAdminRole());
        assertNotNull(service.getGroupAdminRole());

        config.setAdminGroup("dummy1");
        config.setGroupAdminGroup("dummy2");
        createRoleService(false, null, null);

        assertNull(service.getAdminRole());
        assertNull(service.getGroupAdminRole());

        config.setAdminGroup("admin");
        config.setGroupAdminGroup("other");
        createRoleService(false, false, "test_");
        assertEquals("test_admin", service.getAdminRole().toString());
    }

    protected void checkUserNamesForRole(String roleName, int expected, boolean userFilter)
            throws IOException {
        createRoleService(userFilter, null, null);

        SortedSet<String> userNames = service.getUserNamesForRole(new GeoServerRole(roleName));
        assertNotNull(userNames);
        assertEquals(expected, userNames.size());

        createRoleService(userFilter, false, "test_");

        userNames = service.getUserNamesForRole(new GeoServerRole(roleName));
        assertNotNull(userNames);
        assertEquals(expected, userNames.size());
    }

    protected void checkRoleByName() throws IOException {
        createRoleService(false, null, null);

        assertNotNull(service.getRoleByName("admin"));
        assertNull(service.getRoleByName("dummy"));

        createRoleService(false, false, "test_");

        assertNotNull(service.getRoleByName("admin"));
        assertNull(service.getRoleByName("dummy"));
    }

    protected void checkRoleCount() throws IOException {
        createRoleService(false, null, null);

        assertTrue(service.getRoleCount() > 0);

        createRoleService(false, false, "test_");

        assertTrue(service.getRoleCount() > 0);
    }

    protected void checkAllRoles() throws IOException {
        createRoleService(false, null, null);

        SortedSet<GeoServerRole> roles = service.getRoles();
        assertNotNull(roles);
        assertTrue(roles.size() > 0);
        GeoServerRole role = roles.first();
        assertTrue(role.toString().startsWith("ROLE_"));
        assertEquals(role.toString().toUpperCase(), role.toString());

        createRoleService(false, false, "test_");

        roles = service.getRoles();
        assertNotNull(roles);
        assertTrue(roles.size() > 0);
        role = roles.first();
        assertTrue(role.toString().startsWith("test_"));
        assertNotEquals(role.toString().toUpperCase(), role.toString());
    }

    protected void checkUserRoles(String username, boolean userFilter) throws IOException {
        createRoleService(userFilter, null, null);
        SortedSet<GeoServerRole> allRoles = service.getRoles();
        SortedSet<GeoServerRole> roles = service.getRolesForUser(username);
        assertNotNull(roles);
        assertTrue(roles.size() > 0);
        assertTrue(roles.size() < allRoles.size());
        GeoServerRole role = roles.first();
        assertTrue(role.toString().startsWith("ROLE_"));
        assertEquals(role.toString().toUpperCase(), role.toString());

        createRoleService(userFilter, false, "test_");
        allRoles = service.getRoles();
        roles = service.getRolesForUser(username);
        assertNotNull(roles);
        assertTrue(roles.size() > 0);
        assertTrue(roles.size() < allRoles.size());
        role = roles.first();
        assertTrue(role.toString().startsWith("test_"));
        assertNotEquals(role.toString().toUpperCase(), role.toString());
    }

    @Override
    protected void createConfig() {
        config = new LDAPRoleServiceConfig();
    }

    @RunWith(FrameworkRunner.class)
    @CreateLdapServer(
            transports = {@CreateTransport(protocol = "LDAP", address = "localhost")},
            allowAnonymousAccess = true)
    @CreateDS(
            name = "myDS",
            partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)})
    @ApplyLdifFiles({"data.ldif"})
    public static class LDAPRoleServiceLdiffTest extends LDAPRoleServiceTest {

        @Test
        public void testGetRoles() throws Exception {
            getService().setAllowAnonymousAccess(true);

            checkAllRoles();
        }

        @Test
        public void testGetRolesAuthenticated() throws Exception {
            getService().setAllowAnonymousAccess(false);
            configureAuthentication();
            checkAllRoles();
        }

        @Test
        public void testGetRolesCount() throws Exception {
            getService().setAllowAnonymousAccess(true);

            checkRoleCount();
        }

        @Test
        public void testGetRolesCountAuthenticated() throws Exception {
            getService().setAllowAnonymousAccess(true);
            configureAuthentication();
            checkRoleCount();
        }

        @Test
        public void testGetRoleByName() throws Exception {
            getService().setAllowAnonymousAccess(true);

            checkRoleByName();
        }

        @Test
        public void testGetRoleByNameAuthenticated() throws Exception {
            getService().setAllowAnonymousAccess(false);
            configureAuthentication();
            checkRoleByName();
        }

        @Test
        public void testGetAdminRoles() throws Exception {
            getService().setAllowAnonymousAccess(true);

            checkAdminRoles();
        }

        @Test
        public void testGetAdminRolesAuthenticated() throws Exception {
            getService().setAllowAnonymousAccess(false);
            configureAuthentication();
            checkAdminRoles();
        }

        @Test
        public void testGetRolesForUser() throws Exception {
            getService().setAllowAnonymousAccess(true);

            checkUserRoles("admin", false);
        }

        @Test
        public void testGetRolesForUserAuthenticated() throws Exception {
            getService().setAllowAnonymousAccess(false);

            configureAuthentication();
            checkUserRoles("admin", false);
        }

        @Test
        public void testGetUserNamesForRole() throws Exception {
            getService().setAllowAnonymousAccess(true);

            checkUserNamesForRole("admin", 1, false);
            checkUserNamesForRole("other", 2, false);
        }

        /** Tests LDAP Hierarchical roles retrieval for an user. */
        @Test
        public void checkUserHierarchicalRoles() throws IOException {
            config.setUseNestedParentGroups(true);
            config.setNestedGroupSearchFilter("member=cn={0}");
            config.setGroupSearchFilter("member=cn={0}");
            config.setUserFilter("uid={0}");
            service = new LDAPRoleService();
            service.initializeFromConfig(config);
            SortedSet<GeoServerRole> roles = service.getRolesForUser("nestedUser");
            assertNotNull(roles);
            assertEquals(2, roles.size());
            // check parent role ROLE_EXTRA
            assertTrue(roles.stream().anyMatch(r -> "ROLE_EXTRA".equals(r.getAuthority())));
        }
    }

    @RunWith(FrameworkRunner.class)
    @CreateLdapServer(
            transports = {@CreateTransport(protocol = "LDAP", address = "localhost")},
            allowAnonymousAccess = true)
    @CreateDS(
            name = "myDS",
            partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)})
    @ApplyLdifFiles({"data2.ldif"})
    public static class LDAPRoleServiceLdiff2Test extends LDAPRoleServiceTest {

        @Test
        public void testGetRolesForUserUsingUserFilter() throws Exception {

            checkUserRoles("admin", true);
        }

        @Test
        public void testGetRolesForUserAuthenticatedUsingUserFilter() throws Exception {
            getService().setAllowAnonymousAccess(false);

            configureAuthentication();
            checkUserRoles("admin", true);
        }

        @Test
        public void testGetUserNamesForRoleUsingUserFilter() throws Exception {
            getService().setAllowAnonymousAccess(true);
            checkUserNamesForRole("admin", 1, true);
            checkUserNamesForRole("other", 2, true);
        }
    }

    @RunWith(FrameworkRunner.class)
    @CreateLdapServer(
            transports = {@CreateTransport(protocol = "LDAP", address = "localhost")},
            allowAnonymousAccess = true)
    @CreateDS(
            name = "myDS",
            partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)})
    @ApplyLdifFiles({"data4.ldif"})
    public static class LDAPRoleServiceLdiff4Test extends LDAPRoleServiceTest {

        @Test
        public void checkHierarchicalRolesUsers() throws IOException {
            createRoleService(true, null, null);
            config.setUserNameAttribute("uid");
            config.setGroupNameAttribute("cn");
            config.setUseNestedParentGroups(true);
            // ,dc=example,dc=com
            config.setNestedGroupSearchFilter("member={1}");
            config.setGroupSearchFilter("member={1},dc=example,dc=com");
            config.setUserFilter("uid={0}");
            config.setMaxGroupSearchLevel(5);
            service = new LDAPRoleService();
            service.initializeFromConfig(config);
            SortedSet<String> userNames =
                    service.getUserNamesForRole(service.getRoleByName("ROLE_EXTRA"));
            assertNotNull(userNames);
            assertEquals(2, userNames.size());
            // check parent role ROLE_EXTRA
            assertTrue(userNames.stream().anyMatch(u -> "nestedUser".equals(u)));
            // check nested roles
            SortedSet<GeoServerRole> roles = service.getRolesForUser("nestedUser");
            assertEquals(6, roles.size());
        }
    }
}
