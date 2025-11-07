/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import static org.geoserver.acl.authorization.WorkspaceAccessSummary.ANY;
import static org.geoserver.acl.authorization.WorkspaceAccessSummary.NO_WORKSPACE;
import static org.geoserver.catalog.Predicates.acceptAll;
import static org.geoserver.catalog.Predicates.acceptNone;
import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.catalog.Predicates.in;
import static org.geoserver.catalog.Predicates.isInstanceOf;
import static org.geoserver.catalog.Predicates.isNull;
import static org.geoserver.catalog.Predicates.not;
import static org.geoserver.catalog.Predicates.notEqual;
import static org.geoserver.catalog.Predicates.or;
import static org.geotools.api.filter.Filter.EXCLUDE;
import static org.geotools.api.filter.Filter.INCLUDE;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.acl.authorization.AccessSummary;
import org.geoserver.acl.authorization.WorkspaceAccessSummary;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.filter.Filter;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Builds catalog security filters based on ACL {@link AccessSummary access summaries}.
 *
 * <p>This class constructs {@link Filter} objects that restrict catalog queries to only return catalog items
 * (workspaces, layers, styles, etc.) that the current user has permission to view according to ACL rules. The filters
 * are optimized to handle various scenarios including:
 *
 * <ul>
 *   <li>Workspace-level access control
 *   <li>Layer-level access control with allow/deny lists
 *   <li>Global vs. workspace-specific styles
 *   <li>Layer groups with null or specific workspaces
 * </ul>
 *
 * <p>The generated filters are simplified using {@link SimplifyingFilterVisitor} to optimize query performance.
 *
 * @author Gabriel Roldan - Camptocamp
 */
class CatalogSecurityFilterBuilder {

    private final AccessSummary viewables;

    public CatalogSecurityFilterBuilder(AccessSummary viewables) {
        this.viewables = Objects.requireNonNull(viewables);
    }

    /**
     * Convenience method to build a security filter for a specific catalog info type.
     *
     * @param viewables the access summary containing allowed workspaces, layers, and restrictions
     * @param infoType the type of catalog info to build a filter for
     * @return a filter that restricts queries to viewable items of the specified type
     */
    public static Filter buildSecurityFilter(AccessSummary viewables, Class<? extends CatalogInfo> infoType) {
        return new CatalogSecurityFilterBuilder(viewables).build(infoType);
    }

    /**
     * Builds a security filter for the specified catalog info type.
     *
     * <p>The filter construction is type-specific:
     *
     * <ul>
     *   <li>{@link WorkspaceInfo} - filters by workspace name
     *   <li>{@link NamespaceInfo} - filters by namespace prefix (mapped to workspace)
     *   <li>{@link StoreInfo} - filters by workspace name of the store
     *   <li>{@link ResourceInfo} - filters by workspace and layer name with allow/deny rules
     *   <li>{@link PublishedInfo} - delegates to layer or layer group filters
     *   <li>{@link StyleInfo} - filters by workspace, including global styles (null workspace)
     * </ul>
     *
     * @param clazz the catalog info class to build a filter for
     * @return a filter restricting queries to viewable items, or {@link Filter#EXCLUDE} if no access
     * @throws UnsupportedOperationException if the catalog info type is not supported
     */
    @SuppressWarnings("unchecked")
    public Filter build(Class<? extends CatalogInfo> clazz) {
        Objects.requireNonNull(clazz);
        if (viewables.getWorkspaces().isEmpty()) {
            return EXCLUDE;
        }
        if (WorkspaceInfo.class.isAssignableFrom(clazz)) {
            return workspaceNameFilter("name", false);
        }
        if (NamespaceInfo.class.isAssignableFrom(clazz)) {
            return workspaceNameFilter("prefix", false);
        }
        if (StoreInfo.class.isAssignableFrom(clazz)) {
            return workspaceNameFilter("workspace.name", false);
        }
        if (ResourceInfo.class.isAssignableFrom(clazz)) {
            return layerFilter("store.workspace.name", "name", ResourceInfo.class);
        }
        if (PublishedInfo.class.isAssignableFrom(clazz)) {
            return publishedInfoFilter((Class<? extends PublishedInfo>) clazz);
        }
        if (StyleInfo.class.isAssignableFrom(clazz)) {
            return styleFilter();
        }
        throw new UnsupportedOperationException("Unknown CatalogInfo type: " + clazz.getCanonicalName());
    }

