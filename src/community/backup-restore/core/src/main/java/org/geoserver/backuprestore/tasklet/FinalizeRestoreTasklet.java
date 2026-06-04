package org.geoserver.backuprestore.tasklet;

import java.util.logging.Level;
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/** Spring batch tasklet responsible for performing final restore steps. In particular, reloaded the catalog */
public class FinalizeRestoreTasklet extends AbstractCatalogBackupRestoreTasklet {

    private boolean dryRun;

    public FinalizeRestoreTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {
        // Reload GeoServer Catalog
        if (jobExecution.getStatus() != BatchStatus.STOPPED) {

            GeoServer geoserver = backupFacade.getGeoServer();
            Catalog catalog = geoserver.getCatalog();

            if (!dryRun) {
                try {
                    // TODO: add option 'cleanUpGeoServerDataDir'
                    // TODO: purge/preserve GEOSERVER_DATA_DIR
                    // The restore wrote through to the data dir as it ran: the restore catalog shares the live
                    // ResourceLoader and copies the live catalog listeners (incl. GeoServerConfigPersister), so the
                    // restored state is already persisted on disk. Dispose the in-memory state; the reload() below
                    // re-reads it from disk. (A prior in-memory geoserver.reload(getCatalog()) here was redundant: it
                    // was immediately superseded by that disk re-read.)
                    catalog.getResourcePool().dispose();
                    catalog.dispose();
                    geoserver.dispose();
                } catch (Exception e) {
                    LOGGER.log(
                            Level.WARNING, "Error occurred while disposing the GeoServer Catalog before reload: ", e);
                }
            }

            geoserver.reload();
        }

        return RepeatStatus.FINISHED;
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        dryRun = Boolean.parseBoolean(stepExecution.getJobParameters().getString(Backup.PARAM_DRY_RUN_MODE, "false"));
    }
}
