package org.geoserver.security;

import java.io.IOException;
import java.util.Collections;

import org.geoserver.security.impl.AbstractSecurityServiceTest;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.GroupAdminProperty;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLRoleServiceConfig;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.security.xml.XMLUserGroupServiceConfig;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class GroupAdminServiceTest extends AbstractSecurityServiceTest {

    protected GeoServerUserGroupStore ugStore;
    protected GeoServerRoleStore roleStore;

    GeoServerUser bob, alice;
    GeoServerUserGroup users, admins;

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        //set up the services
        GeoServerUserGroupService ugService = createUserGroupService("gaugs");

        GeoServerRoleService roleService = createRoleService("gars");
        getSecurityManager().setActiveRoleService(roleService);

        //add the users
        ugStore = ugService.createStore();

        bob = ugStore.createUserObject("bob", "foobar", true);
        GroupAdminProperty.set(bob.getProperties(), new String[]{"users"});
        ugStore.addUser(bob);

        alice = ugStore.createUserObject("alice", "foobar", true);
        ugStore.addUser(alice);

        users = ugStore.createGroupObject("users", true);
        ugStore.addGroup(users);

        admins = ugStore.createGroupObject("admins", true);
        ugStore.addGroup(admins);

        ugStore.store();

        //grant bob group admin privilege
        roleStore = roleService.createStore();
        roleStore.addRole(GeoServerRole.ADMIN_ROLE);
        roleStore.addRole(GeoServerRole.GROUP_ADMIN_ROLE);
        
        roleStore.associateRoleToUser(roleStore.getGroupAdminRole(), bob.getUsername());
        roleStore.store();
    }

    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        clearAuth();
    }

    @Override
    public GeoServerRoleService createRoleService(String name) throws Exception {
        XMLRoleServiceConfig config = new XMLRoleServiceConfig();
        config.setName(name);
        config.setClassName(XMLRoleService.class.getName());
        config.setCheckInterval(1000);   
        config.setFileName("roles.xml");
        getSecurityManager().saveRoleService(config);
        return getSecurityManager().loadRoleService(config.getName());
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String name)
            throws Exception {
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
        Authentication auth = new UsernamePasswordAuthenticationToken(
            bob, bob.getPassword(), Collections.singletonList(GeoServerRole.GROUP_ADMIN_ROLE));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    void clearAuth() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    public void testWrapRoleService() throws Exception {
        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        assertFalse(roleService instanceof GroupAdminRoleService);

        setAuth();
        roleService = getSecurityManager().getActiveRoleService();
        assertTrue(roleService instanceof GroupAdminRoleService);
    }

    public void testWrapUserGroupService() throws Exception {
        GeoServerUserGroupService ugService = 
                getSecurityManager().loadUserGroupService(ugStore.getName());
        assertFalse(ugService instanceof GroupAdminUserGroupService);

        setAuth();
        ugService = getSecurityManager().loadUserGroupService(ugStore.getName());
        assertTrue(ugService instanceof GroupAdminUserGroupService);
    }

    public void testHideAdminRole() throws Exception {
        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        assertTrue(roleService.getRoles().contains(GeoServerRole.ADMIN_ROLE));
        assertNotNull(roleService.getAdminRole());
        assertNotNull(roleService.getRoleByName(GeoServerRole.ADMIN_ROLE.getAuthority()));
        
        setAuth();
        roleService = getSecurityManager().getActiveRoleService();
        assertFalse(roleService.getRoles().contains(GeoServerRole.ADMIN_ROLE));
        assertNull(roleService.getAdminRole());
        assertNull(roleService.getRoleByName(GeoServerRole.ADMIN_ROLE.getAuthority()));
    }

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

    public void testRoleServiceReadOnly() throws Exception {
        setAuth();
        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        assertFalse(roleService.canCreateStore());
        assertNull(roleService.createStore());
    }

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
        assertEquals(1, ugStore.getGroupsForUser(bill).size());
        assertTrue(ugStore.getGroupsForUser(bill).contains(users));
        assertFalse(ugStore.getGroupsForUser(bill).contains(admins));
    }

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
        }
        catch(IOException e) {};


    }
}
