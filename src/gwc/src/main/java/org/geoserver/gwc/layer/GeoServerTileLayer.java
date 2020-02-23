/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static org.geoserver.gwc.GWC.tileLayerName;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import java.awt.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.dispatch.GwcServiceDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RequestInfo;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.capabilities.CapabilityUtil;
import org.geoserver.wms.capabilities.LegendSample;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.config.legends.LegendInfoBuilder;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.filter.parameters.ParameterException;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.request.RequestFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.OutsideCoverageException;
import org.geowebcache.grid.SRS;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.ExpirationRule;
import org.geowebcache.layer.LayerListenerList;
import org.geowebcache.layer.MetaTile;
import org.geowebcache.layer.ProxyLayer;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerListener;
import org.geowebcache.layer.meta.ContactInformation;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.meta.MetadataURL;
import org.geowebcache.layer.updatesource.UpdateSourceDefinition;
import org.geowebcache.locks.LockProvider.Lock;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.geowebcache.util.GWCVars;
import org.geowebcache.util.ServletUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.util.ResponseUtils;

/**
 * GeoServer {@link TileLayer} implementation. Delegates to {@link GeoServerTileLayerInfo} for layer
 * configuration.
 */
public class GeoServerTileLayer extends TileLayer implements ProxyLayer {

    private static final Logger LOGGER = Logging.getLogger(GeoServerTileLayer.class);
    public static final int ENV_TX_POINTS =
            Integer.parseInt(System.getProperty("GWC_ENVELOPE_TX_POINTS", "5"));

    private final GeoServerTileLayerInfo info;

    public static final String GWC_SEED_INTERCEPT_TOKEN = "GWC_SEED_INTERCEPT";

    public static final ThreadLocal<WebMap> WEB_MAP = new ThreadLocal<WebMap>();

    private String configErrorMessage;

    private Map<String, GridSubset> subSets;

    private static LayerListenerList listeners = new LayerListenerList();

    private final GridSetBroker gridSetBroker;

    private Catalog catalog;

    private String publishedId;

    private volatile PublishedInfo publishedInfo;

    private LegendSample legendSample;

    private WMS wms;

    public GeoServerTileLayer(
            final PublishedInfo publishedInfo,
            final GWCConfig configDefaults,
            final GridSetBroker gridsets) {
        checkNotNull(publishedInfo, "publishedInfo");
        checkNotNull(gridsets, "gridsets");
        checkNotNull(configDefaults, "configDefaults");

        this.gridSetBroker = gridsets;
        this.publishedInfo = publishedInfo;
        this.info = TileLayerInfoUtil.loadOrCreate(getPublishedInfo(), configDefaults);
    }

    public GeoServerTileLayer(
            final PublishedInfo publishedInfo,
            final GridSetBroker gridsets,
            final GeoServerTileLayerInfo state) {
        checkNotNull(publishedInfo, "publishedInfo");
        checkNotNull(gridsets, "gridsets");
        checkNotNull(state, "state");

        this.gridSetBroker = gridsets;
        this.publishedInfo = publishedInfo;
        this.info = state;
        TileLayerInfoUtil.checkAutomaticStyles(publishedInfo, state);
    }

    public GeoServerTileLayer(
            final Catalog catalog,
            final String publishedId,
            final GWCConfig configDefaults,
            final GridSetBroker gridsets) {
        checkNotNull(catalog, "catalog");
        checkNotNull(publishedId, "publishedId");
        checkNotNull(gridsets, "gridsets");
        checkNotNull(configDefaults, "configDefaults");

        this.gridSetBroker = gridsets;
        this.catalog = catalog;
        this.publishedId = publishedId;
        this.info = TileLayerInfoUtil.loadOrCreate(getPublishedInfo(), configDefaults);
    }

    public GeoServerTileLayer(
            final Catalog catalog,
            final String publishedId,
            final GridSetBroker gridsets,
            final GeoServerTileLayerInfo state) {
        checkNotNull(catalog, "catalog");
        checkNotNull(publishedId, "publishedId");
        checkNotNull(gridsets, "gridsets");
        checkNotNull(state, "state");

        this.gridSetBroker = gridsets;
        this.catalog = catalog;
        this.publishedId = publishedId;
        this.info = state;
    }

    @Override
    public String getId() {
        return info.getId();
    }

    @Override
    public String getBlobStoreId() {
        return info.getBlobStoreId();
    }

    @Override
    public String getName() {
        // getting the current gwc operation
        String gwcOperation = GwcServiceDispatcherCallback.GWC_OPERATION.get();
        // checking if we are in the context of a get capabilities request
        if (gwcOperation != null && gwcOperation.equalsIgnoreCase("GetCapabilities")) {
            // this is a get capabilities request, we need to check if we are in the context of
            // virtual service
            return getNoPrefixedNameIfVirtualService();
        }
        return info.getName();
    }

    private String getNoPrefixedNameIfVirtualService() {
        // let's see if this a virtual service request
        WorkspaceInfo localWorkspace = LocalWorkspace.get();
        if (localWorkspace != null) {
            // yes this is a virtual service request so removing the workspace prefix
            return CatalogConfiguration.removeWorkspacePrefix(info.getName(), catalog);
        }
        // this a normal request so just returning the prefixed layer name
        return info.getName();
    }

    void setConfigErrorMessage(String configErrorMessage) {
        this.configErrorMessage = configErrorMessage;
    }

    public String getConfigErrorMessage() {
        return configErrorMessage;
    }

    @Override
    public List<ParameterFilter> getParameterFilters() {
        return new ArrayList<ParameterFilter>(info.getParameterFilters());
    }

    public void resetParameterFilters() {
        super.defaultParameterFilterValues = null; // reset default values
    }

