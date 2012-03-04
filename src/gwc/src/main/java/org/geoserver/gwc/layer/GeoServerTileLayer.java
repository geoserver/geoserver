/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RenderedImageMap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.CanonicalSet;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.filter.parameters.ParameterException;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.geowebcache.filter.request.RequestFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.grid.OutsideCoverageException;
import org.geowebcache.grid.SRS;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.GridLocObj;
import org.geowebcache.layer.LayerListenerList;
import org.geowebcache.layer.MetaTile;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerListener;
import org.geowebcache.layer.meta.ContactInformation;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.updatesource.UpdateSourceDefinition;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.geowebcache.util.GWCVars;
import org.geowebcache.util.ServletUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.springframework.util.Assert;

public class GeoServerTileLayer extends TileLayer {

    private static final Logger LOGGER = Logging.getLogger(GeoServerTileLayer.class);

    private final GeoServerTileLayerInfo info;

    public static final String GWC_SEED_INTERCEPT_TOKEN = "GWC_SEED_INTERCEPT";

    public static final ThreadLocal<WebMap> WEB_MAP = new ThreadLocal<WebMap>();

    private CatalogConfiguration mediator;

    private final String layerId;

    private final String layerGroupId;

    private String configErrorMessage;

    private List<ParameterFilter> parameterFilters;

    private Map<String, GridSubset> subSets;

    private static LayerListenerList listeners = new LayerListenerList();

    public GeoServerTileLayer(final CatalogConfiguration mediator, final LayerGroupInfo layerGroup) {
        this.mediator = mediator;
        this.layerId = null;
        this.layerGroupId = layerGroup.getId();
        GWCConfig configDefaults = mediator.getConfig();
        this.info = GeoServerTileLayerInfo.create(layerGroup, configDefaults);
    }

    public GeoServerTileLayer(final CatalogConfiguration mediator, final LayerInfo layerInfo) {
        this.mediator = mediator;
        this.layerId = layerInfo.getId();
        this.layerGroupId = null;
        GWCConfig configDefaults = mediator.getConfig();
        this.info = GeoServerTileLayerInfo.create(layerInfo, configDefaults);
    }

    @Override
    public String getId() {
        if (layerGroupId != null) {
            return layerGroupId;
        }
        return layerId;
    }

    @Override
    public String getName() {
        if (layerGroupId != null) {
            LayerGroupInfo layerGroupInfo = getLayerGroupInfo();
            return layerGroupInfo.getName();
        }
        LayerInfo layerInfo = getLayerInfo();
        ResourceInfo resource = layerInfo.getResource();
        return resource.getPrefixedName();
    }

    void setConfigErrorMessage(String configErrorMessage) {
        this.configErrorMessage = configErrorMessage;
    }

    public String getConfigErrorMessage() {
        return configErrorMessage;
    }

    @Override
    public List<ParameterFilter> getParameterFilters() {
        if (parameterFilters == null) {
            Set<String> cachedStyles = info.getCachedStyles();
            if (cachedStyles.size() > 0) {
                String defaultStyle = getStyles();
                if (defaultStyle == null) {
                    // may be null if backed by a LayerGroupInfo, but in that case
                    // cachedStyles.size() can't be > 0
                    throw new IllegalStateException(
                            "TileLayer backed by a LayerGroup should not have alternate styles!");
                }
                ParameterFilter stylesParameterFilter;
                stylesParameterFilter = createStylesParameterFilters(defaultStyle, cachedStyles);
                if (stylesParameterFilter != null) {
                    LOGGER.fine("Created STYLES parameter filter for layer " + getName()
                            + " and styles " + stylesParameterFilter.getLegalValues());
                    List<ParameterFilter> paramFilters = Arrays.asList(stylesParameterFilter);
                    this.parameterFilters = paramFilters;
                }
            }
        }
        return parameterFilters;
    }

    public void resetParameterFilters() {
        super.defaultParameterFilterValues = null;// reset default values
        this.parameterFilters = null;
    }

