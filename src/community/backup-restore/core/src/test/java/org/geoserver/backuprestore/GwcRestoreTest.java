/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.geoserver.catalog.Catalog;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

public class GwcRestoreTest extends BackupRestoreTestSupport {

    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testGwcRestore() throws Exception {
        cleanCatalogInternal();

        Map<String, String> params = new HashMap<>();
        params.put(Backup.PARAM_BEST_EFFORT_MODE, "true");
        params.put(Backup.PARAM_SKIP_GWC, "false");

        RestoreExecutionAdapter restoreExecution =
                backupFacade.runRestoreAsync(file("testGWC.zip"), null, null, null, params);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getRestoreExecutions());
        assertTrue(!backupFacade.getRestoreExecutions().isEmpty());

        assertNotNull(restoreExecution);

        Thread.sleep(100);

        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        assertNotNull(restoreCatalog);

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

        final TileLayerCatalog gwcCatalog =
                (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");
        assertNotNull(gwcCatalog.getLayerByName("sf:AggregateGeoFeature"));
        assertEquals(30, gwcCatalog.getLayerNames().size());
    }

    private void cleanCatalogInternal() {
        catalog.getWorkspaces().forEach(ws -> removeWorkspace(ws.getName()));
    }
}
