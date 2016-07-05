/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.listener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;

/**
 * Implements a Spring Batch {@link JobExecutionListener}.
 * 
 * It's used to perform operations accordingly to the {@link Backup} batch {@link JobExecution} status.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class BackupJobExecutionListener implements JobExecutionListener {

    /**
     * logger
     */
    private static final Logger LOGGER = Logging.getLogger(BackupJobExecutionListener.class);

    public static final LockType lockType = LockType.WRITE;

    private Backup backupFacade;

    private BackupExecutionAdapter backupExecution;

    GeoServerConfigurationLock locker;

    public BackupJobExecutionListener(Backup backupFacade, GeoServerConfigurationLock locker) {
        this.backupFacade = backupFacade;
        this.locker = locker;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Acquire GeoServer Configuration Lock in READ mode
        locker.lock(lockType);

        if (backupFacade.getBackupExecutions().get(jobExecution.getId()) != null) {
            this.backupExecution = backupFacade.getBackupExecutions().get(jobExecution.getId());
        } else {
            Long id = null;
            BackupExecutionAdapter bkp = null;

            for (Entry<Long, BackupExecutionAdapter> entry : backupFacade.getBackupExecutions()
                    .entrySet()) {
                id = entry.getKey();
                bkp = entry.getValue();

                if (bkp.getJobParameters().getLong(Backup.PARAM_TIME)
                        .equals(jobExecution.getJobParameters().getLong(Backup.PARAM_TIME))) {
                    break;
                } else {
                    id = null;
                    bkp = null;
                }
            }

            if (bkp != null) {
                Resource archiveFile = bkp.getArchiveFile();
                boolean overwrite = bkp.isOverwrite();
                List<String> options = bkp.getOptions();
                Filter filter = bkp.getFilter();

                this.backupFacade.getBackupExecutions().remove(id);

                this.backupExecution = new BackupExecutionAdapter(jobExecution,
                        backupFacade.getTotalNumberOfBackupSteps());
                this.backupExecution.setArchiveFile(archiveFile);
                this.backupExecution.setOverwrite(overwrite);
                this.backupExecution.setFilter(filter);
                this.backupExecution.getOptions().addAll(options);

                this.backupFacade.getBackupExecutions().put(jobExecution.getId(),
                        this.backupExecution);
            }
        }
    }

    @SuppressWarnings("unused")
    @Override
    public void afterJob(JobExecution jobExecution) {
        boolean dryRun = Boolean.parseBoolean(
                jobExecution.getJobParameters().getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        boolean bestEffort = Boolean.parseBoolean(
                jobExecution.getJobParameters().getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));

        try {
            final Long executionId = jobExecution.getId();

            LOGGER.fine("Running Executions IDs : " + executionId);

            if (jobExecution.getStatus() != BatchStatus.STOPPED) {
                LOGGER.fine("Executions Step Summaries : "
                        + backupFacade.getJobOperator().getStepExecutionSummaries(executionId));
                LOGGER.fine("Executions Parameters : "
                        + backupFacade.getJobOperator().getParameters(executionId));
                LOGGER.fine("Executions Summary : "
                        + backupFacade.getJobOperator().getSummary(executionId));

                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    JobParameters jobParameters = backupExecution.getJobParameters();
                    Resource sourceFolder = Resources
                            .fromURL(jobParameters.getString(Backup.PARAM_OUTPUT_FILE_PATH));
                    BackupUtils.compressTo(sourceFolder, backupExecution.getArchiveFile());
                }
            }

            // Collect errors
        } catch (NoSuchJobExecutionException | IOException e) {
            if (!bestEffort) {
                this.backupExecution.addFailureExceptions(Arrays.asList(e));
                throw new RuntimeException(e);
            } else {
                this.backupExecution.addWarningExceptions(Arrays.asList(e));
            }
        } finally {
            // Release locks on GeoServer Configuration
            locker.unlock(lockType);
        }
    }
}
