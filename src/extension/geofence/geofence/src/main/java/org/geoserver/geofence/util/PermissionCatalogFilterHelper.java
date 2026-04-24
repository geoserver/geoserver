/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.util;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.geofence.services.dto.PermsResult;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.util.logging.Logging;

/**
 * Helper class that translates a {@link PermsResult} from GeoFence into a GeoServer catalog pre-filter
 * ({@link Filter}).
 *
 * <p>The CQL filter inside {@link PermsResult} uses the property names {@code workspace} and {@code layer} as produced
 * by the GeoFence rule engine. This helper renames those properties to the appropriate GeoServer catalog property paths
 * depending on the target {@link CatalogInfo} subtype.
 *
 * @author etj
 */
public class PermissionCatalogFilterHelper {

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();
    static final Logger LOGGER = Logging.getLogger(PermissionCatalogFilterHelper.class);

    /** Property name used for workspace in GeoFence CQL filters. */
    static final String GF_WORKSPACE = "workspace";

    /** Property name used for layer in GeoFence CQL filters. */
    static final String GF_LAYER = "layer";

    /**
     * Builds a GeoServer catalog pre-filter from the given {@link PermsResult}.
     *
     * <p>The filter restricts catalog listings to objects the user has at least read access to. Property names in the
     * result are tailored to the target {@link CatalogInfo} subtype.
     *
     * <p>For {@link WorkspaceInfo}, visibility is derived from the accessible-resources set (since the CQL layer
     * conditions are not meaningful at workspace level). For all layer-level types ({@link LayerInfo},
     * {@link LayerGroupInfo}, {@link ResourceInfo}) the CQL filter provided by GeoFence is parsed and its
     * {@code workspace} / {@code layer} property names are remapped to the correct catalog property paths.
     *
     * @param permsResult the permission result returned by {@code RuleReaderService.getPermissionFilter}
     * @param clazz the target catalog type for which to build the filter
     * @return a GeoTools {@link Filter} suitable for pre-filtering the GeoServer catalog
     */
    public Filter buildCatalogFilter(PermsResult permsResult, Class<? extends CatalogInfo> clazz) {

        LOGGER.log(Level.FINER, "Building GeoFence security pre-filter for {0}", clazz.getName());

        // StyleInfo is not subject to workspace/layer rules in GeoFence
        if (StyleInfo.class.isAssignableFrom(clazz)) {
            return Filter.INCLUDE;
        }

        // WorkspaceInfo visibility is derived from the accessible-resources set
        if (WorkspaceInfo.class.isAssignableFrom(clazz)) {
            return buildWorkspaceFilter(permsResult);
        }

        // For layer-level types use the CQL filter with property renaming
        final String wsProp;
        final String layerProp = "name";
        if (LayerInfo.class.isAssignableFrom(clazz)) {
            wsProp = "resource.store.workspace.name";
        } else if (LayerGroupInfo.class.isAssignableFrom(clazz)) {
            wsProp = "workspace.name";
        } else if (ResourceInfo.class.isAssignableFrom(clazz)) {
            wsProp = "store.workspace.name";
        } else if (PublishedInfo.class.isAssignableFrom(clazz)) {
            wsProp = "resource.store.workspace.name";
        } else {
            LOGGER.log(Level.WARNING, "Unhandled catalog type for GeoFence security pre-filter: {0}", clazz.getName());
            return Filter.EXCLUDE;
        }

        return parseCqlAndRenameProperties(permsResult.getCqlFilter(), wsProp, layerProp);
    }

    /**
     * Derives workspace visibility from the accessible-resources set.
     *
     * <p>A workspace is visible if the user has access to at least one resource in it. Any cross- workspace grant
     * ({@code *:*} for global access, or {@code *:layer} for a specific layer in any workspace) makes all workspaces
     * visible.
     */
    private Filter buildWorkspaceFilter(PermsResult permsResult) {
        SortedSet<String> resources = permsResult.getAccessibleResources();

        if (resources.isEmpty()) {
            return Filter.EXCLUDE;
        }

        // global wildcard or cross-workspace layer grant → all workspaces visible
        if (resources.contains("*:*") || resources.stream().anyMatch(r -> r.startsWith("*:"))) {
            return Filter.INCLUDE;
        }

        // Collect distinct workspace names from "ws:layer" entries
        List<Filter> wsFilters = resources.stream()
                .map(r -> {
                    int colon = r.indexOf(':');
                    return colon > 0 ? r.substring(0, colon) : null;
                })
                .filter(ws -> ws != null && !"*".equals(ws))
                .distinct()
                .map(ws -> FF.equals(FF.property("name"), FF.literal(ws)))
                .collect(Collectors.toList());

        if (wsFilters.isEmpty()) return Filter.EXCLUDE;
        return wsFilters.size() == 1 ? wsFilters.get(0) : FF.or(wsFilters);
    }

    /**
     * Parses the CQL filter string and renames {@code workspace} / {@code layer} property references to the target
     * property paths.
     */
    private Filter parseCqlAndRenameProperties(String cqlFilter, String newWsProp, String newLayerProp) {
        Filter filter;
        try {
            filter = ECQL.toFilter(cqlFilter);
        } catch (CQLException e) {
            LOGGER.log(Level.WARNING, "Failed to parse PermsResult CQL filter '" + cqlFilter + "', denying access", e);
            return Filter.EXCLUDE;
        }

        if (Filter.INCLUDE.equals(filter) || Filter.EXCLUDE.equals(filter)) {
            return filter;
        }

        PropertyNameRemapper remapper =
                new PropertyNameRemapper(Map.of(GF_WORKSPACE, newWsProp, GF_LAYER, newLayerProp));
        return (Filter) filter.accept(remapper, null);
    }

    /** A {@link DuplicatingFilterVisitor} that renames {@link PropertyName} nodes according to a given mapping. */
    static class PropertyNameRemapper extends DuplicatingFilterVisitor {

        private final Map<String, String> nameMap;

        PropertyNameRemapper(Map<String, String> nameMap) {
            this.nameMap = nameMap;
        }

        @Override
        public Object visit(PropertyName expression, Object extraData) {
            String mapped = nameMap.get(expression.getPropertyName());
            return mapped != null ? ff.property(mapped) : super.visit(expression, extraData);
        }
    }
}
