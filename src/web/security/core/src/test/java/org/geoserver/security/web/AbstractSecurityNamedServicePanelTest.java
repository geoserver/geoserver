/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestHandler;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.wicket.GeoServerDialog;
import org.junit.Before;

public abstract class AbstractSecurityNamedServicePanelTest extends AbstractSecurityWicketTestSupport {

    
    public static final String FIRST_COLUM_PATH="itemProperties:0:component:link";
    public static final String CHECKBOX_PATH="selectItemContainer:selectItem";
    
    protected AbstractSecurityPage basePage;
    protected String basePanelId;
    protected FormTester formTester;
    
    GeoServerSecurityManager manager; 
    
    protected void newFormTester() {
        newFormTester("form");
    }

    protected void newFormTester(String path) {
        //formTester = tester.newFormTester(getDetailsFormComponentId());
        formTester = tester.newFormTester(path);
    }

    @Before
    public void init() throws Exception {
        manager= getSecurityManager();
    }
    
    protected abstract AbstractSecurityPage getBasePage();
    protected abstract String getBasePanelId();
    protected abstract Integer getTabIndex();
    protected abstract Class<? extends Component> getNamedServicesClass();
    protected abstract String getDetailsFormComponentId();

    

    
    protected void activatePanel() {
        basePage=getBasePage();
        basePanelId=getBasePanelId();
        tester.startPage(basePage);
        tester.assertRenderedPage(basePage.getPageClass());
        
        //String linkId = getTabbedPanel().getId()+":tabs-container:tabs:"+getTabIndex()+":link";        
        //tester.clickLink(linkId,true);
        //assertEquals(getNamedServicesClass(), getNamedServicesPanel().getClass());
    }
    
//    protected AjaxTabbedPanel getTabbedPanel() {
//        return (AjaxTabbedPanel) tabbedPage.get(AbstractSecurityPage.TabbedPanelId);
//    }
    
//    protected NamedServicesPanel getNamedServicesPanel() {
//        return (NamedServicesPanel) tabbedPage.get(getTabbedPanel().getId()+":panel");
//        
//    }
    
    protected void clickAddNew() {
        tester.clickLink(basePanelId + ":add");
    }
    
    protected void clickRemove() {
        tester.clickLink(basePanelId + ":remove");
    }

    protected Component getRemoveLink() {
        Component result =tester.getLastRenderedPage().get("tabbedPanel:panel:removeSelected");
        assertNotNull(result);
        return result;
    }

    
    protected DataView<SecurityNamedServiceConfig> getDataView() {
        return (DataView<SecurityNamedServiceConfig>)
            basePage.get(basePanelId + ":table:listContainer:items");
    }
    
    protected long countItems() {
        tester.debugComponentTrees();
        return getDataView().getItemCount();
    }
    
    protected SecurityNamedServiceConfig getSecurityNamedServiceConfig(String name) {
        //<SecurityNamedServiceConfig>
       Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
       while (it.hasNext()) {
           Item<SecurityNamedServiceConfig> item = it.next();
           if (name.equals(item.getModelObject().getName()))
               return item.getModelObject();
       }
    return null;   
    }
    
    protected void clickNamedServiceConfig(String name) {
        //<SecurityNamedServiceConfig>
       Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
       while (it.hasNext()) {
           Item<SecurityNamedServiceConfig> item = it.next();
           if (name.equals(item.getModelObject().getName()))
               tester.clickLink(item.getPageRelativePath()+":"+FIRST_COLUM_PATH);
       }
       
    }
    

