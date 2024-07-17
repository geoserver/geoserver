/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.geoserver.rest.RestException;
import org.geoserver.rest.security.xml.JaxbGroupList;
import org.geoserver.rest.security.xml.JaxbRoleList;
import org.geoserver.rest.security.xml.JaxbUser;
import org.geoserver.rest.security.xml.JaxbUserList;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class UserRoleRestControllerTest extends GeoServerTestSupport {

    private static final String USER_SERVICE = "default";

    protected UsersRestController usersController;

    protected RolesRestController rolesController;

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        usersController = applicationContext.getBean(UsersRestController.class);
        rolesController = applicationContext.getBean(RolesRestController.class);
    }

    @Test
    public void testRolesAndUsers() throws PasswordPolicyException, IOException {

        JaxbUser user = new JaxbUser();
        user.setUserName("pipo");
        user.setPassword("secret");
        user.setEnabled(true);

        usersController.insertUser(USER_SERVICE, user);
        usersController.insertGroup(USER_SERVICE, "clowns");
        usersController.insertGroup(USER_SERVICE, "circus");
        usersController.associateUserToGroup(USER_SERVICE, "pipo", "clowns");
        usersController.associateUserToGroup(USER_SERVICE, "pipo", "circus");

        JaxbUserList users = usersController.getUsers(USER_SERVICE);
        boolean found = false;
        for (JaxbUser u : users.getUsers()) {
            if ("pipo".equals(u.getUserName())) {
                assertTrue(u.isEnabled());
                found = true;
            }
        }
        assertTrue(found);

        users = usersController.getUsersFromGroup(USER_SERVICE, "clowns");
        found = false;
        for (JaxbUser u : users.getUsers()) {
            if ("pipo".equals(u.getUserName())) {
                assertTrue(u.isEnabled());
                found = true;
            }
        }
        assertTrue(found);

        JaxbGroupList groups = usersController.getGroupsFromUser(USER_SERVICE, "pipo");
        assertEquals(2, groups.getGroups().size());
        assertTrue(groups.getGroups().contains("clowns"));
        assertTrue(groups.getGroups().contains("circus"));

        usersController.disassociateUserFromGroup(USER_SERVICE, "pipo", "circus");
        groups = usersController.getGroupsFromUser(USER_SERVICE, "pipo");
        assertEquals(1, groups.getGroups().size());
        assertTrue(groups.getGroups().contains("clowns"));
        assertFalse(groups.getGroups().contains("circus"));

        usersController.deleteGroup(USER_SERVICE, "circus");
        assertEquals(1, groups.getGroups().size());
        assertTrue(groups.getGroups().contains("clowns"));
        assertFalse(groups.getGroups().contains("circus"));

        user = new JaxbUser();
        user.setEnabled(false);
        usersController.updateUser(USER_SERVICE, "pipo", user);
        users = usersController.getUsers(USER_SERVICE);
        found = false;
        for (JaxbUser u : users.getUsers()) {
            if ("pipo".equals(u.getUserName())) {
                assertFalse(u.isEnabled());
                found = true;
            }
        }
        assertTrue(found);

        rolesController.insert("vozen");
        rolesController.insert("kwiestenbiebel");
        users = usersController.getUsers(USER_SERVICE);
        JaxbRoleList roles = rolesController.get();
        assertTrue(roles.getRoles().contains("vozen"));
        assertTrue(roles.getRoles().contains("kwiestenbiebel"));

        rolesController.associateUser("vozen", "pipo");
        rolesController.associateUser("kwiestenbiebel", "pipo");

        roles = rolesController.getUser("pipo");
        assertEquals(2, roles.getRoles().size());
        assertTrue(roles.getRoles().contains("vozen"));
        assertTrue(roles.getRoles().contains("kwiestenbiebel"));

        rolesController.disassociateUser("kwiestenbiebel", "pipo");
        roles = rolesController.getUser("pipo");
        assertEquals(1, roles.getRoles().size());
        assertTrue(roles.getRoles().contains("vozen"));
        assertFalse(roles.getRoles().contains("kwiestenbiebel"));

        rolesController.delete("kwiestenbiebel");
        assertEquals(1, roles.getRoles().size());
        assertTrue(roles.getRoles().contains("vozen"));
        assertFalse(roles.getRoles().contains("kwiestenbiebel"));

        usersController.deleteUser(USER_SERVICE, "pipo");
        users = usersController.getUsers(USER_SERVICE);
        found = false;
        for (JaxbUser u : users.getUsers()) {
            if ("pipo".equals(u.getUserName())) {
                found = true;
            }
        }
        assertFalse(found);

        // not found errors - will be translated by spring exception handler to code 404
        boolean notfound = false;
        try {
            usersController.getUsers("blabla");
        } catch (IllegalArgumentException e) {
            notfound = true;
        }
        assertTrue(notfound);

        notfound = false;
        try {
            usersController.getGroupsFromUser(USER_SERVICE, "niemand");
        } catch (IllegalArgumentException e) {
            notfound = true;
        }
        assertTrue(notfound);

        notfound = false;
        try {
            usersController.getUsersFromGroup(USER_SERVICE, "onbestaand");
        } catch (IllegalArgumentException e) {
            notfound = true;
        }
        assertTrue(notfound);

        notfound = false;
        try {
            rolesController.delete("onbestaand");
        } catch (IllegalArgumentException e) {
            notfound = true;
        }
        assertTrue(notfound);
    }

    @Test
    public void testUserRolesEndpoint() throws Exception {
        // validate that the user roles endpoint is returning the correct xml using admin user
        String userXML = getAsString("rest/security/roles/user/admin.xml");
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><roles><role>ADMIN</role></roles>",
                userXML.trim());
    }

    @Test
    public void testRolesEndpoint() throws Exception {
        // validate that the roles endpoint is returning the correct xml
        String userXML = getAsString("rest/security/roles");
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><roles><role>ADMIN</role><role>GROUP_ADMIN</role></roles>",
                userXML.trim());
    }

    @Test
    public void testUserEndpoint() throws Exception {
        // validate that the users endpoint is returning the correct xml
        String userXML = getAsString("rest/security/usergroup/service/" + USER_SERVICE + "/users");
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><users><user><enabled>true</enabled><userName>admin</userName></user></users>",
                userXML.trim());
    }

    @Test
    public void testGroupEndpoint() throws Exception {
        // validate that the groups endpoint is returning the correct xml
        usersController.insertGroup(USER_SERVICE, "clowns");
        usersController.insertGroup(USER_SERVICE, "circus");
        String userXML = getAsString("rest/security/usergroup/service/" + USER_SERVICE + "/groups");
        assertEquals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><groups><group>circus</group><group>clowns</group></groups>",
                userXML.trim());
    }

    @Test
    public void testGroups() throws PasswordPolicyException, IOException {

        JaxbUser user = new JaxbUser();
        user.setUserName("pipo");
        user.setPassword("secret");
        user.setEnabled(true);

        usersController.insertUser(USER_SERVICE, user);
        usersController.insertGroup(USER_SERVICE, "clowns");
        usersController.insertGroup(USER_SERVICE, "circus");
        usersController.associateUserToGroup(USER_SERVICE, "pipo", "clowns");
        usersController.associateUserToGroup(USER_SERVICE, "pipo", "circus");

        rolesController.insert("vozen");
        rolesController.insert("kwiestenbiebel");
        JaxbRoleList roles = rolesController.get();
        assertTrue(roles.getRoles().contains("vozen"));
        assertTrue(roles.getRoles().contains("kwiestenbiebel"));

        rolesController.associateGroup("vozen", "clowns");
        rolesController.associateGroup("kwiestenbiebel", "clowns");

        roles = rolesController.getGroup("clowns");
        assertEquals(2, roles.getRoles().size());
        assertTrue(roles.getRoles().contains("vozen"));
        assertTrue(roles.getRoles().contains("kwiestenbiebel"));

        rolesController.disassociateGroup("kwiestenbiebel", "clowns");
        roles = rolesController.getGroup("clowns");
        assertEquals(1, roles.getRoles().size());
        assertTrue(roles.getRoles().contains("vozen"));
        assertFalse(roles.getRoles().contains("kwiestenbiebel"));

        rolesController.associateGroup("default", "vozen", "circus");
        rolesController.associateGroup("default", "kwiestenbiebel", "circus");

        roles = rolesController.getGroup("default", "circus");
        assertEquals(2, roles.getRoles().size());
        assertTrue(roles.getRoles().contains("vozen"));
        assertTrue(roles.getRoles().contains("kwiestenbiebel"));

        rolesController.disassociateGroup("default", "kwiestenbiebel", "circus");
        roles = rolesController.getGroup("default", "circus");
        assertEquals(1, roles.getRoles().size());
        assertTrue(roles.getRoles().contains("vozen"));
        assertFalse(roles.getRoles().contains("kwiestenbiebel"));
    }

    @Test
    public void testDoubleUserGroupAssociation() throws PasswordPolicyException, IOException {

        JaxbUser user = new JaxbUser();
        user.setUserName("pipo");
        user.setPassword("secret");
        user.setEnabled(true);

        usersController.insertUser(USER_SERVICE, user);
        usersController.insertGroup(USER_SERVICE, "clowns");
        usersController.associateUserToGroup(USER_SERVICE, "pipo", "clowns");

        RestException exception =
                assertThrows(
                        RestException.class,
                        () -> {
                            usersController.associateUserToGroup(USER_SERVICE, "pipo", "clowns");
                        });

        assertEquals("Username already associated with this groupname", exception.getMessage());
        assertEquals(HttpStatus.OK, exception.getStatus());
    }
}
