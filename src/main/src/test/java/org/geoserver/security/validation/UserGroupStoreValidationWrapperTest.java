package org.geoserver.security.validation;

import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.MemoryUserGroupService;
import org.geoserver.security.password.PasswordValidator;
import org.geotools.util.logging.Logging;
import static org.geoserver.security.validation.UserGroupServiceException.*;

public class UserGroupStoreValidationWrapperTest extends GeoServerSecurityTestSupport {

    
    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");
    
    protected UserGroupStoreValidationWrapper createStore(String name) throws IOException {
        MemoryUserGroupServiceConfigImpl config = new MemoryUserGroupServiceConfigImpl();         
        config.setName(name);        
        config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        GeoServerUserGroupService service = new MemoryUserGroupService();
        service.setSecurityManager(getSecurityManager());
        service.initializeFromConfig(config);        
        return new UserGroupStoreValidationWrapper(service.createStore());
    }

    protected void assertSecurityException (IOException ex, String id, Object... params) {
        assertTrue (ex.getCause() instanceof AbstractSecurityException);
        AbstractSecurityException secEx = (AbstractSecurityException) ex.getCause(); 
        assertEquals(id,secEx.getErrorId());
        for (int i = 0; i <  params.length ;i++) {
            assertEquals(params[i], secEx.getArgs()[i]);
        }
    }
    
    public void testUserGroupStoreWrapper() throws Exception {
        boolean failed;
        UserGroupStoreValidationWrapper store = createStore("test");
                        
        failed=false;
        try { 
            store.addUser(store.createUserObject("", "", true));            
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_01);
            failed=true;
        }
        assertTrue(failed);
        
        failed=false;
        try { 
            store.addGroup(store.createGroupObject(null, true));            
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_02);
            failed=true;
        }
        assertTrue(failed);

        store.addUser(store.createUserObject("user1", "abc", true));
        store.addGroup(store.createGroupObject("group1", true));
        assertEquals(1, store.getUsers().size());
        assertEquals(1, store.getUserCount());
        assertEquals(1, store.getUserGroups().size());
        assertEquals(1, store.getGroupCount());

        failed=false;
        try { 
            store.addUser(store.createUserObject("user1", "abc", true));            
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_05,"user1");
            failed=true;
        }
        assertTrue(failed);
        
        failed=false;
        try { 
            store.addGroup(store.createGroupObject("group1", true));            
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_06,"group1");
            failed=true;
        }
        assertTrue(failed);

        store.updateUser(store.createUserObject("user1", "abc", false));
        store.updateGroup(store.createGroupObject("group1", false));
        

        failed=false;
        try { 
            store.updateUser(store.createUserObject("user1xxxx", "abc", true));            
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_03,"user1xxxx");
            failed=true;
        }
        assertTrue(failed);
        
        failed=false;
        try { 
            store.updateGroup(store.createGroupObject("group1xxx", true));            
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_04,"group1xxx");
            failed=true;
        }
        assertTrue(failed);

        GeoServerUser user1 = store.getUserByUsername("user1");
        GeoServerUserGroup group1 = store.getGroupByGroupname("group1");
        failed=false;
        try { 
            store.associateUserToGroup(
                    store.createUserObject("xxx", "abc", true),
                    group1);
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_03,"xxx");
            failed=true;
        }
        assertTrue(failed);

        failed=false;
        try { 
            store.associateUserToGroup(
                    user1,
                    store.createGroupObject("yyy", true));
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_04,"yyy");
            failed=true;
        }
        assertTrue(failed);

        store.associateUserToGroup(user1,group1);
        assertEquals(1,store.getUsersForGroup(group1).size());
        assertEquals(1,store.getGroupsForUser(user1).size());

        failed=false;
        try { 
            store.getGroupsForUser(
                    store.createUserObject("xxx", "abc", true));
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_03,"xxx");
            failed=true;
        }
        assertTrue(failed);

        failed=false;
        try { 
            store.getUsersForGroup(
                    store.createGroupObject("yyy", true));
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_04,"yyy");
            failed=true;
        }
        assertTrue(failed);

        failed=false;
        try { 
            store.disAssociateUserFromGroup(
                    store.createUserObject("xxx", "abc", true),
                    group1);
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_03,"xxx");
            failed=true;
        }
        assertTrue(failed);

        failed=false;
        try { 
            store.disAssociateUserFromGroup(
                    user1,
                    store.createGroupObject("yyy", true));
        } catch (IOException ex) {
            assertSecurityException(ex, UG_ERR_04,"yyy");
            failed=true;
        }
        assertTrue(failed);
        
        store.disAssociateUserFromGroup(user1,group1);
        store.removeUser(user1);
        store.removeGroup(group1);
    }

}
