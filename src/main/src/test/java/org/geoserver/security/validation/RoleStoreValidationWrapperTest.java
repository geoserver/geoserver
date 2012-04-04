package org.geoserver.security.validation;

import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.MemoryRoleService;
import org.geoserver.security.impl.MemoryUserGroupService;
import org.geoserver.security.password.PasswordValidator;
import org.geotools.util.logging.Logging;
import static org.geoserver.security.validation.RoleServiceException.*;

public class RoleStoreValidationWrapperTest extends GeoServerSecurityTestSupport {

    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");

    public RoleStoreValidationWrapper createStore(String name,String adminRole,
            GeoServerUserGroupService... services) throws IOException, SecurityConfigException {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName(name);
        config.setClassName(MemoryRoleService.class.getName());
        config.setAdminRoleName(adminRole);
        GeoServerRoleService service = new MemoryRoleService();
        service.initializeFromConfig(config);
        service.setSecurityManager(GeoServerExtensions.bean(GeoServerSecurityManager.class));
        getSecurityManager().saveRoleService(config);
        return new RoleStoreValidationWrapper(service.createStore(), services);
    }

    protected GeoServerUserGroupStore createUGStore(String name) throws IOException {
        MemoryUserGroupServiceConfigImpl config = new MemoryUserGroupServiceConfigImpl();
        config.setName(name);
        config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        GeoServerUserGroupService service = new MemoryUserGroupService();
        service.setSecurityManager(GeoServerExtensions.bean(GeoServerSecurityManager.class));
        service.initializeFromConfig(config);
        return service.createStore();
    }

    protected void assertSecurityException(IOException ex, String id, Object... params) {
        assertTrue(ex.getCause() instanceof AbstractSecurityException);
        AbstractSecurityException secEx = (AbstractSecurityException) ex.getCause();
        assertEquals(id, secEx.getErrorId());
        for (int i = 0; i < params.length; i++) {
            assertEquals(params[i], secEx.getArgs()[i]);
        }
    }

    public void testRoleStoreWrapper() throws Exception {
        boolean failed;
        RoleStoreValidationWrapper store = createStore("test",GeoServerRole.ADMIN_ROLE.getAuthority());

        failed = false;
        try {
            store.addRole(store.createRoleObject(""));
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_01);
            failed = true;
        }
        assertTrue(failed);

        store.addRole(store.createRoleObject("role1"));
        assertEquals(1, store.getRoles().size());
        assertEquals(1, store.getRoleCount());
        GeoServerRole role1 = store.getRoleByName("role1");

        failed = false;
        try {
            store.addRole(role1);
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_03, "role1");
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.updateRole(store.createRoleObject("xxx"));
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_02, "xxx");
            failed = true;
        }
        assertTrue(failed);

        store.addRole(store.createRoleObject("parent1"));
        GeoServerRole parent1 = store.getRoleByName("parent1");
        assertNotNull(parent1);
        failed = false;
        try {
            store.setParentRole(role1, store.createRoleObject("xxx"));
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_02, "xxx");
            failed = true;
        }
        assertTrue(failed);

        store.setParentRole(role1, parent1);
        store.setParentRole(role1, null);

        failed = false;
        try {
            store.associateRoleToGroup(role1, "");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_05);
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.disAssociateRoleFromGroup(role1, "");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_05);
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.associateRoleToUser(role1, "");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_04);
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.disAssociateRoleFromUser(role1, "");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_04);
            failed = true;
        }
        assertTrue(failed);

        store.associateRoleToGroup(role1, "group1");
        store.associateRoleToUser(role1, "user1");

        failed = false;
        try {
            store.getRolesForUser(null);
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_04);
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.getRolesForGroup(null);
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_05);
            failed = true;
        }
        assertTrue(failed);

        assertEquals(1, store.getRolesForGroup("group1").size());
        assertEquals(1, store.getRolesForUser("user1").size());

        store.disAssociateRoleFromGroup(role1, "group1");
        store.disAssociateRoleFromUser(role1, "user1");

        DataAccessRuleDAO dao = DataAccessRuleDAO.get();
        DataAccessRule newRule = new DataAccessRule();
        newRule.setAccessMode(AccessMode.READ);
        newRule.setWorkspace(DataAccessRule.ANY);
        newRule.setLayer(DataAccessRule.ANY);
        newRule.getRoles().add(role1.getAuthority());
        dao.addRule(newRule);
        dao.storeRules();
                
        RoleStoreValidationWrapper store2 = new 
                RoleStoreValidationWrapper(
                        (GeoServerRoleStore) store.getWrappedService(),true);
        failed = false;
        try {        
            store2.removeRole(parent1);
            store2.removeRole(role1);
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_09,
                    role1.getAuthority(),newRule.getKey());
            failed = true;
        }
        assertTrue(failed);
        
        dao.removeRule(newRule);
        dao.storeRules();
        store2.removeRole(role1);
                
        failed = false;
        try {
            store.removeRole(GeoServerRole.ADMIN_ROLE);
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_08,
                    GeoServerRole.ADMIN_ROLE.getAuthority());
            failed = true;
        }
        assertTrue(failed);

    }

    public void testRoleStoreWrapperWithUGServices() throws Exception {
        boolean failed;
        GeoServerUserGroupStore ugStore1 = createUGStore("test1");
        ugStore1.addUser(ugStore1.createUserObject("user1", "abc", true));
        ugStore1.addGroup(ugStore1.createGroupObject("group1", true));
        ugStore1.store();

        GeoServerUserGroupStore ugStore2 = createUGStore("test2");
        ugStore2.addUser(ugStore1.createUserObject("user2", "abc", true));
        ugStore2.addGroup(ugStore1.createGroupObject("group2", true));
        ugStore2.store();

        RoleStoreValidationWrapper store = createStore("test", "role1",ugStore1, ugStore2);
        GeoServerRole role1 = store.createRoleObject("role1");
        store.addRole(role1);
        store.store();

        store.associateRoleToGroup(role1, "group1");
        store.associateRoleToGroup(role1, "group2");
        failed = false;
        try {
            store.associateRoleToGroup(role1, "group3");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_07, "group3");
            failed = true;
        }
        assertTrue(failed);

        store.associateRoleToUser(role1, "user1");
        store.associateRoleToUser(role1, "user1");
        failed = false;
        try {
            store.associateRoleToUser(role1, "user3");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_06, "user3");
            failed = true;
        }
        assertTrue(failed);

        assertEquals(1, store.getRolesForGroup("group1").size());
        assertEquals(1, store.getRolesForUser("user1").size());

        failed = false;
        try {
            store.getRolesForGroup("group3");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_07, "group3");
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.getRolesForUser("user3");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_06, "user3");
            failed = true;
        }
        assertTrue(failed);

        store.disAssociateRoleFromGroup(role1, "group1");
        store.disAssociateRoleFromGroup(role1, "group2");
        failed = false;
        try {
            store.disAssociateRoleFromGroup(role1, "group3");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_07, "group3");
            failed = true;
        }
        assertTrue(failed);

        store.disAssociateRoleFromUser(role1, "user1");
        store.disAssociateRoleFromUser(role1, "user1");
        failed = false;
        try {
            store.disAssociateRoleFromUser(role1, "user3");
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_06, "user3");
            failed = true;
        }
        assertTrue(failed);
        
        failed = false;
        try {
            store.removeRole(role1);
        } catch (IOException ex) {
            assertSecurityException(ex, ROLE_ERR_08,
                    "role1");
            failed = true;
        }
        assertTrue(failed);

        store.removeRole(GeoServerRole.ADMIN_ROLE);
    }

}
