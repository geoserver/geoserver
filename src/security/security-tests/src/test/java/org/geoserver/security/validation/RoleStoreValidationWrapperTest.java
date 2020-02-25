/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.validation;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.geoserver.security.validation.RoleServiceException.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.TreeSet;
import org.geoserver.data.test.MockCreator;
import org.geoserver.data.test.MockTestData;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.*;
import org.geoserver.test.GeoServerMockTestSupport;
import org.junit.Test;

// @TestSetup(run=TestSetupFrequency.REPEAT)
public class RoleStoreValidationWrapperTest extends GeoServerMockTestSupport {

    protected void assertSecurityException(IOException ex, String id, Object... params) {
        assertTrue(ex.getCause() instanceof AbstractSecurityException);
        AbstractSecurityException secEx = (AbstractSecurityException) ex.getCause();
        assertEquals(id, secEx.getId());
        for (int i = 0; i < params.length; i++) {
            assertEquals(params[i], secEx.getArgs()[i]);
        }
    }

    @Test
    public void testRoleStoreWrapper() throws Exception {
        setMockCreator(
                new MockCreator() {
                    @Override
                    public GeoServerSecurityManager createSecurityManager(MockTestData testData)
                            throws Exception {
                        GeoServerSecurityManager secMgr =
                                createMock(GeoServerSecurityManager.class);

                        GeoServerRoleStore roleStore1 =
                                createRoleStore("test", secMgr, "role1", "parent1");
                        addRolesToCreate(roleStore1, "", "duplicated", "xxx");

                        GeoServerRoleStore roleStore2 =
                                createRoleStore("test1", secMgr, "duplicated");

                        expect(secMgr.listRoleServices())
                                .andReturn(new TreeSet<String>(Arrays.asList("test", "test1")))
                                .anyTimes();

                        replay(roleStore1, roleStore2, secMgr);
                        return secMgr;
                    }
                });

        GeoServerSecurityManager secMgr = getSecurityManager();
        GeoServerRoleStore roleStore = (GeoServerRoleStore) secMgr.loadRoleService("test");

        RoleStoreValidationWrapper store = new RoleStoreValidationWrapper(roleStore);
        try {
            store.addRole(store.createRoleObject(""));
            fail("empty role name should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, NAME_REQUIRED);
        }

        try {
            store.addRole(store.createRoleObject(""));
            fail("empty role name should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, NAME_REQUIRED);
        }

        GeoServerRole role1 = store.getRoleByName("role1");

        try {
            store.addRole(role1);
            fail("already existing role name should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, ALREADY_EXISTS, "role1");
        }

        for (GeoServerRole srole : GeoServerRole.SystemRoles) {
            try {
                store.addRole(store.createRoleObject(srole.getAuthority()));
                fail("reserved role name should throw exception");
            } catch (IOException ex) {
                assertSecurityException(ex, RESERVED_NAME, srole.getAuthority());
            }
        }

        GeoServerRoleStore roleStore1 = (GeoServerRoleStore) secMgr.loadRoleService("test1");
        RoleStoreValidationWrapper store1 = new RoleStoreValidationWrapper(roleStore1);

        try {
            store.addRole(store.createRoleObject("duplicated"));
            fail("reserved role name should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, ALREADY_EXISTS_IN, "duplicated", store1.getName());
        }

        try {
            String authRole = GeoServerRole.AUTHENTICATED_ROLE.getAuthority();
            store.addRole(store.createRoleObject(authRole));
            fail(authRole + " is reserved and should throw exception");
        } catch (IOException ex) {
            assertSecurityException(
                    ex, RESERVED_NAME, GeoServerRole.AUTHENTICATED_ROLE.getAuthority());
        }

        try {
            store.updateRole(store.createRoleObject("xxx"));
            fail("update role object that does not exist should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, NOT_FOUND, "xxx");
        }

        try {
            store.setParentRole(role1, store.createRoleObject("xxx"));
        } catch (IOException ex) {
            assertSecurityException(ex, NOT_FOUND, "xxx");
        }

        try {
            store.associateRoleToGroup(role1, "");
            fail("empty group name should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_REQUIRED);
        }

        try {
            store.disAssociateRoleFromGroup(role1, "");
            fail("empty group name should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_REQUIRED);
        }

        try {
            store.associateRoleToUser(role1, "");
            fail("empty user name should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_REQUIRED);
        }

        try {
            store.disAssociateRoleFromUser(role1, "");
            fail("empty user name should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_REQUIRED);
        }

        try {
            store.getRolesForUser(null);
            fail("null user name should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_REQUIRED);
        }

        try {
            store.getRolesForGroup(null);
            fail("null group name should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_REQUIRED);
        }
    }

    @Test
    @SuppressWarnings("SelfComparison")
    public void testRoleServiceWrapperAccessRules() throws Exception {
        setMockCreator(
                new MockCreator() {
                    @Override
                    public GeoServerSecurityManager createSecurityManager(MockTestData testData)
                            throws Exception {
                        GeoServerSecurityManager secMgr =
                                createNiceMock(GeoServerSecurityManager.class);

                        GeoServerRoleStore roleStore =
                                createRoleStore("test", secMgr, "role1", "parent1");
                        expect(roleStore.removeRole(new GeoServerRole("unused"))).andReturn(true);

                        DataAccessRule dataAccessRule = createNiceMock(DataAccessRule.class);
                        expect(dataAccessRule.compareTo(dataAccessRule)).andReturn(0).anyTimes();
                        expect(dataAccessRule.getKey()).andReturn("foo").anyTimes();
                        expect(dataAccessRule.getRoles())
                                .andReturn(new TreeSet<String>(Arrays.asList("role1")))
                                .anyTimes();
                        replay(dataAccessRule);

                        DataAccessRuleDAO dataAccessDAO = createNiceMock(DataAccessRuleDAO.class);
                        expect(dataAccessDAO.getRulesAssociatedWithRole("role1"))
                                .andReturn(
                                        new TreeSet<DataAccessRule>(Arrays.asList(dataAccessRule)))
                                .anyTimes();
                        expect(dataAccessDAO.getRulesAssociatedWithRole("parent1"))
                                .andReturn(new TreeSet<DataAccessRule>())
                                .anyTimes();
                        expect(secMgr.getDataAccessRuleDAO()).andReturn(dataAccessDAO).anyTimes();

                        ServiceAccessRuleDAO serviceAccessDAO =
                                createNiceMock(ServiceAccessRuleDAO.class);
                        expect(serviceAccessDAO.getRulesAssociatedWithRole((String) anyObject()))
                                .andReturn(new TreeSet<ServiceAccessRule>())
                                .anyTimes();
                        expect(secMgr.getServiceAccessRuleDAO())
                                .andReturn(serviceAccessDAO)
                                .anyTimes();

                        replay(dataAccessDAO, serviceAccessDAO, roleStore, secMgr);
                        return secMgr;
                    }
                });

        RoleStoreValidationWrapper store =
                new RoleStoreValidationWrapper(
                        (GeoServerRoleStore) getSecurityManager().loadRoleService("test"), true);
        GeoServerRole role = store.getRoleByName("role1");
        GeoServerRole parent = store.getRoleByName("parent1");

        store.removeRole(parent);
        try {
            store.removeRole(role);
            fail("used role should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_IN_USE_$2, role.getAuthority(), "foo");
        }
    }

    @Test
    public void testRoleStoreWrapperWithUGServices() throws Exception {
        setMockCreator(
                new MockCreator() {
                    @Override
                    public GeoServerSecurityManager createSecurityManager(MockTestData testData)
                            throws Exception {
                        GeoServerSecurityManager secMgr =
                                createNiceMock(GeoServerSecurityManager.class);

                        GeoServerUserGroupStore ugStore1 = createUserGroupStore("test1", secMgr);
                        addUsers(ugStore1, "user1", "abc");
                        addGroups(ugStore1, "group1");

                        GeoServerUserGroupStore ugStore2 = createUserGroupStore("test2", secMgr);
                        addUsers(ugStore1, "user2", "abc");
                        addGroups(ugStore1, "group2");

                        GeoServerRoleStore roleStore = createRoleStore("test", secMgr, "role1");
                        expect(roleStore.getGroupNamesForRole(new GeoServerRole("role1")))
                                .andReturn(new TreeSet<String>(Arrays.asList("group1", "group2")))
                                .anyTimes();

                        replay(ugStore1, ugStore2, roleStore, secMgr);
                        return secMgr;
                    }
                });

        GeoServerSecurityManager secMgr = getSecurityManager();
        GeoServerUserGroupStore ugStore1 =
                (GeoServerUserGroupStore) secMgr.loadUserGroupService("test1");
        GeoServerUserGroupStore ugStore2 =
                (GeoServerUserGroupStore) secMgr.loadUserGroupService("test2");

        RoleStoreValidationWrapper store =
                new RoleStoreValidationWrapper(
                        (GeoServerRoleStore) secMgr.loadRoleService("test"), ugStore1, ugStore2);

        GeoServerRole role1 = store.getRoleByName("role1");
        try {
            store.associateRoleToGroup(role1, "group3");
            fail("unkown group should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_NOT_FOUND_$1, "group3");
        }

        try {
            store.associateRoleToUser(role1, "user3");
            fail("unkown user should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_NOT_FOUND_$1, "user3");
        }

        try {
            store.getRolesForGroup("group3");
            fail("unkown group should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_NOT_FOUND_$1, "group3");
        }

        try {
            store.getRolesForUser("user3");
            fail("unkown user should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_NOT_FOUND_$1, "user3");
        }

        store.disAssociateRoleFromGroup(role1, "group1");
        store.disAssociateRoleFromGroup(role1, "group2");
        try {
            store.disAssociateRoleFromGroup(role1, "group3");
            fail("unkown group should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_NOT_FOUND_$1, "group3");
        }

        store.disAssociateRoleFromUser(role1, "user1");
        store.disAssociateRoleFromUser(role1, "user1");

        try {
            store.disAssociateRoleFromUser(role1, "user3");
            fail("unkown user should throw exception");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_NOT_FOUND_$1, "user3");
        }
    }

    @Test
    public void testMappedRoles() throws Exception {
        setMockCreator(
                new MockCreator() {
                    @Override
                    public GeoServerSecurityManager createSecurityManager(MockTestData testData)
                            throws Exception {
                        GeoServerSecurityManager secMgr =
                                createNiceMock(GeoServerSecurityManager.class);

                        GeoServerRoleStore roleStore =
                                createRoleStore("test", secMgr, "admin", "groupAdmin", "role1");
                        addRolesToCreate(roleStore, "admin", "groupAdmin");
                        expect(roleStore.getAdminRole())
                                .andReturn(new GeoServerRole("admin"))
                                .anyTimes();
                        expect(roleStore.getGroupAdminRole())
                                .andReturn(new GeoServerRole("groupAdmin"))
                                .anyTimes();

                        replay(roleStore, secMgr);
                        return secMgr;
                    }
                });

        GeoServerSecurityManager secMgr = getSecurityManager();
        RoleStoreValidationWrapper store =
                new RoleStoreValidationWrapper((GeoServerRoleStore) secMgr.loadRoleService("test"));

        try {
            store.removeRole(store.createRoleObject("admin"));
            fail("removing admin role should fail");
        } catch (IOException ex) {
            assertSecurityException(ex, ADMIN_ROLE_NOT_REMOVABLE_$1, "admin");
        }

        try {
            store.removeRole(store.createRoleObject("groupAdmin"));
            fail("removing group admin role should fail");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUP_ADMIN_ROLE_NOT_REMOVABLE_$1, "groupAdmin");
        }
    }
}
