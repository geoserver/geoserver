/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.geoserver.gwc.GWC.tileLayerName;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.util.LangUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geotools.util.logging.Logging;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.storage.StorageBroker;

/**
 * Listens to {@link Catalog} layers added/removed events and adds/removes {@link
 * GeoServerTileLayer}s to/from the {@link CatalogConfiguration}
 *
 * <p>Handles the following cases:
 *
 * <ul>
 *   <li><b>Layer added</b>: a {@link LayerInfo} or {@link LayerGroupInfo} has been added. A {@link
 *       GeoServerTileLayer} is {@link #createTileLayer is created} with the {@link GWCConfig
 *       default settings} only if the integrated GWC configuration is set to {@link
 *       GWCConfig#isCacheLayersByDefault() cache layers by default}.
 *   <li><b>Layer removed</b>: a {@code LayerInfo} or {@code LayerGroupInfo} has been removed. GWC
 *       is instructed to remove the layer, deleting it's cache and any other associated information
 *       completely (for example, the disk quota information for the layer is also deleted and the
 *       global usage updated accordingly).
 *   <li><b>Layer renamed</b>: a {@link LayerInfo} or {@link LayerGroupInfo} has been renamed. GWC
 *       is {@link StorageBroker#rename instructed to rename} the corresponding tile layer
 *       preserving the cache and any other information (usage statistics, disk quota usage, etc).
 *   <li><b>Workspace renamed</b>: a {@link WorkspaceInfo} as been renamed. GWC is {@link
 *       StorageBroker#rename instructed to rename} all the corresponding tile layer associated to
 *       the workspace, preserving the cache and any other information (usage statistics, disk quota
 *       usage, etc).
 *   <li><b>Namespace changed</b>: a {@link ResourceInfo} has been assigned to a different {@link
 *       NamespaceInfo namespace}. As the GWC tile layers are named after the resource's {@link
 *       ResourceInfo#prefixedName() prefixed name} and not only after the {@link
 *       LayerInfo#getName()} (at least until GeoServer separates out data from publication - the
 *       famous data/publish split), GWC is instructed to rename the layer preserving the cache and
 *       any other information for the layer.
 *   <li><b>LayerGroupInfo modified</b>: either the {@link LayerGroupInfo#layers() layers} or {@link
 *       LayerGroupInfo#styles() styles} changed for a {@code LayerGroupInfo}. It's cache is
 *       truncated.
 *   <li><b>LayerInfo default style replaced</b>: a {@code LayerInfo} has been assigned a different
 *       {@link LayerInfo#getDefaultStyle() default style}. The corresponding tile layer's cache is
 *       truncated for the default style.
 *   <li><b>LayerInfo alternate styles changed</b> the set of a {@code LayerInfo}'s {@link
 *       LayerInfo#getStyles() alternate styles} has been modified. For any added style, if the
 *       {@link GeoServerTileLayer} is configured to {@link
 *       GeoServerTileLayerInfo#isAutoCacheStyles() automatically cache all styles}, the style name
 *       is added to the set of {@link GeoServerTileLayerInfo#cachedStyles() cached styles}. For any
 *       <b>removed</b> style, if it was one of the {@link GeoServerTileLayerInfo#cachedStyles()
 *       cached styles}, the layer's cache for that style is truncated, and it's removed from the
 *       tile layer's set of cached styles. Subsequently, the {@link GeoServerTileLayer} will create
 *       a {@link StringParameterFilter "STYLES" parameter filter} for all the cached styles on
 *       demand
 * </ul>
 *
 * @author Arne Kepp
 * @author Gabriel Roldan
 */
public class CatalogLayerEventListener implements CatalogListener {

    private static Logger log = Logging.getLogger(CatalogLayerEventListener.class);

    private final GWC mediator;

    private final Catalog catalog;

