package org.geoserver.backuprestore;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.geoserver.platform.resource.Files;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

public class BackupWithoutSettingsTest extends BackupRestoreTestSupport {
    @Test
    public void testRunSpringBatchBackupJob() throws Exception {

        Map<String,String> params = new HashMap<>();
        params.put(Backup.PARAM_BEST_EFFORT_MODE, "true");
        params.put(Backup.PARAM_SKIP_SETTINGS, "true");

        File backupFile = File.createTempFile("testRunSpringBatchBackupJob", ".zip");
        BackupExecutionAdapter backupExecution = backupFacade.runBackupAsync(
            Files.asResource(backupFile), true, null, params);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getBackupExecutions());
        assertTrue(!backupFacade.getBackupExecutions().isEmpty());
        assertNotNull(backupExecution);

        while (backupExecution.getStatus() != BatchStatus.COMPLETED) {
            Thread.sleep(100);

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

        assertTrue(backupExecution.getStatus() == BatchStatus.COMPLETED);
        ZipFile backupZip = new ZipFile(backupFile);
        ZipEntry zipEntry = backupZip.getEntry("global.xml");
        assertNull(zipEntry);
        assertNull(backupZip.getEntry("logging.xml"));
        assertNull(backupZip.getEntry("services.xml"));
        assertNull(backupZip.getEntry("security"));
    }
}
