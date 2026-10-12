/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.logging.Level;
import org.geoserver.catalog.*;
import org.geotools.util.factory.Hints;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

public class WmsWmtsRestoreTest extends BackupRestoreTestSupport {

    @Override
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testWmsWmtsRestore() throws Exception {
        // Given
        cleanCatalogInternal();

        Hints hints = new Hints(new HashMap<>(2));
        hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

        // When
        RestoreExecutionAdapter restoreExecution =
                backupFacade.runRestoreAsync(file("testWmsWmtsLayers.zip"), null, null, null, hints);
        waitForExecution(restoreExecution);

        // Then
        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();

        WMSStoreInfo wmsStoreInfo = restoreCatalog.getStoreByName("gs", "some-wms-store", WMSStoreInfo.class);
        assertNotNull(wmsStoreInfo);
        WMTSStoreInfo wmtsStoreInfo = restoreCatalog.getStoreByName("gs", "some-wmts-store", WMTSStoreInfo.class);
        assertNotNull(wmtsStoreInfo);
        WMSLayerInfo wmsLayerInfo = restoreCatalog.getResourceByStore(wmsStoreInfo, "wmsLayer", WMSLayerInfo.class);
        assertNotNull(wmsLayerInfo);
        WMTSLayerInfo wmtsLayerInfo =
                restoreCatalog.getResourceByStore(wmtsStoreInfo, "wmtsLayer", WMTSLayerInfo.class);
        assertNotNull(wmtsLayerInfo);
        LayerInfo wmsLayer = restoreCatalog.getLayerByName("gs:wmsLayer");
        assertNotNull(wmsLayer);
        LayerInfo wmtsLayer = restoreCatalog.getLayerByName("gs:wmtsLayer");
        assertNotNull(wmtsLayer);
    }

    /**
     * Without best-effort mode, so that a failure to write a cascaded resource fails the restore rather than ending up
     * as a warning next to a resource that is in the catalog but was never saved.
     */
    @Test
    public void testWmsWmtsRestoreWithoutBestEffort() throws Exception {
        cleanCatalogInternal();

        RestoreExecutionAdapter restoreExecution =
                backupFacade.runRestoreAsync(file("testWmsWmtsLayers.zip"), null, null, null, new HashMap<>());
        waitForExecution(restoreExecution);

        assertEquals(BatchStatus.COMPLETED, restoreExecution.getStatus());
        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        assertNotNull(restoreCatalog.getLayerByName("gs:wmsLayer"));
        assertNotNull(restoreCatalog.getLayerByName("gs:wmtsLayer"));
    }

    private void cleanCatalogInternal() {
        catalog.getWorkspaces().forEach(ws -> removeWorkspace(ws.getName()));
    }

    private void waitForExecution(RestoreExecutionAdapter restoreExecutionAdapter) throws InterruptedException {
        Thread.sleep(100);
        int cnt = 0;
        while (cnt < 100
                && (restoreExecutionAdapter.getStatus() != BatchStatus.COMPLETED
                        || restoreExecutionAdapter.isRunning())) {
            Thread.sleep(100);
            cnt++;

            if (restoreExecutionAdapter.getStatus() == BatchStatus.ABANDONED
                    || restoreExecutionAdapter.getStatus() == BatchStatus.FAILED
                    || restoreExecutionAdapter.getStatus() == BatchStatus.UNKNOWN) {

                for (Throwable exception : restoreExecutionAdapter.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }
    }
}
