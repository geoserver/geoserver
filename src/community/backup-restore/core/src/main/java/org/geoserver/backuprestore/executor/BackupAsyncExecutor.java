package org.geoserver.backuprestore.executor;

import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.platform.resource.Resource;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class BackupAsyncExecutor {

    private final Backup backup;

    public BackupAsyncExecutor(Backup backup) {
        this.backup = backup;
    }

    /** */
    public BackupExecutionAdapter executeAsyncBackup(
            final Resource archiveFile,
            final boolean overwrite,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            final JobParametersBuilder paramsBuilder)
            throws IOException {

        // Check whether the user is authenticated or not and, in the second case, if it is an
        // Administrator or not
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = backup.getSecurityManager().checkAuthenticationForAdminRole(auth);

        if (!isAdmin) {
            throw new IllegalStateException("Not enough privileges to run a Restore process!");
        }

        // Check if archiveFile exists
        if (archiveFile.file().exists()) {
            if (!overwrite && FileUtils.sizeOf(archiveFile.file()) > 0) {
                // Unless the user explicitly wants to overwrite the archiveFile, throw an exception
                // whenever it already exists
                throw new IOException(
                        "The target archive file already exists. Use 'overwrite=TRUE' if you want to overwrite it.");
            } else {
                FileUtils.forceDelete(archiveFile.file());
            }
        } else {
            // Make sure the parent path exists
            if (!archiveFile.file().getParentFile().exists()) {
                try {
                    archiveFile.file().getParentFile().mkdirs();
                } finally {
                    if (!archiveFile.file().getParentFile().exists()) {
                        throw new IOException("The path to target archive file is unreachable.");
                    }
                }
            }
        }

        // Initialize ZIP
        FileUtils.touch(archiveFile.file());

        // Write flat files into a temporary folder
        Resource tmpDir = BackupUtils.geoServerTmpDir(backup.getGeoServerDataDirectory());

        if (wsFilter != null) {
            paramsBuilder.addString("wsFilter", ECQL.toCQL(wsFilter));
        }
        if (siFilter != null) {
            paramsBuilder.addString("siFilter", ECQL.toCQL(siFilter));
        }
        if (liFilter != null) {
            paramsBuilder.addString("liFilter", ECQL.toCQL(liFilter));
        }

        paramsBuilder
                .addString(Backup.PARAM_JOB_NAME, Backup.BACKUP_JOB_NAME)
                .addString(
                        Backup.PARAM_OUTPUT_FILE_PATH,
                        BackupUtils.getArchiveURLProtocol(tmpDir) + tmpDir.path())
                .addLong(Backup.PARAM_TIME, System.currentTimeMillis());

        //        parseParams(params, paramsBuilder);

        JobParameters jobParameters = paramsBuilder.toJobParameters();

        // Send Execution Signal
        BackupExecutionAdapter backupExecution;
        try {
            if (backup.getBackupRunningExecutions().isEmpty()) {
                synchronized (backup.getJobOperator()) {
                    // Start a new Job
                    JobExecution jobExecution =
                            backup.getJobLauncher().run(backup.getBackupJob(), jobParameters);
                    backupExecution =
                            new BackupExecutionAdapter(
                                    jobExecution, backup.getTotalNumberOfBackupSteps());
                    backup.getBackupExecutions().put(backupExecution.getId(), backupExecution);

                    backupExecution.setArchiveFile(archiveFile);
                    backupExecution.setOverwrite(overwrite);
                    backupExecution.setWsFilter(wsFilter);
                    backupExecution.setSiFilter(siFilter);
                    backupExecution.setLiFilter(liFilter);

                    backupExecution.getOptions().add("OVERWRITE=" + overwrite);
                    for (Map.Entry jobParam : jobParameters.toProperties().entrySet()) {
                        if (!Backup.PARAM_OUTPUT_FILE_PATH.equals(jobParam.getKey())
                                && !Backup.PARAM_INPUT_FILE_PATH.equals(jobParam.getKey())
                                && !Backup.PARAM_TIME.equals(jobParam.getKey())) {
                            backupExecution
                                    .getOptions()
                                    .add(jobParam.getKey() + "=" + jobParam.getValue());
                        }
                    }

                    return backupExecution;
                }
            } else {
                throw new IOException(
                        "Could not start a new Backup Job Execution since there are currently Running jobs.");
            }
        } catch (JobExecutionAlreadyRunningException
                | JobRestartException
                | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException e) {
            throw new IOException("Could not start a new Backup Job Execution: ", e);
        } finally {
        }
    }
}
