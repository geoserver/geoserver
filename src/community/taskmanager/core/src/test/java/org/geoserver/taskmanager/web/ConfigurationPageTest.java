/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.impl.ConfigurationImpl;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.external.impl.FileServiceImpl;
import org.geoserver.taskmanager.tasks.CopyTableTaskTypeImpl;
import org.geoserver.taskmanager.tasks.CreateViewTaskTypeImpl;
import org.geoserver.taskmanager.tasks.FileLocalPublicationTaskTypeImpl;
import org.geoserver.taskmanager.tasks.FileRemotePublicationTaskTypeImpl;
import org.geoserver.taskmanager.tasks.MetadataSyncTaskTypeImpl;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.geoserver.taskmanager.web.panel.ButtonPanel;
import org.geoserver.taskmanager.web.panel.FileUploadPanel;
import org.geoserver.taskmanager.web.panel.NamePanel;
import org.geoserver.taskmanager.web.panel.NewTaskPanel;
import org.geoserver.taskmanager.web.panel.PanelListPanel;
import org.geoserver.taskmanager.web.panel.TaskParameterPanel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

public class ConfigurationPageTest extends AbstractBatchesPanelTest<ConfigurationPage> {

    @Rule public TemporaryFolder tempDir = new TemporaryFolder();

    @Rule public TemporaryFolder tempDestDir = new TemporaryFolder();

    @Autowired LookupService<FileService> fileServiceRegistry;

