package org.geoserver.backuprestore.executor;

import java.io.IOException;
import java.util.Map;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
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

public class RestoreAsyncExecutor {

    private final Backup backup;

    public RestoreAsyncExecutor(Backup backup) {
        this.backup = backup;
    }

    public RestoreExecutionAdapter executeAsyncRestore(
            Resource archiveFile,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            JobParametersBuilder paramsBuilder)
            throws IOException {

        // Check whether the user is authenticated or not and, in the second case, if it is an
        // Administrator or not
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = backup.getSecurityManager().checkAuthenticationForAdminRole(auth);

        if (!isAdmin) {
            throw new IllegalStateException("Not enough privileges to run a Restore process!");
        }

        Resource tmpDir = BackupUtils.geoServerTmpDir(backup.getGeoServerDataDirectory());
        BackupUtils.extractTo(archiveFile, tmpDir);
        RestoreExecutionAdapter restoreExecution;

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
                .addString(backup.PARAM_JOB_NAME, backup.RESTORE_JOB_NAME)
                .addString(
                        backup.PARAM_INPUT_FILE_PATH,
                        BackupUtils.getArchiveURLProtocol(tmpDir) + tmpDir.path())
                .addLong(backup.PARAM_TIME, System.currentTimeMillis());

        JobParameters jobParameters = paramsBuilder.toJobParameters();

        try {
            if (backup.getRestoreRunningExecutions().isEmpty()) {
                synchronized (backup.getJobOperator()) {
                    // Start a new Job
                    JobExecution jobExecution =
                            backup.getJobLauncher().run(backup.getRestoreJob(), jobParameters);
                    restoreExecution =
                            new RestoreExecutionAdapter(
                                    jobExecution, backup.getTotalNumberOfRestoreSteps());
                    backup.getRestoreExecutions().put(restoreExecution.getId(), restoreExecution);
                    restoreExecution.setArchiveFile(archiveFile);
                    restoreExecution.setWsFilter(wsFilter);
                    restoreExecution.setSiFilter(siFilter);
                    restoreExecution.setLiFilter(liFilter);

                    for (Map.Entry jobParam : jobParameters.toProperties().entrySet()) {
                        if (!backup.PARAM_OUTPUT_FILE_PATH.equals(jobParam.getKey())
                                && !backup.PARAM_INPUT_FILE_PATH.equals(jobParam.getKey())
                                && !backup.PARAM_TIME.equals(jobParam.getKey())) {
                            restoreExecution
                                    .getOptions()
                                    .add(jobParam.getKey() + "=" + jobParam.getValue());
                        }
                    }

                    return restoreExecution;
                }
            } else {
                throw new IOException(
                        "Could not start a new Restore Job Execution since there are currently Running jobs.");
            }
        } catch (JobExecutionAlreadyRunningException
                | JobRestartException
                | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException e) {
            throw new IOException("Could not start a new Restore Job Execution: ", e);
        }
    }
}
