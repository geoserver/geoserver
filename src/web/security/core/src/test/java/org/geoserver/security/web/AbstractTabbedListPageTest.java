/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractTabbedListPageTest<T> extends AbstractSecurityWicketTestSupport {

    public static final String FIRST_COLUM_PATH = "itemProperties:0:component:link";

    @Before
    public void setUp() throws Exception {
        login();
    }

    @Before
    public void initService() throws Exception {
        doInitialize();
        clearServices();
        insertValues();
    }

    protected void doInitialize() throws Exception {
        initializeForXML();
    }

    @Test
    public void testRenders() throws Exception {
        tester.assertRenderedPage(listPage(getServiceName()).getClass());
    }

    protected String getItemsPath() {
        return getTabbedPanelPath() + ":panel:table:listContainer:items";
    };

    protected abstract String getTabbedPanelPath();

    protected abstract String getServiceName();

    protected abstract Page listPage(String serviceName);

    protected abstract Page newPage(AbstractSecurityPage responsePage, Object... params);

    protected abstract Page editPage(AbstractSecurityPage responsePage, Object... params);

    protected abstract String getSearchString() throws Exception;

    protected abstract Property<T> getEditProperty();

    protected abstract boolean checkEditForm(String search);

    @Test
    public void testEdit() throws Exception {
        // the name link for the first user
        AbstractSecurityPage listPage = (AbstractSecurityPage) listPage(getServiceName());
        // tester.startPage(listPage);

        String search = getSearchString();
        assertNotNull(search);
        Component c = getFromList(FIRST_COLUM_PATH, search, getEditProperty());
        assertNotNull(c);
        tester.clickLink(c.getPageRelativePath());

        tester.assertRenderedPage(editPage(listPage).getClass());
        assertTrue(checkEditForm(search));
    }

    protected Component getFromList(String columnPath, Object columnValue, Property<T> property) {

        MarkupContainer listView =
                (MarkupContainer) tester.getLastRenderedPage().get(getItemsPath());

        @SuppressWarnings("unchecked")
        Iterator<Component> it = (Iterator<Component>) listView.iterator();

        while (it.hasNext()) {
            MarkupContainer m = (MarkupContainer) it.next();
            Component c = m.get(columnPath);
            @SuppressWarnings("unchecked")
            T modelObject = (T) c.getDefaultModelObject();
            if (columnValue.equals(property.getPropertyValue(modelObject))) return c;
        }
        return null;
    }

    @Test
    public void testNew() throws Exception {
        listPage(getServiceName());
        tester.clickLink(getTabbedPanelPath() + ":panel:header:addNew");
        Page newPage = tester.getLastRenderedPage();
        tester.assertRenderedPage(newPage.getClass());
    }

    @Test
    public void testRemove() throws Exception {
        addAdditonalData();
        doRemove(getTabbedPanelPath() + ":panel:header:removeSelected");
    }

    protected void doRemove(String pathForLink) throws Exception {
        Page testPage = listPage(getServiceName());

        String selectAllPath =
                getTabbedPanelPath() + ":panel:table:listContainer:selectAllContainer:selectAll";
        tester.assertComponent(selectAllPath, CheckBox.class);
        CheckBox selectAllComponent =
                (CheckBox) tester.getComponentFromLastRenderedPage(selectAllPath);

        // simulate setting a form value, without an actual form around it
        setFormComponentValue(selectAllComponent, "true");
        tester.executeAjaxEvent(selectAllPath, "click");

        String windowPath = getTabbedPanelPath() + ":panel:dialog:dialog";
        ModalWindow w = (ModalWindow) testPage.get(windowPath);
        assertNull(w.getTitle()); // window was not opened
        tester.executeAjaxEvent(pathForLink, "click");
        assertNotNull(w.getTitle()); // window was opened
        simulateDeleteSubmit();
        executeModalWindowCloseButtonCallback(w);
    }

    protected abstract void simulateDeleteSubmit() throws Exception;

    protected Component getRemoveLink() {
        Component result =
                tester.getLastRenderedPage()
                        .get(getTabbedPanelPath() + ":panel:header:removeSelected");
        assertNotNull(result);
        return result;
    }

    protected Component getRemoveLinkWithRoles() {
        Component result =
                tester.getLastRenderedPage()
                        .get(getTabbedPanelPath() + ":panel:header:removeSelectedWithRoles");
        assertNotNull(result);
        return result;
    }

    protected Component getAddLink() {
        Component result =
                tester.getLastRenderedPage().get(getTabbedPanelPath() + ":panel:header:addNew");
        assertNotNull(result);
        return result;
    }
}