    /**
     * Builds a filter for styles, including global styles (null workspace).
     *
     * @return a filter that includes workspace-specific and global styles
     */
    private Filter styleFilter() {
        return workspaceNameFilter("workspace.name", true);
    }

    /**
     * Builds a filter for published info (layers and layer groups).
     *
     * <p>This method handles both {@link LayerInfo} and {@link LayerGroupInfo}. If the specific type is requested, it
     * returns the appropriate filter. For the base {@link PublishedInfo} type, it combines both layer and layer group
     * filters with OR logic.
     *
     * @param clazz the published info class (LayerInfo, LayerGroupInfo, or PublishedInfo)
     * @return a filter for viewable published resources
     */
    private Filter publishedInfoFilter(Class<? extends PublishedInfo> clazz) {
        if (LayerInfo.class.isAssignableFrom(clazz)) {
            return layerFilter("resource.store.workspace.name", "name", LayerInfo.class);
        }
        if (LayerGroupInfo.class.isAssignableFrom(clazz)) {
            return layerFilter("workspace.name", "name", LayerGroupInfo.class);
        }
        Filter layerInfoFilter = build(LayerInfo.class);
        Filter layerGroupInfoFilter = build(LayerGroupInfo.class);
        if (INCLUDE.equals(layerInfoFilter) && INCLUDE.equals(layerGroupInfoFilter)) {
            return INCLUDE;
        }
        Filter layerFilter = instanceOfAnd(LayerInfo.class, layerInfoFilter);
        Filter groupFilter = instanceOfAnd(LayerGroupInfo.class, layerGroupInfoFilter);

        if (EXCLUDE.equals(layerInfoFilter)) {
            return groupFilter;
        } else if (EXCLUDE.equals(layerGroupInfoFilter)) {
            return layerFilter;
        }

        return or(layerFilter, groupFilter);
    }

    /**
     * Combines an instanceof check with another filter using AND logic.
     *
     * @param typeOf the class to check instanceof
     * @param andThen the filter to combine with the instanceof check
     * @return {@link Filter#EXCLUDE} if andThen is EXCLUDE, otherwise instanceof AND andThen
     */
    private Filter instanceOfAnd(Class<?> typeOf, Filter andThen) {
        if (EXCLUDE.equals(andThen)) return andThen;
        return and(isInstanceOf(typeOf), andThen);
    }

    /**
     * Builds a filter for layer-like resources (layers, resources, layer groups) with workspace and name filtering.
     *
     * <p>This method processes all workspace access summaries and combines them into a single filter. It handles:
     *
     * <ul>
     *   <li>Workspaces marked as "hide all" - explicitly excluded from results
     *   <li>Workspaces with specific allowed/forbidden layer lists
     *   <li>Null workspace handling for layer groups
     * </ul>
     *
     * @param workspaceProperty the property path to the workspace name
     * @param nameProperty the property path to the layer/resource name
     * @param type the catalog info type being filtered
     * @return a simplified filter combining all workspace and layer access rules
     */
    private Filter layerFilter(String workspaceProperty, String nameProperty, Class<? extends CatalogInfo> type) {
        List<WorkspaceAccessSummary> summaries = viewables.getWorkspaces();

        Filter filter = acceptNone();
        Set<String> hideAllWorkspaceNames = new TreeSet<>();
        for (WorkspaceAccessSummary wsSummary : summaries) {
            String workspace = wsSummary.getWorkspace();
            if (wsSummary.hideAll()) {
                if (!ANY.equals(workspace)) {
                    hideAllWorkspaceNames.add(workspace);
                }
            } else {
                boolean isNullWorkspace = NO_WORKSPACE.equals(workspace);
                boolean supportsNullWorkspace = LayerGroupInfo.class.equals(type);
                // ignore if workspace is null and type is LayerInfo or ResourceInfo
                if (!isNullWorkspace || supportsNullWorkspace) {
                    Filter wsLayersFitler =
                            filterLayersOnWorkspace(wsSummary, workspaceProperty, supportsNullWorkspace, nameProperty);

                    if (EXCLUDE.equals(filter)) {
                        filter = wsLayersFitler;
                    } else {
                        filter = or(filter, wsLayersFitler);
                    }
                }
            }
        }
        filter = prependHideAllWorkspaces(filter, workspaceProperty, hideAllWorkspaceNames);
        return SimplifyingFilterVisitor.simplify(filter);
    }

