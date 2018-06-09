/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import static org.junit.Assert.*;

import java.util.SortedSet;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.web.AbstractSecurityPage;
import org.junit.Before;
import org.junit.Test;

public class EditUserPageTest extends AbstractUserPageTest {

    GeoServerUser current;

    @Before
    public void init() throws Exception {
        doInitialize();
        clearServices();

        deactivateRORoleService();
        deactivateROUGService();
    }

    @Test
    public void testFill() throws Exception {
        doTestFill();
    }

    protected void doTestFill() throws Exception {
        insertValues();
        addAdditonalData();

        current = ugService.getUserByUsername("user1");
        initializeTester();
        tester.assertRenderedPage(EditUserPage.class);

        assertFalse(tester.getComponentFromLastRenderedPage("form:username").isEnabled());

        tester.assertModelValue("form:username", "user1");
        GeoServerPasswordEncoder encoder =
                (GeoServerPasswordEncoder)
                        GeoServerExtensions.bean(ugService.getPasswordEncoderName());
        String enc =
                (String)
                        tester.getComponentFromLastRenderedPage("form:password")
                                .getDefaultModelObject();
        assertTrue(encoder.isPasswordValid(enc, "11111", null));
        enc =
                (String)
                        tester.getComponentFromLastRenderedPage("form:confirmPassword")
                                .getDefaultModelObject();
        assertTrue(encoder.isPasswordValid(enc, "11111", null));
        tester.assertModelValue("form:enabled", Boolean.TRUE);

        newFormTester();
        form.setValue("enabled", false);
        // addUserProperty("coord", "10 10");

        assertTrue(page.userGroupPalette.isEnabled());
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        tester.debugComponentTrees();
        addNewRole("ROLE_NEW");
        tester.assertRenderedPage(EditUserPage.class);

        assignRole("ROLE_NEW");

        // reopen new role dialog again to ensure that the current state is not lost
        openCloseRolePanel(EditUserPage.class);
        assertCalculatedRoles(
                new String[] {"ROLE_AUTHENTICATED", "ROLE_NEW", "ROLE_WFS", "ROLE_WMS"});

        addNewGroup("testgroup");
        assignGroup("testgroup");

        openCloseGroupPanel(EditUserPage.class);

        assertCalculatedRoles(new String[] {"ROLE_NEW"});
        // print(tester.getLastRenderedPage(),true,true);
        form.submit("save");

        tester.assertErrorMessages(new String[0]);
        // tester.assertRenderedPage(UserGroupTabbedPage.class);

        GeoServerUser user = ugService.getUserByUsername("user1");
        assertNotNull(user);
        assertFalse(user.isEnabled());

        // assertEquals(1,user.getProperties().size());
        // assertEquals("10 10",user.getProperties().get("coord"));
        SortedSet<GeoServerUserGroup> groupList = ugService.getGroupsForUser(user);
        assertEquals(1, groupList.size());
        assertTrue(groupList.contains(ugService.getGroupByGroupname("testgroup")));

        SortedSet<GeoServerRole> roleList = gaService.getRolesForUser("user1");
        assertEquals(1, roleList.size());
        assertTrue(roleList.contains(gaService.getRoleByName("ROLE_NEW")));
    }

    @Test
    public void testReadOnlyUserGroupService() throws Exception {
        initializeForXML();
        doTestReadOnlyUserGroupService();
    }

    protected void doTestReadOnlyUserGroupService() throws Exception {
        insertValues();
        activateROUGService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getROUserGroupServiceName());
        current = ugService.getUserByUsername("user1");
        tester.startPage(
                page =
                        (AbstractUserPage)
                                new EditUserPage(getROUserGroupServiceName(), current)
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditUserPage.class);

        assertFalse(tester.getComponentFromLastRenderedPage("form:username").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:password").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:confirmPassword").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:enabled").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:roles").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:groups").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());
        tester.assertVisible("form:save");

        newFormTester();
        assignRole(GeoServerRole.ADMIN_ROLE.getAuthority());
        form.submit("save");

        SortedSet<GeoServerRole> roleList = gaService.getRolesForUser("user1");
        assertEquals(1, roleList.size());
        assertTrue(
                roleList.contains(
                        gaService.getRoleByName(GeoServerRole.ADMIN_ROLE.getAuthority())));
    }

    @Test
    public void testReadOnlyRoleService() throws Exception {
        doTestReadOnlyRoleService();
    }

    protected void doTestReadOnlyRoleService() throws Exception {
        insertValues();
        activateRORoleService();
        current = ugService.getUserByUsername("user1");
        initializeTester();
        tester.assertRenderedPage(EditUserPage.class);
        assertFalse(tester.getComponentFromLastRenderedPage("form:username").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:password").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:confirmPassword").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:enabled").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:roles").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:groups").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());

        tester.assertVisible("form:save");

        newFormTester();
        form.setValue("enabled", Boolean.FALSE);
        form.submit("save");

        GeoServerUser user = ugService.getUserByUsername("user1");
        assertNotNull(user);
        assertFalse(user.isEnabled());
    }

    @Test
    public void testAllServicesReadOnly() throws Exception {
        insertValues();
        activateROUGService();
        activateRORoleService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getROUserGroupServiceName());
        current = ugService.getUserByUsername("user1");
        tester.startPage(
                page =
                        (AbstractUserPage)
                                new EditUserPage(getROUserGroupServiceName(), current)
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditUserPage.class);
        assertFalse(tester.getComponentFromLastRenderedPage("form:username").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:password").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:confirmPassword").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:enabled").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:roles").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:groups").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());
        // tester.assertInvisible("form:save");
    }

    @Override
    protected void initializeTester() {
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(
                page =
                        (AbstractUserPage)
                                new EditUserPage(getUserGroupServiceName(), current)
                                        .setReturnPage(returnPage));
    }

    @Test
    public void testPasswordsDontMatch() throws Exception {
        insertValues();
        current = ugService.getUserByUsername("user1");
        super.doTestPasswordsDontMatch(EditUserPage.class);
    }
}
