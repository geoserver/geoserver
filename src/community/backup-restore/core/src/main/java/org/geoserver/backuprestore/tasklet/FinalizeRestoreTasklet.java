package org.geoserver.backuprestore.tasklet;

import java.util.logging.Level;
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/** Spring batch tasklet responsible for performing final restore steps (catalog reload). */
public class FinalizeRestoreTasklet extends AbstractCatalogBackupRestoreTasklet {

    private boolean dryRun;

    public FinalizeRestoreTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {

        // Respect a requested stop promptly
        if (jobExecution.isStopping() || jobExecution.getStatus() == BatchStatus.STOPPED) {
            StepExecution se = chunkContext.getStepContext().getStepExecution();
            se.setExitStatus(ExitStatus.STOPPED);
            se.setStatus(BatchStatus.STOPPED);
            return RepeatStatus.FINISHED;
        }

        GeoServer geoserver = backupFacade.getGeoServer();
        Catalog live = geoserver.getCatalog();

        if (!dryRun) {
            try {
                // Dispose live components and reload using the temporary restore catalog
                live.getResourcePool().dispose();
                live.dispose();
                // keep the container context intact; no hard shutdowns of the app context here
                geoserver.reload(getCatalog());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error occurred while reloading the GeoServer catalog: ", e);
            }
        } else {
            // Dry-run: light reload of services only
            geoserver.reload();
        }

        return RepeatStatus.FINISHED;
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        dryRun = Boolean.parseBoolean(stepExecution.getJobParameters().getString(Backup.PARAM_DRY_RUN_MODE, "false"));
    }
}
