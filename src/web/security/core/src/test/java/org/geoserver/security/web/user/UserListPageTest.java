/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.SortedSet;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.AbstractTabbedListPageTest;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Test;

public class UserListPageTest extends AbstractTabbedListPageTest<GeoServerUser> {
    protected boolean withRoles = false;

    @Override
    protected AbstractSecurityPage listPage(String serviceName) {
        AbstractSecurityPage result = initializeForUGServiceNamed(serviceName);
        tester.clickLink(getTabbedPanelPath() + ":tabs-container:tabs:1:link", true);
        return result;
    }

    @Override
    protected Page newPage(AbstractSecurityPage page, Object... params) {
        if (params.length == 0)
            return new NewUserPage(getUserGroupServiceName()).setReturnPage(page);
        else return new NewUserPage((String) params[0]).setReturnPage(page);
    }

    @Override
    protected Page editPage(AbstractSecurityPage page, Object... params) {
        if (params.length == 0) {
            return new EditUserPage(getUserGroupServiceName(), new GeoServerUser("dummyuser"))
                    .setReturnPage(page);
        }

        if (params.length == 1)
            return new EditUserPage(getUserGroupServiceName(), (GeoServerUser) params[0])
                    .setReturnPage(page);
        else
            return new EditUserPage((String) params[0], (GeoServerUser) params[1])
                    .setReturnPage(page);
    }

    @Override
    protected String getSearchString() throws Exception {
        GeoServerUser u = ugService.getUserByUsername("user1");
        assertNotNull(u);
        return u.getUsername();
    }

    @Override
    protected Property<GeoServerUser> getEditProperty() {
        return UserListProvider.USERNAME;
    }

    @Override
    protected boolean checkEditForm(String objectString) {
        return objectString.equals(
                tester.getComponentFromLastRenderedPage("form:username").getDefaultModelObject());
    }

    @Test
    public void testReadOnlyService() throws Exception {
        doInitialize();
        tester.startPage(listPage(getUserGroupServiceName()));
        tester.assertVisible(getRemoveLink().getPageRelativePath());
        tester.assertVisible(getRemoveLinkWithRoles().getPageRelativePath());
        tester.assertVisible(getAddLink().getPageRelativePath());

        activateRORoleService();
        tester.startPage(listPage(getUserGroupServiceName()));
        tester.assertVisible(getRemoveLink().getPageRelativePath());
        tester.assertInvisible(getRemoveLinkWithRoles().getPageRelativePath());
        tester.assertVisible(getAddLink().getPageRelativePath());

        activateROUGService();
        tester.startPage(listPage(getROUserGroupServiceName()));
        tester.assertInvisible(getRemoveLink().getPageRelativePath());
        tester.assertInvisible(getAddLink().getPageRelativePath());
        tester.assertInvisible(getRemoveLinkWithRoles().getPageRelativePath());
    }

    @Override
    protected void simulateDeleteSubmit() throws Exception {
        SelectionUserRemovalLink link =
                (SelectionUserRemovalLink) (withRoles ? getRemoveLinkWithRoles() : getRemoveLink());
        Method m =
                link.delegate
                        .getClass()
                        .getDeclaredMethod("onSubmit", AjaxRequestTarget.class, Component.class);
        m.invoke(link.delegate, null, null);

        SortedSet<GeoServerUser> users = ugService.getUsers();
        assertEquals(0, users.size());
        if (withRoles) assertEquals(0, gaService.getRolesForUser("user1").size());
        else assertEquals(2, gaService.getRolesForUser("user1").size());
    }

    @Test
    public void testRemoveWithRoles() throws Exception {
        withRoles = true;
        // initializeForXML();
        // insertValues();
        addAdditonalData();
        doRemove(getTabbedPanelPath() + ":panel:header:removeSelectedWithRoles");
    }

    @Override
    protected String getTabbedPanelPath() {
        // return "UserGroupTabbedPage";
        return "panel:panel";
    }

    @Override
    protected String getServiceName() {
        return getUserGroupServiceName();
    }
}