    protected void checkNamedServiceConfig(String name) {
        //<SecurityNamedServiceConfig>
       Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
       while (it.hasNext()) {
           Item<SecurityNamedServiceConfig> item = it.next();
           if (name.equals(item.getModelObject().getName()))
               tester.executeAjaxEvent(item.getPageRelativePath()+":"+CHECKBOX_PATH,"onclick");
       }       
    }

    
    protected void doRemove(String pathForLink, String ... serviceNames) throws Exception {
//        GeoserverTablePanelTestPage testPage = 
//         new GeoserverTablePanelTestPage(new ComponentBuilder() {            
//            private static final long serialVersionUID = 1L;
//
//            public Component buildComponent(String id) {
//                try {
//                    return basePage;
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//
//        tester = new WicketTester();
//        tester.startPage(basePage);
//        
        AbstractSecurityPage testPage = (AbstractSecurityPage) tester.getLastRenderedPage();

        //form:0:tabbedPanel:panel:table:listContainer:items:5:selectItemContainer:selectItem
        if (serviceNames.length==0) {
            String selectAllPath = basePanelId + ":table:listContainer:selectAllContainer:selectAll";
            //String selectAllPath = testPage.getWicketPath() + ":" + relId; 
            tester.assertComponent(selectAllPath, CheckBox.class);

            FormTester ft = tester.newFormTester(GeoserverTablePanelTestPage.FORM);
            //ft.setValue(testPage.getComponentId()+":"+relId, "true");
            tester.executeAjaxEvent(selectAllPath, "onclick");
        } 
        else {
            DataView<SecurityNamedServiceConfig> dataview = (DataView<SecurityNamedServiceConfig>)
                testPage.get(basePanelId + ":table:listContainer:items");
                //testPage.get(testPage.getWicketPath() + ":" + basePanelId + ":table:listContainer:items");
                //testPage.get("form:0:tabbedPanel:panel:");
            List<String> nameList = Arrays.asList(serviceNames);
            //FormTester ft = tester.newFormTester(GeoserverTablePanelTestPage.FORM);
            //print(testPage,true,true);
            Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
            while (it.hasNext()) {
                Item<SecurityNamedServiceConfig> item = it.next();
                if (nameList.contains(item.getModelObject().getName())) {
                    String checkBoxPath=item.getPageRelativePath()+":"+CHECKBOX_PATH;

                    tester.assertComponent(checkBoxPath, CheckBox.class);
                    //ft.setValue(testPage.getComponentId()+":"+checkBoxPath.replace("form:0:", ""), true);
                    testPage.get(checkBoxPath).setDefaultModelObject(true);
                    //tester.executeAjaxEvent(checkBoxPath, "onclick");
                }
            }
        }

        tester.assertNoErrorMessage();
        
        tester.assertComponent(basePanelId + ":dialog:dialog", ModalWindow.class);
        ModalWindow w  = (ModalWindow) testPage.get(basePanelId + ":dialog:dialog");
                /*(ModalWindow) testPage.get(
            testPage.getWicketPath() + ":dialog:dialog");*/

        assertFalse(w.isShown());
        tester.clickLink(basePanelId + ":remove", true);
        assertTrue(w.isShown());
        
        ((GeoServerDialog)w.getParent()).submit(new AjaxRequestHandler(tester.getLastRenderedPage()));
        //simulateDeleteSubmit();        
        //executeModalWindowCloseButtonCallback(w);
    }

    protected void simulateDeleteSubmit() throws Exception {
        //AjaxLink link = (AjaxLInk) tester.getLastRenderedPage().get(basePanelId + ":remove");
        //link.on
    }
    

    protected void setSecurityConfigName(String aName) {
        formTester.setValue("panel:content:name", aName);
    }
    
    protected String getSecurityConfigName() {
        return formTester.getForm().get("config.name").getDefaultModelObjectAsString();
    }

    protected String getSecurityConfigClassName() {
        return formTester.getForm().get("config.className").getDefaultModelObjectAsString();
    }
    
    protected <T extends SecurityNamedServicePanelInfo> void setSecurityConfigClassName(Class<T> clazz) {
        ListView list = (ListView) tester.getLastRenderedPage().get("servicesContainer:services");
        int toClick = -1;
        for (int i = 0; i < list.getList().size(); i++) {
            if (clazz.isInstance(list.getList().get(0))) {
                toClick = i;
                break;
            }
         }
        AjaxLink link = (AjaxLink) ((MarkupContainer)list.get(toClick)).get("link");
        tester.executeAjaxEvent(link, "onclick");
//        formTester.select("config.className", index);     
//        tester.executeAjaxEvent(formTester.getForm().getPageRelativePath()+":config.className", "onchange");
    }

    protected void clickSave() {        
          formTester.submit("save");
    }
    protected void clickCancel() {        
        formTester.submitLink("cancel",false);
    }
    
}
