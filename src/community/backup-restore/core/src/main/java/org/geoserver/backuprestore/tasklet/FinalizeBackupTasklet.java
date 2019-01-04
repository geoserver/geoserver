package org.geoserver.backuprestore.tasklet;

import java.io.IOException;
import java.util.Arrays;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/** Final step of the backup: creating the final zip file */
public class FinalizeBackupTasklet extends AbstractCatalogBackupRestoreTasklet {

    public FinalizeBackupTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {}

    @Override
    RepeatStatus doExecute(
            StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {

        BackupExecutionAdapter backupExecution =
                backupFacade.getBackupExecutions().get(jobExecution.getId());
        boolean bestEffort =
                Boolean.parseBoolean(
                        jobExecution
                                .getJobParameters()
                                .getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));

        final Long executionId = jobExecution.getId();

        LOGGER.fine("Running Executions IDs : " + executionId);

        if (jobExecution.getStatus() != BatchStatus.STOPPED) {
            try {
                JobParameters jobParameters = backupExecution.getJobParameters();
                Resource sourceFolder =
                        Resources.fromURL(jobParameters.getString(Backup.PARAM_OUTPUT_FILE_PATH));

                dumpBackupIndex(sourceFolder);

                BackupUtils.compressTo(sourceFolder, backupExecution.getArchiveFile());
            } catch (IOException e) {
                LOGGER.severe("Backup failed while creating final ");
                if (!bestEffort) {
                    backupExecution.addFailureExceptions(Arrays.asList(e));
                    throw new RuntimeException(e);
                } else {
                    backupExecution.addWarningExceptions(Arrays.asList(e));
                }
            }
        }

        return RepeatStatus.FINISHED;
    }
}
