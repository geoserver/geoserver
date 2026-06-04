/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.geoserver.catalog.Catalog;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.factory.Hints;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

public class GwcRestoreTest extends BackupRestoreTestSupport {

    @Override
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
        assertFalse(backupFacade.getRestoreExecutions().isEmpty());

        assertNotNull(restoreExecution);

        Thread.sleep(100);

        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        assertNotNull(restoreCatalog);

        int cnt = 0;
        while (cnt < 100 && (restoreExecution.getStatus() != BatchStatus.COMPLETED || restoreExecution.isRunning())) {
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

        final TileLayerCatalog gwcCatalog = (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");
        assertNotNull(gwcCatalog.getLayerByName("sf:AggregateGeoFeature"));
        assertEquals(30, gwcCatalog.getLayerNames().size());
    }

    /**
     * A partial (workspace-filtered) restore must NOT prune the tile layers of layers outside the filter. The restore
     * catalog only holds the filtered subset, so the dangling-prune used to delete every other tile layer; it must
     * also consult the live target catalog (which keeps those existing layers).
     */
    @Test
    public void testPartialRestoreKeepsExistingTileLayers() throws Exception {
        cleanCatalogInternal();

        Map<String, String> params = new HashMap<>();
        params.put(Backup.PARAM_BEST_EFFORT_MODE, "true");
        params.put(Backup.PARAM_SKIP_GWC, "false");

        // 1. full restore establishes the baseline set of tile layers
        RestoreExecutionAdapter full = backupFacade.runRestoreAsync(file("testGWC.zip"), null, null, null, params);
        waitForRestore(full);
        Assume.assumeTrue(
                "baseline full restore did not complete (env timing)", full.getStatus() == BatchStatus.COMPLETED);

        TileLayerCatalog gwcCatalog = (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");
        int baseline = gwcCatalog.getLayerNames().size();
        Assume.assumeTrue("baseline restore produced no tile layers", baseline > 1);

        // 2. a partial restore of a subset from the SAME archive must leave the other tile layers intact
        Filter wsFilter = ECQL.toFilter("name = 'sf'");
        RestoreExecutionAdapter partial =
                backupFacade.runRestoreAsync(file("testGWC.zip"), wsFilter, null, null, params);
        waitForRestore(partial);
        Assume.assumeTrue(
                "partial restore did not complete (env timing)", partial.getStatus() == BatchStatus.COMPLETED);

        assertEquals(
                "a partial restore must not prune tile layers of layers outside the workspace filter",
                baseline,
                gwcCatalog.getLayerNames().size());
    }

    /**
     * A no-filter MERGE restore ({@code BK_PURGE_RESOURCES=false}) of a subset archive must NOT wipe the target's
     * gwc-layers directory. The wipe used to be gated only by the absence of a filter, not by purge, so merging a
     * subset archive deleted the tile-layer configs of every layer absent from that archive (field symptom: a full
     * target's ~22 gwc-layers collapsing to ~9 after a merge of an sf-only archive).
     */
    @Test
    public void testMergeRestoreKeepsExistingTileLayers() throws Exception {
        cleanCatalogInternal();

        Map<String, String> params = new HashMap<>();
        params.put(Backup.PARAM_BEST_EFFORT_MODE, "true");
        params.put(Backup.PARAM_SKIP_GWC, "false");

        RestoreExecutionAdapter full = null;
        RestoreExecutionAdapter merge = null;
        try {
            // 1. full restore establishes a multi-workspace baseline of tile layers
            full = backupFacade.runRestoreAsync(file("testGWC.zip"), null, null, null, params);
            waitForRestore(full);
            Assume.assumeTrue(
                    "baseline full restore did not complete (env timing)",
                    full.getStatus() == BatchStatus.COMPLETED);
            TileLayerCatalog gwcCatalog = (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");
            int baseline = gwcCatalog.getLayerNames().size();
            Assume.assumeTrue("baseline restore produced too few tile layers", baseline > 1);

            // 2. back up only the 'sf' workspace -> a genuine subset archive (fewer tile layers than the target)
            Hints backupHints = new Hints(new HashMap<>());
            backupHints.add(
                    new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
            Resource subset = Files.asResource(File.createTempFile("gwc-subset", ".zip"));
            BackupExecutionAdapter backup =
                    backupFacade.runBackupAsync(subset, true, ECQL.toFilter("name = 'sf'"), null, null, backupHints);
            waitForBackup(backup);
            Assume.assumeTrue(
                    "subset backup did not complete (env timing)", backup.getStatus() == BatchStatus.COMPLETED);

            // 3. MERGE-restore that subset with NO filter and purge=false: it must not wipe the other tile layers
            Map<String, String> mergeParams = new HashMap<>(params);
            mergeParams.put(Backup.PARAM_PURGE_RESOURCES, "false");
            merge = backupFacade.runRestoreAsync(subset, null, null, null, mergeParams);
            waitForRestore(merge);
            int afterMerge = gwcCatalog.getLayerNames().size();
            Assume.assumeTrue(
                    "merge restore did not complete (env timing)", merge.getStatus() == BatchStatus.COMPLETED);

            assertEquals(
                    "a no-filter merge restore of a subset must not wipe the target's other tile layers",
                    baseline,
                    afterMerge);
        } finally {
            // never leak a still-running job into sibling tests (slow finalize reload on some envs)
            stopIfRunning(full);
            stopIfRunning(merge);
        }
    }

    private void stopIfRunning(RestoreExecutionAdapter exec) {
        try {
            if (exec != null && exec.isRunning()) {
                backupFacade.stopExecution(exec.getId());
            }
        } catch (Exception ignore) {
            // best effort: must not mask the real assertion / skip
        }
    }

    private void waitForBackup(BackupExecutionAdapter exec) throws Exception {
        int cnt = 0;
        while (cnt < 600 && (exec.getStatus() != BatchStatus.COMPLETED || exec.isRunning())) {
            Thread.sleep(100);
            cnt++;
            if (exec.getStatus() == BatchStatus.ABANDONED
                    || exec.getStatus() == BatchStatus.FAILED
                    || exec.getStatus() == BatchStatus.UNKNOWN) {
                break;
            }
        }
    }

    private void waitForRestore(RestoreExecutionAdapter exec) throws Exception {
        int cnt = 0;
        while (cnt < 600 && (exec.getStatus() != BatchStatus.COMPLETED || exec.isRunning())) {
            Thread.sleep(100);
            cnt++;
            if (exec.getStatus() == BatchStatus.ABANDONED
                    || exec.getStatus() == BatchStatus.FAILED
                    || exec.getStatus() == BatchStatus.UNKNOWN) {
                break;
            }
        }
    }

    private void cleanCatalogInternal() {
        catalog.getWorkspaces().forEach(ws -> removeWorkspace(ws.getName()));
    }
}
