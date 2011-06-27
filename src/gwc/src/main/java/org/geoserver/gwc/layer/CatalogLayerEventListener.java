/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geotools.util.logging.Logging;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.geowebcache.storage.StorageBroker;

/**
 * Listens to {@link Catalog}'s layer added/removed events and adds/removes
 * {@link GeoServerTileLayer}s to/from the {@link CatalogConfiguration}
 * <p>
 * Handles the following cases:
 * <ul>
 * <li><b>Layer added</b>: a {@link LayerInfo} or {@link LayerGroupInfo} has been added. A
 * {@link GeoServerTileLayer} is {@link CatalogConfiguration#createLayer is created} with the
 * {@link GWCConfig default settings} only if the integrated GWC configuration is set to
 * {@link GWCConfig#isCacheLayersByDefault() cache layers by default}.</li>
 * <li><b>Layer removed</b>: a {@code LayerInfo} or {@code LayerGroupInfo} has been removed. GWC is
 * instructed to remove the layer, deleting it's cache and any other associated information
 * completely (for example, the disk quota information for the layer is also deleted and the global
 * usage updated accordingly).
 * <li><b>Layer renamed</b>: a {@link LayerInfo} or {@link LayerGroupInfo} has been renamed. GWC is
 * {@link StorageBroker#rename instructed to rename} the corresponding tile layer preserving the
 * cache and any other information (usage statistics, disk quota usage, etc).</li>
 * <li><b>Namespace changed</b>: a {@link ResourceInfo} has been assigned to a different
 * {@link NamespaceInfo namespace}. As the GWC tile layers are named after the resource's
 * {@link ResourceInfo#getPrefixedName() prefixed name} and not only after the
 * {@link LayerInfo#getName()} (at least until GeoServer separates out data from publication - the
 * famous data/publish split), GWC is instructed to rename the layer preserving the cache and any
 * other information for the layer.</li>
 * <li><b>LayerGroupInfo modified</b>: either the {@link LayerGroupInfo#getLayers() layers} or
 * {@link LayerGroupInfo#getStyles() styles} changed for a {@code LayerGroupInfo}. It's cache is
 * truncated.</li>
 * <li><b>LayerInfo default style replaced</b>: a {@code LayerInfo} has been assigned a different
 * {@link LayerInfo#getDefaultStyle() default style}. The corresponding tile layer's cache is
 * truncated for the default style.</li>
 * <li><b>LayerInfo alternate styles changed</b> the set of a {@code LayerInfo}'s
 * {@link LayerInfo#getStyles() alternate styles} has been modified. For any added style, if the
 * {@link GeoServerTileLayer} is configured to {@link GeoServerTileLayerInfo#isAutoCacheStyles()
 * automatically cache all styles}, the style name is added to the set of
 * {@link GeoServerTileLayerInfo#getCachedStyles() cached styles}. For any <b>removed</b> style, if
 * it was one of the {@link GeoServerTileLayerInfo#getCachedStyles() cached styles}, the layer's
 * cache for that style is truncated, and it's removed from the tile layer's set of cached styles.
 * Subsequently, the {@link GeoServerTileLayer} will create a {@link StringParameterFilter "STYLES"
 * parameter filter} for all the cached styles on demand</li>
 * </ul>
 * </p>
 * 
 * @author Arne Kepp
 * @author Gabriel Roldan
 */
public class CatalogLayerEventListener implements CatalogListener {

    private static Logger log = Logging.getLogger(CatalogLayerEventListener.class);

    private final CatalogConfiguration catalogConfig;

    /**
     * Holds the CatalogModifyEvent from {@link #handleModifyEvent} to be taken after the change was
     * applied to the {@link Catalog} at {@link #handlePostModifyEvent} and check whether it is
     * necessary to perform any action on the cache based on the changed properties
     */
    private static ThreadLocal<CatalogModifyEvent> PRE_MODIFY_EVENT = new ThreadLocal<CatalogModifyEvent>();

    public CatalogLayerEventListener(final CatalogConfiguration catalogConfiguration) {
        this.catalogConfig = catalogConfiguration;
    }

