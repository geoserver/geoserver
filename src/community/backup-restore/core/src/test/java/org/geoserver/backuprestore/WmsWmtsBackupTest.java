/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.geoserver.platform.resource.Files;
import org.geotools.util.factory.Hints;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

public class WmsWmtsBackupTest extends BackupRestoreTestSupport {

    @Override
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testWmsWmtsBackup() throws IOException, InterruptedException {
        // Given
        Hints hints = new Hints(new HashMap<>(2));
        hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

        File backupFile = File.createTempFile("testRunSpringBatchBackupWms", ".zip");

        // When
        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(Files.asResource(backupFile), true, null, null, null, hints);
        waitForExecution(backupExecution);

        // Then
        if (backupExecution.getStatus() == BatchStatus.COMPLETED) {
            // unzip the completed backup
            Scanner scanner;
            boolean hasWmsStore;
            boolean hasWmtsStore;
            try (ZipFile backup = new ZipFile(backupFile)) {
                ZipEntry entry = backup.getEntry("store.dat.1");

                scanner = new Scanner(backup.getInputStream(entry), StandardCharsets.UTF_8);
                hasWmsStore = false;
                hasWmtsStore = false;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (!hasWmsStore) hasWmsStore = line.contains("some-wms-store");
                    if (!hasWmtsStore) hasWmtsStore = line.contains("some-wmts-store");
                }
            }
            assertTrue("Expected the store output to contain WMS store", hasWmsStore);
            assertTrue("Expected the store output to contain WMTS store", hasWmtsStore);
        }
    }

    private void waitForExecution(BackupExecutionAdapter backupExecution) throws InterruptedException {
        Thread.sleep(100);
        int cnt = 0;
        while (cnt < 100 && (backupExecution.getStatus() != BatchStatus.COMPLETED || backupExecution.isRunning())) {
            Thread.sleep(100);
            cnt++;

            if (backupExecution.getStatus() == BatchStatus.ABANDONED
                    || backupExecution.getStatus() == BatchStatus.FAILED
                    || backupExecution.getStatus() == BatchStatus.UNKNOWN) {

                for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }
    }
}