    private TaskManagerDataUtil util;
    private TaskManagerTaskUtil tutil;
    private IModel<Configuration> configModel;
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
        return configModel.getObject();
    }

    @Override
    protected ConfigurationPage newPage() {
        configModel.setObject(dao.init(configModel.getObject()));
        return new ConfigurationPage(configModel);
    }

    @Override
    protected String prefix() {
        return "configurationForm:";
    }

    @Override
    protected Collection<Batch> getBatches() {
        return configModel.getObject().getBatches().values();
    }

    @Before
    public void before() {
        super.before();
        util = TaskManagerBeans.get().getDataUtil();
        tutil = TaskManagerBeans.get().getTaskUtil();
        scheduler = GeoServerApplication.get().getBeanOfType(Scheduler.class);
        login();
        configModel = new Model<Configuration>(createConfiguration());

        // configure proper temp directory for testing
        FileServiceImpl fs = (FileServiceImpl) fileServiceRegistry.get("temp-directory");
        fs.setRootFolder(tempDestDir.getRoot().getAbsolutePath());
        try {
            tempDestDir.newFolder(); // at least one subfolder
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void after() {
        // clean-up
        dao.delete(configModel.getObject());
        logout();
        // restore temp directory service
        FileServiceImpl fs = (FileServiceImpl) fileServiceRegistry.get("temp-directory");
        fs.setRootFolder("/tmp");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTasksAndAttributes() {
        ConfigurationPage page = new ConfigurationPage(configModel);

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
        formTester.select("userPanel:type", 9);
        formTester.submit("submit");
        assertEquals(3, tasksPanel.getDataProvider().size());
        assertEquals(3, configModel.getObject().getTasks().size());
        assertEquals(
                MetadataSyncTaskTypeImpl.NAME,
                configModel.getObject().getTasks().get("task3").getType());
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

        assertNotNull(configModel.getObject().getTasks().get("new_name_for_task").getName());
        assertEquals(
                "new_name_for_task",
                configModel.getObject().getTasks().get("new_name_for_task").getName());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCopyTask() {
        ConfigurationPage page = new ConfigurationPage(configModel);

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
        assertEquals(3, configModel.getObject().getTasks().size());
        assertEquals(
                CopyTableTaskTypeImpl.NAME,
                configModel.getObject().getTasks().get("task3").getType());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteTasksAndSaveApplyCancel() throws SchedulerException {
        Batch dummyBatch = dummyBatch1();
        dummyBatch.setEnabled(true);
        dummyBatch = dao.save(dummyBatch);

        ConfigurationPage page = new ConfigurationPage(configModel);
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

        assertEquals(2, configModel.getObject().getTasks().size());
        assertEquals("my_configuration", configModel.getObject().getName());

        // apply
        page = new ConfigurationPage(configModel);
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

        assertEquals("the_greatest_configuration", configModel.getObject().getName());
        assertEquals(0, dao.init(configModel.getObject()).getTasks().size());

        // new batch has been scheduled
        assertNotNull(scheduler.getJobDetail(JobKey.jobKey(dummyBatch.getId().toString())));

        // save
        formTester.setValue("name", "foo_bar_configuration");
        tester.clickLink("configurationForm:save");
        tester.assertRenderedPage(ConfigurationsPage.class);
        assertEquals("foo_bar_configuration", dao.reload(configModel.getObject()).getName());

        dao.delete(dummyBatch);
    }

    @Test
    public void testTemplateNotValidated() {
        configModel.getObject().setTemplate(true);
        dao.save(configModel.getObject());
        ConfigurationPage page = new ConfigurationPage(configModel);
        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        // save with tasks results in validation errors, not with template
        tester.clickLink("configurationForm:save");
        tester.assertRenderedPage(TemplatesPage.class);
    }

    @Test
    public void testMissingOrDuplicateName() {
        ConfigurationPage page = new ConfigurationPage(new ConfigurationImpl());
        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        // save without name
        tester.clickLink("configurationForm:save");
        tester.assertRenderedPage(ConfigurationPage.class);
        assertFeedback("topFeedback", "'Name' is required");
        assertFeedback("bottomFeedback", "'Name' is required");

        FormTester formTester = tester.newFormTester("configurationForm");
        formTester.setValue("name", "my_configuration");
        tester.clickLink("configurationForm:save");

        assertFeedback("topFeedback", "Unique");
        assertFeedback("bottomFeedback", "Unique");
    }

    @Test
    public void testActionEditLayer() {

        Task task3 = tutil.initTask(FileRemotePublicationTaskTypeImpl.NAME, "task3");
        util.addTaskToConfiguration(configModel.getObject(), task3);
        configModel.setObject(dao.save(configModel.getObject()));

        ConfigurationPage page = new ConfigurationPage(configModel);
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

    @Test
    public void testActionFileUpload() throws IOException {

        Task task4 = tutil.initTask(FileLocalPublicationTaskTypeImpl.NAME, "task4");
        util.addTaskToConfiguration(configModel.getObject(), task4);
        configModel.setObject(dao.save(configModel.getObject()));

        ConfigurationPage page = new ConfigurationPage(configModel);
        tester.startPage(page);
        tester.assertRenderedPage(ConfigurationPage.class);

        tester.assertComponent(
                "configurationForm:attributesPanel:listContainer:items:10:itemProperties:2:component",
                PanelListPanel.class);
        tester.assertComponent(
                "configurationForm:attributesPanel:listContainer:items:10:itemProperties:2:component:listview:0:panel",
                ButtonPanel.class);

        tester.assertModelValue(
                "configurationForm:attributesPanel:listContainer:items:9:itemProperties:0:component",
                "fileService");
        tester.assertModelValue(
                "configurationForm:attributesPanel:listContainer:items:10:itemProperties:2:component:listview:0:panel:button",
                "Upload..");

        FormTester formTester = tester.newFormTester("configurationForm");
        formTester.submit(
                "attributesPanel:listContainer:items:10:itemProperties:2:component:listview:0:panel:button");
        assertFeedback("topFeedback", "You cannot execute this action with this value.");

        formTester.select(
                "attributesPanel:listContainer:items:9:itemProperties:1:component:dropdown", 1);
        formTester.submit(
                "attributesPanel:listContainer:items:10:itemProperties:2:component:listview:0:panel:button");
        tester.assertNoErrorMessage();

        tester.assertComponent("dialog:dialog:content:form:userPanel", FileUploadPanel.class);

        FormTester dialogFormTester = tester.newFormTester("dialog:dialog:content:form");
        dialogFormTester.submit("submit");
        tester.assertErrorMessages("Field 'File folder' is required.", "Field 'File' is required.");

        dialogFormTester.select("userPanel:folderSelection", 0);
        tester.assertComponent(
                "dialog:dialog:content:form:userPanel:fileInput", FileUploadField.class);
        tester.assertComponent("dialog:dialog:content:form:userPanel:prepare", CheckBox.class);
        tester.assertModelValue("dialog:dialog:content:form:userPanel:prepare", true);

        dialogFormTester.setFile(
                "userPanel:fileInput", new File(tempDir.newFile().getAbsolutePath()), "");

        dialogFormTester.submit("submit");

        tester.assertNoErrorMessage();

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