    /**
     * Returns whether this tile layer is enabled.
     *
     * <p>The layer is enabled if the following conditions apply:
     *
     * <ul>
     *   <li>Caching for this layer is enabled by configuration
     *   <li>Its backing {@link LayerInfo} or {@link LayerGroupInfo} is enabled and not errored (as
     *       per {@link LayerInfo#enabled()} {@link LayerGroupInfo}
     *   <li>The layer is not errored ({@link #getConfigErrorMessage() == null}
     * </ul>
     *
     * The layer is enabled by configuration if: the {@code GWC.enabled} metadata property is set to
     * {@code true} in it's corresponding {@link LayerInfo} or {@link LayerGroupInfo} {@link
     * MetadataMap}, or there's no {@code GWC.enabled} property set at all but the global {@link
     * GWCConfig#isCacheLayersByDefault()} is {@code true}.
     *
     * @see org.geowebcache.layer.TileLayer#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        final boolean tileLayerInfoEnabled = info.isEnabled();
        if (!tileLayerInfoEnabled) {
            return false;
        }
        if (getConfigErrorMessage() != null) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest(
                        "Layer "
                                + getName()
                                + "is not enabled due to config error: "
                                + getConfigErrorMessage());
            }
            return false;
        }
        boolean geoserverLayerEnabled;
        PublishedInfo published = getPublishedInfo();
        if (published instanceof LayerInfo) {
            geoserverLayerEnabled = ((LayerInfo) published).enabled();
        } else {
            // LayerGroupInfo has no enabled property, so assume true
            geoserverLayerEnabled = true;
        }
        return tileLayerInfoEnabled && geoserverLayerEnabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        info.setEnabled(enabled);
    }

    /**
     * @see org.geowebcache.layer.TileLayer#isQueryable()
     * @see WMS#isQueryable(LayerGroupInfo)
     * @see WMS#isQueryable(LayerInfo)
     */
    @Override
    public boolean isQueryable() {
        boolean queryable = GWC.get().isQueryable(this);
        return queryable;
    }

