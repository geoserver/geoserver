/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

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
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BulkOperationsTest extends AbstractWicketTaskManagerTest {

    protected TaskManagerFactory fac;
    protected TaskManagerDao dao;

    @Before
    public void before() {
        fac = TaskManagerBeans.get().getFac();
        dao = TaskManagerBeans.get().getDao();

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

        tester.assertComponent("form:tabs:panel:workspace", TextField.class);

        tester.assertComponent("form:tabs:panel:configuration", TextField.class);

        tester.assertComponent("form:tabs:panel:name", TextField.class);

        tester.assertComponent("form:tabs:panel:startDelay", NumberTextField.class);

        tester.assertComponent("form:tabs:panel:betweenDelay", NumberTextField.class);

        tester.assertComponent("form:tabs:panel:batchesFound", Label.class);

        tester.assertModelValue(
                "form:tabs:panel:batchesFound",
                "Found 0 batches that match the specified criteria");

        FormTester formTester = tester.newFormTester("form");

        formTester.setValue("tabs:panel:configuration", null);

        formTester.setValue("tabs:panel:betweenDelay", "60");

        tester.executeAjaxEvent("form:tabs:panel:configuration", "change");

        tester.assertModelValue(
                "form:tabs:panel:batchesFound",
                "Found 2 batches that match the specified criteria");

        formTester.setValue("tabs:panel:name", "Q%");

        tester.executeAjaxEvent("form:tabs:panel:name", "change");

        tester.assertModelValue(
                "form:tabs:panel:batchesFound",
                "Found 1 batches that match the specified criteria");

        formTester.setValue("tabs:panel:name", "%");

        tester.executeAjaxEvent("form:tabs:panel:name", "change");

        formTester.submit("tabs:panel:run");

        tester.assertModelValue(
                "form:tabs:panel:dialog:dialog:content:form:userPanel",
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

        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("form:tabs")).setSelectedTab(1);

        tester.assertComponent("form:tabs:panel:template", DropDownChoice.class);

        tester.assertComponent("form:tabs:panel:fileUpload", FileUploadField.class);

        tester.assertComponent("form:tabs:panel:validate", CheckBox.class);

        tester.assertModelValue("form:tabs:panel:validate", true);

        FormTester formTester = tester.newFormTester("form");

        formTester.select("tabs:panel:template", 0);

        File csv = File.createTempFile("import", ".csv");
        FileUtils.writeStringToFile(
                csv, "name;description\na;aaa\nb;bbb\n", StandardCharsets.UTF_8);
        formTester.setFile(
                "tabs:panel:fileUpload", new org.apache.wicket.util.file.File(csv), "text/csv");
        formTester.submit("tabs:panel:import");

        tester.assertModelValue(
                "form:tabs:panel:dialog:dialog:content:form:userPanel",
                "Are you sure you want to import 2 configurations?");

        dao.delete(temp);
    }

    @Test
    public void testInitializeConfigurations() throws IOException {

        Configuration config1 = fac.createConfiguration();
        config1.setName("Q-CONFIG");
        Batch batch1 = fac.createBatch();
        batch1.setName("@Initialize");
        batch1 = dao.save(batch1);
        TaskManagerBeans.get().getDataUtil().addBatchToConfiguration(config1, batch1);
        config1 = dao.save(config1);

        Configuration config2 = fac.createConfiguration();
        config2.setName("Z-CONFIG");
        Batch batch2 = fac.createBatch();
        batch2.setName("@Initialize");
        batch2 = dao.save(batch2);
        TaskManagerBeans.get().getDataUtil().addBatchToConfiguration(config2, batch2);
        config2 = dao.save(config2);

        tester.startPage(BulkOperationsPage.class);

        tester.assertRenderedPage(BulkOperationsPage.class);

        ((TabbedPanel<?>) tester.getComponentFromLastRenderedPage("form:tabs")).setSelectedTab(2);

        tester.assertRenderedPage(BulkOperationsPage.class);

        tester.assertComponent("form:tabs:panel:workspace", TextField.class);

        tester.assertComponent("form:tabs:panel:configuration", TextField.class);

        tester.assertComponent("form:tabs:panel:startDelay", NumberTextField.class);

        tester.assertComponent("form:tabs:panel:betweenDelay", NumberTextField.class);

        tester.assertComponent("form:tabs:panel:configsFound", Label.class);

        tester.assertModelValue(
                "form:tabs:panel:configsFound",
                "Found 2 configurations that match the specified criteria");

        FormTester formTester = tester.newFormTester("form");

        formTester.setValue("tabs:panel:configuration", "Q%");

        formTester.setValue("tabs:panel:betweenDelay", "60");

        tester.executeAjaxEvent("form:tabs:panel:configuration", "change");

        tester.assertModelValue(
                "form:tabs:panel:configsFound",
                "Found 1 configurations that match the specified criteria");

        formTester.setValue("tabs:panel:configuration", "%");

        tester.executeAjaxEvent("form:tabs:panel:configuration", "change");

        formTester.submit("tabs:panel:run");

        tester.assertModelValue(
                "form:tabs:panel:dialog:dialog:content:form:userPanel",
                "Are you sure you want to initialize 2 configurations? This will take at least 1 minutes.");

        dao.delete(batch1);
        dao.delete(batch2);
        dao.delete(config1);
        dao.delete(config2);
    }
}
