/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.Wrapper;
import org.geoserver.security.AbstractCatalogFilter;

/**
 * Filters the resources that are not in the current workspace (used only if virtual services are
 * active)
 * 
 * @author Justin DeOliveira
 * 
 */
public class LocalWorkspaceCatalogFilter extends AbstractCatalogFilter {

    /** the real/raw catalog, can't be a wrapper */
    Catalog catalog;

    public LocalWorkspaceCatalogFilter(Catalog catalog) {
        //unwrap it just to be sure
        while (catalog instanceof Wrapper && ((Wrapper)catalog).isWrapperFor(Catalog.class)) {
            Catalog unwrapped = ((Wrapper)catalog).unwrap(Catalog.class);
            if (unwrapped == catalog || unwrapped == null) {
                break;
            }

            catalog = unwrapped;
        }
        this.catalog = catalog;
    }

    public boolean hideLayer(LayerInfo layer) {
        return LocalLayer.get() != null && !LocalLayer.get().equals(layer);
    }

    public boolean hideResource(ResourceInfo resource) {
        if (LocalLayer.get() != null) {
            for (LayerInfo l : resource.getCatalog().getLayers(resource)) {
                if (!l.equals(LocalLayer.get())) {
                    return true;
                }
            }
        }
        return hideWorkspace(resource.getStore().getWorkspace());
    }

    public boolean hideWorkspace(WorkspaceInfo workspace) {
        return LocalWorkspace.get() != null && !LocalWorkspace.get().equals(workspace);
    }

    public boolean hideStyle(StyleInfo style) {
        if (style.getWorkspace() == null) {
            //global style, hide it if a local workspace style shars the same name, ie overrides it
            if (LocalWorkspace.get() != null) {
                if (catalog.getStyleByName(LocalWorkspace.get(), style.getName()) != null) {
                    return true;
                }
            }
            return false;
        }
        return hideWorkspace(style.getWorkspace());
    }

    @Override
    public boolean hideLayerGroup(LayerGroupInfo layerGroup) {
        if (layerGroup.getWorkspace() == null) {
            //global layer group, hide it if a local workspace layer group shared the same name, ie 
            // overrides it
            if (LocalWorkspace.get() != null) {
                if (catalog.getLayerGroupByName(LocalWorkspace.get(), layerGroup.getName()) != null) {
                    return true;
                }
            }
            return false;
        }
        return hideWorkspace(layerGroup.getWorkspace());
    }
}
