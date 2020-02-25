/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.backuprestore.Backup;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/** Extension point for generic backup / restore jobs. */
public final class GenericTasklet extends AbstractCatalogBackupRestoreTasklet {

    // key used to register in the job context the handlers that need to run again
    private static final String GENERIC_CONTINUABLE_HANDLERS_KEY = "GENERIC_CONTINUABLE_HANDLERS";

    public GenericTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        // this is invoked for each run, which means that this may be invoked multiple times for
        // continuable handlers
        getAllHandlers().forEach(handler -> handler.initialize(stepExecution, this));
    }

    @Override
    public RepeatStatus doExecute(
            StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {
        // get the available generic handlers or the continuable ones
        List<GenericTaskletHandler> handlers = getHandlers(jobExecution);
        List<GenericTaskletHandler> continuable = new ArrayList<>();
        // execute each handler and store the continuable ones
        handlers.forEach(
                handler -> {
                    RepeatStatus status =
                            handler.handle(contribution, chunkContext, jobExecution, this);
                    if (status == RepeatStatus.CONTINUABLE) {
                        continuable.add(handler);
                    }
                });
        // register the continuable ones overriding the existing ones
        putContinuableHandlers(jobExecution, continuable);
        if (continuable.isEmpty()) {
            // no continuable jobs, we are done
            return RepeatStatus.FINISHED;
        }
        // there is continuable jobs
        return RepeatStatus.CONTINUABLE;
    }

    /**
     * Put the provided continuable jobs in the job execution context overriding any existing ones.
     */
    private void putContinuableHandlers(
            JobExecution jobExecution, List<GenericTaskletHandler> handlers) {
        jobExecution.getExecutionContext().put(GENERIC_CONTINUABLE_HANDLERS_KEY, handlers);
    }

    /**
     * Helper method that return the handlers that should be executed. If there is any pending
     * continuable handler we only run the pending continuable handlers otherwise we run all the
     * available handlers.
     */
    @SuppressWarnings("unchecked")
    private List<GenericTaskletHandler> getHandlers(JobExecution jobExecution) {
        // let's see if we have any pending continuable jobs
        Object value = jobExecution.getExecutionContext().get(GENERIC_CONTINUABLE_HANDLERS_KEY);
        if (value == null || !List.class.isAssignableFrom(value.getClass())) {
            // no pending continuable handlers, use the normal handlers
            return getAllHandlers();
        }
        List values = (List) value;
        if (values.isEmpty()
                || !GenericTaskletHandler.class.isAssignableFrom(values.get(0).getClass())) {
            // not what we expect, use the normal handlers
            return getAllHandlers();
        }
        // pending continuable handlers
        return (List<GenericTaskletHandler>) values;
    }

    /**
     * Helper method that just retrieves all the available generic handlers contributed by
     * extensions.
     */
    private List<GenericTaskletHandler> getAllHandlers() {
        return GeoServerExtensions.extensions(GenericTaskletHandler.class);
    }
}
