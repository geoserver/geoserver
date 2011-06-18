package org.geoserver.security.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.geoserver.security.PropertyFileWatcher;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UsernameNotFoundException;

public class GeoServerUserDaoTest extends TestCase {

    static class TestableUserDao extends GeoserverUserDao {
        
        public TestableUserDao(Properties p) throws IOException {
            userMap = loadUsersFromProperties(p);
        }
        
        @Override
        void checkUserMap() throws DataAccessResourceFailureException {
            // do nothing, for this test we don't write on the fs by default
        }
        
        void loadUserMap() {
            super.checkUserMap();
        }
    }

    Properties props;
    TestableUserDao dao;
    
    @Override
    protected void setUp() throws Exception {
        props = new Properties();
        props.put("admin", "gs,ROLE_ADMINISTRATOR");
        props.put("wfs", "webFeatureService,ROLE_WFS_READ,ROLE_WFS_WRITE");
        props.put("disabledUser", "nah,ROLE_TEST,disabled");
        dao = new TestableUserDao(props);
    }
    
    public void testGetUsers() throws Exception {
        List<User> users = dao.getUsers();
        assertEquals(3, users.size());
    }
    
    public void testLoadUser() throws Exception {
        UserDetails admin = dao.loadUserByUsername("admin");
        assertEquals("admin", admin.getUsername());
        assertEquals("gs", admin.getPassword());
        assertEquals(1, admin.getAuthorities().length);
        assertEquals("ROLE_ADMINISTRATOR", admin.getAuthorities()[0].getAuthority());
    }
    
    public void testMissingUser() throws Exception {
        try {
            dao.loadUserByUsername("notThere");
            fail("This user should not be there");
        } catch(Exception e) {
            // ok
        }
    }
    
    public void testSetUser() throws Exception {
        dao.setUser(new User("wfs", "pwd", true, true, true, true, 
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_WFS_ALL"), new GrantedAuthorityImpl("ROLE_WMS_ALL")}));
        UserDetails user = dao.loadUserByUsername("wfs");
        assertEquals("wfs", user.getUsername());
        assertEquals("pwd", user.getPassword());
        assertEquals(2, user.getAuthorities().length);
        // ok... order dependent... making one non order dep takes too much time...
        assertEquals("ROLE_WFS_ALL", user.getAuthorities()[0].getAuthority());
        assertEquals("ROLE_WMS_ALL", user.getAuthorities()[1].getAuthority());
    }
    
    public void testSetMissingUser() throws Exception {
        try {
            dao.setUser(new User("notther", "pwd", true, true, true, true, 
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_WFS_ALL")}));
            fail("The user is not there, setUser should fail");
        } catch(IllegalArgumentException e) {
            // cool
        }
    }
    
    public void testAddUser() throws Exception {
        dao.putUser(new User("newuser", "pwd", true, true, true, true, 
                new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_WFS_ALL")}));
        assertNotNull(dao.loadUserByUsername("newuser"));
    }
    
    public void addExistingUser() throws Exception {
        try {
            dao.putUser(new User("admin", "pwd", true, true, true, true, 
                    new GrantedAuthority[] {new GrantedAuthorityImpl("ROLE_WFS_ALL")}));
            fail("The user is already there, addUser should fail");
        } catch(IllegalArgumentException e) {
            // cool
        }
    }
    
    public void testRemoveUser() throws Exception {
        assertFalse(dao.removeUser("notthere"));
        assertTrue(dao.removeUser("wfs"));
        try {
            dao.loadUserByUsername("wfs");
            fail("The user is not there, loadUserByName should fail");
        } catch(UsernameNotFoundException e) {
            // cool
        }
    }
    
    
    public void testStoreReload() throws Exception {
        File temp = File.createTempFile("sectest", "", new File("target"));
        temp.delete();
        temp.mkdir();
        try {
            dao.securityDir = temp;
            dao.storeUsers();
            File propFile = new File(temp, "users.properties");
            dao.userDefinitionsFile = new PropertyFileWatcher(propFile);
            dao.userMap.clear();
            dao.loadUserMap();
        } finally {
            temp.delete();
        }
        
        assertEquals(3, dao.getUsers().size());
        testLoadUser();
    }
}
