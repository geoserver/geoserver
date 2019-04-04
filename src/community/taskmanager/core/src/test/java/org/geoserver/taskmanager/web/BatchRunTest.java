/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import java.util.Date;
import org.geoserver.taskmanager.AbstractWicketTaskManagerTest;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.Run.Status;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BatchRunTest extends AbstractWicketTaskManagerTest {

    protected TaskManagerFactory fac;
    protected TaskManagerDao dao;
    private TaskManagerDataUtil util;
    private TaskManagerTaskUtil tutil;
    private BatchJobService bjservice;
    private Batch batch;

    @Before
    public void before() {
        fac = TaskManagerBeans.get().getFac();
        dao = TaskManagerBeans.get().getDao();
        util = TaskManagerBeans.get().getDataUtil();
        tutil = TaskManagerBeans.get().getTaskUtil();
        bjservice = TaskManagerBeans.get().getBjService();
        login();
        batch = createBatch();
    }

    @After
    public void after() {
        dao.delete(batch.getConfiguration());
        logout();
    }

    public Batch createBatch() {
        Configuration config = fac.createConfiguration();
        config.setName("my_configuration");
        config.setDescription("my very new configuration");
        config.setWorkspace("gs");

        Task task1 = tutil.initTask(TestTaskTypeImpl.NAME, "task1");
        util.addTaskToConfiguration(config, task1);

        Task task2 = tutil.initTask(TestTaskTypeImpl.NAME, "task2");
        task2.getParameters().get(TestTaskTypeImpl.PARAM_DELAY).setValue("10000");
        util.addTaskToConfiguration(config, task2);

        Task task3 = tutil.initTask(TestTaskTypeImpl.NAME, "task3");
        util.addTaskToConfiguration(config, task3);

        config.setValidated(true);
        config = dao.save(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");
        task3 = config.getTasks().get("task3");

        Batch batch = fac.createBatch();
        util.addBatchElement(batch, task1);
        util.addBatchElement(batch, task2);
        util.addBatchElement(batch, task3);

        batch.setConfiguration(config);
        batch.setName("this is the test batch");
        batch.setDescription("this is the test description");
        batch.setEnabled(true);
        batch.setWorkspace("gs");
        return bjservice.saveAndSchedule(batch);
    }

    @Test
    public void testRunStop() throws InterruptedException {
        tester.startPage(new BatchesPage());
        tester.assertRenderedPage(BatchesPage.class);

        tester.clickLink(
                "batchesPanel:form:batchesPanel:listContainer:items:1:itemProperties:6:component:link");

        Thread.sleep(500);

        tester.clickLink("batchesPanel:refresh");

        tester.assertModelValue(
                "batchesPanel:form:batchesPanel:listContainer:items:2:itemProperties:7:component",
                Status.RUNNING);

        tester.clickLink(
                "batchesPanel:form:batchesPanel:listContainer:items:2:itemProperties:7:component:link");

        tester.assertRenderedPage(BatchRunsPage.class);

        tester.assertModelValue(
                "form:runsPanel:listContainer:items:1:itemProperties:2:component", Status.RUNNING);

        tester.clickLink("form:runsPanel:listContainer:items:1:itemProperties:0:component:link");

        tester.assertRenderedPage(BatchRunPage.class);

        tester.assertModelValue(
                "runPanel:listContainer:items:1:itemProperties:3:component",
                Status.READY_TO_COMMIT);

        tester.assertModelValue(
                "runPanel:listContainer:items:2:itemProperties:3:component", Status.RUNNING);

        tester.clickLink("close");

        tester.assertRenderedPage(BatchRunsPage.class);

        tester.clickLink("form:runsPanel:listContainer:items:1:itemProperties:3:component:link");

        BatchRun br = dao.initHistory(batch).getBatchRuns().get(0);
        while (!(br = dao.reload(br)).getStatus().isClosed()) {
            Thread.sleep(100);
        }

        tester.clickLink("refresh");

        tester.assertModelValue(
                "form:runsPanel:listContainer:items:2:itemProperties:2:component",
                Status.ROLLED_BACK);

        tester.clickLink("form:runsPanel:listContainer:items:2:itemProperties:0:component:link");

        tester.assertRenderedPage(BatchRunPage.class);

        tester.assertModelValue(
                "runPanel:listContainer:items:1:itemProperties:3:component", Status.ROLLED_BACK);

        tester.assertModelValue(
                "runPanel:listContainer:items:2:itemProperties:3:component", Status.ROLLED_BACK);

        tester.clickLink("close");

        tester.assertRenderedPage(BatchRunsPage.class);

        tester.clickLink("close");

        tester.assertRenderedPage(BatchesPage.class);

        tester.clickLink("batchesPanel:refresh");

        tester.assertModelValue(
                "batchesPanel:form:batchesPanel:listContainer:items:3:itemProperties:7:component",
                Status.ROLLED_BACK);
    }

    @Test
    public void testEmptyBatchRun() throws InterruptedException {

        BatchRun br1 = fac.createBatchRun();
        Run run = fac.createRun();
        run.setStart(new Date());
        run.setStatus(Status.RUNNING);
        run.setBatchRun(br1);
        br1.getRuns().add(run);
        br1.setBatch(batch);
        br1 = dao.save(br1);

        BatchRun br2 = fac.createBatchRun();
        br2.setBatch(batch);
        dao.save(br2);

        tester.startPage(new BatchesPage());
        tester.assertRenderedPage(BatchesPage.class);

        tester.assertModelValue(
                "batchesPanel:form:batchesPanel:listContainer:items:1:itemProperties:7:component",
                Status.RUNNING);

        tester.clickLink(
                "batchesPanel:form:batchesPanel:listContainer:items:1:itemProperties:7:component:link");

        tester.assertRenderedPage(BatchRunsPage.class);

        tester.assertModelValue(
                "form:runsPanel:listContainer:items:1:itemProperties:2:component", Status.RUNNING);
    }
}
