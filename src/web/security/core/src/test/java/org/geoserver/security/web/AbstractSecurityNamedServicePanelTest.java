/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestHandler;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.wicket.GSModalWindow;
import org.geoserver.web.wicket.GeoServerDialog;
import org.junit.Before;

public abstract class AbstractSecurityNamedServicePanelTest extends AbstractSecurityWicketTestSupport {

    public static final String FIRST_COLUM_PATH = "itemProperties:0:component:link";
    public static final String CHECKBOX_PATH = "selectItemContainer:selectItem";

    protected AbstractSecurityPage basePage;
    protected String basePanelId;
    protected FormTester formTester;

    GeoServerSecurityManager manager;

    protected void newFormTester() {
        newFormTester("form");
    }

    protected void newFormTester(String path) {
        // formTester = tester.newFormTester(getDetailsFormComponentId());
        formTester = tester.newFormTester(path);
    }

    @Before
    public void init() throws Exception {
        manager = getSecurityManager();
    }

    protected abstract AbstractSecurityPage getBasePage();

    protected abstract String getBasePanelId();

    protected abstract Integer getTabIndex();

    protected abstract Class<? extends Component> getNamedServicesClass();

    protected abstract String getDetailsFormComponentId();

    protected void activatePanel() {
        basePage = getBasePage();
        basePanelId = getBasePanelId();
        tester.startPage(basePage);
        tester.assertRenderedPage(basePage.getPageClass());

        // String linkId = getTabbedPanel().getId()+":tabs-container:tabs:"+getTabIndex()+":link";
        // tester.clickLink(linkId,true);
        // assertEquals(getNamedServicesClass(), getNamedServicesPanel().getClass());
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
        Component result = tester.getLastRenderedPage().get("tabbedPanel:panel:removeSelected");
        assertNotNull(result);
        return result;
    }

    @SuppressWarnings("unchecked")
    protected DataView<SecurityNamedServiceConfig> getDataView() {
        return (DataView<SecurityNamedServiceConfig>) basePage.get(basePanelId + ":table:listContainer:items");
    }

    protected long countItems() {
        tester.debugComponentTrees();
        return getDataView().getItemCount();
    }

    protected SecurityNamedServiceConfig getSecurityNamedServiceConfig(String name) {
        // <SecurityNamedServiceConfig>
        Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
        while (it.hasNext()) {
            Item<SecurityNamedServiceConfig> item = it.next();
            if (name.equals(item.getModelObject().getName())) return item.getModelObject();
        }
        return null;
    }

    protected void clickNamedServiceConfig(String name) {
        // <SecurityNamedServiceConfig>
        Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
        while (it.hasNext()) {
            Item<SecurityNamedServiceConfig> item = it.next();
            if (name.equals(item.getModelObject().getName()))
                tester.clickLink(item.getPageRelativePath() + ":" + FIRST_COLUM_PATH);
        }
    }

    protected void checkNamedServiceConfig(String name) {
        // <SecurityNamedServiceConfig>
        Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
        while (it.hasNext()) {
            Item<SecurityNamedServiceConfig> item = it.next();
            if (name.equals(item.getModelObject().getName()))
                tester.executeAjaxEvent(item.getPageRelativePath() + ":" + CHECKBOX_PATH, "click");
        }
    }

    protected void doRemove(String pathForLink, String... serviceNames) throws Exception {
        AbstractSecurityPage testPage = (AbstractSecurityPage) tester.getLastRenderedPage();

        if (serviceNames.length == 0) {
            String selectAllPath = basePanelId + ":table:listContainer:selectAllContainer:selectAll";
            tester.assertComponent(selectAllPath, CheckBox.class);

            FormComponent selectAllPathComponent =
                    (FormComponent) tester.getComponentFromLastRenderedPage(selectAllPath);
            setFormComponentValue(selectAllPathComponent, "true");
            tester.executeAjaxEvent(selectAllPath, "click");
        } else {
            List<String> nameList = Arrays.asList(serviceNames);

            Iterator<Item<SecurityNamedServiceConfig>> it = getDataView().getItems();
            while (it.hasNext()) {
                Item<SecurityNamedServiceConfig> item = it.next();
                if (nameList.contains(item.getModelObject().getName())) {
                    String checkBoxPath = item.getPageRelativePath() + ":" + CHECKBOX_PATH;

                    tester.assertComponent(checkBoxPath, CheckBox.class);

                    FormComponent checkBoxPathComponent =
                            (FormComponent) tester.getComponentFromLastRenderedPage(checkBoxPath);
                    setFormComponentValue(checkBoxPathComponent, "true");

                    testPage.get(checkBoxPath).setDefaultModelObject(true);
                    tester.executeAjaxEvent(checkBoxPath, "click");
                }
            }
        }

        tester.assertNoErrorMessage();

        tester.assertComponent(basePanelId + ":dialog:dialog", GSModalWindow.class);
        GSModalWindow w = (GSModalWindow) testPage.get(basePanelId + ":dialog:dialog");
        /*(GSModalWindow) testPage.get(
        testPage.getWicketPath() + ":dialog:dialog");*/

        assertFalse(w.isShown());
        tester.clickLink(basePanelId + ":remove", true);
        assertTrue(w.isShown());

        ((GeoServerDialog) w.getParent()).submit(new AjaxRequestHandler(tester.getLastRenderedPage()));
        // simulateDeleteSubmit();
        // executeModalWindowCloseButtonCallback(w);
    }

    protected void simulateDeleteSubmit() throws Exception {
        // AjaxLink link = (AjaxLInk) tester.getLastRenderedPage().get(basePanelId + ":remove");
        // link.on
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

    @SuppressWarnings("unchecked")
    protected <T extends SecurityNamedServicePanelInfo> void setSecurityConfigClassName(Class<T> clazz)
            throws Exception {
        ListView list = (ListView) tester.getLastRenderedPage().get("servicesContainer:services");
        list.forEach(i -> {
            if (i instanceof ListItem<? extends Object> listItem) {
                if (clazz.isInstance(listItem.getModelObject())) {
                    listItem.forEach(action -> {
                        if (action instanceof AjaxLink link) {
                            if (link.isEnabled()) {
                                tester.executeAjaxEvent(link, "click");
                            }
                        }
                    });
                }
            }
        });
    }

    protected void clickSave() {
        formTester.submit("save");
    }

    protected void clickCancel() {
        formTester.submitLink("cancel", false);
    }
}
