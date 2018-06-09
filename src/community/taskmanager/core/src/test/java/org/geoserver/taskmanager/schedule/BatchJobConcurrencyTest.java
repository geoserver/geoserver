/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
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
 * Tests concurrency with batch jobs.
 *
 * @author Niels Charlier
 */
public class BatchJobConcurrencyTest extends AbstractTaskManagerTest {

    private static final String ATT_DELAY2_COMMIT = "delay2_commit";

    private static final String ATT_DELAY2 = "delay2";

    private static final String ATT_DELAY1_COMMIT = "delay1_commit";

    private static final String ATT_DELAY1 = "delay1";

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil util;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    @Autowired private TestTaskTypeImpl testTaskType;

    private Configuration config;

    private Batch batch1;

    private Batch batch2;

    @Before
    public void setupBatch() {
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(TestTaskTypeImpl.NAME);
        util.setTaskParameterToAttribute(task1, TestTaskTypeImpl.PARAM_DELAY, ATT_DELAY1);
        util.setTaskParameterToAttribute(
                task1, TestTaskTypeImpl.PARAM_DELAY_COMMIT, ATT_DELAY1_COMMIT);
        util.addTaskToConfiguration(config, task1);

        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        util.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_DELAY, ATT_DELAY2);
        util.setTaskParameterToAttribute(
                task1, TestTaskTypeImpl.PARAM_DELAY_COMMIT, ATT_DELAY2_COMMIT);
        util.addTaskToConfiguration(config, task2);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");

        batch1 = fac.createBatch();
        batch1.setName("batch_1");
        util.addBatchElement(batch1, task1);
        util.addBatchElement(batch1, task2);

        batch2 = fac.createBatch();
        batch2.setName("batch_2");
        util.addBatchElement(batch2, task2);

        batch1 = bjService.saveAndSchedule(batch1);
        batch2 = bjService.saveAndSchedule(batch2);
    }

    @After
    public void clearDataFromDatabase() {
        dao.delete(batch1);
        dao.delete(batch2);
        dao.delete(config);
    }

    @Test
    public void testConcurrency() throws InterruptedException, SchedulerException {

        util.setConfigurationAttribute(config, ATT_DELAY1, "1000");
        util.setConfigurationAttribute(config, ATT_DELAY2, "5000");
        util.setConfigurationAttribute(config, ATT_DELAY1_COMMIT, "0");
        util.setConfigurationAttribute(config, ATT_DELAY2_COMMIT, "0");

        // start both tasks simultaneously
        Trigger trigger1 =
                TriggerBuilder.newTrigger().forJob(batch1.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger1);
        Trigger trigger2 =
                TriggerBuilder.newTrigger().forJob(batch2.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger2);

        // wait until task 2 in batch 1 has started
        while (testTaskType.getStatus().get("batch_1:my_config/task2") == null
                || testTaskType.getStatus().get("batch_1:my_config/task2").intValue() < 1) {
            Thread.sleep(100);
        }

        // verify it has finished task 2 in batch 1 first
        assertTrue(testTaskType.getStatus().get("batch_2:my_config/task2").intValue() >= 2);

        // make sure it is all finished before we delete
        while (scheduler.getTriggerState(trigger1.getKey()) != TriggerState.NONE) {}

        while (scheduler.getTriggerState(trigger2.getKey()) != TriggerState.NONE) {}
    }

    @Test
    public void testConcurrencyCommit() throws InterruptedException, SchedulerException {

        util.setConfigurationAttribute(config, ATT_DELAY1, "0");
        util.setConfigurationAttribute(config, ATT_DELAY2, "0");
        util.setConfigurationAttribute(config, ATT_DELAY1_COMMIT, "1000");
        util.setConfigurationAttribute(config, ATT_DELAY2_COMMIT, "5000");

        // start both tasks simultaneously
        Trigger trigger1 =
                TriggerBuilder.newTrigger().forJob(batch1.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger1);
        Trigger trigger2 =
                TriggerBuilder.newTrigger().forJob(batch2.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger2);

        // wait until task 2 in batch 1 has started committing
        while (testTaskType.getStatus().get("batch_1:my_config/task2") == null
                || testTaskType.getStatus().get("batch_1:my_config/task2").intValue() < 3) {
            Thread.sleep(100);
        }

        // verify it has committed task 2 in batch 1 first
        assertEquals(4, testTaskType.getStatus().get("batch_2:my_config/task2").intValue());

        // make sure it is all finished before we delete
        while (scheduler.getTriggerState(trigger1.getKey()) != TriggerState.NONE) {}

        while (scheduler.getTriggerState(trigger2.getKey()) != TriggerState.NONE) {}
    }
}
