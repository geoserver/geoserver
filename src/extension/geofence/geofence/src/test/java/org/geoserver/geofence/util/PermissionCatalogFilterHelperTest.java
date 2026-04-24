/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.util;

import static org.junit.Assert.assertEquals;

import java.util.TreeSet;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.geofence.services.dto.PermsResult;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.PropertyName;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link PermissionCatalogFilterHelper}.
 *
 * <p>Tests cover all code paths in {@link PermissionCatalogFilterHelper#buildCatalogFilter} without
 * requiring a live GeoFence service.
 */
public class PermissionCatalogFilterHelperTest {

    private PermissionCatalogFilterHelper helper;

    @Before
    public void setUp() {
        helper = new PermissionCatalogFilterHelper();
    }

    // -----------------------------------------------------------------------
    // StyleInfo
    // -----------------------------------------------------------------------

    @Test
    public void testStyleInfoAlwaysIncluded() {
        PermsResult permsResult = emptyPermsResult();
        Filter result = helper.buildCatalogFilter(permsResult, StyleInfo.class);
        assertEquals(
                "StyleInfo should always return Filter.INCLUDE regardless of permsResult",
                Filter.INCLUDE,
                result);
    }

    // -----------------------------------------------------------------------
    // WorkspaceInfo
    // -----------------------------------------------------------------------

    @Test
    public void testWorkspaceInfoEmptyResourcesExcluded() {
        PermsResult permsResult = emptyPermsResult();
        Filter result = helper.buildCatalogFilter(permsResult, WorkspaceInfo.class);
        assertEquals(
                "WorkspaceInfo with no accessible resources should return Filter.EXCLUDE",
                Filter.EXCLUDE,
                result);
    }

    @Test
    public void testWorkspaceInfoGlobalWildcardIncluded() {
        PermsResult permsResult = permsResultWithResources("*:*");
        Filter result = helper.buildCatalogFilter(permsResult, WorkspaceInfo.class);
        assertEquals(
                "WorkspaceInfo with '*:*' grant should return Filter.INCLUDE",
                Filter.INCLUDE,
                result);
    }

    @Test
    public void testWorkspaceInfoCrossWorkspaceLayerGrantIncluded() {
        PermsResult permsResult = permsResultWithResources("*:someLayer");
        Filter result = helper.buildCatalogFilter(permsResult, WorkspaceInfo.class);
        assertEquals(
                "WorkspaceInfo with '*:layer' grant should return Filter.INCLUDE (all workspaces visible)",
                Filter.INCLUDE,
                result);
    }

    @Test
    public void testWorkspaceInfoSingleWorkspaceFilter() {
        PermsResult permsResult = permsResultWithResources("myws:layerA");
        Filter result = helper.buildCatalogFilter(permsResult, WorkspaceInfo.class);
        PropertyIsEqualTo equalTo = assertPropertyIsEqualTo(result);
        assertEquals("name", ((PropertyName) equalTo.getExpression1()).getPropertyName());
        assertEquals("myws", equalTo.getExpression2().evaluate(null));
    }

    @Test
    public void testWorkspaceInfoMultipleWorkspacesOrFilter() {
        PermsResult permsResult = permsResultWithResources("ws1:layerA", "ws1:layerB", "ws2:layerC");
        Filter result = helper.buildCatalogFilter(permsResult, WorkspaceInfo.class);
        Or orFilter = assertOr(result);
        assertEquals(
                "Should have two workspace branches (ws1 and ws2) in the OR",
                2,
                orFilter.getChildren().size());
    }

    @Test
    public void testWorkspaceInfoDeduplicatesWorkspaces() {
        // Two entries for the same workspace should produce a single equality filter, not OR
        PermsResult permsResult = permsResultWithResources("ws1:layer1", "ws1:layer2");
        Filter result = helper.buildCatalogFilter(permsResult, WorkspaceInfo.class);
        PropertyIsEqualTo equalTo = assertPropertyIsEqualTo(result);
        assertEquals("name", ((PropertyName) equalTo.getExpression1()).getPropertyName());
        assertEquals("ws1", equalTo.getExpression2().evaluate(null));
    }

    // -----------------------------------------------------------------------
    // LayerInfo - CQL filter with workspace/layer property remapping
    // -----------------------------------------------------------------------

    @Test
    public void testLayerInfoCqlFilterInclude() {
        PermsResult permsResult = cqlPermsResult("INCLUDE");
        Filter result = helper.buildCatalogFilter(permsResult, LayerInfo.class);
        assertEquals(Filter.INCLUDE, result);
    }

    @Test
    public void testLayerInfoCqlFilterExclude() {
        PermsResult permsResult = cqlPermsResult("EXCLUDE");
        Filter result = helper.buildCatalogFilter(permsResult, LayerInfo.class);
        assertEquals(Filter.EXCLUDE, result);
    }

    @Test
    public void testLayerInfoWorkspacePropertyRemapped() {
        PermsResult permsResult = cqlPermsResult("workspace = 'myws'");
        Filter result = helper.buildCatalogFilter(permsResult, LayerInfo.class);
        PropertyIsEqualTo eq = assertPropertyIsEqualTo(result);
        assertEquals(
                "workspace property for LayerInfo should be remapped to 'resource.store.workspace.name'",
                "resource.store.workspace.name",
                ((PropertyName) eq.getExpression1()).getPropertyName());
    }

    @Test
    public void testLayerInfoLayerPropertyRemapped() {
        PermsResult permsResult = cqlPermsResult("layer = 'myLayer'");
        Filter result = helper.buildCatalogFilter(permsResult, LayerInfo.class);
        PropertyIsEqualTo eq = assertPropertyIsEqualTo(result);
        assertEquals(
                "layer property for LayerInfo should be remapped to 'name'",
                "name",
                ((PropertyName) eq.getExpression1()).getPropertyName());
    }

    // -----------------------------------------------------------------------
    // LayerGroupInfo - CQL filter with workspace/layer property remapping
    // -----------------------------------------------------------------------

    @Test
    public void testLayerGroupInfoWorkspacePropertyRemapped() {
        PermsResult permsResult = cqlPermsResult("workspace = 'myws'");
        Filter result = helper.buildCatalogFilter(permsResult, LayerGroupInfo.class);
        PropertyIsEqualTo eq = assertPropertyIsEqualTo(result);
        assertEquals(
                "workspace property for LayerGroupInfo should be remapped to 'workspace.name'",
                "workspace.name",
                ((PropertyName) eq.getExpression1()).getPropertyName());
    }

    @Test
    public void testLayerGroupInfoLayerPropertyRemapped() {
        PermsResult permsResult = cqlPermsResult("layer = 'myGroup'");
        Filter result = helper.buildCatalogFilter(permsResult, LayerGroupInfo.class);
        PropertyIsEqualTo eq = assertPropertyIsEqualTo(result);
        assertEquals(
                "layer property for LayerGroupInfo should be remapped to 'name'",
                "name",
                ((PropertyName) eq.getExpression1()).getPropertyName());
    }

    // -----------------------------------------------------------------------
    // ResourceInfo - CQL filter with workspace/layer property remapping
    // -----------------------------------------------------------------------

    @Test
    public void testResourceInfoWorkspacePropertyRemapped() {
        PermsResult permsResult = cqlPermsResult("workspace = 'myws'");
        Filter result = helper.buildCatalogFilter(permsResult, ResourceInfo.class);
        PropertyIsEqualTo eq = assertPropertyIsEqualTo(result);
        assertEquals(
                "workspace property for ResourceInfo should be remapped to 'store.workspace.name'",
                "store.workspace.name",
                ((PropertyName) eq.getExpression1()).getPropertyName());
    }

    @Test
    public void testResourceInfoLayerPropertyRemapped() {
        PermsResult permsResult = cqlPermsResult("layer = 'myResource'");
        Filter result = helper.buildCatalogFilter(permsResult, ResourceInfo.class);
        PropertyIsEqualTo eq = assertPropertyIsEqualTo(result);
        assertEquals(
                "layer property for ResourceInfo should be remapped to 'name'",
                "name",
                ((PropertyName) eq.getExpression1()).getPropertyName());
    }

    // -----------------------------------------------------------------------
    // PublishedInfo - CQL filter with workspace/layer property remapping
    // -----------------------------------------------------------------------

    @Test
    public void testPublishedInfoWorkspacePropertyRemapped() {
        PermsResult permsResult = cqlPermsResult("workspace = 'myws'");
        Filter result = helper.buildCatalogFilter(permsResult, PublishedInfo.class);
        PropertyIsEqualTo eq = assertPropertyIsEqualTo(result);
        assertEquals(
                "workspace property for PublishedInfo should be remapped to 'resource.store.workspace.name'",
                "resource.store.workspace.name",
                ((PropertyName) eq.getExpression1()).getPropertyName());
    }

    @Test
    public void testPublishedInfoLayerPropertyRemapped() {
        PermsResult permsResult = cqlPermsResult("layer = 'myLayer'");
        Filter result = helper.buildCatalogFilter(permsResult, PublishedInfo.class);
        PropertyIsEqualTo eq = assertPropertyIsEqualTo(result);
        assertEquals(
                "layer property for PublishedInfo should be remapped to 'name'",
                "name",
                ((PropertyName) eq.getExpression1()).getPropertyName());
    }

    // -----------------------------------------------------------------------
    // Unhandled type
    // -----------------------------------------------------------------------

    @Test
    public void testUnhandledCatalogTypeExcluded() {
        PermsResult permsResult = cqlPermsResult("INCLUDE");
        // CatalogInfo is not handled by any branch in buildCatalogFilter
        Filter result = helper.buildCatalogFilter(permsResult, CatalogInfo.class);
        assertEquals(
                "Unhandled CatalogInfo subtype should return Filter.EXCLUDE",
                Filter.EXCLUDE,
                result);
    }

    // -----------------------------------------------------------------------
    // Invalid CQL filter
    // -----------------------------------------------------------------------

    @Test
    public void testInvalidCqlFilterExcluded() {
        PermsResult permsResult = cqlPermsResult("THIS IS NOT VALID CQL !!!@#$");
        Filter result = helper.buildCatalogFilter(permsResult, LayerInfo.class);
        assertEquals(
                "An invalid CQL filter in PermsResult should produce Filter.EXCLUDE",
                Filter.EXCLUDE,
                result);
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private static PermsResult emptyPermsResult() {
        PermsResult pr = new PermsResult();
        pr.setCqlFilter("INCLUDE");
        return pr;
    }

    private static PermsResult permsResultWithResources(String... resources) {
        PermsResult pr = new PermsResult();
        pr.setCqlFilter("INCLUDE");
        TreeSet<String> set = new TreeSet<>();
        for (String r : resources) {
            set.add(r);
        }
        pr.setAccessibleResources(set);
        return pr;
    }

    private static PermsResult cqlPermsResult(String cqlFilter) {
        PermsResult pr = new PermsResult();
        pr.setCqlFilter(cqlFilter);
        return pr;
    }

    private static PropertyIsEqualTo assertPropertyIsEqualTo(Filter filter) {
        if (!(filter instanceof PropertyIsEqualTo)) {
            throw new AssertionError("Expected PropertyIsEqualTo, got: " + filter.getClass().getSimpleName());
        }
        return (PropertyIsEqualTo) filter;
    }

    private static Or assertOr(Filter filter) {
        if (!(filter instanceof Or)) {
            throw new AssertionError("Expected Or filter, got: " + filter.getClass().getSimpleName());
        }
        return (Or) filter;
    }
}
