/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.Wrapper;
import org.geoserver.security.AbstractCatalogFilter;
import org.geotools.filter.expression.InternalVolatileFunction;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;

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
    
    /**
     * Returns true if the sublayers of a layer group are all hidden.
     * @param layerGroup
     * @return
     */
    protected boolean subLayersHidden(LayerGroupInfo layerGroup) {
        boolean anySublayersVisible=false;
        for(PublishedInfo subLayer: layerGroup.getLayers()) {
            if(subLayer instanceof LayerInfo) {
                if(!hideLayer((LayerInfo)subLayer)){
                    anySublayersVisible=true;
                    break;
                };
            } else if (subLayer instanceof LayerGroupInfo) {
                if(!hideLayerGroup((LayerGroupInfo)subLayer)) {
                    anySublayersVisible=true;
                    break;
                };
            }
        }
        
        return !anySublayersVisible;
    }
    
    private Filter inWorkspace() {
        WorkspaceInfo localWS = LocalWorkspace.get();
        if(localWS==null) return Predicates.acceptAll();
        return Predicates.equal("workspace.id", localWS.getId());
    }
    
    private Filter standardFilter(Class<? extends CatalogInfo> clazz) {
        final Filter forGlobal;
        if (LocalWorkspace.get() != null) {
            // TODO need a well known implementation
            // Show globals unless an object with the same name is in the local workspace
            forGlobal = super.getSecurityFilter(clazz);
        } else {
            // Global request, show all globals
            forGlobal = Predicates.acceptAll();
        }
        // If it's a global use the global filter, otherwise check if it's in the local workspace
        return Predicates.or(
                Predicates.and(Predicates.isNull("workspace.id"), forGlobal),
                Predicates.and(Predicates.factory.not(Predicates.isNull("workspace.id")), inWorkspace())
               );
    }
    @Override
    public Filter getSecurityFilter(final Class<? extends CatalogInfo> clazz) {
        WorkspaceInfo localWS = LocalWorkspace.get();
        LayerInfo localLayer = LocalLayer.get();
        if(localWS==null && localLayer==null) {
            return Predicates.acceptAll();
        }
        if(ResourceInfo.class.isAssignableFrom(clazz)) {
            // Show if it's in a visible workspace or used by the local layer
            Filter localLayerFilter;
            if(localLayer==null) {
                localLayerFilter=Predicates.acceptAll();
            } else {
                // TODO Well known check if it's used by the local layer
                return super.getSecurityFilter(clazz);
            }
            return Predicates.or(localLayerFilter, inWorkspace());
        } else if(WorkspaceInfo.class.isAssignableFrom(clazz)) {
            // Show if there's no local workspace or if it is the local workspace
            if(localWS==null) return Predicates.acceptAll();
            return Predicates.equal("id", localWS.getId());
        } else if(LayerGroupInfo.class.isAssignableFrom(clazz)) {
            Filter filter = standardFilter(clazz);
            
            // TODO Need a well known recursive filter for layer groups instead of using an 
            // InternalVolatileFunction, KS
            Function subLayersHidden = new InternalVolatileFunction() {
                @Override
                public Boolean evaluate(Object object) {
                    return !subLayersHidden((LayerGroupInfo) object);
                }
            };
            FilterFactory factory = Predicates.factory;

            // hide the layer if its sublayers are hidden
            filter = Predicates.and(filter, 
                    factory.equals(factory.literal(Boolean.TRUE), subLayersHidden));

            // Only show a layer group in a layer local request if it is the local layer
            if(localLayer!=null) {
                filter = Predicates.and(filter,
                        Predicates.equal("id", localLayer.getId()));
            }
            return filter;
        } else if(StyleInfo.class.isAssignableFrom(clazz)) {
            return standardFilter(clazz);
        } else if(LayerInfo.class.isAssignableFrom(clazz)) {
            // If there's a local Layer, only show that layer, otherwise show all.
            if(localLayer==null) {
                return Predicates.acceptAll();
            } else {
                return Predicates.equal("id", localLayer.getId());
            }
        } else if(NamespaceInfo.class.isAssignableFrom(clazz)) {
            // TODO
            return super.getSecurityFilter(clazz);
        } else {
            return super.getSecurityFilter(clazz);
        }
    }

}
