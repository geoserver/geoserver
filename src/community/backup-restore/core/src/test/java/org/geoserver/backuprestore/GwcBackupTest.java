/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

public class GwcBackupTest extends BackupRestoreTestSupport {

    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testBackupExcludedResources() throws Exception {
        GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();

        BackupUtils.dir(dd.get(Paths.BASE), "foo/folder");
        assertTrue(Resources.exists(dd.get("foo/folder")));

        Hints hints = new Hints(new HashMap(3));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                        Backup.PARAM_BEST_EFFORT_MODE));
        hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_SKIP_GWC), Backup.PARAM_SKIP_GWC));

        Resource backupFile =
                Files.asResource(File.createTempFile("testRunSpringBatchBackupGWC", ".zip"));
        if (Resources.exists(backupFile)) {
            assertTrue(backupFile.delete());
        }
        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(backupFile, true, null, null, null, hints);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getBackupExecutions());
        assertTrue(!backupFacade.getBackupExecutions().isEmpty());
        assertNotNull(backupExecution);

        int cnt = 0;
        while (cnt < 100
                && (backupExecution.getStatus() != BatchStatus.COMPLETED
                        || backupExecution.isRunning())) {
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

        assertEquals(backupExecution.getStatus(), BatchStatus.COMPLETED);

        assertTrue(Resources.exists(backupFile));
        Resource srcDir = BackupUtils.dir(dd.get(Paths.BASE), "WEB-INF");
        assertTrue(Resources.exists(srcDir));

        Resource targetFolder = BackupUtils.geoServerTmpDir(dd);
        BackupUtils.extractTo(backupFile, targetFolder);

        if (Resources.exists(targetFolder)) {
            assertTrue(Resources.exists(targetFolder.get("/gwc-layers")));
        }
    }
}
