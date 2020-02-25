/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.util.CloseableIterator;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.MultiValuedFilter.MatchAction;

/**
 * Cascade deletes the visited objects, and modifies related object so that they are still
 * consistent. In particular:
 *
 * <ul>
 *   <li>When removing a {@link LayerInfo} the {@link LayerGroupInfo} are modified by removing the
 *       layer. If the layer was the last one, the layer group is removed as well.
 *   <li>When a {@link StyleInfo} is removed the layers using it as the default style are set with
 *       the default style, the layers that use is as an extra style are modified by removing it.
 *       Also, the layer groups using it are changed so that the default layer style is used in
 *       place of the one being removed
 */
public class CascadeDeleteVisitor implements CatalogVisitor {
    static final Logger LOGGER = Logging.getLogger(CascadeDeleteVisitor.class);

    Catalog catalog;

    public CascadeDeleteVisitor(Catalog catalog) {
        this.catalog = catalog;
    }

    public void visit(Catalog catalog) {}

    public void visit(WorkspaceInfo workspace) {
        // remove owned stores
        for (StoreInfo s : catalog.getStoresByWorkspace(workspace, StoreInfo.class)) {
            s.accept(this);
        }

        // remove any linked namespaces
        NamespaceInfo ns = catalog.getNamespaceByPrefix(workspace.getName());
        if (ns != null) {
            ns.accept(this);
        }

        // remove styles contained in this workspace
        for (StyleInfo style : catalog.getStylesByWorkspace(workspace)) {
            style.accept(this);
        }

        // remove layer groups contained in this workspace
        for (LayerGroupInfo group : catalog.getLayerGroupsByWorkspace(workspace)) {
            group.accept(this);
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
            if (!layers.isEmpty()) {
                for (LayerInfo li : layers) {
                    li.accept(this);
                }
            } else {
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

    @Override
    public void visit(WMTSStoreInfo store) {
        visitStore(store);
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

        Filter groupContainsLayer = Predicates.equal("layers.id", layer.getId(), MatchAction.ANY);
        try (CloseableIterator<LayerGroupInfo> groups =
                catalog.list(LayerGroupInfo.class, groupContainsLayer)) {
            while (groups.hasNext()) {
                LayerGroupInfo group = groups.next();

                // parallel remove of layer and styles
                int index = group.getLayers().indexOf(layer);
                while (index != -1) {
                    group.getLayers().remove(index);
                    group.getStyles().remove(index);
                    index = group.getLayers().indexOf(layer);
                }

                // either update or remove the group
                if (group.getLayers().size() == 0) {
                    visit(catalog.getLayerGroup(group.getId()));
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

    private StyleInfo getResourceDefaultStyle(ResourceInfo resource, StyleInfo removedStyle) {
        StyleInfo style = null;
        try {
            style = new CatalogBuilder(catalog).getDefaultStyle(resource);
        } catch (IOException e) {
            // we fall back on the default style (since we cannot roll back the
            // entire operation, no transactions in the catalog)
            LOGGER.log(
                    Level.WARNING,
                    "Could not find default style for resource " + resource + ", using Point style",
                    e);
        }

        if (style == null || style.equals(removedStyle)) {
            return catalog.getStyleByName(StyleInfo.DEFAULT_POINT);
        }

        return style;
    }

    private void removeStyleInLayer(LayerInfo layer, StyleInfo style) {
        boolean dirty = false;

        // remove it from the associated styles
        if (layer.getStyles().remove(style)) {
            dirty = true;
        }

        // if it's the default style, choose an associated style or reset it to the default one
        StyleInfo ds = layer.getDefaultStyle();
        if (ds != null && ds.equals(style)) {
            dirty = true;

            StyleInfo newDefaultStyle;
            if (layer.getStyles().size() > 0) {
                newDefaultStyle = layer.getStyles().iterator().next();
                layer.getStyles().remove(newDefaultStyle);
            } else {
                newDefaultStyle = getResourceDefaultStyle(layer.getResource(), style);
            }

            layer.setDefaultStyle(newDefaultStyle);
        }

        if (dirty) {
            catalog.save(layer);
        }
    }

    private void removeStyleInLayerGroup(LayerGroupInfo group, StyleInfo style) {
        boolean dirty = false;

        // root layer style
        if (style.equals(group.getRootLayerStyle())) {
            group.setRootLayerStyle(
                    getResourceDefaultStyle(group.getRootLayer().getResource(), style));
            dirty = true;
        }

        // layer styles
        List<StyleInfo> styles = group.getStyles();
        for (int i = 0; i < styles.size(); i++) {
            StyleInfo publishedStyle = styles.get(i);
            if (publishedStyle != null && publishedStyle.equals(style)) {
                // if publishedStyle is not null, we have a layer
                LayerInfo layer = (LayerInfo) group.getLayers().get(i);

                if (!layer.getDefaultStyle().equals(style)) {
                    // use default style
                    styles.set(i, layer.getDefaultStyle());
                } else {
                    styles.set(i, getResourceDefaultStyle(layer.getResource(), style));
                }

                dirty = true;
            }
        }

        if (dirty) {
            catalog.save(group);
        }
    }

    public void visit(StyleInfo style) {
        // find the layers having this style as primary or secondary
        Filter anyStyle = Predicates.equal("styles.id", style.getId(), MatchAction.ANY);
        Filter layersAssociated =
                Predicates.or(Predicates.equal("defaultStyle.id", style.getId()), anyStyle);

        // remove style references in layers
        try (CloseableIterator<LayerInfo> it = catalog.list(LayerInfo.class, layersAssociated)) {
            while (it.hasNext()) {
                LayerInfo layer = it.next();
                removeStyleInLayer(layer, style);
            }
        }
        // groups can also refer to style, reset each reference to the
        // associated layer default style
        Filter groupAssociated =
                Predicates.or(Predicates.equal("rootLayerStyle.id", style.getId()), anyStyle);
        try (CloseableIterator<LayerGroupInfo> it =
                catalog.list(LayerGroupInfo.class, groupAssociated)) {
            while (it.hasNext()) {
                LayerGroupInfo group = it.next();
                removeStyleInLayerGroup(group, style);
            }
        }

        // finally remove the style
        catalog.remove(style);
    }

    public void visit(LayerGroupInfo layerGroupToRemove) {
        // remove layerGroupToRemove references from other groups
        Filter associatedTo =
                Predicates.equal("layers.id", layerGroupToRemove.getId(), MatchAction.ANY);
        try (CloseableIterator<LayerGroupInfo> it =
                catalog.list(LayerGroupInfo.class, associatedTo)) {
            while (it.hasNext()) {
                LayerGroupInfo group = it.next();

                // parallel remove of layer and styles
                int index = getLayerGroupIndex(layerGroupToRemove, group);
                while (index != -1) {
                    group.getLayers().remove(index);
                    group.getStyles().remove(index);
                    index = getLayerGroupIndex(layerGroupToRemove, group);
                }
                if (group.getLayers().size() == 0) {
                    // if group is empty, delete it
                    visit(group);
                } else {
                    catalog.save(group);
                }
            }
        }

        // finally remove the group
        catalog.remove(layerGroupToRemove);
    }

    /**
     * Between modification proxies and security buffering the list of layers of a group it's just
     * safer and more predictable to use a id comparison instead of a equals that accounts for each
     * and every field
     */
    private int getLayerGroupIndex(LayerGroupInfo layerGroup, LayerGroupInfo container) {
        int idx = 0;
        final String id = layerGroup.getId();
        for (PublishedInfo pi : container.getLayers()) {
            if (pi instanceof LayerGroupInfo && id.equals(pi.getId())) {
                return idx;
            }
            idx++;
        }

        return -1;
    }

    public void visit(WMSLayerInfo wmsLayer) {
        catalog.remove(wmsLayer);
    }

    @Override
    public void visit(WMTSLayerInfo wmtsLayer) {
        catalog.remove(wmtsLayer);
    }
}
