/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import org.springframework.security.core.Authentication;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.ResourceAccessManagerWrapper;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * Restricts access to data based on the value of {@link LocalWorkspace} and {@link LocalLayer}. 
 * 
 * @author Justin Deoliveira, OpenGeo
 * @author David Winslow, OpenGeo
 */
public class LocalWorkspaceResourceAccessManager extends ResourceAccessManagerWrapper {
    private DataAccessLimits hide(ResourceInfo info) {
        if (info instanceof FeatureTypeInfo) {
            return new VectorAccessLimits(CatalogMode.HIDE, null, Filter.EXCLUDE, null, Filter.EXCLUDE);
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
            return new WorkspaceAccessLimits(CatalogMode.HIDE, false, false);
        } else {
            return super.getAccessLimits(user, workspace);
        }
    }

    private boolean hideLayer(LayerInfo layer) {
        return LocalLayer.get() != null && !LocalLayer.get().equals(layer);
    }

    private boolean hideResource(ResourceInfo resource) {
        if (LocalLayer.get() != null) {
            for (LayerInfo l : resource.getCatalog().getLayers(resource)) {
                if (!l.equals(LocalLayer.get())) {
                    return true;
                }
            }
        }
        return hideWorkspace(resource.getStore().getWorkspace());
    }

    private boolean hideWorkspace(WorkspaceInfo workspace) {
        return LocalWorkspace.get() != null && !LocalWorkspace.get().equals(workspace);
    }
}
