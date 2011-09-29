/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AbstractCatalogFilter;

/**
 * Filters the resources that are not in the current workspace (used only if virtual services are
 * active)
 * 
 * @author Justin DeOliveira
 * 
 */
public class LocalWorkspaceCatalogFilter extends AbstractCatalogFilter {

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
}
