/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.TestReportServiceImpl;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.report.ReportService.Filter;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the batch job.
 *
 * @author Niels Charlier
 */
public class BatchJobTest extends AbstractTaskManagerTest {

    private static final String ATT_DELAY = "delay";

    private static final String ATT_FAIL = "fail";

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil util;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    @Autowired private TestTaskTypeImpl testTaskType;

    @Autowired private TestReportServiceImpl testReportService;

    private Configuration config;

    private Batch batch;

    @Before
    public void setupBatch() {
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");
        util.setConfigurationAttribute(config, ATT_FAIL, "false");
        util.setConfigurationAttribute(config, ATT_DELAY, "0");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(TestTaskTypeImpl.NAME);
        util.addTaskToConfiguration(config, task1);

        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        util.addTaskToConfiguration(config, task2);

        Task task3 = fac.createTask();
        task3.setName("task3");
        task3.setType(TestTaskTypeImpl.NAME);
        util.setTaskParameterToAttribute(task3, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        util.setTaskParameterToAttribute(task3, TestTaskTypeImpl.PARAM_DELAY, ATT_DELAY);
        util.addTaskToConfiguration(config, task3);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");
        task3 = config.getTasks().get("task3");

        batch = fac.createBatch();

        batch.setName("my_batch");
        util.addBatchElement(batch, task1);
        util.addBatchElement(batch, task2);
        util.addBatchElement(batch, task3);

        batch = bjService.saveAndSchedule(batch);

        config = dao.init(config);
        task1 = config.getTasks().get("task1");

        // clear report service
        testReportService.clear();
        testReportService.setFilter(Filter.ALL);
    }

    @After
    public void clearDataFromDatabase() {
        dao.delete(batch);
        dao.delete(config);
    }

    @Test
    public void testSuccess() throws InterruptedException, SchedulerException {

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertEquals(4, testTaskType.getStatus().get("my_batch:my_config/task1").intValue());
        assertEquals(4, testTaskType.getStatus().get("my_batch:my_config/task2").intValue());
        assertEquals(4, testTaskType.getStatus().get("my_batch:my_config/task3").intValue());

        assertEquals(
                Run.Status.COMMITTED, dao.getLatestRun(batch.getElements().get(0)).getStatus());
        assertEquals(
                Run.Status.COMMITTED, dao.getLatestRun(batch.getElements().get(1)).getStatus());
        assertEquals(
                Run.Status.COMMITTED, dao.getLatestRun(batch.getElements().get(2)).getStatus());

        assertEquals(
                "Report: Batch my_batch was successful",
                testReportService.getLastReport().getTitle());
        assertTrue(
                testReportService
                        .getLastReport()
                        .getContent()
                        .contains("my_config/task1, started"));
        assertTrue(
                testReportService
                        .getLastReport()
                        .getContent()
                        .contains("my_config/task2, started"));
        assertTrue(
                testReportService
                        .getLastReport()
                        .getContent()
                        .contains("my_config/task3, started"));
        assertTrue(testReportService.getLastReport().getContent().contains(", ended"));
        assertTrue(testReportService.getLastReport().getContent().contains("status is COMMITTED"));

        // repeat with different report filter
        testReportService.clear();
        testReportService.setFilter(Filter.FAILED_AND_CANCELLED);

        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertNull(testReportService.getLastReport());
    }

    @Test
    public void testFailed() throws InterruptedException, SchedulerException {

        util.setConfigurationAttribute(config, ATT_FAIL, "true");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertEquals(0, testTaskType.getStatus().get("my_batch:my_config/task1").intValue());
        assertEquals(0, testTaskType.getStatus().get("my_batch:my_config/task2").intValue());
        assertEquals(2, testTaskType.getStatus().get("my_batch:my_config/task3").intValue());

        assertEquals(
                Run.Status.ROLLED_BACK, dao.getLatestRun(batch.getElements().get(0)).getStatus());
        assertEquals(
                Run.Status.ROLLED_BACK, dao.getLatestRun(batch.getElements().get(1)).getStatus());
        assertEquals(Run.Status.FAILED, dao.getLatestRun(batch.getElements().get(2)).getStatus());

        assertEquals(
                "Report: Batch my_batch has failed", testReportService.getLastReport().getTitle());
        assertTrue(
                testReportService.getLastReport().getContent().contains("status is ROLLED_BACK"));
        assertTrue(testReportService.getLastReport().getContent().contains("status is FAILED"));
        assertTrue(
                testReportService
                        .getLastReport()
                        .getContent()
                        .contains("message: purposely failed task (check logs for more details"));
    }

    @Test
    public void testCancel() throws InterruptedException, SchedulerException {

        config.getAttributes().remove("");
        config = dao.save(config);
        util.setConfigurationAttribute(config, ATT_DELAY, "5000");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (testTaskType.getStatus().get("my_batch:my_config/task3") == null) {}

        Thread.sleep(1000);
        batch = dao.initHistory(batch);
        BatchRun br = batch.getBatchRuns().get(batch.getBatchRuns().size() - 1);
        br.setInterruptMe(true);
        br = dao.save(br);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertEquals(0, testTaskType.getStatus().get("my_batch:my_config/task1").intValue());
        assertEquals(0, testTaskType.getStatus().get("my_batch:my_config/task2").intValue());
        assertEquals(0, testTaskType.getStatus().get("my_batch:my_config/task3").intValue());

        assertEquals(
                Run.Status.ROLLED_BACK, dao.getLatestRun(batch.getElements().get(0)).getStatus());
        assertEquals(
                Run.Status.ROLLED_BACK, dao.getLatestRun(batch.getElements().get(1)).getStatus());
        assertEquals(
                Run.Status.ROLLED_BACK, dao.getLatestRun(batch.getElements().get(2)).getStatus());

        assertEquals(
                "Report: Batch my_batch was cancelled",
                testReportService.getLastReport().getTitle());
        assertTrue(
                testReportService.getLastReport().getContent().contains("status is ROLLED_BACK"));

        // run with filter
        testReportService.clear();
        testReportService.setFilter(Filter.FAILED_ONLY);

        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertNull(testReportService.getLastReport());
    }

    @Test
    public void testParameterValidation() throws InterruptedException, SchedulerException {

        util.setConfigurationAttribute(config, ATT_DELAY, "bla");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertEquals(0, testTaskType.getStatus().get("my_batch:my_config/task1").intValue());
        assertEquals(0, testTaskType.getStatus().get("my_batch:my_config/task2").intValue());
        assertEquals(1, testTaskType.getStatus().get("my_batch:my_config/task3").intValue());

        assertEquals(
                Run.Status.ROLLED_BACK, dao.getLatestRun(batch.getElements().get(0)).getStatus());
        assertEquals(
                Run.Status.ROLLED_BACK, dao.getLatestRun(batch.getElements().get(1)).getStatus());
        assertEquals(Run.Status.FAILED, dao.getLatestRun(batch.getElements().get(2)).getStatus());

        assertEquals(
                "Report: Batch my_batch has failed", testReportService.getLastReport().getTitle());
        assertTrue(
                testReportService.getLastReport().getContent().contains("status is ROLLED_BACK"));
        assertTrue(testReportService.getLastReport().getContent().contains("status is FAILED"));
        assertTrue(
                testReportService
                        .getLastReport()
                        .getContent()
                        .contains(
                                "message: There were validation errors: [bla is not a valid parameter value for parameter delay in task type Test] (check logs for more details)"));
    }
}
