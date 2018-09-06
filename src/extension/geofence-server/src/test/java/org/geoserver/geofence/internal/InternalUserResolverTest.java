/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AbstractSecurityServiceTest;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.MemoryRoleService;
import org.junit.Before;
import org.junit.Test;

/**
 * *
 *
 * @author Niels Charlier
 */
public class InternalUserResolverTest extends AbstractSecurityServiceTest {

    protected GeoServerRoleService service;

    protected GeoServerRoleStore store;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        addUser("pippo", "clown", null, Arrays.asList("CIRCUS", "KLINIEK", "ZEVER"));
        addUser("jantje", "jantje", null, Arrays.asList("MOPJES", "ZEVER"));

        GeoServerSecurityManager secMgr = getSecurityManager();
        GeoServerUserGroupService userGroupService = secMgr.loadUserGroupService("default");
        GeoServerRoleService roleService = secMgr.loadRoleService("default");

        GeoServerUserGroupStore userGroupStore = userGroupService.createStore();
        GeoServerRoleStore rolesStore = roleService.createStore();

        GeoServerUser roleUserTest =
                userGroupService.createUserObject("role_user_test", "role_user_test", true);
        userGroupStore.addUser(roleUserTest);

        GeoServerRole roleTest = rolesStore.createRoleObject("ROLE_TEST");
        rolesStore.addRole(roleTest);
        rolesStore.associateRoleToUser(roleTest, "role_user_test");

        GeoServerRole roleTest2 = rolesStore.createRoleObject("ROLE_TEST_2");
        rolesStore.addRole(roleTest2);

        GeoServerUserGroup roleGroup = userGroupService.createGroupObject("ROLE_GROUP", true);
        userGroupStore.addGroup(roleGroup);

        userGroupStore.associateUserToGroup(roleUserTest, roleGroup);

        rolesStore.associateRoleToGroup(roleTest2, "ROLE_GROUP");

        userGroupStore.store();
        rolesStore.store();
    }

    @Override
    public GeoServerRoleService createRoleService(String name) throws Exception {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName(name);
        config.setAdminRoleName("adminRole");
        config.setGroupAdminRoleName("groupAdminRole");
        config.setClassName(MemoryRoleService.class.getName());

        service = new MemoryRoleService();
        service.initializeFromConfig(config);
        service.setSecurityManager(getSecurityManager());
        store = service.createStore();
        addTestUser("user1", Arrays.asList("adminRole", "groupAdminRole"), service, store);
        addTestUser("user2", Arrays.asList("adminRole"), service, store);
        addTestUser("user3", Arrays.asList("role1"), service, store);
        getSecurityManager().saveRoleService(config);
        return service;
    }

    @Before
    public void setDefaultUserService() throws Exception {
        service = createRoleService("test");
        service = getSecurityManager().loadRoleService("test");
        System.setProperty(InternalUserResolver.DEFAULT_USER_GROUP_SERVICE_KEY, "test");
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        /** Dispose Services */
        this.service = null;
        this.testData = new SystemTestData();

        try {
            if (System.getProperty("IS_GEOFENCE_AVAILABLE") != null) {
                System.clearProperty("IS_GEOFENCE_AVAILABLE");
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Could not remove System ENV variable {IS_GEOFENCE_AVAILABLE}",
                    e);
        }
    }

    protected void addTestUser(
            String username,
            List<String> roles,
            GeoServerRoleService service,
            GeoServerRoleStore store)
            throws Exception {

        if (roles != null && !roles.isEmpty()) {
            for (String roleName : roles) {
                GeoServerRole role = store.getRoleByName(roleName);
                if (role == null) {
                    role = service.createRoleObject(roleName);
                    store.addRole(role);
                }

                store.associateRoleToUser(role, username);
            }
        }
        store.store();
    }

    @Test
    public void testInternalUserResolver() throws Exception {
        InternalUserResolver resolver = new InternalUserResolver(getSecurityManager());

        // Test the Security Manager default UserGroupService
        assertEquals("default", getSecurityManager().getActiveRoleService().getName());

        assertTrue(resolver.existsUser("pippo"));
        assertTrue(resolver.existsUser("jantje"));

        assertTrue(resolver.existsUser("role_user_test"));

        assertTrue(resolver.existsRole("ZEVER"));
        assertTrue(resolver.existsRole("CIRCUS"));
        assertTrue(resolver.existsRole("MOPJES"));
        assertTrue(resolver.existsRole("KLINIEK"));

        assertTrue(resolver.existsRole("ROLE_TEST"));

        Set<String> roles = resolver.getRoles("pippo");
        assertEquals(3, roles.size());
        assertTrue(roles.contains("CIRCUS"));
        assertTrue(roles.contains("ZEVER"));
        assertTrue(roles.contains("KLINIEK"));

        roles = resolver.getRoles("jantje");
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ZEVER"));
        assertTrue(roles.contains("MOPJES"));

        roles = resolver.getRoles("role_user_test");
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_TEST"));
        assertTrue(roles.contains("ROLE_TEST_2"));

        // Test the GeoFence Default User Group / Role Service
        assertEquals("test", resolver.getDefaultSecurityService().getName());
        assertTrue(resolver.getDefaultSecurityService() instanceof GeoServerRoleService);

        GeoServerRoleStore store =
                ((GeoServerRoleService) resolver.getDefaultSecurityService()).createStore();
        addTestUser("user1", Arrays.asList("adminRole", "groupAdminRole"), service, store);
        addTestUser("user2", Arrays.asList("adminRole"), service, store);
        addTestUser("user3", Arrays.asList("role1"), service, store);

        assertTrue(service.getRoleCount() == 3);
        assertTrue(
                ((GeoServerRoleService) resolver.getDefaultSecurityService()).getRoleCount() == 3);
        assertTrue(resolver.existsUser("user1"));
        assertTrue(resolver.existsUser("user2"));
        assertTrue(resolver.existsUser("user3"));

        assertTrue(resolver.existsRole("adminRole"));
        assertTrue(resolver.existsRole("groupAdminRole"));
        assertTrue(resolver.existsRole("role1"));

        roles = resolver.getRoles("user1");
        assertEquals(2, roles.size());
        assertTrue(roles.contains("adminRole"));
        assertTrue(roles.contains("groupAdminRole"));

        roles = resolver.getRoles("user2");
        assertEquals(1, roles.size());
        assertTrue(roles.contains("adminRole"));

        roles = resolver.getRoles("user3");
        assertEquals(1, roles.size());
        assertTrue(roles.contains("role1"));
    }
}
