/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geotools.util.logging.Logging;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the batch job service.
 *
 * @author Niels Charlier
 */
@Service("batchJobService")
public class BatchJobServiceImpl
        implements BatchJobService, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = Logging.getLogger(BatchJobServiceImpl.class);

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private Scheduler scheduler;

    private boolean init = true;

    @Transactional("tmTransactionManager")
    protected void schedule(Batch batch) throws SchedulerException {
        // check for inactive tasks
        for (BatchElement be : batch.getElements()) {
            if (!be.getTask().isActive()) {
                throw new IllegalArgumentException(
                        "Cannot save & schedule a batch with inactive tasks!");
            }
        }

        JobKey jobKey = JobKey.jobKey(batch.getId().toString());

        boolean exists = scheduler.checkExists(jobKey);

        if (!batch.isActive()) {
            if (exists) {
                scheduler.deleteJob(jobKey);
            }

            LOGGER.log(Level.INFO, "Successfully unscheduled batch " + batch.getFullName());

        } else {
            if (!exists) {
                JobDetail jobDetail =
                        JobBuilder.newJob(BatchJobImpl.class)
                                .withIdentity(jobKey)
                                .storeDurably()
                                .build();

                scheduler.addJob(jobDetail, true);
            }

            TriggerKey triggerKey = TriggerKey.triggerKey(batch.getId().toString());
            scheduler.unscheduleJob(triggerKey);

            if (batch.isEnabled()
                    && batch.getFrequency() != null
                    && !batch.getElements().isEmpty()
                    && (batch.getConfiguration() == null
                            || batch.getConfiguration().isValidated())) {
                Trigger trigger =
                        TriggerBuilder.newTrigger()
                                .withIdentity(triggerKey)
                                .forJob(jobKey)
                                .withSchedule(
                                        CronScheduleBuilder.cronSchedule(batch.getFrequency()))
                                .build();

                scheduler.scheduleJob(trigger);
            }

            LOGGER.log(Level.INFO, "Successfully (re)scheduled batch " + batch.getFullName());
        }
    }

    @Override
    @Transactional("tmTransactionManager")
    public Batch saveAndSchedule(Batch batch) {
        batch = dao.save(batch);
        if (batch.getConfiguration() == null || !batch.getConfiguration().isTemplate()) {
            try {
                schedule(batch);
            } catch (SchedulerException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                throw new IllegalArgumentException(e);
            }
        }
        return batch;
    }

    @Override
    @Transactional("tmTransactionManager")
    public Configuration saveAndSchedule(Configuration config) {
        config = dao.save(config);
        if (!config.isTemplate()) {
            try {
                for (Batch batch : config.getBatches().values()) {
                    schedule(batch);
                }
            } catch (SchedulerException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                throw new IllegalArgumentException(e);
            }
        }
        return config;
    }

    @Override
    @Transactional("tmTransactionManager")
    public Batch remove(Batch batch) {
        try {
            scheduler.deleteJob(JobKey.jobKey(batch.getId().toString()));
        } catch (SchedulerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new IllegalArgumentException(e);
        }
        return dao.remove(batch);
    }

    @Override
    @Transactional("tmTransactionManager")
    public Configuration remove(Configuration config) {
        config = dao.lockReload(config);
        for (Batch batch : config.getBatches().values()) {
            try {
                scheduler.deleteJob(JobKey.jobKey(batch.getId().toString()));
            } catch (SchedulerException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                throw new IllegalArgumentException(e);
            }
        }
        return dao.remove(config);
    }

    @Override
    @Transactional("tmTransactionManager")
    public void reloadFromData() {
        LOGGER.info("Reloading scheduler from data.");

        try {
            scheduler.clear();
        } catch (SchedulerException e) {
            LOGGER.log(Level.WARNING, "Failed to clear scheduler ", e);
            throw new IllegalStateException(e);
        }

        for (Batch batch : dao.getAllBatches()) {
            try {
                schedule(batch);
            } catch (SchedulerException | IllegalArgumentException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to schedule batch " + batch.getName() + ", disabling. ",
                        e);
                batch.setEnabled(false);
                dao.save(batch);
            }

            for (BatchRun br : dao.getCurrentBatchRuns(batch)) {
                LOGGER.log(
                        Level.WARNING,
                        "Automatically closing inactive batch run at start-up: "
                                + batch.getFullName());
                dataUtil.closeBatchRun(br, "closed at start-up");
            }
        }
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

    @Transactional("tmTransactionManager")
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // call only once at start-up, so not for child contexts.
        if (event.getApplicationContext().getParent() == null) {
            if (init) {
                reloadFromData();
            } else {
                LOGGER.info("Skipping initialization as specified in configuration.");
            }
            try {
                scheduler.start();
            } catch (SchedulerException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    @Override
    @Transactional("tmTransactionManager")
    public String scheduleNow(Batch batch) {
        batch = dao.reload(batch);
        if (batch.getElements().isEmpty()) {
            LOGGER.log(Level.WARNING, "Ignoring manual empty batch run: " + batch.getFullName());
            return null;
        }

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        try {
            scheduler.scheduleJob(trigger);
        } catch (SchedulerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return trigger.getKey().getName();
    }

    @Override
    @Transactional("tmTransactionManager")
    public void interrupt(BatchRun batchRun) {
        batchRun = dao.lockReload(batchRun);
        if (!batchRun.getStatus().isClosed()) {
            if (batchRun.getSchedulerReference() != null) {
                try {
                    TriggerKey triggerKey = TriggerKey.triggerKey(batchRun.getSchedulerReference());
                    Trigger trigger = scheduler.getTrigger(triggerKey);
                    boolean lastFire = trigger != null ? (trigger.getNextFireTime() == null) : true;
                    TriggerState state = scheduler.getTriggerState(triggerKey);

                    // the blocked check only works thanks to @DisallowConcurrentExecution
                    // otherwise it would go straight back to waiting and we wouldn't know
                    // when the job was finished.
                    if ((lastFire && state == TriggerState.NONE)
                            || (!lastFire && state != TriggerState.BLOCKED)) {
                        dataUtil.closeBatchRun(batchRun, "manually closed due to inactivity");
                        return;
                    }
                } catch (SchedulerException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            batchRun.setInterruptMe(true);
            dao.save(batchRun);
        }
    }
}
