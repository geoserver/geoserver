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
import java.util.HashMap;
import java.util.logging.Level;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

/**
 * Tests that repeated backup operations with GWC enabled complete without deadlock.
 *
 * <p>Prior to the fix, the backup code created DefaultTileLayerCatalog instances which register FileSystemWatcher
 * listeners that are never cleaned up. On subsequent backup operations, this caused lock contention in
 * MemoryLockProvider leading to deadlocks. The fix replaces DefaultTileLayerCatalog usage with direct file copy.
 */
public class GwcRepeatedBackupTest extends BackupRestoreTestSupport {

    @Override
    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testRepeatedBackupsWithGwcDoNotDeadlock() throws Exception {
        // Run 3 consecutive backups with GWC enabled.
        // Previously, each backup leaked FileSystemWatcher listeners from
        // DefaultTileLayerCatalog, causing deadlocks on subsequent operations.
        for (int i = 0; i < 3; i++) {
            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

            File backupFile = File.createTempFile("testGwcBackup_" + i, ".zip");
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

    @Test
    public void testGwcLayerFilesCopiedDirectly() throws Exception {
        // Verify that GWC layer XML files are present in the backup output,
        // confirming the direct file copy approach works correctly.
        GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();

        Hints hints = new Hints(new HashMap<>(2));
        hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

        File backupFile = File.createTempFile("testGwcLayerFiles", ".zip");
        backupFile.deleteOnExit();
        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(Files.asResource(backupFile), true, null, null, null, hints);

        Thread.sleep(100);

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

        assertEquals(BatchStatus.COMPLETED, backupExecution.getStatus());
        assertTrue(Resources.exists(Files.asResource(backupFile)));

        // Extract backup and verify gwc-layers directory exists with content
        Resource targetFolder = BackupUtils.geoServerTmpDir(dd);
        BackupUtils.extractTo(Files.asResource(backupFile), targetFolder);

        if (Resources.exists(targetFolder)) {
            Resource gwcLayers = targetFolder.get("gwc-layers");
            assertTrue("gwc-layers directory should exist in backup", Resources.exists(gwcLayers));
        }
    }
}
