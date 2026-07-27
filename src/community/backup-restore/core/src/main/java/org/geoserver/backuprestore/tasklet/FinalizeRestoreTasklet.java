package org.geoserver.backuprestore.tasklet;

import org.geoserver.backuprestore.Backup;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * Spring Batch tasklet for the final restore step.
 *
 * <p>The catalog dispose and {@code GeoServer.reload()} that used to run here were moved to
 * {@link Backup#afterJob(org.springframework.batch.core.job.JobExecution)}. Tasklets execute on a separate executor
 * thread while the restore job holds the GeoServer configuration write lock on the job thread; reloading here runs the
 * parallel data-directory loader, whose worker threads acquire that same lock and deadlock against it (~10 minutes
 * until the lock times out, leaving the job {@code STOPPED}). {@code afterJob} runs on the job thread after the lock is
 * released, so the reload performed there is lock-free.
 */
public class FinalizeRestoreTasklet extends AbstractCatalogBackupRestoreTasklet {

    public FinalizeRestoreTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {
        return RepeatStatus.FINISHED;
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        // No per-step initialization required: the catalog reload moved to Backup.afterJob.
    }
}
