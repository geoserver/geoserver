/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.writer.backup.BackupRunner;
import org.geoserver.backuprestore.writer.restore.RestoreRunner;
import org.geoserver.catalog.*;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.ResourceStore;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Concrete implementation of the {@link AbstractCatalogBackupRestoreTasklet}. <br>
 * Actually takes care of dumping/restoring everything is not a {@link ResourceInfo}, like the
 * GeoServer settings, logging and global/local (workspaces) infos.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogBackupRestoreTasklet extends AbstractCatalogBackupRestoreTasklet {

    public static final String GWC_GEOSERVER_CONFIG_PERSISTER = "gwcGeoServervConfigPersister";

    // whether existing resources should be deleted
    private boolean purge = true;
    // whether global settings should be skipped
    private boolean skipSettings = true;
    // whether GWC should be skipped
    private boolean skipGWC = false;
    private BackupRunner backup;
    private RestoreRunner restore;

    public CatalogBackupRestoreTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    public boolean isPurge() {
        return purge;
    }

    public void setPurge(boolean purge) {
        this.purge = purge;
    }

    public boolean isSkipSettings() {
        return skipSettings;
    }

    public void setSkipSettings(boolean skipSettings) {
        this.skipSettings = skipSettings;
    }

    public boolean isSkipGWC() {
        return skipGWC;
    }

    public void setSkipGWC(boolean skipGWC) {
        this.skipGWC = skipGWC;
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        this.skipSettings =
                Boolean.parseBoolean(
                        stepExecution
                                .getJobParameters()
                                .getString(Backup.PARAM_SKIP_SETTINGS, "true"));

        this.skipGWC =
                Boolean.parseBoolean(
                        stepExecution.getJobParameters().getString(Backup.PARAM_SKIP_GWC, "false"));

        this.purge =
                Boolean.parseBoolean(
                        stepExecution
                                .getJobParameters()
                                .getString(Backup.PARAM_PURGE_RESOURCES, "false"));

        this.backup = new BackupRunner(this);
        this.restore = new RestoreRunner(this);
    }

    @Override
    RepeatStatus doExecute(
            StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {
        final GeoServer geoserver = backupFacade.getGeoServer();
        final GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();
        final ResourceStore resourceStore = dd.getResourceStore();

        try {
            if (!isNew()) {
                backup.doBackup(jobExecution, geoserver, dd, resourceStore);
            } else {
                restore.doRestore(jobExecution, geoserver, dd);
            }
        } catch (Exception e) {
            logValidationExceptions(
                    (ValidationResult) null,
                    new UnexpectedJobExecutionException(
                            "Exception occurred while storing GeoServer globals and services settings!",
                            e));
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * @param layerGroupInfo
     * @return
     */
    public WorkspaceInfo getLayerGroupWorkspace(LayerGroupInfo layerGroupInfo) {
        WorkspaceInfo ws =
                layerGroupInfo.getWorkspace() != null
                        ? getCatalog().getWorkspaceByName(layerGroupInfo.getWorkspace().getName())
                        : null;
        return ws;
    }

    /**
     * @param layerInfo
     * @return
     */
    public WorkspaceInfo getLayerWorkspace(LayerInfo layerInfo) {
        WorkspaceInfo ws =
                layerInfo.getResource() != null
                                && layerInfo.getResource().getStore() != null
                                && layerInfo.getResource().getStore().getWorkspace() != null
                        ? getCatalog()
                                .getWorkspaceByName(
                                        layerInfo.getResource().getStore().getWorkspace().getName())
                        : null;
        return ws;
    }
}
