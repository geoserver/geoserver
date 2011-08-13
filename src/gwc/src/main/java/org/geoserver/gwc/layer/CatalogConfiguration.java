/* Copyright (c) 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMap;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.config.Configuration;
import org.geowebcache.config.meta.ServiceInformation;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.springframework.util.Assert;

/**
 * A GWC's {@link Configuration} implementation that provides {@link TileLayer}s directly from the
 * GeoServer {@link Catalog}'s {@link LayerInfo}s and {@link LayerGroupInfo}s.
 * <p>
 * The sole responsibility of the class is to
 * 
 * @see #createLayer(LayerInfo)
 * @see #createLayer(LayerGroupInfo)
 * @see #getTileLayers(boolean)
 * @see CatalogStyleChangeListener
 */
public class CatalogConfiguration implements Configuration {

    private static Logger log = Logging.getLogger(CatalogConfiguration.class);

    private static Map<String, Response> cachedTileEncoders = new HashMap<String, Response>();

    private Catalog catalog;

    private CatalogLayerEventListener catalogLayerEventListener;

    private CatalogStyleChangeListener catalogStyleChangeListener;

    private final GWCConfigPersister configProvider;

    private final GridSetBroker gridSetBroker;

    private final WMS wmsFacade;

    /**
     * 
     * @param mediator
     */
    public CatalogConfiguration(final Catalog catalog, final GWCConfigPersister configProvider,
            final GridSetBroker gridSetBroker, final WMS wmsFacade) {
        Assert.notNull(catalog);
        Assert.notNull(configProvider);
        Assert.notNull(gridSetBroker);
        this.catalog = catalog;
        this.configProvider = configProvider;
        this.gridSetBroker = gridSetBroker;
        this.wmsFacade = wmsFacade;

        this.catalogLayerEventListener = new CatalogLayerEventListener(this);
        this.catalogStyleChangeListener = new CatalogStyleChangeListener(this);
        catalog.addListener(catalogLayerEventListener);
        catalog.addListener(catalogStyleChangeListener);
    }

    /**
     * 
     * @see org.geowebcache.config.Configuration#getIdentifier()
     */
    public String getIdentifier() {
        return "GeoServer Catalog Configuration";
    }

    private GWC getGWC() {
        return GWC.get();
    }

    /**
     * @see org.geowebcache.config.Configuration#getServiceInformation()
     * @return {@code null}
     */
    public ServiceInformation getServiceInformation() throws GeoWebCacheException {
        return null;
    }

    /**
     * @see org.geowebcache.config.Configuration#isRuntimeStatsEnabled()
     */
    public boolean isRuntimeStatsEnabled() {
        return true;
    }

    /**
     * Returns the list of {@link GeoServerTileLayer} objects matching the GeoServer ones.
     * <p>
     * The list is built dynamically on each call.
     * </p>
     * 
     * @see org.geowebcache.config.Configuration#getTileLayers(boolean)
     * @see #createLayer(LayerInfo)
     * @see #createLayer(LayerGroupInfo)
     * @see org.geowebcache.config.Configuration#getTileLayers()
     */
    public List<GeoServerTileLayer> getTileLayers() {
        List<LayerGroupInfo> layerGroups = catalog.getLayerGroups();
        List<LayerInfo> layerInfos = catalog.getLayers();
        List[] sublists = { layerInfos, layerGroups };
        CompositeList composite = new CompositeList(sublists);
        LazyGeoServerTileLayerList tileLayers = new LazyGeoServerTileLayerList(composite, this);
        return tileLayers;
    }

    /**
     * Returns a dynamic list of cached layer names out of the GeoServer {@link Catalog}
     * 
     * @see org.geowebcache.config.Configuration#getTileLayerNames()
     */
    public Set<String> getTileLayerNames() {
        Set<String> names = new HashSet<String>();
        for (LayerGroupInfo lgi : catalog.getLayerGroups()) {
            names.add(lgi.getName());
        }
        for (LayerInfo li : catalog.getLayers()) {
            names.add(li.getResource().getPrefixedName());
        }
        return names;
    }

    /**
     * @see org.geowebcache.config.Configuration#getTileLayer(java.lang.String)
     */
    public GeoServerTileLayer getTileLayer(final String layerName) {
        // System.err.println("Returning new GeoServerTileLayer " + layerName);
        // return layers.get(layerName);
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        if (layerInfo != null) {
            return new GeoServerTileLayer(this, layerInfo);
        }
        LayerGroupInfo lgi = catalog.getLayerGroupByName(layerName);
        if (lgi != null) {
            return new GeoServerTileLayer(this, lgi);
        }
        return null;
    }

    /**
     * @see org.geowebcache.config.Configuration#getTileLayerCount()
     */
    public int getTileLayerCount() {
        List<LayerGroupInfo> layerGroups = catalog.getLayerGroups();
        List<LayerInfo> layerInfos = catalog.getLayers();
        int count = layerGroups.size() + layerInfos.size();
        return count;
    }

