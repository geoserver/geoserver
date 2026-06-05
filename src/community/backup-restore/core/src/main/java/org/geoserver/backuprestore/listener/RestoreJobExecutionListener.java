/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.listener;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListener;

/**
 * Implements a Spring Batch {@link JobExecutionListener}.
 *
 * <p>It's used to perform operations accordingly to the {@link Backup} batch {@link JobExecution} status.
 *
 * <p><b>Transactional restore (snapshot / rollback).</b> A restore commits to the <em>live</em> data directory
 * incrementally as the chunk and tasklet steps run: the catalog is persisted by the {@code GeoServerConfigPersister}
 * carried on the restore catalog, GWC by {@code CatalogBackupRestoreTasklet} and security by
 * {@code CatalogSecurityManagerTasklet}. Therefore the pre-flight
 * {@link org.geoserver.backuprestore.tasklet.ValidateRestoreTasklet} abort and the Dry-Run mode, by themselves, only
 * skip the final in-memory reload — they do not undo what was already written to disk. To make those two opt-in modes
 * <em>trustworthy</em> (a Dry-Run must not mutate the data directory, and a {@code BK_FAIL_ON_INVALID} abort must leave
 * it untouched) this listener snapshots the affected data-directory subtrees in {@link #beforeJob} and, in
 * {@link #afterJob}, either rolls the live tree back from the snapshot (Dry-Run, or any non-COMPLETED outcome) or
 * discards the snapshot (a real restore that completed). Both modes are off by default, so ordinary restores keep the
 * historical incremental-commit behaviour and pay no snapshot cost.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class RestoreJobExecutionListener implements JobExecutionListener {

    static Logger LOGGER = Logging.getLogger(RestoreJobExecutionListener.class);

    /**
     * Data-directory subtrees the restore writes into and that the snapshot/rollback covers. Catalog config lives under
     * {@code workspaces} / {@code styles} / {@code layergroups}; GWC under {@code gwc} (the {@code gwc-gs.xml} provider
     * dir) and {@code gwc-layers} (the per-layer tile-layer catalog); security under {@code security}.
     */
    private static final String[] SNAPSHOT_SUBTREES = {
        "workspaces", "styles", "layergroups", "gwc", "gwc-layers", "security"
    };

    private Backup backupFacade;

    private RestoreExecutionAdapter restoreExecution;

    /**
     * Location of the data-directory snapshot taken in {@link #beforeJob} for a transactional (Dry-Run or
     * fail-on-invalid) restore, consumed in {@link #afterJob}. {@code null} when the running restore is not
     * transactional. Restores are serialized (a single listener instance, one restore at a time), so a plain field is
     * sufficient.
     */
    private File snapshotDir;

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

        // Transactional restore: snapshot the live data dir up-front so afterJob can roll it back on a Dry-Run or any
        // non-COMPLETED outcome. Best-effort and strictly opt-in: a snapshot failure is logged and the restore proceeds
        // as a normal (non-transactional) one rather than aborting.
        this.snapshotDir = null;
        if (isTransactionalRestore(jobExecution.getJobParameters())) {
            try {
                this.snapshotDir = snapshotDataDirectory();
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Transactional restore requested (Dry-Run or "
                                + Backup.PARAM_FAIL_ON_INVALID
                                + ") but the pre-restore data-directory snapshot failed; the restore will run WITHOUT"
                                + " rollback protection.",
                        e);
                this.snapshotDir = null;
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
        return Backup.isPurgeResources(params);
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

            // Transactional restore: decide whether to roll the live data dir back to its pre-restore snapshot or to
            // discard the snapshot. This runs first (and independently of the STOPPED short-circuit below) so a
            // Dry-Run or a failed/aborted restore never leaves the data directory mutated.
            if (this.snapshotDir != null) {
                boolean rollback = dryRun || jobExecution.getStatus() != BatchStatus.COMPLETED;
                handleSnapshot(rollback);
            }

            if (jobExecution.getStatus() != BatchStatus.STOPPED) {
                LOGGER.fine("Executions Step Summaries : " + jobExecution.getStepExecutions());
                LOGGER.fine("Executions Parameters : " + jobExecution.getJobParameters());
                LOGGER.fine("Executions Summary : " + jobExecution);

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

    /**
     * A restore is "transactional" (snapshot + rollback) when it is a Dry-Run or when it opts into pre-flight
     * fail-on-invalid. Both modes are off by default, so ordinary restores are untouched and pay no snapshot cost.
     */
    private boolean isTransactionalRestore(JobParameters params) {
        boolean dryRun = Boolean.parseBoolean(params.getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        boolean failOnInvalid = Boolean.parseBoolean(params.getString(Backup.PARAM_FAIL_ON_INVALID, "false"));
        return dryRun || failOnInvalid;
    }

    /**
     * Snapshots the live data directory ({@link #backupFacade}'s base dir) and returns the snapshot location. Thin
     * wrapper over the testable {@link #snapshotDataDirectory(File)}.
     */
    private File snapshotDataDirectory() throws IOException {
        final File baseDir = backupFacade.getResourceLoader().getBaseDirectory();
        final File snapshot = snapshotDataDirectory(baseDir);
        LOGGER.info("Transactional restore: snapshotted the live data directory to " + snapshot.getAbsolutePath());
        return snapshot;
    }

    /**
     * Copies the data-directory subtrees and root configuration files a restore writes into ({@link #SNAPSHOT_SUBTREES}
     * plus the root {@code *.xml} files such as {@code global.xml} / {@code logging.xml}) from {@code baseDir} into a
     * fresh temp directory, returning its location. Missing subtrees are skipped (a restore may create them). Uses
     * {@link java.nio.file.Files} and {@link FileUtils} as mandated. Package-visible and static so the snapshot
     * mechanics can be unit-tested against a hand-built data directory without a running restore job.
     */
    static File snapshotDataDirectory(File baseDir) throws IOException {
        final File snapshot = Files.createTempDirectory("gs-restore-snapshot-").toFile();

        for (String subtree : SNAPSHOT_SUBTREES) {
            File src = new File(baseDir, subtree);
            if (src.isDirectory()) {
                FileUtils.copyDirectory(src, new File(snapshot, subtree));
            }
        }
        // Root global *.xml (global.xml, logging.xml, service descriptors, gwc-gs.xml, ...): they live directly in the
        // data-dir root and are rewritten by the global / GWC restore steps.
        File[] rootXml =
                baseDir.listFiles((FileFilter) f -> f.isFile() && f.getName().endsWith(".xml"));
        if (rootXml != null) {
            for (File xml : rootXml) {
                FileUtils.copyFile(xml, new File(snapshot, xml.getName()));
            }
        }
        return snapshot;
    }

    /**
     * Finalizes the transactional snapshot taken in {@link #beforeJob}: on {@code rollback}, restores every snapshotted
     * subtree / root file over the live data directory and reloads GeoServer + the security subsystem; otherwise simply
     * discards the snapshot. On a <em>successful</em> rollback (or discard) the snapshot directory is deleted; on a
     * <em>failed</em> rollback the snapshot is intentionally KEPT (and a SEVERE is logged) so an operator can recover
     * by hand. {@link #snapshotDir} is always cleared so the snapshot is finalized exactly once.
     */
    private void handleSnapshot(boolean rollback) {
        final File snapshot = this.snapshotDir;
        this.snapshotDir = null;
        if (snapshot == null) {
            return;
        }
        boolean keepSnapshot = false;
        try {
            if (rollback) {
                rollbackDataDirectory(snapshot);
            }
        } catch (Exception e) {
            keepSnapshot = true;
            LOGGER.log(
                    Level.SEVERE,
                    "Transactional restore ROLLBACK FAILED. The live data directory may be in an inconsistent state."
                            + " The pre-restore snapshot has been PRESERVED for manual recovery at: "
                            + snapshot.getAbsolutePath(),
                    e);
        } finally {
            if (!keepSnapshot) {
                try {
                    FileUtils.deleteDirectory(snapshot);
                } catch (IOException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Could not delete the transactional restore snapshot at " + snapshot.getAbsolutePath()
                                    + "; it can be removed manually.",
                            e);
                }
            }
        }
    }

    /**
     * Rolls the live data directory back from {@code snapshot} on disk and then reloads the in-memory GeoServer
     * configuration and the security manager so the rolled-back on-disk state is the one in effect. Throws on the first
     * failure so {@link #handleSnapshot} preserves the snapshot.
     */
    private void rollbackDataDirectory(File snapshot) throws Exception {
        final File baseDir = backupFacade.getResourceLoader().getBaseDirectory();

        rollbackDataDirectory(baseDir, snapshot);

        LOGGER.info("Transactional restore: rolled the live data directory back from snapshot "
                + snapshot.getAbsolutePath() + "; reloading GeoServer and the security subsystem.");

        // Re-read the rolled-back configuration into memory.
        backupFacade.getGeoServer().reload();
        GeoServerSecurityManager securityManager = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        if (securityManager != null) {
            securityManager.reload();
        }
    }

    /**
     * Replaces each snapshotted subtree / root file over {@code baseDir} (delete-then-copy). A subtree present in the
     * snapshot is restored verbatim; a subtree absent from the snapshot but present live (i.e. created by the restore)
     * is removed. Package-visible and static so the on-disk rollback mechanics can be unit-tested without the live
     * GeoServer / security singletons that the in-memory reload needs.
     */
    static void rollbackDataDirectory(File baseDir, File snapshot) throws IOException {
        for (String subtree : SNAPSHOT_SUBTREES) {
            File live = new File(baseDir, subtree);
            File saved = new File(snapshot, subtree);
            if (saved.isDirectory()) {
                // restore the subtree exactly as it was
                if (live.exists()) {
                    FileUtils.deleteDirectory(live);
                }
                FileUtils.copyDirectory(saved, live);
            } else if (live.isDirectory()) {
                // the subtree did not exist before the restore (the restore created it): remove it
                FileUtils.deleteDirectory(live);
            }
        }
        // Root global *.xml: overwrite each saved file back over the live one. (We do not delete extra root xml files
        // the restore may have introduced; the catalog/global reload re-reads the authoritative ones.)
        File[] savedRootXml =
                snapshot.listFiles((FileFilter) f -> f.isFile() && f.getName().endsWith(".xml"));
        if (savedRootXml != null) {
            for (File xml : savedRootXml) {
                FileUtils.copyFile(xml, new File(baseDir, xml.getName()));
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
