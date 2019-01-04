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
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractListPageTest<T> extends AbstractSecurityWicketTestSupport {

    public static final String ITEMS_PATH = "table:listContainer:items";
    public static final String FIRST_COLUM_PATH = "itemProperties:0:component:link";

    @Before
    public void setUp() throws Exception {
        login();
    }

    @Test
    public void testRenders() throws Exception {
        initializeForXML();
        tester.startPage(listPage(null));
        tester.assertRenderedPage(listPage(null).getClass());
    }

    protected abstract Page listPage(PageParameters params);

    protected abstract Page newPage(Object... params);

    protected abstract Page editPage(Object... params);

    protected abstract String getSearchString() throws Exception;

    protected abstract Property<T> getEditProperty();

    protected abstract boolean checkEditForm(String search);

    @Test
    public void testEdit() throws Exception {
        // the name link for the first user
        initializeForXML();
        // insertValues();

        tester.startPage(listPage(null));

        String search = getSearchString();
        assertNotNull(search);
        Component c = getFromList(FIRST_COLUM_PATH, search, getEditProperty());
        assertNotNull(c);
        tester.clickLink(c.getPageRelativePath());

        tester.assertRenderedPage(editPage().getClass());
        assertTrue(checkEditForm(search));
    }

    protected Component getFromList(String columnPath, Object columnValue, Property<T> property) {
        MarkupContainer listView = (MarkupContainer) tester.getLastRenderedPage().get(ITEMS_PATH);

        @SuppressWarnings("unchecked")
        Iterator<Component> it = (Iterator<Component>) listView.iterator();

        while (it.hasNext()) {
            Component container = it.next();
            Component c = container.get(columnPath);
            @SuppressWarnings("unchecked")
            T modelObject = (T) c.getDefaultModelObject();
            if (columnValue.equals(property.getPropertyValue(modelObject))) return c;
        }
        return null;
    }

    @Test
    public void testNew() throws Exception {
        initializeForXML();
        tester.startPage(listPage(null));
        tester.clickLink("headerPanel:addNew");
        Page newPage = tester.getLastRenderedPage();
        tester.assertRenderedPage(newPage.getClass());
    }

    @Test
    public void testRemove() throws Exception {
        initializeForXML();
        insertValues();
        addAdditonalData();
        doRemove("headerPanel:removeSelected");
    }

    protected void doRemove(String pathForLink) throws Exception {
        Page testPage = tester.startPage(listPage(null));

        String selectAllPath = "table:listContainer:selectAllContainer:selectAll";
        tester.assertComponent(selectAllPath, CheckBox.class);
        CheckBox selectAllComponent =
                (CheckBox) tester.getComponentFromLastRenderedPage(selectAllPath);

        setFormComponentValue(selectAllComponent, "true");
        tester.executeAjaxEvent(selectAllPath, "click");

        ModalWindow w = (ModalWindow) tester.getLastRenderedPage().get("dialog:dialog");
        assertNull(w.getTitle()); // window was not opened
        tester.executeAjaxEvent(pathForLink, "click");
        assertNotNull(w.getTitle()); // window was opened
        simulateDeleteSubmit();
        executeModalWindowCloseButtonCallback(w);
    }

    protected abstract void simulateDeleteSubmit() throws Exception;

    protected Component getRemoveLink() {
        Component result = tester.getLastRenderedPage().get("headerPanel:removeSelected");
        assertNotNull(result);
        return result;
    }

    protected Component getRemoveLinkWithRoles() {
        Component result = tester.getLastRenderedPage().get("headerPanel:removeSelectedWithRoles");
        assertNotNull(result);
        return result;
    }

    protected Component getAddLink() {
        Component result = tester.getLastRenderedPage().get("headerPanel:addNew");
        assertNotNull(result);
        return result;
    }
}