    /**
     * If either a {@link LayerInfo} or {@link LayerGroupInfo} has been added to the {@link Catalog}
     * , create a corresponding GWC TileLayer.
     * 
     * @see org.geoserver.catalog.event.CatalogListener#handleAddEvent
     * @see GWC#createLayer(LayerInfo)
     * @see GWC#createLayer(LayerGroupInfo)
     */
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        Object obj = event.getSource();
        // We only handle layers here. Layer groups are initially empty
        if (obj instanceof LayerInfo) {
            log.finer("Handling add event: " + obj);
            LayerInfo layerInfo = (LayerInfo) obj;
            catalogConfig.createLayer(layerInfo);
        } else if (obj instanceof LayerGroupInfo) {
            LayerGroupInfo lgi = (LayerGroupInfo) obj;
            catalogConfig.createLayer(lgi);
        }
    }

    /**
     * @see org.geoserver.catalog.event.CatalogListener#handleModifyEvent(org.geoserver.catalog.event.CatalogModifyEvent)
     * @see #handlePostModifyEvent
     */
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        CatalogInfo source = event.getSource();
        if (source instanceof LayerInfo || source instanceof LayerGroupInfo
                || source instanceof FeatureTypeInfo || source instanceof CoverageInfo
                || source instanceof WMSLayerInfo) {
            PRE_MODIFY_EVENT.set(event);
        }
    }

    /**
     * In case the event refers to the addition or removal of a {@link LayerInfo} or
     * {@link LayerGroupInfo} adds or removes the corresponding {@link GeoServerTileLayer} through
     * {@link GWC#createLayer}.
     * <p>
     * Note this method does not discriminate whether the change in the layer or layergroup deserves
     * a change in its matching TileLayer, it just re-creates the TileLayer
     * </p>
     * 
     * @see org.geoserver.catalog.event.CatalogListener#handlePostModifyEvent(org.geoserver.catalog.event.CatalogPostModifyEvent)
     */
    public void handlePostModifyEvent(final CatalogPostModifyEvent event) throws CatalogException {
        final Object source = event.getSource();
        if (!(source instanceof LayerInfo || source instanceof LayerGroupInfo
                || source instanceof FeatureTypeInfo || source instanceof CoverageInfo || source instanceof WMSLayerInfo)) {
            return;
        }

        final CatalogModifyEvent preModifyEvent = PRE_MODIFY_EVENT.get();
        if (preModifyEvent == null) {
            throw new IllegalStateException(
                    "PostModifyEvent called without having called handlePreModify first?");
        }
        PRE_MODIFY_EVENT.remove();

        final List<String> changedProperties = preModifyEvent.getPropertyNames();
        final List<Object> oldValues = preModifyEvent.getOldValues();
        final List<Object> newValues = preModifyEvent.getNewValues();

        log.finer("Handling modify event for " + source);
        if (source instanceof FeatureTypeInfo || source instanceof CoverageInfo
                || source instanceof WMSLayerInfo || source instanceof LayerGroupInfo) {
            /*
             * Handle the rename case. For LayerInfos it's actually the related ResourceInfo what
             * gets renamed, at least until the data/publish split is implemented in GeoServer. For
             * LayerGroupInfo it's the group itself
             */
            if (changedProperties.contains("name") || changedProperties.contains("namespace")) {
                handleRename(source, changedProperties, oldValues, newValues);
            }
        }

        if (source instanceof LayerInfo) {
            if (changedProperties.contains("defaultStyle") || changedProperties.contains("styles")) {
                // REVISIT: what about truncating the LayerGroups containing the modified layer?
                // checking the style applies of course
                final LayerInfo li = (LayerInfo) source;
                handleLayerInfo(changedProperties, oldValues, newValues, li);
            }

        } else if (source instanceof LayerGroupInfo) {
            if (changedProperties.contains("layers") || changedProperties.contains("styles")) {
                LayerGroupInfo lgInfo = (LayerGroupInfo) source;
                handleLayerGroupInfo(changedProperties, oldValues, newValues, lgInfo);
            }
        }
    }

    private void handleLayerGroupInfo(final List<String> changedProperties,
            final List<Object> oldValues, final List<Object> newValues, LayerGroupInfo lgInfo) {
        boolean truncate = false;
        if (changedProperties.contains("layers")) {
            final int layersIndex = changedProperties.indexOf("layers");
            Object oldLayers = oldValues.get(layersIndex);
            Object newLayers = newValues.get(layersIndex);
            truncate = !oldLayers.equals(newLayers);
        }

        if (!truncate && changedProperties.contains("styles")) {
            final int stylesIndex = changedProperties.indexOf("styles");
            Object oldStyles = oldValues.get(stylesIndex);
            Object newStyles = newValues.get(stylesIndex);
            truncate = !oldStyles.equals(newStyles);
        }
        if (truncate) {
            log.info("Truncating TileLayer for layer group '" + lgInfo.getName()
                    + "' due to a change in its layers or styles");
            String layerName = lgInfo.getName();
            catalogConfig.truncate(layerName);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleLayerInfo(final List<String> changedProperties,
            final List<Object> oldValues, final List<Object> newValues, final LayerInfo li) {
        final String layerName = li.getResource().getPrefixedName();
        final GeoServerTileLayer tileLayer;
        tileLayer = (GeoServerTileLayer) catalogConfig.getTileLayer(layerName);

        boolean save = false;
        if (changedProperties.contains("defaultStyle")) {
            final int propIndex = changedProperties.indexOf("defaultStyle");
            final StyleInfo oldStyle = (StyleInfo) oldValues.get(propIndex);
            final StyleInfo newStyle = (StyleInfo) newValues.get(propIndex);
            final String oldStyleName = oldStyle.getName();
            final String newStyleName = newStyle.getName();
            if (!oldStyleName.equals(newStyleName)) {
                save = true;
                catalogConfig.truncate(layerName, oldStyleName);
            }
        }
        if (changedProperties.contains("styles")) {
            final GeoServerTileLayerInfo info = tileLayer.getInfo();
            final int propIndex = changedProperties.indexOf("styles");
            final Set<StyleInfo> oldStyles = (Set<StyleInfo>) oldValues.get(propIndex);
            final Set<StyleInfo> currentStyles = (Set<StyleInfo>) newValues.get(propIndex);
            Set<String> newStyleSet = new HashSet<String>(info.getCachedStyles());
            if (!oldStyles.equals(currentStyles)) {
                Set<StyleInfo> removed = new HashSet<StyleInfo>(oldStyles);
                removed.removeAll(currentStyles);

                // remove any style detacched from the layer
                for (StyleInfo deletedStyle : removed) {
                    String styleName = deletedStyle.getName();
                    newStyleSet.remove(styleName);
                    catalogConfig.truncate(layerName, styleName);
                }
                // add new cached styles if tilelayer is configured to do so
                if (info.isAutoCacheStyles()) {
                    Set<StyleInfo> added = new HashSet<StyleInfo>(currentStyles);
                    added.removeAll(oldStyles);
                    for (StyleInfo addedStyle : added) {
                        String styleName = addedStyle.getName();
                        newStyleSet.add(styleName);
                    }
                }
            }
            // prune any tangling style from info
            Set<String> currentStyleNames = new HashSet<String>();
            for (StyleInfo current : currentStyles) {
                currentStyleNames.add(current.getName());
            }
            newStyleSet.retainAll(currentStyleNames);
            // recreate parameter filters if need be
            if (!newStyleSet.equals(info.getCachedStyles())) {
                save = true;
                info.setCachedStyles(newStyleSet);
                tileLayer.resetParameterFilters();
            }
        }
        if (save) {
            catalogConfig.save(tileLayer);
        }
    }

    private void handleRename(final Object source, final List<String> changedProperties,
            final List<Object> oldValues, final List<Object> newValues) {
        final int nameIndex = changedProperties.indexOf("name");
        final int namespaceIndex = changedProperties.indexOf("namespace");

        String oldLayerName;
        String newLayerName;
        if (source instanceof ResourceInfo) {// covers LayerInfo, CoverageInfo, and WMSLayerInfo
            // must cover prefix:name
            final ResourceInfo resourceInfo = (ResourceInfo) source;
            final NamespaceInfo currNamespace = resourceInfo.getNamespace();
            final NamespaceInfo oldNamespace;
            if (namespaceIndex > -1) {
                // namespace changed
                oldNamespace = (NamespaceInfo) oldValues.get(namespaceIndex);
            } else {
                oldNamespace = currNamespace;
            }

            newLayerName = resourceInfo.getPrefixedName();
            if (nameIndex > -1) {
                oldLayerName = (String) oldValues.get(nameIndex);
            } else {
                oldLayerName = resourceInfo.getName();
            }
            oldLayerName = oldNamespace.getPrefix() + ":" + oldLayerName;
        } else {
            // it's a layer group, no need to worry about namespace
            oldLayerName = (String) oldValues.get(nameIndex);
            newLayerName = (String) newValues.get(nameIndex);
        }

        if (!oldLayerName.equals(newLayerName)) {
            catalogConfig.renameTileLayer(oldLayerName, newLayerName);
        }
    }

    /**
     * 
     * @see org.geoserver.catalog.event.CatalogListener#handleRemoveEvent(org.geoserver.catalog.event.CatalogRemoveEvent)
     * @see GWC#removeLayer(String)
     */
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        Object obj = event.getSource();

        String prefixedName = null;

        if (obj instanceof LayerGroupInfo) {
            LayerGroupInfo lgInfo = (LayerGroupInfo) obj;
            prefixedName = lgInfo.getName();
        } else if (obj instanceof LayerInfo) {
            LayerInfo layerInfo = (LayerInfo) obj;
            prefixedName = layerInfo.getResource().getPrefixedName();
        }

        if (null != prefixedName) {
            catalogConfig.removeLayer(prefixedName);
        }
    }

    /**
     * 
     * @see org.geoserver.catalog.event.CatalogListener#reloaded()
     */
    public void reloaded() {
        //
    }

}
