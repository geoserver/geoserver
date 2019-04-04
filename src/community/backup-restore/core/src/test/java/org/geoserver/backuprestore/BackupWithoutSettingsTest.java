package org.geoserver.backuprestore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.geoserver.platform.resource.Files;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

public class BackupWithoutSettingsTest extends BackupRestoreTestSupport {

    protected static Backup backupFacade;

    @Before
    public void beforeTest() throws InterruptedException {
        backupFacade = (Backup) applicationContext.getBean("backupFacade");
        ensureCleanedQueues();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testRunSpringBatchBackupJob() throws Exception {

        Map<String, String> params = new HashMap<>();
        params.put(Backup.PARAM_BEST_EFFORT_MODE, "true");
        params.put(Backup.PARAM_SKIP_SETTINGS, "true");

        File backupFile = File.createTempFile("testRunSpringBatchBackupJob", ".zip");
        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(
                        Files.asResource(backupFile), true, null, null, null, params);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getBackupExecutions());
        assertTrue(!backupFacade.getBackupExecutions().isEmpty());
        assertNotNull(backupExecution);

        int cnt = 0;
        while (cnt < 100
                && (backupExecution.getStatus() != BatchStatus.COMPLETED
                        || !backupExecution.isRunning())) {
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

        if (backupExecution.getStatus() != BatchStatus.COMPLETED && backupExecution.isRunning()) {
            backupFacade.stopExecution(backupExecution.getId());
        }

        if (backupExecution.getStatus() == BatchStatus.COMPLETED) {
            ZipFile backupZip = new ZipFile(backupFile);
            ZipEntry zipEntry = backupZip.getEntry("global.xml");
            assertNull(zipEntry);
            assertNull(backupZip.getEntry("logging.xml"));
            assertNull(backupZip.getEntry("services.xml"));
            assertNull(backupZip.getEntry("security"));
        }
    }
}
