/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import static org.geoserver.acl.authorization.AccessSummary.of;
import static org.geoserver.acl.plugin.accessmanager.CatalogSecurityFilterBuilder.buildSecurityFilter;
import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.catalog.Predicates.in;
import static org.geoserver.catalog.Predicates.isInstanceOf;
import static org.geoserver.catalog.Predicates.isNull;
import static org.geoserver.catalog.Predicates.not;
import static org.geoserver.catalog.Predicates.notEqual;
import static org.geoserver.catalog.Predicates.or;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.geoserver.acl.authorization.AccessSummary;
import org.geoserver.acl.authorization.WorkspaceAccessSummary;
import org.geoserver.acl.domain.adminrules.AdminGrantType;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.filter.Filter;
import org.junit.Test;

public class CatalogSecurityFilterBuilderTest {

    private final WorkspaceAccessSummary ALLOW_ALL = workspace("*", "*");
    private final WorkspaceAccessSummary HIDE_ALL = workspace("*", Set.of(), Set.of("*"));

    @Test
    public void unknownCatalogInfoArgument() {
        AccessSummary accessSummary = of(workspace("cite"));
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class, () -> buildSecurityFilter(accessSummary, CatalogInfo.class));
        assertThat(ex.getMessage(), containsString("Unknown CatalogInfo type"));
    }

    @Test
    public void emptyAccessSummary() {
        AccessSummary accessSummary = AccessSummary.of(List.of());
        assertForAllInterfaces(accessSummary, Filter.EXCLUDE);
    }

    @Test
    public void hideAll() {
        AccessSummary accessSummary = of(HIDE_ALL);
        assertForAllInterfaces(accessSummary, Filter.EXCLUDE);
    }

    @Test
    public void allowAll() {
        AccessSummary accessSummary = of(ALLOW_ALL);
        assertForAllInterfaces(accessSummary, Filter.INCLUDE);
    }

    @Test
    public void manyWorkspacesAndAllowAll() {
        AccessSummary accessSummary = of(workspace("ne", "l1"), workspace("cite", "*"), ALLOW_ALL);
        assertForAllInterfaces(accessSummary, Filter.INCLUDE);
    }

    @Test
    public void manyWorkspacesAndDenyAll() {
        WorkspaceAccessSummary allRootLayerGroups = workspace(WorkspaceAccessSummary.NO_WORKSPACE, "*");
        AccessSummary summary = of(
                workspace("ne", "l1"),
                workspace("cite", "*"),
                HIDE_ALL,
                workspace("topp", "states"),
                allRootLayerGroups);

        List<String> conflatedVisibles = List.copyOf(summary.visibleWorkspaces());
        // excludes the no workspace and all workspace markers
        assertEquals(List.of("cite", "ne", "topp"), conflatedVisibles);

        assertBuildFilter(WorkspaceInfo.class, summary, in("name", conflatedVisibles));
        assertBuildFilter(NamespaceInfo.class, summary, in("prefix", conflatedVisibles));
        assertBuildFilter(StoreInfo.class, summary, in("workspace.name", conflatedVisibles));
        assertBuildFilter(
                StyleInfo.class, summary, or(isNull("workspace.name"), in("workspace.name", conflatedVisibles)));

        assertBuildFilter(
                ResourceInfo.class,
                summary,
                or(
                        equal("store.workspace.name", "cite"),
                        and(equal("store.workspace.name", "ne"), equal("name", "l1")),
                        and(equal("store.workspace.name", "topp"), equal("name", "states"))));

        assertBuildFilter(
                LayerInfo.class,
                summary,
                or(
                        equal("resource.store.workspace.name", "cite"),
                        and(equal("resource.store.workspace.name", "ne"), equal("name", "l1")),
                        and(equal("resource.store.workspace.name", "topp"), equal("name", "states"))));
    }

    private void assertForAllInterfaces(AccessSummary accessSummary, Filter expected) {
        Stream.of(
                        WorkspaceInfo.class,
                        NamespaceInfo.class,
                        StoreInfo.class,
                        ResourceInfo.class,
                        StyleInfo.class,
                        LayerInfo.class,
                        LayerGroupInfo.class,
                        PublishedInfo.class)
                .forEach(type -> assertBuildFilter(type, accessSummary, expected));
    }

    private void assertBuildFilter(Class<? extends CatalogInfo> type, AccessSummary accessSummary, Filter expected) {
        assertEquals(
                String.format("Mismatch for %s with %s", type.getSimpleName(), accessSummary),
                expected,
                buildSecurityFilter(accessSummary, type));
    }

    @Test
    public void workspaceInfoFilter() {
        AccessSummary accessSummary = of(workspace("cite"));
        Filter filter = buildSecurityFilter(accessSummary, WorkspaceInfo.class);
        Filter expected = equal("name", "cite");
        assertEquals(expected, filter);
    }

    @Test
    public void workspaceInfoFilterMany() {
        AccessSummary accessSummary = of(workspace("cite"), workspace("ne"), workspace("topp"));
        Filter filter = buildSecurityFilter(accessSummary, WorkspaceInfo.class);
        Filter expected = in("name", List.of("cite", "ne", "topp"));
        assertEquals(expected, filter);
    }

    @Test
    public void workspaceInfoFilterManyContainsWildcard() {
        AccessSummary accessSummary = of(
                workspace("*"), builder("ne").adminAccess(AdminGrantType.ADMIN).build());
        Filter filter = buildSecurityFilter(accessSummary, WorkspaceInfo.class);
        Filter expected = Filter.INCLUDE;
        assertEquals(expected, filter);
    }

    @Test
    public void namespaceInfoFilter() {
        AccessSummary accessSummary = of(workspace("cite"));
        Filter filter = buildSecurityFilter(accessSummary, NamespaceInfo.class);
        Filter expected = equal("prefix", "cite");
        assertEquals(expected, filter);
    }

    @Test
    public void namespaceInfoFilterMany() {
        AccessSummary accessSummary = of(workspace("cite"), workspace("ne"), workspace("topp"));
        Filter filter = buildSecurityFilter(accessSummary, NamespaceInfo.class);
        Filter expected = in("prefix", List.of("cite", "ne", "topp"));
        assertEquals(expected, filter);
    }

    @Test
    public void namespaceInfoFilterManyAndHideAll() {
        AccessSummary accessSummary = of(HIDE_ALL, workspace("cite"), workspace("ne"), workspace("topp"));
        Filter filter = buildSecurityFilter(accessSummary, NamespaceInfo.class);
        Filter expected = in("prefix", List.of("cite", "ne", "topp"));
        assertEquals(expected, filter);
    }

    @Test
    public void namespaceInfoFilterManyAndAllowAll() {
        AccessSummary accessSummary = of(ALLOW_ALL, workspace("cite"), workspace("ne"), workspace("topp"));
        Filter filter = buildSecurityFilter(accessSummary, NamespaceInfo.class);
        Filter expected = Filter.INCLUDE;
        assertEquals(expected, filter);
    }

    @Test
    public void storeInfoFilter() {
        AccessSummary accessSummary = of(workspace("cite"));
        Filter expected = equal("workspace.name", "cite");
        List.of(StoreInfo.class, DataStoreInfo.class, CoverageStoreInfo.class, WMSStoreInfo.class, WMTSStoreInfo.class)
                .forEach(type -> assertEquals(expected, buildSecurityFilter(accessSummary, type)));
    }

    @Test
    public void storeInfoFilterMany() {
        AccessSummary accessSummary = of(workspace("cite"), workspace("ne"), workspace("topp"));
        Filter expected = in("workspace.name", List.of("cite", "ne", "topp"));
        List.of(StoreInfo.class, DataStoreInfo.class, CoverageStoreInfo.class, WMSStoreInfo.class, WMTSStoreInfo.class)
                .forEach(type -> assertEquals(expected, buildSecurityFilter(accessSummary, type)));
    }

    @Test
    public void resourceInfoFilterWhenWorkspaceSummaryHasNoLayers() {
        AccessSummary accessSummary = of(workspace("cite"));
        Filter expected = equal("store.workspace.name", "cite");
        List.of(ResourceInfo.class, FeatureTypeInfo.class, CoverageInfo.class, WMSLayerInfo.class, WMTSLayerInfo.class)
                .forEach(type -> assertEquals(expected, buildSecurityFilter(accessSummary, type)));
    }

    @Test
    public void resourceInfoFilterManyWhenWorkspaceSummaryHasNoLayers() {
        AccessSummary accessSummary = of(workspace("cite"), workspace("ne"), workspace("topp"));
        Filter expected = or(
                equal("store.workspace.name", "cite"),
                equal("store.workspace.name", "ne"),
                equal("store.workspace.name", "topp"));
        List.of(ResourceInfo.class, FeatureTypeInfo.class, CoverageInfo.class, WMSLayerInfo.class, WMTSLayerInfo.class)
                .forEach(type -> assertEquals(expected, buildSecurityFilter(accessSummary, type)));
    }

    @Test
    public void resourceInfoFilterSingleLayer() {
        AccessSummary accessSummary = of(workspace("cite", "layer1"));
        Filter expected = and(equal("store.workspace.name", "cite"), equal("name", "layer1"));
        List.of(ResourceInfo.class, FeatureTypeInfo.class, CoverageInfo.class, WMSLayerInfo.class, WMTSLayerInfo.class)
                .forEach(type -> assertEquals(expected, buildSecurityFilter(accessSummary, type)));
    }

    @Test
    public void resourceInfoFilterMultipleLayers() {
        AccessSummary accessSummary = of(workspace("cite", "layer1", "layer2"));
        List<String> layers = List.copyOf(accessSummary.workspace("cite").getAllowed());
        Filter expected = and(equal("store.workspace.name", "cite"), in("name", layers));
        assertEquals(expected, buildSecurityFilter(accessSummary, ResourceInfo.class));
    }

    @Test
    public void resourceInfoFilterMultipleWorkspacesMultipleLayers() {
        AccessSummary accessSummary = of( //
                workspace("cite", "layer1", "layer2"), //
                workspace("ne", "layer3", "layer4"), //
                workspace("topp") //
                );

        List<String> citelayers = List.copyOf(accessSummary.workspace("cite").getAllowed());
        Filter citefilter = and(equal("store.workspace.name", "cite"), in("name", citelayers));

        List<String> nelayers = List.copyOf(accessSummary.workspace("ne").getAllowed());
        Filter nefilter = and(equal("store.workspace.name", "ne"), in("name", nelayers));

        Filter toppfilter = equal("store.workspace.name", "topp");

        Filter expected = or(citefilter, nefilter, toppfilter);
        assertEquals(expected, buildSecurityFilter(accessSummary, ResourceInfo.class));
    }

    @Test
    public void resourceInfoFilterMultipleLayersAndWildcard() {
        AccessSummary accessSummary = of(workspace("cite", "layer1", "layer2", "*"));
        Filter expected = equal("store.workspace.name", "cite");
        assertEquals(expected, buildSecurityFilter(accessSummary, ResourceInfo.class));
    }

    @Test
    public void layerInfoFilter() {
        AccessSummary accessSummary = of(workspace("cite", "layer1", "layer2"));
        List<String> layers = List.copyOf(accessSummary.workspace("cite").getAllowed());
        Filter expected = and(equal("resource.store.workspace.name", "cite"), in("name", layers));
        assertEquals(expected, buildSecurityFilter(accessSummary, LayerInfo.class));
    }

    @Test
    public void layerInfoFilterAllVisibleAndSomeHiddenLayers() {
        Set<String> visible = Set.of("*");
        Set<String> hidden = Set.of("hidden1", "hidden2");
        AccessSummary accessSummary = of(workspace("cite", visible, hidden));

        List<String> layers = List.copyOf(accessSummary.workspace("cite").getForbidden());
        assertEquals(hidden, Set.copyOf(layers));

        // conflates to negating the hidden ones only
        Filter expected = and(equal("resource.store.workspace.name", "cite"), not(in("name", layers)));

        Filter actual = buildSecurityFilter(accessSummary, LayerInfo.class);

        assertEquals(expected, actual);
    }

    @Test
    public void layerInfoFilterAllHiddenAndSomeVisibleLayers() {
        Set<String> visible = Set.of("visible1", "visible2");
        Set<String> hidden = Set.of("*");
        AccessSummary accessSummary = of(workspace("cite", visible, hidden));

        List<String> visibleNames = List.copyOf(accessSummary.workspace("cite").getAllowed());
        assertEquals(visible, Set.copyOf(visibleNames));

        // conflates to visibles only
        Filter expected = and(equal("resource.store.workspace.name", "cite"), in("name", visibleNames));
        Filter actual = buildSecurityFilter(accessSummary, LayerInfo.class);
        assertEquals(expected, actual);
    }

    @Test
    public void layerInfoFilterSomeVisibleAndSomeHiddenLayers() {
        Set<String> visible = Set.of("visible1", "visible2");
        Set<String> hidden = Set.of("hidden1", "hidden2");
        AccessSummary accessSummary = of(workspace("cite", visible, hidden));

        List<String> visibleNames = List.copyOf(accessSummary.workspace("cite").getAllowed());
        assertEquals(visible, Set.copyOf(visibleNames));

        // conflates to visibles only
        Filter expected = and(equal("resource.store.workspace.name", "cite"), in("name", visibleNames));

        Filter actual = buildSecurityFilter(accessSummary, LayerInfo.class);

        assertEquals(expected, actual);
    }

    @Test
    public void layerInfoFilterAllowAllButOneWorkspace() {
        AccessSummary accessSummary = of(
                // allow all
                ALLOW_ALL,
                // but hide all from cite
                workspace("cite", Set.of(), Set.of("*")));

        // conflates to hidding the cite workspace
        Filter expected = notEqual("resource.store.workspace.name", "cite");

        Filter actual = buildSecurityFilter(accessSummary, LayerInfo.class);
        assertEquals(expected, actual);

        // same thing if the order is reversed
        accessSummary = of(
                // hide all from cite
                workspace("cite", Set.of(), Set.of("*")),
                // allow everything else
                ALLOW_ALL);

        actual = buildSecurityFilter(accessSummary, LayerInfo.class);
        assertEquals(expected, actual);
    }

    @Test
    public void layerInfoFilterAllowAllButSomeWorkspaces() {
        AccessSummary accessSummary = of(
                ALLOW_ALL, // allow all
                // but hide all from cite and ne workspaces
                workspace("cite", Set.of(), Set.of("*")),
                workspace("ne", Set.of(), Set.of("*")));

        // conflates to hidding the cite and ne workspaces
        Filter expected = not(in("resource.store.workspace.name", List.of("cite", "ne")));

        Filter actual = buildSecurityFilter(accessSummary, LayerInfo.class);
        assertEquals(expected, actual);
    }

    @Test
    public void layerInfoFilterMultipleWorkspacesMultipleLayers() {
        AccessSummary accessSummary = of( //
                workspace("cite", "layer1", "layer2"), //
                workspace("ne", "layer3", "layer4"), //
                workspace("topp") //
                );

        List<String> citelayers = List.copyOf(accessSummary.workspace("cite").getAllowed());
        Filter citefilter = and(equal("resource.store.workspace.name", "cite"), in("name", citelayers));

        List<String> nelayers = List.copyOf(accessSummary.workspace("ne").getAllowed());
        Filter nefilter = and(equal("resource.store.workspace.name", "ne"), in("name", nelayers));

        Filter toppfilter = equal("resource.store.workspace.name", "topp");

        Filter expected = or(citefilter, nefilter, toppfilter);
        Filter actual = buildSecurityFilter(accessSummary, LayerInfo.class);
        assertEquals(expected, actual);
    }

    @Test
    public void layerInfoFilterDenyAllButAWorkspace() {
        AccessSummary accessSummary = of(HIDE_ALL, workspace("ne", "neLayer1", "neLayer2"));

        List<String> nelayers = List.copyOf(accessSummary.workspace("ne").getAllowed());

        Filter nefilter = and(equal("resource.store.workspace.name", "ne"), in("name", nelayers));
        Filter expected = nefilter;
        Filter actual = buildSecurityFilter(accessSummary, LayerInfo.class);
        assertEquals(expected, actual);
    }

    @Test
    public void layerGroupInfoFilterNoWorkspace() {
        AccessSummary accessSummary = of(workspace("", "lg1"));
        Filter workspaceFilter = isNull("workspace.name");
        Filter nameFilter = equal("name", "lg1");
        Filter expected = and(workspaceFilter, nameFilter);
        assertEquals(expected, buildSecurityFilter(accessSummary, LayerGroupInfo.class));
    }

    @Test
    public void layerGroupInfoFilterNoWorkspaceAll() {
        AccessSummary accessSummary = of(workspace("", "*"));
        Filter workspaceFilter = isNull("workspace.name");
        assertEquals(workspaceFilter, buildSecurityFilter(accessSummary, LayerGroupInfo.class));
    }

    @Test
    public void layerGroupInfoFilterNoWorkspaceMany() {
        AccessSummary accessSummary = of(workspace("", "lg1", "lg2"));
        List<String> layers = List.copyOf(accessSummary.workspace("").getAllowed());
        Filter workspaceFilter = isNull("workspace.name");
        Filter nameFilter = in("name", layers);
        Filter expected = and(workspaceFilter, nameFilter);
        assertEquals(expected, buildSecurityFilter(accessSummary, LayerGroupInfo.class));
    }

    @Test
    public void publishedInfoInfoFilterOnlyRootLayerGroups() {
        AccessSummary accessSummary = of(workspace("", "lg1", "lg2"));

        List<String> rootLgs = List.copyOf(accessSummary.workspace("").getAllowed());
        Filter rootLgFilters = and(isNull("workspace.name"), in("name", rootLgs));

        Filter expected = and(isInstanceOf(LayerGroupInfo.class), rootLgFilters);
        Filter actual = buildSecurityFilter(accessSummary, PublishedInfo.class);

        // workaround for IsInstanceOf function not implementing equals()
        assertEquals(expected.toString(), actual.toString());
    }

    /**
     *
     *
     * <pre>
     * <code>
     * [
     * 		[
     * 			[ IsInstanceOf(interface org.geoserver.catalog.LayerInfo) = true ]
     * 			AND [ resource.store.workspace.name = ne ] AND [ in([name], [populated_places], [world]) = true ]
     *      ]
     * 		OR
     *      [
     *      	[ IsInstanceOf(interface org.geoserver.catalog.LayerGroupInfo) = true ]
     *          AND [
     *          		[[ workspace.name IS NULL ] AND [ in([name], [lg2], [lg1]) = true ]]
     *          		OR
     *          		[[ workspace.name = ne ] AND [ in([name], [populated_places], [world]) = true ]]
     *          	]
     *      ]
     * ]
     * </code>
     * </pre>
     */
    @Test
    public void publishedInfoInfoFilterMixingRootLayerGroupsAndWorkspaceLayerNames() {
        AccessSummary accessSummary = of(workspace("", "lg1", "lg2"), workspace("ne", "world", "populated_places"));

        List<String> nelayers = List.copyOf(accessSummary.workspace("ne").getAllowed());

        Filter layerInfoFilter = and(
                isInstanceOf(LayerInfo.class), and(equal("resource.store.workspace.name", "ne"), in("name", nelayers)));

        List<String> rootLgs = List.copyOf(accessSummary.workspace("").getAllowed());
        Filter rootLgFilters = and(isNull("workspace.name"), in("name", rootLgs));
        Filter workspaceLgFilters = and(equal("workspace.name", "ne"), in("name", nelayers));

        Filter layerGroupInfoFilter = and(isInstanceOf(LayerGroupInfo.class), or(rootLgFilters, workspaceLgFilters));

        Filter expected = or(layerInfoFilter, layerGroupInfoFilter);
        Filter actual = buildSecurityFilter(accessSummary, PublishedInfo.class);
        // workaround for IsInstanceOf function not implementing equals()
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void publishedInfoInfoFilterMultipleWorkspaces() {
        AccessSummary accessSummary = of(workspace("ne", "world"), workspace("cite", "test"), workspace("topp", "*"));

        Filter layerFilter = and(
                isInstanceOf(LayerInfo.class),
                or(
                        and(equal("resource.store.workspace.name", "ne"), equal("name", "world")),
                        and(equal("resource.store.workspace.name", "cite"), equal("name", "test")),
                        equal("resource.store.workspace.name", "topp")));

        Filter layerGroupFilter = and(
                isInstanceOf(LayerGroupInfo.class),
                or(
                        and(equal("workspace.name", "ne"), equal("name", "world")),
                        and(equal("workspace.name", "cite"), equal("name", "test")),
                        equal("workspace.name", "topp")));

        Filter expected = or(layerFilter, layerGroupFilter);
        Filter actual = buildSecurityFilter(accessSummary, PublishedInfo.class);
        // workaround for IsInstanceOf function not implementing equals()
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void styleWorkspaceNullFilter() {
        AccessSummary accessSummary = of(workspace("", "rootlg"));

        Filter expected = isNull("workspace.name");
        Filter actual = buildSecurityFilter(accessSummary, StyleInfo.class);
        assertEquals(expected, actual);
    }

    @Test
    public void styleWorkspaceFilter() {
        AccessSummary accessSummary = of(workspace("ne", "world"));

        Filter expected = equal("workspace.name", "ne");
        Filter actual = buildSecurityFilter(accessSummary, StyleInfo.class);
        assertEquals(expected, actual);
    }

    @Test
    public void styleMultipleWorkspaceFilter() {
        AccessSummary accessSummary = of(workspace("ne", "world"), workspace("cite", "*"));

        Filter expected = in("workspace.name", List.of("cite", "ne"));
        Filter actual = buildSecurityFilter(accessSummary, StyleInfo.class);
        assertEquals(expected, actual);
    }

    @Test
    public void styleHiddenWorkspaceFilter() {

        AccessSummary accessSummary = of(workspace("cite", Set.of("*"), Set.of("hidden1", "hidden2")));

        Filter expected = equal("workspace.name", "cite");
        Filter actual = buildSecurityFilter(accessSummary, StyleInfo.class);
        assertEquals("hidden layers do not affect workspace visibility for styles", expected, actual);
    }

    @Test
    public void styleMultipleWorkspaceAndNoWorkspaceFilter() {
        AccessSummary accessSummary = of(workspace("ne", "world"), workspace("cite", "*"), workspace("", "rootlg"));

        Filter expected = or(isNull("workspace.name"), in("workspace.name", List.of("cite", "ne")));
        Filter actual = buildSecurityFilter(accessSummary, StyleInfo.class);
        assertEquals(expected, actual);
    }

    private WorkspaceAccessSummary workspace(String workspace, String... visibleLayers) {
        Set<String> allowed = null == visibleLayers ? Set.of() : Set.of(visibleLayers);
        return workspace(workspace, allowed, Set.of());
    }

    private WorkspaceAccessSummary workspace(String workspace, Set<String> visibleLayers, Set<String> hiddenLayers) {
        return builder(workspace).allowed(visibleLayers).forbidden(hiddenLayers).build();
    }

    private WorkspaceAccessSummary.Builder builder(String workspace) {
        // adminGrant is irrelevant to build the filter
        AdminGrantType adminGrant = AdminGrantType.USER;
        return WorkspaceAccessSummary.builder().workspace(workspace).adminAccess(adminGrant);
    }
}
