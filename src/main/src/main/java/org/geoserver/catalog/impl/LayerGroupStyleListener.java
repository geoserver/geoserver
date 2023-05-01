/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerExtensions;
import org.opengis.filter.Filter;

/**
 * A CatalogListener handling the renaming of LayerGroupStyle. When a LayerGroupStyle gets renamed
 * it searches for LayerGroup that have a reference to that style and renames it in corresponding
 * LayerGroupEntry.
 */
public class LayerGroupStyleListener implements CatalogListener {

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {}

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {}

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {}

    private void updateLayerGroupStyleNames(
            LayerGroupInfo groupInfo, List<Pair<StyleInfo, StyleInfo>> changedNameStyles) {
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        for (Pair<StyleInfo, StyleInfo> oldAndNew : changedNameStyles) {
            StyleInfo oldStyleName = oldAndNew.getLeft();

            Filter containsGroup = Predicates.equal("layers.id", groupInfo.getId());

            // uses contains instead of equals predicate because the result of the PropertyName
            // is a List of List and would cause the filter to fail the test.
            Filter stylesContainsGroup =
                    Predicates.contains("layerGroupStyles.layers.id", groupInfo.getId());

            Filter or = Predicates.or(containsGroup, stylesContainsGroup);
            try (CloseableIterator<LayerGroupInfo> it = catalog.list(LayerGroupInfo.class, or)) {
                while (it.hasNext()) {
                    LayerGroupInfo toUpdate = it.next();
                    // eventually update style names in the default LayerGroup configuration
                    updateStyleName(
                            toUpdate.getLayers(),
                            toUpdate.getStyles(),
                            groupInfo,
                            oldStyleName,
                            oldAndNew.getRight());
                    for (LayerGroupStyle s : toUpdate.getLayerGroupStyles()) {
                        // eventually update style names in the LayerGroup styles
                        updateStyleName(
                                s.getLayers(),
                                s.getStyles(),
                                groupInfo,
                                oldStyleName,
                                oldAndNew.getRight());
                    }
                    catalog.save(toUpdate);
                }
            }
        }
    }

    private void updateStyleName(
            List<PublishedInfo> publishedInfos,
            List<StyleInfo> styles,
            LayerGroupInfo groupInfo,
            StyleInfo oldStyleName,
            StyleInfo newStyleName) {
        int groupIndex = publishedInfos.indexOf(groupInfo);
        if (groupIndex != -1) {
            StyleInfo styleInfo = styles.get(groupIndex);
            if (styleInfo.getName().equals(oldStyleName.getName())) {
                StyleInfo newName = newStyleName;
                styles.set(groupIndex, newName);
            }
        }
    }

    private List<Pair<StyleInfo, StyleInfo>> findChangedNameStyles(List<LayerGroupStyle> news) {
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        List<Pair<StyleInfo, StyleInfo>> changedStylesNames = new ArrayList<>();
        for (LayerGroupStyle groupStyle : news) {
            // gets the Proxy of each style
            ModificationProxy styleProxy = getModificationProxy(groupStyle);
            if (styleProxy != null) {
                int index = styleProxy.getPropertyNames().indexOf("name");
                if (index != -1) {
                    Object newNameInfo = styleProxy.getNewValues().get(index);
                    // then the one of the style name
                    ModificationProxy nameProxy = getModificationProxy(newNameInfo);
                    index = nameProxy.getPropertyNames().indexOf("name");
                    if (index != -1) {
                        String oldName = (String) nameProxy.getOldValues().get(index);
                        String newName = (String) nameProxy.getNewValues().get(index);
                        if (oldName != null && newName != null) {
                            StyleInfo oldStyleInfo = new StyleInfoImpl(catalog);
                            oldStyleInfo.setName(oldName);
                            StyleInfo newStyleInfo = new StyleInfoImpl(catalog);
                            newStyleInfo.setName(newName);
                            changedStylesNames.add(new ImmutablePair<>(oldStyleInfo, newStyleInfo));
                        }
                    }
                }
            }
        }
        return changedStylesNames;
    }

    private ModificationProxy getModificationProxy(Object o) {
        ModificationProxy modProxy = null;
        if (o instanceof Proxy) {
            modProxy = (ModificationProxy) Proxy.getInvocationHandler(o);
        }
        return modProxy;
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        final CatalogInfo source = event.getSource();
        if (source instanceof LayerGroupInfo) {
            LayerGroupInfo lg = (LayerGroupInfo) event.getSource();
            int nameIdx = event.getPropertyNames().indexOf("layerGroupStyles");
            if (nameIdx != -1) {
                @SuppressWarnings("unchecked")
                List<LayerGroupStyle> newStyles =
                        (List<LayerGroupStyle>) event.getNewValues().get(nameIdx);
                List<Pair<StyleInfo, StyleInfo>> changedNames = findChangedNameStyles(newStyles);
                if (!changedNames.isEmpty()) {
                    updateLayerGroupStyleNames(lg, changedNames);
                }
            }
        }
    }

    @Override
    public void reloaded() {}
}
