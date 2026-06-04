/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.logging.Level;
import org.geoserver.backuprestore.tasklet.AbstractCatalogBackupRestoreTasklet;
import org.geoserver.platform.resource.Files;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Tests that backup/restore tasklets use a concurrency-limited executor to prevent deadlock.
 *
 * <p>Prior to the fix, the default SimpleAsyncTaskExecutor had no concurrency limit, spawning new threads for each
 * tasklet. This caused deadlock when multiple tasklets competed for the same GeoServer resource locks
 * (MemoryLockProvider/GlobalLockProvider). The fix limits concurrency to 1, ensuring tasklets run sequentially while
 * preserving async execution for timeout and cancellation support.
 */
public class SequentialBackupTest extends BackupRestoreTestSupport {

    @Override
    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testTaskExecutorHasConcurrencyLimit() throws Exception {
        // Verify that the tasklet's executor is a SimpleAsyncTaskExecutor with concurrency
        // limit of 1, which prevents deadlock from concurrent lock acquisition while still
        // preserving timeout and cancellation support (unlike SyncTaskExecutor).
        AbstractCatalogBackupRestoreTasklet<?> tasklet =
                applicationContext.getBean("catalogBackupTasklet", AbstractCatalogBackupRestoreTasklet.class);
        assertNotNull("catalogBackupTasklet bean should exist", tasklet);

        Field executorField = AbstractCatalogBackupRestoreTasklet.class.getDeclaredField("taskExecutor");
        executorField.setAccessible(true);
        TaskExecutor executor = (TaskExecutor) executorField.get(tasklet);

        assertTrue(
                "TaskExecutor should be SimpleAsyncTaskExecutor (not SyncTaskExecutor)",
                executor instanceof SimpleAsyncTaskExecutor);
        SimpleAsyncTaskExecutor asyncExecutor = (SimpleAsyncTaskExecutor) executor;
        assertEquals("Concurrency limit should be 1 to prevent deadlock", 1, asyncExecutor.getConcurrencyLimit());
    }

    @Test
    public void testSequentialBackupsComplete() throws Exception {
        // Run two sequential backups to verify no deadlock occurs.
        for (int i = 0; i < 2; i++) {
            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

            File backupFile = File.createTempFile("testSequentialBackup_" + i, ".zip");
            backupFile.deleteOnExit();
            BackupExecutionAdapter backupExecution =
                    backupFacade.runBackupAsync(Files.asResource(backupFile), true, null, null, null, hints);

            Thread.sleep(100);

            assertNotNull(backupFacade.getBackupExecutions());
            assertFalse(backupFacade.getBackupExecutions().isEmpty());
            assertNotNull(backupExecution);

            int cnt = 0;
            while (cnt < 100 && (backupExecution.getStatus() != BatchStatus.COMPLETED || backupExecution.isRunning())) {
                Thread.sleep(100);
                cnt++;

                if (backupExecution.getStatus() == BatchStatus.ABANDONED
                        || backupExecution.getStatus() == BatchStatus.FAILED
                        || backupExecution.getStatus() == BatchStatus.UNKNOWN) {
                    for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                        LOGGER.log(Level.WARNING, "ERROR: " + exception.getLocalizedMessage(), exception);
                    }
                    break;
                }
            }

            assertEquals(
                    "Backup " + i + " should complete successfully",
                    BatchStatus.COMPLETED,
                    backupExecution.getStatus());
        }
    }
}
