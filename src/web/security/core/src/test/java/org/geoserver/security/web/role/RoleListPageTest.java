/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.SortedSet;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.AbstractTabbedListPageTest;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Test;

public class RoleListPageTest extends AbstractTabbedListPageTest<GeoServerRole> {

    public static final String SECOND_COLUM_PATH = "itemProperties:1:component:link";

    protected String getServiceName() {
        return getRoleServiceName();
    }

    protected AbstractSecurityPage listPage(String roleServiceName) {
        AbstractSecurityPage result = initializeForRoleServiceNamed(roleServiceName);
        tester.clickLink(getTabbedPanelPath() + ":tabs-container:tabs:1:link", true);
        return result;
    }

    protected Page newPage(AbstractSecurityPage page, Object... params) {
        if (params.length == 0)
            return new NewRolePage(getSecurityManager().getActiveRoleService().getName())
                    .setReturnPage(page);
        else return new NewRolePage((String) params[0]).setReturnPage(page);
    }

    protected Page editPage(AbstractSecurityPage page, Object... params) {
        if (params.length == 0) {
            return new EditRolePage(
                            getSecurityManager().getActiveRoleService().getName(),
                            GeoServerRole.ADMIN_ROLE)
                    .setReturnPage(page);
        }
        if (params.length == 1)
            return new EditRolePage(
                            getSecurityManager().getActiveRoleService().getName(),
                            (GeoServerRole) params[0])
                    .setReturnPage(page);
        else
            return new EditRolePage((String) params[0], (GeoServerRole) params[1])
                    .setReturnPage(page);
    }

    protected String getTabbedPanelPath() {
        return "panel:panel";
    };

    protected String getItemsPath() {
        return getTabbedPanelPath() + ":panel:table:listContainer:items";
    };

    @Test
    public void testEditParentRole() throws Exception {

        tester.startPage(listPage(getRoleServiceName()));

        GeoServerRole role = gaService.getRoleByName("ROLE_AUTHENTICATED");
        assertNotNull(role);
        List<Property<GeoServerRole>> props =
                new RoleListProvider(getRoleServiceName()).getProperties();
        Property<GeoServerRole> parentProp = null;
        for (Property<GeoServerRole> prop : props) {
            if (RoleListProvider.ParentPropertyName.equals(prop.getName())) {
                parentProp = prop;
                break;
            }
        }
        Component c = getFromList(SECOND_COLUM_PATH, role, parentProp);
        assertNotNull(c);
        tester.clickLink(c.getPageRelativePath());

        tester.assertRenderedPage(EditRolePage.class);
        assertTrue(checkEditForm(role.getAuthority()));
    }

    @Override
    protected String getSearchString() throws Exception {
        GeoServerRole role = gaService.getRoleByName(GeoServerRole.ADMIN_ROLE.getAuthority());
        assertNotNull(role);
        return role.getAuthority();
    }

    @Override
    protected Property<GeoServerRole> getEditProperty() {
        return RoleListProvider.ROLENAME;
    }

    @Override
    protected boolean checkEditForm(String objectString) {
        return objectString.equals(
                tester.getComponentFromLastRenderedPage("form:name").getDefaultModelObject());
    }

    @Test
    public void testReadOnlyService() throws Exception {
        listPage(getRoleServiceName());
        tester.assertVisible(getRemoveLink().getPageRelativePath());
        tester.assertVisible(getAddLink().getPageRelativePath());

        activateRORoleService();

        listPage(getRORoleServiceName());
        tester.assertInvisible(getRemoveLink().getPageRelativePath());
        tester.assertInvisible(getAddLink().getPageRelativePath());
    }

    @Override
    protected void simulateDeleteSubmit() throws Exception {

        SelectionRoleRemovalLink link = (SelectionRoleRemovalLink) getRemoveLink();
        Method m =
                link.delegate
                        .getClass()
                        .getDeclaredMethod("onSubmit", AjaxRequestTarget.class, Component.class);
        m.invoke(link.delegate, null, null);

        SortedSet<GeoServerRole> roles = gaService.getRoles();
        assertEquals(3, roles.size());
        assertFalse(roles.contains(createNewRoleForRemvovalTest()));
    }

    GeoServerRole createNewRoleForRemvovalTest() throws Exception {
        return gaStore.createRoleObject("NEW_ROLE");
    }

    @Override
    protected void doRemove(String pathForLink) throws Exception {
        GeoServerRole newRole = createNewRoleForRemvovalTest();
        gaStore.addRole(newRole);
        gaStore.store();
        assertEquals(5, gaService.getRoles().size());

        super.doRemove(pathForLink);
    }
}
