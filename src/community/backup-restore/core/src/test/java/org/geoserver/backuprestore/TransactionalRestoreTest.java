/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.geoserver.backuprestore.listener.RestoreJobExecutionListener;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

/**
 * End-to-end checks for the transactional restore (snapshot / rollback) wired into {@link RestoreJobExecutionListener}.
 *
 * <p>A restore commits to the live data directory incrementally: in particular the catalog {@code CatalogItemWriter}
 * persists every restored object to disk through the {@code GeoServerConfigPersister} the restore catalog carries, with
 * <em>no</em> {@code isDryRun()} gate on that write path. The snapshot taken in {@code beforeJob} and the rollback in
 * {@code afterJob} are what make a Dry-Run (and a {@code BK_FAIL_ON_INVALID} abort) leave the data directory as it was.
 *
 * <p>These tests drive the listener's {@code beforeJob}/{@code afterJob} seam directly against the live GeoServer data
 * directory (real {@code GeoServer.reload()} / security reload), rather than through an asynchronous restore job. That
 * is deliberate: a Dry-Run restore in this in-process Windows harness does not finalize (it stays {@code STARTED} — the
 * shipped {@code RESTRestoreTest} only asserts {@code COMPLETED} <em>if</em> the job happened to finish), so
 * {@code afterJob} would never run inside a test window. Driving the seam directly exercises exactly the production
 * rollback code synchronously and deterministically. The byte-for-byte equality of the underlying snapshot/rollback
 * file mechanics is additionally covered by {@link RestoreSnapshotRollbackTest}.
 */
public class TransactionalRestoreTest extends BackupRestoreTestSupport {

    @Override
    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    /**
     * Dry-Run: the listener snapshots the data dir, then a mid-run persister write (simulated by writing a workspace
     * descriptor the restore would have added) must be undone by the rollback, while content that predated the restore
     * survives.
     */
    @Test
    public void dryRunRollsBackDataDirWrites() throws Exception {
        runRollbackScenario("tx_dryrun", dryRunParams(), BatchStatus.COMPLETED);
    }

    /**
     * Fail-on-invalid abort: a non-COMPLETED outcome must trigger the same rollback even when it is not a Dry-Run (the
     * pre-flight validation failed the job after the catalog steps already committed to disk).
     */
    @Test
    public void failOnInvalidAbortRollsBackDataDirWrites() throws Exception {
        runRollbackScenario("tx_abort", failOnInvalidParams(), BatchStatus.FAILED);
    }

    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Drives the snapshot/rollback seam: register an execution, {@code beforeJob} (snapshot), simulate the restore
     * writing a new workspace to disk, then {@code afterJob} with {@code finalStatus} and assert the write was rolled
     * back and a pre-existing workspace preserved.
     */
    private void runRollbackScenario(String prefix, JobParameters params, BatchStatus finalStatus) throws Exception {
        RestoreJobExecutionListener listener =
                (RestoreJobExecutionListener) applicationContext.getBean("restoreJobExecutionListener");

        // A bystander workspace that exists on disk before the restore and must survive the rollback.
        addThrowawayWorkspace(prefix + "_bystander");
        getGeoServer().reload();
        File workspaces = workspacesDir();
        File bystander = new File(workspaces, prefix + "_bystander/workspace.xml");
        assertTrue("precondition: the bystander workspace must be on disk before the restore", bystander.exists());

        long executionId = System.nanoTime();
        JobExecution jobExecution = newRestoreExecution(executionId, params, finalStatus);

        // beforeJob takes the snapshot (transactional mode is on for both dry-run and fail-on-invalid params).
        listener.beforeJob(jobExecution);

        // Simulate the catalog write path committing a brand-new object to disk mid-restore (what the persister does
        // with no dry-run gate). This file is NOT in the snapshot, so a correct rollback must delete it.
        File simWrite = new File(workspaces, prefix + "_sim_written/workspace.xml");
        writeFile(
                simWrite, "<workspace><id>" + prefix + "_sim</id><name>" + prefix + "_sim_written</name></workspace>");
        assertTrue("precondition: the simulated restore write must be on disk", simWrite.exists());

        // afterJob rolls back (dry-run, or non-COMPLETED status) and reloads.
        listener.afterJob(jobExecution);

        assertFalse("the rollback must remove the file the restore wrote after the snapshot", simWrite.exists());
        assertTrue("the rollback must preserve content that predated the restore", bystander.exists());
    }

    private JobExecution newRestoreExecution(long executionId, JobParameters params, BatchStatus finalStatus) {
        JobInstance jobInstance = new JobInstance(executionId, Backup.RESTORE_JOB_NAME);
        JobExecution jobExecution = new JobExecution(executionId, jobInstance, params);
        jobExecution.setStatus(finalStatus);
        if (finalStatus == BatchStatus.FAILED) {
            jobExecution.setExitStatus(ExitStatus.FAILED);
        }
        // The listener looks the execution up in the facade's running map (afterJob reads restoreExecution from it).
        RestoreExecutionAdapter adapter =
                new RestoreExecutionAdapter(jobExecution, backupFacade.getTotalNumberOfRestoreSteps());
        backupFacade.getRestoreExecutions().put(executionId, adapter);
        return jobExecution;
    }

    private JobParameters dryRunParams() {
        return new JobParametersBuilder()
                .addString(Backup.PARAM_DRY_RUN_MODE, "true")
                .addString(Backup.PARAM_BEST_EFFORT_MODE, "true")
                .addLong(Backup.PARAM_TIME, System.currentTimeMillis())
                .toJobParameters();
    }

    private JobParameters failOnInvalidParams() {
        return new JobParametersBuilder()
                .addString(Backup.PARAM_FAIL_ON_INVALID, "true")
                .addString(Backup.PARAM_BEST_EFFORT_MODE, "true")
                .addLong(Backup.PARAM_TIME, System.currentTimeMillis())
                .toJobParameters();
    }

    private File workspacesDir() {
        return new File(getTestData().getDataDirectoryRoot(), "workspaces");
    }

    private void addThrowawayWorkspace(String name) {
        WorkspaceInfo ws = catalog.getFactory().createWorkspace();
        ws.setName(name);
        catalog.add(ws);
        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix(name);
        ns.setURI("http://" + name);
        catalog.add(ns);
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName(name + "_style");
        s.setWorkspace(ws);
        s.setFilename(name + ".sld");
        catalog.add(s);
    }

    private static void writeFile(File f, String content) throws java.io.IOException {
        f.getParentFile().mkdirs();
        java.nio.file.Files.write(f.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
