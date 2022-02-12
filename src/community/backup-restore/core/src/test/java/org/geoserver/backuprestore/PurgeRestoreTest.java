/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;

/**
 * Tests the BK_PURGE_RESOURCES flag is respected and all current catalog artifacts are removed
 * before restore the backup zip file.
 */
public class PurgeRestoreTest extends BackupRestoreTestSupport {

    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testPurgeRestore() throws Exception {

        Map<String, String> params = new HashMap<>();
        params.put(Backup.PARAM_BEST_EFFORT_MODE, "true");
        params.put(Backup.PARAM_SKIP_GWC, "false");
        params.put(Backup.PARAM_PURGE_RESOURCES, "true");

        RestoreExecutionAdapter restoreExecution =
                backupFacade.runRestoreAsync(file("purgeBk.zip"), null, null, null, params);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getRestoreExecutions());
        assertTrue(!backupFacade.getRestoreExecutions().isEmpty());

        assertNotNull(restoreExecution);

        Thread.sleep(100);

        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        assertNotNull(restoreCatalog);

        waitRestoreFinish(restoreExecution);

        List<LayerInfo> citeLayers =
                catalog.getLayers().stream()
                        .filter(li -> li.prefixedName().startsWith("cite:"))
                        .collect(Collectors.toList());
        assertEquals(1, citeLayers.size());
    }

    private void waitRestoreFinish(RestoreExecutionAdapter restoreExecution)
            throws InterruptedException, NoSuchJobExecutionException,
                    JobExecutionNotRunningException {
        int cnt = 0;
        while (cnt < 100
                && (restoreExecution.getStatus() != BatchStatus.COMPLETED
                        || !restoreExecution.isRunning())) {
            Thread.sleep(100);
            cnt++;

            if (restoreExecution.getStatus() == BatchStatus.ABANDONED
                    || restoreExecution.getStatus() == BatchStatus.FAILED
                    || restoreExecution.getStatus() == BatchStatus.UNKNOWN) {

                for (Throwable exception : restoreExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }

        if (restoreExecution.getStatus() != BatchStatus.COMPLETED && restoreExecution.isRunning()) {
            backupFacade.stopExecution(restoreExecution.getId());
        }
    }
}
