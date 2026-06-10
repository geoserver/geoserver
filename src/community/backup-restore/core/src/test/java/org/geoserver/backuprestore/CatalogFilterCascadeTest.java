/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.backuprestore.processor.CatalogItemProcessor;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Test;

/**
 * Unit-level coverage for the workspace-filter <em>cascade</em> in {@link BackupRestoreItem#filteredResource}.
 *
 * <p>A backup/restore workspace filter ({@code filters[0]}) historically only scoped the workspace step; stores,
 * resources and layers honoured only the store/layer filters and styles/layergroups were never filtered at all, so a
 * "{@code name = 'sf'}" backup still carried every other workspace's stores, layers and styles. The cascade evaluates
 * the workspace filter against each item's resolved workspace, so a workspace filter yields a self-contained subset.
 * Global (workspace-less) styles and layergroups are deliberately retained — they are shared and reconciled by the
 * dependency-closure pass, not dropped by the cascade.
 *
 * <p>{@code filteredResource} only consults the configured filters (not the job context), so the decision is exercised
 * directly here with a hand-built {@link CatalogItemProcessor}, keeping the test fast and free of Spring Batch wiring.
 */
public class CatalogFilterCascadeTest extends BackupRestoreTestSupport {

    private static Filter[] wsOnly(String cql) throws Exception {
        return new Filter[] {ECQL.toFilter(cql), null, null};
    }

    /**
     * A processor configured as if it were running a backup ({@code isNew == false}), since the workspace-filter
     * cascade is scoped to backups (a restore replays the archive verbatim). The full job sets this through
     * {@code retrieveInterstepData}; here it is set directly to keep the decision test free of Spring Batch wiring.
     */
    private <T> CatalogItemProcessor<T> backupProcessor(Class<T> clazz, Filter[] filters) {
        CatalogItemProcessor<T> proc = new CatalogItemProcessor<>(clazz, backupFacade);
        proc.setFilters(filters);
        proc.setNew(false);
        return proc;
    }

    @Test
    public void testWorkspaceFilterCascadesToStores() throws Exception {
        WorkspaceInfo sf = catalog.getWorkspaceByName("sf");
        WorkspaceInfo cite = catalog.getWorkspaceByName("cite");
        assertNotNull(sf);
        assertNotNull(cite);

        List<StoreInfo> sfStores = catalog.getStoresByWorkspace(sf, StoreInfo.class);
        List<StoreInfo> citeStores = catalog.getStoresByWorkspace(cite, StoreInfo.class);
        assertFalse("expected the 'sf' workspace to own at least one store", sfStores.isEmpty());
        assertFalse("expected the 'cite' workspace to own at least one store", citeStores.isEmpty());

        CatalogItemProcessor<StoreInfo> proc = backupProcessor(StoreInfo.class, wsOnly("name = 'sf'"));

        // a store in the filtered workspace is kept ...
        assertFalse(proc.filteredResource(sfStores.get(0), sf, true, StoreInfo.class));
        // ... while a store in any other workspace is cascaded out (previously leaked: only filters[1] was honoured)
        assertTrue(proc.filteredResource(citeStores.get(0), cite, true, StoreInfo.class));
        // a strict resource whose workspace cannot be resolved cannot belong to the subset
        assertTrue(proc.filteredResource(sfStores.get(0), null, true, StoreInfo.class));
    }

    @Test
    public void testWorkspaceFilterCascadesToStylesButKeepsGlobals() throws Exception {
        WorkspaceInfo sf = catalog.getWorkspaceByName("sf");
        WorkspaceInfo cite = catalog.getWorkspaceByName("cite");

        StyleInfo sfStyle = catalog.getStyleByName(sf, "sf_style");
        StyleInfo citeStyle = catalog.getStyleByName(cite, "cite_style");
        StyleInfo globalPoint = catalog.getStyleByName(StyleInfo.DEFAULT_POINT);
        assertNotNull(sfStyle);
        assertNotNull(citeStyle);
        assertNotNull(globalPoint);

        CatalogItemProcessor<StyleInfo> proc = backupProcessor(StyleInfo.class, wsOnly("name = 'sf'"));

        // workspace-scoped style inside the subset is kept ...
        assertFalse(proc.filteredResource(sfStyle, sf, false, StyleInfo.class));
        // ... a workspace-scoped style from another workspace is cascaded out (previously never filtered) ...
        assertTrue(proc.filteredResource(citeStyle, cite, false, StyleInfo.class));
        // ... and a global, workspace-less style is retained for the dependency-closure pass to reconcile.
        assertFalse(proc.filteredResource(globalPoint, null, false, StyleInfo.class));
    }

    @Test
    public void testWorkspaceFilterCascadesToNamespaces() throws Exception {
        WorkspaceInfo sf = catalog.getWorkspaceByName("sf");
        WorkspaceInfo cite = catalog.getWorkspaceByName("cite");

        NamespaceInfo sfNs = catalog.getNamespaceByPrefix("sf");
        NamespaceInfo citeNs = catalog.getNamespaceByPrefix("cite");
        assertNotNull(sfNs);
        assertNotNull(citeNs);

        CatalogItemProcessor<NamespaceInfo> proc = backupProcessor(NamespaceInfo.class, wsOnly("name = 'sf'"));

        // a namespace is paired with its workspace (shared prefix); only the subset's namespace survives
        assertFalse(proc.filteredResource(sfNs, sf, true, NamespaceInfo.class));
        assertTrue(proc.filteredResource(citeNs, cite, true, NamespaceInfo.class));
    }

    @Test
    public void testStoreOrLayerFilterWithoutWorkspaceFilterIsUnaffected() throws Exception {
        WorkspaceInfo cite = catalog.getWorkspaceByName("cite");
        List<StoreInfo> citeStores = catalog.getStoresByWorkspace(cite, StoreInfo.class);
        assertFalse(citeStores.isEmpty());

        // only a layer filter is set: filters[0] is null, so the workspace cascade stays inert and a
        // store from any workspace is still evaluated solely against the (here unset) store filter.
        CatalogItemProcessor<StoreInfo> proc =
                backupProcessor(StoreInfo.class, new Filter[] {null, null, ECQL.toFilter("name = 'irrelevant'")});

        assertFalse(proc.filteredResource(citeStores.get(0), cite, true, StoreInfo.class));
    }
}
