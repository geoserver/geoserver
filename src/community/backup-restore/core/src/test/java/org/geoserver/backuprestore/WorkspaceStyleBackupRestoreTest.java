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
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.resource.Files;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

/**
 * Tests that workspace-specific styles are correctly included in backup and restored with proper workspace context so
 * they survive GeoServer restarts.
 *
 * <p>Prior to the fix:
 *
 * <ul>
 *   <li>Backup only copied SLD files but not style XML metadata for workspace styles
 *   <li>Restore used getStyleByName() without workspace context, failing to match workspace styles
 *   <li>Style XML was written with name-based workspace references instead of ID-based, causing GeoServer's catalog
 *       loader to fail on restart
 * </ul>
 */
public class WorkspaceStyleBackupRestoreTest extends BackupRestoreTestSupport {

    @Override
    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    /**
     * Verifies that workspace style XML metadata files are included in the backup zip. Without the fix, only SLD files
     * were backed up for workspace styles, not the style.xml metadata.
     */
    @Test
    public void testWorkspaceStyleXmlIncludedInBackup() throws Exception {
        File backupFile = File.createTempFile("testWorkspaceStyleBackup", ".zip");
        backupFile.deleteOnExit();

        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(Files.asResource(backupFile), true, null, null, null, createHints());

        waitForCompletion(backupExecution);
        assertEquals(BatchStatus.COMPLETED, backupExecution.getStatus());

        // Verify the backup zip contains workspace style XML metadata files.
        // The test setup (BackupRestoreTestSupport) creates sf_style in workspace sf
        // and cite_style in workspace cite.
        try (ZipFile zip = new ZipFile(backupFile)) {
            ZipEntry sfStyleXml = zip.getEntry("workspaces/sf/styles/sf_style.xml");
            assertNotNull("Backup should contain workspace style XML: workspaces/sf/styles/sf_style.xml", sfStyleXml);

            // Verify XML content references the workspace
            try (InputStream is = zip.getInputStream(sfStyleXml)) {
                String content = new String(is.readAllBytes());
                assertTrue("Style XML should contain workspace reference", content.contains("<workspace>"));
                assertTrue("Style XML should contain style name", content.contains("<name>sf_style</name>"));
            }

            ZipEntry citeStyleXml = zip.getEntry("workspaces/cite/styles/cite_style.xml");
            assertNotNull(
                    "Backup should contain workspace style XML:" + " workspaces/cite/styles/cite_style.xml",
                    citeStyleXml);
        }
    }

    /**
     * Verifies that workspace styles survive a backup-then-restore round trip and are correctly associated with their
     * workspaces after restore. This uses a fresh backup (not pre-built test data) to ensure the backup includes the
     * workspace style XML metadata added by the fix.
     */
    @Test
    public void testWorkspaceStyleRoundTrip() throws Exception {
        // Step 1: Create a backup (which now includes workspace style XMLs)
        File backupFile = File.createTempFile("testWorkspaceStyleRoundTrip", ".zip");
        backupFile.deleteOnExit();

        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(Files.asResource(backupFile), true, null, null, null, createHints());

        waitForCompletion(backupExecution);
        assertEquals(BatchStatus.COMPLETED, backupExecution.getStatus());

        // Step 2: Restore from that backup
        RestoreExecutionAdapter restoreExecution =
                backupFacade.runRestoreAsync(Files.asResource(backupFile), null, null, null, createHints());

        Thread.sleep(100);

        assertNotNull(backupFacade.getRestoreExecutions());
        assertFalse(backupFacade.getRestoreExecutions().isEmpty());
        assertNotNull(restoreExecution);

        Thread.sleep(100);

        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        assertNotNull(restoreCatalog);

        waitForCompletion(restoreExecution);
        assertEquals(BatchStatus.COMPLETED, restoreExecution.getStatus());

        // Step 3: Verify workspace styles are restored with correct workspace association.
        // Without the fix, getStyleByName(workspace, name) would return null because
        // the restore used the non-workspace-aware getStyleByName(name) lookup.
        StyleInfo sfStyle = restoreCatalog.getStyleByName(restoreCatalog.getWorkspaceByName("sf"), "sf_style");
        assertNotNull("sf_style should be found via workspace-aware lookup", sfStyle);
        assertNotNull("sf_style should have workspace set", sfStyle.getWorkspace());
        assertEquals("sf", sfStyle.getWorkspace().getName());

        StyleInfo citeStyle = restoreCatalog.getStyleByName(restoreCatalog.getWorkspaceByName("cite"), "cite_style");
        assertNotNull("cite_style should be found via workspace-aware lookup", citeStyle);
        assertNotNull("cite_style should have workspace set", citeStyle.getWorkspace());
        assertEquals("cite", citeStyle.getWorkspace().getName());
    }

    private Hints createHints() {
        Hints hints = new Hints(new HashMap<>(2));
        hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
        return hints;
    }

    private void waitForCompletion(AbstractExecutionAdapter execution) throws Exception {
        int cnt = 0;
        while (cnt < 100 && (execution.getStatus() != BatchStatus.COMPLETED || execution.isRunning())) {
            Thread.sleep(100);
            cnt++;

            if (execution.getStatus() == BatchStatus.ABANDONED
                    || execution.getStatus() == BatchStatus.FAILED
                    || execution.getStatus() == BatchStatus.UNKNOWN) {
                for (Throwable exception : execution.getAllFailureExceptions()) {
                    LOGGER.log(Level.WARNING, "ERROR: " + exception.getLocalizedMessage(), exception);
                }
                break;
            }
        }
    }
}
