/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geotools.api.filter.Filter;

/**
 * Computes the transitive dependency-closure of a workspace-filtered backup subset.
 *
 * <p>The {@link org.geoserver.backuprestore.processor.CatalogItemProcessor workspace cascade} keeps the objects that
 * live <em>inside</em> the filtered workspaces. This class returns the extra ids that must be force-included to make
 * the archive self-contained: the catalog objects <em>outside</em> the filtered workspaces that the subset references
 * (cross-workspace layergroup members and, transitively, their stores / resources / workspaces / namespaces / styles),
 * plus the global (workspace-less) styles and layergroups the subset actually uses.
 *
 * <p>Because a workspace-less style or layergroup that is <em>absent</em> from this set is pruned by the cascade, the
 * closure doubles as the rule that keeps shared objects the subset does not reference out of the archive.
 *
 * <p>The walk reads the live catalog with simple getters and is defensive against nulls / unresolved references, so it
 * never throws; a malformed object is skipped rather than failing the backup.
 */
public final class SubsetClosure {

    private SubsetClosure() {}

    /**
     * @param catalog the live catalog being backed up
     * @param wsFilter the workspace filter (ECQL evaluated against {@link WorkspaceInfo}); {@code null} matches all
     * @param siFilter the store filter (unused for closure seeding; stores follow their layers)
     * @param liFilter the layer filter; only subset layers that match it seed the closure
     * @return the ids of the out-of-subset / global objects the subset depends on (never {@code null})
     */
    public static Set<String> compute(Catalog catalog, Filter wsFilter, Filter siFilter, Filter liFilter) {
        if (catalog == null) {
            return Collections.emptySet();
        }
        Set<String> subsetWorkspaces = new HashSet<>();
        for (WorkspaceInfo ws : catalog.getWorkspaces()) {
            if (ws != null && ws.getName() != null && (wsFilter == null || wsFilter.evaluate(ws))) {
                subsetWorkspaces.add(ws.getName());
            }
        }
        if (subsetWorkspaces.isEmpty()) {
            return Collections.emptySet();
        }

        Deque<CatalogInfo> work = new ArrayDeque<>();
        // Seed with the subset's own publishables, then let the fixpoint pull their dependencies in.
        for (LayerInfo layer : catalog.getLayers()) {
            WorkspaceInfo ws = workspaceOf(catalog, layer);
            if (ws != null
                    && subsetWorkspaces.contains(ws.getName())
                    && (liFilter == null || liFilter.evaluate(layer))) {
                work.add(layer);
            }
        }
        for (LayerGroupInfo lg : catalog.getLayerGroups()) {
            WorkspaceInfo ws = lg.getWorkspace();
            if (ws != null) {
                if (subsetWorkspaces.contains(ws.getName())) {
                    work.add(lg);
                }
            } else if (referencesSubset(catalog, lg, subsetWorkspaces)) {
                // a global layergroup is part of the subset only if it actually groups subset content
                work.add(lg);
            }
        }

        Set<String> visited = new HashSet<>();
        Set<String> forced = new HashSet<>();
        while (!work.isEmpty()) {
            CatalogInfo item = work.poll();
            String id = item.getId();
            if (id != null && !visited.add(id)) {
                continue;
            }
            // an object outside the subset workspaces (cross-workspace, or workspace-less/global) is a forced
            // dependency: the cascade would otherwise drop or prune it
            if (id != null && isOutsideSubset(catalog, item, subsetWorkspaces)) {
                forced.add(id);
            }
            for (CatalogInfo dependency : directDependencies(catalog, item)) {
                if (dependency != null && dependency.getId() != null && !visited.contains(dependency.getId())) {
                    work.add(dependency);
                }
            }
        }
        return forced;
    }

    /** Whether a global layergroup references at least one publishable that resolves into the subset workspaces. */
    private static boolean referencesSubset(Catalog catalog, LayerGroupInfo lg, Set<String> subsetWorkspaces) {
        for (PublishedInfo published : safe(lg.getLayers())) {
            WorkspaceInfo ws = workspaceOf(catalog, published);
            if (ws != null && subsetWorkspaces.contains(ws.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOutsideSubset(Catalog catalog, CatalogInfo item, Set<String> subsetWorkspaces) {
        WorkspaceInfo ws = workspaceOf(catalog, item);
        return ws == null || ws.getName() == null || !subsetWorkspaces.contains(ws.getName());
    }

    /** The catalog objects directly referenced by {@code item} (styles, members, store, resource, workspace, ns). */
    private static List<CatalogInfo> directDependencies(Catalog catalog, CatalogInfo item) {
        java.util.ArrayList<CatalogInfo> deps = new java.util.ArrayList<>();
        if (item instanceof LayerInfo layer) {
            deps.add(layer.getDefaultStyle());
            deps.addAll(safe(layer.getStyles()));
            deps.add(layer.getResource());
        } else if (item instanceof ResourceInfo resource) {
            deps.add(resource.getStore());
            deps.add(resource.getNamespace());
        } else if (item instanceof StoreInfo store) {
            deps.add(store.getWorkspace());
        } else if (item instanceof StyleInfo style) {
            deps.add(style.getWorkspace());
        } else if (item instanceof WorkspaceInfo ws) {
            deps.add(ws.getName() == null ? null : catalog.getNamespaceByPrefix(ws.getName()));
        } else if (item instanceof LayerGroupInfo lg) {
            deps.add(lg.getWorkspace());
            deps.add(lg.getRootLayer());
            deps.add(lg.getRootLayerStyle());
            deps.addAll(safe(lg.getLayers()));
            deps.addAll(safe(lg.getStyles()));
            for (LayerGroupStyle lgStyle : safe(lg.getLayerGroupStyles())) {
                deps.addAll(safe(lgStyle.getLayers()));
                deps.addAll(safe(lgStyle.getStyles()));
            }
        }
        // NamespaceInfo has no further catalog dependencies.
        return deps;
    }

    /** Resolves the workspace an object belongs to, or {@code null} for global / unresolvable objects. */
    private static WorkspaceInfo workspaceOf(Catalog catalog, CatalogInfo item) {
        if (item instanceof WorkspaceInfo ws) {
            return ws;
        } else if (item instanceof NamespaceInfo ns) {
            return ns.getPrefix() == null ? null : catalog.getWorkspaceByName(ns.getPrefix());
        } else if (item instanceof StoreInfo store) {
            return store.getWorkspace();
        } else if (item instanceof ResourceInfo resource) {
            StoreInfo store = resource.getStore();
            return store == null ? null : store.getWorkspace();
        } else if (item instanceof LayerInfo layer) {
            ResourceInfo resource = layer.getResource();
            StoreInfo store = resource == null ? null : resource.getStore();
            return store == null ? null : store.getWorkspace();
        } else if (item instanceof LayerGroupInfo lg) {
            return lg.getWorkspace();
        } else if (item instanceof StyleInfo style) {
            return style.getWorkspace();
        }
        return null;
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private static Set<StyleInfo> safe(Set<StyleInfo> set) {
        return set == null ? Collections.emptySet() : set;
    }
}
