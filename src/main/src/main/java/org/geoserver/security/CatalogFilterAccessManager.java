/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

/**
 * Filters viewable layers based on the registered CatalogFilter
 *
 * @author Justin Deoliveira, OpenGeo
 * @author David Winslow, OpenGeo
 * @author Andrea Aime, GeoSolutions
 */
public class CatalogFilterAccessManager extends ResourceAccessManagerWrapper {

    private List<? extends CatalogFilter> filters;

    private DataAccessLimits hide(ResourceInfo info) {
        if (info instanceof FeatureTypeInfo) {
            return new VectorAccessLimits(
                    CatalogMode.HIDE, null, Filter.EXCLUDE, null, Filter.EXCLUDE);
        } else if (info instanceof CoverageInfo) {
            return new CoverageAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE, null, null);
        } else if (info instanceof WMSLayerInfo) {
            return new WMSAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE, null, false);
        } else {
            // TODO: Log warning about unknown resource type
            return new DataAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE);
        }
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        if (hideLayer(layer) || hideResource(layer.getResource())) {
            return hide(layer.getResource());
        }
        return super.getAccessLimits(user, layer);
    }

    @Override
    public DataAccessLimits getAccessLimits(
            Authentication user, LayerInfo layer, List<LayerGroupInfo> containers) {
        if (hideLayer(layer) || hideResource(layer.getResource())) {
            return hide(layer.getResource());
        }
        return super.getAccessLimits(user, layer, containers);
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        if (hideResource(resource)) {
            return hide(resource);
        } else {
            return super.getAccessLimits(user, resource);
        }
    }

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        if (hideWorkspace(workspace)) {
            return new WorkspaceAccessLimits(CatalogMode.HIDE, false, false, false);
        } else {
            return super.getAccessLimits(user, workspace);
        }
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        if (hideStyle(style)) {
            return new StyleAccessLimits(CatalogMode.HIDE);
        } else {
            return super.getAccessLimits(user, style);
        }
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        if (hideLayerGroup(layerGroup)) {
            return new LayerGroupAccessLimits(CatalogMode.HIDE);
        }
        return super.getAccessLimits(user, layerGroup);
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(
            Authentication user, LayerGroupInfo layerGroup, List<LayerGroupInfo> containers) {
        if (hideLayerGroup(layerGroup)) {
            return new LayerGroupAccessLimits(CatalogMode.HIDE);
        } else {
            return super.getAccessLimits(user, layerGroup, containers);
        }
    }

    private boolean hideResource(ResourceInfo resource) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideResource(resource)) {
                return true;
            }
        }
        return false;
    }

    private boolean hideLayer(LayerInfo layer) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideLayer(layer)) {
                return true;
            }
        }
        return false;
    }

    private boolean hideWorkspace(WorkspaceInfo workspace) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideWorkspace(workspace)) {
                return true;
            }
        }
        return false;
    }

    private boolean hideStyle(StyleInfo style) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideStyle(style)) {
                return true;
            }
        }
        return false;
    }

    private boolean hideLayerGroup(LayerGroupInfo layerGroup) {
        for (CatalogFilter filter : getCatalogFilters()) {
            if (filter.hideLayerGroup(layerGroup)) {
                return true;
            }
        }
        return false;
    }

    private List<? extends CatalogFilter> getCatalogFilters() {
        if (filters == null) {
            filters = GeoServerExtensions.extensions(CatalogFilter.class);
        }
        return filters;
    }

    /**
     * Designed for testing, allows to manually configure the catalog filters bypassing the Spring
     * context lookup
     */
    public void setCatalogFilters(List<? extends CatalogFilter> filters) {
        this.filters = filters;
    }

    @Override
    public Filter getSecurityFilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        // If there are no CatalogFilters, just get the delegate's filter
        if (filters == null || filters.isEmpty()) return delegate.getSecurityFilter(user, clazz);

        // Result is the conjunction of delegate's filter, and those of all the CatalogFilters
        ArrayList<Filter> convertedFilters = new ArrayList<Filter>(this.filters.size() + 1);
        convertedFilters.add(delegate.getSecurityFilter(user, clazz)); // Delegate's filter

        for (CatalogFilter filter : getCatalogFilters()) {
            convertedFilters.add(filter.getSecurityFilter(clazz)); // Each CatalogFilter's filter
        }
        return Predicates.and(convertedFilters.toArray(new Filter[convertedFilters.size()]));
    }
}
