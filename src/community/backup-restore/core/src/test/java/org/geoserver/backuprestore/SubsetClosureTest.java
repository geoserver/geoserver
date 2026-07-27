/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;

/**
 * Unit coverage for {@link SubsetClosure}: the transitive dependency-closure that makes a workspace-filtered backup
 * self-contained. The closure returns the ids of the objects <em>outside</em> the filtered workspaces (or global) that
 * the subset references; the workspace cascade keeps the in-subset objects and prunes any global object the closure
 * leaves out.
 *
 * <p>The standard test catalog already carries a global layergroup ({@code global}) that groups the {@code sf} layers
 * and a workspace-scoped one ({@code local}) in {@code sf}, which exercises the global-vs-scoped distinction directly.
 */
public class SubsetClosureTest extends BackupRestoreTestSupport {

    private static Filter sf() throws Exception {
        return ECQL.toFilter("name = 'sf'");
    }

    @Test
    public void testReferencedGlobalsIncludedSubsetScopedExcluded() throws Exception {
        Set<String> closure = SubsetClosure.compute(catalog, sf(), null, null);

        // the global layergroup groups sf layers -> pulled into the closure, together with the global style it uses
        LayerGroupInfo globalLg = catalog.getLayerGroupByName("global");
        assertNotNull(globalLg);
        assertTrue(
                "the global layergroup referencing the subset must be in the closure",
                closure.contains(globalLg.getId()));
        StyleInfo point = catalog.getStyleByName(StyleInfo.DEFAULT_POINT);
        assertNotNull(point);
        assertTrue("a global style the subset uses must be in the closure", closure.contains(point.getId()));

        // subset-scoped objects are kept by the workspace cascade, so they are NOT part of the forced closure set
        WorkspaceInfo sf = catalog.getWorkspaceByName("sf");
        assertFalse("the subset workspace is kept by the cascade, not forced", closure.contains(sf.getId()));
        assertFalse(
                "a workspace-scoped style is kept by the cascade, not forced",
                closure.contains(catalog.getStyleByName(sf, "sf_style").getId()));
        LayerGroupInfo localLg = catalog.getLayerGroupByName("sf", "local");
        assertNotNull(localLg);
        assertFalse(
                "a workspace-scoped layergroup is kept by the cascade, not forced", closure.contains(localLg.getId()));
    }

    @Test
    public void testUnreferencedGlobalStyleIsNotInClosure() throws Exception {
        StyleInfo orphan = catalog.getFactory().createStyle();
        orphan.setName("orphan_global_closure");
        orphan.setFilename("orphan.sld");
        catalog.add(orphan);
        try {
            Set<String> closure = SubsetClosure.compute(catalog, sf(), null, null);
            assertFalse(
                    "a global style nothing in the subset references must be absent (so the cascade prunes it)",
                    closure.contains(
                            catalog.getStyleByName("orphan_global_closure").getId()));
        } finally {
            catalog.remove(catalog.getStyleByName("orphan_global_closure"));
        }
    }

    /**
     * The MOST IMPORTANT closure path: an in-subset layer that paints with a GLOBAL (workspace-less) style. The layer
     * is kept by the workspace cascade, but its global default style lives outside any workspace, so the cascade would
     * prune it unless the closure force-includes it. This exercises the {@code LayerInfo -> getDefaultStyle()} ->
     * global {@code StyleInfo} branch of {@code directDependencies}, distinct from the global-style-via-layergroup path
     * already covered above. The standard sf layers paint with the global {@code "Default"} vector style.
     */
    @Test
    public void testInScopeLayerPullsItsGlobalDefaultStyle() throws Exception {
        LayerInfo sfLayer = catalog.getLayerByName("sf:PrimitiveGeoFeature");
        assertNotNull(sfLayer);
        StyleInfo globalDefault = sfLayer.getDefaultStyle();
        assertNotNull("the sf layer is expected to carry a default style", globalDefault);
        // sanity: it really is the shared, workspace-less "Default" vector style, not an sf-scoped one
        assertNull("the layer's default style must be a global, workspace-less style", globalDefault.getWorkspace());
        assertEquals(CiteTestData.DEFAULT_VECTOR_STYLE, globalDefault.getName());

        Set<String> closure = SubsetClosure.compute(catalog, sf(), null, null);

        assertTrue(
                "the global default style of an in-subset layer must be force-included so the cascade does not prune it",
                closure.contains(globalDefault.getId()));
        // and it is reachable through the layer alone: the in-subset layer's own workspace stays cascade-kept, never
        // forced, which proves the style id arrived via the layer -> default-style edge rather than via a layergroup
        WorkspaceInfo sf = catalog.getWorkspaceByName("sf");
        assertFalse("the in-subset workspace is kept by the cascade, not forced", closure.contains(sf.getId()));
    }

    @Test
    public void testCrossWorkspaceLayerGroupMembersArePulledIn() throws Exception {
        WorkspaceInfo sf = catalog.getWorkspaceByName("sf");
        LayerInfo sfLayer = catalog.getLayerByName("sf:PrimitiveGeoFeature");
        assertNotNull(sfLayer);
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

        LayerGroupInfo mixLg = catalog.getFactory().createLayerGroup();
        mixLg.setName("crossws_global_closure");
        mixLg.getLayers().add(sfLayer);
        mixLg.getLayers().add(foreignLayer);
        mixLg.getStyles().add(null);
        mixLg.getStyles().add(null);
        mixLg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));
        catalog.add(mixLg);
        try {
            Set<String> closure = SubsetClosure.compute(catalog, sf(), null, null);

            // the foreign member layer is pulled in, and so is the workspace it needs to resolve against
            assertTrue(
                    "a cross-workspace layergroup member layer must be pulled into the closure",
                    closure.contains(foreignLayer.getId()));
            assertTrue(
                    "the foreign member's workspace must be pulled in so it resolves on restore",
                    closure.contains(foreignWs.getId()));
            // the sf member stays a subset object (kept by the cascade), so it is not in the forced set
            assertFalse(closure.contains(sf.getId()));
        } finally {
            catalog.remove(mixLg);
        }
    }
}
