package org.geoserver.backuprestore.tasklet;

import org.geoserver.backuprestore.Backup;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Spring batch tasklet responsible for performing final restore steps. In particular, reloaded the catalog
 */
public class FinalizeRestoreTasklet extends AbstractCatalogBackupRestoreTasklet {

    private boolean dryRun;

    public FinalizeRestoreTasklet(Backup backupFacade, XStreamPersisterFactory xStreamPersisterFactory) {
        super(backupFacade, xStreamPersisterFactory);
    }

    @Override
    RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
        throws Exception {
        // Reload GeoServer Catalog
        if (jobExecution.getStatus() != BatchStatus.STOPPED) {
            if (!dryRun) {
                backupFacade.getGeoServer().reload();
            }

            backupFacade.getGeoServer().reset();
        }


        return RepeatStatus.FINISHED;
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        dryRun = Boolean.parseBoolean(
            stepExecution.getJobParameters().getString(Backup.PARAM_DRY_RUN_MODE, "false"));
    }
}