    /**
     * Holds the CatalogModifyEvent from {@link #handleModifyEvent} to be taken after the change was
     * applied to the {@link Catalog} at {@link #handlePostModifyEvent} and check whether it is
     * necessary to perform any action on the cache based on the changed properties
     */
    private static ThreadLocal<CatalogModifyEvent> PRE_MODIFY_EVENT =
            new ThreadLocal<CatalogModifyEvent>();

    private static ThreadLocal<GeoServerTileLayerInfo> PRE_MODIFY_TILELAYER =
            new ThreadLocal<GeoServerTileLayerInfo>();

    public CatalogLayerEventListener(final GWC mediator, Catalog catalog) {
        this.mediator = mediator;
        this.catalog = catalog;
    }

    /**
     * If either a {@link LayerInfo} or {@link LayerGroupInfo} has been added to the {@link Catalog}
     * , create a corresponding GWC TileLayer depending on the value of {@link
     * GWCConfig#isCacheLayersByDefault()}.
     *
     * @see org.geoserver.catalog.event.CatalogListener#handleAddEvent
     * @see #createTileLayer(LayerInfo)
     * @see #createTileLayer(LayerGroupInfo)
     */
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        GWCConfig config = mediator.getConfig();
        boolean sane = config.isSane();
        boolean cacheLayersByDefault = config.isCacheLayersByDefault();
        if (!cacheLayersByDefault) {
            return;
        }
        if (!sane) {
            log.info(
                    "Ignoring auto-creation of tile layer for "
                            + event.getSource()
                            + ": global gwc settings are not sane");
        }
        Object obj = event.getSource();
        // We only handle layers here. Layer groups are initially empty
        if (obj instanceof LayerInfo) {
            log.finer("Handling add event: " + obj);
            LayerInfo layerInfo = (LayerInfo) obj;
            createTileLayer(layerInfo);
        } else if (obj instanceof LayerGroupInfo) {
            LayerGroupInfo lgi = (LayerGroupInfo) obj;
            createTileLayer(lgi);
        }
    }

    /**
     * LayerInfo has been created, add a matching {@link GeoServerTileLayer}
     *
     * @see CatalogLayerEventListener#handleAddEvent
     * @see GWC#add(GeoServerTileLayer)
     */
    void createTileLayer(final LayerInfo layerInfo) {
        GWCConfig defaults = mediator.getConfig();
        if (defaults.isSane() && defaults.isCacheLayersByDefault()) {
            GridSetBroker gridSetBroker = mediator.getGridSetBroker();
            GeoServerTileLayer tileLayer =
                    new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
            mediator.add(tileLayer);
        }
    }

    /**
     * LayerGroupInfo has been created, add a matching {@link GeoServerTileLayer}
     *
     * @see CatalogLayerEventListener#handleAddEvent
     * @see GWC#add(GeoServerTileLayer)
     */
    public void createTileLayer(LayerGroupInfo lgi) {
        GWCConfig defaults = mediator.getConfig();
        GridSetBroker gridSetBroker = mediator.getGridSetBroker();
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(lgi, defaults, gridSetBroker);
        mediator.add(tileLayer);
    }

    /**
     * @see
     *     org.geoserver.catalog.event.CatalogListener#handleModifyEvent(org.geoserver.catalog.event.CatalogModifyEvent)
     * @see #handlePostModifyEvent
     */
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        CatalogInfo source = event.getSource();
        if (source instanceof LayerInfo
                || source instanceof LayerGroupInfo
                || source instanceof FeatureTypeInfo
                || source instanceof CoverageInfo
                || source instanceof WMSLayerInfo
                || source instanceof WorkspaceInfo) {
            PRE_MODIFY_EVENT.set(event);

            if (mediator.hasTileLayer(source)) {
                try {
                    GeoServerTileLayer tileLayer = mediator.getTileLayer(source);
                    GeoServerTileLayerInfo tileLayerInfo = tileLayer.getInfo();
                    PRE_MODIFY_TILELAYER.set(tileLayerInfo);
                } catch (RuntimeException e) {
                    log.info("Ignoring misconfigured tile layer info for " + source);
                }
            }
        }
    }

    /**
     * In case the event refers to the addition or removal of a {@link LayerInfo} or {@link
     * LayerGroupInfo} adds or removes the corresponding {@link GeoServerTileLayer} through {@link
     * #createTileLayer}.
     *
     * <p>Note this method does not discriminate whether the change in the layer or layergroup
     * deserves a change in its matching TileLayer, it just re-creates the TileLayer
     *
     * @see
     *     org.geoserver.catalog.event.CatalogListener#handlePostModifyEvent(org.geoserver.catalog.event.CatalogPostModifyEvent)
     */
    public void handlePostModifyEvent(final CatalogPostModifyEvent event) throws CatalogException {
        final CatalogInfo source = event.getSource();
        if (!(source instanceof LayerInfo
                || source instanceof LayerGroupInfo
                || source instanceof FeatureTypeInfo
                || source instanceof CoverageInfo
                || source instanceof WMSLayerInfo
                || source instanceof WorkspaceInfo)) {
            return;
        }

        final GeoServerTileLayerInfo tileLayerInfo = PRE_MODIFY_TILELAYER.get();
        PRE_MODIFY_TILELAYER.remove();

        final CatalogModifyEvent preModifyEvent = PRE_MODIFY_EVENT.get();
        PRE_MODIFY_EVENT.remove();

        if (tileLayerInfo == null && !(source instanceof WorkspaceInfo)) {
            return; // no tile layer associated, no need to continue
        }
        if (preModifyEvent == null) {
            throw new IllegalStateException(
                    "PostModifyEvent called without having called handlePreModify first?");
        }

        final List<String> changedProperties = preModifyEvent.getPropertyNames();
        final List<Object> oldValues = preModifyEvent.getOldValues();
        final List<Object> newValues = preModifyEvent.getNewValues();

        log.finer("Handling modify event for " + source);
        if (source instanceof ResourceInfo || source instanceof LayerGroupInfo) {
            /*
             * Handle changing the filter definition, this is the kind of change that affects the
             * full output contents
             */
            if (changedProperties.contains("cqlFilter") && source instanceof FeatureTypeInfo) {
                mediator.truncate(((FeatureTypeInfo) source).prefixedName());
            }

            /*
             * Handle the rename case. For LayerInfos it's actually the related ResourceInfo what
             * gets renamed, at least until the data/publish split is implemented in GeoServer. For
             * LayerGroupInfo it's either the group name itself or its workspace
             */
            if (changedProperties.contains("name")
                    || changedProperties.contains("namespace")
                    || changedProperties.contains("workspace")) {
                handleRename(tileLayerInfo, source, changedProperties, oldValues, newValues);
            }
        } else if (source instanceof WorkspaceInfo) {
            if (changedProperties.contains("name")) {
                handleWorkspaceRename(source, changedProperties, oldValues, newValues);
            }
        }

        if (source instanceof LayerInfo) {
            final LayerInfo li = (LayerInfo) source;

            handleLayerInfoChange(changedProperties, oldValues, newValues, li, tileLayerInfo);

        } else if (source instanceof LayerGroupInfo) {
            LayerGroupInfo lgInfo = (LayerGroupInfo) source;
            handleLayerGroupInfoChange(
                    changedProperties, oldValues, newValues, lgInfo, tileLayerInfo);
        }
    }

    private void handleLayerGroupInfoChange(
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues,
            final LayerGroupInfo lgInfo,
            final GeoServerTileLayerInfo tileLayerInfo) {

        checkNotNull(lgInfo);
        checkNotNull(tileLayerInfo);

        final String layerName = tileLayerName(lgInfo);

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
            log.info(
                    "Truncating TileLayer for layer group '"
                            + layerName
                            + "' due to a change in its layers or styles");
            mediator.truncate(layerName);
        }
    }

    /**
     * Handles changes of interest to GWC on a {@link LayerInfo}.
     *
     * <ul>
     *   <li>If the name of the default style changed, then the layer's cache for the default style
     *       is truncated. This method doesn't check if the contents of the styles are equal. That
     *       is handled by {@link CatalogStyleChangeListener} whenever a style is modified.
     *   <li>If the tile layer is {@link GeoServerTileLayerInfo#isAutoCacheStyles() auto caching
     *       styles} and the layerinfo's "styles" list changed, the tile layer's STYLE parameter
     *       filter is updated to match the actual list of layer styles and any removed style is
     *       truncated.
     * </ul>
     */
    private void handleLayerInfoChange(
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues,
            final LayerInfo li,
            final GeoServerTileLayerInfo tileLayerInfo) {
        checkNotNull(tileLayerInfo);

        final String layerName = tileLayerName(li);

        boolean save = false;
        boolean defaultStyleChanged = false;

        final String defaultStyle;

        /*
         * If default style name changed
         */
        if (changedProperties.contains("defaultStyle")) {
            final int propIndex = changedProperties.indexOf("defaultStyle");
            final StyleInfo oldStyle = (StyleInfo) oldValues.get(propIndex);
            final StyleInfo newStyle = (StyleInfo) newValues.get(propIndex);

            final String oldStyleName = oldStyle.prefixedName();
            defaultStyle = newStyle.prefixedName();
            if (!Objects.equal(oldStyleName, defaultStyle)) {
                save = true;
                defaultStyleChanged = true;
                log.info(
                        "Truncating default style for layer "
                                + layerName
                                + ", as it changed from "
                                + oldStyleName
                                + " to "
                                + defaultStyle);
                mediator.truncateByLayerDefaultStyle(layerName);
            }
        } else {
            StyleInfo styleInfo = li.getDefaultStyle();
            defaultStyle = styleInfo == null ? null : styleInfo.prefixedName();
        }

        if (tileLayerInfo.isAutoCacheStyles()) {
            Set<String> styles = new HashSet<String>();
            for (StyleInfo s : li.getStyles()) {
                styles.add(s.prefixedName());
            }
            styles.add(defaultStyle);
            ImmutableSet<String> cachedStyles = tileLayerInfo.cachedStyles();
            if (!styles.equals(cachedStyles)) {
                // truncate no longer existing cached styles
                Set<String> notCachedAnyMore = Sets.difference(cachedStyles, styles);
                for (String oldCachedStyle : notCachedAnyMore) {
                    log.info(
                            "Truncating cached style "
                                    + oldCachedStyle
                                    + " of layer "
                                    + layerName
                                    + " as it's no longer one of the layer's styles");
                    mediator.truncateByLayerAndStyle(layerName, oldCachedStyle);
                }
                // reset STYLES parameter filter
                TileLayerInfoUtil.checkAutomaticStyles(li, tileLayerInfo);
                save = true;
            }
        }

        // check the caching settings, have they changed?
        boolean cachingInfoChanged = false;
        int metadataIdx = changedProperties.indexOf("metadata");
        if (metadataIdx >= 0) {
            MetadataMap oldMetadata = (MetadataMap) oldValues.get(metadataIdx);
            MetadataMap newMetadata = (MetadataMap) newValues.get(metadataIdx);
            boolean cachingEnabledChanged =
                    LangUtils.equals(
                            oldMetadata.get(ResourceInfo.CACHING_ENABLED, Boolean.class),
                            newMetadata.get(ResourceInfo.CACHING_ENABLED, Boolean.class));
            boolean cachingMaxAgeChanged =
                    LangUtils.equals(
                            oldMetadata.get(ResourceInfo.CACHE_AGE_MAX, Boolean.class),
                            newMetadata.get(ResourceInfo.CACHE_AGE_MAX, Boolean.class));
            // we do we don't need to truncate the layer, but we need to update
            // its LayerInfo so that the resulting caching headers get updated
            if (cachingEnabledChanged || cachingMaxAgeChanged) {
                cachingInfoChanged = true;
                save = true;
            }
        }

        if (save) {
            GridSetBroker gridSetBroker = mediator.getGridSetBroker();
            GeoServerTileLayer tileLayer = new GeoServerTileLayer(li, gridSetBroker, tileLayerInfo);
            mediator.save(tileLayer);
        }
        // caching info and default style changes affect also the layer groups containing the layer
        if (cachingInfoChanged || defaultStyleChanged) {
            List<LayerGroupInfo> groups = catalog.getLayerGroups();
            for (LayerGroupInfo lg : groups) {
                GeoServerTileLayer tileLayer = mediator.getTileLayer(lg);
                if (tileLayer != null) {
                    LayerGroupHelper helper = new LayerGroupHelper(lg);
                    int idx = helper.allLayers().indexOf(li);
                    if (idx >= 0) {
                        // we need to save in case something changed in one of the layer
                        GridSetBroker gridSetBroker = mediator.getGridSetBroker();
                        GeoServerTileLayerInfo groupTileLayerInfo = tileLayer.getInfo();
                        GeoServerTileLayer newTileLayer =
                                new GeoServerTileLayer(lg, gridSetBroker, groupTileLayerInfo);
                        mediator.save(newTileLayer);

                        // we also need to truncate the group if the layer default style changed,
                        // and the layer group was using
                        if (defaultStyleChanged && lg.getStyles().get(idx) == null) {
                            mediator.truncate(groupTileLayerInfo.getName());
                        }
                    }
                }
            }
        }
    }

    private void handleWorkspaceRename(
            final CatalogInfo source,
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues) {
        final int nameIndex = changedProperties.indexOf("name");
        final String oldWorkspaceName = (String) oldValues.get(nameIndex);
        final String newWorkspaceName = (String) newValues.get(nameIndex);

        // handle layers rename
        CloseableIterator<LayerInfo> layers =
                catalog.list(
                        LayerInfo.class,
                        Predicates.equal("resource.store.workspace.name", newWorkspaceName));
        try {
            while (layers.hasNext()) {
                LayerInfo layer = layers.next();
                String oldName = oldWorkspaceName + ":" + layer.getName();
                String newName = newWorkspaceName + ":" + layer.getName();

                // see if the tile layer existed and it is one that we can rename (admin
                // could have overwritten it with a direct layer in geowebcache.xml)
                TileLayer tl;
                try {
                    tl = mediator.getTileLayerByName(oldName);
                    if (!(tl instanceof GeoServerTileLayer)) {
                        continue;
                    }
                } catch (IllegalArgumentException e) {
                    // this happens if the layer is not there, move on
                    continue;
                }

                try {
                    if (layer.getType() == PublishedType.VECTOR
                            && ((FeatureTypeInfo) layer.getResource())
                                            .getFeatureType()
                                            .getGeometryDescriptor()
                                    == null) {
                        // skip geometryless layers
                        continue;
                    }
                } catch (IOException e) {
                    // this should not happen...
                    log.log(
                            Level.FINE,
                            "Failed to determine if layer"
                                    + layer
                                    + " is geometryless while renaming tile layers for workspace name change "
                                    + oldName
                                    + " -> "
                                    + newName,
                            e);
                }

                try {
                    if (tl instanceof GeoServerTileLayer) {
                        GeoServerTileLayer gstl = (GeoServerTileLayer) tl;
                        renameTileLayer(gstl.getInfo(), oldName, newName);
                    }
                } catch (Exception e) {
                    // this should not happen, but we don't want to
                    log.log(
                            Level.WARNING,
                            "Failed to rename tile layer for geoserver layer "
                                    + layer
                                    + " while renaming tile layers for workspace name change "
                                    + oldName
                                    + " -> "
                                    + newName,
                            e);
                }
            }
        } finally {
            layers.close();
        }

        // handle layer group renames
        CloseableIterator<LayerGroupInfo> groups =
                catalog.list(
                        LayerGroupInfo.class, Predicates.equal("workspace.name", newWorkspaceName));
        try {
            while (groups.hasNext()) {
                LayerGroupInfo group = groups.next();
                String oldName = oldWorkspaceName + ":" + group.getName();
                String newName = newWorkspaceName + ":" + group.getName();

                // see if the tile layer existed and it is one that we can rename (admin
                // could have overwritten it with a direct layer in geowebcache.xml)
                TileLayer tl;
                try {
                    tl = mediator.getTileLayerByName(oldName);
                    if (!(tl instanceof GeoServerTileLayer)) {
                        continue;
                    }
                } catch (IllegalArgumentException e) {
                    // this happens if the layer is not there, move on
                    continue;
                }

                try {
                    if (tl instanceof GeoServerTileLayer) {
                        GeoServerTileLayer gstl = (GeoServerTileLayer) tl;
                        renameTileLayer(gstl.getInfo(), oldName, newName);
                    }
                } catch (Exception e) {
                    // this should not happen, but we don't want to
                    log.log(
                            Level.WARNING,
                            "Failed to rename tile layer for geoserver group "
                                    + group
                                    + " while renaming tile layers for workspace name change "
                                    + oldName
                                    + " -> "
                                    + newName,
                            e);
                }
            }
        } finally {
            groups.close();
        }
    }

    private void handleRename(
            final GeoServerTileLayerInfo tileLayerInfo,
            final CatalogInfo source,
            final List<String> changedProperties,
            final List<Object> oldValues,
            final List<Object> newValues) {

        final int nameIndex = changedProperties.indexOf("name");
        final int namespaceIndex = changedProperties.indexOf("namespace");

        String oldLayerName;
        String newLayerName;
        if (source instanceof ResourceInfo) { // covers LayerInfo, CoverageInfo, and WMSLayerInfo
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

            newLayerName = resourceInfo.prefixedName();
            if (nameIndex > -1) {
                oldLayerName = (String) oldValues.get(nameIndex);
            } else {
                oldLayerName = resourceInfo.getName();
            }
            oldLayerName = oldNamespace.getPrefix() + ":" + oldLayerName;
        } else {
            // it's a layer group, no need to worry about namespace
            oldLayerName = tileLayerInfo.getName();
            newLayerName = tileLayerName((LayerGroupInfo) source);
        }

        if (!oldLayerName.equals(newLayerName)) {
            renameTileLayer(tileLayerInfo, oldLayerName, newLayerName);
        }
    }

    private void renameTileLayer(
            final GeoServerTileLayerInfo tileLayerInfo, String oldLayerName, String newLayerName) {
        tileLayerInfo.setName(newLayerName);

        // notify the mediator of the rename so it changes the name of the layer in GWC without
        // affecting its caches
        final GeoServerTileLayer oldTileLayer =
                (GeoServerTileLayer) mediator.getTileLayerByName(oldLayerName);

        checkState(
                null != oldTileLayer,
                "handleRename: old tile layer not found: '"
                        + oldLayerName
                        + "'. New name: '"
                        + newLayerName
                        + "'");

        mediator.rename(oldLayerName, newLayerName);
    }

    /**
     * @see
     *     org.geoserver.catalog.event.CatalogListener#handleRemoveEvent(org.geoserver.catalog.event.CatalogRemoveEvent)
     * @see GWC#removeTileLayers(List)
     */
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        CatalogInfo obj = event.getSource();
        if (!(obj instanceof LayerInfo || obj instanceof LayerGroupInfo)) {
            return;
        }
        if (!mediator.hasTileLayer(obj)) {
            return;
        }

        String prefixedName = null;

        if (obj instanceof LayerGroupInfo) {
            LayerGroupInfo lgInfo = (LayerGroupInfo) obj;
            prefixedName = tileLayerName(lgInfo);
        } else if (obj instanceof LayerInfo) {
            LayerInfo layerInfo = (LayerInfo) obj;
            prefixedName = tileLayerName(layerInfo);
        }

        if (null != prefixedName) {
            // notify the layer has been removed
            mediator.removeTileLayers(Arrays.asList(prefixedName));
        }
    }

    /** @see org.geoserver.catalog.event.CatalogListener#reloaded() */
    public void reloaded() {
        //
    }
}
