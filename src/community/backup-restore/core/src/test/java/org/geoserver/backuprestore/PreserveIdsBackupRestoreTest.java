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
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.geoserver.platform.resource.Files;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

/**
 * Verifies the opt-in id-preserving backup mode ({@link Backup#PARAM_PRESERVE_IDS}).
 *
 * <p>Id preservation now defaults to on: a backup keeps every object's id and writes cross-references by id, so the
 * archive can be migrated through a restore into a different catalog with its identities (and, in turn, GWC tile-layer
 * links) intact. Opting out with {@code BK_PRESERVE_IDS=false} restores the legacy name-based archive (ids stripped).
 */
public class PreserveIdsBackupRestoreTest extends BackupRestoreTestSupport {

    @Override
    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testIdPreservationIsOnByDefaultAndCanBeDisabled() throws Exception {
        // ids are preserved by default now: a backup is a migration artifact, so identities (and GWC links) survive
        assertTrue(
                "a default backup must keep object ids",
                backupAndReadDat(null, "workspace.dat").contains("<id>"));
        assertTrue(
                "an explicit BK_PRESERVE_IDS=true backup must keep object ids",
                backupAndReadDat(Boolean.TRUE, "workspace.dat").contains("<id>"));

        // opting out restores the legacy name-based archive (ids stripped, portable into an empty data directory)
        assertFalse(
                "BK_PRESERVE_IDS=false must strip object ids",
                backupAndReadDat(Boolean.FALSE, "workspace.dat").contains("<id>"));
    }

    /**
     * Runs a best-effort backup and returns the first matching {@code .dat} fragment. {@code preserveIds} of {@code
     * null} leaves the option unset (exercising the default); {@code true}/{@code false} pass an explicit state.
     */
    private String backupAndReadDat(Boolean preserveIds, String datPrefix) throws Exception {
        File backupZip = File.createTempFile("preserveIds-" + preserveIds + "-", ".zip");
        backupZip.deleteOnExit();

        Hints hints = new Hints(new HashMap<>(2));
        hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
        if (preserveIds != null) {
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_PRESERVE_IDS, "*"), Boolean.toString(preserveIds)));
        }

        BackupExecutionAdapter exec =
                backupFacade.runBackupAsync(Files.asResource(backupZip), true, null, null, null, hints);
        waitForCompletion(exec);
        assertEquals(BatchStatus.COMPLETED, exec.getStatus());

        return readFirstEntry(backupZip, datPrefix);
    }

    /**
     * Reads the content of the first archive entry whose name starts with {@code namePrefix} (e.g. "workspace.dat").
     */
    private String readFirstEntry(File zipFile, String namePrefix) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry match = null;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.getName().startsWith(namePrefix)) {
                    match = e;
                    break;
                }
            }
            assertNotNull("archive should contain a " + namePrefix + " fragment", match);
            try (InputStream is = zip.getInputStream(match)) {
                return new String(is.readAllBytes());
            }
        }
    }

    private void waitForCompletion(AbstractExecutionAdapter execution) throws Exception {
        int cnt = 0;
        while (cnt < 100 && (execution.getStatus() != BatchStatus.COMPLETED || execution.isRunning())) {
            Thread.sleep(100);
            cnt++;
            if (execution.getStatus() == BatchStatus.ABANDONED
                    || execution.getStatus() == BatchStatus.FAILED
                    || execution.getStatus() == BatchStatus.UNKNOWN) {
                break;
            }
        }
    }
}