    /**
     * Prepends workspace exclusions to an existing filter.
     *
     * <p>Workspaces marked as "hide all" are explicitly excluded from the filter results by prepending a NOT IN filter
     * for those workspace names.
     *
     * @param filter the existing filter to prepend to
     * @param workspaceProperty the property path to the workspace name
     * @param hideAllWorkspaceNames the set of workspace names to exclude
     * @return the combined filter with workspace exclusions prepended
     */
    private Filter prependHideAllWorkspaces(
            Filter filter, String workspaceProperty, Set<String> hideAllWorkspaceNames) {
        if (hideAllWorkspaceNames.isEmpty()) {
            return filter;
        }
        Filter hiddenWorkspaces = denyWorkspacesFilter(workspaceProperty, hideAllWorkspaceNames);
        if (INCLUDE.equals(filter)) {
            return hiddenWorkspaces;
        }
        return and(hiddenWorkspaces, filter);
    }

    /**
     * Creates a filter that denies access to specified workspaces.
     *
     * @param workspaceProperty the property path to the workspace name
     * @param hideAllWorkspaceNames the set of workspace names to deny (must not be empty)
     * @return a filter that excludes the specified workspaces
     */
    private Filter denyWorkspacesFilter(String workspaceProperty, Set<String> hideAllWorkspaceNames) {
        Assert.isTrue(!hideAllWorkspaceNames.isEmpty(), "hidden workspace names can't be empty");
        return notEqualOrIn(workspaceProperty, hideAllWorkspaceNames, EXCLUDE);
    }

    /**
     * Creates a filter for layers within a specific workspace based on the workspace access summary.
     *
     * <p>Combines workspace filtering with layer-level allow/deny rules. If the workspace has no specific layer
     * restrictions, only the workspace filter is applied.
     *
     * @param vl the workspace access summary containing allowed and forbidden layer lists
     * @param workspaceProperty the property path to the workspace name
     * @param includeNullWorkspace whether to include items with null workspace
     * @param nameProperty the property path to the layer name
     * @return a filter combining workspace and layer-level restrictions
     */
    @NonNull
    private Filter filterLayersOnWorkspace(
            WorkspaceAccessSummary vl, String workspaceProperty, boolean includeNullWorkspace, String nameProperty) {

        final String workspace = vl.getWorkspace();
        final Set<String> allowed = vl.getAllowed();
        final Set<String> forbidden = vl.getForbidden();

        Filter workspaceFilter = workspaceNameFilter(workspaceProperty, includeNullWorkspace, Set.of(workspace));
        Filter filter;
        if (allowed.isEmpty() && forbidden.isEmpty()) {
            filter = workspaceFilter;
        } else {
            Filter layerFilter = mergeLayers(nameProperty, allowed, forbidden);
            filter = and(workspaceFilter, layerFilter);
        }
        return filter;
    }

    /**
     * Merges allowed and forbidden layer lists into a single filter.
     *
     * <p>The logic prioritizes the allowed list. If both lists are present, the allowed list takes precedence. If only
     * forbidden is specified, a NOT IN filter is created.
     *
     * @param nameProperty the property path to the layer name
     * @param allowed the set of explicitly allowed layer names
     * @param forbidden the set of explicitly forbidden layer names
     * @return a filter combining allow and deny rules
     */
    private Filter mergeLayers(String nameProperty, Set<String> allowed, Set<String> forbidden) {
        Filter allowFilter = equalOrIn(nameProperty, allowed, INCLUDE);
        Filter hideFilter = notEqualOrIn(nameProperty, forbidden, EXCLUDE);
        if (INCLUDE.equals(hideFilter)) {
            return allowFilter;
        }
        if (INCLUDE.equals(allowFilter)) {
            return hideFilter;
        }
        // neither is include, conflates to the allow filter
        return allowFilter;
    }

