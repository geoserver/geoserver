/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Visits the specified objects cascading down to contained/related objects,
 * and collects information about which objects will be removed or modified
 * once the root objects are cascade deleted with {@link CascadeDeleteVisitor} 
 */
public class CascadeRemovalReporter implements CatalogVisitor {

    /**
     * The various types of modifications a catalog object can be subjected to
     * in case of cascade removal. They are sorted from stronger to weaker. 
     */
    public enum ModificationType {
        DELETE, STYLE_RESET, EXTRA_STYLE_REMOVED, GROUP_CHANGED;
    }

    /**
     * The catalog used to drill down into the containment hierarchy
     */
    Catalog catalog;

    /**
     * The set of objects collected during the scan
     */
    Map<CatalogInfo, ModificationType> objects;
    
    /**
     * Used to track which layers are going to be removed from a group, if
     * we remove them all the group will have to be removed as well
     */
    Map<LayerGroupInfo, Set<LayerInfo>> groups;

    public CascadeRemovalReporter(Catalog catalog) {
        this.catalog = catalog;
        reset();
    }
    
    public void visit(Catalog catalog) {
    }
    
    /**
     * Resets the visitor so that it can be reused for another search
     */
    public void reset() {
        this.objects = new HashMap<CatalogInfo, ModificationType>();
        this.groups = new HashMap<LayerGroupInfo, Set<LayerInfo>>();
    }

    /**
     * Returns the objects that will be affected by the removal, filtering them by type and by kind
     * of modification they will sustain as a consequence of the removal
     * 
     * @param <T>
     * @param catalogClass
     *            The type of object to be searched for, or null if no type filtering is desired
     * @param modification
     *            The kind of modification to be searched for, or null if no modification type
     *            filtering is desired
     * @return
     */
    public <T> List<T> getObjects(Class<T> catalogClass, ModificationType... modifications) {
        List<T> result = new ArrayList<T>();
        List<ModificationType> mods = (modifications == null || modifications.length == 0) ? 
                                      null : Arrays.asList(modifications); 
        for (CatalogInfo ci : objects.keySet()) {
            if (catalogClass == null || catalogClass.isAssignableFrom(ci.getClass())) {
                if (mods == null || mods.contains(objects.get(ci)))
                    result.add((T) ci);
            }
        }
        return result;
    }
    
    /**
     * Allows removal of the specified objects from the reachable set (usually, the user
     * will not want the roots to be part of the set)
     * @param objects
     */
    public void removeAll(Collection<? extends CatalogInfo> objects) {
        for (CatalogInfo ci : objects) {
            this.objects.remove(ci);
        }
    }

    /**
     * Adds a CatalogInfo into the objects map, eventually overriding the
     * type if the modification is stronger that the one already registered
     */
    void add(CatalogInfo ci, ModificationType type) {
        ModificationType oldType = objects.get(ci);
        if(oldType == null || oldType.compareTo(type) > 0) {
            objects.put(ci, type);
        }
    }
    
    
    public void visit(WorkspaceInfo workspace) {
        // drill down on stores
        List<StoreInfo> stores = catalog.getStoresByWorkspace(workspace, StoreInfo.class);
        for (StoreInfo storeInfo : stores) {
            storeInfo.accept(this);
        }
        // drill into namespaces
        // catalog.getNamespaceByPrefix(workspace.getName()).accept(this);

        // add self
        add(workspace, ModificationType.DELETE);
    }

    public void visit(NamespaceInfo namespace) {
        add(namespace, ModificationType.DELETE);
    }

    public void visit(DataStoreInfo dataStore) {
        visitStore(dataStore);
    }

    public void visit(CoverageStoreInfo coverageStore) {
        visitStore(coverageStore);
    }
    
    public void visit(WMSStoreInfo store) {
        visitStore(store);
    }

    void visitStore(StoreInfo dataStore) {
        // drill down into layers (into resources since we cannot scan layers)
        List<ResourceInfo> resources = catalog.getResourcesByStore(dataStore, ResourceInfo.class);
        for (ResourceInfo ri : resources) {
            List<LayerInfo> layers = catalog.getLayers(ri);
            if (!layers.isEmpty()) {
                for (LayerInfo li : layers) {
                    li.accept(this);
                }
            }
            else {
                ri.accept(this);
            }
        }

        add(dataStore, ModificationType.DELETE);
    }

    public void visit(FeatureTypeInfo featureType) {
        add(featureType, ModificationType.DELETE);
    }

    public void visit(CoverageInfo coverage) {
        add(coverage, ModificationType.DELETE);
    }
    
    public void visit(WMSLayerInfo wmsLayer) {
        add(wmsLayer, ModificationType.DELETE);
    }

    public void visit(LayerInfo layer) {
        // mark layer and resource as removed
        add(layer.getResource(), ModificationType.DELETE);
        add(layer, ModificationType.DELETE);
        
        // scan the layer groups and find those that do use the
        // current layer
        for (LayerGroupInfo group : catalog.getLayerGroups()) {
            if(group.getLayers().contains(layer)) {
                // mark the layer as one that will be removed
                Set<LayerInfo> layers = groups.get(group);
                if(layers == null) {
                    layers = new HashSet<LayerInfo>();
                    groups.put(group, layers);
                }
                layers.add(layer);
                
                // a group can contain the same layer multiple times. We want to
                // make sure to mark the group as removed if all the layers inside of
                // it are going to be removed, just changed otherwise
                if(layers.size() == new HashSet<PublishedInfo>(group.getLayers()).size()) {
                    add(group, ModificationType.DELETE);
                } else {
                    add(group, ModificationType.GROUP_CHANGED);
                }
            }
        }
    }

    public void visit(StyleInfo style) {
        // add users of this style among the related objects: layers
        List<LayerInfo> layer = catalog.getLayers();
        for (LayerInfo li : layer) {
            if (style.equals(li.getDefaultStyle()))
                add(li, ModificationType.STYLE_RESET);
            else if(li.getStyles().contains(style))
                add(li, ModificationType.EXTRA_STYLE_REMOVED);
        }

        // groups can also refer styles
        List<LayerGroupInfo> groups = catalog.getLayerGroups();
        for (LayerGroupInfo group : groups) {
            if (style.equals(group.getRootLayerStyle())) {
                add(group, ModificationType.GROUP_CHANGED);                
            }
            
            if (group.getStyles().contains(style)) {
                add(group, ModificationType.GROUP_CHANGED);
            }
        }
        
        // add the style
        add(style, ModificationType.DELETE);
    }

    public void visit(LayerGroupInfo layerGroupToRemove) {
        List<LayerGroupInfo> groups = catalog.getLayerGroups();
        for (LayerGroupInfo group : groups) {
            if (group.getLayers().remove(layerGroupToRemove)) {
                if (group.getLayers().size() == 0) {
                    visit(group);
                } else {
                    add(group, ModificationType.GROUP_CHANGED);
                }
            }
        }
        
        add(layerGroupToRemove, ModificationType.DELETE);
    }
}