    private ReferencedEnvelope getLatLonBbox() throws IllegalStateException {
        final CoordinateReferenceSystem wgs84LonFirst;
        try {
            final boolean longitudeFirst = true;
            wgs84LonFirst = CRS.decode("EPSG:4326", longitudeFirst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ReferencedEnvelope latLongBbox;
        if (getPublishedInfo() instanceof LayerGroupInfo) {
            LayerGroupInfo groupInfo = (LayerGroupInfo) getPublishedInfo();
            try {
                ReferencedEnvelope bounds = groupInfo.getBounds();
                boolean lenient = true;
                latLongBbox = bounds.transform(wgs84LonFirst, lenient);
            } catch (Exception e) {
                String msg =
                        "Can't get lat long bounds for layer group " + tileLayerName(groupInfo);
                LOGGER.log(Level.WARNING, msg, e);
                throw new IllegalStateException(msg, e);
            }
        } else {
            ResourceInfo resourceInfo = getResourceInfo();
            latLongBbox = resourceInfo.getLatLonBoundingBox();
            if (null == latLongBbox) {
                latLongBbox = new ReferencedEnvelope(wgs84LonFirst);
            }
            if (null == latLongBbox.getCoordinateReferenceSystem()) {
                ReferencedEnvelope tmp = new ReferencedEnvelope(wgs84LonFirst);
                tmp.init(
                        latLongBbox.getMinX(),
                        latLongBbox.getMaxX(),
                        latLongBbox.getMinY(),
                        latLongBbox.getMaxY());
                latLongBbox = tmp;
            }
        }
        return latLongBbox;
    }

    public PublishedInfo getPublishedInfo() {
        if (publishedInfo == null) {
            synchronized (this) {
                if (publishedInfo == null) {
                    // see if it's a layer or a layer group
                    PublishedInfo work = catalog.getLayer(publishedId);
                    if (work == null) {
                        work = catalog.getLayerGroup(publishedId);
                    }

                    if (work != null) {
                        TileLayerInfoUtil.checkAutomaticStyles(work, info);
                    } else {
                        throw new IllegalStateException(
                                "Could not locate a layer or layer group with id "
                                        + publishedId
                                        + " within GeoServer configuration, the GWC configuration seems to be out of synch");
                    }
                    this.publishedInfo = work;
                }
            }
        }

        return publishedInfo;
    }

    private ResourceInfo getResourceInfo() {
        return getPublishedInfo() instanceof LayerInfo
                ? ((LayerInfo) getPublishedInfo()).getResource()
                : null;
    }

    /**
     * Overrides to return a dynamic view of the backing {@link LayerInfo} or {@link LayerGroupInfo}
     * metadata adapted to GWC
     *
     * @see org.geowebcache.layer.TileLayer#getMetaInformation()
     */
    @Override
    public LayerMetaInformation getMetaInformation() {
        LayerMetaInformation meta = null;
        String title = getName();
        String description = "";
        List<String> keywords = Collections.emptyList();
        List<ContactInformation> contacts = Collections.emptyList();

        ResourceInfo resourceInfo = getResourceInfo();
        if (resourceInfo != null) {
            title = resourceInfo.getTitle();
            description = resourceInfo.getAbstract();
            keywords = new ArrayList<>();
            for (KeywordInfo kw : resourceInfo.getKeywords()) {
                keywords.add(kw.getValue());
            }
        } else {
            if (publishedInfo instanceof LayerGroupInfo) {
                LayerGroupInfo lg = (LayerGroupInfo) publishedInfo;
                if (lg.getTitle() != null) {
                    title = lg.getTitle();
                }
                if (lg.getAbstract() != null) {
                    description = lg.getAbstract();
                }
            }
        }
        meta = new LayerMetaInformation(title, description, keywords, contacts);
        return meta;
    }

    /**
     * The default style name for the layer, as advertised by its backing {@link
     * LayerInfo#getDefaultStyle()}, or {@code null} if this tile layer is backed by a {@link
     * LayerGroupInfo}.
     *
     * <p>As the default style is always cached, its name is not stored as part of this tile layer's
     * {@link GeoServerTileLayerInfo}. Instead it's 'live' and retrieved from the current {@link
     * LayerInfo} every time this method is invoked.
     *
     * @see org.geowebcache.layer.TileLayer#getStyles()
     */
    @Override
    public String getStyles() {
        PublishedInfo published = getPublishedInfo();
        if (!(published instanceof LayerInfo)) {
            // there's no such thing as default style for a layer group
            return null;
        }
        LayerInfo layerInfo = (LayerInfo) published;
        StyleInfo defaultStyle = layerInfo.getDefaultStyle();
        if (defaultStyle == null) {
            setConfigErrorMessage("Underlying GeoSever Layer has no default style");
            return null;
        }
        return defaultStyle.prefixedName();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getFeatureInfo
     * @see GWC#dispatchOwsRequest
     */
    @Override
    public Resource getFeatureInfo(
            ConveyorTile convTile, BoundingBox bbox, int height, int width, int x, int y)
            throws GeoWebCacheException {

        Map<String, String> params = buildGetFeatureInfo(convTile, bbox, height, width, x, y);
        Resource response;
        try {
            response = GWC.get().dispatchOwsRequest(params, (Cookie[]) null);
        } catch (Exception e) {
            throw new GeoWebCacheException(e);
        }
        return response;
    }

    private Map<String, String> buildGetFeatureInfo(
            ConveyorTile convTile, BoundingBox bbox, int height, int width, int x, int y) {
        Map<String, String> wmsParams = new HashMap<String, String>();
        wmsParams.put("SERVICE", "WMS");
        wmsParams.put("VERSION", "1.1.1");
        wmsParams.put("REQUEST", "GetFeatureInfo");
        wmsParams.put("LAYERS", getName());
        wmsParams.put("STYLES", "");
        wmsParams.put("QUERY_LAYERS", getName());
        MimeType mimeType = convTile.getMimeType();
        if (mimeType == null) {
            mimeType = getMimeTypes().get(0);
        }
        wmsParams.put("FORMAT", mimeType.getFormat());
        wmsParams.put("EXCEPTIONS", GetMapRequest.SE_XML);

        wmsParams.put("INFO_FORMAT", convTile.getMimeType().getFormat());

        GridSubset gridSubset = convTile.getGridSubset();

        wmsParams.put("SRS", gridSubset.getSRS().toString());
        wmsParams.put("HEIGHT", String.valueOf(height));
        wmsParams.put("WIDTH", String.valueOf(width));
        wmsParams.put("BBOX", bbox.toString());
        wmsParams.put("X", String.valueOf(x));
        wmsParams.put("Y", String.valueOf(y));
        String featureCount;
        {
            Map<String, String> values =
                    ServletUtils.selectedStringsFromMap(
                            convTile.servletReq.getParameterMap(),
                            convTile.servletReq.getCharacterEncoding(),
                            "feature_count");
            featureCount = values.get("feature_count");
        }
        if (featureCount != null) {
            wmsParams.put("FEATURE_COUNT", featureCount);
        }

        Map<String, String> fullParameters = convTile.getFilteringParameters();
        if (fullParameters.isEmpty()) {
            fullParameters = getDefaultParameterFilters();
        }
        wmsParams.putAll(fullParameters);

        return wmsParams;
    }

    @Override
    public ConveyorTile getTile(ConveyorTile tile)
            throws GeoWebCacheException, IOException, OutsideCoverageException {
        MimeType mime = tile.getMimeType();
        final List<MimeType> formats = getMimeTypes();
        if (mime == null) {
            mime = formats.get(0);
        } else {
            if (!formats.contains(mime)) {
                throw new IllegalArgumentException(
                        mime.getFormat() + " is not a supported format for " + getName());
            }
        }

        final String tileGridSetId = tile.getGridSetId();
        final GridSubset gridSubset = getGridSubset(tileGridSetId);
        if (gridSubset == null) {
            throw new IllegalArgumentException("Requested gridset not found: " + tileGridSetId);
        }

        final long[] gridLoc = tile.getTileIndex();
        checkNotNull(gridLoc);

        // Final preflight check, throws OutsideCoverageException if necessary
        gridSubset.checkCoverage(gridLoc);

        ConveyorTile returnTile;

        int metaX;
        int metaY;
        if (mime.supportsTiling()) {
            metaX = info.getMetaTilingX();
            metaY = info.getMetaTilingY();
        } else {
            metaX = metaY = 1;
        }

        returnTile = getMetatilingReponse(tile, true, metaX, metaY);

        sendTileRequestedEvent(returnTile);

        return returnTile;
    }

    @Override
    public void addLayerListener(final TileLayerListener listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeLayerListener(final TileLayerListener listener) {
        listeners.removeListener(listener);
        return true;
    }

    protected final void sendTileRequestedEvent(ConveyorTile tile) {
        if (listeners != null) {
            listeners.sendTileRequested(this, tile);
        }
    }

    private ConveyorTile getMetatilingReponse(
            ConveyorTile tile, final boolean tryCache, final int metaX, final int metaY)
            throws GeoWebCacheException, IOException {

        final GridSubset gridSubset = getGridSubset(tile.getGridSetId());
        final int zLevel = (int) tile.getTileIndex()[2];
        tile.setMetaTileCacheOnly(!gridSubset.shouldCacheAtZoom(zLevel));

        if (tryCache && tryCacheFetch(tile)) {
            return finalizeTile(tile);
        }

        final GeoServerMetaTile metaTile = createMetaTile(tile, metaX, metaY);
        Lock lock = null;
        try {
            /* ****************** Acquire lock ******************* */
            lock = GWC.get().getLockProvider().getLock(buildLockKey(tile, metaTile));
            // got the lock on the meta tile, try again
            if (tryCache && tryCacheFetch(tile)) {
                LOGGER.finest(
                        "--> "
                                + Thread.currentThread().getName()
                                + " returns cache hit for "
                                + Arrays.toString(metaTile.getMetaGridPos()));
            } else {
                LOGGER.finer(
                        "--> "
                                + Thread.currentThread().getName()
                                + " submitting getMap request for meta grid location "
                                + Arrays.toString(metaTile.getMetaGridPos())
                                + " on "
                                + metaTile);
                WebMap map;
                try {
                    long requestTime = System.currentTimeMillis();
                    map = dispatchGetMap(tile, metaTile);
                    checkNotNull(map, "Did not obtain a WebMap from GeoServer's Dispatcher");
                    metaTile.setWebMap(map);
                    saveTiles(metaTile, tile, requestTime);
                } catch (Exception e) {
                    Throwables.throwIfInstanceOf(e, GeoWebCacheException.class);
                    throw new GeoWebCacheException("Problem communicating with GeoServer", e);
                }
            }
            /* ****************** Return lock and response ****** */
        } finally {
            if (lock != null) {
                lock.release();
            }
            metaTile.dispose();
        }

        return finalizeTile(tile);
    }

    private String buildLockKey(ConveyorTile tile, GeoServerMetaTile metaTile) {
        StringBuilder metaKey = new StringBuilder();

        final long[] tileIndex;
        if (metaTile != null) {
            tileIndex = metaTile.getMetaGridPos();
            metaKey.append("gsmeta_");
        } else {
            tileIndex = tile.getTileIndex();
            metaKey.append("tile_");
        }
        long x = tileIndex[0];
        long y = tileIndex[1];
        long z = tileIndex[2];

        metaKey.append(tile.getLayerId());
        metaKey.append("_").append(tile.getGridSetId());
        metaKey.append("_").append(x).append("_").append(y).append("_").append(z);
        if (tile.getParametersId() != null) {
            metaKey.append("_").append(tile.getParametersId());
        }
        metaKey.append(".").append(tile.getMimeType().getFileExtension());

        return metaKey.toString();
    }

    private WebMap dispatchGetMap(final ConveyorTile tile, final MetaTile metaTile)
            throws Exception {

        Map<String, String> params = buildGetMap(tile, metaTile);
        WebMap map;
        try {
            HttpServletRequest actualRequest = tile.servletReq;
            Cookie[] cookies = actualRequest == null ? null : actualRequest.getCookies();

            GWC.get().dispatchOwsRequest(params, cookies);
            map = WEB_MAP.get();
            if (!(map instanceof WebMap)) {
                throw new IllegalStateException("Expected: RenderedImageMap, got " + map);
            }
        } finally {
            WEB_MAP.remove();
        }

        return map;
    }

    private GeoServerMetaTile createMetaTile(ConveyorTile tile, final int metaX, final int metaY) {
        GeoServerMetaTile metaTile;

        String tileGridSetId = tile.getGridSetId();
        GridSubset gridSubset = getGridSubset(tileGridSetId);
        MimeType responseFormat = tile.getMimeType();
        FormatModifier formatModifier = null;
        long[] tileGridPosition = tile.getTileIndex();
        int gutter = responseFormat.isVector() ? 0 : info.getGutter();
        metaTile =
                new GeoServerMetaTile(
                        gridSubset,
                        responseFormat,
                        formatModifier,
                        tileGridPosition,
                        metaX,
                        metaY,
                        gutter);

        return metaTile;
    }

    private Map<String, String> buildGetMap(final ConveyorTile tile, final MetaTile metaTile)
            throws ParameterException {

        Map<String, String> params = new HashMap<String, String>();

        final MimeType mimeType = tile.getMimeType();
        final String gridSetId = tile.getGridSetId();
        final GridSubset gridSubset = getGridSubset(gridSetId);

        int width = metaTile.getMetaTileWidth();
        int height = metaTile.getMetaTileHeight();
        String srs = gridSubset.getSRS().toString();
        String format = mimeType.getFormat();
        BoundingBox bbox = metaTile.getMetaTileBounds();

        params.put("SERVICE", "WMS");
        params.put("VERSION", "1.1.1");
        params.put("REQUEST", "GetMap");
        params.put("LAYERS", getName());
        params.put("SRS", srs);
        params.put("FORMAT", format);
        params.put("WIDTH", String.valueOf(width));
        params.put("HEIGHT", String.valueOf(height));
        params.put("BBOX", bbox.toString());

        params.put("EXCEPTIONS", GetMapRequest.SE_XML);
        params.put("STYLES", "");
        params.put("TRANSPARENT", "true");
        params.put(GWC_SEED_INTERCEPT_TOKEN, "true");

        Map<String, String> filteredParams = tile.getFilteringParameters();
        if (filteredParams.isEmpty()) {
            filteredParams = getDefaultParameterFilters();
        }
        params.putAll(filteredParams);

        return params;
    }

    private boolean tryCacheFetch(ConveyorTile tile) {
        int expireCache = this.getExpireCache((int) tile.getTileIndex()[2]);
        if (expireCache != GWCVars.CACHE_DISABLE_CACHE) {
            try {
                return tile.retrieve(expireCache * 1000L);
            } catch (GeoWebCacheException gwce) {
                LOGGER.info(gwce.getMessage());
                tile.setErrorMsg(gwce.getMessage());
                return false;
            }
        }
        return false;
    }

    private ConveyorTile finalizeTile(ConveyorTile tile) {
        if (tile.getStatus() == 0 && !tile.getError()) {
            tile.setStatus(200);
        }

        if (tile.servletResp != null) {
            setExpirationHeader(tile.servletResp, (int) tile.getTileIndex()[2]);
            setTileIndexHeader(tile);
        }

        tile.setTileLayer(this);
        return tile;
    }

    /** @param tile */
    private void setTileIndexHeader(ConveyorTile tile) {
        tile.servletResp.addHeader("geowebcache-tile-index", Arrays.toString(tile.getTileIndex()));
    }

    @Override
    public ConveyorTile getNoncachedTile(ConveyorTile tile) throws GeoWebCacheException {
        try {
            return getMetatilingReponse(tile, false, 1, 1);
        } catch (IOException e) {
            throw new GeoWebCacheException(e);
        }
    }

    @Override
    public ConveyorTile doNonMetatilingRequest(ConveyorTile tile) throws GeoWebCacheException {
        try {
            return getMetatilingReponse(tile, true, 1, 1);
        } catch (IOException e) {
            throw new GeoWebCacheException(e);
        }
    }

    @Override
    public void seedTile(ConveyorTile tile, boolean tryCache)
            throws GeoWebCacheException, IOException {

        // Ignore a seed call on a tile that's outside the cached grid levels range
        final GridSubset gridSubset = getGridSubset(tile.getGridSetId());
        final int zLevel = (int) tile.getTileIndex()[2];
        if (!gridSubset.shouldCacheAtZoom(zLevel)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest(
                        "Ignoring seed call on tile "
                                + tile
                                + " as it's outside the cacheable zoom level range");
            }
            return;
        }

        int metaX = info.getMetaTilingX();
        int metaY = info.getMetaTilingY();
        if (!tile.getMimeType().supportsTiling()) {
            metaX = metaY = 1;
        }
        getMetatilingReponse(tile, tryCache, metaX, metaY);
    }

    /** @see org.geowebcache.layer.TileLayer#getGridSubsets() */
    @Override
    public synchronized Set<String> getGridSubsets() {
        checkGridSubsets();
        return new HashSet<String>(subSets.keySet());
    }

    @Override
    public GridSubset getGridSubset(final String gridSetId) {
        checkGridSubsets();
        return subSets.get(gridSetId);
    }

    private synchronized void checkGridSubsets() {
        if (this.subSets == null) {
            ReferencedEnvelope latLongBbox = getLatLonBbox();
            try {
                this.subSets = getGrids(latLongBbox, gridSetBroker);
            } catch (ConfigurationException e) {
                String msg = "Can't create grids for '" + getName() + "': " + e.getMessage();
                LOGGER.log(Level.WARNING, msg, e);
                setConfigErrorMessage(msg);
            }
        }
    }

    @Override
    public synchronized GridSubset removeGridSubset(String gridSetId) {
        checkGridSubsets();
        final GridSubset oldValue = this.subSets.remove(gridSetId);

        Set<XMLGridSubset> gridSubsets = new HashSet<XMLGridSubset>(info.getGridSubsets());
        for (Iterator<XMLGridSubset> it = gridSubsets.iterator(); it.hasNext(); ) {
            if (it.next().getGridSetName().equals(gridSetId)) {
                it.remove();
                break;
            }
        }
        info.setGridSubsets(gridSubsets);
        return oldValue;
    }

    @Override
    public void addGridSubset(GridSubset gridSubset) {
        XMLGridSubset gridSubsetInfo = new XMLGridSubset(gridSubset);
        if (gridSubset instanceof DynamicGridSubset) {
            gridSubsetInfo.setExtent(null);
        }
        Set<XMLGridSubset> gridSubsets = new HashSet<XMLGridSubset>(info.getGridSubsets());
        gridSubsets.add(gridSubsetInfo);
        info.setGridSubsets(gridSubsets);
        this.subSets = null;
    }

    private Map<String, GridSubset> getGrids(
            final ReferencedEnvelope latLonBbox, final GridSetBroker gridSetBroker)
            throws ConfigurationException {

        Set<XMLGridSubset> cachedGridSets = info.getGridSubsets();
        if (cachedGridSets.size() == 0) {
            return Collections.emptyMap();
        }

        Map<String, GridSubset> grids = new HashMap<String, GridSubset>(2);
        for (XMLGridSubset xmlGridSubset : cachedGridSets) {
            final String gridSetId = xmlGridSubset.getGridSetName();
            final GridSet gridSet = gridSetBroker.get(gridSetId);
            if (gridSet == null) {
                LOGGER.info("No GWC GridSet named '" + gridSetId + "' exists.");
                continue;
            }
            BoundingBox extent = xmlGridSubset.getExtent();
            boolean dynamic = Objects.isNull(extent);
            if (dynamic) {
                try {
                    SRS srs = gridSet.getSrs();
                    try {
                        extent = getBounds(srs);
                    } catch (RuntimeException cantComputeBounds) {
                        final String msg =
                                "Can't compute bounds for tile layer "
                                        + getName()
                                        + " in CRS "
                                        + srs
                                        + ". Assuming full GridSet bounds. ("
                                        + cantComputeBounds.getMessage()
                                        + ")";
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, msg, cantComputeBounds);
                        } else {
                            LOGGER.warning(msg);
                        }
                        extent = gridSet.getBounds();
                    }

                    BoundingBox maxBounds = gridSet.getBounds();
                    BoundingBox intersection = maxBounds.intersection(extent);
                    extent = intersection;
                } catch (RuntimeException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Error computing layer bounds, assuming whole GridSet bounds",
                            e);
                    extent = gridSet.getOriginalExtent();
                }
            }
            xmlGridSubset.setExtent(extent);

            GridSubset gridSubSet = xmlGridSubset.getGridSubSet(gridSetBroker);
            if (dynamic) {
                gridSubSet = new DynamicGridSubset(gridSubSet);
            }

            grids.put(gridSetId, gridSubSet);
        }

        return grids;
    }

