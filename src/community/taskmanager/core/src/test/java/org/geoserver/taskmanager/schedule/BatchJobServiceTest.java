/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.lang3.time.DateUtils;
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
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the batch job service.
 *
 * @author Niels Charlier
 */
public class BatchJobServiceTest extends AbstractTaskManagerTest {

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil util;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    private Configuration config;

    private Batch batch;

    private Batch otherBatch;

    @Before
    public void setupBatch() {
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(TestTaskTypeImpl.NAME);
        util.addTaskToConfiguration(config, task1);
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        util.addTaskToConfiguration(config, task2);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");

        batch = fac.createBatch();
        batch.setName("my_batch");
        util.addBatchElement(batch, task1);

        otherBatch = fac.createBatch();
        otherBatch.setName("my_other_batch");
        util.addBatchElement(otherBatch, task2);

        batch = bjService.saveAndSchedule(batch);

        otherBatch = bjService.saveAndSchedule(otherBatch);
    }

    @After
    public void clearDataFromDatabase() {
        dao.delete(batch);
        dao.delete(otherBatch);
        dao.delete(config);
    }

    @Test
    public void testBatchJobService() throws SchedulerException {
        JobKey jobKey = new JobKey(batch.getId().toString());
        TriggerKey triggerKey = new TriggerKey(batch.getId().toString());

        // not scheduled yet
        assertTrue(scheduler.checkExists(jobKey));
        assertFalse(scheduler.checkExists(triggerKey));

        // give it a frequency
        batch.setFrequency("0 0 * * * ?");
        batch.setEnabled(true);
        bjService.saveAndSchedule(batch);

        assertTrue(scheduler.checkExists(jobKey));
        assertTrue(scheduler.checkExists(triggerKey));
        Trigger trigger = scheduler.getTrigger(triggerKey);
        Date nextFireTime = DateUtils.ceiling(new Date(), Calendar.HOUR);
        assertEquals(nextFireTime, trigger.getNextFireTime());

        // change the frequency
        batch.setFrequency("0 30 * * * ?");
        bjService.saveAndSchedule(batch);

        assertTrue(scheduler.checkExists(jobKey));
        assertTrue(scheduler.checkExists(triggerKey));
        trigger = scheduler.getTrigger(triggerKey);
        nextFireTime = DateUtils.addMinutes(DateUtils.round(new Date(), Calendar.HOUR), 30);
        assertEquals(nextFireTime, trigger.getNextFireTime());

        // de-activate it
        batch.setEnabled(false);
        bjService.saveAndSchedule(batch);

        assertTrue(scheduler.checkExists(jobKey));
        assertFalse(scheduler.checkExists(triggerKey));

        // delete it
        batch.setActive(false);
        bjService.saveAndSchedule(batch);

        assertFalse(scheduler.checkExists(jobKey));
        assertFalse(scheduler.checkExists(triggerKey));
    }

    @Test
    public void testBulkSchedule() throws SchedulerException {
        bjService.scheduleNow(Lists.newArrayList(batch, otherBatch), 60, 10);

        JobKey jobKey = new JobKey(batch.getId().toString());

        JobKey otherJobKey = new JobKey(otherBatch.getId().toString());

        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
        assertEquals(1, triggers.size());
        List<? extends Trigger> otherTriggers = scheduler.getTriggersOfJob(otherJobKey);
        assertEquals(1, otherTriggers.size());

        assertEquals(
                triggers.get(0).getStartTime().getTime() + 10000,
                otherTriggers.get(0).getStartTime().getTime());
    }

    @Test
    public void testBulkScheduleWithCallback() throws SchedulerException {
        Set<String> calledbackBatches = new HashSet<>();
        bjService.scheduleNow(
                Lists.newArrayList(batch, otherBatch),
                0,
                0,
                new Consumer<Batch>() {
                    @Override
                    public void accept(Batch batch) {
                        calledbackBatches.add(batch.getName());
                    }
                });

        JobKey jobKey = new JobKey(batch.getId().toString());
        JobKey otherJobKey = new JobKey(otherBatch.getId().toString());
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
        assertEquals(1, triggers.size());
        List<? extends Trigger> otherTriggers = scheduler.getTriggersOfJob(otherJobKey);
        assertEquals(1, otherTriggers.size());

        while (scheduler.getTriggerState(triggers.get(0).getKey()) != TriggerState.NONE) {}
        while (scheduler.getTriggerState(otherTriggers.get(0).getKey()) != TriggerState.NONE) {}

        assertEquals(2, calledbackBatches.size());
    }
}
