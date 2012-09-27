/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

/**
 * Cascade deletes the visited objects, and modifies related object
 * so that they are still consistent.
 * In particular:
 * <ul>
 *   <li>When removing a {@link LayerInfo} the {@link LayerGroupInfo} are modified 
 *       by removing the layer. If the layer was the last one, the layer group
 *       is removed as well.
 *   </li>
 *   <li>When a {@link StyleInfo} is removed the layers using it as the default
 *       style are set with the default style, the layers that use is as an extra
 *       style are modified by removing it. Also, the layer groups using it 
 *       are changed so that the default layer style is used in place of the
 *       one being removed
 *   </li>
 */
public class CascadeDeleteVisitor implements CatalogVisitor {
    static final Logger LOGGER = Logging.getLogger(CascadeDeleteVisitor.class);
    
    Catalog catalog;
    
    public CascadeDeleteVisitor(Catalog catalog) {
        this.catalog = catalog;
    }
    
    public void visit(Catalog catalog) {
    }

    public void visit(WorkspaceInfo workspace) {
        // remove owned stores
        for ( StoreInfo s : catalog.getStoresByWorkspace( workspace, StoreInfo.class ) ) {
            s.accept(this);
        }

        //remove any linked namespaces
        NamespaceInfo ns = catalog.getNamespaceByPrefix( workspace.getName() );
        if ( ns != null ) {
            ns.accept(this);
        }

        catalog.remove(workspace);
    }

    public void visit(NamespaceInfo workspace) {
        catalog.remove(workspace);
    }
    
    void visitStore(StoreInfo store) {
        // drill down into layers (into resources since we cannot scan layers)
        List<ResourceInfo> resources = catalog.getResourcesByStore(store, ResourceInfo.class);
        for (ResourceInfo ri : resources) {
            List<LayerInfo> layers = catalog.getLayers(ri);
            if (!layers.isEmpty()){ 
                for (LayerInfo li : layers) {
                    li.accept(this);
                }
            }
            else {
                // no layers for the resource, delete directly
                ri.accept(this);
            }
        }

        catalog.remove(store);
    }

    public void visit(DataStoreInfo dataStore) {
        visitStore(dataStore);
    }

    public void visit(CoverageStoreInfo coverageStore) {
        visitStore(coverageStore);
    }
    
    public void visit(WMSStoreInfo wmsStore) {
        visitStore(wmsStore);
    }


    public void visit(FeatureTypeInfo featureType) {
        // when the resource/layer split is done, delete all layers linked to the resource
        catalog.remove(featureType);
    }

    public void visit(CoverageInfo coverage) {
        // when the resource/layer split is done, delete all layers linked to the resource
        catalog.remove(coverage);
    }

    public void visit(LayerInfo layer) {
        // first update the groups, remove the layer, and if no
        // other layers remained, remove the group as well
        for (LayerGroupInfo group : catalog.getLayerGroups()) {
            if(group.getLayers().contains(layer)) {
                // parallel remove of layer and styles
                int index = group.getLayers().indexOf(layer);
                group.getLayers().remove(index);
                group.getStyles().remove(index);
                
                // either update or remove the group
                if(group.getLayers().size() == 0) {
                    catalog.remove(group);
                } else {
                    catalog.save(group);
                }
            }
        }
        
        // remove the layer and (for the moment) its resource as well
        // TODO: change this to just remove the resource once the 
        // resource/publish split is done
        ResourceInfo resource = layer.getResource();
        catalog.remove(layer);
        catalog.remove(resource);
    }

    public void visit(StyleInfo style) {
        // add users of this style among the related objects: layers
        List<LayerInfo> layer = catalog.getLayers();
        for (LayerInfo li : layer) {
            // if it's the default style, reset it to the default one
            StyleInfo ds = li.getDefaultStyle();
            if (ds != null && ds.equals(style)) {
                try {
                    li.setDefaultStyle(new CatalogBuilder(catalog).getDefaultStyle(li.getResource()));
                    catalog.save(li);
                } catch(IOException e) {
                    // we fall back on the default style (since we cannot roll back the
                    // entire operation, no transactions in the catalog)
                    LOGGER.log(Level.WARNING, "Could not find default style for resource " 
                            + li.getResource() + " resetting the default to point", e);
                    li.setDefaultStyle(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
                    catalog.save(li);
                }
            }
            // remove it also from the associated styles
            if(li.getStyles().remove(style)) {
                catalog.save(li);
            }
        }

        // groups can also refer styles, reset each reference to the
        // associated layer default style
        List<LayerGroupInfo> groups = catalog.getLayerGroups();
        for (LayerGroupInfo group : groups) {
            List<StyleInfo> styles = group.getStyles();
            boolean dirty = false;
            for (int i = 0; i < styles.size(); i++) {
                StyleInfo si = styles.get(i);
                if(si != null && si.equals(style)) {
                    styles.set(i, group.getLayers().get(i).getDefaultStyle());
                    dirty = true;
                }
            }
            if(dirty)
                catalog.save(group);
        }
        
        // finally remove the style
        catalog.remove(style);
    }

    public void visit(LayerGroupInfo layerGroup) {
        catalog.remove(layerGroup);
    }

    public void visit(WMSLayerInfo wmsLayer) {
        catalog.remove(wmsLayer);
        
    }

}
