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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.resource.Files;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

/**
 * End-to-end coverage for the workspace-filter <em>subset</em>: a workspace-filtered backup must produce an archive
 * carrying only the filtered workspace's objects, instead of leaking every other workspace's stores, resources, layers,
 * styles and namespaces (the historical behaviour, where only the workspace step honoured the filter).
 *
 * <p>The test runs a real {@code wsFilter='sf'} backup of the standard CITE test catalog and inspects the produced
 * archive fragments directly (no restore — that keeps it non-destructive and free of restore-into-the-same-instance
 * artefacts such as GWC tile-layer clashes). It asserts the cascade scoped the steps that previously ignored the
 * workspace filter: {@code store.dat} carries the {@code sf} store but not the default-workspace {@code foo} store, and
 * {@code namespace.dat} carries only the {@code sf} namespace, not the sibling {@code cdf}/{@code cgf} ones.
 *
 * <p>Assertions match object {@code <name>}/prefixes rather than workspace <em>references</em>, so they hold whether
 * the archive writes cross-references by name or by id ({@code BK_PRESERVE_IDS}).
 */
public class BackupSubsetClosureTest extends BackupRestoreTestSupport {

    @Override
    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();
        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testWorkspaceFilteredBackupYieldsSelfContainedSubset() throws Exception {
        // sanity: the live catalog is multi-workspace and carries a non-sf store ("foo", in the default workspace)
        WorkspaceInfo sf = catalog.getWorkspaceByName("sf");
        assertNotNull(sf);
        assertNotNull(catalog.getWorkspaceByName("cite"));
        List<StoreInfo> sfStores = catalog.getStoresByWorkspace(sf, StoreInfo.class);
        assertFalse("expected the 'sf' workspace to own at least one store", sfStores.isEmpty());
        String sfStoreName = sfStores.get(0).getName();

        // workspace-filtered backup of the live catalog
        Filter wsFilter = ECQL.toFilter("name = 'sf'");
        File backupZip = File.createTempFile("subset-closure-sf-", ".zip");
        backupZip.deleteOnExit();
        Hints hints = new Hints(new HashMap<>(2));
        hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(Files.asResource(backupZip), true, wsFilter, null, null, hints);
        waitForCompletion(backupExecution);
        assertEquals(BatchStatus.COMPLETED, backupExecution.getStatus());

        String workspaceDat = readFirstEntry(backupZip, "workspace.dat");
        String storeDat = readFirstEntry(backupZip, "store.dat");
        String namespaceDat = readFirstEntry(backupZip, "namespace.dat");

        // the workspace step honoured the filter (baseline that already worked) ...
        assertTrue("workspace.dat must carry the filtered 'sf' workspace", workspaceDat.contains("<name>sf</name>"));
        assertFalse("workspace.dat must not carry the 'cdf' workspace", workspaceDat.contains("<name>cdf</name>"));

        // ... the cascade now scopes the store step too — previously store.dat carried every workspace's stores,
        // including the default-workspace 'foo' GeoPackage store.
        assertTrue("store.dat must carry the sf store", storeDat.contains("<name>" + sfStoreName + "</name>"));
        assertFalse(
                "the default-workspace 'foo' store leaked into the sf subset (workspace cascade failed)",
                storeDat.contains("<name>foo</name>"));

        // ... and namespaces cascade with their workspace: only the sf namespace survives. 'cdf'/'cgf' are clean
        // markers — neither string occurs anywhere in the sf namespace's prefix or URI.
        assertFalse("the 'cdf' namespace leaked into the sf subset", namespaceDat.contains("cdf"));
        assertFalse("the 'cgf' namespace leaked into the sf subset", namespaceDat.contains("cgf"));
    }

    @Test
    public void testClosurePrunesUnreferencedGlobalStyleAndKeepsScopedStyle() throws Exception {
        // a global style that nothing references; the dependency-closure must leave it out of the sf subset
        StyleInfo orphan = catalog.getFactory().createStyle();
        orphan.setName("orphan_global_subset");
        orphan.setFilename("orphan.sld");
        catalog.add(orphan);
        try {
            Filter wsFilter = ECQL.toFilter("name = 'sf'");
            File backupZip = File.createTempFile("subset-closure-styles-", ".zip");
            backupZip.deleteOnExit();
            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

            BackupExecutionAdapter backupExecution =
                    backupFacade.runBackupAsync(Files.asResource(backupZip), true, wsFilter, null, null, hints);
            waitForCompletion(backupExecution);
            assertEquals(BatchStatus.COMPLETED, backupExecution.getStatus());

            String styleDat = readFirstEntry(backupZip, "style.dat");
            assertFalse(
                    "an unreferenced global style must be pruned from the subset by the dependency-closure",
                    styleDat.contains("orphan_global_subset"));
            assertTrue("the workspace-scoped sf_style must be kept in the subset", styleDat.contains("sf_style"));
        } finally {
            catalog.remove(catalog.getStyleByName("orphan_global_subset"));
        }
    }

