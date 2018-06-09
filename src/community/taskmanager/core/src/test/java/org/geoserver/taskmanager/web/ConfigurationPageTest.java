/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.List;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.impl.ConfigurationImpl;
import org.geoserver.taskmanager.tasks.CopyTableTaskTypeImpl;
import org.geoserver.taskmanager.tasks.CreateViewTaskTypeImpl;
import org.geoserver.taskmanager.tasks.DbRemotePublicationTaskTypeImpl;
import org.geoserver.taskmanager.tasks.FileRemotePublicationTaskTypeImpl;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.geoserver.taskmanager.web.panel.ButtonPanel;
import org.geoserver.taskmanager.web.panel.NamePanel;
import org.geoserver.taskmanager.web.panel.NewTaskPanel;
import org.geoserver.taskmanager.web.panel.PanelListPanel;
import org.geoserver.taskmanager.web.panel.TaskParameterPanel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

public class ConfigurationPageTest extends AbstractBatchesPanelTest<ConfigurationPage> {

    private TaskManagerDataUtil util;
    private TaskManagerTaskUtil tutil;
    private Configuration config;
    private Scheduler scheduler;

    protected boolean setupDataDirectory() throws Exception {
        DATA_DIRECTORY.addWcs11Coverages();
        return true;
    }

    public Configuration createConfiguration() {
        Configuration config = fac.createConfiguration();
        config.setName("my_configuration");
        config.setDescription("my very new configuration");
        config.setWorkspace("gs");

        Task task1 = tutil.initTask(CopyTableTaskTypeImpl.NAME, "task1");
        util.addTaskToConfiguration(config, task1);

        Task task2 = tutil.initTask(CreateViewTaskTypeImpl.NAME, "task2");
        util.addTaskToConfiguration(config, task2);

        return dao.save(config);
    }

    @Override
    protected Configuration getConfiguration() {
        return config;
    }

    @Override
    protected ConfigurationPage newPage() {
        return new ConfigurationPage(config = dao.reload(config));
    }

    @Override
    protected String prefix() {
        return "configurationForm:";
    }

    @Override
    protected Collection<Batch> getBatches() {
        return config.getBatches().values();
    }

    @Before
    public void before() {
        super.before();
        util = TaskManagerBeans.get().getDataUtil();
        tutil = TaskManagerBeans.get().getTaskUtil();
        scheduler = GeoServerApplication.get().getBeanOfType(Scheduler.class);
        login();
        config = createConfiguration();
    }

