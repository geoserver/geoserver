/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.*;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.util.URLs;

/**
 * Base test class
 *
 * @author christian
 */
public abstract class AbstractSecurityServiceTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpSecurity();
    }

    public GeoServerUserGroupService createUserGroupService(String name) throws Exception {
        return null;
    }

    public GeoServerRoleService createRoleService(String name) throws Exception {
        return null;
    }

    protected boolean isNewRoleService(String name) throws IOException {
        return !(getSecurityManager().listRoleServices().contains(name));
    }

    protected boolean isNewUGService(String name) throws IOException {
        return !(getSecurityManager().listUserGroupServices().contains(name));
    }

    public GeoServerUserGroupStore createStore(GeoServerUserGroupService service)
            throws IOException {
        return service.createStore();
    }

    public GeoServerRoleStore createStore(GeoServerRoleService service) throws IOException {
        return service.createStore();
    }

    protected void checkEmpty(GeoServerRoleService roleService) throws IOException {
        assertEquals(0, roleService.getRoles().size());
        assertEquals(0, roleService.getRoleCount());
    }

    public void insertValues(GeoServerRoleStore roleStore) throws IOException {

        GeoServerRole role_admin =
                roleStore.createRoleObject(GeoServerRole.ADMIN_ROLE.getAuthority());
        GeoServerRole role_auth = roleStore.createRoleObject("ROLE_AUTHENTICATED");
        GeoServerRole role_wfs = roleStore.createRoleObject("ROLE_WFS");
        GeoServerRole role_wms = roleStore.createRoleObject("ROLE_WMS");

        role_auth.getProperties().put("employee", "");
        role_auth.getProperties().put("bbox", "lookupAtRuntime");

        roleStore.addRole(role_admin);
        roleStore.addRole(role_auth);
        roleStore.addRole(role_wfs);
        roleStore.addRole(role_wms);

        roleStore.setParentRole(role_wms, role_auth);
        roleStore.setParentRole(role_wfs, role_auth);

        roleStore.associateRoleToUser(role_admin, "admin");
        roleStore.associateRoleToUser(role_wms, "user1");
        roleStore.associateRoleToUser(role_wfs, "user1");

        roleStore.associateRoleToGroup(role_wms, "g_wms");
        roleStore.associateRoleToGroup(role_wfs, "g_wfs");
        roleStore.associateRoleToGroup(role_wms, "g_all");
        roleStore.associateRoleToGroup(role_wfs, "g_all");
    }

    public void removeValues(GeoServerRoleStore roleStore) throws IOException {
        GeoServerRole role_auth = roleStore.createRoleObject("ROLE_AUTHENTICATED");
        GeoServerRole role_wfs = roleStore.getRoleByName("ROLE_WFS");
        roleStore.removeRole(role_wfs);
        roleStore.removeRole(role_auth);
    }

    public void modifyValues(GeoServerRoleStore roleStore) throws IOException {

        GeoServerRole role_auth = roleStore.getRoleByName("ROLE_AUTHENTICATED");
        GeoServerRole role_wfs = roleStore.getRoleByName("ROLE_WFS");
        GeoServerRole role_wms = roleStore.getRoleByName("ROLE_WMS");

        role_auth.getProperties().remove("bbox");
        role_auth.getProperties().setProperty("employee", "4711");
        roleStore.updateRole(role_auth);

        role_wms.getProperties().setProperty("envelope", "10 10 20 20");
        roleStore.updateRole(role_wms);

        roleStore.disAssociateRoleFromGroup(role_wfs, "g_all");
        roleStore.disAssociateRoleFromUser(role_wfs, "user1");
        roleStore.setParentRole(role_wms, null);
        roleStore.setParentRole(role_wfs, role_wms);
    }

    protected void checkValuesRemoved(GeoServerRoleService roleService) throws IOException {
        GeoServerRole role_admin =
                roleService.getRoleByName(GeoServerRole.ADMIN_ROLE.getAuthority());
        GeoServerRole role_wms = roleService.getRoleByName("ROLE_WMS");

        assertEquals(2, roleService.getRoles().size());
        assertEquals(2, roleService.getRoleCount());
        assertTrue(roleService.getRoles().contains(role_admin));
        assertTrue(roleService.getRoles().contains(role_wms));

        assertNull(roleService.getParentRole(role_wms));
        assertEquals(1, roleService.getRolesForUser("user1").size());
        assertTrue(roleService.getRolesForUser("user1").contains(role_wms));

        assertEquals(0, roleService.getRolesForGroup("g_wfs").size());
        assertEquals(1, roleService.getRolesForGroup("g_all").size());
        assertTrue(roleService.getRolesForGroup("g_all").contains(role_wms));
    }

    protected void checkValuesModified(GeoServerRoleService roleService) throws IOException {
        GeoServerRole role_auth = roleService.getRoleByName("ROLE_AUTHENTICATED");
        GeoServerRole role_wms = roleService.getRoleByName("ROLE_WMS");
        GeoServerRole role_wfs = roleService.getRoleByName("ROLE_WFS");

        assertEquals(1, role_auth.getProperties().size());
        assertEquals("4711", role_auth.getProperties().get("employee"));
        assertEquals(1, role_wms.getProperties().size());
        assertEquals("10 10 20 20", role_wms.getProperties().get("envelope"));
        assertEquals(0, role_wfs.getProperties().size());

        for (GeoServerRole role : roleService.getRoles()) {
            if ("ROLE_AUTHENTICATED".equals(role.getAuthority())) {
                assertEquals(1, role.getProperties().size());
                assertEquals("4711", role.getProperties().get("employee"));
            } else if ("ROLE_WMS".equals(role.getAuthority())) {
                assertEquals(1, role.getProperties().size());
                assertEquals("10 10 20 20", role.getProperties().get("envelope"));
            } else {
                assertEquals(0, role.getProperties().size());
            }
        }
        assertEquals(1, roleService.getGroupNamesForRole(role_wfs).size());
        assertTrue(roleService.getGroupNamesForRole(role_wfs).contains("g_wfs"));
        assertEquals(0, roleService.getUserNamesForRole(role_wfs).size());

        assertEquals(1, roleService.getRolesForGroup("g_all").size());
        assertTrue(roleService.getRolesForGroup("g_all").contains(role_wms));
        GeoServerRole role = roleService.getRolesForGroup("g_all").iterator().next();
        assertEquals(1, role.getProperties().size());
        assertEquals("10 10 20 20", role.getProperties().get("envelope"));

        assertEquals(1, roleService.getRolesForUser("user1").size());
        assertTrue(roleService.getRolesForUser("user1").contains(role_wms));
        role = roleService.getRolesForUser("user1").iterator().next();
        assertEquals(1, role.getProperties().size());
        assertEquals("10 10 20 20", role.getProperties().get("envelope"));

        assertNull(roleService.getParentRole(role_wms));
        assertEquals(role_wms, roleService.getParentRole(role_wfs));
    }

    protected void checkValuesInserted(GeoServerRoleService roleService) throws IOException {

        GeoServerRole role_auth = roleService.getRoleByName("ROLE_AUTHENTICATED");
        GeoServerRole role_wfs = roleService.getRoleByName("ROLE_WFS");
        GeoServerRole role_wms = roleService.getRoleByName("ROLE_WMS");
        GeoServerRole role_admin =
                roleService.getRoleByName(GeoServerRole.ADMIN_ROLE.getAuthority());

        assertEquals(4, roleService.getRoles().size());
        assertEquals(4, roleService.getRoleCount());
        assertTrue(roleService.getRoles().contains(role_admin));
        assertTrue(roleService.getRoles().contains(role_auth));
        assertTrue(roleService.getRoles().contains(role_wfs));
        assertTrue(roleService.getRoles().contains(role_wms));

        assertNull(roleService.getRoleByName("xxx"));

        for (GeoServerRole role : roleService.getRoles()) {
            if (role_auth.getAuthority().equals(role.getAuthority())) {
                assertEquals(2, role.getProperties().size());
                assertEquals(role.getProperties().getProperty("employee"), "");
                assertEquals(role.getProperties().getProperty("bbox"), "lookupAtRuntime");
            } else {
                assertEquals(0, role.getProperties().size());
            }
        }

        assertEquals(0, role_admin.getProperties().size());
        assertEquals(0, role_wfs.getProperties().size());
        assertEquals(0, role_wms.getProperties().size());

        assertEquals(2, role_auth.getProperties().size());
        assertEquals(role_auth.getProperties().getProperty("employee"), "");
        assertEquals(role_auth.getProperties().getProperty("bbox"), "lookupAtRuntime");

        assertNull(roleService.getParentRole(role_admin));
        assertNull(roleService.getParentRole(role_auth));
        assertEquals(role_auth, roleService.getParentRole(role_wms));
        assertEquals(role_auth, roleService.getParentRole(role_wfs));
        assertEquals(2, roleService.getParentRole(role_wfs).getProperties().size());
        assertEquals(
                roleService.getParentRole(role_wfs).getProperties().getProperty("employee"), "");
        assertEquals(
                roleService.getParentRole(role_wfs).getProperties().getProperty("bbox"),
                "lookupAtRuntime");

        assertEquals(0, roleService.getRolesForUser("xxx").size());
        assertEquals(1, roleService.getRolesForUser("admin").size());
        assertTrue(roleService.getRolesForUser("admin").contains(GeoServerRole.ADMIN_ROLE));

        assertEquals(2, roleService.getRolesForUser("user1").size());
        assertTrue(roleService.getRolesForUser("user1").contains(role_wfs));
        assertTrue(roleService.getRolesForUser("user1").contains(role_wms));

        assertEquals(0, roleService.getRolesForGroup("xxx").size());

        assertEquals(1, roleService.getRolesForGroup("g_wfs").size());
        assertTrue(roleService.getRolesForGroup("g_wfs").contains(role_wfs));

        assertEquals(1, roleService.getRolesForGroup("g_wms").size());
        assertTrue(roleService.getRolesForGroup("g_wms").contains(role_wms));

        assertEquals(2, roleService.getRolesForGroup("g_all").size());
        assertTrue(roleService.getRolesForGroup("g_all").contains(role_wfs));
        assertTrue(roleService.getRolesForGroup("g_all").contains(role_wms));

        assertEquals(1, roleService.getUserNamesForRole(GeoServerRole.ADMIN_ROLE).size());
        assertTrue(roleService.getUserNamesForRole(GeoServerRole.ADMIN_ROLE).contains("admin"));
        assertEquals(1, roleService.getUserNamesForRole(role_wfs).size());
        assertTrue(roleService.getUserNamesForRole(role_wfs).contains("user1"));
        assertEquals(1, roleService.getUserNamesForRole(role_wms).size());
        assertTrue(roleService.getUserNamesForRole(role_wms).contains("user1"));
        assertEquals(
                0, roleService.getUserNamesForRole(roleService.createRoleObject("xxx")).size());

        assertEquals(2, roleService.getGroupNamesForRole(role_wfs).size());
        assertTrue(roleService.getGroupNamesForRole(role_wfs).contains("g_wfs"));
        assertTrue(roleService.getGroupNamesForRole(role_wfs).contains("g_all"));

        assertEquals(2, roleService.getGroupNamesForRole(role_wms).size());
        assertTrue(roleService.getGroupNamesForRole(role_wms).contains("g_wms"));
        assertTrue(roleService.getGroupNamesForRole(role_wms).contains("g_all"));

        assertEquals(
                0, roleService.getGroupNamesForRole(roleService.createRoleObject("xxx")).size());

        assertEquals(4, roleService.getParentMappings().size());
        assertNull(roleService.getParentMappings().get(GeoServerRole.ADMIN_ROLE.getAuthority()));
        assertNull(roleService.getParentMappings().get(role_auth.getAuthority()));
        assertEquals(
                roleService.getParentMappings().get(role_wfs.getAuthority()),
                role_auth.getAuthority());
        assertEquals(
                roleService.getParentMappings().get(role_wms.getAuthority()),
                role_auth.getAuthority());
    }

    protected void checkEmpty(GeoServerUserGroupService userService) throws IOException {
        assertEquals(0, userService.getUsers().size());
        assertEquals(0, userService.getUserGroups().size());
        assertEquals(0, userService.getUserCount());
        assertEquals(0, userService.getGroupCount());
    }

    protected void checkValuesInserted(GeoServerUserGroupService userGroupService)
            throws IOException {
        assertEquals(5, userGroupService.getUsers().size());
        assertEquals(5, userGroupService.getUserCount());

        GeoServerUser admin = userGroupService.getUserByUsername(GeoServerUser.ADMIN_USERNAME);
        GeoServerUser user1 = userGroupService.getUserByUsername("user1");
        GeoServerUser user2 = userGroupService.getUserByUsername("user2");
        GeoServerUser disableduser = userGroupService.getUserByUsername("disableduser");
        GeoServerUser groupAdminUser = userGroupService.getUserByUsername("groupAdminUser");

        assertNull(userGroupService.getUserByUsername("xxx"));

        assertTrue(userGroupService.getUsers().contains(admin));
        assertTrue(userGroupService.getUsers().contains(user1));
        assertTrue(userGroupService.getUsers().contains(user2));
        assertTrue(userGroupService.getUsers().contains(disableduser));
        assertTrue(userGroupService.getUsers().contains(groupAdminUser));

        // check if properties are loaded too
        for (GeoServerUser user : userGroupService.getUsers()) {
            if (user2.getUsername().equals(user.getUsername())) {
                assertEquals(2, user.getProperties().size());
                assertEquals(user.getProperties().getProperty("mail"), "user2@gmx.com");
                assertEquals(user.getProperties().getProperty("tel"), "12-34-38");
            } else {
                assertEquals(0, user.getProperties().size());
            }
        }

        assertTrue(admin.isEnabled());
        assertTrue(user1.isEnabled());
        assertTrue(user2.isEnabled());
        assertFalse(disableduser.isEnabled());
        assertTrue(groupAdminUser.isEnabled());

        /*assertFalse(user1.isGroupAdmin());
        assertFalse(user2.isGroupAdmin());
        assertTrue(groupAdminUser.isGroupAdmin());*/

        GeoServerMultiplexingPasswordEncoder encoder = getEncoder(userGroupService);
        assertTrue(encoder.isPasswordValid(admin.getPassword(), "geoserver", null));
        assertTrue(encoder.isPasswordValid(user1.getPassword(), "11111", null));
        assertTrue(encoder.isPasswordValid(user2.getPassword(), "22222", null));
        assertTrue(encoder.isPasswordValid(disableduser.getPassword(), "", null));

        assertEquals(0, admin.getProperties().size());
        assertEquals(0, user1.getProperties().size());
        assertEquals(0, disableduser.getProperties().size());

        assertEquals(2, user2.getProperties().size());
        assertEquals(user2.getProperties().getProperty("mail"), "user2@gmx.com");
        assertEquals(user2.getProperties().getProperty("tel"), "12-34-38");

        assertEquals(4, userGroupService.getUserGroups().size());
        assertEquals(4, userGroupService.getGroupCount());
        GeoServerUserGroup admins = userGroupService.getGroupByGroupname("admins");
        GeoServerUserGroup group1 = userGroupService.getGroupByGroupname("group1");
        GeoServerUserGroup group2 = userGroupService.getGroupByGroupname("group2");
        GeoServerUserGroup disabledgroup = userGroupService.getGroupByGroupname("disabledgroup");

        assertNull(userGroupService.getGroupByGroupname("yyy"));

        assertTrue(userGroupService.getUserGroups().contains(admins));
        assertTrue(userGroupService.getUserGroups().contains(group1));
        assertTrue(userGroupService.getUserGroups().contains(group2));
        assertTrue(userGroupService.getUserGroups().contains(disabledgroup));

        assertTrue(admins.isEnabled());
        assertTrue(group1.isEnabled());
        assertTrue(group2.isEnabled());
        assertFalse(disabledgroup.isEnabled());

        assertEquals(2, userGroupService.getUsersForGroup(group1).size());
        assertTrue(userGroupService.getUsersForGroup(group1).contains(user1));
        assertTrue(userGroupService.getUsersForGroup(group1).contains(user2));
        // check if properties are loaded too
        for (GeoServerUser user : userGroupService.getUsersForGroup(group1)) {
            if (user2.getUsername().equals(user.getUsername())) {
                assertEquals(2, user.getProperties().size());
                assertEquals(user.getProperties().getProperty("mail"), "user2@gmx.com");
                assertEquals(user.getProperties().getProperty("tel"), "12-34-38");
            } else {
                assertEquals(0, user.getProperties().size());
            }
        }

        assertEquals(1, userGroupService.getUsersForGroup(admins).size());
        assertTrue(userGroupService.getUsersForGroup(admins).contains(admin));

        assertEquals(1, userGroupService.getUsersForGroup(disabledgroup).size());
        assertTrue(userGroupService.getUsersForGroup(disabledgroup).contains(disableduser));

        assertEquals(1, userGroupService.getGroupsForUser(admin).size());
        assertTrue(userGroupService.getGroupsForUser(admin).contains(admins));

        assertEquals(1, userGroupService.getGroupsForUser(user1).size());
        assertTrue(userGroupService.getGroupsForUser(user1).contains(group1));

        assertEquals(1, userGroupService.getGroupsForUser(user2).size());
        assertTrue(userGroupService.getGroupsForUser(user2).contains(group1));

        assertEquals(1, userGroupService.getGroupsForUser(disableduser).size());
        assertTrue(userGroupService.getGroupsForUser(disableduser).contains(disabledgroup));

        assertEquals(1, userGroupService.getGroupsForUser(groupAdminUser).size());
        assertTrue(userGroupService.getGroupsForUser(groupAdminUser).contains(group2));

        assertEquals(0, userGroupService.getUserCountHavingProperty("unknown"));
        assertEquals(
                userGroupService.getUserCount(),
                userGroupService.getUserCountNotHavingProperty("unknown"));
        assertEquals(0, userGroupService.getUserCountHavingPropertyValue("tel", "123"));

        assertEquals(1, userGroupService.getUserCountHavingProperty("tel"));
        assertEquals(
                userGroupService.getUserCount() - 1,
                userGroupService.getUserCountNotHavingProperty("tel"));
        assertEquals(1, userGroupService.getUserCountHavingPropertyValue("tel", "12-34-38"));

        assertEquals(0, userGroupService.getUsersHavingProperty("unknown").size());
        assertEquals(
                userGroupService.getUserCount(),
                userGroupService.getUsersNotHavingProperty("unknown").size());
        // check if properties are loaded too
        for (GeoServerUser user : userGroupService.getUsersNotHavingProperty("unknown")) {
            if (user2.getUsername().equals(user.getUsername())) {
                assertEquals(2, user.getProperties().size());
                assertEquals(user.getProperties().getProperty("mail"), "user2@gmx.com");
                assertEquals(user.getProperties().getProperty("tel"), "12-34-38");
            } else {
                assertEquals(0, user.getProperties().size());
            }
        }
        assertEquals(0, userGroupService.getUsersHavingPropertyValue("tel", "123").size());

        assertEquals(1, userGroupService.getUsersHavingProperty("mail").size());
        user2 = userGroupService.getUsersHavingProperty("mail").first();
        assertEquals(user2.getProperties().getProperty("mail"), "user2@gmx.com");
        assertEquals(user2.getProperties().getProperty("tel"), "12-34-38");

        assertEquals(
                userGroupService.getUserCount() - 1,
                userGroupService.getUsersNotHavingProperty("mail").size());
        for (GeoServerUser user : userGroupService.getUsersNotHavingProperty("mail")) {
            assertEquals(0, user.getProperties().size());
        }

        assertEquals(1, userGroupService.getUsersHavingPropertyValue("tel", "12-34-38").size());
        user2 = userGroupService.getUsersHavingPropertyValue("tel", "12-34-38").first();
        assertEquals(user2.getProperties().getProperty("mail"), "user2@gmx.com");
        assertEquals(user2.getProperties().getProperty("tel"), "12-34-38");
    }

    protected void checkValuesModified(GeoServerUserGroupService userGroupService)
            throws IOException {
        GeoServerUser disableduser = userGroupService.getUserByUsername("disableduser");
        assertTrue(disableduser.isEnabled());
        GeoServerMultiplexingPasswordEncoder encoder = getEncoder(userGroupService);
        assertTrue(encoder.isPasswordValid(disableduser.getPassword(), "hallo", null));
        assertEquals(1, disableduser.getProperties().size());
        assertEquals("miller", disableduser.getProperties().getProperty("lastname"));

        GeoServerUser user2 = userGroupService.getUserByUsername("user2");
        assertEquals(1, user2.getProperties().size());
        assertEquals("11-22-33", user2.getProperties().getProperty("tel"));

        GeoServerUserGroup disabledgroup = userGroupService.getGroupByGroupname("disabledgroup");
        assertTrue(disabledgroup.isEnabled());

        GeoServerUserGroup group1 = userGroupService.getGroupByGroupname("group1");
        GeoServerUser user1 = userGroupService.getUserByUsername("user1");
        assertEquals(1, userGroupService.getUsersForGroup(group1).size());
        assertTrue(userGroupService.getUsersForGroup(group1).contains(user1));

        assertEquals(0, userGroupService.getGroupsForUser(user2).size());

        assertEquals(0, userGroupService.getUsersHavingProperty("mail").size());
        assertEquals(0, userGroupService.getUsersHavingPropertyValue("tel", "12-34-38").size());
        assertEquals(1, userGroupService.getUsersHavingPropertyValue("tel", "11-22-33").size());
        user2 = userGroupService.getUsersHavingPropertyValue("tel", "11-22-33").first();
        assertEquals("11-22-33", user2.getProperties().getProperty("tel"));
    }

    protected void checkValuesRemoved(GeoServerUserGroupService userGroupService)
            throws IOException {

        GeoServerUser admin = GeoServerUser.createDefaultAdmin();
        GeoServerUser user1 = userGroupService.getUserByUsername("user1");
        GeoServerUser disableduser = userGroupService.getUserByUsername("disableduser");
        GeoServerUser groupAdminUser = userGroupService.getUserByUsername("groupAdminUser");

        assertEquals(4, userGroupService.getUsers().size());
        assertEquals(4, userGroupService.getUserCount());
        assertTrue(userGroupService.getUsers().contains(admin));
        assertTrue(userGroupService.getUsers().contains(user1));
        assertTrue(userGroupService.getUsers().contains(disableduser));
        assertTrue(userGroupService.getUsers().contains(groupAdminUser));

        GeoServerUserGroup admins = userGroupService.getGroupByGroupname("admins");
        GeoServerUserGroup group1 = userGroupService.getGroupByGroupname("group1");
        assertEquals(3, userGroupService.getUserGroups().size());
        assertEquals(3, userGroupService.getGroupCount());
        assertTrue(userGroupService.getUserGroups().contains(admins));
        assertTrue(userGroupService.getUserGroups().contains(group1));

        assertEquals(0, userGroupService.getGroupsForUser(disableduser).size());
        assertEquals(1, userGroupService.getUsersForGroup(group1).size());
        assertTrue(userGroupService.getUsersForGroup(group1).contains(user1));

        assertEquals(0, userGroupService.getUsersHavingProperty("mail").size());
        assertEquals(0, userGroupService.getUsersHavingPropertyValue("tel", "11-22-33").size());
    }

    public void insertValues(GeoServerUserGroupStore userGroupStore) throws Exception {

        GeoServerUser admin =
                userGroupStore.createUserObject(
                        GeoServerUser.ADMIN_USERNAME,
                        GeoServerUser.DEFAULT_ADMIN_PASSWD,
                        GeoServerUser.AdminEnabled);
        GeoServerUser user1 = userGroupStore.createUserObject("user1", "11111", true);
        GeoServerUser user2 = userGroupStore.createUserObject("user2", "22222", true);
        GeoServerUser disableduser = userGroupStore.createUserObject("disableduser", "", false);
        GeoServerUser groupAdminUser =
                userGroupStore.createUserObject("groupAdminUser", "foo", true);
        // groupAdminUser.setGroupAdmin(true);

        user2.getProperties().put("mail", "user2@gmx.com");
        user2.getProperties().put("tel", "12-34-38");

        userGroupStore.addUser(admin);
        userGroupStore.addUser(user1);
        userGroupStore.addUser(user2);
        userGroupStore.addUser(disableduser);
        userGroupStore.addUser(groupAdminUser);

        GeoServerUserGroup admins = userGroupStore.createGroupObject("admins", true);
        GeoServerUserGroup group1 = userGroupStore.createGroupObject("group1", true);
        GeoServerUserGroup group2 = userGroupStore.createGroupObject("group2", true);

        GeoServerUserGroup disabledgroup = userGroupStore.createGroupObject("disabledgroup", false);

        userGroupStore.addGroup(admins);
        userGroupStore.addGroup(group1);
        userGroupStore.addGroup(disabledgroup);
        userGroupStore.addGroup(group2);

        userGroupStore.associateUserToGroup(admin, admins);
        userGroupStore.associateUserToGroup(user1, group1);
        userGroupStore.associateUserToGroup(user2, group1);
        userGroupStore.associateUserToGroup(disableduser, disabledgroup);
        userGroupStore.associateUserToGroup(groupAdminUser, group2);
    }

    public void modifyValues(GeoServerUserGroupStore userGroupStore) throws Exception {
        GeoServerUser disableduser = userGroupStore.getUserByUsername("disableduser");
        disableduser.setEnabled(true);
        disableduser.setPassword("hallo");
        disableduser.getProperties().put("lastname", "miller");
        userGroupStore.updateUser(disableduser);

        GeoServerUser user2 = userGroupStore.getUserByUsername("user2");
        user2.getProperties().remove("mail");
        user2.getProperties().put("tel", "11-22-33");
        userGroupStore.updateUser(user2);

        GeoServerUserGroup disabledgroup = userGroupStore.getGroupByGroupname("disabledgroup");
        disabledgroup.setEnabled(true);
        userGroupStore.updateGroup(disabledgroup);

        GeoServerUserGroup group1 = userGroupStore.getGroupByGroupname("group1");
        userGroupStore.disAssociateUserFromGroup(user2, group1);
    }

    public void removeValues(GeoServerUserGroupStore userGroupStore) throws IOException {
        GeoServerUser user2 = userGroupStore.getUserByUsername("user2");
        if (user2 != null) {
            userGroupStore.removeUser(user2);
        }
        GeoServerUserGroup disabledGroup = userGroupStore.getGroupByGroupname("disabledgroup");
        if (disabledGroup != null) {
            userGroupStore.removeGroup(disabledGroup);
        }
    }

    public static File unpackTestDataDir() throws Exception {
        URL url = AbstractSecurityServiceTest.class.getResource("/data_dir/default");
        if (!"file".equals(url.getProtocol())) {
            // means a dependency is using this directory via a jarfile, copy out manually
            File dataDir = File.createTempFile("data", "live", new File("./target"));
            dataDir.delete();
            dataDir.mkdirs();

            // TODO: instead of harcoding files, dynamically read all subentries from the jar
            // and copy them out
            FileUtils.copyURLToFile(
                    AbstractSecurityServiceTest.class.getResource("/data_dir/default/dummy.txt"),
                    new File(dataDir, "dummy.txt"));
            return dataDir;
        }

        return URLs.urlToFile(url);
    }

    /**
     * Indicates if the test is a JDBC test All test are based on the fact that locking appears at
     * row level. This is not true for JDBC databases - Locking may be based on blocks - Since only
     * a few records are in the table, the whole table my be locked. (lock escalation)
     *
     * <p>Use this method to avoid to aggressive checks
     */
    protected boolean isJDBCTest() {
        return false;
    }

    protected GeoServerMultiplexingPasswordEncoder getEncoder(GeoServerUserGroupService ugService)
            throws IOException {
        return new GeoServerMultiplexingPasswordEncoder(getSecurityManager(), ugService);
    }

    /** Accessor for plain text password encoder. */
    protected GeoServerPlainTextPasswordEncoder getPlainTextPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPlainTextPasswordEncoder.class);
    }

    /** Accessor for digest password encoder. */
    protected GeoServerDigestPasswordEncoder getDigestPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerDigestPasswordEncoder.class);
    }

    /** Accessor for regular (weak encryption) pbe password encoder. */
    protected GeoServerPBEPasswordEncoder getPBEPasswordEncoder() {
        return getSecurityManager()
                .loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, false);
    }

    /** Accessor for strong encryption pbe password encoder. */
    protected GeoServerPBEPasswordEncoder getStrongPBEPasswordEncoder() {
        return getSecurityManager()
                .loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, true);
    }

    /** Accessor for empty password encoder. */
    protected GeoServerEmptyPasswordEncoder getEmptyEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerEmptyPasswordEncoder.class);
    }

    /** Accessor for the geoserver master password. */
    protected String getMasterPassword() {
        return new String(getSecurityManager().getMasterPassword());
    }
}
