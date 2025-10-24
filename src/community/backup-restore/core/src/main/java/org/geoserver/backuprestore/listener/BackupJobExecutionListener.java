/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.listener;

import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;

/**
 * Spring Batch listener for the {@code backupJob}.
 *
 * <p>Resolves the matching {@link BackupExecutionAdapter} and performs post-processing (logging and temporary resource
 * cleanup) when the job ends.
 */
public class BackupJobExecutionListener implements JobExecutionListener {

    private static final Logger LOGGER = Logging.getLogger(BackupJobExecutionListener.class);

    private final Backup backupFacade;
    private BackupExecutionAdapter backupExecution; // resolved in beforeJob

    public BackupJobExecutionListener(Backup backupFacade) {
        this.backupFacade = backupFacade;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // 1) Try direct lookup by execution id
        BackupExecutionAdapter adapter = backupFacade.getBackupExecutions().get(jobExecution.getId());
        if (adapter == null) {
            // 2) Fallback: match on PARAM_TIME (added before launch)
            Long matchedKey = null;
            for (Entry<Long, BackupExecutionAdapter> e :
                    backupFacade.getBackupExecutions().entrySet()) {
                BackupExecutionAdapter candidate = e.getValue();
                Long a = candidate.getJobParameters().getLong(Backup.PARAM_TIME);
                Long b = jobExecution.getJobParameters().getLong(Backup.PARAM_TIME);
                if (a != null && a.equals(b)) {
                    matchedKey = e.getKey();
                    adapter = candidate;
                    break;
                }
            }
            if (adapter != null) {
                // Replace placeholder adapter with the real one tied to this JobExecution
                Resource archiveFile = adapter.getArchiveFile();
                boolean overwrite = adapter.isOverwrite();
                var options = List.copyOf(adapter.getOptions());

                backupFacade.getBackupExecutions().remove(matchedKey);

                BackupExecutionAdapter resolved =
                        new BackupExecutionAdapter(jobExecution, backupFacade.getTotalNumberOfBackupSteps());
                resolved.setArchiveFile(archiveFile);
                resolved.setOverwrite(overwrite);
                resolved.setWsFilter(adapter.getWsFilter());
                resolved.setSiFilter(adapter.getSiFilter());
                resolved.setLiFilter(adapter.getLiFilter());
                resolved.getOptions().addAll(options);

                backupFacade.getBackupExecutions().put(jobExecution.getId(), resolved);
                adapter = resolved;
            }
        }
        this.backupExecution = adapter;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // If we somehow couldn’t resolve the adapter, just log and exit safely.
        if (backupExecution == null) {
            LOGGER.warning("BackupJobExecutionListener.afterJob: no adapter found for executionId="
                    + jobExecution.getId() + ", status=" + jobExecution.getStatus());
            return;
        }

        final JobParameters jp = backupExecution.getJobParameters();
        final boolean dryRun = Boolean.parseBoolean(jp.getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        final boolean bestEffort = Boolean.parseBoolean(jp.getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));

        try {
            final long executionId = jobExecution.getId();

            if (jobExecution.getStatus() != BatchStatus.STOPPED) {
                // Operator calls for diagnostics (in SB 5.2 these don’t declare checked exceptions)
                var op = backupFacade.getJobOperator();
                if (op != null) {
                    try {
                        LOGGER.fine(() -> {
                            try {
                                return "Step summaries: " + op.getStepExecutionSummaries(executionId);
                            } catch (NoSuchJobExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        LOGGER.fine(() -> {
                            try {
                                return "Parameters: " + op.getParameters(executionId);
                            } catch (NoSuchJobExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        LOGGER.fine(() -> {
                            try {
                                return "Summary: " + op.getSummary(executionId);
                            } catch (NoSuchJobExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } catch (Exception e) {
                        // These can throw at runtime if the execution is no longer visible
                        if (!bestEffort) {
                            backupExecution.addFailureExceptions(List.of(e));
                            throw e;
                        } else {
                            backupExecution.addWarningExceptions(List.of(e));
                        }
                    }
                }

                // On successful completion, optionally cleanup the temp staging directory
                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    String outUrl = jp.getString(Backup.PARAM_OUTPUT_FILE_PATH);
                    Resource sourceFolder = Resources.fromURL(outUrl);

                    boolean cleanup = Boolean.parseBoolean(jp.getString(Backup.PARAM_CLEANUP_TEMP, "false"));
                    if (cleanup && sourceFolder != null && Resources.exists(sourceFolder)) {
                        try {
                            if (!sourceFolder.delete()) {
                                LOGGER.warning("Could not delete temp resources under '" + sourceFolder.path()
                                        + "'. Please verify they were removed.");
                            }
                        } catch (Exception e) {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Failed to cleanup temporary resources under '" + sourceFolder.path() + "'.",
                                    e);
                        }
                    }
                }
            }
        } catch (Exception e) { // SB 5.2: no checked NoSuchJobExecutionException to catch specifically
            if (!bestEffort) {
                backupExecution.addFailureExceptions(List.of(e));
                // Re-throw to surface failure if not in best-effort mode
                throw new RuntimeException(e);
            } else {
                backupExecution.addWarningExceptions(List.of(e));
            }
        }
    }
}
