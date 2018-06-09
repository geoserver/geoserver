/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.GroupAdminProperty;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLRoleServiceConfig;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.security.xml.XMLUserGroupServiceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class GroupAdminServiceTest extends AbstractSecurityServiceTest {

    protected GeoServerUserGroupStore ugStore;
    protected GeoServerRoleStore roleStore;

    GeoServerUser bob, alice;
    GeoServerUserGroup users, admins;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // set up the services
        GeoServerUserGroupService ugService = createUserGroupService("gaugs");

        GeoServerRoleService roleService = createRoleService("gars");
        getSecurityManager().setActiveRoleService(roleService);

        // add the users
        GeoServerUserGroupStore ugStore = createStore(ugService);

        GeoServerUser bob = ugStore.createUserObject("bob", "foobar", true);
        GroupAdminProperty.set(bob.getProperties(), new String[] {"users"});
        ugStore.addUser(bob);

        GeoServerUser alice = ugStore.createUserObject("alice", "foobar", true);
        ugStore.addUser(alice);

        GeoServerUserGroup users = ugStore.createGroupObject("users", true);
        ugStore.addGroup(users);

        GeoServerUserGroup admins = ugStore.createGroupObject("admins", true);
        ugStore.addGroup(admins);

        ugStore.store();

        // grant bob group admin privilege
        GeoServerRole groupAdminRole = null;
        GeoServerRoleStore roleStore = createStore(roleService);
        roleStore.addRole(roleStore.createRoleObject("adminRole"));
        roleStore.addRole(groupAdminRole = roleStore.createRoleObject("groupAdminRole"));

        roleStore.associateRoleToUser(groupAdminRole, bob.getUsername());
        roleStore.store();
    }

    @Before
    public void init() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        ugStore = secMgr.loadUserGroupService("gaugs").createStore();
        roleStore = secMgr.loadRoleService("gars").createStore();

        bob = ugStore.getUserByUsername("bob");
        alice = ugStore.getUserByUsername("alice");
        users = ugStore.getGroupByGroupname("users");
        admins = ugStore.getGroupByGroupname("admins");
    }

    @Before
    public void removeBill() throws Exception {
        GeoServerUserGroupStore ugStore =
                getSecurityManager().loadUserGroupService("gaugs").createStore();
        GeoServerUser bill = ugStore.getUserByUsername("bill");
        if (bill != null) {
            ugStore.removeUser(bill);
            ugStore.store();
        } else {
            ugStore.load();
        }
    }

    @After
    public void clearAuth() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    public GeoServerRoleService createRoleService(String name) throws Exception {
        XMLRoleServiceConfig config = new XMLRoleServiceConfig();
        config.setName(name);
        config.setAdminRoleName("adminRole");
        config.setGroupAdminRoleName("groupAdminRole");
        config.setClassName(XMLRoleService.class.getName());
        config.setCheckInterval(1000);
        config.setFileName("roles.xml");
        getSecurityManager().saveRoleService(config);
        return getSecurityManager().loadRoleService(config.getName());
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String name) throws Exception {
        XMLUserGroupServiceConfig config = new XMLUserGroupServiceConfig();
        config.setName(name);
        config.setClassName(XMLUserGroupService.class.getName());
        config.setFileName("users.xml");
        config.setCheckInterval(1000);
        config.setPasswordEncoderName(getDigestPasswordEncoder().getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);

        getSecurityManager().saveUserGroupService(config);

        return getSecurityManager().loadUserGroupService(name);
    }

    void setAuth() {
        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        bob,
                        bob.getPassword(),
                        Collections.singletonList(GeoServerRole.GROUP_ADMIN_ROLE));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    public void testWrapRoleService() throws Exception {
        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        assertFalse(roleService instanceof GroupAdminRoleService);

        setAuth();
        roleService = getSecurityManager().getActiveRoleService();
        assertTrue(roleService instanceof GroupAdminRoleService);
    }

    @Test
    public void testWrapUserGroupService() throws Exception {
        GeoServerUserGroupService ugService =
                getSecurityManager().loadUserGroupService(ugStore.getName());
        assertFalse(ugService instanceof GroupAdminUserGroupService);

        setAuth();
        ugService = getSecurityManager().loadUserGroupService(ugStore.getName());
        assertTrue(ugService instanceof GroupAdminUserGroupService);
    }

    @Test
    public void testHideAdminRole() throws Exception {
        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        GeoServerRole adminRole = roleService.createRoleObject("adminRole");
        assertTrue(roleService.getRoles().contains(adminRole));
        assertNotNull(roleService.getAdminRole());
        assertNotNull(roleService.getRoleByName("adminRole"));

        setAuth();
        roleService = getSecurityManager().getActiveRoleService();
        assertFalse(roleService.getRoles().contains(adminRole));
        assertNull(roleService.getAdminRole());
        assertNull(roleService.getRoleByName("adminRole"));
    }

    @Test
    public void testHideGroups() throws Exception {
        GeoServerUserGroupService ugService =
                getSecurityManager().loadUserGroupService(ugStore.getName());
        assertTrue(ugService.getUserGroups().contains(users));
        assertNotNull(ugService.getGroupByGroupname("users"));
        assertTrue(ugService.getUserGroups().contains(admins));
        assertNotNull(ugService.getGroupByGroupname("admins"));

        setAuth();
        ugService = getSecurityManager().loadUserGroupService(ugStore.getName());
        assertTrue(ugService.getUserGroups().contains(users));
        assertNotNull(ugService.getGroupByGroupname("users"));
        assertFalse(ugService.getUserGroups().contains(admins));
        assertNull(ugService.getGroupByGroupname("admins"));
    }

    @Test
    public void testRoleServiceReadOnly() throws Exception {
        setAuth();
        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        assertFalse(roleService.canCreateStore());
        assertNull(roleService.createStore());
    }

    @Test
    public void testCreateNewUser() throws Exception {
        setAuth();

        GeoServerUserGroupService ugService =
                getSecurityManager().loadUserGroupService(ugStore.getName());
        GeoServerUserGroupStore ugStore = ugService.createStore();

        GeoServerUser bill = ugStore.createUserObject("bill", "foobar", true);
        ugStore.addUser(bill);
        ugStore.store();

        assertNotNull(ugService.getUserByUsername("bill"));
    }

    @Test
    public void testAssignUserToGroup() throws Exception {
        testCreateNewUser();

        GeoServerUserGroupService ugService =
                getSecurityManager().loadUserGroupService(ugStore.getName());
        GeoServerUserGroupStore ugStore = ugService.createStore();

        GeoServerUser bill = ugStore.getUserByUsername("bill");
        ugStore.associateUserToGroup(bill, users);
        ugStore.store();

        assertEquals(1, ugStore.getGroupsForUser(bill).size());
        assertTrue(ugStore.getGroupsForUser(bill).contains(users));

        ugStore.associateUserToGroup(bill, admins);
        ugStore.store();
        assertEquals(1, ugStore.getGroupsForUser(bill).size());
        assertTrue(ugStore.getGroupsForUser(bill).contains(users));
        assertFalse(ugStore.getGroupsForUser(bill).contains(admins));
    }

    @Test
    public void testRemoveUserInGroup() throws Exception {
        testAssignUserToGroup();

        GeoServerUserGroupService ugService =
                getSecurityManager().loadUserGroupService(ugStore.getName());
        GeoServerUserGroupStore ugStore = ugService.createStore();
        GeoServerUser bill = ugStore.getUserByUsername("bill");

        ugStore.removeUser(bill);
        ugStore.store();

        assertNull(ugStore.getUserByUsername("bill"));
    }

    @Test
    public void testRemoveUserNotInGroup() throws Exception {
        GeoServerUserGroupService ugService =
                getSecurityManager().loadUserGroupService(ugStore.getName());
        GeoServerUserGroupStore ugStore = ugService.createStore();

        GeoServerUser sally = ugStore.createUserObject("sally", "foobar", true);
        ugStore.addUser(sally);
        ugStore.associateUserToGroup(sally, admins);
        ugStore.store();

        setAuth();
        ugService = getSecurityManager().loadUserGroupService(ugStore.getName());
        ugStore = ugService.createStore();
        try {
            ugStore.removeUser(sally);
            fail();
        } catch (IOException e) {
            ugStore.load();
        }
        ;
    }
}
