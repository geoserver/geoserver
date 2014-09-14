/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;


import static org.junit.Assert.*;

import java.util.Iterator;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractListPageTest<T> extends AbstractSecurityWicketTestSupport {
    
     public static final String ITEMS_PATH = "table:listContainer:items";
     public static final String FIRST_COLUM_PATH="itemProperties:0:component:link";
    

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
    
    abstract protected Page listPage(PageParameters params);
    abstract protected Page newPage(Object...params);
    abstract protected Page editPage(Object...params);
 
    abstract protected String getSearchString() throws Exception;
    abstract protected Property<T> getEditProperty();
    abstract protected boolean checkEditForm(String search);
    
    
    @Test
    public void testEdit() throws Exception {
        // the name link for the first user
        initializeForXML();
        //insertValues();
        
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
        Iterator<MarkupContainer> it = (Iterator<MarkupContainer>) listView.iterator();
        
        while (it.hasNext()) {
            MarkupContainer m = it.next();
            Component c = m.get(columnPath);
            @SuppressWarnings("unchecked")
            T modelObject = (T) c.getDefaultModelObject();
            if (columnValue.equals(property.getPropertyValue(modelObject)))
                return c;
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
        
        
        GeoserverTablePanelTestPage testPage = 
         new GeoserverTablePanelTestPage(new ComponentBuilder() {            
            private static final long serialVersionUID = 1L;

            public Component buildComponent(String id) {
                try {
                    return listPage(null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        
        tester.startPage(testPage);
                
        String selectAllPath = testPage.getWicketPath()+":table:listContainer:selectAllContainer:selectAll";        
        tester.assertComponent(selectAllPath, CheckBox.class);
        
        FormTester ft = tester.newFormTester(GeoserverTablePanelTestPage.FORM);
        ft.setValue(testPage.getComponentId()+":table:listContainer:selectAllContainer:selectAll", "true");
        tester.executeAjaxEvent(selectAllPath, "onclick");
        
        ModalWindow w  = (ModalWindow) tester.getLastRenderedPage().get("dialog:dialog");        
        assertNull(w.getTitle()); // window was not opened
        tester.executeAjaxEvent(pathForLink, "onclick");
        assertNotNull(w.getTitle()); // window was opened        
        simulateDeleteSubmit();        
        executeModalWindowCloseButtonCallback(w);
    }
        
    protected abstract void simulateDeleteSubmit() throws Exception;        

    protected Component getRemoveLink() {
        Component result =tester.getLastRenderedPage().get("headerPanel:removeSelected");
        assertNotNull(result);
        return result;
    }
    
    protected Component getRemoveLinkWithRoles() {
        Component result =tester.getLastRenderedPage().get("headerPanel:removeSelectedWithRoles");
        assertNotNull(result);
        return result;
    }

    
    protected Component getAddLink() {
        Component result =tester.getLastRenderedPage().get("headerPanel:addNew");
        assertNotNull(result);
        return result;
    }

    
}
