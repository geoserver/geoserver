/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.group;

import static org.junit.Assert.*;

import java.util.SortedSet;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.role.NewRolePage;
import org.junit.Before;
import org.junit.Test;

public class EditGroupPageTest extends AbstractSecurityWicketTestSupport {

    EditGroupPage page;

    @Before
    public void init() throws Exception {
        doInitialize();
        clearServices();

        deactivateRORoleService();
        deactivateROUGService();
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

        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(
                page =
                        (EditGroupPage)
                                new EditGroupPage(
                                                getUserGroupServiceName(),
                                                ugService.getGroupByGroupname("group1"))
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditGroupPage.class);

        tester.assertRenderedPage(EditGroupPage.class);
        assertFalse(tester.getComponentFromLastRenderedPage("form:groupname").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:enabled").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:roles").isEnabled());
        tester.assertVisible("form:save");

        tester.assertModelValue("form:groupname", "group1");
        tester.assertModelValue("form:enabled", Boolean.TRUE);

        FormTester form = tester.newFormTester("form");
        form.setValue("enabled", Boolean.FALSE);

        // add a role on the fly
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        form = tester.newFormTester("form");
        form.setValue("name", "ROLE_NEW");
        form.submit("save");

        // assign the new role to the new group
        form = tester.newFormTester("form");
        tester.assertRenderedPage(EditGroupPage.class);
        form.setValue("roles:palette:recorder", gaService.getRoleByName("ROLE_NEW").getAuthority());

        // reopen new role dialog again to ensure that the current state is not lost
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(EditGroupPage.class);

        // now save
        form = tester.newFormTester("form");
        form.submit("save");

        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.assertErrorMessages(new String[0]);

        GeoServerUserGroup group = ugService.getGroupByGroupname("group1");
        assertNotNull(group);
        assertFalse(group.isEnabled());
        SortedSet<GeoServerRole> roleList = gaService.getRolesForGroup("group1");
        assertEquals(1, roleList.size());
        assertEquals("ROLE_NEW", roleList.iterator().next().getAuthority());
    }

    @Test
    public void testReadOnlyUserGroupService() throws Exception {
        doTestReadOnlyUserGroupService();
    }

    protected void doTestReadOnlyUserGroupService() throws Exception {
        insertValues();
        activateROUGService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getROUserGroupServiceName());
        tester.startPage(
                page =
                        (EditGroupPage)
                                new EditGroupPage(
                                                getROUserGroupServiceName(),
                                                ugService.getGroupByGroupname("group1"))
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditGroupPage.class);
        assertFalse(tester.getComponentFromLastRenderedPage("form:groupname").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:enabled").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:roles").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:save").isEnabled());

        FormTester form = tester.newFormTester("form");
        form.setValue("roles:palette:recorder", gaService.getRoleByName("ROLE_WFS").getAuthority());
        form.submit("save");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        SortedSet<GeoServerRole> roleList = gaService.getRolesForGroup("group1");
        assertEquals(1, roleList.size());
        assertEquals("ROLE_WFS", roleList.iterator().next().getAuthority());
    }

    @Test
    public void testReadOnlyRoleService() throws Exception {
        doTestReadOnlyRoleService();
    }

    protected void doTestReadOnlyRoleService() throws Exception {
        insertValues();

        activateRORoleService();
        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getUserGroupServiceName());
        tester.startPage(
                page =
                        (EditGroupPage)
                                new EditGroupPage(
                                                getUserGroupServiceName(),
                                                ugService.getGroupByGroupname("group1"))
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditGroupPage.class);
        assertFalse(tester.getComponentFromLastRenderedPage("form:groupname").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:enabled").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:roles").isEnabled());
        tester.assertVisible("form:save");

        FormTester form = tester.newFormTester("form");
        form.setValue("enabled", Boolean.FALSE);
        form.submit("save");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        GeoServerUserGroup group = ugService.getGroupByGroupname("group1");
        assertNotNull(group);
        assertFalse(group.isEnabled());
    }

    @Test
    public void testAllServicesReadOnly() throws Exception {
        activateROUGService();
        activateRORoleService();

        AbstractSecurityPage returnPage = initializeForUGServiceNamed(getROUserGroupServiceName());
        tester.startPage(
                page =
                        (EditGroupPage)
                                new EditGroupPage(
                                                getROUserGroupServiceName(),
                                                ugService.getGroupByGroupname("group1"))
                                        .setReturnPage(returnPage));
        tester.assertRenderedPage(EditGroupPage.class);
        assertFalse(tester.getComponentFromLastRenderedPage("form:groupname").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:enabled").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:roles").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:save").isEnabled());
    }
}
