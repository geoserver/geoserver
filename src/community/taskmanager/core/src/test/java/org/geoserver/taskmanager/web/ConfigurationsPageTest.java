/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.taskmanager.AbstractWicketTaskManagerTest;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.web.model.ConfigurationsModel;
import org.geoserver.taskmanager.web.panel.DropDownPanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationsPageTest extends AbstractWicketTaskManagerTest {

    private TaskManagerFactory fac;
    private TaskManagerDao dao;
    private Configuration config;

    @Before
    public void before() {
        fac = TaskManagerBeans.get().getFac();
        dao = TaskManagerBeans.get().getDao();

        config = fac.createConfiguration();
        config.setTemplate(true);
        config.setDescription("template description");
        config.setName("my_template");
        config = dao.save(config);
        login();
    }

    @After
    public void clearDataFromDatabase() {
        dao.delete(config);
        logout();
    }

    private Configuration dummyConfiguration1() {
        Configuration config = fac.createConfiguration();
        config.setName("Z-CONFIG");
        config.setDescription("z description");
        return config;
    }

    private Configuration dummyConfiguration2() {
        Configuration config = fac.createConfiguration();
        config.setName("A-CONFIG");
        return config;
    }

    @Test
    public void testPage() {
        ConfigurationsPage page = new ConfigurationsPage();

        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationsPage.class);

        tester.assertComponent("configurationsPanel", GeoServerTablePanel.class);
        tester.assertComponent("dialog", GeoServerDialog.class);

        tester.assertComponent("addNew", AjaxLink.class);
        tester.assertComponent("removeSelected", AjaxLink.class);
    }

    @Test
    public void testConfigurations() throws Exception {
        ConfigurationsPage page = new ConfigurationsPage();

        Configuration dummy1 = dao.save(dummyConfiguration1());

        List<Configuration> configurations = dao.getConfigurations(false);

        tester.startPage(page);

        @SuppressWarnings("unchecked")
        GeoServerTablePanel<Configuration> table =
                (GeoServerTablePanel<Configuration>)
                        tester.getComponentFromLastRenderedPage("configurationsPanel");

        assertEquals(configurations.size(), table.getDataProvider().size());
        assertTrue(containsConfig(getConfigurationsFromTable(table), dummy1));

        Configuration dummy2 = dao.save(dummyConfiguration2());

        ((ConfigurationsModel) table.getDataProvider()).reset();

        assertEquals(configurations.size() + 1, table.getDataProvider().size());
        assertTrue(containsConfig(getConfigurationsFromTable(table), dummy2));

        dao.delete(dummy1);
        dao.delete(dummy2);
    }

    @Test
    public void testNew() {
        login();

        ConfigurationsPage page = new ConfigurationsPage();
        tester.startPage(page);

        tester.clickLink("addNew");

        tester.assertComponent("dialog:dialog:content:form:userPanel", DropDownPanel.class);

        tester.executeAjaxEvent("dialog:dialog:content:form:submit", "click");

        tester.assertRenderedPage(ConfigurationPage.class);

        tester.assertModelValue("configurationForm:description", null);

        logout();
    }

    @Test
    public void testEdit() {
        Configuration dummy1 = dao.save(dummyConfiguration1());

        login();

        ConfigurationsPage page = new ConfigurationsPage();
        tester.startPage(page);

        tester.clickLink(
                "configurationsPanel:listContainer:items:1:itemProperties:1:component:link");

        tester.assertRenderedPage(ConfigurationPage.class);

        tester.assertModelValue("configurationForm:name", dummy1.getName());

        tester.assertModelValue("configurationForm:description", dummy1.getDescription());

        dao.delete(dummy1);

        logout();
    }

    @Test
    public void testNewFromTemplate() {
        login();

        ConfigurationsPage page = new ConfigurationsPage();
        tester.startPage(page);

        tester.clickLink("addNew");

        tester.assertComponent("dialog:dialog:content:form:userPanel", DropDownPanel.class);

        FormTester formTester = tester.newFormTester("dialog:dialog:content:form");

        formTester.select("userPanel:dropdown", 0);

        tester.executeAjaxEvent("dialog:dialog:content:form:submit", "click");

        tester.assertRenderedPage(ConfigurationPage.class);

        tester.assertModelValue("configurationForm:description", "template description");

        logout();
    }

    @Test
    public void testDelete() throws Exception {
        login();

        ConfigurationsPage page = new ConfigurationsPage();
        tester.startPage(page);

        @SuppressWarnings("unchecked")
        GeoServerTablePanel<Configuration> table =
                (GeoServerTablePanel<Configuration>)
                        tester.getComponentFromLastRenderedPage("configurationsPanel");

        Configuration dummy1 = dao.save(dummyConfiguration1());
        Configuration dummy2 = dao.save(dummyConfiguration2());

        assertTrue(containsConfig(dao.getConfigurations(false), dummy1));
        assertTrue(containsConfig(dao.getConfigurations(false), dummy2));

        ((ConfigurationsModel) table.getDataProvider()).reset();

        // sort descending on name
        tester.clickLink("configurationsPanel:listContainer:sortableLinks:1:header:link");
        tester.clickLink("configurationsPanel:listContainer:sortableLinks:1:header:link");

        // select
        CheckBox selector =
                ((CheckBox)
                        tester.getComponentFromLastRenderedPage(
                                "configurationsPanel:listContainer:items:1:selectItemContainer:selectItem"));
        tester.getRequest().setParameter(selector.getInputName(), "true");
        tester.executeAjaxEvent(selector, "click");

        assertEquals(dummy1.getId(), table.getSelection().get(0).getId());

        // click delete
        ModalWindow w = (ModalWindow) tester.getComponentFromLastRenderedPage("dialog:dialog");
        assertFalse(w.isShown());
        tester.clickLink("removeSelected");
        assertTrue(w.isShown());

        // confirm
        tester.executeAjaxEvent("dialog:dialog:content:form:submit", "click");

        assertFalse(containsConfig(dao.getConfigurations(false), dummy1));
        assertTrue(containsConfig(dao.getConfigurations(false), dummy2));

        ((ConfigurationsModel) table.getDataProvider()).reset();

        assertFalse(containsConfig(getConfigurationsFromTable(table), dummy1));
        assertTrue(containsConfig(getConfigurationsFromTable(table), dummy2));

        dao.delete(dummy2);

        logout();
    }

    @Test
    public void testCopy() throws Exception {
        login();

        ConfigurationsPage page = new ConfigurationsPage();

        Configuration dummy1 = dao.save(dummyConfiguration1());

        tester.startPage(page);

        // select
        CheckBox selector =
                ((CheckBox)
                        tester.getComponentFromLastRenderedPage(
                                "configurationsPanel:listContainer:items:1:selectItemContainer:selectItem"));
        tester.getRequest().setParameter(selector.getInputName(), "true");
        tester.executeAjaxEvent(selector, "click");

        // click copy
        tester.clickLink("copySelected");

        dao.delete(dummy1);

        tester.assertRenderedPage(ConfigurationPage.class);

        tester.assertModelValue("configurationForm:description", "z description");

        logout();
    }

    protected List<Configuration> getConfigurationsFromTable(
            GeoServerTablePanel<Configuration> table) {
        List<Configuration> result = new ArrayList<Configuration>();
        Iterator<Configuration> it = table.getDataProvider().iterator(0, table.size());
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    protected boolean containsConfig(Collection<Configuration> coll, Configuration config) {
        for (Configuration c : coll) {
            if (config.getId().equals(c.getId())) {
                return true;
            }
        }
        return false;
    }
}
