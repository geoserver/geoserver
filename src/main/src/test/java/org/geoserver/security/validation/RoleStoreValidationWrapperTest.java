package org.geoserver.security.validation;

import static org.geoserver.security.validation.RoleServiceException.*;

import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
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
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLRoleServiceConfig;
import org.geotools.util.logging.Logging;

public class RoleStoreValidationWrapperTest extends GeoServerSecurityTestSupport {

    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");

    public RoleStoreValidationWrapper createStore(String name,String adminRole,String groupAdminRole,
            GeoServerUserGroupService... services) throws IOException, SecurityConfigException {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName(name);
        config.setClassName(MemoryRoleService.class.getName());
        config.setAdminRoleName(adminRole);
        config.setGroupAdminRoleName(groupAdminRole);
        GeoServerRoleService service = new MemoryRoleService();
        service.initializeFromConfig(config);
        service.setSecurityManager(GeoServerExtensions.bean(GeoServerSecurityManager.class));
        getSecurityManager().saveRoleService(config);
        return new RoleStoreValidationWrapper(service.createStore(), services);
    }
    
    public RoleStoreValidationWrapper createXMLStore(String name,String adminRole,String groupAdminRole,
            GeoServerUserGroupService... services) throws IOException, SecurityConfigException {
        XMLRoleServiceConfig config = new XMLRoleServiceConfig();
        config.setName(name);
        config.setClassName(XMLRoleService.class.getName());
        config.setAdminRoleName(adminRole);
        config.setGroupAdminRoleName(groupAdminRole);
        config.setFileName("roles.xml");
        getSecurityManager().saveRoleService(config);
        return new RoleStoreValidationWrapper(getSecurityManager().loadRoleService(name).createStore(), services);
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
        assertEquals(id, secEx.getId());
        for (int i = 0; i < params.length; i++) {
            assertEquals(params[i], secEx.getArgs()[i]);
        }
    }

