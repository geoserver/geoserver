package org.geoserver.taskmanager.web;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.taskmanager.AbstractWicketTaskManagerTest;
import org.geoserver.taskmanager.beans.DummyTaskTypeImpl;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Run.Status;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InitConfigPageTest extends AbstractWicketTaskManagerTest {

    private TaskManagerDao dao;

    private TaskManagerFactory fac;

    private TaskManagerDataUtil util;

    private TaskManagerTaskUtil tutil;

    private Configuration config;

    protected boolean setupDataDirectory() throws Exception {
        return true;
    }

    @Before
    public void before() {

        fac = TaskManagerBeans.get().getFac();
        dao = TaskManagerBeans.get().getDao();
        util = TaskManagerBeans.get().getDataUtil();
        tutil = TaskManagerBeans.get().getTaskUtil();

        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("gs");

        Task task1 = tutil.initTask(TestTaskTypeImpl.NAME, "task1");
        util.addTaskToConfiguration(config, task1);

        Task task2 = tutil.initTask(DummyTaskTypeImpl.NAME, "task2");
        util.addTaskToConfiguration(config, task2);

        Batch batch = fac.createBatch();
        batch.setName("@Initialize");
        util.addBatchElement(batch, task1);
        util.addBatchToConfiguration(config, batch);

        Batch otherBatch = fac.createBatch();
        otherBatch.setName("otherBatch");
        util.addBatchElement(otherBatch, task1);
        util.addBatchToConfiguration(config, otherBatch);

        config = dao.save(config);

        login();
    }

    @After
    public void after() {
        dao.delete(config);
        logout();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test() throws InterruptedException {
        ConfigurationPage page = new ConfigurationPage(config);

        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        // attributes table
        GeoServerTablePanel<Attribute> attributesPanel =
                (GeoServerTablePanel<Attribute>)
                        tester.getComponentFromLastRenderedPage(
                                "configurationForm:attributesPanel");
        assertEquals(3, attributesPanel.getDataProvider().size());

        // tasks table
        GeoServerTablePanel<Task> tasksPanel =
                (GeoServerTablePanel<Task>)
                        tester.getComponentFromLastRenderedPage("configurationForm:tasksPanel");
        assertEquals(1, tasksPanel.getDataProvider().size());

        // batches panel
        GeoServerTablePanel<Batch> batchesPanel =
                (GeoServerTablePanel<Batch>)
                        tester.getComponentFromLastRenderedPage(
                                "configurationForm:batchesPanel:form:batchesPanel");
        assertEquals(1, batchesPanel.getDataProvider().size());

        tester.assertInvisible("configurationForm:save");

        tester.clickLink("configurationForm:apply");
        tester.assertRenderedPage(InitConfigurationPage.class);

        Thread.sleep(1000);

        tester.executeBehavior(
                tester.getLastRenderedPage().getBehaviors(AbstractAjaxTimerBehavior.class).get(0));

        tester.assertRenderedPage(ConfigurationPage.class);

        // attributes table
        attributesPanel =
                (GeoServerTablePanel<Attribute>)
                        tester.getComponentFromLastRenderedPage(
                                "configurationForm:attributesPanel");
        assertEquals(5, attributesPanel.getDataProvider().size());

        // tasks table
        tasksPanel =
                (GeoServerTablePanel<Task>)
                        tester.getComponentFromLastRenderedPage("configurationForm:tasksPanel");
        assertEquals(2, tasksPanel.getDataProvider().size());

        // batches panel
        batchesPanel =
                (GeoServerTablePanel<Batch>)
                        tester.getComponentFromLastRenderedPage(
                                "configurationForm:batchesPanel:form:batchesPanel");
        assertEquals(2, batchesPanel.getDataProvider().size());

        tester.assertModelValue(
                "configurationForm:batchesPanel:form:batchesPanel:listContainer:items:1:itemProperties:7:component",
                Status.COMMITTED);

        tester.assertVisible("configurationForm:save");

        FormTester formTester = tester.newFormTester("configurationForm");
        formTester.select(
                "attributesPanel:listContainer:items:4:itemProperties:1:component:dropdown", 0);
        tester.executeAjaxEvent(
                "configurationForm:attributesPanel:listContainer:items:4:itemProperties:1:component:dropdown",
                "change");
        formTester.select(
                "attributesPanel:listContainer:items:5:itemProperties:1:component:dropdown", 0);

        tester.clickLink("configurationForm:apply");
        tester.assertRenderedPage(ConfigurationPage.class);
    }
}
