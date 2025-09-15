/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.security.core.Authentication;

/**
 * Abstract class for wrappers around an existing resource access manager.
 *
 * @author David Winslow, OpenGeo
 */
public abstract class ResourceAccessManagerWrapper implements ResourceAccessManager {
    protected ResourceAccessManager delegate;
    private static FilterFactory factory = CommonFactoryFinder.getFilterFactory(null);
    private static GeometryFactory geomFactory = new GeometryFactory();

    protected CatalogMode intersection(CatalogMode a, CatalogMode b) {
        if (a == CatalogMode.HIDE || b == CatalogMode.HIDE) {
            return CatalogMode.HIDE;
        } else if (a == CatalogMode.MIXED || b == CatalogMode.MIXED) {
            return CatalogMode.MIXED;
        } else if (a == CatalogMode.CHALLENGE || b == CatalogMode.CHALLENGE) {
            return CatalogMode.CHALLENGE;
        }

        // TODO: Log error - neither of the modes was a known value!
        return CatalogMode.HIDE;
    }

    protected DataAccessLimits intersection(DataAccessLimits a, DataAccessLimits b) {
        if (a == null) return b;
        if (b == null) return a;

        if (a instanceof VectorAccessLimits limits3 && b instanceof VectorAccessLimits limits3) {
            return intersection(limits3, limits3);
        } else if (a instanceof CoverageAccessLimits limits2 && b instanceof CoverageAccessLimits limits2) {
            return intersection(limits2, limits2);
        } else if (a instanceof WMSAccessLimits limits && b instanceof WMSAccessLimits limits1) {
            return intersection(limits, limits1);
        }

        throw new IllegalArgumentException(
                "Tried to get intersection of differing or unanticipated types of DataAccessLimits ("
                        + a
                        + " && "
                        + b
                        + ")");
    }

    protected VectorAccessLimits intersection(VectorAccessLimits a, VectorAccessLimits b) {
        if (a == null) return b;
        if (b == null) return a;

        CatalogMode mode = intersection(a.getMode(), b.getMode());
        List<PropertyName> readAttributes = intersection(a.getReadAttributes(), b.getReadAttributes());
        Filter readFilter = intersection(a.getReadFilter(), b.getReadFilter());
        List<PropertyName> writeAttributes = intersection(a.getReadAttributes(), b.getReadAttributes());
        Filter writeFilter = intersection(a.getWriteFilter(), b.getWriteFilter());

        return new VectorAccessLimits(mode, readAttributes, readFilter, writeAttributes, writeFilter);
    }

    protected CoverageAccessLimits intersection(CoverageAccessLimits a, CoverageAccessLimits b) {
        if (a == null) return b;
        if (b == null) return a;

        final CatalogMode mode = intersection(a.getMode(), b.getMode());
        final MultiPolygon rasterFilter;

        {
            MultiPolygon aFilter = a.getRasterFilter(), bFilter = b.getRasterFilter();
            if (aFilter == null) rasterFilter = bFilter;
            else if (bFilter == null) rasterFilter = aFilter;
            else {
                Geometry intersection = aFilter.intersection(bFilter);
                if (intersection instanceof MultiPolygon polygon1) {
                    rasterFilter = polygon1;
                } else {
                    final List<Polygon> accum = new ArrayList<>();
                    intersection.apply((GeometryComponentFilter) geom -> {
                        if (geom instanceof Polygon polygon) accum.add(polygon);
                    });

                    rasterFilter = geomFactory.createMultiPolygon(accum.toArray(new Polygon[accum.size()]));
                }
            }
        }

        final Filter readFilter;
        if (rasterFilter != null && rasterFilter.getNumGeometries() == 0) {
            readFilter = Filter.EXCLUDE;
        } else {
            readFilter = intersection(a.getReadFilter(), b.getReadFilter());
        }

        GeneralParameterValue[] params = intersection(a.getParams(), b.getParams());

        return new CoverageAccessLimits(mode, readFilter, rasterFilter, params);
    }

    protected WMSAccessLimits intersection(WMSAccessLimits a, WMSAccessLimits b) {
        if (a == null) return b;
        if (b == null) return a;

        CatalogMode mode = intersection(a.getMode(), b.getMode());
        Filter readFilter = intersection(a.getReadFilter(), b.getReadFilter());
        MultiPolygon rasterFilter = null;

        {
            MultiPolygon aFilter = a.getRasterFilter(), bFilter = b.getRasterFilter();
            if (aFilter == null) rasterFilter = bFilter;
            else if (bFilter == null) rasterFilter = aFilter;
            else rasterFilter = (MultiPolygon) aFilter.intersection(bFilter);
        }

        boolean allowFeatureInfo = a.isAllowFeatureInfo() && b.isAllowFeatureInfo();

        return new WMSAccessLimits(mode, readFilter, rasterFilter, allowFeatureInfo);
    }

    protected Filter intersection(Filter a, Filter b) {
        if (a == null) return b;
        if (b == null) return a;

        if (a == Filter.INCLUDE && b == Filter.INCLUDE) {
            return Filter.INCLUDE;
        } else if (a == Filter.EXCLUDE || b == Filter.EXCLUDE) {
            return Filter.EXCLUDE;
        } else {
            return factory.and(a, b);
        }
    }

    protected GeneralParameterValue[] intersection(GeneralParameterValue[] a, GeneralParameterValue[] b) {
        if (a == null) return b;
        if (b == null) return a;

        List<Integer> indices = new ArrayList<>(Math.min(a.length, b.length));
        List<GeneralParameterValue> bAsList = Arrays.asList(b);

        for (int i = 0; i < a.length; i++) {
            if (bAsList.contains(a[i])) {
                indices.add(i);
            }
        }

        if (indices.size() == a.length) {
            return a;
        } else {
            GeneralParameterValue[] results = new GeneralParameterValue[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                results[i] = a[indices.get(i)];
            }
            return results;
        }
    }

    protected List<PropertyName> intersection(List<PropertyName> a, List<PropertyName> b) {
        if (a == null) return b;
        if (b == null) return a;

        List<PropertyName> results = new ArrayList<>();
        for (PropertyName p : a) {
            if (b.contains(p)) {
                results.add(p);
            }
        }

        return results;
    }

    protected WorkspaceAccessLimits intersection(WorkspaceAccessLimits a, WorkspaceAccessLimits b) {
        CatalogMode mode = intersection(a.getMode(), b.getMode());
        return new WorkspaceAccessLimits(
                mode,
                a.isReadable() && b.isReadable(),
                a.isWritable() && b.isWritable(),
                a.isAdminable() && b.isAdminable());
    }

    public void setDelegate(ResourceAccessManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        return delegate.getAccessLimits(user, resource);
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        return delegate.getAccessLimits(user, layer);
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer, List<LayerGroupInfo> containers) {
        return delegate.getAccessLimits(user, layer, containers);
    }

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        return delegate.getAccessLimits(user, workspace);
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        return delegate.getAccessLimits(user, style);
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        return delegate.getAccessLimits(user, layerGroup);
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(
            Authentication user, LayerGroupInfo layerGroup, List<LayerGroupInfo> containers) {
        return delegate.getAccessLimits(user, layerGroup, containers);
    }

    @Override
    public Filter getSecurityFilter(Authentication user, final Class<? extends CatalogInfo> clazz) {
        return delegate.getSecurityFilter(user, clazz);
    }

    @Override
    public boolean isWorkspaceAdmin(Authentication user, Catalog catalog) {
        return delegate.isWorkspaceAdmin(user, catalog);
    }

    public ResourceAccessManager unwrap() {
        return this.delegate;
    }
}
