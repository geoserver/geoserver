/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.PropertyFileWatcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class GeoServerUserDaoTest {

    static class TestableUserDao extends GeoServerUserDao {

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

    @Before
    public void setUp() throws Exception {
        props = new Properties();
        props.put("admin", "gs,ROLE_ADMINISTRATOR");
        props.put("wfs", "webFeatureService,ROLE_WFS_READ,ROLE_WFS_WRITE");
        props.put("disabledUser", "nah,ROLE_TEST,disabled");
        dao = new TestableUserDao(props);
    }

    @Test
    public void testGetUsers() throws Exception {
        List<User> users = dao.getUsers();
        assertEquals(3, users.size());
    }

    @Test
    public void testLoadUser() throws Exception {
        UserDetails admin = dao.loadUserByUsername("admin");
        assertEquals("admin", admin.getUsername());
        assertEquals("gs", admin.getPassword());
        assertEquals(1, admin.getAuthorities().size());
        assertEquals("ROLE_ADMINISTRATOR", admin.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    public void testMissingUser() throws Exception {
        try {
            dao.loadUserByUsername("notThere");
            fail("This user should not be there");
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void testSetUser() throws Exception {
        dao.setUser(
                new User(
                        "wfs",
                        "pwd",
                        true,
                        true,
                        true,
                        true,
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_WFS_ALL"),
                                    new SimpleGrantedAuthority("ROLE_WMS_ALL")
                                })));
        UserDetails user = dao.loadUserByUsername("wfs");
        assertEquals("wfs", user.getUsername());
        assertEquals("pwd", user.getPassword());
        assertEquals(2, user.getAuthorities().size());
        Set<String> authorities = new HashSet<String>();
        for (GrantedAuthority ga : user.getAuthorities()) {
            authorities.add(ga.getAuthority());
        }
        //  order independent
        assertTrue(authorities.contains("ROLE_WFS_ALL"));
        assertTrue(authorities.contains("ROLE_WMS_ALL"));
    }

    @Test
    public void testSetMissingUser() throws Exception {
        try {
            dao.setUser(
                    new User(
                            "notther",
                            "pwd",
                            true,
                            true,
                            true,
                            true,
                            Arrays.asList(
                                    new GrantedAuthority[] {
                                        new SimpleGrantedAuthority("ROLE_WFS_ALL")
                                    })));
            fail("The user is not there, setUser should fail");
        } catch (IllegalArgumentException e) {
            // cool
        }
    }

    @Test
    public void testAddUser() throws Exception {
        dao.putUser(
                new User(
                        "newuser",
                        "pwd",
                        true,
                        true,
                        true,
                        true,
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_WFS_ALL")
                                })));
        assertNotNull(dao.loadUserByUsername("newuser"));
    }

    @Test
    public void addExistingUser() throws Exception {
        try {
            dao.putUser(
                    new User(
                            "admin",
                            "pwd",
                            true,
                            true,
                            true,
                            true,
                            Arrays.asList(
                                    new GrantedAuthority[] {
                                        new SimpleGrantedAuthority("ROLE_WFS_ALL")
                                    })));
            fail("The user is already there, addUser should fail");
        } catch (IllegalArgumentException e) {
            // cool
        }
    }

    @Test
    public void testRemoveUser() throws Exception {
        assertFalse(dao.removeUser("notthere"));
        assertTrue(dao.removeUser("wfs"));
        try {
            dao.loadUserByUsername("wfs");
            fail("The user is not there, loadUserByName should fail");
        } catch (UsernameNotFoundException e) {
            // cool
        }
    }

    @Test
    public void testStoreReload() throws Exception {
        File temp = File.createTempFile("sectest", "", new File("target"));
        temp.delete();
        temp.mkdir();
        File propFile = new File(temp, "users.properties");
        try {
            dao.userDefinitionsFile = new PropertyFileWatcher(Files.asResource(propFile));
            dao.storeUsers();
            dao.userMap.clear();
            dao.loadUserMap();
        } finally {
            temp.delete();
        }

        assertEquals(3, dao.getUsers().size());
        testLoadUser();
    }
}
