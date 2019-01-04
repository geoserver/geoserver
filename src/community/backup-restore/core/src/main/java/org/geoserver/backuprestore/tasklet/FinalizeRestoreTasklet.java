package org.geoserver.backuprestore.tasklet;

import java.util.logging.Level;
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Spring batch tasklet responsible for performing final restore steps. In particular, reloaded the
 * catalog
 */
public class FinalizeRestoreTasklet extends AbstractCatalogBackupRestoreTasklet {

    private boolean dryRun;

    public FinalizeRestoreTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    RepeatStatus doExecute(
            StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {
        // Reload GeoServer Catalog
        if (jobExecution.getStatus() != BatchStatus.STOPPED) {

            GeoServer geoserver = backupFacade.getGeoServer();
            Catalog catalog = geoserver.getCatalog();

            if (!dryRun) {
                try {
                    // TODO: add option 'cleanUpGeoServerDataDir'
                    // TODO: purge/preserve GEOSERVER_DATA_DIR
                    catalog.getResourcePool().dispose();
                    catalog.dispose();
                    geoserver.dispose();
                    geoserver.reload(getCatalog());
                } catch (Exception e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Error occurred while trying to Reload the GeoServer Catalog: ",
                            e);
                }
            }

            geoserver.reload();
        }

        return RepeatStatus.FINISHED;
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        dryRun =
                Boolean.parseBoolean(
                        stepExecution
                                .getJobParameters()
                                .getString(Backup.PARAM_DRY_RUN_MODE, "false"));
    }
}
