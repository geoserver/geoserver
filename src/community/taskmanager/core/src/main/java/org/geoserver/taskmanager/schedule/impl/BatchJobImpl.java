/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule.impl;

import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.report.Report;
import org.geoserver.taskmanager.report.ReportService;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geotools.util.logging.Logging;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;

/**
 * The scheduled batch job implementation.
 *
 * @author Niels Charlier
 */
@DisallowConcurrentExecution
public class BatchJobImpl implements Job {

    private static final Logger LOGGER = Logging.getLogger(BatchJobImpl.class);

    private static final int CONCURRENCY_DELAY = 1000;

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        // get all the context beans
        ApplicationContext appContext;
        try {
            appContext =
                    (ApplicationContext)
                            context.getScheduler().getContext().get("applicationContext");
        } catch (SchedulerException e) {
            throw new JobExecutionException(e);
        }
        TaskManagerBeans beans = appContext.getBean(TaskManagerBeans.class);

        // get the batch
        Integer batchId;
        try {
            batchId = Integer.parseInt((String) context.getJobDetail().getKey().getName());
        } catch (NumberFormatException e) {
            throw new JobExecutionException(e);
        }
        Batch batch = beans.getDao().init(beans.getDao().getBatch(batchId));

        LOGGER.log(Level.INFO, "Starting batch " + batch.getFullName());

        // start new batch run
        BatchRun batchRun = beans.getFac().createBatchRun();
        batchRun.setBatch(batch);
        batchRun.setSchedulerReference(context.getTrigger().getKey().getName());
        batchRun = beans.getDao().save(batchRun);

        try {
            // get batch elements
            List<? extends BatchElement> elements = batch.getElements();

            // stacks for processing
            Stack<TaskResult> resultStack = new Stack<TaskResult>();
            Stack<Run> runStack = new Stack<Run>();

            boolean rollback = false;

            BatchContext bContext = beans.getTaskUtil().createContext(batchRun);

            for (int i = 0; i < elements.size(); i++) {
                rollback = false;

                BatchElement element = elements.get(i);

                // if this task is currently running, wait
                Run run = null;
                while ((run = beans.getDataUtil().runIfPossible(element, batchRun)) == null) {
                    try {
                        Thread.sleep(CONCURRENCY_DELAY);
                    } catch (InterruptedException e) {
                    }
                }

                // OK, let's go
                TaskContext ctx = beans.getTaskUtil().createContext(element.getTask(), bContext);

                TaskType type = beans.getTaskTypes().get(element.getTask().getType());

                try {
                    resultStack.push(type.run(ctx));

                    run.setStatus(Run.Status.READY_TO_COMMIT);
                    run.setEnd(new Date());
                    run = beans.getDao().save(run);
                    runStack.push(run);
                } catch (Exception e) {
                    LOGGER.log(
                            Level.SEVERE,
                            "Task "
                                    + element.getTask().getFullName()
                                    + " failed in batch "
                                    + batch.getFullName()
                                    + ", rolling back.",
                            e);
                    run.setMessage(e.getMessage());
                    run.setEnd(new Date());
                    run.setStatus(Run.Status.FAILED);
                    run = beans.getDao().save(run);
                    rollback = true;
                }

                // make sure we are working with the 'good' batchRun
                batchRun = beans.getDao().reload(batchRun);
                if (batchRun.isInterruptMe()) {
                    LOGGER.log(
                            Level.INFO,
                            "Batch  " + batch.getFullName() + " manually cancelled, rolling back.");
                    rollback = true;
                }

                if (rollback) {
                    while (!resultStack.isEmpty()) {
                        Run runPop = runStack.pop();
                        runPop.setStatus(Run.Status.ROLLING_BACK);
                        runPop = beans.getDao().save(runPop);
                        try {
                            resultStack.pop().rollback();
                            runPop.setStatus(Run.Status.ROLLED_BACK);
                        } catch (Exception e) {
                            Task popTask = runPop.getBatchElement().getTask();
                            runPop.setMessage(e.getMessage());
                            runPop.setStatus(Run.Status.NOT_ROLLED_BACK);
                            LOGGER.log(
                                    Level.SEVERE,
                                    "Task "
                                            + popTask.getFullName()
                                            + " failed to rollback in batch "
                                            + batch.getFullName()
                                            + "",
                                    e);
                        }
                        runPop = beans.getDao().save(runPop);
                    }
                    break; // leave for-loop
                }
            }

            if (!rollback) {
                LOGGER.log(Level.INFO, "Committing batch " + batch.getFullName());
            }

            while (!runStack.isEmpty()) {
                Run runPop = runStack.pop();
                Run runTemp;
                // to avoid concurrent commit, if this task is currently still waiting for a commit,
                // wait
                while ((runTemp = beans.getDataUtil().startCommitIfPossible(runPop)) == null) {
                    try {
                        Thread.sleep(CONCURRENCY_DELAY);
                    } catch (InterruptedException e) {
                    }
                }
                runPop = runTemp;
                try {
                    resultStack.pop().commit();
                    runPop.setStatus(Run.Status.COMMITTED);
                } catch (Exception e) {
                    Task task = runPop.getBatchElement().getTask();
                    LOGGER.log(
                            Level.SEVERE,
                            "Task "
                                    + task.getFullName()
                                    + " failed to commit in batch "
                                    + batch.getFullName(),
                            e);
                    runPop.setMessage(e.getMessage());
                    runPop.setStatus(Run.Status.NOT_COMMITTED);
                }
                runPop = beans.getDao().save(runPop);
            }

            LOGGER.log(Level.INFO, "Finished batch " + batch.getFullName());
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Batch " + batch.getFullName() + " was aborted due to an external exception",
                    e);
            batchRun = beans.getDataUtil().closeBatchRun(batchRun, e.getMessage());
        }

        // send the report
        Report report =
                beans.getReportBuilder().buildBatchRunReport(beans.getDao().reload(batchRun));
        for (ReportService reportService : beans.getReportServices()) {
            if (reportService.getFilter().matches(report.getType())) {
                reportService.sendReport(report);
            }
        }
    }
}