    private BoundingBox getBounds(final SRS srs) {

        CoordinateReferenceSystem targetCrs;
        try {
            final String epsgCode = srs.toString();
            final boolean longitudeFirst = true;
            targetCrs = CRS.decode(epsgCode, longitudeFirst);
            checkNotNull(targetCrs);
        } catch (Exception e) {
            throwIfUnchecked(e);
            throw new RuntimeException(e);
        }

        ReferencedEnvelope nativeBounds;
        if (getResourceInfo() != null) {
            // projection policy for these bounds are already taken care of by the geoserver
            // configuration
            try {
                nativeBounds = getResourceInfo().boundingBox();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            nativeBounds = ((LayerGroupInfo) getPublishedInfo()).getBounds();
        }
        checkState(nativeBounds != null, getName(), " has no native bounds set");

        Envelope transformedBounds;
        // try reprojecting directly
        try {
            transformedBounds = nativeBounds.transform(targetCrs, true, ENV_TX_POINTS);
        } catch (Exception e) {
            // no luck, try the expensive way
            final Geometry targetAov = GWC.getAreaOfValidityAsGeometry(targetCrs, gridSetBroker);
            if (null == targetAov) {
                String msg =
                        "Can't compute tile layer bounds out of resource native bounds for CRS "
                                + srs;
                LOGGER.log(Level.WARNING, msg, e);
                throw new IllegalArgumentException(msg, e);
            }
            LOGGER.log(
                    Level.FINE,
                    "Can't compute tile layer bounds out of resource "
                            + "native bounds for CRS "
                            + srs,
                    e);

            final CoordinateReferenceSystem nativeCrs = nativeBounds.getCoordinateReferenceSystem();

            try {

                ReferencedEnvelope targetAovBounds =
                        new ReferencedEnvelope(targetAov.getEnvelopeInternal(), targetCrs);
                // transform target AOV in target CRS to native CRS
                ReferencedEnvelope targetAovInNativeCrs =
                        targetAovBounds.transform(nativeCrs, true, ENV_TX_POINTS);
                // get the intersection between the target aov in native crs and native layer bounds
                Envelope intersection = targetAovInNativeCrs.intersection(nativeBounds);
                ReferencedEnvelope clipped = new ReferencedEnvelope(intersection, nativeCrs);

                // transform covered area in native crs to target crs
                transformedBounds = clipped.transform(targetCrs, true, ENV_TX_POINTS);
            } catch (Exception e1) {
                throwIfUnchecked(e);
                throw new RuntimeException(e);
            }
        }

        BoundingBox targetBbox =
                new BoundingBox(
                        transformedBounds.getMinX(),
                        transformedBounds.getMinY(),
                        transformedBounds.getMaxX(),
                        transformedBounds.getMaxY());
        return targetBbox;
    }

    public GeoServerTileLayerInfo getInfo() {
        return info;
    }

    /** @see org.geowebcache.layer.TileLayer#getUpdateSources() */
    @Override
    public List<UpdateSourceDefinition> getUpdateSources() {
        return Collections.emptyList();
    }

    /** @see org.geowebcache.layer.TileLayer#useETags() */
    @Override
    public boolean useETags() {
        return false;
    }

    /** @see org.geowebcache.layer.TileLayer#getFormatModifiers() */
    @Override
    public List<FormatModifier> getFormatModifiers() {
        return Collections.emptyList();
    }

    /** @see org.geowebcache.layer.TileLayer#setFormatModifiers(java.util.List) */
    @Override
    public void setFormatModifiers(List<FormatModifier> formatModifiers) {
        throw new UnsupportedOperationException();
    }

    /** @see org.geowebcache.layer.TileLayer#getMetaTilingFactors() */
    @Override
    public int[] getMetaTilingFactors() {
        return new int[] {info.getMetaTilingX(), info.getMetaTilingY()};
    }

    /**
     * @return {@code true}
     * @see #getNoncachedTile(ConveyorTile)
     * @see org.geowebcache.layer.TileLayer#isCacheBypassAllowed()
     */
    @Override
    public Boolean isCacheBypassAllowed() {
        return true;
    }

    /** @see org.geowebcache.layer.TileLayer#setCacheBypassAllowed(boolean) */
    @Override
    public void setCacheBypassAllowed(boolean allowed) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return {@code 0}
     * @see org.geowebcache.layer.TileLayer#getBackendTimeout()
     */
    @Override
    public Integer getBackendTimeout() {
        return Integer.valueOf(0);
    }

    /** @see org.geowebcache.layer.TileLayer#setBackendTimeout(int) */
    @Override
    public void setBackendTimeout(int seconds) {
        throw new UnsupportedOperationException();
    }

    /** @see org.geowebcache.layer.TileLayer#getMimeTypes() */
    @Override
    public List<MimeType> getMimeTypes() {
        Set<String> mimeFormats = info.getMimeFormats();
        List<MimeType> mimeTypes = new ArrayList<MimeType>(mimeFormats.size());
        for (String format : mimeFormats) {
            try {
                mimeTypes.add(MimeType.createFromFormat(format));
            } catch (MimeException e) {
                LOGGER.log(Level.WARNING, "Can't create MimeType from format " + format, e);
            }
        }
        return mimeTypes;
    }

    /**
     * Gets the expiration time to be declared to clients, calculated based on the metadata of the
     * underlying {@link LayerInfo} or {@link LayerGroupInfo} This calculation can be overridden by
     * setting {@link GeoServerTileLayerInfo#setExpireClients(int)}
     *
     * @see org.geowebcache.layer.TileLayer#getExpireClients(int)
     * @param zoomLevel ignored
     * @return the expiration time
     */
    @Override
    public int getExpireClients(int zoomLevel) {
        if (info.getExpireClients() > 0) {
            return info.getExpireClients();
        }

        PublishedInfo published = getPublishedInfo();
        if (published instanceof LayerInfo) {
            return getLayerMaxAge((LayerInfo) published);
        }
        LayerGroupInfo layerGroupInfo = (LayerGroupInfo) published;
        if (layerGroupInfo != null) {
            return getGroupMaxAge(layerGroupInfo);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "Found a GeoServerTileLayer that is not base on either"
                                + "LayerInfo or LayerGroupInfo, setting its max age to 0");
            }
            return 0;
        }
    }

    /** Returns the max age of a layer group by looking for the minimum max age of its components */
    private int getGroupMaxAge(LayerGroupInfo lg) {
        int maxAge = Integer.MAX_VALUE;
        for (PublishedInfo pi : lg.getLayers()) {
            int piAge;
            if (pi instanceof LayerInfo) {
                piAge = getLayerMaxAge((LayerInfo) pi);
            } else if (pi instanceof LayerGroupInfo) {
                piAge = getGroupMaxAge((LayerGroupInfo) pi);
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                            Level.FINE,
                            "Found a PublishedInfo that is nor LayerInfo nor "
                                    + "LayerGroupInfo, setting its max age to 0: "
                                    + pi);
                }
                piAge = 0;
            }
            maxAge = Math.min(piAge, maxAge);
        }

        return maxAge;
    }

    /** Returns the max age for the specified layer */
    private int getLayerMaxAge(LayerInfo li) {
        MetadataMap metadata = li.getResource().getMetadata();
        Object enabled = metadata.get(ResourceInfo.CACHING_ENABLED);
        if (enabled != null && enabled.toString().equalsIgnoreCase("true")) {
            Integer maxAge = metadata.get(ResourceInfo.CACHE_AGE_MAX, Integer.class);
            if (maxAge != null) {
                return maxAge;
            } else {
                return 0;
            }
        }

        return 0;
    }

    /**
     * Gets the expiration time for tiles in the cache, based on the expiration rules from {@link
     * GeoServerTileLayerInfo#getExpireCacheList()}. If no matching rules are found, defaults to
     * {@link GeoServerTileLayerInfo#getExpireCache()}
     *
     * @see org.geowebcache.layer.TileLayer#getExpireCache(int)
     * @param zoomLevel the zoom level used to filter expiration rules
     * @return the expiration time for tiles at the given zoom level
     */
    @Override
    public int getExpireCache(int zoomLevel) {
        if (info.getExpireCacheList() != null) {
            ExpirationRule matchedRule = null;
            for (ExpirationRule rule : info.getExpireCacheList()) {
                if (zoomLevel >= rule.getMinZoom()) {
                    matchedRule = rule;
                } else {
                    // ExpirationRules should be zoomlevel ascending
                    break;
                }
            }
            if (matchedRule != null) {
                return matchedRule.getExpiration();
            }
        }
        return info.getExpireCache();
    }

    /**
     * @return {@code null}, no request filters supported so far
     * @see org.geowebcache.layer.TileLayer#getRequestFilters()
     */
    @Override
    public List<RequestFilter> getRequestFilters() {
        return null;
    }

    /**
     * Empty method, returns {@code true}, initialization is dynamic for this class.
     *
     * @see org.geowebcache.layer.TileLayer#initialize(org.geowebcache.grid.GridSetBroker)
     */
    @Override
    public boolean initialize(final GridSetBroker gridSetBroker) {
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[")
                .append(info)
                .append("]")
                .toString();
    }

    @Override
    public List<MimeType> getInfoMimeTypes() {
        // Get the formats WMS supports for GetFeatureInfo
        List<String> typeStrings =
                ((WMS) GeoServerExtensions.bean("wms")).getAvailableFeatureInfoFormats();
        List<MimeType> types = new ArrayList<MimeType>(typeStrings.size());
        for (String typeString : typeStrings) {
            try {
                types.add(MimeType.createFromFormat(typeString));
            } catch (MimeException e) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }
        return types;
    }

    @Override
    public void proxyRequest(ConveyorTile tile) throws GeoWebCacheException {
        try {
            GWC.get().proxyOwsRequest(tile);
        } catch (Exception e) {
            throw new GeoWebCacheException("Failed to cascade request", e);
        }
    }

    @Override
    public List<MetadataURL> getMetadataURLs() {
        List<MetadataLinkInfo> gsMetadataLinks;
        List<MetadataURL> gwcMetadataLinks = new ArrayList<>();
        PublishedInfo published = getPublishedInfo();
        if (published instanceof LayerInfo) {
            // this is a normal layer
            gsMetadataLinks = ((LayerInfo) published).getResource().getMetadataLinks();
        } else {
            // this is a layer group
            gsMetadataLinks = new ArrayList<>();
            for (LayerInfo layer :
                    Iterables.filter(((LayerGroupInfo) published).getLayers(), LayerInfo.class)) {
                // getting metadata of all layers of the layer group
                List<MetadataLinkInfo> metadataLinksLayer = layer.getResource().getMetadataLinks();
                if (metadataLinksLayer != null) {
                    gsMetadataLinks.addAll(metadataLinksLayer);
                }
            }
        }
        String baseUrl = baseUrl();
        for (MetadataLinkInfo gsMetadata : gsMetadataLinks) {
            String url = ResponseUtils.proxifyMetadataLink(gsMetadata, baseUrl);
            try {
                gwcMetadataLinks.add(
                        new MetadataURL(
                                gsMetadata.getMetadataType(), gsMetadata.getType(), new URL(url)));
            } catch (MalformedURLException exception) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("Error adding layer metadata URL.");
                }
            }
        }
        return gwcMetadataLinks;
    }

    @Override
    public boolean isAdvertised() {
        return true;
    }

    @Override
    public void setAdvertised(boolean advertised) {}

    @Override
    public boolean isTransientLayer() {
        return false;
    }

    @Override
    public void setTransientLayer(boolean transientLayer) {}

    @Override
    public void setBlobStoreId(String blobStoreId) {
        info.setBlobStoreId(blobStoreId);
    }

    @Override
    public Map<String, org.geowebcache.config.legends.LegendInfo> getLayerLegendsInfo() {
        if (!(publishedInfo instanceof LayerInfo)) {
            return Collections.emptyMap();
        }
        Map<String, org.geowebcache.config.legends.LegendInfo> legends = new HashMap<>();
        LayerInfo layerInfo = (LayerInfo) publishedInfo;
        Set<StyleInfo> styles = new HashSet<>(layerInfo.getStyles());
        styles.add(layerInfo.getDefaultStyle());
        for (StyleInfo styleInfo : styles) {
            if (styleInfo == null) {
                continue;
            }
            // compute min and max scales denominators od the style
            NumberRange<Double> scalesDenominator;
            try {
                scalesDenominator =
                        CapabilityUtil.searchMinMaxScaleDenominator(
                                Collections.singleton(styleInfo));
            } catch (Exception exception) {
                throw new RuntimeException(
                        String.format(
                                "Error searching max and min scale denominators for style '%s'.",
                                styleInfo.getName()),
                        exception);
            }
            org.geoserver.catalog.LegendInfo legendInfo = styleInfo.getLegend();
            LegendInfoBuilder gwcLegendInfo = new LegendInfoBuilder();
            if (legendInfo != null) {
                String baseUrl = baseUrl();
                gwcLegendInfo
                        .withStyleName(styleInfo.getName())
                        .withWidth(legendInfo.getWidth())
                        .withHeight(legendInfo.getHeight())
                        .withFormat(legendInfo.getFormat())
                        .withMinScale(scalesDenominator.getMinimum())
                        .withMaxScale(scalesDenominator.getMaximum())
                        .withCompleteUrl(
                                buildURL(
                                        baseUrl,
                                        legendInfo.getOnlineResource(),
                                        null,
                                        URLMangler.URLType.SERVICE));
                legends.put(styleInfo.prefixedName(), gwcLegendInfo.build());
            } else {
                int finalWidth = GetLegendGraphicRequest.DEFAULT_WIDTH;
                int finalHeight = GetLegendGraphicRequest.DEFAULT_HEIGHT;
                String finalFormat = GetLegendGraphicRequest.DEFAULT_FORMAT;
                try {
                    Dimension dimension = getLegendSample().getLegendURLSize(styleInfo);
                    if (dimension != null) {
                        finalWidth = (int) dimension.getWidth();
                        finalHeight = (int) dimension.getHeight();
                    }
                    if (null == getWms().getLegendGraphicOutputFormat(finalFormat)) {
                        if (LOGGER.isLoggable(Level.WARNING)) {
                            LOGGER.warning(
                                    "Default legend format ("
                                            + finalFormat
                                            + ")is not supported (jai not available?), can't add LegendURL element");
                        }
                        continue;
                    }
                } catch (Exception exception) {
                    LOGGER.log(
                            Level.WARNING,
                            "Error getting LegendURL dimensions from sample",
                            exception);
                }
                String layerName = layerInfo.prefixedName();
                Map<String, String> params =
                        params(
                                "service",
                                "WMS",
                                "request",
                                "GetLegendGraphic",
                                "format",
                                finalFormat,
                                "width",
                                String.valueOf(finalWidth),
                                "height",
                                String.valueOf(finalHeight),
                                "layer",
                                layerName);
                if (!styleInfo.getName().equals(layerInfo.getDefaultStyle().getName())) {
                    params.put("style", styleInfo.getName());
                }
                String baseUrl = baseUrl();
                gwcLegendInfo
                        .withStyleName(styleInfo.getName())
                        .withWidth(finalWidth)
                        .withHeight(finalHeight)
                        .withFormat(finalFormat)
                        .withMinScale(scalesDenominator.getMinimum())
                        .withMaxScale(scalesDenominator.getMaximum())
                        .withCompleteUrl(
                                buildURL(baseUrl, "ows", params, URLMangler.URLType.RESOURCE));
                legends.put(styleInfo.prefixedName(), gwcLegendInfo.build());
            }
        }
        return legends;
    }

    /** Helper that gets the LegendSample bean from Spring context when needed. */
    private LegendSample getLegendSample() {
        if (legendSample == null) {
            // no need for synchronization the bean is always the same
            legendSample = GeoServerExtensions.bean(LegendSample.class);
        }
        return legendSample;
    }

    /** Helper that gets the WMS bean from Spring context when needed. */
    private WMS getWms() {
        if (wms == null) {
            // no need for synchronization the bean is always the same
            wms = GeoServerExtensions.bean(WMS.class);
        }
        return wms;
    }

    void setLegendSample(LegendSample legendSample) {
        this.legendSample = legendSample;
    }

    void setWms(WMS wms) {
        this.wms = wms;
    }

    /**
     * Gets the base URL of the server, this value is retrieved from the current HTTP request. If no
     * HTTP request is in progress NULL is returned. Only the use cases where an OWS service or a
     * REST end-point was target are handled.
     */
    private static String baseUrl() {
        // let's see if a OWS service was targeted
        Request owsRequest = Dispatcher.REQUEST.get();
        if (owsRequest != null) {
            // retrieve the base URL from the dispatcher request
            return org.geoserver.ows.util.ResponseUtils.baseURL(
                    Dispatcher.REQUEST.get().getHttpRequest());
        }
        // let's see if a REST end-point was targeted
        RequestInfo restRequest = RequestInfo.get();
        if (restRequest != null) {
            // retrieve the base URL from REST request
            return restRequest.getBaseURL();
        }
        // no HTTP request is in progress
        return null;
    }
}