    /**
     * Regression guard for the cross-workspace layergroup-member <em>data loss</em>: a GLOBAL layergroup grouping an
     * {@code sf} layer (in the filter) and a foreign-workspace layer (NOT in the filter) must drag the WHOLE foreign
     * chain into the {@code wsFilter='sf'} archive — the foreign member's workspace, namespace, store, resource and
     * layer — so the group restores with every member intact.
     *
     * <p>The {@link SubsetClosure} math already places the foreign member's <em>workspace</em> in the forced set
     * (proven by {@code SubsetClosureTest.testCrossWorkspaceLayerGroupMembersArePulledIn}). This test exercises the
     * live pipeline end-to-end and asserts the WRITTEN ARCHIVE, catching the case where the closure is computed but not
     * applied to the workspace step: a foreign workspace whose id is in the closure was still dropped by the bare
     * workspace-filter check, so {@code workspace.dat} carried only {@code sf} and the foreign member was orphaned (its
     * store/resource/layer survived but had no workspace to resolve against on restore).
     */
    @Test
    public void testGlobalLayerGroupDragsForeignMemberWorkspaceIntoSubset() throws Exception {
        WorkspaceInfo sf = catalog.getWorkspaceByName("sf");
        assertNotNull(sf);
        LayerInfo sfLayer = catalog.getLayerByName("sf:PrimitiveGeoFeature");
        assertNotNull(sfLayer);

        // a real, persisted layer living in a workspace other than the filtered 'sf' one
        LayerInfo foreignLayer = catalog.getLayers().stream()
                .filter(l -> l.getResource() != null
                        && l.getResource().getStore() != null
                        && l.getResource().getStore().getWorkspace() != null
                        && !"sf"
                                .equals(l.getResource()
                                        .getStore()
                                        .getWorkspace()
                                        .getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a non-sf layer in the test catalog"));
        WorkspaceInfo foreignWs = foreignLayer.getResource().getStore().getWorkspace();
        String foreignWsName = foreignWs.getName();
        String foreignStoreName = foreignLayer.getResource().getStore().getName();
        String foreignLayerName = foreignLayer.getName();
        // sanity: the foreign member really is outside the subset, so a plain cascade would drop it
        assertFalse("test fixture broken: the 'foreign' member is actually in 'sf'", "sf".equals(foreignWsName));

        // a GLOBAL (workspace-less) layergroup grouping the in-subset sf layer and the foreign-workspace layer
        LayerGroupInfo mixLg = catalog.getFactory().createLayerGroup();
        mixLg.setName("crossws_global_backup");
        mixLg.getLayers().add(sfLayer);
        mixLg.getLayers().add(foreignLayer);
        mixLg.getStyles().add(null);
        mixLg.getStyles().add(null);
        mixLg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));
        catalog.add(mixLg);
        try {
            Filter wsFilter = ECQL.toFilter("name = 'sf'");
            File backupZip = File.createTempFile("subset-closure-crossws-", ".zip");
            backupZip.deleteOnExit();
            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

            BackupExecutionAdapter backupExecution =
                    backupFacade.runBackupAsync(Files.asResource(backupZip), true, wsFilter, null, null, hints);
            waitForCompletion(backupExecution);
            assertEquals(BatchStatus.COMPLETED, backupExecution.getStatus());

            String workspaceDat = readFirstEntry(backupZip, "workspace.dat");
            String storeDat = readFirstEntry(backupZip, "store.dat");
            String layerDat = readFirstEntry(backupZip, "layer.dat");
            String layerGroupDat = readFirstEntry(backupZip, "layerGroup.dat");

            // baseline that already worked: the filtered workspace and the global layergroup are in the archive
            assertTrue(
                    "workspace.dat must carry the filtered 'sf' workspace", workspaceDat.contains("<name>sf</name>"));
            assertTrue(
                    "the global layergroup grouping subset content must be in the archive",
                    layerGroupDat.contains("crossws_global_backup"));

            // the data-loss assertions: the foreign member's whole chain must be dragged in by the closure.
            assertTrue(
                    "DATA LOSS: the foreign member's workspace '"
                            + foreignWsName
                            + "' was dropped from the subset, so its layer cannot resolve on restore",
                    workspaceDat.contains("<name>" + foreignWsName + "</name>"));
            assertTrue(
                    "the foreign member's store must be dragged into the subset",
                    storeDat.contains("<name>" + foreignStoreName + "</name>"));
            assertTrue(
                    "the foreign member layer itself must be dragged into the subset",
                    layerDat.contains("<name>" + foreignLayerName + "</name>"));
        } finally {
            catalog.remove(mixLg);
        }
    }

    /** Reads the content of the first archive entry whose name starts with {@code namePrefix} (e.g. "store.dat"). */
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

    private void waitForCompletion(AbstractExecutionAdapter execution) throws InterruptedException {
        // A backup of the full catalog runs past a few seconds on a loaded box; wait generously, bail early only on a
        // terminal failure status. (No restore here, so none of the documented restore-STARTED flakiness applies.)
        int cnt = 0;
        while (cnt < 600 && (execution.getStatus() != BatchStatus.COMPLETED || execution.isRunning())) {
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