    /**
     * @see org.geowebcache.config.Configuration#remove(java.lang.String)
     */
    public boolean remove(String layerName) {
        // nothing to remove, we're lazy, but let TileLayerDispatcher continue trying with the other
        // configurations is may hold
        return false;
    }

    private static class CompositeList extends AbstractList<Object> {

        private final List<Object>[] decorated;

        @SuppressWarnings("unchecked")
        public CompositeList(List[] sublists) {
            this.decorated = sublists;
        }

        @Override
        public Object get(final int index) {
            int subIndex = index;
            List<Object> sublist;
            for (int i = 0; i < decorated.length; i++) {
                sublist = decorated[i];
                if (subIndex < sublist.size()) {
                    return sublist.get(subIndex);
                }
                subIndex -= sublist.size();
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int size() {
            int size = 0;
            List<Object> sublist;
            for (int i = 0; i < decorated.length; i++) {
                sublist = decorated[i];
                size += sublist.size();
            }
            return size;
        }
    }

    private static class LazyGeoServerTileLayerList extends AbstractList<GeoServerTileLayer> {

        private final List<Object> infos;

        private final CatalogConfiguration mediator;

        public LazyGeoServerTileLayerList(final List<Object> infos,
                final CatalogConfiguration catalogConfiguration) {
            this.infos = infos;
            this.mediator = catalogConfiguration;
        }

        @Override
        public GeoServerTileLayer get(int index) {
            Object object = infos.get(index);
            if (object instanceof LayerInfo) {
                return new GeoServerTileLayer(mediator, (LayerInfo) object);
            } else if (object instanceof LayerGroupInfo) {
                return new GeoServerTileLayer(mediator, (LayerGroupInfo) object);
            }
            throw new IllegalStateException();
        }

        @Override
        public int size() {
            return infos.size();
        }

    }

    public GWCConfig getConfig() {
        return configProvider.getConfig();
    }

    public GridSetBroker getGridSetBroker() {
        return gridSetBroker;
    }

    public void save(GeoServerTileLayer layer) {
        log.info("Saving " + layer.getName());
        MetadataMap metadata;
        LayerInfo layerInfo = layer.getLayerInfo();
        LayerGroupInfo layerGroupInfo = layer.getLayerGroupInfo();
        if (layerInfo == null) {
            metadata = layerGroupInfo.getMetadata();
        } else {
            metadata = layerInfo.getMetadata();
        }

        GeoServerTileLayerInfo tileLayerInfo = layer.getInfo();
        tileLayerInfo.saveTo(metadata);

        if (layerInfo != null) {
            catalog.save(layerInfo);
        } else {
            catalog.save(layerGroupInfo);
        }
    }

    public boolean isQueryable(final GeoServerTileLayer geoServerTileLayer) {
        LayerInfo layerInfo = geoServerTileLayer.getLayerInfo();
        if (layerInfo != null) {
            return wmsFacade.isQueryable(layerInfo);
        }
        LayerGroupInfo lgi = geoServerTileLayer.getLayerGroupInfo();
        return wmsFacade.isQueryable(lgi);
    }

    /**
     * @param cookies
     * @see GWC#dispatchOwsRequest(Map, Cookie[])
     */
    public Resource dispatchOwsRequest(Map<String, String> params, Cookie[] cookies)
            throws Exception {
        return getGWC().dispatchOwsRequest(params, cookies);
    }

    /**
     * @return the {@link LayerInfo} based on the given {@link LayerInfo#getId() layerId}
     */
    public LayerInfo getLayerInfoById(final String layerId) {
        return catalog.getLayer(layerId);
    }

    public LayerGroupInfo getLayerGroupById(final String layerGroupId) {
        return catalog.getLayerGroup(layerGroupId);
    }

    public void renameTileLayer(final String oldLayerName, final String newLayerName) {
        getGWC().layerRenamed(oldLayerName, newLayerName);
    }

    /**
     * @see GWC#truncate(String, String)
     */
    public void truncate(String layerName, String styleName) {
        getGWC().truncate(layerName, styleName);
    }

    /**
     * @see GWC#truncate(String)
     */
    public void truncate(String layerName) {
        getGWC().truncate(layerName);
    }

    /**
     * @see GWC#layerRemoved(String)
     */
    public void removeLayer(final String tileLayerName) {
        getGWC().layerRemoved(tileLayerName);
    }

    /**
     * LayerInfo has been created, add a matching {@link GeoServerTileLayer}
     * 
     * @see CatalogLayerEventListener#handleAddEvent
     * @see CatalogConfiguration#createLayer(LayerInfo)
     * @see GWC#layerAdded(GeoServerTileLayer)
     */
    public void createLayer(LayerInfo layerInfo) {
        // /GeoServerTileLayer tileLayer = embeddedConfig.createLayer(layerInfo);
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(this, layerInfo);
        save(tileLayer);
        getGWC().layerAdded(tileLayer);
    }

    /**
     * LayerGroupInfo has been created, add a matching {@link GeoServerTileLayer}
     * 
     * @see CatalogLayerEventListener#handleAddEvent
     * @see CatalogConfiguration#createLayer(LayerGroupInfo)
     * @see GWC#layerAdded(GeoServerTileLayer)
     */
    public void createLayer(LayerGroupInfo lgi) {
        // /GeoServerTileLayer tileLayer = embeddedConfig.createLayer(lgi);
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(this, lgi);
        save(tileLayer);
        getGWC().layerAdded(tileLayer);
    }

    /**
     * Returns the tile layers that refer to the given style, either as the tile layer's
     * {@link GeoServerTileLayer#getStyles() default style} or one of the
     * {@link GeoServerTileLayerInfo#getCachedStyles() cached styles}.
     * <p>
     * The result may be different from {@link #getLayerInfosFor(StyleInfo)} and
     * {@link #getLayerGroupsFor(StyleInfo)} as the {@link GeoServerTileLayerInfo}'s backing each
     * {@link GeoServerTileLayer} may have assigned a subset of the layerinfo styles for caching.
     * </p>
     */
    public List<GeoServerTileLayer> getTileLayersForStyle(final String styleName) {
        List<GeoServerTileLayer> tileLayers = getTileLayers();

        List<GeoServerTileLayer> affected = new ArrayList<GeoServerTileLayer>();
        for (GeoServerTileLayer tl : tileLayers) {
            GeoServerTileLayerInfo info = tl.getInfo();
            String defaultStyle = tl.getStyles();// may be null if backed by a LayerGroupInfo
            Set<String> cachedStyles = info.getCachedStyles();
            if (styleName.equals(defaultStyle) || cachedStyles.contains(styleName)) {
                affected.add(tl);
            }
        }
        return affected;
    }

    /**
     * @return all the {@link LayerInfo}s in the {@link Catalog} that somehow refer to the given
     *         style
     */
    public Iterable<LayerInfo> getLayerInfosFor(final StyleInfo style) {
        final String styleName = style.getName();
        List<LayerInfo> result = new ArrayList<LayerInfo>();
        {
            List<LayerInfo> layers = catalog.getLayers();
            for (LayerInfo layer : layers) {
                String name = layer.getDefaultStyle().getName();
                if (styleName.equals(name)) {
                    result.add(layer);
                    continue;
                }
                for (StyleInfo alternateStyle : layer.getStyles()) {
                    name = alternateStyle.getName();
                    if (styleName.equals(name)) {
                        result.add(layer);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * @return all the layergroups that somehow refer to the given style
     */
    public Iterable<LayerGroupInfo> getLayerGroupsFor(final StyleInfo style) {
        List<LayerGroupInfo> layerGroups = new ArrayList<LayerGroupInfo>();
        for (LayerGroupInfo layerGroup : catalog.getLayerGroups()) {

            final List<StyleInfo> explicitLayerGroupStyles = layerGroup.getStyles();
            final List<LayerInfo> groupLayers = layerGroup.getLayers();

            for (int layerN = 0; layerN < groupLayers.size(); layerN++) {

                LayerInfo childLayer = groupLayers.get(layerN);
                StyleInfo assignedLayerStyle = explicitLayerGroupStyles.get(layerN);
                if (assignedLayerStyle == null) {
                    assignedLayerStyle = childLayer.getDefaultStyle();
                }

                if (style.equals(assignedLayerStyle)) {
                    layerGroups.add(layerGroup);
                    break;
                }
            }
        }
        return layerGroups;
    }

    /**
     * @see org.geowebcache.config.Configuration#initialize(org.geowebcache.grid.GridSetBroker)
     */
    public int initialize(GridSetBroker gridSetBroker) throws GeoWebCacheException {
        return getTileLayerCount();
    }

    @SuppressWarnings("unchecked")
    public Response getResponseEncoder(final MimeType responseFormat, final WebMap webMap) {
        final String format = responseFormat.getFormat();
        final String mimeType = responseFormat.getMimeType();

        Response response = cachedTileEncoders.get(format);
        if (response == null) {
            final Operation operation;
            {
                GetMapRequest getMap = new GetMapRequest();
                getMap.setFormat(mimeType);
                Object[] parameters = { getMap };
                Service service = (Service) GeoServerExtensions.bean("wms-1_1_1-ServiceDescriptor");
                if (service == null) {
                    throw new IllegalStateException(
                            "Didn't find service descriptor 'wms-1_1_1-ServiceDescriptor'");
                }
                operation = new Operation("GetMap", service, (Method) null, parameters);
            }

            final List<Response> extensions = GeoServerExtensions.extensions(Response.class);
            final Class<?> webMapClass = webMap.getClass();
            for (Response r : extensions) {
                if (r.getBinding().isAssignableFrom(webMapClass) && r.canHandle(operation)) {
                    synchronized (cachedTileEncoders) {
                        cachedTileEncoders.put(mimeType, r);
                        response = r;
                        break;
                    }
                }
            }
            if (response == null) {
                throw new IllegalStateException("Didn't find a " + Response.class.getName()
                        + " to handle " + mimeType);
            }
        }
        return response;
    }

}
