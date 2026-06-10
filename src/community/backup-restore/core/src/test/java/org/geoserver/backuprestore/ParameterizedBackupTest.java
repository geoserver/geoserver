package org.geoserver.backuprestore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
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

/** In its own test because other tests conflict with it unfortunately */
public class ParameterizedBackupTest extends BackupRestoreTestSupport {

    protected static Backup backupFacade;

    @Override
    public void beforeTest() throws InterruptedException {
        backupFacade = (Backup) applicationContext.getBean("backupFacade");
        ensureCleanedQueues();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testParameterizePasswordsInBackup() throws Exception {
        Hints hints = new Hints(new HashMap<>(2));
        hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
        hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_PARAMETERIZE_PASSWDS), Backup.PARAM_PARAMETERIZE_PASSWDS));

        File parameterizedBackup = File.createTempFile("parameterizedBackup", ".zip");
        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(Files.asResource(parameterizedBackup), true, null, null, null, hints);

        // Wait a bit
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

        if (backupExecution.getStatus() != BatchStatus.COMPLETED && backupExecution.isRunning()) {
            backupFacade.stopExecution(backupExecution.getId());
        }

        if (backupExecution.getStatus() == BatchStatus.COMPLETED) {
            // unzip the completed backup. Read the entry while the ZipFile is still open: a try-with-resources here
            // previously closed it (and the entry stream) before the scanner ran, so it always read nothing.
            String storeContent;
            try (ZipFile backup = new ZipFile(parameterizedBackup)) {
                ZipEntry entry = backup.getEntry("store.dat.1");
                try (Scanner scanner =
                        new Scanner(backup.getInputStream(entry), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                    storeContent = scanner.hasNext() ? scanner.next() : "";
                }
            }
            assertTrue(
                    "Expected the parameterized store output to contain a tokenized password, was:\n" + storeContent,
                    storeContent.contains("tokenizedPassword"));
        }
    }
}
