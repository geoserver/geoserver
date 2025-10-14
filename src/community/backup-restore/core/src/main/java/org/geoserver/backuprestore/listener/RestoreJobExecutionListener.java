/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.listener;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;

/**
 * Implements a Spring Batch {@link JobExecutionListener}.
 *
 * <p>It's used to perform operations accordingly to the {@link Backup} batch {@link JobExecution} status.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class RestoreJobExecutionListener implements JobExecutionListener {

    static Logger LOGGER = Logging.getLogger(RestoreJobExecutionListener.class);

    private Backup backupFacade;

    private RestoreExecutionAdapter restoreExecution;

    public RestoreJobExecutionListener(Backup backupFacade) {
        this.backupFacade = backupFacade;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Prior starting the JobExecution, lets store a new empty GeoServer Catalog into the
        // context.
        // It will be used to load the resources on a temporary in-memory configuration, which will
        // be
        // swapped at the end of the Restore if everything goes well.
        if (backupFacade.getRestoreExecutions().get(jobExecution.getId()) != null) {
            this.restoreExecution = backupFacade.getRestoreExecutions().get(jobExecution.getId());
            this.restoreExecution.setRestoreCatalog(createRestoreCatalog(jobExecution.getJobParameters()));
        } else {
            Long id = null;
            RestoreExecutionAdapter rst = null;

            for (Entry<Long, RestoreExecutionAdapter> entry :
                    backupFacade.getRestoreExecutions().entrySet()) {
                id = entry.getKey();
                rst = entry.getValue();

                if (rst.getJobParameters()
                        .getLong(Backup.PARAM_TIME)
                        .equals(jobExecution.getJobParameters().getLong(Backup.PARAM_TIME))) {
                    break;
                } else {
                    id = null;
                    rst = null;
                }
            }

            if (rst != null) {
                Resource archiveFile = rst.getArchiveFile();
                Catalog restoreCatalog = rst.getRestoreCatalog();
                List<String> options = rst.getOptions();

                this.backupFacade.getRestoreExecutions().remove(id);

                this.restoreExecution =
                        new RestoreExecutionAdapter(jobExecution, backupFacade.getTotalNumberOfRestoreSteps());
                this.restoreExecution.setArchiveFile(archiveFile);
                this.restoreExecution.setRestoreCatalog(restoreCatalog);
                this.restoreExecution.setWsFilter(rst.getWsFilter());
                this.restoreExecution.setSiFilter(rst.getSiFilter());
                this.restoreExecution.setLiFilter(rst.getLiFilter());

                this.restoreExecution.getOptions().addAll(options);

                this.backupFacade.getRestoreExecutions().put(jobExecution.getId(), this.restoreExecution);
            }
        }
    }

    private synchronized Catalog createRestoreCatalog(JobParameters params) {
        boolean purge = getPurgeResources(params);
        CatalogImpl restoreCatalog = new CatalogImpl();
        Catalog gsCatalog = unwrapGsCatalog();

        restoreCatalog.setResourceLoader(gsCatalog.getResourceLoader());
        restoreCatalog.setResourcePool(gsCatalog.getResourcePool());
        // only synchronize catalogs if purge flag is not set to true
        if (!purge) {
            syncCatalogs(restoreCatalog, gsCatalog);
        }

        for (CatalogListener listener : gsCatalog.getListeners()) {
            restoreCatalog.addListener(listener);
        }

        return restoreCatalog;
    }

    private Catalog unwrapGsCatalog() {
        Catalog gsCatalog = backupFacade.getGeoServer().getCatalog();
        if (gsCatalog instanceof Wrapper wrapper) {
            gsCatalog = wrapper.unwrap(Catalog.class);
        }
        return gsCatalog;
    }

    private boolean getPurgeResources(JobParameters params) {
        String value = params.getString(Backup.PARAM_PURGE_RESOURCES);
        if (value == null) return false;
        return Boolean.valueOf(value.trim());
    }

    /** Synchronizes catalogs content. */
    private void syncCatalogs(CatalogImpl restoreCatalog, Catalog gsCatalog) {
        LOGGER.fine("Synchronizing catalogs items.");
        if (gsCatalog instanceof CatalogImpl impl) {
            restoreCatalog.sync(impl);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        boolean dryRun =
                Boolean.parseBoolean(jobExecution.getJobParameters().getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        boolean bestEffort =
                Boolean.parseBoolean(jobExecution.getJobParameters().getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));
        try {
            final Long executionId = jobExecution.getId();

            this.restoreExecution = backupFacade.getRestoreExecutions().get(jobExecution.getId());

            LOGGER.fine("Running Executions IDs : " + executionId);

            if (jobExecution.getStatus() != BatchStatus.STOPPED) {
                LOGGER.fine("Executions Step Summaries : "
                        + backupFacade.getJobOperator().getStepExecutionSummaries(executionId));
                LOGGER.fine("Executions Parameters : "
                        + backupFacade.getJobOperator().getParameters(executionId));
                LOGGER.fine(
                        "Executions Summary : " + backupFacade.getJobOperator().getSummary(executionId));

                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    cleanUp();
                }
            }
            // Collect errors
        } catch (Exception e) {
            if (!bestEffort) {
                this.restoreExecution.addFailureExceptions(Arrays.asList(e));
                throw new RuntimeException(e);
            } else {
                this.restoreExecution.addWarningExceptions(Arrays.asList(e));
            }
        }
    }

    /** Clean up temp folder if required on settings. */
    private void cleanUp() {
        JobParameters jobParameters = restoreExecution.getJobParameters();
        Resource tempFolder = Resources.fromURL(jobParameters.getString(Backup.PARAM_INPUT_FILE_PATH));

        // Cleanup Temporary Resources
        String cleanUpTempFolders = jobParameters.getString(Backup.PARAM_CLEANUP_TEMP);
        if (cleanUpTempFolders != null && Boolean.parseBoolean(cleanUpTempFolders) && tempFolder != null) {
            if (Resources.exists(tempFolder)) {
                try {
                    if (!tempFolder.delete()) {
                        LOGGER.warning(
                                "It was not possible to cleanup Temporary Resources. Please double check that Resources inside the Temp GeoServer Data Directory have been removed.");
                    }
                } catch (Exception e) {
                    LOGGER.log(
                            Level.WARNING,
                            "It was not possible to cleanup Temporary Resources. Please double check that Resources inside the Temp GeoServer Data Directory have been removed.",
                            e);
                }
            }
        }
    }
}