    /**
     * Creates a NOT EQUAL or NOT IN filter based on the set size.
     *
     * @param nameProperty the property to compare
     * @param names the set of values to exclude
     * @param defaultIfAny the default filter to return if names contains the wildcard "*"
     * @return {@link Filter#INCLUDE} if {@code names} is empty, {@code defaultIfAny} if names contains "*",
     *     {@code not(equal)} for single value, {@code not(in)} for multiple values
     */
    private Filter notEqualOrIn(String nameProperty, Set<String> names, Filter defaultIfAny) {
        if (names.isEmpty()) return INCLUDE;
        if (names.contains(ANY)) return defaultIfAny;

        if (names.size() == 1) {
            return notEqual(nameProperty, names.iterator().next());
        }
        return not(equalOrIn(nameProperty, names, /* has no effect */ defaultIfAny));
    }

    /**
     * Creates an EQUAL or IN filter based on the set size.
     *
     * <p>Optimizes the filter based on the number of values:
     *
     * <ul>
     *   <li>Empty set -> {@link Filter#INCLUDE}
     *   <li>Contains "*" wildcard -> {@code defaultIfAny}
     *   <li>Single value -> {@code name = value}
     *   <li>Multiple values -> {@code name IN (values)}
     * </ul>
     *
     * @param nameProperty the property to compare
     * @param names the set of values to match
     * @param defaultIfAny the default filter to return if names contains the wildcard "*"
     * @return an optimized filter based on the set size and contents
     */
    private Filter equalOrIn(String nameProperty, Set<String> names, Filter defaultIfAny) {
        if (names.isEmpty()) return INCLUDE;
        if (names.contains(ANY)) return defaultIfAny;
        if (names.size() == 1) {
            return equal(nameProperty, names.iterator().next());
        }
        return in(nameProperty, List.copyOf(names));
    }

    /**
     * Returns the set of workspace names that are visible to the current user.
     *
     * @return set of visible workspace names
     */
    private Set<String> getVisibleWorkspaces() {
        return viewables.visibleWorkspaces();
    }

    /**
     * Creates a workspace name filter for all visible workspaces.
     *
     * <p>Optionally includes items with null workspace (e.g., global styles, global layer groups).
     *
     * @param workspaceProperty the property path to the workspace name
     * @param includeNullWorkspace whether to include items with null workspace
     * @return a filter matching visible workspaces
     */
    private Filter workspaceNameFilter(String workspaceProperty, boolean includeNullWorkspace) {
        Set<String> visibleWorkspaces = getVisibleWorkspaces();
        if (includeNullWorkspace && viewables.workspace(NO_WORKSPACE) != null) {
            visibleWorkspaces = new TreeSet<>(visibleWorkspaces);
            visibleWorkspaces.add(NO_WORKSPACE);
        }
        return workspaceNameFilter(workspaceProperty, includeNullWorkspace, visibleWorkspaces);
    }

    /**
     * Creates a workspace name filter for the specified set of workspaces.
     *
     * <p>Handles special cases:
     *
     * <ul>
     *   <li>Wildcard "*" -> {@link Filter#INCLUDE} (all workspaces allowed)
     *   <li>NO_WORKSPACE constant -> filters for null workspace if includeNullWorkspace is true
     *   <li>Single workspace -> optimized EQUAL filter
     *   <li>Multiple workspaces -> IN filter
     * </ul>
     *
     * @param workspaceProperty the property path to the workspace name
     * @param includeNullWorkspace whether to include items with null workspace
     * @param visibleWorkspaces the set of visible workspace names
     * @return a filter matching the specified workspaces
     */
    private Filter workspaceNameFilter(
            String workspaceProperty, boolean includeNullWorkspace, Set<String> visibleWorkspaces) {
        if (visibleWorkspaces.contains(ANY)) {
            return acceptAll();
        }
        Filter filter = acceptNone();
        if (visibleWorkspaces.contains(NO_WORKSPACE)) {
            if (includeNullWorkspace) {
                filter = isNull(workspaceProperty);
            }
            visibleWorkspaces = new TreeSet<>(visibleWorkspaces);
            visibleWorkspaces.remove(NO_WORKSPACE);
        }
        if (!visibleWorkspaces.isEmpty()) {
            List<String> workspaces = List.copyOf(visibleWorkspaces);
            Filter namesFilter;
            if (workspaces.size() == 1) {
                namesFilter = equal(workspaceProperty, workspaces.get(0));
            } else {
                namesFilter = in(workspaceProperty, workspaces);
            }
            if (EXCLUDE.equals(filter)) {
                filter = namesFilter;
            } else {
                filter = or(filter, namesFilter);
            }
        }
        return filter;
    }
}
