/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWC.tileLayerName;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.gwc.GWC;
import org.geotools.util.logging.Logging;
import org.geowebcache.filter.parameters.ParameterFilter;

/**
 * Listens to changes in {@link StyleInfo styles} for the GeoServer {@link Catalog} and applies the
 * needed {@link ParameterFilter} changes to the corresponding {@link GeoServerTileLayer}.
 *
 * @author Arne Kepp
 * @author Gabriel Roldan
 */
public class CatalogStyleChangeListener implements CatalogListener {

    private static Logger log = Logging.getLogger(CatalogStyleChangeListener.class);

    /**
     * Holds the CatalogModifyEvent from {@link #handleModifyEvent} to be taken after the change was
     * applied to the {@link Catalog} at {@link #handlePostModifyEvent} and check whether it is
     * necessary to perform any action on the cache based on the changed properties
     */
    private static ThreadLocal<CatalogModifyEvent> PRE_MODIFY_EVENT =
            new ThreadLocal<CatalogModifyEvent>();

    private final GWC mediator;

    private Catalog catalog;

    public CatalogStyleChangeListener(final GWC mediator, Catalog catalog) {
        this.mediator = mediator;
        this.catalog = catalog;
    }

    /**
     * @see
     *     org.geoserver.catalog.event.CatalogListener#handleAddEvent(org.geoserver.catalog.event.CatalogAddEvent)
     */
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        // no need to handle style additions, they are added before being attached to a layerinfo
    }

    /**
     * Handles the rename of a style, truncating the cache for any layer that has it as a cached
     * alternate style; modifications to the actual style are handled at {@link
     * #handlePostModifyEvent(CatalogPostModifyEvent)}.
     *
     * <p>When a style is renamed, the {@link LayerInfo} and {@link LayerGroupInfo} that refer to it
     * are not modified, since they refer to the {@link StyleInfo} by id, so they don't really care
     * about the name of the style. For the tiled layer its different, because styles are referred
     * to by {@link GeoServerTileLayerInfoImpl#cachedStyles() name} in order to create the
     * appropriate "STYLES" {@link ParameterFilter parameter filter}. This method will look for any
     * tile layer backed by a {@link LayerInfo} that has a cache for the renamed style as an
     * alternate style (i.e. through a parameter filter) and will truncate the cache for that
     * layer/style. This is so because there's no way in GWC to just rename a style (that would
     * imply getting to the parameter filters that refer to that style in the meta-store and change
     * it's value preserving the parametersId)
     *
     * <p>
     *
     * @see
     *     org.geoserver.catalog.event.CatalogListener#handleModifyEvent(org.geoserver.catalog.event.CatalogModifyEvent)
     */
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        CatalogInfo source = event.getSource();
        if (source instanceof StyleInfo) {
            final List<String> propertyNames = event.getPropertyNames();
            if (!propertyNames.contains("name") && !propertyNames.contains("workspace")) {
                return;
            }
            final int nameIdx = propertyNames.indexOf("name");
            final String oldName =
                    nameIdx != -1 ? (String) event.getOldValues().get(nameIdx) : null;
            final String newName =
                    nameIdx != -1 ? (String) event.getNewValues().get(nameIdx) : null;
            final int workspaceIdx = propertyNames.indexOf("wokspace");
            final String oldWorkspaceName =
                    workspaceIdx != -1 ? (String) event.getOldValues().get(workspaceIdx) : null;
            final String newWorkspaceName =
                    workspaceIdx != -1 ? (String) event.getNewValues().get(workspaceIdx) : null;
            final String oldStyleName = getPrefixedName(oldWorkspaceName, oldName);
            final String newStyleName = getPrefixedName(newWorkspaceName, newName);

            if (nameIdx != -1) {
                // for now, only handle the even if the name changes. most likely we should also do
                // the truncate if
                // the workspace changes too, but that needs a more thorough investigation
                handleStyleRenamed(oldStyleName, newStyleName);
            }
        } else if (source instanceof WorkspaceInfo) {
            PRE_MODIFY_EVENT.set(event);
        }
    }

    private void handleStyleRenamed(final String oldStyleName, final String newStyleName) {
        if (oldStyleName.equals(newStyleName)) {
            return;
        }
        List<GeoServerTileLayer> affectedLayers;
        affectedLayers = mediator.getTileLayersForStyle(oldStyleName);

        for (GeoServerTileLayer tl : affectedLayers) {
            if (!(tl.getPublishedInfo() instanceof LayerInfo)) {
                // no extra styles for layer groups
                continue;
            }

            GeoServerTileLayerInfo info = tl.getInfo();
            ImmutableSet<String> styleNames = info.cachedStyles();
            if (styleNames.contains(oldStyleName)) {
                tl.resetParameterFilters();
                // pity, we don't have a way to just rename a style in GWC
                mediator.truncateByLayerAndStyle(tl.getName(), oldStyleName);
                Set<String> newStyles = new HashSet<String>(styleNames);
                newStyles.remove(oldStyleName);
                newStyles.add(newStyleName);
                LayerInfo layerInfo = (LayerInfo) tl.getPublishedInfo();
                String defaultStyle =
                        layerInfo.getDefaultStyle() == null
                                ? null
                                : layerInfo.getDefaultStyle().prefixedName();
                TileLayerInfoUtil.setCachedStyles(info, defaultStyle, newStyles);

                mediator.save(tl);
            }
        }
    }

    private String getPrefixedName(String workspace, String name) {
        if (workspace != null) {
            return workspace + ":" + name;
        } else {
            return name;
        }
    }

    /**
     * Truncates all tile sets referring the modified {@link StyleInfo}
     *
     * @see org.geoserver.catalog.event.CatalogListener#handlePostModifyEvent
     */
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        Object source = event.getSource();
        if (source instanceof StyleInfo) {
            StyleInfo si = (StyleInfo) source;
            handleStyleChange(si);
        } else if (source instanceof WorkspaceInfo) {
            WorkspaceInfo ws = (WorkspaceInfo) source;
            handleWorkspaceChange(ws);
        }
    }

    private void handleWorkspaceChange(WorkspaceInfo ws) {
        final CatalogModifyEvent preModifyEvent = PRE_MODIFY_EVENT.get();
        PRE_MODIFY_EVENT.remove();

        final List<String> changedProperties = preModifyEvent.getPropertyNames();

        // was the workspace name modified? this implies a name change in workspace local styles
        int nameIdx = changedProperties.indexOf("name");
        if (nameIdx == -1) {
            return;
        }
        String oldWorkspaceName = (String) preModifyEvent.getOldValues().get(nameIdx);
        String newWorkspaceName = (String) preModifyEvent.getNewValues().get(nameIdx);

        // grab the styles
        CloseableIterator<StyleInfo> styles =
                catalog.list(StyleInfo.class, Predicates.equal("workspace.name", newWorkspaceName));
        try {
            while (styles.hasNext()) {
                StyleInfo style = styles.next();
                String oldStyleName = oldWorkspaceName + ":" + style.getName();
                String newStyleName = newWorkspaceName + ":" + style.getName();

                handleStyleRenamed(oldStyleName, newStyleName);
            }
        } finally {
            styles.close();
        }
    }

    /**
     * Options are:
     *
     * <ul>
     *   <li>A {@link LayerInfo} has {@code modifiedStyle} as either its default or style or as one
     *       of its alternate styles
     *   <li>A {@link LayerGroupInfo} contains a layer using {@code modifiedStyle}
     *   <li>{@code modifiedStyle} is explicitly assigned to a {@link LayerGroupInfo}
     * </ul>
     */
    private void handleStyleChange(final StyleInfo modifiedStyle) {
        final String styleName = modifiedStyle.prefixedName();
        log.finer("Handling style modification: " + styleName);
        // First we collect all the layers that use this style
        Iterable<LayerInfo> layers = mediator.getLayerInfosFor(modifiedStyle);
        for (LayerInfo affectedLayer : layers) {
            // If the style name changes, we need to update the layer's parameter filter
            String prefixedName = tileLayerName(affectedLayer);
            log.info(
                    "Truncating layer '"
                            + prefixedName
                            + "' due to a change in style '"
                            + styleName
                            + "'");
            mediator.truncateByLayerAndStyle(prefixedName, styleName);
        }

        // Now we check for layer groups that are affected
        for (LayerGroupInfo layerGroup : mediator.getLayerGroupsFor(modifiedStyle)) {
            String layerGroupName = tileLayerName(layerGroup);
            log.info(
                    "Truncating layer group '"
                            + layerGroupName
                            + "' due to a change in style '"
                            + styleName
                            + "'");
            mediator.truncate(layerGroupName);
        }
    }

    /**
     * No need to do anything here, when a style is removed all the layers that reference it are
     * updated first
     *
     * @see org.geoserver.catalog.event.CatalogListener#handleRemoveEvent
     */
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        //
    }

    /** */
    public void reloaded() {
        //
    }
}
