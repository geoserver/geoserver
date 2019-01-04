/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.SortedSet;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/** @author Niels Charlier */
public class LDAPUserGroupServiceTest extends LDAPBaseTest {
    GeoServerUserGroupService service;

    @Override
    protected void createConfig() {
        config = new LDAPUserGroupServiceConfig();
    }

    @Before
    public void createUserGroupService() throws Exception {
        config.setGroupNameAttribute("cn");
        config.setUserSearchBase("ou=People");
        config.setUserNameAttribute("uid");
        config.setGroupSearchFilter("member={1},dc=example,dc=com");
        ((LDAPUserGroupServiceConfig) config)
                .setPopulatedAttributes("sn, givenName, telephoneNumber, mail");
        service = new LDAPUserGroupService(config);

        Assume.assumeTrue(
                LDAPTestUtils.initLdapServer(true, ldapServerUrl, basePath, "data4.ldif"));
    }

    @Test
    public void testUsers() throws Exception {
        SortedSet<GeoServerUser> users = service.getUsers();
        assertNotNull(users);
        assertEquals(3, users.size());
    }

    @Test
    public void testGroupByName() throws Exception {
        assertNotNull(service.getGroupByGroupname("extra"));
        assertNull(service.getGroupByGroupname("dummy"));
    }

    @Test
    public void testUserByName() throws Exception {
        GeoServerUser user = service.getUserByUsername("other");
        assertNotNull(user);
        assertEquals("other", user.getProperties().get("givenName"));
        assertEquals("dude", user.getProperties().get("sn"));
        assertEquals("2", user.getProperties().get("telephoneNumber"));
        assertNull(service.getUserByUsername("dummy"));
    }

    @Test
    public void testUsersForGroup() throws Exception {
        SortedSet<GeoServerUser> users =
                service.getUsersForGroup(service.getGroupByGroupname("other"));
        assertNotNull(users);
        assertEquals(2, users.size());
    }

    @Test
    public void testGroupsForUser() throws Exception {
        SortedSet<GeoServerUserGroup> groups =
                service.getGroupsForUser(service.getUserByUsername("other"));
        assertNotNull(groups);
        assertEquals(1, groups.size());
    }

    @Test
    public void testUserCount() throws Exception {
        assertEquals(3, service.getUserCount());
    }

    @Test
    public void testGroupCount() throws Exception {
        assertEquals(3, service.getGroupCount());
    }

    @Test
    public void testUsersHavingProperty() throws Exception {
        SortedSet<GeoServerUser> users = service.getUsersHavingProperty("mail");
        assertEquals(1, users.size());
        for (GeoServerUser user : users) {
            assertEquals("extra", user.getUsername());
        }
    }

    @Test
    public void testUsersNotHavingProperty() throws Exception {
        SortedSet<GeoServerUser> users = service.getUsersNotHavingProperty("telephoneNumber");
        assertEquals(1, users.size());
        for (GeoServerUser user : users) {
            assertEquals("extra", user.getUsername());
        }
    }

    @Test
    public void testCountUsersHavingProperty() throws Exception {
        assertEquals(1, service.getUserCountHavingProperty("mail"));
    }

    @Test
    public void testCountUsersNotHavingProperty() throws Exception {
        assertEquals(1, service.getUserCountNotHavingProperty("telephoneNumber"));
    }

    @Test
    public void testUsersHavingPropertyValue() throws Exception {
        SortedSet<GeoServerUser> users =
                service.getUsersHavingPropertyValue("telephoneNumber", "2");
        assertEquals(1, users.size());
        for (GeoServerUser user : users) {
            assertEquals("other", user.getUsername());
        }
    }

    @Test
    public void testUserCountHavingPropertyValue() throws Exception {
        assertEquals(1, service.getUserCountHavingPropertyValue("telephoneNumber", "2"));
    }
}
