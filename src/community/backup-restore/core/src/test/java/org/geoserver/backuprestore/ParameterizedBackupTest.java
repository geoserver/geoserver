package org.geoserver.backuprestore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
    @Test
    public void testParameterizePasswordsInBackup() throws Exception {
        Hints hints = new Hints(new HashMap(2));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                        Backup.PARAM_BEST_EFFORT_MODE));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_PARAMETERIZE_PASSWDS),
                        Backup.PARAM_PARAMETERIZE_PASSWDS));

        File parameterizedBackup = File.createTempFile("parameterizedBackup", ".zip");
        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(
                        Files.asResource(parameterizedBackup), true, null, hints);

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

        // unzip the completed backup
        ZipFile backup = new ZipFile(parameterizedBackup);
        ZipEntry entry = backup.getEntry("store.dat.1");

        Scanner scanner = new Scanner(backup.getInputStream(entry), "UTF-8");
        boolean hasExpectedValue = false;
        while (scanner.hasNextLine() && !hasExpectedValue) {
            String line = scanner.nextLine();
            hasExpectedValue = line.contains("encryptedValue");
        }
        assertTrue("Expected the store output to contain tokenized password", hasExpectedValue);
    }
}
