/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import static org.junit.Assert.*;

import org.apache.wicket.Page;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.group.NewGroupPage;
import org.geoserver.security.web.role.NewRolePage;
import org.junit.Test;

public abstract class AbstractUserPageTest extends AbstractSecurityWicketTestSupport {

    protected AbstractUserPage page;
    protected FormTester form;

    protected abstract void initializeTester();

    @Test
    public void testReadOnlyRoleService() throws Exception {
        doInitialize();
        activateRORoleService();
        initializeTester();
        assertTrue(page.userGroupPalette.isEnabled());
    }

    protected void doInitialize() throws Exception {
        initializeForXML();
    }

    protected void doTestPasswordsDontMatch(Class<? extends Page> pageClass) throws Exception {
        doInitialize();
        initializeTester();
        newFormTester();
        form.setValue("username", "user");
        form.setValue("password", "pwd1");
        form.setValue("confirmPassword", "pwd2");
        form.submit("save");

        assertTrue(testErrorMessagesWithRegExp(".*[Pp]assword.*"));
        tester.assertRenderedPage(pageClass);
    }

    protected void newFormTester() {
        form = tester.newFormTester("form");
    }

    protected void addNewRole(String roleName) {
        // add a role on the fly
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);

        FormTester roleform = tester.newFormTester("form");
        roleform.setValue("name", "ROLE_NEW");
        roleform.submit("save");

        newFormTester();
    }

    protected void addNewGroup(String groupName) {
        // add a role on the fly
        form.submit("groups:addGroup");
        tester.assertRenderedPage(NewGroupPage.class);
        FormTester groupform = tester.newFormTester("form");
        groupform.setValue("groupname", groupName);
        groupform.submit("save");
        newFormTester();
    }

    protected void assignRole(String roleName) throws Exception {
        form.setValue("roles:palette:recorder", gaService.getRoleByName(roleName).getAuthority());
        form.submit();
        tester.executeAjaxEvent("form:roles:palette:recorder", "change");
        newFormTester();
        form.setValue("roles:palette:recorder", gaService.getRoleByName(roleName).getAuthority());
    }

    protected void assignGroup(String groupName) throws Exception {
        String theName = ugService.getGroupByGroupname(groupName).getGroupname();
        form.setValue("groups:palette:recorder", theName);
        form.submit();
        tester.executeAjaxEvent("form:groups:palette:recorder", "change");
        newFormTester();
        form.setValue("groups:palette:recorder", theName);
    }

    protected void openCloseRolePanel(Class<? extends Page> responseClass) {
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(responseClass);
        newFormTester();
    }

    protected void openCloseGroupPanel(Class<? extends Page> responseClass) {
        form.submit("groups:addGroup");
        tester.assertRenderedPage(NewGroupPage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(responseClass);
        newFormTester();
    }

    protected void addUserProperty(String key, String value) {
        tester.executeAjaxEvent("form:properties:add", "click");
        // newFormTester();

        form.setValue("properties:container:list:0:key", key);
        form.setValue("properties:container:list:0:value", value);
    }

    protected void assertCalculatedRoles(String[] roles) throws Exception {
        for (int i = 0; i < roles.length; i++)
            tester.assertModelValue(
                    "form:calculatedRolesContainer:calculatedRoles:" + i,
                    gaService.getRoleByName(roles[i]));
    }
}
