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
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.resource.Files;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.ecql.ECQL;
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
        assertFalse(
                "workspace.dat must not carry the 'cdf' workspace", workspaceDat.contains("<name>cdf</name>"));

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
            assertTrue(
                    "the workspace-scoped sf_style must be kept in the subset", styleDat.contains("sf_style"));
        } finally {
            catalog.remove(catalog.getStyleByName("orphan_global_subset"));
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