    public void testRoleStoreWrapper() throws Exception {
        boolean failed;
        RoleStoreValidationWrapper store = createStore("test",null,"");
        RoleStoreValidationWrapper store1 = createXMLStore("test1",null,"");

        
        failed = false;
        try {
            store.addRole(store.createRoleObject(""));
        } catch (IOException ex) {
            assertSecurityException(ex, NAME_REQUIRED);
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
            assertSecurityException(ex, ALREADY_EXISTS, "role1");
            failed = true;
        }
        assertTrue(failed);
        
        for (GeoServerRole srole : GeoServerRole.SystemRoles) {
            failed = false;
            try {
                store.addRole(store.createRoleObject(srole.getAuthority()));
            } catch (IOException ex) {
                assertSecurityException(ex, RESERVED_NAME, srole.getAuthority());
                failed = true;
            }
            assertTrue(failed);
        }
        
        store1.addRole(store.createRoleObject("duplicated"));
        store1.store();
        failed = false;
        try {
            store.addRole(store.createRoleObject("duplicated"));
        } catch (IOException ex) {
            assertSecurityException(ex, ALREADY_EXISTS_IN, "duplicated",store1.getName());
            failed = true;
        }
        assertTrue(failed);

        
        
        failed = false;
        try {
            store.addRole(store.createRoleObject(GeoServerRole.AUTHENTICATED_ROLE.getAuthority()));
        } catch (IOException ex) {
            assertSecurityException(ex, RESERVED_NAME, GeoServerRole.AUTHENTICATED_ROLE.getAuthority());
            failed = true;
        }
        assertTrue(failed);

        

        failed = false;
        try {
            store.updateRole(store.createRoleObject("xxx"));
        } catch (IOException ex) {
            assertSecurityException(ex, NOT_FOUND, "xxx");
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
            assertSecurityException(ex, NOT_FOUND, "xxx");
            failed = true;
        }
        assertTrue(failed);

        store.setParentRole(role1, parent1);
        store.setParentRole(role1, null);

        failed = false;
        try {
            store.associateRoleToGroup(role1, "");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_REQUIRED);
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.disAssociateRoleFromGroup(role1, "");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_REQUIRED);
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.associateRoleToUser(role1, "");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_REQUIRED);
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.disAssociateRoleFromUser(role1, "");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_REQUIRED);
            failed = true;
        }
        assertTrue(failed);

        store.associateRoleToGroup(role1, "group1");
        store.associateRoleToUser(role1, "user1");

        failed = false;
        try {
            store.getRolesForUser(null);
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_REQUIRED);
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.getRolesForGroup(null);
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_REQUIRED);
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
            assertSecurityException(ex, ROLE_IN_USE_$2,
                    role1.getAuthority(),newRule.getKey());
            failed = true;
        }
        assertTrue(failed);
        
        dao.removeRule(newRule);
        dao.storeRules();
        store2.removeRole(role1);
                

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

        RoleStoreValidationWrapper store = createStore("test", null,null,ugStore1, ugStore2);
        GeoServerRole role1 = store.createRoleObject("role1");
        store.addRole(role1);
        store.store();

        store.associateRoleToGroup(role1, "group1");
        store.associateRoleToGroup(role1, "group2");
        failed = false;
        try {
            store.associateRoleToGroup(role1, "group3");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_NOT_FOUND_$1 , "group3");
            failed = true;
        }
        assertTrue(failed);

        store.associateRoleToUser(role1, "user1");
        store.associateRoleToUser(role1, "user1");
        failed = false;
        try {
            store.associateRoleToUser(role1, "user3");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_NOT_FOUND_$1, "user3");
            failed = true;
        }
        assertTrue(failed);

        assertEquals(1, store.getRolesForGroup("group1").size());
        assertEquals(1, store.getRolesForUser("user1").size());

        failed = false;
        try {
            store.getRolesForGroup("group3");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_NOT_FOUND_$1 , "group3");
            failed = true;
        }
        assertTrue(failed);

        failed = false;
        try {
            store.getRolesForUser("user3");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_NOT_FOUND_$1, "user3");
            failed = true;
        }
        assertTrue(failed);

        store.disAssociateRoleFromGroup(role1, "group1");
        store.disAssociateRoleFromGroup(role1, "group2");
        failed = false;
        try {
            store.disAssociateRoleFromGroup(role1, "group3");
        } catch (IOException ex) {
            assertSecurityException(ex, GROUPNAME_NOT_FOUND_$1 , "group3");
            failed = true;
        }
        assertTrue(failed);

        store.disAssociateRoleFromUser(role1, "user1");
        store.disAssociateRoleFromUser(role1, "user1");
        failed = false;
        try {
            store.disAssociateRoleFromUser(role1, "user3");
        } catch (IOException ex) {
            assertSecurityException(ex, USERNAME_NOT_FOUND_$1, "user3");
            failed = true;
        }
        assertTrue(failed);
        
        store.removeRole(role1);

    }
    
    public void testMappedRoles() throws Exception {
        boolean failed;
        
        RoleStoreValidationWrapper store = createXMLStore("test","admin","groupAdmin");
        store.addRole(store.createRoleObject("admin"));
        store.addRole(store.createRoleObject("groupAdmin"));
        store.addRole(store.createRoleObject("role1"));
        store.store();
        
        
        store = new RoleStoreValidationWrapper(getSecurityManager().loadRoleService("test").createStore());        
        failed = false;
        try {
            store.removeRole(store.createRoleObject("admin"));
        } catch (IOException ex) {
            assertSecurityException(ex,ADMIN_ROLE_NOT_REMOVABLE_$1,"admin" );
            failed = true;
        }
        assertTrue(failed);
        
        failed = false;
        try {
            store.removeRole(store.createRoleObject("groupAdmin"));
        } catch (IOException ex) {
            assertSecurityException(ex,GROUP_ADMIN_ROLE_NOT_REMOVABLE_$1,"groupAdmin" );
            failed = true;
        }
        assertTrue(failed);
        
        store.removeRole(store.createRoleObject("role1"));

    }

}