    /**
     * Returns whether this tile layer is enabled.
     * <p>
     * The layer is enabled if the following conditions apply:
     * <ul>
     * <li>Caching for this layer is enabled by configuration
     * <li>Its backing {@link LayerInfo} or {@link LayerGroupInfo} is enabled and not errored (as
     * per {@link LayerInfo#enabled()} {@link LayerGroupInfo#}
     * <li>The layer is not errored ({@link #getConfigErrorMessage() == null}
     * </ul>
     * The layer is enabled by configuration if: the {@code GWC.enabled} metadata property is set to
     * {@code true} in it's corresponding {@link LayerInfo} or {@link LayerGroupInfo}
     * {@link MetadataMap}, or there's no {@code GWC.enabled} property set at all but the global
     * {@link GWCConfig#isCacheLayersByDefault()} is {@code true}.
     * </p>
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
                LOGGER.finest("Layer " + getName() + "is not enabled due to config error: "
                        + getConfigErrorMessage());
            }
            return false;
        }
        boolean geoserverLayerEnabled;
        LayerInfo layerInfo = getLayerInfo();
        if (layerInfo != null) {
            geoserverLayerEnabled = layerInfo.enabled();
        } else {
            // LayerGroupInfo has no enabled property, so assume true
            geoserverLayerEnabled = true;
        }
        return tileLayerInfoEnabled && geoserverLayerEnabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        boolean oldVal = info.isEnabled();
        info.setEnabled(enabled);
        if (oldVal != enabled) {
            mediator.save(this);
        }
    }

    /**
     * 
     * @see org.geowebcache.layer.TileLayer#isQueryable()
     * @see WMS#isQueryable(LayerGroupInfo)
     * @see WMS#isQueryable(LayerInfo)
     */
    @Override
    public boolean isQueryable() {
        return mediator.isQueryable(this);
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
        if (getLayerInfo() == null) {
            LayerGroupInfo groupInfo = getLayerGroupInfo();
            try {
                ReferencedEnvelope bounds = groupInfo.getBounds();
                boolean lenient = true;
                latLongBbox = bounds.transform(wgs84LonFirst, lenient);
            } catch (Exception e) {
                String msg = "Can't get lat long bounds for layer group " + groupInfo.getName();
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
                tmp.init(latLongBbox.getMinX(), latLongBbox.getMaxX(), latLongBbox.getMinY(),
                        latLongBbox.getMaxY());
                latLongBbox = tmp;
            }
        }
        return latLongBbox;
    }

    /**
     * Creates parameter filters for each additional layer style
     * 
     * @return
     */
    public static StringParameterFilter createStylesParameterFilters(final String defaultStyle,
            final Set<String> styleNames) {
        Assert.notNull(defaultStyle, "defaultStyle");
        Assert.notNull(defaultStyle, "alternate styles");
        if (styleNames.size() == 0) {
            return null;
        }

        Set<String> possibleValues = new TreeSet<String>();
        possibleValues.add(defaultStyle);
        possibleValues.addAll(styleNames);
        StringParameterFilter styleParamFilter = new StringParameterFilter();
        styleParamFilter.setKey("STYLES");
        styleParamFilter.setDefaultValue(defaultStyle);
        styleParamFilter.getValues().addAll(possibleValues);
        return styleParamFilter;
    }

    /**
     * @return the {@link LayerInfo} for this layer, or {@code null} if it's backed by a
     *         {@link LayerGroupInfo} instead
     */
    public LayerInfo getLayerInfo() {
        if (layerId == null) {
            return null;
        }
        LayerInfo layerInfo = mediator.getLayerInfoById(layerId);
        return layerInfo;
    }

    /**
     * @return the {@link LayerGroupInfo} for this layer, or {@code null} if it's backed by a
     *         {@link LayerInfo} instead
     */
    public LayerGroupInfo getLayerGroupInfo() {
        if (layerGroupId == null) {
            return null;
        }
        LayerGroupInfo layerGroupInfo = mediator.getLayerGroupById(layerGroupId);
        return layerGroupInfo;
    }

