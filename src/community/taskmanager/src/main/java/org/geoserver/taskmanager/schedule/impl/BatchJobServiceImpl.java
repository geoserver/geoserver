/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.LookupService;
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
 *
 */
@Service
public class BatchJobServiceImpl implements BatchJobService, ApplicationListener<ContextRefreshedEvent>  {
    
    private static final Logger LOGGER = Logging.getLogger(BatchJobServiceImpl.class);
    
    @Autowired 
    TaskManagerDao dao;

    @Autowired 
    TaskManagerDataUtil dataUtil;
    
    @Autowired
    Scheduler scheduler;

    @Autowired
    LookupService<TaskType> taskTypes;
    
    @Autowired
    TaskManagerFactory factory;

    @Transactional("tmTransactionManager")
    protected void schedule(Batch batch) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(batch.getFullName());        
        
        try {
            boolean exists = scheduler.checkExists(jobKey);
            
            if (!batch.isActive()) {
                if (exists) {
                    scheduler.deleteJob(jobKey);
                }
                
                LOGGER.log(Level.INFO, "Successfully unscheduled batch " + batch.getFullName());
                
            } else {                
                if (!exists) {        
                    JobDetail jobDetail = JobBuilder.newJob(BatchJobImpl.class)
                            .withIdentity(jobKey)
                            .storeDurably().build();

                    scheduler.addJob(jobDetail, true);
                }

                TriggerKey triggerKey = TriggerKey.triggerKey(batch.getFullName());
                scheduler.unscheduleJob(triggerKey);
               
                if (batch.isEnabled() && batch.getFrequency() != null
                        && !batch.getElements().isEmpty()
                        && (batch.getConfiguration() == null || batch.getConfiguration().isValidated())) {
                    Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity(triggerKey)
                            .forJob(jobKey)
                            .withSchedule(CronScheduleBuilder.cronSchedule(batch.getFrequency())).build();
                    
                    scheduler.scheduleJob(trigger);            
                }
                
                LOGGER.log(Level.INFO, "Successfully (re)scheduled batch " + batch.getName());
            }
            
        } catch (SchedulerException e) {
           
        }
    }
    
    @Override
    @Transactional("tmTransactionManager")
    public Batch saveAndSchedule(Batch batch) {
        batch = dao.save(batch);
        if (batch.getConfiguration() == null
                || !batch.getConfiguration().isTemplate()) {
            try {
                schedule(batch);
            } catch (SchedulerException e) {
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
                throw new IllegalArgumentException(e);
            }
        }
        return config;
    }
    
    @Override
    public void reloadFromData() {
        LOGGER.info("Reloading scheduler from data.");
        
        try {
            scheduler.clear();
        } catch (SchedulerException e) {
            LOGGER.log(Level.WARNING, "Failed to clear scheduler ", e);
            throw new IllegalStateException(e);
        }
                
        for (Batch batch : dao.getBatches(false)) {
            try {
                schedule(batch);
            } catch (SchedulerException e) {
                LOGGER.log(Level.WARNING, "Failed to schedule batch " + batch.getName() + ", disabling. ", e);
                batch.setEnabled(false);
                dao.save(batch); 
            }
            
            for (BatchRun br : dao.getCurrentBatchRuns(batch)) {
                LOGGER.log(Level.WARNING, "Automatically closing inactive batch run at start-up: " + batch.getFullName());
                dataUtil.closeBatchRun(br, "closed at start-up");
            }
        }
    }
    
    public void onApplicationEvent(ContextRefreshedEvent event) {
        reloadFromData();        
        try {
            scheduler.start();
        } catch (SchedulerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    
    @Override
    public void scheduleNow(Batch batch) {
        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(batch.getFullName())
                .startNow()        
                .build();
        try {
            scheduler.scheduleJob(trigger);
        } catch (SchedulerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
        
    @Override
    public void interrupt(BatchRun batchRun) {
        if (batchRun.getSchedulerReference() != null) {
            TriggerState state;
            try {
                state = scheduler.getTriggerState(TriggerKey.triggerKey(batchRun.getSchedulerReference()));
                if (state == TriggerState.COMPLETE || state == TriggerState.ERROR
                        || state == TriggerState.NONE) {
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