    @After
    public void after() {
        // clean-up
        dao.delete(config);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTasksAndAttributes() {
        ConfigurationPage page = new ConfigurationPage(config);

        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        // plain fields
        tester.assertModelValue("configurationForm:name", "my_configuration");
        tester.assertModelValue("configurationForm:description", "my very new configuration");

        // tasks table
        GeoServerTablePanel<Task> tasksPanel =
                (GeoServerTablePanel<Task>)
                        tester.getComponentFromLastRenderedPage("configurationForm:tasksPanel");
        assertEquals(2, tasksPanel.getDataProvider().size());

        // attributes table
        GeoServerTablePanel<Attribute> attributesPanel =
                (GeoServerTablePanel<Attribute>)
                        tester.getComponentFromLastRenderedPage(
                                "configurationForm:attributesPanel");
        assertEquals(8, attributesPanel.getDataProvider().size());

        // add task
        tester.clickLink("configurationForm:addNew");
        tester.assertComponent("dialog:dialog:content:form:userPanel", NewTaskPanel.class);
        FormTester formTester = tester.newFormTester("dialog:dialog:content:form");
        formTester.setValue("userPanel:name", "task3");
        formTester.submit("submit");
        assertFeedback("dialog:dialog:content:form:userPanel:feedback", "required");
        formTester.select("userPanel:type", 8);
        formTester.submit("submit");
        assertEquals(3, tasksPanel.getDataProvider().size());
        assertEquals(3, config.getTasks().size());
        assertEquals(
                DbRemotePublicationTaskTypeImpl.NAME, config.getTasks().get("task3").getType());
        assertEquals(10, attributesPanel.getDataProvider().size());

        // edit task parameters
        tester.clickLink(
                "configurationForm:tasksPanel:listContainer:items:1:itemProperties:2:component:link");

        tester.assertComponent("dialog:dialog:content:form:userPanel", TaskParameterPanel.class);

        tester.assertModelValue(
                "dialog:dialog:content:form:userPanel:parametersPanel:listContainer:items:4:itemProperties:1:component",
                "${target-table-name}");
        formTester = tester.newFormTester("dialog:dialog:content:form");

        formTester.setValue(
                "userPanel:parametersPanel:listContainer:items:4:itemProperties:1:component:textfield",
                "${table-name}");
        formTester.submit("submit");

        // attributes are updated
        assertEquals(9, attributesPanel.getDataProvider().size());

        // edit task name
        tester.clickLink(
                "configurationForm:tasksPanel:listContainer:items:1:itemProperties:0:component:link");
        tester.assertComponent("dialog:dialog:content:form:userPanel", NamePanel.class);
        formTester = tester.newFormTester("dialog:dialog:content:form");

        formTester.setValue("userPanel:textfield", "");
        formTester.submit("submit");
        assertFeedback("dialog:dialog:content:form:userPanel:feedback", "required");
        formTester.setValue("userPanel:textfield", "new_name_for_task");
        formTester.submit("submit");

        assertNotNull(config.getTasks().get("new_name_for_task").getName());
        assertEquals("new_name_for_task", config.getTasks().get("new_name_for_task").getName());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCopyTask() {
        ConfigurationPage page = new ConfigurationPage(config);

        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        // tasks table
        GeoServerTablePanel<Task> tasksPanel =
                (GeoServerTablePanel<Task>)
                        tester.getComponentFromLastRenderedPage("configurationForm:tasksPanel");
        assertEquals(2, tasksPanel.getDataProvider().size());

        // copy task
        tester.clickLink("configurationForm:addNew");
        tester.assertComponent("dialog:dialog:content:form:userPanel", NewTaskPanel.class);
        FormTester formTester = tester.newFormTester("dialog:dialog:content:form");
        formTester.select("userPanel:copy", 0);
        tester.executeAjaxEvent("dialog:dialog:content:form:userPanel:copy", "change");
        tester.assertModelValue(
                "dialog:dialog:content:form:userPanel:type", CopyTableTaskTypeImpl.NAME);
        formTester.setValue("userPanel:name", "task3");
        formTester.submit("submit");
        assertEquals(3, tasksPanel.getDataProvider().size());
        assertEquals(3, config.getTasks().size());
        assertEquals(CopyTableTaskTypeImpl.NAME, config.getTasks().get("task3").getType());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteTasksAndSaveApplyCancel() throws SchedulerException {
        Batch dummyBatch = dummyBatch1();
        dummyBatch.setEnabled(true);
        dummyBatch = dao.save(dummyBatch);
        config = dao.reload(config);

        ConfigurationPage page = new ConfigurationPage(config);
        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        // save with tasks results in validation errors
        tester.clickLink("configurationForm:save");
        tester.assertRenderedPage(ConfigurationPage.class);
        assertFeedback("topFeedback", 7);
        assertFeedback("bottomFeedback", 7);

        // cancel
        FormTester formTester = tester.newFormTester("configurationForm");
        formTester.setValue(
                "tasksPanel:listContainer:items:1:selectItemContainer:selectItem", true);
        tester.executeAjaxEvent(
                "configurationForm:tasksPanel:listContainer:items:1:selectItemContainer:selectItem",
                "click");
        formTester.setValue(
                "tasksPanel:listContainer:items:2:selectItemContainer:selectItem", true);
        tester.executeAjaxEvent(
                "configurationForm:tasksPanel:listContainer:items:2:selectItemContainer:selectItem",
                "click");
        formTester.submit("removeSelected");
        tester.newFormTester("dialog:dialog:content:form").submit("submit");
        GeoServerTablePanel<Task> tasksPanel =
                (GeoServerTablePanel<Task>)
                        tester.getComponentFromLastRenderedPage("configurationForm:tasksPanel");
        assertEquals(0, tasksPanel.getDataProvider().size());
        formTester.setValue("name", "the_greatest_configuration");

        tester.clickLink("configurationForm:cancel");
        tester.assertRenderedPage(ConfigurationsPage.class);

        config = dao.reload(config);
        assertEquals(2, config.getTasks().size());
        assertEquals("my_configuration", config.getName());

        // apply
        page = new ConfigurationPage(config);
        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        formTester = tester.newFormTester("configurationForm");
        formTester.setValue(
                "tasksPanel:listContainer:items:1:selectItemContainer:selectItem", true);
        tester.executeAjaxEvent(
                "configurationForm:tasksPanel:listContainer:items:1:selectItemContainer:selectItem",
                "click");
        formTester.setValue(
                "tasksPanel:listContainer:items:2:selectItemContainer:selectItem", true);
        tester.executeAjaxEvent(
                "configurationForm:tasksPanel:listContainer:items:2:selectItemContainer:selectItem",
                "click");
        formTester.submit("removeSelected");
        tester.newFormTester("dialog:dialog:content:form").submit("submit");
        tasksPanel =
                (GeoServerTablePanel<Task>)
                        tester.getComponentFromLastRenderedPage("configurationForm:tasksPanel");
        assertEquals(0, tasksPanel.getDataProvider().size());
        formTester.setValue("name", "the_greatest_configuration");

        tester.clickLink("configurationForm:apply");
        tester.assertRenderedPage(ConfigurationPage.class);

        config = dao.reload(config);
        assertEquals("the_greatest_configuration", config.getName());
        assertEquals(0, config.getTasks().size());

        // new batch has been scheduled
        assertNotNull(scheduler.getJobDetail(JobKey.jobKey(dummyBatch.getId().toString())));

        // save
        formTester.setValue("name", "foo_bar_configuration");
        tester.clickLink("configurationForm:save");
        tester.assertRenderedPage(ConfigurationsPage.class);
        config = dao.reload(config);
        assertEquals("foo_bar_configuration", config.getName());

        dao.delete(dummyBatch);
    }

    @Test
    public void testTemplateNotValidated() {
        config.setTemplate(true);
        ConfigurationPage page = new ConfigurationPage(config);
        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        // save with tasks results in validation errors
        tester.clickLink("configurationForm:save");
        tester.assertRenderedPage(TemplatesPage.class);
        config = dao.reload(config);
        assertTrue(config.isTemplate());
    }

    @Test
    public void testMissingOrDuplicateName() {
        ConfigurationPage page = new ConfigurationPage(new ConfigurationImpl());
        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        // save without name
        tester.clickLink("configurationForm:save");
        tester.assertRenderedPage(ConfigurationPage.class);
        config = dao.reload(config);
        assertFeedback("topFeedback", "'Name' is required");
        assertFeedback("bottomFeedback", "'Name' is required");

        FormTester formTester = tester.newFormTester("configurationForm");
        formTester.setValue("name", "my_configuration");
        tester.clickLink("configurationForm:save");

        assertFeedback("topFeedback", "unique");
        assertFeedback("bottomFeedback", "unique");
    }

    @Test
    public void testActionEditLayer() {

        Task task3 = tutil.initTask(FileRemotePublicationTaskTypeImpl.NAME, "task3");
        util.addTaskToConfiguration(config, task3);
        config = dao.save(config);

        ConfigurationPage page = new ConfigurationPage(config);
        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        tester.assertComponent(
                "configurationForm:attributesPanel:listContainer:items:10:itemProperties:2:component",
                PanelListPanel.class);
        tester.assertComponent(
                "configurationForm:attributesPanel:listContainer:items:10:itemProperties:2:component:listview:0:panel",
                ButtonPanel.class);

        tester.assertModelValue(
                "configurationForm:attributesPanel:listContainer:items:10:itemProperties:2:component:listview:0:panel:button",
                "Edit Layer..");

        FormTester formTester = tester.newFormTester("configurationForm");
        formTester.submit(
                "attributesPanel:listContainer:items:10:itemProperties:2:component:listview:0:panel:button");
        assertFeedback("topFeedback", "You cannot execute this action with this value.");

        formTester.select(
                "attributesPanel:listContainer:items:10:itemProperties:1:component:dropdown", 1);
        formTester.submit(
                "attributesPanel:listContainer:items:10:itemProperties:2:component:listview:0:panel:button");
        tester.assertNoErrorMessage();

        tester.assertRenderedPage(ResourceConfigurationPage.class);

        tester.clickLink("publishedinfo:cancel");

        tester.assertRenderedPage(ConfigurationPage.class);
    }

    private void assertFeedback(String path, int numberOfMessages) {
        final FeedbackPanel fbp = (FeedbackPanel) tester.getComponentFromLastRenderedPage(path);
        final IModel<List<FeedbackMessage>> model = fbp.getFeedbackMessagesModel();
        final List<FeedbackMessage> renderedMessages = model.getObject();
        if (renderedMessages == null) {
            fail(String.format("feedback panel at path [%s] returned null messages", path));
        }
        if (numberOfMessages != renderedMessages.size()) {
            fail(
                    String.format(
                            "you expected '%d' messages for the feedback panel [%s], but there were actually '%d'",
                            numberOfMessages, path, renderedMessages.size()));
        }
    }

    public void assertFeedback(String path, String partOfMessage) {
        final FeedbackPanel fbp = (FeedbackPanel) tester.getComponentFromLastRenderedPage(path);
        final IModel<List<FeedbackMessage>> model = fbp.getFeedbackMessagesModel();
        final List<FeedbackMessage> renderedMessages = model.getObject();
        boolean found = false;
        for (FeedbackMessage actual : renderedMessages) {
            if (actual.getMessage().toString().contains(partOfMessage)) {
                found = true;
                break;
            }
        }
        if (!found) {
            fail("Missing expected feedback message: " + partOfMessage);
        }
    }
}