    private ResourceInfo getResourceInfo() {
        LayerInfo layerInfo = getLayerInfo();
        return layerInfo == null ? null : layerInfo.getResource();
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
            keywords = new ArrayList<String>();
            for (KeywordInfo kw : resourceInfo.getKeywords()) {
                keywords.add(kw.getValue());
            }
        }
        meta = new LayerMetaInformation(title, description, keywords, contacts);
        return meta;
    }

    /**
     * The default style name for the layer, as advertised by its backing
     * {@link LayerInfo#getDefaultStyle()}, or {@code null} if this tile layer is backed by a
     * {@link LayerGroupInfo}.
     * <p>
     * As the default style is always cached, its name is not stored as part of this tile layer's
     * {@link GeoServerTileLayerInfo}. Instead it's 'live' and retrieved from the current
     * {@link LayerInfo} every time this method is invoked.
     * </p>
     * 
     * @see org.geowebcache.layer.TileLayer#getStyles()
     * @see GeoServerTileLayerInfo#getDefaultStyle()
     */
    @Override
    public String getStyles() {
        if (layerGroupId != null) {
            // there's no such thing as default style for a layer group
            return null;
        }
        LayerInfo layerInfo = getLayerInfo();
        StyleInfo defaultStyle = layerInfo.getDefaultStyle();
        if (defaultStyle == null) {
            setConfigErrorMessage("Underlying GeoSever Layer has no default style");
            return null;
        }
        return defaultStyle.getName();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getFeatureInfo
     * @see GWC#dispatchOwsRequest
     */
    @Override
    public Resource getFeatureInfo(ConveyorTile convTile, BoundingBox bbox, int height, int width,
            int x, int y) throws GeoWebCacheException {

        Map<String, String> params = buildGetFeatureInfo(convTile, bbox, height, width, x, y);
        Resource response;
        try {
            response = mediator.dispatchOwsRequest(params, (Cookie[]) null);
        } catch (Exception e) {
            throw new GeoWebCacheException(e);
        }
        return response;
    }

    private Map<String, String> buildGetFeatureInfo(ConveyorTile convTile, BoundingBox bbox,
            int height, int width, int x, int y) {
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
            Map<String, String> values = ServletUtils.selectedStringsFromMap(
                    convTile.servletReq.getParameterMap(),
                    convTile.servletReq.getCharacterEncoding(), "feature_count");
            featureCount = values.get("feature_count");
        }
        if (featureCount != null) {
            wmsParams.put("FEATURE_COUNT", featureCount);
        }

        Map<String, String> fullParameters = convTile.getFullParameters();
        if (fullParameters.isEmpty()) {
            fullParameters = getDefaultParameterFilters();
        }
        wmsParams.putAll(fullParameters);

        return wmsParams;
    }

    @Override
    public ConveyorTile getTile(ConveyorTile tile) throws GeoWebCacheException, IOException,
            OutsideCoverageException {
        MimeType mime = tile.getMimeType();
        final List<MimeType> formats = getMimeTypes();
        if (mime == null) {
            mime = formats.get(0);
        } else {
            if (!formats.contains(mime)) {
                throw new IllegalArgumentException(mime.getFormat()
                        + " is not a supported format for " + getName());
            }
        }

        final String tileGridSetId = tile.getGridSetId();
        final GridSubset gridSubset = getGridSubset(tileGridSetId);
        if (gridSubset == null) {
            throw new IllegalArgumentException("Requested gridset not found: " + tileGridSetId);
        }

        final long[] gridLoc = tile.getTileIndex();
        Assert.notNull(gridLoc);

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

    private static final CanonicalSet<GridLocObj> META_GRID_LOCKS = CanonicalSet
            .newInstance(GridLocObj.class);

    private ConveyorTile getMetatilingReponse(ConveyorTile tile, final boolean tryCache,
            final int metaX, final int metaY) throws GeoWebCacheException, IOException {

        // Acquire lock
        if (tryCache && tryCacheFetch(tile)) {
            return finalizeTile(tile);
        }

        final GeoServerMetaTile metaTile = createMetaTile(tile, metaX, metaY);
        final GridLocObj metaGridLoc;
        metaGridLoc = META_GRID_LOCKS.unique(new GridLocObj(metaTile.getMetaGridPos(), 32));
        synchronized (metaGridLoc) {
            // got the lock on the meta tile, try again
            if (tryCache && tryCacheFetch(tile)) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest("--> " + Thread.currentThread().getName()
                            + " returns cache hit for "
                            + Arrays.toString(metaTile.getMetaGridPos()));
                }
            } else {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("--> " + Thread.currentThread().getName()
                            + " submitting getMap request for meta grid location "
                            + Arrays.toString(metaTile.getMetaGridPos()) + " on " + metaTile);
                }
                RenderedImageMap map;
                try {
                    map = dispatchGetMap(tile, metaTile);
                    Assert.notNull(map, "Did not obtain a WebMap from GeoServer's Dispatcher");
                    metaTile.setWebMap(map);
                    saveTiles(metaTile, tile);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new GeoWebCacheException("Problem communicating with GeoServer", e);
                } finally {
                    META_GRID_LOCKS.remove(metaGridLoc);
                    metaTile.dispose();
                }
            }
        }

        return finalizeTile(tile);
    }

    private RenderedImageMap dispatchGetMap(final ConveyorTile tile, final MetaTile metaTile)
            throws Exception {

        Map<String, String> params = buildGetMap(tile, metaTile);
        WebMap map;
        try {
            HttpServletRequest actualRequest = tile.servletReq;
            Cookie[] cookies = actualRequest == null ? null : actualRequest.getCookies();

            mediator.dispatchOwsRequest(params, cookies);
            map = WEB_MAP.get();
            if (!(map instanceof RenderedImageMap)) {
                throw new IllegalStateException("Expected: RenderedImageMap, got " + map);
            }
        } finally {
            WEB_MAP.remove();
        }

        return (RenderedImageMap) map;
    }

    private GeoServerMetaTile createMetaTile(ConveyorTile tile, final int metaX, final int metaY) {
        GeoServerMetaTile metaTile;

        String tileGridSetId = tile.getGridSetId();
        GridSubset gridSubset = getGridSubset(tileGridSetId);
        MimeType responseFormat = tile.getMimeType();
        FormatModifier formatModifier = null;
        long[] tileGridPosition = tile.getTileIndex();
        int gutter = info.getGutter();
        String layerName = this.getName();
        metaTile = new GeoServerMetaTile(layerName, gridSubset, responseFormat, formatModifier,
                tileGridPosition, metaX, metaY, gutter, mediator);

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

        Map<String, String> filteredParams = tile.getFullParameters();
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

        return tile;
    }

    /**
     * @param tile
     */
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
    public void seedTile(ConveyorTile tile, boolean tryCache) throws GeoWebCacheException,
            IOException {

        int metaX = info.getMetaTilingX();
        int metaY = info.getMetaTilingY();
        if (!tile.getMimeType().supportsTiling()) {
            metaX = metaY = 1;
        }
        getMetatilingReponse(tile, tryCache, metaX, metaY);
    }

    @Override
    public void acquireLayerLock() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void releaseLayerLock() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private Map<String, GridSubset> getGrids(final ReferencedEnvelope latLonBbox,
            final GridSetBroker gridSetBroker) throws ConfigurationException {

        Set<String> cachedGridSetIds = info.getCachedGridSetIds();
        if (cachedGridSetIds.size() == 0) {
            throw new IllegalStateException("TileLayer " + getName()
                    + " has no gridsets configured");
        }

        Map<String, GridSubset> grids = new HashMap<String, GridSubset>(2);
        for (String gridSetId : cachedGridSetIds) {
            GridSet gridSet = gridSetBroker.get(gridSetId);
            if (gridSet == null) {
                throw new ConfigurationException("No GWC GridSet named '" + gridSetId + "' exists.");
            }
            BoundingBox extent = getBoundsFromWGS84Bounds(latLonBbox, gridSet.getSrs());
            Integer zoomStart = 0;
            Integer zoomStop = gridSet.getGridLevels().length - 1;
            GridSubset gridSubSet;
            gridSubSet = GridSubsetFactory.createGridSubSet(gridSet, extent, zoomStart, zoomStop);
            grids.put(gridSetId, gridSubSet);
        }

        return grids;
    }

    private BoundingBox getBoundsFromWGS84Bounds(final ReferencedEnvelope latLonBbox, final SRS srs) {
        Assert.notNull(latLonBbox);
        Assert.notNull(latLonBbox.getCoordinateReferenceSystem());
        Assert.notNull(srs);
        final double minX = latLonBbox.getMinX();
        final double minY = latLonBbox.getMinY();
        final double maxX = latLonBbox.getMaxX();
        final double maxY = latLonBbox.getMaxY();

        final String epsgCode = srs.toString();
        final boolean longitudeFirst = true;
        ReferencedEnvelope transformedBounds;

        BoundingBox bounds;
        if ("EPSG:900913".equals(epsgCode) || "EPSG:3857".equals(epsgCode)) {
            bounds = new BoundingBox(longToSphericalMercatorX(minX), latToSphericalMercatorY(minY),
                    longToSphericalMercatorX(maxX), latToSphericalMercatorY(maxY));
        } else {

            try {
                CoordinateReferenceSystem crs;
                crs = CRS.decode(epsgCode, longitudeFirst);
                Assert.notNull(crs);
                transformedBounds = latLonBbox.transform(crs, true, 20);
            } catch (NoSuchAuthorityCodeException e) {
                throw new RuntimeException(e);
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            } catch (TransformException e) {
                throw new RuntimeException(e);
            }
            bounds = new BoundingBox(transformedBounds.getMinX(), transformedBounds.getMinY(),
                    transformedBounds.getMaxX(), transformedBounds.getMaxY());
        }

        // BoundingBox bounds4326 = new BoundingBox(minX, minY, maxX, maxY);

        return bounds;
    }

    private double longToSphericalMercatorX(double x) {
        return (x / 180.0) * 20037508.34;
    }

    private double latToSphericalMercatorY(double y) {
        if (y > 85.05112) {
            y = 85.05112;
        }

        if (y < -85.05112) {
            y = -85.05112;
        }

        y = (Math.PI / 180.0) * y;
        double tmp = Math.PI / 4.0 + y / 2.0;
        return 20037508.34 * Math.log(Math.tan(tmp)) / Math.PI;
    }

    public GeoServerTileLayerInfo getInfo() {
        return info;
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getGridSubsets()
     */
    @Override
    public Set<String> getGridSubsets() {
        if (this.subSets == null) {
            ReferencedEnvelope latLongBbox = getLatLonBbox();
            try {
                GridSetBroker gridSetBroker = mediator.getGridSetBroker();
                this.subSets = getGrids(latLongBbox, gridSetBroker);
            } catch (ConfigurationException e) {
                String msg = "Can't create grids for '" + getName() + "': " + e.getMessage();
                LOGGER.log(Level.WARNING, msg, e);
                setConfigErrorMessage(msg);
                return Collections.emptySet();
            }
        }
        return new HashSet<String>(this.subSets.keySet());
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getGridSubset(java.lang.String)
     */
    @Override
    public GridSubset getGridSubset(String gridSetId) {
        if (!getGridSubsets().contains(gridSetId)) {
            return null;
        }
        return subSets.get(gridSetId);
    }

    /**
     * @see org.geowebcache.layer.TileLayer#removeGridSubset(java.lang.String)
     */
    @Override
    public GridSubset removeGridSubset(String gridSetId) {
        throw new UnsupportedOperationException("not yet implemented nor used");
    }

    /**
     * @see org.geowebcache.layer.TileLayer#addGridSubset(org.geowebcache.grid.GridSubset)
     */
    @Override
    public void addGridSubset(GridSubset gridSubset) {
        throw new UnsupportedOperationException("not yet implemented nor used");
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getUpdateSources()
     */
    @Override
    public List<UpdateSourceDefinition> getUpdateSources() {
        return Collections.emptyList();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#useETags()
     */
    @Override
    public boolean useETags() {
        return false;
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getFormatModifiers()
     */
    @Override
    public List<FormatModifier> getFormatModifiers() {
        return Collections.emptyList();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#setFormatModifiers(java.util.List)
     */
    @Override
    public void setFormatModifiers(List<FormatModifier> formatModifiers) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getMetaTilingFactors()
     */
    @Override
    public int[] getMetaTilingFactors() {
        return new int[] { info.getMetaTilingX(), info.getMetaTilingY() };
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

    /**
     * @throws UnsupportedOperationException
     * @see org.geowebcache.layer.TileLayer#setCacheBypassAllowed(boolean)
     */
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

    /**
     * @throws UnsupportedOperationException
     * @see org.geowebcache.layer.TileLayer#setBackendTimeout(int)
     */
    @Override
    public void setBackendTimeout(int seconds) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getMimeTypes()
     */
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
     * @see org.geowebcache.layer.TileLayer#getExpireClients(int)
     */
    @Override
    public int getExpireClients(int zoomLevel) {
        // TODO: make configurable
        return 0;
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getExpireCache(int)
     */
    @Override
    public int getExpireCache(int zoomLevel) {
        // TODO: make configurable
        return 0;
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
     * Empty method that returns {@code true}
     * 
     * @see org.geowebcache.layer.TileLayer#initialize(org.geowebcache.grid.GridSetBroker)
     */
    @Override
    public boolean initialize(GridSetBroker gridSetBroker) {
        return true;
    }

}
