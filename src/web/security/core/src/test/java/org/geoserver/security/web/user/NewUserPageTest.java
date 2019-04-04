/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import static org.junit.Assert.*;

import java.util.SortedSet;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.feedback.FeedbackMessage;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.junit.Before;
import org.junit.Test;

public class NewUserPageTest extends AbstractUserPageTest {

    protected void initializeTester() {
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(
                page =
                        (AbstractUserPage)
                                new NewUserPage(getUserGroupServiceName())
                                        .setReturnPage(returnPage));
    }

    @Before
    public void init() throws Exception {
        doInitialize();
        clearServices();
    }

    protected void doInitialize() throws Exception {
        initializeForXML();
    }

    @Test
    public void testFill() throws Exception {
        doTestFill();
    }

    protected void doTestFill() throws Exception {
        insertValues();
        initializeTester();
        tester.assertRenderedPage(NewUserPage.class);

        newFormTester();
        form.setValue("username", "testuser");
        form.setValue("password", "pwd");
        form.setValue("confirmPassword", "pwd");

        assertTrue(((GeoServerUser) page.get("form").getDefaultModelObject()).isEnabled());
        form.setValue("enabled", false);

        // addUserProperty("coord", "10 10");

        assertTrue(page.userGroupPalette.isEnabled());
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        addNewRole("ROLE_NEW");
        tester.assertRenderedPage(NewUserPage.class);
        tester.assertNoErrorMessage();

        assignRole("ROLE_NEW");

        // reopen new role dialog again to ensure that the current state is not lost
        openCloseRolePanel(NewUserPage.class);
        tester.assertNoErrorMessage();

        addNewGroup("testgroup");
        assignGroup("testgroup");
        tester.assertNoErrorMessage();

        openCloseGroupPanel(NewUserPage.class);
        tester.assertNoErrorMessage();

        assertCalculatedRoles(new String[] {"ROLE_NEW"});
        form.submit("save");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        GeoServerUser user = ugService.getUserByUsername("testuser");
        assertNotNull(user);
        assertFalse(user.isEnabled());

        // assertEquals(1,user.getProperties().size());
        // assertEquals("10 10",user.getProperties().get("coord"));
        SortedSet<GeoServerUserGroup> groupList = ugService.getGroupsForUser(user);
        assertEquals(1, groupList.size());
        assertEquals("testgroup", groupList.iterator().next().getGroupname());

        SortedSet<GeoServerRole> roleList = gaService.getRolesForUser("testuser");
        assertEquals(1, roleList.size());
        assertEquals("ROLE_NEW", roleList.iterator().next().getAuthority());
    }

    @Test
    public void testFill3() throws Exception {
        doTestFill3();
    }

    protected void doTestFill3() throws Exception {
        insertValues();
        initializeTester();
        tester.assertRenderedPage(NewUserPage.class);

        newFormTester();
        form.setValue("username", "testuser");
        form.setValue("password", "pwd");
        form.setValue("confirmPassword", "pwd");

        // TODO: this is a dummy call for the formtester to store
        // the above vaules in the model, otherwise we would
        // lose the values due to the assingRole call
        openCloseGroupPanel(NewUserPage.class);

        assignRole("ROLE_WMS");
        assertCalculatedRoles(new String[] {"ROLE_AUTHENTICATED", "ROLE_WMS"});

        form.submit("save");

        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        GeoServerUser user = ugService.getUserByUsername("testuser");
        assertNotNull(user);
        assertTrue(user.isEnabled());

        SortedSet<GeoServerUserGroup> groupList = ugService.getGroupsForUser(user);
        assertEquals(0, groupList.size());

        SortedSet<GeoServerRole> roleList = gaService.getRolesForUser("testuser");
        assertEquals(1, roleList.size());
        assertTrue(roleList.contains(gaService.createRoleObject("ROLE_WMS")));

        user = (GeoServerUser) ugService.loadUserByUsername("testuser");
        assertEquals(2, user.getAuthorities().size());
        assertTrue(
                user.getAuthorities().contains(gaService.createRoleObject("ROLE_AUTHENTICATED")));
        assertTrue(user.getAuthorities().contains(gaService.createRoleObject("ROLE_WMS")));
    }

    @Test
    public void testFill2() throws Exception {
        // initializeForXML();
        doTestFill2();
    }

    protected void doTestFill2() throws Exception {
        insertValues();
        addAdditonalData();
        initializeTester();
        tester.assertRenderedPage(NewUserPage.class);

        newFormTester();
        form.setValue("username", "testuser");
        form.setValue("password", "pwd");
        form.setValue("confirmPassword", "pwd");

        // TODO: this is a dummy call for the formtester to store
        // the above vaules in the model, otherwise we would
        // lose the values due to the assingGroup call
        openCloseGroupPanel(NewUserPage.class);

        assignGroup("group1");
        assertCalculatedRoles(new String[] {"ROLE_AUTHENTICATED", "ROLE_WFS", "ROLE_WMS"});

        form.submit("save");

        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        GeoServerUser user = ugService.getUserByUsername("testuser");
        assertNotNull(user);
        assertTrue(user.isEnabled());

        SortedSet<GeoServerUserGroup> groupList = ugService.getGroupsForUser(user);
        assertEquals(1, groupList.size());
        assertEquals("group1", groupList.iterator().next().getGroupname());

        SortedSet<GeoServerRole> roleList = gaService.getRolesForUser("testuser");
        assertEquals(0, roleList.size());

        user = (GeoServerUser) ugService.loadUserByUsername("testuser");
        assertEquals(3, user.getAuthorities().size());
        assertTrue(
                user.getAuthorities().contains(gaService.createRoleObject("ROLE_AUTHENTICATED")));
        assertTrue(user.getAuthorities().contains(gaService.createRoleObject("ROLE_WFS")));
        assertTrue(user.getAuthorities().contains(gaService.createRoleObject("ROLE_WMS")));
    }

    @Test
    public void testUserNameConflict() throws Exception {
        insertValues();

        initializeTester();
        tester.assertRenderedPage(NewUserPage.class);
        newFormTester();
        form.setValue("username", "user1");
        form.setValue("password", "pwd");
        form.setValue("confirmPassword", "pwd");
        form.submit("save");

        assertTrue(testErrorMessagesWithRegExp(".*user1.*"));
        tester.getMessages(FeedbackMessage.ERROR);
        tester.assertRenderedPage(NewUserPage.class);
    }

    @Test
    public void testInvalidWorkflow() throws Exception {

        activateROUGService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getROUserGroupServiceName());
        boolean fail = true;
        try {
            tester.startPage(
                    page =
                            (AbstractUserPage)
                                    new NewUserPage(getROUserGroupServiceName())
                                            .setReturnPage(returnPage));
        } catch (RuntimeException ex) {
            fail = false;
        }
        if (fail) fail("No runtime exception for read only UserGroupService");
    }

    @Test
    public void testPasswordsDontMatch() throws Exception {
        super.doTestPasswordsDontMatch(NewUserPage.class);
    }
}
