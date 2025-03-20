/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import java.awt.Dimension;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.dispatch.GwcServiceDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RequestInfo;
import org.geoserver.util.DimensionWarning;
import org.geoserver.util.HTTPWarningAppender;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.capabilities.CapabilityUtil;
import org.geoserver.wms.capabilities.LegendSample;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
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
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.ExpirationRule;
import org.geowebcache.layer.LayerListenerList;
import org.geowebcache.layer.MetaTile;
import org.geowebcache.layer.ProxyLayer;
import org.geowebcache.layer.TileJSONProvider;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerListener;
import org.geowebcache.layer.meta.ContactInformation;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.meta.MetadataURL;
import org.geowebcache.layer.meta.TileJSON;
import org.geowebcache.layer.meta.VectorLayerMetadata;
import org.geowebcache.layer.updatesource.UpdateSourceDefinition;
import org.geowebcache.locks.LockProvider.Lock;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileObject;
import org.geowebcache.util.GWCVars;
import org.geowebcache.util.ServletUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.vfny.geoserver.util.ResponseUtils;

/** GeoServer {@link TileLayer} implementation. Delegates to {@link GeoServerTileLayerInfo} for layer configuration. */
public class GeoServerTileLayer extends TileLayer implements ProxyLayer, TileJSONProvider {

    private static final Logger LOGGER = Logging.getLogger(GeoServerTileLayer.class);
    public static final int ENV_TX_POINTS = Integer.parseInt(System.getProperty("GWC_ENVELOPE_TX_POINTS", "5"));

    private final GeoServerTileLayerInfo info;

    public static final String GWC_SEED_INTERCEPT_TOKEN = "GWC_SEED_INTERCEPT";

    public static final ThreadLocal<WebMap> WEB_MAP = new ThreadLocal<>();
    public static final ThreadLocal<Set<DimensionWarning>> DIMENSION_WARNINGS = new ThreadLocal<>();

    private String configErrorMessage;

    /**
     * Lazily and atomically initialized mapping of {@link GridSubset#getName() name} to {@link GridSubset}.
     *
     * <p>Not to be used directly but through {@link #gridSubsets()} for querying. The map is not synchronized nor a
     * {@code ConcurrentMap}. The reference held by this variable is reset to null instead on mutating operations such
     * as {@link #addGridSubset} and {@link #removeGridSubset} for it to be re-computed from the corresponding
     * {@link GeoServerTileLayerInfo} when needed.
     */
    private final AtomicReference<Map<String, GridSubset>> _subSets;

    private static LayerListenerList listeners = new LayerListenerList();

    private final GridSetBroker gridSetBroker;

    private Catalog catalog;

    /**
     * The {@link Catalog}'s {@link LayerInfo} or {@link LayerGroupInfo} id, when created through a constructor that
     * receives the id instead of the {@link PublishedInfo} instance itself. See {@link #getPublishedInfo()}
     */
    private final String publishedInfoId;

    /**
     * Atomic reference to the {@link PublishedInfo} this tile layer references. Either assigned directly at a
     * constructor that receives the {@code PublishedInfo}, or computed lazily and atomically from the
     * {@code PublishedInfo} id held at {@link #publishedInfoId}
     */
    private final AtomicReference<PublishedInfo> _publishedInfo;

    private LegendSample legendSample;

    private WMS wms;

    public GeoServerTileLayer(
            final PublishedInfo publishedInfo, final GWCConfig configDefaults, final GridSetBroker gridsets) {
        checkNotNull(publishedInfo, "publishedInfo");
        checkNotNull(gridsets, "gridsets");
        checkNotNull(configDefaults, "configDefaults");

        this.gridSetBroker = gridsets;
        this._publishedInfo = new AtomicReference<>(publishedInfo);
        this.publishedInfoId = publishedInfo.getId();
        this.info = TileLayerInfoUtil.loadOrCreate(getPublishedInfo(), configDefaults);
        this._subSets = new AtomicReference<>();
    }

    public GeoServerTileLayer(
            final PublishedInfo publishedInfo, final GridSetBroker gridsets, final GeoServerTileLayerInfo state) {
        checkNotNull(publishedInfo, "publishedInfo");
        checkNotNull(gridsets, "gridsets");
        checkNotNull(state, "state");

        this.gridSetBroker = gridsets;
        this._publishedInfo = new AtomicReference<>(publishedInfo);
        this.publishedInfoId = publishedInfo.getId();
        this.info = state;
        this._subSets = new AtomicReference<>();
        TileLayerInfoUtil.checkAutomaticStyles(publishedInfo, state);
    }

    public GeoServerTileLayer(
            final Catalog catalog,
            final String publishedId,
            final GridSetBroker gridsetBroker,
            final GeoServerTileLayerInfo state) {
        checkNotNull(catalog, "catalog");
        checkNotNull(publishedId, "publishedId");
        checkNotNull(gridsetBroker, "gridsets");
        checkNotNull(state, "state");

        this.gridSetBroker = gridsetBroker;
        this.catalog = catalog;
        this.publishedInfoId = publishedId;
        this._publishedInfo = new AtomicReference<>();
        this.info = state;
        this._subSets = new AtomicReference<>();
    }

