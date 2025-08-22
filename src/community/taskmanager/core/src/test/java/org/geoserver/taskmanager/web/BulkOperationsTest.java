/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.taskmanager.AbstractWicketTaskManagerTest;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Run.Status;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BulkOperationsTest extends AbstractWicketTaskManagerTest {

    protected TaskManagerFactory fac;
    protected TaskManagerDao dao;
    protected TaskManagerDataUtil util;

    @Before
    public void before() {
        fac = TaskManagerBeans.get().getFac();
        dao = TaskManagerBeans.get().getDao();
        util = TaskManagerBeans.get().getDataUtil();

        login();
    }

    @After
    public void after() {
        logout();
    }

    @Test
    public void testBulkRunBatches() {

        Batch batch1 = fac.createBatch();
        batch1.setName("Z-BATCH");
        batch1 = dao.save(batch1);
        Batch batch2 = fac.createBatch();
        batch2.setName("Q-BATCH");
        batch2 = dao.save(batch2);

        tester.startPage(BulkOperationsPage.class);

        tester.assertComponent("tabs:panel:form:workspace", TextField.class);

        tester.assertComponent("tabs:panel:form:configuration", TextField.class);

        tester.assertComponent("tabs:panel:form:name", TextField.class);

        tester.assertComponent("tabs:panel:form:startDelay", NumberTextField.class);

        tester.assertComponent("tabs:panel:form:betweenDelay", NumberTextField.class);

        tester.assertComponent("tabs:panel:form:batchesFound", Label.class);

        tester.assertModelValue("tabs:panel:form:batchesFound", "Found 0 batches that match the specified criteria");

        FormTester formTester = tester.newFormTester("tabs:panel:form");

        formTester.setValue("configuration", null);

        formTester.setValue("betweenDelay", "60");

        tester.executeAjaxEvent("tabs:panel:form:configuration", "change");

        tester.assertModelValue("tabs:panel:form:batchesFound", "Found 2 batches that match the specified criteria");

        formTester.setValue("name", "Q%");

        tester.executeAjaxEvent("tabs:panel:form:name", "change");

        tester.assertModelValue("tabs:panel:form:batchesFound", "Found 1 batches that match the specified criteria");

        formTester.setValue("name", "%");

        tester.executeAjaxEvent("tabs:panel:form:name", "change");

        formTester.submit("run");

        tester.assertModelValue(
                "tabs:panel:dialog:dialog:modal:overlay:dialog:content:content:form:userPanel",
                "Are you sure you want to run 2 batches? This will take at least 1 minutes.");

        dao.delete(batch1);

        dao.delete(batch2);
    }

    @Test
    public void testImportConfigurations() throws IOException {

        Configuration temp = fac.createConfiguration();
        temp.setName("temp");
        temp.setTemplate(true);
        temp = dao.save(temp);

        tester.startPage(BulkOperationsPage.class);

        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, true);

        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("tabs")).setSelectedTab(1);

        tester.assertComponent("tabs:panel:form:template", DropDownChoice.class);

        tester.assertComponent("tabs:panel:form:fileUpload", FileUploadField.class);

        tester.assertComponent("tabs:panel:form:validate", CheckBox.class);

        tester.assertModelValue("tabs:panel:form:validate", true);

        FormTester formTester = tester.newFormTester("tabs:panel:form");

        formTester.select("template", 0);

        File csv = File.createTempFile("import", ".csv");
        FileUtils.writeStringToFile(csv, "name;description\na;aaa\nb;bbb\n", StandardCharsets.UTF_8);
        formTester.setFile("fileUpload", new org.apache.wicket.util.file.File(csv), "text/csv");

        formTester.submit("import");
        tester.assertModelValue(
                "tabs:panel:dialog:dialog:modal:overlay:dialog:content:content:form:userPanel",
                "Are you sure you want to import 2 configurations?");

        dao.delete(temp);
    }

    @Test
    public void testInitializeConfigurations() throws IOException, InterruptedException {

        Configuration config1 = fac.createConfiguration();
        config1.setName("Q-CONFIG");
        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(TestTaskTypeImpl.NAME);
        util.addTaskToConfiguration(config1, task1);
        config1 = dao.save(config1);
        task1 = config1.getTasks().get("task1");
        Batch batch1 = fac.createBatch();
        util.addBatchElement(batch1, task1);
        batch1.setName("@Initialize");
        batch1 = dao.save(batch1);
        util.addBatchToConfiguration(config1, batch1);
        config1 = dao.save(config1);

        Configuration config2 = fac.createConfiguration();
        config2.setName("Z-CONFIG");
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        util.addTaskToConfiguration(config2, task2);
        config2 = dao.save(config2);
        task2 = config2.getTasks().get("task2");
        Batch batch2 = fac.createBatch();
        util.addBatchElement(batch2, task2);
        batch2.setName("@Initialize");
        batch2 = dao.save(batch2);
        util.addBatchToConfiguration(config2, batch2);
        config2 = dao.save(config2);

        TaskManagerBeans.get().getBjService().reloadFromData();

        tester.startPage(BulkOperationsPage.class);

        tester.assertRenderedPage(BulkOperationsPage.class);

        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("tabs")).setSelectedTab(2);

        tester.assertRenderedPage(BulkOperationsPage.class);

        tester.assertComponent("tabs:panel:form:workspace", TextField.class);

        tester.assertComponent("tabs:panel:form:configuration", TextField.class);

        tester.assertComponent("tabs:panel:form:startDelay", NumberTextField.class);

        tester.assertComponent("tabs:panel:form:betweenDelay", NumberTextField.class);

        tester.assertComponent("tabs:panel:form:configsFound", Label.class);

        tester.assertModelValue(
                "tabs:panel:form:configsFound", "Found 2 configurations that match the specified criteria");

        FormTester formTester = tester.newFormTester("tabs:panel:form");

        formTester.setValue("configuration", "Q%");

        formTester.setValue("betweenDelay", "60");

        tester.executeAjaxEvent("tabs:panel:form:configuration", "change");

        tester.assertModelValue(
                "tabs:panel:form:configsFound", "Found 1 configurations that match the specified criteria");

        formTester.setValue("configuration", "%");

        tester.executeAjaxEvent("tabs:panel:form:configuration", "change");

        formTester.submit("run");

        tester.assertModelValue(
                "tabs:panel:dialog:dialog:modal:overlay:dialog:content:content:form:userPanel",
                "Are you sure you want to initialize 2 configurations? This will take at least 1 minutes.");

        formTester = tester.newFormTester("tabs:panel:dialog:dialog:modal:overlay:dialog:content:content:form");

        formTester.submit("submit");

        do {
            Thread.sleep(100);
            batch1 = dao.reload(batch1);
        } while (batch1 == null
                || batch1.getLatestBatchRun() != null
                        && batch1.getLatestBatchRun().getBatchRun().getStatus() != Status.COMMITTED);

        Thread.sleep(500);
        config1 = dao.reload(config1);
        assertTrue(config1.isValidated());

        dao.delete(batch1);
        dao.delete(batch2);
        dao.delete(config1);
        dao.delete(config2);
    }
}