    protected GeoServerTileLayer(final GeoServerTileLayer layer) {
        this.gridSetBroker = layer.gridSetBroker;
        this.catalog = layer.catalog;
        this.publishedInfoId = layer.publishedInfoId;
        this._publishedInfo = new AtomicReference<>();
        this.info = layer.info;
        this._subSets = new AtomicReference<>();
        this.legendSample = layer.legendSample;
        this.wms = layer.wms;
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
            return getContextualName();
        }
        return info.getName();
    }

    /**
     * Returns the local name if in a workspace specific service, full name otherwise. It's not done fully automatically
     * in getName because it would break tile lookups in blob stores (getName limits this behavior to the
     * GetCapabilities request)
     */
    public String getContextualName() {
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
        return new ArrayList<>(info.getParameterFilters());
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
     *   <li>Its backing {@link LayerInfo} or {@link LayerGroupInfo} is enabled and not errored (as per
     *       {@link LayerInfo#enabled()} {@link LayerGroupInfo}
     *   <li>The layer is not errored ({@link #getConfigErrorMessage() == null}
     * </ul>
     *
     * <p>The layer is enabled by configuration if: the {@code GWC.enabled} metadata property is set to {@code true} in
     * it's corresponding {@link LayerInfo} or {@link LayerGroupInfo} {@link MetadataMap}, or there's no
     * {@code GWC.enabled} property set at all but the global {@link GWCConfig#isCacheLayersByDefault()} is
     * {@code true}.
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
                LOGGER.finest("Layer " + getName() + "is not enabled due to config error: " + getConfigErrorMessage());
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

    /**
     * LayerInfo or LayerGroupInfo being drawn as a tile.
     *
     * @return the {@link LayerInfo} or {@link LayerGroupInfo} this tile layer is associated with
     * @throws IllegalStateException if this {@code GeoServerTileLayer} was created with a {@link PublishedInfo} id but
     *     such object does not exist in the {@link Catalog}
     * @implNote The returned {@link PublishedInfo} is either assigned at construction time, or lazily obtained from the
     *     catalog here in a thread contention free way
     */
    public PublishedInfo getPublishedInfo() {
        PublishedInfo publishedInfo = this._publishedInfo.get();
        while (publishedInfo == null) {
            // see if it's a layer or a layer group
            PublishedInfo catalogLayer = catalog.getLayer(publishedInfoId);
            if (catalogLayer == null) {
                catalogLayer = catalog.getLayerGroup(publishedInfoId);
            }
            if (catalogLayer == null) {
                throw new IllegalStateException("Could not locate a layer or layer group with id "
                        + publishedInfoId
                        + " within GeoServer configuration, the GWC configuration seems to be out of "
                        + "synch");
            } else {
                TileLayerInfoUtil.checkAutomaticStyles(catalogLayer, info);
            }
            this._publishedInfo.compareAndSet(null, catalogLayer);
            publishedInfo = this._publishedInfo.get();
        }
        return publishedInfo;
    }

    private ResourceInfo getResourceInfo() {
        PublishedInfo publishedInfo = getPublishedInfo();
        return publishedInfo instanceof LayerInfo ? ((LayerInfo) publishedInfo).getResource() : null;
    }

    /**
     * Overrides to return a dynamic view of the backing {@link LayerInfo} or {@link LayerGroupInfo} metadata adapted to
     * GWC
     *
     * @see org.geowebcache.layer.TileLayer#getMetaInformation()
     */
    @Override
    public LayerMetaInformation getMetaInformation() {
        String title = getName();
        String description = "";
        List<String> keywords = Collections.emptyList();
        List<ContactInformation> contacts = Collections.emptyList();

        PublishedInfo publishedInfo = getPublishedInfo();
        ResourceInfo resourceInfo =
                publishedInfo instanceof LayerInfo ? ((LayerInfo) publishedInfo).getResource() : null;
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
        LayerMetaInformation meta = new LayerMetaInformation(title, description, keywords, contacts);
        return meta;
    }

    /**
     * The default style name for the layer, as advertised by its backing {@link LayerInfo#getDefaultStyle()}, or
     * {@code null} if this tile layer is backed by a {@link LayerGroupInfo}.
     *
     * <p>As the default style is always cached, its name is not stored as part of this tile layer's
     * {@link GeoServerTileLayerInfo}. Instead it's 'live' and retrieved from the current {@link LayerInfo} every time
     * this method is invoked.
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
    public Resource getFeatureInfo(ConveyorTile convTile, BoundingBox bbox, int height, int width, int x, int y)
            throws GeoWebCacheException {

        Map<String, String> params = buildGetFeatureInfo(convTile, bbox, height, width, x, y);
        Resource response;
        try {
            response = GWC.get().dispatchOwsRequest(params, null);
        } catch (Exception e) {
            throw new GeoWebCacheException(e);
        }
        return response;
    }

    private Map<String, String> buildGetFeatureInfo(
            ConveyorTile convTile, BoundingBox bbox, int height, int width, int x, int y) {
        Map<String, String> wmsParams = new HashMap<>();
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
                    convTile.servletReq.getParameterMap(), convTile.servletReq.getCharacterEncoding(), "feature_count");
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
    public ConveyorTile getTile(ConveyorTile tile) throws GeoWebCacheException, IOException, OutsideCoverageException {
        MimeType mime = tile.getMimeType();
        final List<MimeType> formats = getMimeTypes();
        if (mime == null) {
            mime = formats.get(0);
        } else {
            if (!formats.contains(mime)) {
                throw new IllegalArgumentException(mime.getFormat() + " is not a supported format for " + getName());
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

        int metaX;
        int metaY;
        if (mime.supportsTiling()) {
            metaX = info.getMetaTilingX();
            metaY = info.getMetaTilingY();
        } else {
            metaX = metaY = 1;
        }

        ConveyorTile returnTile = getMetatilingResponse(tile, true, metaX, metaY);

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

    protected ConveyorTile getMetatilingResponse(
            ConveyorTile conveyorTile, final boolean tryCache, final int metaX, final int metaY)
            throws GeoWebCacheException, IOException {

        if (tryCache && tryCacheFetch(conveyorTile)) {
            return finalizeTile(conveyorTile);
        }

        final GeoServerMetaTile metaTile = createMetaTile(conveyorTile, metaX, metaY);

        // should we use the metatile executor?
        Executor executor = GWC.get().getMetaTilingExecutor();
        if (Dispatcher.REQUEST.get() == null) {
            // Metatiling concurrency is disabled if this isn't a user request.
            // Concurrency reduces the user-experienced latency but isn't useful for seeding.
            // In fact, it would be harmful for seeding  because it makes it more difficult for an
            // administrator to control the amount of resource usage for significant seeding jobs.
            executor = null;
        }

        /* ****************** Acquire lock on metatile ******************* */
        final Lock metaTileLock = getLock(buildMetaTileLockKey(conveyorTile, metaTile));
        try {
            boolean foundInCache = false;
            if (tryCache) {
                // If we have an executor, tiles are saved asynchronously so we need to grab a
                // tile lock to wait for the potential tile save to complete. Otherwise just read.
                if (executor == null) {
                    foundInCache = fetchPrimaryTile(conveyorTile, metaTile);
                } else {
                    /* ****************** Acquire lock on individual tile ******************* */
                    // Will block here if there is an async thread currently saving this tile
                    String lockKey = buildTileLockKey(conveyorTile, conveyorTile.getTileIndex());
                    final Lock tileLock = getLock(lockKey);
                    try {
                        foundInCache = fetchPrimaryTile(conveyorTile, metaTile);
                    } finally {
                        /* ****************** Release lock on individual tile ******************* */
                        tileLock.release();
                    }
                }
            }

            if (!foundInCache) {
                LOGGER.log(
                        Level.FINER,
                        () -> "--> "
                                + Thread.currentThread().getName()
                                + " submitting getMap request for meta grid location "
                                + Arrays.toString(metaTile.getMetaGridPos())
                                + " on "
                                + metaTile);
                try {
                    computeMetaTile(conveyorTile, metaTile, executor);
                } catch (Exception e) {
                    Throwables.throwIfInstanceOf(e, GeoWebCacheException.class);
                    throw new GeoWebCacheException("Problem communicating with GeoServer", e);
                }
            }

        } finally {
            /* ****************** Release lock on metatile ******************* */
            metaTileLock.release();
        }

        return finalizeTile(conveyorTile);
    }

    /**
     * Looks up the primary tile in a given meta-tile (the requested one). If the tile is found it means it has been
     * computed since the first check, and the metatile gets disposed in preparation for an immediate return.
     */
    private boolean fetchPrimaryTile(ConveyorTile conveyorTile, GeoServerMetaTile metaTile) {
        // quick return for the simple case
        if (!tryCacheFetch(conveyorTile)) return false;

        // otherwise log success, dispose the meta tile and return true
        if (LOGGER.isLoggable(Level.FINEST)) {
            String threadName = Thread.currentThread().getName();
            String gridPos = Arrays.toString(metaTile.getMetaGridPos());
            LOGGER.finest("--> " + threadName + " returns cache hit for " + gridPos);
        }
        metaTile.dispose();
        return true;
    }

    /** Acquires an exclusive lock for the given key (e.g., for a metatile or individual tile) */
    private Lock getLock(String lockKey) throws GeoWebCacheException {
        return GWC.get().getLockProvider().getLock(lockKey);
    }

    private void computeMetaTile(ConveyorTile conveyorTile, GeoServerMetaTile metaTile, Executor executor)
            throws Exception {
        WebMap map;
        long requestTime = System.currentTimeMillis();

        // Actually fetch the metatile data
        map = dispatchGetMap(conveyorTile, metaTile);

        checkNotNull(map, "Did not obtain a WebMap from GeoServer's Dispatcher");
        metaTile.setWebMap(map);

        setupCachingStrategy(conveyorTile);

        final long[][] gridPositions = metaTile.getTilesGridPositions();
        final long[] gridLoc = conveyorTile.getTileIndex();
        final GridSubset gridSubset = getGridSubset(conveyorTile.getGridSetId());
        final int numberOfTiles = gridPositions.length;

        final int zoomLevel = (int) gridLoc[2];
        final boolean store = this.getExpireCache(zoomLevel) != GWCVars.CACHE_DISABLE_CACHE;

        List<CompletableFuture<?>> completableFutures = new ArrayList<>();

        // A latch to track whether we've locked all the individual tiles or not, before
        // we can release the metatile lock.
        CountDownLatch tileLockLatch = new CountDownLatch(numberOfTiles);

        for (int tileIndex = 0; tileIndex < numberOfTiles; tileIndex++) {
            final long[] gridPos = gridPositions[tileIndex];
            final int finalTileIndex = tileIndex;

            boolean isConveyorTile = Arrays.equals(gridLoc, gridPos);
            if (isConveyorTile || store) {
                if (!gridSubset.covers(gridPos)) {
                    // edge tile outside coverage, do not store it
                    tileLockLatch.countDown();
                    continue;
                }

                Supplier<Resource> encodeTileTask = encodeTileTask(metaTile, tileIndex);

                if (isConveyorTile) {
                    // Always encode the conveyor tile on the main thread, and set a tentative
                    // creation time for it (the actual save time will be later, the first
                    // time modification check from the client will re-fetch the tile
                    Resource resource = encodeTileTask.get();
                    conveyorTile.setBlob(resource);
                    conveyorTile.getStorageObject().setCreated(requestTime);

                    // Saving the conveyor tile in the cache can either happen
                    // asynchronously or on the main thread
                    Runnable saveTileTask = withTileLock(
                            conveyorTile,
                            tileLockLatch,
                            gridPos,
                            saveTileTask(metaTile, tileIndex, conveyorTile, resource, requestTime));
                    if (executor == null) {
                        // Save in cache on main thread if there's no executor
                        saveTileTask.run();
                    } else {
                        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(saveTileTask, executor);
                        completableFutures.add(completableFuture);
                    }
                } else {
                    // For all other tiles, either encode/save fully asynchronously or
                    // fully on the main thread
                    Runnable tileSaver = () -> {
                        Resource resource = encodeTileTask.get();
                        saveTileTask(metaTile, finalTileIndex, conveyorTile, resource, requestTime)
                                .run();
                    };
                    Runnable encodeAndSaveTask =
                            withTileLock(conveyorTile, tileLockLatch, gridPos, withRasterCleaner(tileSaver));

                    if (executor == null) {
                        // Run on main thread if there's no executor
                        encodeAndSaveTask.run();
                    } else {
                        // Fully asynchronous
                        CompletableFuture<Void> completableFuture =
                                CompletableFuture.runAsync(encodeAndSaveTask, executor);
                        completableFutures.add(completableFuture);
                    }
                }
            }
        }

        // Wait until we've obtained locks on all individual tiles before proceeding
        tileLockLatch.await();

        // Dispose of meta-tile when all completable futures are done
        if (!completableFutures.isEmpty()) {
            runAsyncAfterAllFuturesComplete(completableFutures, metaTile::dispose, executor);
        } else {
            // There were no asynchronous tasks, everything was run on the main thread
            // so we can dispose of the meta-tile right away
            metaTile.dispose();
        }
    }

    private void runAsyncAfterAllFuturesComplete(
            List<CompletableFuture<?>> futures, Runnable runnable, Executor executor) {
        CompletableFuture<?>[] futureArray = futures.toArray(new CompletableFuture[0]);
        CompletableFuture<Void> afterAllFutures = CompletableFuture.allOf(futureArray);
        afterAllFutures.thenRunAsync(runnable, executor);
    }

    private Runnable withRasterCleaner(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } finally {
                // Raster cleaner normally runs as a dispatcher callback but in this case
                // there is no dispatcher request on this thread. Neglecting to cleanup
                // the images from the various RenderedImageMapResponse implementations
                // would cause a severe memory leak.
                RasterCleaner.cleanup();
            }
        };
    }

    /**
     * Locks a tile before running the runnable and releases the lock at the end.
     *
     * <p>Also counts down the latch to track how many locks have been acquired.
     */
    private Runnable withTileLock(
            ConveyorTile conveyorTile, CountDownLatch tileLockLatch, long[] gridPosition, Runnable runnable) {
        return () -> {
            try {
                Lock tileLock = getLock(buildTileLockKey(conveyorTile, gridPosition));
                try {
                    tileLockLatch.countDown();
                    runnable.run();
                } finally {
                    tileLock.release();
                }
            } catch (GeoWebCacheException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    /** Creates a task for encoding a single tile */
    private Supplier<Resource> encodeTileTask(GeoServerMetaTile metaTile, int tileIndex) {
        return () -> {
            ByteArrayResource resource = new ByteArrayResource(16 * 1024);

            boolean completed;
            try {
                completed = metaTile.writeTileToStream(tileIndex, resource);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to write image tile to ByteArrayOutputStream", e);
                throw new RuntimeException(e);
            }

            if (!completed) {
                LOGGER.severe("metaTile.writeTileToStream returned false, no tiles saved");
            }
            return resource;
        };
    }

    /** Creates a task for saving a single tile to the cache. */
    private Runnable saveTileTask(
            GeoServerMetaTile metaTile, int tileIndex, ConveyorTile tileProto, Resource resource, long requestTime) {
        return () -> {
            try {
                final long[][] gridPositions = metaTile.getTilesGridPositions();
                long[] gridPosition = gridPositions[tileIndex];
                long[] idx = {gridPosition[0], gridPosition[1], gridPosition[2]};

                TileObject tile = TileObject.createCompleteTileObject(
                        this.getName(),
                        idx,
                        tileProto.getGridSetId(),
                        tileProto.getMimeType().getFormat(),
                        tileProto.getParameters(),
                        resource);
                tile.setCreated(requestTime);

                // Save tile to storage
                StorageBroker storageBroker = tileProto.getStorageBroker();
                if (tileProto.isMetaTileCacheOnly()) {
                    storageBroker.putTransient(tile);
                } else {
                    storageBroker.put(tile);
                }
                tileProto.getStorageObject().setCreated(tile.getCreated());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    /**
     * Based on configuration, sets caching to be permanent on blobstore, or to use the transient metatile cache
     * instead. Must be called after dispatching the GetMap request, in order to have warnings available in the
     * response.
     */
    private void setupCachingStrategy(ConveyorTile tile) {
        // skip cache based on gridset caching levels configuration
        final GridSubset gridSubset = getGridSubset(tile.getGridSetId());
        final int zLevel = (int) tile.getTileIndex()[2];

        if (!gridSubset.shouldCacheAtZoom(zLevel)) {
            LOGGER.fine("Skipping tile caching because zoom level is not configured for caching");
            tile.setMetaTileCacheOnly(true);
            return;
        }

        Set<DimensionWarning.WarningType> warningSkips = info.getCacheWarningSkips();
        if (warningSkips != null && HTTPWarningAppender.anyMatch(warningSkips)) {
            LOGGER.fine("Skipping tile caching due to a WMS dimension warning");
            tile.setMetaTileCacheOnly(true);
        }
    }

    /**
     * Builds a unique string for a given metatile.
     *
     * @param tilePrototype A ConveyorTile that has all the metadata we require.
     * @param metaTile The actual metatile we are generating a unique key for.
     */
    private String buildMetaTileLockKey(ConveyorTile tilePrototype, GeoServerMetaTile metaTile) {
        return buildLockKey(tilePrototype, "gwc_metatile_", metaTile.getMetaGridPos());
    }

    /**
     * Builds a unique string for a given tile.
     *
     * @param tilePrototype A ConveyorTile that has all the metadata we require by may not be the actual tile we need a
     *     key for.
     * @param gridPosition The grid position of the ACTUAL tile we need a key for.
     */
    private String buildTileLockKey(ConveyorTile tilePrototype, long[] gridPosition) {
        return buildLockKey(tilePrototype, "gwc_tile_", gridPosition);
    }

    private String buildLockKey(ConveyorTile tilePrototype, String prefix, long[] position) {
        StringBuilder lockKey = new StringBuilder();

        lockKey.append(prefix);

        long x = position[0];
        long y = position[1];
        long z = position[2];

        lockKey.append(tilePrototype.getLayerId());
        lockKey.append("_").append(tilePrototype.getGridSetId());
        lockKey.append("_").append(x).append("_").append(y).append("_").append(z);
        if (tilePrototype.getParametersId() != null) {
            lockKey.append("_").append(tilePrototype.getParametersId());
        }
        lockKey.append(".").append(tilePrototype.getMimeType().getFileExtension());

        return lockKey.toString();
    }

    private WebMap dispatchGetMap(final ConveyorTile tile, final MetaTile metaTile) throws Exception {

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
            Set<DimensionWarning> warnings = DIMENSION_WARNINGS.get();
            if (warnings != null) warnings.forEach(w -> HTTPWarningAppender.addWarning(w));
        } finally {
            WEB_MAP.remove();
        }

        return map;
    }

    private GeoServerMetaTile createMetaTile(ConveyorTile tile, final int metaX, final int metaY) {

        String tileGridSetId = tile.getGridSetId();
        GridSubset gridSubset = getGridSubset(tileGridSetId);
        MimeType responseFormat = tile.getMimeType();
        FormatModifier formatModifier = null;
        long[] tileGridPosition = tile.getTileIndex();
        int gutter = responseFormat.isVector() ? 0 : info.getGutter();
        GeoServerMetaTile metaTile = new GeoServerMetaTile(
                gridSubset, responseFormat, formatModifier, tileGridPosition, metaX, metaY, gutter);

        return metaTile;
    }

    private Map<String, String> buildGetMap(final ConveyorTile tile, final MetaTile metaTile)
            throws ParameterException {

        Map<String, String> params = new HashMap<>();

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

        // we have the layer's ID (the one with a GUID), use the catalog to get its workspace name
        GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
        // gs might be null in test case runs.
        if ((gs != null) && !gs.getGlobal().isGlobalServices()) {
            PublishedInfo publishedInfo = getPublishedInfo();
            if (publishedInfo instanceof LayerInfo) {
                LayerInfo layerInfo = (LayerInfo) publishedInfo;
                params.put("WORKSPACE", layerInfo.getResource().getNamespace().getName());
            } else if (publishedInfo instanceof LayerGroupInfo) {
                LayerGroupInfo groupInfo = (LayerGroupInfo) publishedInfo;
                WorkspaceInfo workspace = groupInfo.getWorkspace();
                if (workspace == null) {
                    throw new ParameterException("Global web services are disabled, global LayerGroup "
                            + groupInfo.getName()
                            + " inaccessible");
                }
                params.put("WORKSPACE", workspace.getName());
            }
        }

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
            // do not call setExpirationHeaders from superclass, we have a more complex logic
            // to determine caching headers here
            Map<String, String> headers = new HashMap<>();
            GWC.setCacheControlHeaders(headers, this, (int) tile.getTileIndex()[2]);
            headers.forEach((k, v) -> tile.servletResp.setHeader(k, v));
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
            return getMetatilingResponse(tile, false, 1, 1);
        } catch (IOException e) {
            throw new GeoWebCacheException(e);
        }
    }

    @Override
    public ConveyorTile doNonMetatilingRequest(ConveyorTile tile) throws GeoWebCacheException {
        try {
            return getMetatilingResponse(tile, true, 1, 1);
        } catch (IOException e) {
            throw new GeoWebCacheException(e);
        }
    }

    @Override
    public void seedTile(ConveyorTile tile, boolean tryCache) throws GeoWebCacheException, IOException {

        // Ignore a seed call on a tile that's outside the cached grid levels range
        final GridSubset gridSubset = getGridSubset(tile.getGridSetId());
        final int zLevel = (int) tile.getTileIndex()[2];
        if (!gridSubset.shouldCacheAtZoom(zLevel)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Ignoring seed call on tile " + tile + " as it's outside the cacheable zoom level range");
            }
            return;
        }

        int metaX = info.getMetaTilingX();
        int metaY = info.getMetaTilingY();
        if (!tile.getMimeType().supportsTiling()) {
            metaX = metaY = 1;
        }
        getMetatilingResponse(tile, tryCache, metaX, metaY);
    }

    /** @see org.geowebcache.layer.TileLayer#getGridSubsets() */
    @Override
    public Set<String> getGridSubsets() {
        Set<XMLGridSubset> gridSubsets = info.getGridSubsets();
        if (gridSubsets == null) return Collections.emptySet();
        return gridSubsets.stream().map(ss -> ss.getGridSetName()).collect(Collectors.toSet());
    }

    @Override
    public GridSubset getGridSubset(final String gridSetId) {
        return gridSubsets().get(gridSetId);
    }

    /**
     * Returns the cached grid subsets from {@link #_subSets} if present, or atomically computes it and returns the
     * cached reference.
     */
    private Map<String, GridSubset> gridSubsets() {
        Map<String, GridSubset> gridSubsets = this._subSets.get();
        // compute the grid subsets atomically without thread contention
        while (gridSubsets == null) {
            // pass null as the update value (first arg) so it's lazly computed inside the
            // function only if needed
            gridSubsets = this._subSets.accumulateAndGet(null, (currValue, nullNewValue) -> {
                if (currValue == null) {
                    return computeGridSubsets();
                }
                // returning null when the current value is not null, prevents
                // accumulateAndGet from replacing the current reference
                return null;
            });
        }
        return gridSubsets;
    }

    @Override
    public GridSubset removeGridSubset(String gridSetId) {
        gridSubsets();
        final GridSubset oldValue = gridSubsets().remove(gridSetId);

        Set<XMLGridSubset> gridSubsets = new HashSet<>(info.getGridSubsets());
        for (Iterator<XMLGridSubset> it = gridSubsets.iterator(); it.hasNext(); ) {
            if (it.next().getGridSetName().equals(gridSetId)) {
                it.remove();
                break;
            }
        }
        info.setGridSubsets(gridSubsets);
        // reset lazy value
        this._subSets.set(null);
        return oldValue;
    }

    @Override
    public void addGridSubset(GridSubset gridSubset) {
        XMLGridSubset gridSubsetInfo = new XMLGridSubset(gridSubset);
        if (gridSubset instanceof DynamicGridSubset) {
            gridSubsetInfo.setExtent(null);
        }
        Set<XMLGridSubset> gridSubsets = new HashSet<>(info.getGridSubsets());
        gridSubsets.add(gridSubsetInfo);
        info.setGridSubsets(gridSubsets);
        // reset lazy value
        this._subSets.set(null);
    }

    /** Can be called to notify the layer bounds changed. Resets the grid subsets cache. */
    public void boundsChanged() {
        this._subSets.set(null);
    }

    /**
     * Actually computes the layer's {@link GridSubset}s. This method is intended as a helper for {@link #gridSubsets()}
     * to compute the mappings atomically in a lock-free way
     */
    private Map<String, GridSubset> computeGridSubsets() {
        try {
            return getGrids(gridSetBroker);
        } catch (ConfigurationException e) {
            String msg = "Can't create grids for '" + getName() + "': " + e.getMessage();
            LOGGER.log(Level.WARNING, msg, e);
            setConfigErrorMessage(msg);
            throw new IllegalStateException(e);
        }
    }

    private Map<String, GridSubset> getGrids(final GridSetBroker gridSetBroker) throws ConfigurationException {
        Set<XMLGridSubset> cachedGridSets = info.getGridSubsets();
        if (cachedGridSets.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, GridSubset> grids = new HashMap<>(2);
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
                        final String msg = "Can't compute bounds for tile layer "
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
                    LOGGER.log(Level.WARNING, "Error computing layer bounds, assuming whole GridSet bounds", e);
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
        final ResourceInfo resourceInfo = getResourceInfo();
        if (resourceInfo != null) {
            // projection policy for these bounds are already taken care of by the geoserver
            // configuration
            try {
                nativeBounds = resourceInfo.boundingBox();
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
                String msg = "Can't compute tile layer bounds out of resource native bounds for CRS " + srs;
                LOGGER.log(Level.WARNING, msg, e);
                throw new IllegalArgumentException(msg, e);
            }
            LOGGER.log(
                    Level.FINE, "Can't compute tile layer bounds out of resource " + "native bounds for CRS " + srs, e);

            final CoordinateReferenceSystem nativeCrs = nativeBounds.getCoordinateReferenceSystem();

            try {

                ReferencedEnvelope targetAovBounds = new ReferencedEnvelope(targetAov.getEnvelopeInternal(), targetCrs);
                // transform target AOV in target CRS to native CRS
                ReferencedEnvelope targetAovInNativeCrs = targetAovBounds.transform(nativeCrs, true, ENV_TX_POINTS);
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

        BoundingBox targetBbox = new BoundingBox(
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
        List<MimeType> mimeTypes = new ArrayList<>(mimeFormats.size());
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
     * Gets the expiration time to be declared to clients, calculated based on the metadata of the underlying
     * {@link LayerInfo} or {@link LayerGroupInfo} This calculation can be overridden by setting
     * {@link GeoServerTileLayerInfo#setExpireClients(int)}
     *
     * @param zoomLevel ignored
     * @return the expiration time
     * @see org.geowebcache.layer.TileLayer#getExpireClients(int)
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
                        "Found a GeoServerTileLayer that is not base on either "
                                + "LayerInfo or LayerGroupInfo, setting its max age to 0");
            }
            return 0;
        }
    }

    /**
     * Returns the max age of a layer group using layer group configuration or by looking for the minimum max age of its
     * components
     */
    private int getGroupMaxAge(LayerGroupInfo lg) {
        if (isCachingEnabled(lg.getMetadata())) {
            return getCacheMaxAge(lg.getMetadata());
        }

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
        if (isCachingEnabled(metadata)) {
            return getCacheMaxAge(metadata);
        }

        return 0;
    }

    private boolean isCachingEnabled(MetadataMap metadata) {
        Boolean value = metadata.get(ResourceInfo.CACHING_ENABLED, Boolean.class);

        return value != null ? value : false;
    }

    private int getCacheMaxAge(MetadataMap metadata) {
        Integer value = metadata.get(ResourceInfo.CACHE_AGE_MAX, Integer.class);

        return value != null ? value : 0;
    }

    /**
     * Gets the expiration time for tiles in the cache, based on the expiration rules from
     * {@link GeoServerTileLayerInfo#getExpireCacheList()}. If no matching rules are found, defaults to
     * {@link GeoServerTileLayerInfo#getExpireCache()}
     *
     * @param zoomLevel the zoom level used to filter expiration rules
     * @return the expiration time for tiles at the given zoom level
     * @see org.geowebcache.layer.TileLayer#getExpireCache(int)
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
        List<String> typeStrings = ((WMS) GeoServerExtensions.bean("wms")).getAvailableFeatureInfoFormats();
        List<MimeType> types = new ArrayList<>(typeStrings.size());
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
            for (LayerInfo layer : Iterables.filter(((LayerGroupInfo) published).getLayers(), LayerInfo.class)) {
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
                gwcMetadataLinks.add(new MetadataURL(gsMetadata.getMetadataType(), gsMetadata.getType(), new URL(url)));
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
        final PublishedInfo publishedInfo = getPublishedInfo();
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
                scalesDenominator = CapabilityUtil.searchMinMaxScaleDenominator(Collections.singleton(styleInfo));
            } catch (Exception exception) {
                throw new RuntimeException(
                        String.format(
                                "Error searching max and min scale denominators for style '%s'.", styleInfo.getName()),
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
                                buildURL(baseUrl, legendInfo.getOnlineResource(), null, URLMangler.URLType.SERVICE));
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
                            LOGGER.warning("Default legend format ("
                                    + finalFormat
                                    + ")is not supported (jai not available?), can't add LegendURL element");
                        }
                        continue;
                    }
                } catch (Exception exception) {
                    LOGGER.log(Level.WARNING, "Error getting LegendURL dimensions from sample", exception);
                }
                String layerName = layerInfo.prefixedName();
                Map<String, String> params = params(
                        "service",
                        "WMS",
                        "request",
                        "GetLegendGraphic",
                        "version",
                        "1.1.0",
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
                        .withCompleteUrl(buildURL(baseUrl, "ows", params, URLMangler.URLType.RESOURCE));
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
     * Gets the base URL of the server, this value is retrieved from the current HTTP request. If no HTTP request is in
     * progress NULL is returned. Only the use cases where an OWS service or a REST end-point was target are handled.
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

    @Override
    public boolean supportsTileJSON() {
        return getGridSubsetForSRS(SRS.getEPSG3857()) != null || getGridSubsetForSRS(SRS.getEPSG900913()) != null;
    }

    @Override
    public TileJSON getTileJSON() {
        TileJSON tileJSON = new TileJSON();
        tileJSON.setName(getName());
        LayerMetaInformation metaInformation = getMetaInformation();
        if (metaInformation != null) {
            tileJSON.setDescription(metaInformation.getDescription());
        }
        BoundingBox wgs84Bounds = getBounds(SRS.getEPSG4326());
        PublishedInfo publishedInfo = getPublishedInfo();
        PublishedType type = publishedInfo.getType();
        List<VectorLayerMetadata> metadataLayers = new ArrayList<>();
        if (type == PublishedType.VECTOR) {
            setVectorLayers(publishedInfo, metadataLayers);
        } else if (type == PublishedType.GROUP) {
            setVectorLayersGroup(publishedInfo, metadataLayers);
        }
        if (!metadataLayers.isEmpty()) {
            tileJSON.setLayers(metadataLayers);
        }

        tileJSON.setBounds(
                new double[] {wgs84Bounds.getMinX(), wgs84Bounds.getMinY(), wgs84Bounds.getMaxX(), wgs84Bounds.getMaxY()
                });

        return tileJSON;
    }

    private void setVectorLayers(PublishedInfo publishedInfo, List<VectorLayerMetadata> metadataLayers) {
        ResourceInfo resource = getResource(publishedInfo);
        if (resource instanceof FeatureTypeInfo) {
            addVectorLayerMetadata((FeatureTypeInfo) resource, metadataLayers);
        }
    }

    private void setVectorLayersGroup(PublishedInfo publishedInfo, List<VectorLayerMetadata> metadataLayers) {
        LayerGroupInfo layerGroupInfo = null;
        if (Proxy.isProxyClass(publishedInfo.getClass())) {
            layerGroupInfo = (LayerGroupInfo) ModificationProxy.unwrap(publishedInfo);
        } else if (publishedInfo instanceof LayerGroupInfo) {
            layerGroupInfo = (LayerGroupInfo) publishedInfo;
        }
        if (layerGroupInfo != null) {
            List<PublishedInfo> layers = layerGroupInfo.getLayers();
            List<FeatureTypeInfo> featureTypes = new ArrayList<>();
            ResourceInfo resource;
            for (PublishedInfo layer : layers) {
                resource = getResource(layer);
                if (!(resource instanceof FeatureTypeInfo)) {
                    // leave the method as soon as we find a not-vector layer
                    return;
                }
                featureTypes.add((FeatureTypeInfo) resource);
            }

            for (FeatureTypeInfo featureTypeInfo : featureTypes) {
                addVectorLayerMetadata(featureTypeInfo, metadataLayers);
            }
        }
    }

    private ResourceInfo getResource(PublishedInfo publishedInfo) {
        ResourceInfo resource = null;
        if (Proxy.isProxyClass(publishedInfo.getClass())) {
            LayerInfo inner = (LayerInfo) ModificationProxy.unwrap(publishedInfo);
            resource = inner.getResource();
        } else if (publishedInfo instanceof LayerInfo) {
            resource = ((LayerInfo) publishedInfo).getResource();
        }
        return resource;
    }

    private void addVectorLayerMetadata(FeatureTypeInfo featureTypeInfo, List<VectorLayerMetadata> metadataLayers) {
        VectorLayerMetadata metadata = null;
        final ResourcePool resourcePool = catalog.getResourcePool();
        final FeatureType featureType;
        try {
            featureType = resourcePool.getFeatureType(featureTypeInfo);
            Collection<PropertyDescriptor> descriptors = featureType.getDescriptors();
            Map<String, String> fields = new HashMap<>();
            for (PropertyDescriptor pd : descriptors) {
                if (!(pd instanceof GeometryDescriptor)) {
                    String pdName = pd.getName().toString();
                    String typeName = pd.getType().getBinding().getSimpleName();
                    fields.put(pdName, typeName);
                }
            }
            metadata = new VectorLayerMetadata();
            metadata.setId(featureTypeInfo.getName());
            metadata.setFields(fields);
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Could not parse featureType " + featureTypeInfo, e);
        }
        if (metadata != null) {
            metadataLayers.add(metadata);
        }
    }
}
