/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.collect.Iterators.forEnumeration;
import static com.google.common.collect.Lists.newArrayList;
import static org.geowebcache.grid.GridUtil.findBestMatchingGrid;
import static org.geowebcache.seed.GWCTask.TYPE.TRUNCATE;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.CatalogLayerEventListener;
import org.geoserver.gwc.layer.CatalogStyleChangeListener;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredLayerInfo;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.ExtractBoundsFilterVisitor;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheEnvironment;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.ConfigurationPersistenceException;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;
import org.geowebcache.diskquota.storage.LayerQuota;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.diskquota.storage.TileSet;
import org.geowebcache.diskquota.storage.TileSetVisitor;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.grid.SRS;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.locks.LockProvider;
import org.geowebcache.locks.LockProvider.Lock;
import org.geowebcache.locks.MemoryLockProvider;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.GWCTask.TYPE;
import org.geowebcache.seed.SeedRequest;
import org.geowebcache.seed.TileBreeder;
import org.geowebcache.seed.TruncateAllRequest;
import org.geowebcache.seed.TruncateBboxRequest;
import org.geowebcache.service.Service;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.BlobStoreAggregator;
import org.geowebcache.storage.CompositeBlobStore;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.TileRange;
import org.locationtech.jts.densify.Densifier;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.MultiValuedFilter.MatchAction;
import org.opengis.filter.Or;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Spring bean acting as a mediator between GWC and GeoServer for the GWC integration classes so
 * that they don't need to worry about complexities nor API changes in either.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GWC implements DisposableBean, InitializingBean, ApplicationContextAware {

    private static final String GLOBAL_LOCK_KEY = "global";
    public static final String WORKSPACE_PARAM = "WORKSPACE";

    /** @see #get() */
    private static GWC INSTANCE;

    static final Logger log = Logging.getLogger(GWC.class);

    /** @see #getResponseEncoder(MimeType, RenderedImageMap) */
    private Map<String, Response> cachedTileEncoders = new HashMap<String, Response>();

    private final TileLayerDispatcher tld;

    private final StorageBroker storageBroker;

    private final TileBreeder tileBreeder;

    private final GWCConfigPersister gwcConfigPersister;

    private final Dispatcher owsDispatcher;

    private final GridSetBroker gridSetBroker;

    private DiskQuotaMonitor monitor;

    private CatalogLayerEventListener catalogLayerEventListener;

    private CatalogStyleChangeListener catalogStyleChangeListener;

    /** The catalog, secured and filtered */
    private final Catalog catalog;

    /** The raw catalog, non secured. Use with extreme caution! */
    private Catalog rawCatalog;

    private ConfigurableLockProvider lockProvider;

    private JDBCConfigurationStorage jdbcConfigurationStorage;

    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    private GeoWebCacheEnvironment gwcEnvironment;

    final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

    // list of GeoServer contributed grid sets that should not be editable by the user
    private final Set<String> geoserverEmbeddedGridSets = new HashSet<>();

    private BlobStoreAggregator blobStoreAggregator;

    /**
     * Constructor for the GWC mediator
     *
     * @param sb The GeoWebCache StorageBroker
     * @param tld The GeoWebCache TileLayer Aggregator
     * @param gridSetBroker The GeoWebCache GridSet Aggregator
     * @param tileBreeder The GeoWebCache TileBreeder (Used for seeding)
     * @param monitor The GeoWebCache DiskQuota Monitor
     * @param owsDispatcher The GeoServer OWS Service Dispatcher
     * @param catalog The GeoServer catalog, secured and filtered
     * @param rawCatalog The raw GeoServer catalog, not secured. Use with extreme caution!
     * @param storageFinder GeoWebcache system variable and configuration source
     * @param jdbcConfigurationStorage GeoServer integrator for GeoWebCache DiskQuota {@link
     *     JDBCConfiguration}
     * @param blobStoreAggregator GeoWebCache BlobStore Aggregator
     */
    public GWC(
            final GWCConfigPersister gwcConfigPersister,
            final StorageBroker sb,
            final TileLayerDispatcher tld,
            final GridSetBroker gridSetBroker,
            final TileBreeder tileBreeder,
            final DiskQuotaMonitor monitor,
            final Dispatcher owsDispatcher,
            final Catalog catalog,
            final Catalog rawCatalog,
            final DefaultStorageFinder storageFinder,
            final JDBCConfigurationStorage jdbcConfigurationStorage,
            final BlobStoreAggregator blobStoreAggregator) {

        this.gwcConfigPersister = gwcConfigPersister;
        this.tld = tld;
        this.storageBroker = sb;
        this.gridSetBroker = gridSetBroker;
        this.tileBreeder = tileBreeder;
        this.monitor = monitor;
        this.owsDispatcher = owsDispatcher;
        this.catalog = catalog;
        this.rawCatalog = rawCatalog;

        catalogLayerEventListener = new CatalogLayerEventListener(this, catalog);
        catalogStyleChangeListener = new CatalogStyleChangeListener(this, catalog);
        this.catalog.addListener(catalogLayerEventListener);
        this.catalog.addListener(catalogStyleChangeListener);

        this.lockProvider = new ConfigurableLockProvider();
        updateLockProvider(getConfig().getLockProviderName());

        this.jdbcConfigurationStorage = jdbcConfigurationStorage;
        this.blobStoreAggregator = blobStoreAggregator;
    }

    /** Updates the configurable lock provider to use the specified bean */
    private void updateLockProvider(String lockProviderName) {
        LockProvider delegate = null;
        if (lockProviderName == null) {
            delegate = new MemoryLockProvider();
        } else {
            Object provider = GeoWebCacheExtensions.bean(lockProviderName);
            if (provider == null) {
                throw new RuntimeException(
                        "Could not find lock provider "
                                + lockProvider
                                + " in the spring application context");
            } else if (!(provider instanceof LockProvider)) {
                throw new RuntimeException(
                        "Found bean "
                                + lockProvider
                                + " in the spring application context, but it was not a LockProvider");
            } else {
                delegate = (LockProvider) provider;
            }
        }

        lockProvider.setDelegate(delegate);
    }

    /**
     * Retrieves the GWC mediator bean, if registered in the spring context or set via {@link
     * #set(GWC)}.
     *
     * @return The {@link GWC} mediator bean
     * @throws IllegalStateException if no {@link GWC} instance was found.
     */
    public static synchronized GWC get() {
        if (GWC.INSTANCE == null) {
            GWC.INSTANCE = GeoServerExtensions.bean(GWC.class);
            if (GWC.INSTANCE == null) {
                throw new IllegalStateException(
                        "No bean of type " + GWC.class.getName() + " found by GeoServerExtensions");
            }
        }
        GWC.INSTANCE.syncEnv();
        return GWC.INSTANCE;
    }

    /**
     * Only to aid in unit testing for the places where a mock GWC mediator is needed and {@link
     * GWC#get()} is used; set it to {@code null} at each {@code tearDown} methed for each test that
     * sets it through {@link GWC#set(GWC)} at its {@code setUp} method
     */
    public static void set(GWC instance) {
        GWC.INSTANCE = instance;
    }

    /** @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet() */
    public void afterPropertiesSet() throws Exception {
        GWC.set(this);
    }

    /** @see org.springframework.beans.factory.DisposableBean#destroy() */
    public void destroy() throws Exception {
        Catalog catalog = getCatalog();
        if (this.catalogLayerEventListener != null) {
            catalog.removeListener(this.catalogLayerEventListener);
        }
        if (this.catalogStyleChangeListener != null) {
            catalog.removeListener(this.catalogStyleChangeListener);
        }
        GWC.set(null);
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public GWCConfig getConfig() {
        return gwcConfigPersister.getConfig();
    }

    /** Fully truncates the given layer, including any ParameterFilter */
    public void truncate(final String layerName) {
        checkNotNull(layerName, "layerName is null");
        // easy, no need to issue truncate tasks
        TileLayer layer;
        try {
            layer = tld.getTileLayer(layerName);
        } catch (GeoWebCacheException e) {
            log.log(Level.INFO, e.getMessage(), e);
            return;
        }
        final Set<String> gridSubsets = layer.getGridSubsets();
        for (String gridSetId : gridSubsets) {
            deleteCacheByGridSetId(layerName, gridSetId);
        }
    }

    /** Truncates the cache for the given layer/style combination */
    public void truncateByLayerAndStyle(final String layerName, final String styleName) {

        // check if the given style is actually cached
        if (log.isLoggable(Level.FINE)) {
            log.fine(
                    "Truncate for layer/style called. Checking if style '"
                            + styleName
                            + "' is cached for layer '"
                            + layerName
                            + "'");
        }
        if (!isStyleCached(layerName, styleName)) {
            log.fine(
                    "Style '"
                            + styleName
                            + "' is not cached for layer "
                            + layerName
                            + "'. No need to truncate.");
            return;
        }
        log.fine("truncating '" + layerName + "' for style '" + styleName + "'");
        String gridSetId = null; // all of them
        BoundingBox bounds = null; // all of them
        String format = null; // all of them
        truncate(layerName, styleName, gridSetId, bounds, format);
    }

    /** Truncates the cache for the default style of the given layer */
    public void truncateByLayerDefaultStyle(final String layerName) {
        checkNotNull(layerName, "layerName can't be null");
        log.fine("truncating '" + layerName + "' for default style");

        final TileLayer layer = getTileLayerByName(layerName);
        final Set<String> gridSetIds = layer.getGridSubsets(); // all of them
        final List<MimeType> mimeTypes = layer.getMimeTypes(); // all of them
        final BoundingBox bounds = null; // all of them
        final Map<String, String> parameters = null; // only default style

        for (String gridSetId : gridSetIds) {
            GridSubset gridSubset = layer.getGridSubset(gridSetId);
            if (gridSubset == null) {
                // layer may no longer have this gridsubset, but we want to truncate any remaining
                // tiles
                GridSet gridSet = gridSetBroker.get(gridSetId);
                gridSubset = GridSubsetFactory.createGridSubSet(gridSet);
            }

            for (MimeType mime : mimeTypes) {
                String formatName = mime.getFormat();
                truncate(layer, bounds, gridSubset, formatName, parameters);
            }
        }
    }

    public void truncate(final String layerName, final ReferencedEnvelope bounds)
            throws GeoWebCacheException {

        final TileLayer tileLayer = tld.getTileLayer(layerName);
        final Collection<String> gridSubSets = tileLayer.getGridSubsets();

        /*
         * Create a truncate task for each gridSubset (CRS), format and style
         */
        for (String gridSetId : gridSubSets) {
            GridSubset layerGrid = tileLayer.getGridSubset(gridSetId);
            BoundingBox intersectingBounds = getIntersectingBounds(layerName, layerGrid, bounds);
            if (intersectingBounds == null) {
                continue;
            }
            try {
                // This iterates over all cached parameters and all formats
                new TruncateBboxRequest(layerName, intersectingBounds, gridSetId)
                        .doTruncate(storageBroker, tileBreeder);
            } catch (StorageException | GeoWebCacheException e) {
                log.log(
                        Level.WARNING,
                        e,
                        () ->
                                String.format(
                                        "Error while truncating modified bounds for layer %s gridset %s",
                                        layerName, gridSetId));
            }
        }
    }

    public TruncateAllRequest truncateAll() throws GeoWebCacheException, StorageException {
        // creating a mock internal request
        TruncateAllRequest truncateAll = new TruncateAllRequest();
        // truncating everything
        truncateAll.doTruncate(storageBroker, tileBreeder);
        log.info("Mass Truncate Completed");
        log.info("Truncated Layers : " + truncateAll.getTrucatedLayersList());
        return truncateAll;
    }

    private BoundingBox getIntersectingBounds(
            String layerName, GridSubset layerGrid, ReferencedEnvelope bounds) {
        final GridSet gridSet = layerGrid.getGridSet();
        final String gridSetId = gridSet.getName();
        final SRS srs = gridSet.getSrs();
        final CoordinateReferenceSystem gridSetCrs;
        try {
            gridSetCrs = CRS.decode("EPSG:" + srs.getNumber(), true);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Can't decode SRS for layer '" + layerName + "': ESPG:" + srs.getNumber());
        }

        ReferencedEnvelope truncateBoundsInGridsetCrs;

        try {
            truncateBoundsInGridsetCrs = bounds.transform(gridSetCrs, true);
        } catch (Exception e) {
            log.warning(
                    "Can't truncate layer "
                            + layerName
                            + ": error transforming requested bounds to layer gridset "
                            + gridSetId
                            + ": "
                            + e.getMessage());
            return null;
        }

        final double minx = truncateBoundsInGridsetCrs.getMinX();
        final double miny = truncateBoundsInGridsetCrs.getMinY();
        final double maxx = truncateBoundsInGridsetCrs.getMaxX();
        final double maxy = truncateBoundsInGridsetCrs.getMaxY();
        final BoundingBox reqBounds = new BoundingBox(minx, miny, maxx, maxy);
        /*
         * layerGrid.getCoverageIntersections is not too robust, so we better check the requested
         * bounds intersect the layer bounds
         */
        // final BoundingBox layerBounds = layerGrid.getCoverageBestFitBounds();
        final BoundingBox layerBounds = layerGrid.getOriginalExtent();
        if (!layerBounds.intersects(reqBounds)) {
            log.fine(
                    "Requested truncation bounds do not intersect cached layer bounds, ignoring truncate request");
            return null;
        }
        final BoundingBox intersectingBounds = BoundingBox.intersection(layerBounds, reqBounds);
        return intersectingBounds;
    }

    /**
     * @param layerName name of the layer to truncate, non {@code null}
     * @param styleName style to truncate, or {@code null} for all
     * @param gridSetName grid set to truncate, {@code null} for all
     * @param bounds bounds to truncate based on, or {@code null} for whole layer
     * @param format {@link MimeType#getFormat() format} to truncate, or {@code null} for all
     */
    public void truncate(
            final String layerName,
            final String styleName,
            final String gridSetName,
            final BoundingBox bounds,
            final String format) {

        checkNotNull(layerName, "layerName can't be null");

        final TileLayer layer = getTileLayerByName(layerName);
        final Set<String> styleNames;
        final Set<String> gridSetIds;
        final List<MimeType> mimeTypes;
        if (styleName == null) {
            styleNames = getCachedStyles(layerName);
            if (styleNames.size() == 0) {
                styleNames.add("");
            }
        } else {
            styleNames = Collections.singleton(styleName);
        }
        if (gridSetName == null) {
            gridSetIds = layer.getGridSubsets();
        } else {
            gridSetIds = Collections.singleton(gridSetName);
        }
        if (format == null) {
            mimeTypes = layer.getMimeTypes();
        } else {
            try {
                mimeTypes = Collections.singletonList(MimeType.createFromFormat(format));
            } catch (MimeException e) {
                throw new RuntimeException();
            }
        }

        final String defaultStyle = layer.getStyles();

        for (String gridSetId : gridSetIds) {
            GridSubset gridSubset = layer.getGridSubset(gridSetId);
            if (gridSubset == null) {
                // layer may no longer have this gridsubset, but we want to truncate any remaining
                // tiles
                GridSet gridSet = gridSetBroker.get(gridSetId);
                gridSubset = GridSubsetFactory.createGridSubSet(gridSet);
            }
            for (String style : styleNames) {
                Map<String, String> parameters;
                if (style.length() == 0 || style.equals(defaultStyle)) {
                    log.finer(
                            "'"
                                    + style
                                    + "' is the layer's default style, "
                                    + "not adding a parameter filter");
                    parameters = null;
                } else {
                    parameters = Collections.singletonMap("STYLES", style);
                }
                for (MimeType mime : mimeTypes) {
                    String formatName = mime.getFormat();
                    truncate(layer, bounds, gridSubset, formatName, parameters);
                }
            }
        }
    }

    private void truncate(
            final TileLayer layer,
            final BoundingBox bounds,
            final GridSubset gridSubset,
            String formatName,
            Map<String, String> parameters) {
        final int threadCount = 1;
        int zoomStart;
        int zoomStop;
        zoomStart = gridSubset.getZoomStart();
        zoomStop = gridSubset.getZoomStop();
        final TYPE taskType = TRUNCATE;
        SeedRequest req =
                new SeedRequest(
                        layer.getName(),
                        bounds,
                        gridSubset.getName(),
                        threadCount,
                        zoomStart,
                        zoomStop,
                        formatName,
                        taskType,
                        parameters);

        GWCTask[] tasks;
        try {
            TileRange tr = TileBreeder.createTileRange(req, layer);
            boolean filterUpdate = false;
            tasks = tileBreeder.createTasks(tr, taskType, threadCount, filterUpdate);
        } catch (GeoWebCacheException e) {
            throw new RuntimeException(e);
        }

        tileBreeder.dispatchTasks(tasks);
    }

    private boolean isStyleCached(final String layerName, final String styleName) {
        Set<String> cachedStyles = getCachedStyles(layerName);
        boolean styleIsCached = cachedStyles.contains(styleName);
        return styleIsCached;
    }

    /** Returns the names of the styles for the layer, including the default style */
    private Set<String> getCachedStyles(final String layerName) {
        final TileLayer l = getTileLayerByName(layerName);
        Set<String> cachedStyles = new HashSet<String>();
        String defaultStyle = l.getStyles();
        if (defaultStyle != null) {
            cachedStyles.add(defaultStyle);
        }
        List<ParameterFilter> parameterFilters = l.getParameterFilters();
        if (parameterFilters != null) {
            for (ParameterFilter pf : parameterFilters) {
                if (!"STYLES".equalsIgnoreCase(pf.getKey())) {
                    continue;
                }
                cachedStyles.add(pf.getDefaultValue());
                cachedStyles.addAll(pf.getLegalValues());
                break;
            }
        }
        return cachedStyles;
    }

    /**
     * Completely eliminates a {@link GeoServerTileLayer} from GWC.
     *
     * <p>This method is intended to be called whenever a {@link LayerInfo} or {@link
     * LayerGroupInfo} is removed from GeoServer, or it is configured not to create a cached layer
     * for it, in order to delete the cache for the layer.
     *
     * @param prefixedName the name of the layer to remove.
     * @return {@code true} if the removal of the entire cache for the layer has succeeded, {@code
     *     false} if there wasn't a cache for that layer.
     */
    public synchronized boolean layerRemoved(final String prefixedName) {
        try {
            return storageBroker.delete(prefixedName);
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    public static void tryReload() {
        GWC instance = GWC.INSTANCE;
        if (instance != null) {
            instance.reload();
        }
    }

    /** Reloads the configuration and notifies GWC of any externally removed layer. */
    @SuppressWarnings("deprecation")
    void reload() {
        final Set<String> currLayerNames = new HashSet<String>(getTileLayerNames());
        try {
            tld.reInit(); // some mock testing uses this blasted method, don't know how to work
            // around it
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Unable to reinit TileLayerDispatcher", e);
            throw e;
        }
        Set<String> newLayerNames = getTileLayerNames();
        SetView<String> removedExternally = Sets.difference(currLayerNames, newLayerNames);
        for (String removedLayerName : removedExternally) {
            log.info("Notifying of TileLayer '" + removedLayerName + "' removed externally");
            layerRemoved(removedLayerName);
        }

        // reload the quota config
        try {
            DiskQuotaMonitor monitor = getDiskQuotaMonitor();
            monitor.reloadConfig();
            ConfigurableQuotaStoreProvider provider =
                    (ConfigurableQuotaStoreProvider) monitor.getQuotaStoreProvider();
            provider.reloadQuotaStore();

            // restart the monitor, the quota store might have been changed and pointed to another
            // DB
            // and we need to re-init the tile pages
            monitor.shutDown(1);
            monitor.startUp();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to reload the disk quoa configuration", e);
        }
    }

    /**
     * Tries to dispatch a tile request represented by a GeoServer WMS {@link GetMapRequest} through
     * GeoWebCache, and returns the {@link ConveyorTile} if succeeded or {@code null} if it wasn't
     * possible.
     *
     * <p>Preconditions:
     *
     * <ul>
     *   <li><code>{@link GetMapRequest#isTiled() request.isTiled()} == true</code>
     * </ul>
     *
     * @param requestMistmatchTarget target string builder where to write the reason of the request
     *     mismatch with the tile cache
     * @return the GWC generated tile result if the request matches a tile cache, or {@code null}
     *     otherwise.
     */
    public final ConveyorTile dispatch(
            final GetMapRequest request, StringBuilder requestMistmatchTarget) {

        final String layerName = request.getRawKvp().get("LAYERS");
        /*
         * This is a quick way of checking if the request was for a single layer. We can't really
         * use request.getLayers() because in the event that a layerGroup was requested, the request
         * parser turned it into a list of actual Layers
         */
        if (layerName.indexOf(',') != -1) {
            requestMistmatchTarget.append("more than one layer requested");
            return null;
        }

        // GEOS-9431 acquire prefixed name if not prefixed already
        final String getPrefixedName =
                (!layerName.contains(":")) ? getPrefixedName(layerName) : layerName;

        if (!tld.layerExists(getPrefixedName)) {
            requestMistmatchTarget.append("not a tile layer");
            return null;
        }

        final TileLayer tileLayer;
        try {
            tileLayer = this.tld.getTileLayer(getPrefixedName);
        } catch (GeoWebCacheException e) {
            throw new RuntimeException(e);
        }
        if (!tileLayer.isEnabled()) {
            requestMistmatchTarget.append("tile layer disabled");
            return null;
        }

        if (getConfig().isSecurityEnabled()) {
            String bboxstr = request.getRawKvp().get("BBOX");
            String srs = request.getRawKvp().get("SRS");
            ReferencedEnvelope bbox = null;
            try {
                bbox = (ReferencedEnvelope) new BBoxKvpParser().parse(bboxstr);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Invalid bbox for layer '" + layerName + "': " + bboxstr);
            }
            if (srs != null) {
                try {
                    bbox = new ReferencedEnvelope(bbox, CRS.decode(srs));
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Can't decode SRS for layer '" + layerName + "': " + srs);
                }
            }
            try {
                verifyAccessLayer(layerName, bbox);
            } catch (ServiceException | SecurityException e) {
                return null;
            }
        }

        ConveyorTile tileReq = prepareRequest(tileLayer, request, requestMistmatchTarget);
        if (null == tileReq) {
            return null;
        }
        ConveyorTile tileResp = null;
        try {
            tileResp = tileLayer.getTile(tileReq);
        } catch (Exception e) {
            log.log(Level.INFO, "Error dispatching tile request to GeoServer", e);
        }
        return tileResp;
    }

    private String getPrefixedName(String layerName) {
        PublishedInfo info = catalog.getLayerByName(layerName);
        if (info == null) info = catalog.getLayerGroupByName(layerName);
        if (info != null) return info.prefixedName();
        if (log.isLoggable(Level.INFO)) log.info("Unable to find a prefix for : " + layerName);
        return layerName;
    }

    ConveyorTile prepareRequest(
            TileLayer tileLayer, GetMapRequest request, StringBuilder requestMistmatchTarget) {

        if (!isCachingPossible(tileLayer, request, requestMistmatchTarget)) {
            return null;
        }

        final MimeType mimeType;
        try {
            mimeType = MimeType.createFromFormat(request.getFormat());
            List<MimeType> tileLayerFormats = tileLayer.getMimeTypes();
            if (!tileLayerFormats.contains(mimeType)) {
                requestMistmatchTarget.append("no tile cache for requested format");
                return null;
            }
        } catch (MimeException me) {
            // not a GWC supported format
            requestMistmatchTarget.append("not a GWC supported format: ").append(me.getMessage());
            return null;
        }

        final GridSubset gridSubset;
        final long[] tileIndex;
        final Map<String, String> fullParameters;
        try {
            boolean axisFlip = false;
            final List<GridSubset> crsMatchingGridSubsets;
            {
                CoordinateReferenceSystem crs = request.getCrs();
                int epsgId;
                // are we in wms 1.3 land?
                if (CRS.getAxisOrder(crs) == AxisOrder.NORTH_EAST) {
                    axisFlip = true;
                }
                String srs = request.getSRS();
                epsgId = Integer.parseInt(srs.substring(srs.lastIndexOf(':') + 1));
                SRS srs2 = SRS.getSRS(epsgId);
                crsMatchingGridSubsets = tileLayer.getGridSubsetsForSRS(srs2);
            }
            final BoundingBox tileBounds;
            {
                Envelope bbox = request.getBbox();
                if (axisFlip) {
                    tileBounds =
                            new BoundingBox(
                                    bbox.getMinY(), bbox.getMinX(), bbox.getMaxY(), bbox.getMaxX());
                } else {
                    tileBounds =
                            new BoundingBox(
                                    bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY());
                }
            }

            if (crsMatchingGridSubsets.isEmpty()) {
                requestMistmatchTarget.append("no cache exists for requested CRS");
                return null;
            }

            {
                long[] matchingTileIndex = new long[3];
                final int reqW = request.getWidth();
                final int reqH = request.getHeight();
                gridSubset =
                        findBestMatchingGrid(
                                tileBounds, crsMatchingGridSubsets, reqW, reqH, matchingTileIndex);
                if (gridSubset == null) {
                    requestMistmatchTarget.append("request does not align to grid(s) ");
                    for (GridSubset gs : crsMatchingGridSubsets) {
                        requestMistmatchTarget.append('\'').append(gs.getName()).append("' ");
                    }
                    return null;
                }
                tileIndex = matchingTileIndex;
            }

            {
                Map<String, String> requestParameterMap = request.getRawKvp();
                fullParameters = tileLayer.getModifiableParameters(requestParameterMap, "UTF-8");
            }

        } catch (Exception e) {
            if (log.isLoggable(Level.FINE)) {
                e.printStackTrace();
                log.log(Level.FINE, "Exception caught checking gwc dispatch preconditions", e);
            }
            Throwable rootCause = getRootCause(e);
            requestMistmatchTarget
                    .append("exception occurred: ")
                    .append(rootCause.getClass().getSimpleName())
                    .append(": ")
                    .append(e.getMessage());
            return null;
        }

        ConveyorTile tileReq;
        final String gridSetId = gridSubset.getName();
        HttpServletRequest servletReq = null;
        HttpServletResponse servletResp = null;
        String layerName = tileLayer.getName();
        tileReq =
                new ConveyorTile(
                        storageBroker,
                        layerName,
                        gridSetId,
                        tileIndex,
                        mimeType,
                        fullParameters,
                        servletReq,
                        servletResp);
        return tileReq;
    }

    /**
     * Determines whether the given {@link GetMapRequest} is a candidate to match a GWC tile or not.
     *
     * @param layer the layer name to check against
     * @param request the GetMap request to check whether it might match a tile
     */
    boolean isCachingPossible(
            TileLayer layer, GetMapRequest request, StringBuilder requestMistmatchTarget) {

        if (null != request.getRemoteOwsType() || null != request.getRemoteOwsURL()) {
            requestMistmatchTarget.append("request uses remote OWS");
            return false;
        }

        Map<String, ParameterFilter> filters;
        {
            List<ParameterFilter> parameterFilters = layer.getParameterFilters();
            if (null != parameterFilters && parameterFilters.size() > 0) {
                filters = new HashMap<String, ParameterFilter>();
                for (ParameterFilter pf : parameterFilters) {
                    filters.put(pf.getKey().toUpperCase(), pf);
                }
            } else {
                filters = Collections.emptyMap();
            }
        }

        // if (request.isTransparent()) {
        // if (!filterApplies(filters, request, "TRANSPARENT")) {
        // return false;
        // }
        // }

        if (request.getEnv() != null && !request.getEnv().isEmpty()) {
            if (!filterApplies(filters, request, "ENV", requestMistmatchTarget)) {
                return false;
            }
        }

        if (request.getFormatOptions() != null && !request.getFormatOptions().isEmpty()) {
            if (!filterApplies(filters, request, "FORMAT_OPTIONS", requestMistmatchTarget)) {
                return false;
            }
        }
        if (0.0 != request.getAngle()) {
            if (!filterApplies(filters, request, "ANGLE", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getRawKvp().get("BGCOLOR")) {
            if (!filterApplies(filters, request, "BGCOLOR", requestMistmatchTarget)) {
                return false;
            }
        }
        if (0 != request.getBuffer()) {
            if (!filterApplies(filters, request, "BUFFER", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getCQLFilter() && !request.getCQLFilter().isEmpty()) {
            if (!filterApplies(filters, request, "CQL_FILTER", requestMistmatchTarget)) {
                return false;
            }
        }
        if (request.getElevation() != null && !request.getElevation().isEmpty()) {
            if (null != request.getElevation().get(0)
                    && !filterApplies(filters, request, "ELEVATION", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getFeatureId() && !request.getFeatureId().isEmpty()) {
            if (!filterApplies(filters, request, "FEATUREID", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getFilter() && !request.getFilter().isEmpty()) {
            boolean sameFilters = checkFilter(request.getFilter(), request.getCQLFilter(), filters);
            if (!sameFilters
                    && !filterApplies(filters, request, "FILTER", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getSortBy() && !request.getSortBy().isEmpty()) {
            if (!filterApplies(filters, request, "SORTBY", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getPalette()) {
            if (!filterApplies(filters, request, "PALETTE", requestMistmatchTarget)) {
                return false;
            }
        }

        // REVISIT: should these be taken into account?
        // if (null != request.getSld()) {
        // if (!filterApplies(filters, request, "SLD", requestMistmatchTarget)) {
        // return false;
        // }
        // }
        // if (null != request.getSldBody()) {
        // if (!filterApplies(filters, request, "SLD_BODY", requestMistmatchTarget)) {
        // return false;
        // }
        // }

        if (null != request.getStartIndex()) {
            if (!filterApplies(filters, request, "STARTINDEX", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getMaxFeatures()) {
            if (!filterApplies(filters, request, "MAXFEATURES", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getTime() && !request.getTime().isEmpty()) {
            if (null != request.getTime().get(0)
                    && !filterApplies(filters, request, "TIME", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getViewParams() && !request.getViewParams().isEmpty()) {
            if (!filterApplies(filters, request, "VIEWPARAMS", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getFeatureVersion()) {
            if (!filterApplies(filters, request, "FEATUREVERSION", requestMistmatchTarget)) {
                return false;
            }
        }

        return true;
    }

    /** Method for checking if CQL_FILTER list and FILTER lists are equals */
    private boolean checkFilter(List filter, List cqlFilter, Map<String, ParameterFilter> filters) {
        // Check if the two filters are equals and the FILTER parameter is not a ParameterFilter
        // Check is done only if the FILTER parameter is not present
        if (!filters.containsKey("FILTER")) {
            // Check if the filter List and cqlFilter lists are not null and not empty
            boolean hasFilter = filter != null && !filter.isEmpty();
            boolean hasCQLFilter = cqlFilter != null && !cqlFilter.isEmpty();
            // If the filters are present, check if they are equals.
            // In this case the Filter List has been taken from the CQL_FILTER list
            if (hasCQLFilter && hasFilter) {
                // First check on the size
                int size = filter.size();
                if (size == cqlFilter.size()) {
                    // Check same elements
                    boolean equals = true;
                    // Loop on the elements
                    for (int i = 0; i < size; i++) {
                        equals &= filter.get(i).equals(cqlFilter.get(i));
                    }
                    return equals;
                }
            }
        }
        // By default return false
        return false;
    }

    private boolean filterApplies(
            Map<String, ParameterFilter> filters,
            GetMapRequest request,
            String key,
            StringBuilder requestMistmatchTarget) {

        ParameterFilter parameterFilter = filters.get(key);
        if (parameterFilter == null) {
            requestMistmatchTarget.append("no parameter filter exists for ").append(key);
            return false;
        }
        String parameter = request.getRawKvp().get(key);
        boolean applies = parameterFilter.applies(parameter);
        if (!applies) {
            requestMistmatchTarget
                    .append(key)
                    .append(" does not apply to parameter filter of the same name");
        }
        return applies;
    }

    /**
     * @return the tile layer named {@code layerName}
     * @throws IllegalArgumentException if no {@link TileLayer} named {@code layeName} is found
     */
    public TileLayer getTileLayerByName(String layerName) throws IllegalArgumentException {
        TileLayer tileLayer;
        try {
            tileLayer = tld.getTileLayer(layerName);
        } catch (GeoWebCacheException e) {
            throw new IllegalArgumentException(e.getMessage(), Throwables.getRootCause(e));
        }
        return tileLayer;
    }

    public Set<String> getTileLayerNames() {
        return tld.getLayerNames();
    }

    /**
     * @return all the GWC tile layers, both GeoServer's and externally defined
     * @see #getGeoServerTileLayers()
     */
    public Iterable<TileLayer> getTileLayers() {
        return tld.getLayerList();
    }

    /**
     * @param nsPrefix the namespace prefix to filter upon, or {@code null} to return all layers
     * @return the tile layers that belong to a layer(group)info in the given prefix, or all the
     *     {@link TileLayer}s in the {@link TileLayerDispatcher} if {@code nsPrefix == null}
     */
    public Iterable<? extends TileLayer> getTileLayersByNamespacePrefix(final String nsPrefix) {
        if (nsPrefix == null) {
            return getTileLayers();
        }

        final Catalog catalog = getCatalog();

        final NamespaceInfo namespaceFilter = catalog.getNamespaceByPrefix(nsPrefix);
        if (namespaceFilter == null) {
            Iterable<TileLayer> tileLayers = getTileLayers();
            return tileLayers;
        }

        Iterable<GeoServerTileLayer> geoServerTileLayers = getGeoServerTileLayers();

        return Iterables.filter(
                geoServerTileLayers,
                new Predicate<GeoServerTileLayer>() {
                    @Override
                    public boolean apply(GeoServerTileLayer tileLayer) {
                        String layerName = tileLayer.getName();
                        if (-1 == layerName.indexOf(':')) {
                            return false;
                        }
                        LayerInfo layerInfo = catalog.getLayerByName(layerName);
                        if (layerInfo != null) {
                            NamespaceInfo layerNamespace = layerInfo.getResource().getNamespace();
                            if (namespaceFilter.equals(layerNamespace)) {
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }

    public Set<String> getLayerNamesForGridSets(final Set<String> gridSetIds) {

        Set<String> layerNames = new TreeSet<String>();

        for (TileLayer layer : getTileLayers()) {
            Set<String> layerGrids = layer.getGridSubsets();
            if (!Sets.intersection(gridSetIds, layerGrids).isEmpty()) {
                layerNames.add(layer.getName());
            }
        }
        return layerNames;
    }

    /**
     * Returns whether the disk quota module is available at all.
     *
     * <p>If not, none of the other diskquota related methods should be even called. The disk quota
     * module may have been completely disabled through the {@code GWC_DISKQUOTA_DISABLED=true}
     * environment variable
     *
     * @return whether the disk quota module is available at all.
     */
    public boolean isDiskQuotaAvailable() {
        DiskQuotaMonitor diskQuotaMonitor = getDiskQuotaMonitor();
        return diskQuotaMonitor.isEnabled();
    }

    /**
     * Returns whether the disk quota module is enabled at all.
     *
     * <p>If not, none of the other diskquota related methods should be even called. The disk quota
     * module may have been completely disabled through the {@code GWC_DISKQUOTA_DISABLED=true}
     * environment variable
     *
     * @return whether the disk quota module is available at all.
     */
    public boolean isDiskQuotaEnabled() {
        DiskQuotaMonitor diskQuotaMonitor = getDiskQuotaMonitor();
        return diskQuotaMonitor.isEnabled() && diskQuotaMonitor.getConfig().isEnabled();
    }

    /**
     * @return the current DiskQuota configuration or {@code null} if the disk quota module has been
     *     disabled (i.e. through the {@code GWC_DISKQUOTA_DISABLED=true} environment variable)
     */
    public DiskQuotaConfig getDiskQuotaConfig() {
        if (!isDiskQuotaAvailable()) {
            return null;
        }
        DiskQuotaMonitor monitor = getDiskQuotaMonitor();
        return monitor.getConfig();
    }

    private DiskQuotaMonitor getDiskQuotaMonitor() {
        return monitor;
    }

    public void saveConfig(GWCConfig gwcConfig) throws IOException {
        gwcConfigPersister.save(gwcConfig);

        // make sure we switch to the lock provider just configured
        updateLockProvider(gwcConfig.getLockProviderName());
    }

    public void saveDiskQuotaConfig(DiskQuotaConfig config, JDBCConfiguration jdbcConfig)
            throws ConfigurationException, IOException, InterruptedException {
        // save the configuration
        checkArgument(isDiskQuotaAvailable(), "DiskQuota is not enabled");
        DiskQuotaMonitor monitor = getDiskQuotaMonitor();
        monitor.saveConfig(config);
        jdbcConfigurationStorage.saveDiskQuotaConfig(config, jdbcConfig);

        // GeoServer own GWC is wired up to use the ConfigurableQuotaStoreProvider, force it to
        // reload
        ConfigurableQuotaStoreProvider provider =
                (ConfigurableQuotaStoreProvider) monitor.getQuotaStoreProvider();
        provider.reloadQuotaStore();

        // restart the monitor, the quota store might have been changed and pointed to another DB
        // and we need to re-init the tile pages
        monitor.shutDown(1);
        monitor.startUp();
    }

    public Quota getGlobalQuota() {
        if (!isDiskQuotaAvailable()) {
            return null;
        }
        return getDiskQuotaConfig().getGlobalQuota();
    }

    /** @return the globally used quota, {@code null} if diskquota is disabled */
    public Quota getGlobalUsedQuota() {
        if (!isDiskQuotaAvailable()) {
            return null;
        }
        try {
            return monitor.getGloballyUsedQuota();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return {@code null} if disk quota is not enabled, the aggregated quota used by all layer
     *     cached for the given gridset otherwise.
     */
    public Quota getUsedQuotaByGridSet(final String gridSetName) {
        checkNotNull(gridSetName, "GridSet name is null");
        if (!isDiskQuotaAvailable()) {
            return null;
        }

        final Quota quota = new Quota();

        TileSetVisitor visitor =
                new TileSetVisitor() {
                    @Override
                    public void visit(final TileSet tileSet, final QuotaStore store) {
                        if (!gridSetName.equals(tileSet.getGridsetId())) {
                            return;
                        }

                        final String tileSetId = tileSet.getId();
                        try {
                            Quota used = store.getUsedQuotaByTileSetId(tileSetId);
                            quota.add(used);
                        } catch (InterruptedException e) {
                            log.fine(e.getMessage());
                            return;
                        }
                    }
                };
        monitor.getQuotaStore().accept(visitor);
        return quota;
    }

    /**
     * @return the Quota limit for the given layer, or {@code null} if no specific limit has been
     *     set for that layer
     */
    public Quota getQuotaLimit(final String layerName) {
        if (!isDiskQuotaAvailable()) {
            return null;
        }

        DiskQuotaConfig disQuotaConfig = getDiskQuotaConfig();
        List<LayerQuota> layerQuotas = disQuotaConfig.getLayerQuotas();
        if (layerQuotas == null) {
            return null;
        }
        for (LayerQuota lq : layerQuotas) {
            if (layerName.equals(lq.getLayer())) {
                return new Quota(lq.getQuota());
            }
        }
        return null;
    }

    /**
     * @return the currently used disk quota for the layer or {@code null} if can't be determined
     */
    public Quota getUsedQuota(final String layerName) {
        if (!isDiskQuotaAvailable()) {
            return null;
        }
        try {
            Quota usedQuotaByLayerName = monitor.getUsedQuotaByLayerName(layerName);
            return usedQuotaByLayerName;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Dispatches a request to the GeoServer OWS {@link Dispatcher}
     *
     * @param params the KVP map of OWS parameters
     * @return an http response wrapper where to grab the raw dispatcher response from
     */
    public Resource dispatchOwsRequest(final Map<String, String> params, Cookie[] cookies)
            throws Exception {

        // If the WORKSPACE parameter is set, remove it and use it to set the workspace of the
        // request
        String workspace = params.remove(WORKSPACE_PARAM);

        FakeHttpServletRequest req = new FakeHttpServletRequest(params, cookies, workspace);
        FakeHttpServletResponse resp = new FakeHttpServletResponse();

        Request request = Dispatcher.REQUEST.get();
        Dispatcher.REQUEST.remove();
        try {
            owsDispatcher.handleRequest(req, resp);
        } finally {
            // reset the old request
            if (request != null) {
                Dispatcher.REQUEST.set(request);
            } else {
                Dispatcher.REQUEST.remove();
            }
        }
        return new ByteArrayResource(resp.getBytes());
    }

    public void proxyOwsRequest(ConveyorTile tile) throws Exception {
        HttpServletRequest actualRequest = tile.servletReq;

        // get the param map and force service to be WMS if missing
        Map<String, String> parameterMap = new HashMap<String, String>();
        Map<String, String[]> params = actualRequest.getParameterMap();
        boolean hasService = false;
        for (Map.Entry<String, String[]> param : params.entrySet()) {
            String key = param.getKey();
            String value = param.getValue()[0];
            parameterMap.put(key, value);
            if ("service".equalsIgnoreCase(key)
                    && (value == null || value.isEmpty() || !"WMS".equalsIgnoreCase(value))) {
                throw new GeoWebCacheException(
                        "Failed to cascade request, service should be WMS but it was: '"
                                + value
                                + "'");
            }
        }
        if (!hasService) {
            parameterMap.put("service", "WMS");
        }

        // cascade
        Cookie[] cookies = actualRequest.getCookies();
        FakeHttpServletRequest request = new FakeHttpServletRequest(parameterMap, cookies);
        owsDispatcher.handleRequest(request, tile.servletResp);
    }

    public GridSetBroker getGridSetBroker() {
        return gridSetBroker;
    }

    public LayerInfo getLayerInfoById(String layerId) {
        return getCatalog().getLayer(layerId);
    }

    public LayerInfo getLayerInfoByName(String layerName) {
        return getCatalog().getLayerByName(layerName);
    }

    public LayerGroupInfo getLayerGroupByName(String layerName) {
        return getCatalog().getLayerGroupByName(layerName);
    }

    public LayerGroupInfo getLayerGroupById(String id) {
        return getCatalog().getLayerGroup(id);
    }

    /** Adds a layer to the {@link CatalogConfiguration} and saves it. */
    public void add(GeoServerTileLayer tileLayer) {
        tld.addLayer(tileLayer);
    }

    /**
     * Notification that a layer has been added; to be called by {@link CatalogConfiguration}
     * whenever {@link CatalogConfiguration#save() save} is called and a layer is added..
     *
     * <p>NOTE: this should be hanlded by GWC itself somehow, like with a configuration listener of
     * some sort.
     */
    public void layerAdded(String layerName) {
        if (isDiskQuotaAvailable()) {
            try {
                monitor.getQuotaStore().createLayer(layerName);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Notification that a layer has been added; to be called by {@link CatalogConfiguration}
     * whenever {@link CatalogConfiguration#save() save} is called and a layer has been renamed.
     *
     * <p>NOTE: this should be hanlded by GWC itself somehow, like with a configuration listener of
     * some sort.
     */
    public void layerRenamed(String oldLayerName, String newLayerName) {
        try {
            log.info("Renaming GWC TileLayer '" + oldLayerName + "' as '" + newLayerName + "'");
            // /embeddedConfig.rename(oldLayerName, newLayerName);
            storageBroker.rename(oldLayerName, newLayerName);
        } catch (StorageException e) {
            log.log(Level.WARNING, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public boolean isServiceEnabled(final Service service) {
        return getConfig().isEnabled(service.getPathName());
    }

    /** @return {@code true} if there's a TileLayer named {@code layerName} */
    public boolean tileLayerExists(final String layerName) {
        return tld.layerExists(layerName);
    }

    /**
     * @param namespaceURI the feature type namespace
     * @param typeName the feature type name
     * @return the set of TileLayer names (from LayerInfos and LayerGroupInfos) affected by the
     *     feature type, may be empty
     */
    public Set<String> getTileLayersByFeatureType(
            final String namespaceURI, final String typeName) {
        NamespaceInfo namespace;
        if (namespaceURI == null || XMLConstants.DEFAULT_NS_PREFIX.equals(namespaceURI)) {
            namespace = getCatalog().getDefaultNamespace();
        } else {
            namespace = getCatalog().getNamespaceByURI(namespaceURI);
        }

        final FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName(namespace, typeName);
        final List<LayerInfo> layers = getCatalog().getLayers(typeInfo);

        Set<String> affectedLayers = new HashSet<String>();

        for (LayerInfo layer : layers) {
            final String tileLayerName = tileLayerName(layer);
            if (tileLayerExists(tileLayerName)) {
                affectedLayers.add(tileLayerName);
            }
        }

        // build a query to find all groups directly containing any
        // of the layers associated to the feature type
        List<Filter> filters = new ArrayList<>();
        for (LayerInfo layer : layers) {
            filters.add(
                    ff.equal(
                            ff.property("layers.id"),
                            ff.literal(layer.getId()),
                            true,
                            MatchAction.ANY));
            filters.add(ff.equal(ff.property("rootLayer.id"), ff.literal(layer.getId())));
        }
        Or groupFilter = ff.or(filters);
        List<LayerGroupInfo> groups = new ArrayList<>();
        try (CloseableIterator<LayerGroupInfo> it =
                getCatalog().list(LayerGroupInfo.class, groupFilter)) {
            while (it.hasNext()) {
                LayerGroupInfo lg = it.next();
                groups.add(lg);
            }
        } catch (Exception e) {
            log.log(
                    Level.SEVERE,
                    "Failed to load groups associated to feature type " + typeName,
                    e);
        }
        // add the parents recursively
        loadGroupParents(groups);
        for (LayerGroupInfo lgi : groups) {
            final String tileLayerName = tileLayerName(lgi);
            if (!tileLayerExists(tileLayerName)) {
                continue;
            }
            affectedLayers.add(tileLayerName);
        }
        return affectedLayers;
    }

    public synchronized void addGridSet(final GridSet gridSet)
            throws IllegalArgumentException, IOException {
        checkNotNull(gridSet);
        tld.addGridSet(gridSet);
    }

    public synchronized void modifyGridSet(final String oldGridSetName, final GridSet newGridSet)
            throws IllegalArgumentException, IOException, GeoWebCacheException {

        checkNotNull(oldGridSetName);
        checkNotNull(newGridSet);

        final GridSet oldGridSet = gridSetBroker.get(oldGridSetName);
        if (null == oldGridSet) {
            throw new IllegalArgumentException("GridSet " + oldGridSetName + " does not exist");
        }

        final boolean needsTruncate = oldGridSet.shouldTruncateIfChanged(newGridSet);
        if (needsTruncate) {
            log.warning("### Changes in gridset force truncation of affected Tile layers");
            log.info("### Old gridset: " + oldGridSet);
            log.info("### New gridset: " + newGridSet);
        }

        Map<TileLayer, GridSubset> affectedLayers = new HashMap<TileLayer, GridSubset>();
        Lock lock = null;
        try {
            lock = lockProvider.getLock(GLOBAL_LOCK_KEY);

            for (TileLayer layer : getTileLayers()) {
                GridSubset gridSubet;
                if (null != (gridSubet = layer.getGridSubset(oldGridSetName))) {
                    affectedLayers.put(layer, gridSubet);
                    layer.removeGridSubset(oldGridSetName);
                    if (needsTruncate) {
                        deleteCacheByGridSetId(layer.getName(), oldGridSetName);
                    }
                }
            }

            getGridSetBroker().remove(oldGridSetName);
            getGridSetBroker().put(newGridSet);

            // tld.removeGridset(oldGridSetName);
            // tld.addGridSet(newGridSet);
            // if (isRename && !needsTruncate) {
            // // /TODO: quotaStore.renameGridSet(oldGridSetName, newGidSetName);
            // }

            final boolean sameSRS = oldGridSet.getSrs().equals(newGridSet.getSrs());

            final int maxZoomLevel = newGridSet.getNumLevels() - 1;

            Set<TileLayerConfiguration> saveConfigurations = new HashSet<>();

            // now restore the gridsubset for each layer
            for (Map.Entry<TileLayer, GridSubset> entry : affectedLayers.entrySet()) {
                TileLayer layer = entry.getKey();
                GridSubset gsubset = entry.getValue();

                BoundingBox gridSetExtent = gsubset.getOriginalExtent();
                if (null != gridSetExtent && sameSRS) {
                    gridSetExtent = newGridSet.getOriginalExtent().intersection(gridSetExtent);
                }

                int zoomStart = gsubset.getZoomStart();
                int zoomStop = gsubset.getZoomStop();

                if (zoomStart > maxZoomLevel) {
                    zoomStart = maxZoomLevel;
                }
                if (zoomStop > maxZoomLevel || zoomStop < zoomStart) {
                    zoomStop = maxZoomLevel;
                }

                GridSubset newGridSubset =
                        GridSubsetFactory.createGridSubSet(
                                newGridSet, gridSetExtent, zoomStart, zoomStop);

                layer.removeGridSubset(oldGridSetName);
                layer.addGridSubset(newGridSubset);

                TileLayerConfiguration config = tld.getConfiguration(layer);
                config.modifyLayer(layer);
                saveConfigurations.add(config);
            }

        } finally {
            if (lock != null) {
                lock.release();
            }
        }
    }

    private BlobStoreAggregator getBlobStoreAggregator() {
        return blobStoreAggregator;
    }

    /**
     * Retrieves a {@link Response} that can encode metatile requests
     *
     * @param responseFormat The format of the tile response
     * @param metaTileMap The metatile map
     * @return A Response object that can encode the request (typically a {@link
     *     RenderedImageMapResponse})
     */
    @SuppressWarnings("unchecked")
    public Response getResponseEncoder(MimeType responseFormat, RenderedImageMap metaTileMap) {
        final String format = responseFormat.getFormat();
        final String mimeType = responseFormat.getMimeType();

        Response response = cachedTileEncoders.get(format);
        if (response == null) {
            final Operation operation;
            {
                GetMapRequest getMap = new GetMapRequest();
                getMap.setFormat(mimeType);
                Object[] parameters = {getMap};
                org.geoserver.platform.Service service =
                        (org.geoserver.platform.Service)
                                GeoServerExtensions.bean("wms-1_1_1-ServiceDescriptor");
                if (service == null) {
                    throw new IllegalStateException(
                            "Didn't find service descriptor 'wms-1_1_1-ServiceDescriptor'");
                }
                operation = new Operation("GetMap", service, (Method) null, parameters);
            }

            final List<Response> extensions = GeoServerExtensions.extensions(Response.class);
            final Class<?> webMapClass = metaTileMap.getClass();
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
                throw new IllegalStateException(
                        "Didn't find a " + Response.class.getName() + " to handle " + mimeType);
            }
        }
        return response;
    }

    /**
     * Determines if the {@link PublishedInfo} associated with a {@link GeoServerTileLayer} is
     * queryable via WMS
     *
     * @param geoServerTileLayer The tile layer to query
     * @return <code>true</code> if the layer is queryable
     */
    public boolean isQueryable(final GeoServerTileLayer geoServerTileLayer) {
        WMS wmsMediator = WMS.get();
        PublishedInfo published = geoServerTileLayer.getPublishedInfo();
        if (published instanceof LayerInfo) {
            return wmsMediator.isQueryable((LayerInfo) published);
        }
        return wmsMediator.isQueryable((LayerGroupInfo) published);
    }

    /**
     * @return all tile layers backed by a geoserver layer/layergroup
     * @see #getTileLayers()
     */
    public Iterable<GeoServerTileLayer> getGeoServerTileLayers() {
        final Iterable<TileLayer> tileLayers = getTileLayers();

        Iterable<GeoServerTileLayer> filtered =
                Iterables.filter(tileLayers, GeoServerTileLayer.class);

        return filtered;
    }

    /**
     * Modifies a {@link TileLayer} via the {@link TileLayerDispatcher}, and logs the change. Only
     * affects the GeoWebCache configuration.
     *
     * @param layer The layer to save.
     */
    public void save(final TileLayer layer) {
        checkNotNull(layer);
        log.info("Saving GeoSeverTileLayer " + layer.getName());
        tld.modify(layer);
    }

    /**
     * Renames a {@link TileLayer} via the {@link TileLayerDispatcher}, and logs the change. Only
     * affects the GeoWebCache configuration.
     *
     * @param oldTileLayerName The old layer name.
     * @param newTileLayerName The new layer name.
     */
    public void rename(String oldTileLayerName, String newTileLayerName) {
        checkNotNull(oldTileLayerName);
        checkNotNull(newTileLayerName);
        log.info("Renaming GeoSeverTileLayer " + oldTileLayerName + " to " + newTileLayerName);
        tld.rename(oldTileLayerName, newTileLayerName);
    }

    /**
     * Returns the tile layers that refer to the given style, either as the tile layer's {@link
     * GeoServerTileLayer#getStyles() default style} or one of the {@link
     * GeoServerTileLayerInfoImpl#cachedStyles() cached styles}.
     *
     * <p>The result may be different from {@link #getLayerInfosFor(StyleInfo)} and {@link
     * #getLayerGroupsFor(StyleInfo)} as the {@link GeoServerTileLayerInfoImpl}'s backing each
     * {@link GeoServerTileLayer} may have assigned a subset of the layerinfo styles for caching.
     */
    public List<GeoServerTileLayer> getTileLayersForStyle(final String styleName) {

        Iterable<GeoServerTileLayer> tileLayers = getGeoServerTileLayers();

        List<GeoServerTileLayer> affected = new ArrayList<GeoServerTileLayer>();
        for (GeoServerTileLayer tl : tileLayers) {
            try {
                GeoServerTileLayerInfo info = tl.getInfo();
                String defaultStyle = tl.getStyles(); // may be null if backed by a LayerGroupInfo
                Set<String> cachedStyles = info.cachedStyles();
                if (styleName.equals(defaultStyle) || cachedStyles.contains(styleName)) {
                    affected.add(tl);
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to retrieve style info for layer" + tl.getName(), e);
            }
        }
        return affected;
    }

    /**
     * @return all the {@link LayerInfo}s in the {@link Catalog} that somehow refer to the given
     *     style
     */
    public Iterable<LayerInfo> getLayerInfosFor(final StyleInfo style) {
        return getLayerInfosFor(style, true);
    }

    /**
     * @return all the {@link LayerInfo}s in the {@link Catalog} that somehow refer to the given
     *     style
     */
    private Iterable<LayerInfo> getLayerInfosFor(
            final StyleInfo style, boolean includeSecondaryStyles) {
        List<LayerInfo> result = new ArrayList<LayerInfo>();
        Filter styleFilter =
                ff.equal(ff.property("defaultStyle.id"), ff.literal(style.getId()), true);
        if (includeSecondaryStyles) {
            styleFilter =
                    ff.or(
                            styleFilter,
                            ff.equal(
                                    ff.property("styles.id"),
                                    ff.literal(style.getId()),
                                    true,
                                    MatchAction.ANY));
        }

        try (CloseableIterator<LayerInfo> it = getCatalog().list(LayerInfo.class, styleFilter)) {
            while (it.hasNext()) {
                result.add(it.next());
            }
        } catch (Exception e) {
            log.log(
                    Level.SEVERE,
                    "Failed to layers associated to style " + style.prefixedName(),
                    e);
        }
        return result;
    }

    /** @return all the layergroups that somehow refer to the given style */
    public Iterable<LayerGroupInfo> getLayerGroupsFor(final StyleInfo style) {
        List<LayerGroupInfo> layerGroups = new ArrayList<LayerGroupInfo>();

        // get the layers whose default style is that style, they might be in layer groups
        // using their default style
        Iterable<LayerInfo> layers = getLayerInfosFor(style);

        // build a query retrieving the first list of candidates
        List<Filter> filters = new ArrayList<>();
        filters.add(
                ff.equal(
                        ff.property("styles.id"),
                        ff.literal(style.getId()),
                        true,
                        MatchAction.ANY));
        filters.add(ff.equal(ff.property("rootLayerStyle.id"), ff.literal(style.getId())));
        for (LayerInfo layer : layers) {
            filters.add(
                    ff.equal(
                            ff.property("layers.id"),
                            ff.literal(layer.getId()),
                            true,
                            MatchAction.ANY));
            filters.add(ff.equal(ff.property("rootLayer.id"), ff.literal(layer.getId())));
        }
        Or groupFilter = ff.or(filters);

        try (CloseableIterator<LayerGroupInfo> it =
                getCatalog().list(LayerGroupInfo.class, groupFilter)) {
            while (it.hasNext()) {
                LayerGroupInfo lg = it.next();
                if (isLayerGroupFor(lg, style)) {
                    layerGroups.add(lg);
                }
            }
        } catch (Exception e) {
            log.log(
                    Level.SEVERE,
                    "Failed to load groups associated to style " + style.prefixedName(),
                    e);
        }

        loadGroupParents(layerGroups);

        return layerGroups;
    }

    /** Given a list of groups, recursively loads all other groups containing any of them */
    private void loadGroupParents(List<LayerGroupInfo> layerGroups) {
        // we now have groups that are directly referencing the incriminated style, and need
        // to find all their parents, recursively...
        boolean foundNewParents = true;
        List<LayerGroupInfo> newGroups = new ArrayList<>(layerGroups);
        while (foundNewParents && !newGroups.isEmpty()) {
            List<Filter> parentFilters = new ArrayList<>();
            for (LayerGroupInfo lg : newGroups) {
                parentFilters.add(
                        ff.equal(
                                ff.property("layers.id"),
                                ff.literal(lg.getId()),
                                true,
                                MatchAction.ANY));
            }
            Or parentFilter = ff.or(parentFilters);
            newGroups.clear();
            foundNewParents = false;
            try (CloseableIterator<LayerGroupInfo> it =
                    getCatalog().list(LayerGroupInfo.class, parentFilter)) {
                while (it.hasNext()) {
                    LayerGroupInfo lg = it.next();
                    if (!layerGroups.contains(lg)) {
                        newGroups.add(lg);
                        layerGroups.add(lg);
                        foundNewParents = true;
                    }
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to recursively load parents group parents ");
            }
        }
    }

    private boolean isLayerGroupFor(LayerGroupInfo lg, StyleInfo style) {
        // check root layer
        if (style.equals(lg.getRootLayerStyle())
                || (lg.getRootLayerStyle() == null
                        && lg.getRootLayer() != null
                        && style.equals(lg.getRootLayer().getDefaultStyle()))) {
            return true;
        }
        // check the layers (and only the layers, not the sub-groups, if we got here
        // it means we have a style involved, or a layer that has the default style we search
        // but we don't know if the default style got overridden
        final int styleCount = lg.getStyles().size();
        final int layerCount = lg.getLayers().size();
        final int count = Math.max(styleCount, layerCount);
        for (int i = 0; i < count; i++) {
            // paranoid check in case the two lists are not in sync
            StyleInfo si = i < styleCount ? lg.getStyles().get(i) : null;
            PublishedInfo pi = i < layerCount ? lg.getLayers().get(i) : null;
            if (pi instanceof LayerInfo) {
                if (style.equals(si)) {
                    return true;
                } else {
                    LayerInfo li = (LayerInfo) pi;
                    if (style.equals(li.getDefaultStyle())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Computes and returns the area of validity for the given CoordinateReferenceSystem.
     *
     * <p>This method returns the prescribed area of validity for the CRS as computed by {@link
     * CRS#getEnvelope(CoordinateReferenceSystem)} with the exception that the following {@code
     * EPSG:900913} compatible CRS's return the GeoWebCache prescribed bounds so that they align
     * with Google Map tiles: {@code EPSG:900913, EPSG:3857, EPSG:3785}.
     *
     * @param coordSys the CRS to compute the area of validity for
     * @return the aov for the CRS, or {@code null} if the CRS does not provide such information
     *     (with the exception of EPSG:900913, see above).
     */
    public ReferencedEnvelope getAreaOfValidity(final CoordinateReferenceSystem coordSys) {
        Geometry aovGeom = getAreaOfValidityAsGeometry(coordSys, gridSetBroker);
        if (aovGeom == null) {
            return null;
        }
        Envelope envelope = aovGeom.getEnvelopeInternal();
        double x1 = envelope.getMinX();
        double x2 = envelope.getMaxX();
        double y1 = envelope.getMinY();
        double y2 = envelope.getMaxY();

        ReferencedEnvelope aov = new ReferencedEnvelope(coordSys);
        aov.init(x1, x2, y1, y2);
        return aov;
    }

    public static Geometry getAreaOfValidityAsGeometry(
            final CoordinateReferenceSystem targetCrs, final GridSetBroker gridSetBroker) {

        CoordinateReferenceSystem variant;
        String[] variants = {"EPSG:900913", "EPSG:3857", "EPSG:3785"};

        boolean is900913Compatible = false;
        for (String variantCode : variants) {
            variant = variant(variantCode);
            is900913Compatible = variant != null && CRS.equalsIgnoreMetadata(targetCrs, variant);
            if (is900913Compatible) {
                break;
            }
        }

        if (is900913Compatible) {
            BoundingBox prescribedBounds = gridSetBroker.getWorldEpsg3857().getBounds();
            return JTS.toGeometry(
                    new Envelope(
                            prescribedBounds.getMinX(),
                            prescribedBounds.getMaxX(),
                            prescribedBounds.getMinY(),
                            prescribedBounds.getMaxY()));
        }

        final org.opengis.geometry.Envelope envelope = CRS.getEnvelope(targetCrs);
        if (envelope == null) {
            return null;
        }

        Geometry aovGeom;

        final double tolerance = 1E-6;
        if (envelope.getSpan(0) < tolerance || envelope.getSpan(1) < tolerance) {
            //
            GeographicBoundingBox latLonBBox = CRS.getGeographicBoundingBox(targetCrs);
            ReferencedEnvelope bbox = new ReferencedEnvelope(new GeneralEnvelope(latLonBBox));
            Polygon geometry = JTS.toGeometry(bbox);
            double distanceTolerance = Math.max(bbox.getSpan(0), bbox.getSpan(1)) / 2E5;
            Geometry densifiedGeom = Densifier.densify(geometry, distanceTolerance);
            MathTransform mathTransform;
            try {
                CoordinateReferenceSystem sourceCRS = bbox.getCoordinateReferenceSystem();
                mathTransform = CRS.findMathTransform(sourceCRS, targetCrs);
                aovGeom = JTS.transform(densifiedGeom, mathTransform);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            aovGeom =
                    JTS.toGeometry(
                            new Envelope(
                                    envelope.getMinimum(0),
                                    envelope.getMaximum(0),
                                    envelope.getMinimum(1),
                                    envelope.getMaximum(1)));
        }

        aovGeom.setUserData(targetCrs);
        return aovGeom;
    }

    private static CoordinateReferenceSystem variant(String code) {
        CoordinateReferenceSystem variant;
        try {
            variant = CRS.decode(code);
        } catch (Exception e) {
            log.log(Level.FINE, e.getMessage(), e);
            return null;
        }
        return variant;
    }

    /**
     * Add the provided grid set id to the list of GeoServer grid sets that cannot be edited by the
     * user.
     */
    public void addEmbeddedGridSet(String gridSetId) {
        geoserverEmbeddedGridSets.add(gridSetId);
    }

    /**
     * @return {@code true} if the GridSet named {@code gridSetId} is a GWC internally defined one,
     *     {@code false} otherwise
     */
    public boolean isInternalGridSet(final String gridSetId) {
        return gridSetBroker.getEmbeddedNames().contains(gridSetId)
                || geoserverEmbeddedGridSets.contains(gridSetId);
    }

    /**
     * Completely deletes the cache for a layer/gridset combination; differs from truncate that the
     * layer doesn't need to have a gridSubset associated for the given gridset at runtime (in order
     * to handle the deletion of a layer's gridsubset)
     *
     * @param layerName The layer name
     * @param gridSetId The gridset name @TODO: make async?, it may take a while to the metastore to
     *     delete all tiles (sigh)
     */
    public void deleteCacheByGridSetId(final String layerName, final String gridSetId) {
        try {
            storageBroker.deleteByGridSetId(layerName, gridSetId);
        } catch (StorageException e) {
            Throwable throwable = getRootCause(e);
            Throwables.throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        }
    }

    /**
     * Completely and persistently eliminates, including the cached contents, the given tile layers.
     */
    public void removeTileLayers(final List<String> tileLayerNames) {
        checkNotNull(tileLayerNames);

        for (String tileLayerName : tileLayerNames) {
            try {
                tld.removeLayer(tileLayerName);
            } catch (IllegalArgumentException e) {
                log.log(
                        Level.WARNING,
                        "Error saving GWC Configuration "
                                + tld.getConfiguration(tileLayerName).getIdentifier(),
                        e);
            }
        }
    }

    public synchronized void removeGridSets(final Set<String> gridsetIds)
            throws IOException, GeoWebCacheException {
        checkNotNull(gridsetIds);

        final Set<String> affectedLayers = getLayerNamesForGridSets(gridsetIds);

        for (String layerName : affectedLayers) {
            TileLayer tileLayer = getTileLayerByName(layerName);
            Lock lock = null;
            try {
                lock = lockProvider.getLock("gwc_lock_layer_" + layerName);

                for (String gridSetId : gridsetIds) {
                    if (tileLayer.getGridSubsets().contains(gridSetId)) {
                        tileLayer.removeGridSubset(gridSetId);
                        deleteCacheByGridSetId(layerName, gridSetId);
                    }
                }
                if (tileLayer.getGridSubsets().isEmpty()) {
                    tileLayer.setEnabled(false);
                }
                try {
                    tld.modify(tileLayer);
                } catch (IllegalArgumentException ignore) {
                    // layer removed? don't care
                }
            } finally {
                if (lock != null) {
                    lock.release();
                }
            }
        }

        for (String gridSetId : gridsetIds) {
            gridSetBroker.remove(gridSetId);
        }
    }

    /**
     * Creates new tile layers for the layers and layergroups given by their names using the
     * settings of the given default config options
     */
    public void autoConfigureLayers(List<String> catalogLayerNames, GWCConfig saneConfig) {
        checkArgument(saneConfig.isSane());

        final Catalog catalog = getCatalog();
        for (String name : catalogLayerNames) {

            checkArgument(
                    !tileLayerExists(name),
                    "Can't auto configure Layer, a tile layer named '",
                    name,
                    "' already exists.");

            GeoServerTileLayer tileLayer = null;
            LayerInfo layer = catalog.getLayerByName(name);

            if (layer != null) {
                tileLayer = new GeoServerTileLayer(layer, saneConfig, gridSetBroker);
            } else {
                LayerGroupInfo layerGroup = catalog.getLayerGroupByName(name);
                if (layerGroup != null) {
                    tileLayer = new GeoServerTileLayer(layerGroup, saneConfig, gridSetBroker);
                }
            }
            if (tileLayer != null) {
                add(tileLayer);
            } else {
                log.warning("Requested layer " + name + " does not exist. Won't create TileLayer");
            }
        }
    }

    /**
     * @param source either a {@link LayerInfo} or a {@link LayerGroupInfo}
     * @return {@code true} if source has a tile layer associated, false otherwise, even if source
     *     is not an instance of {@link LayerInfo} or {@link LayerGroupInfo}
     */
    public boolean hasTileLayer(CatalogInfo source) {
        final String tileLayerName;
        if (source instanceof ResourceInfo) {
            LayerInfo layerInfo =
                    getCatalog().getLayerByName(((ResourceInfo) source).prefixedName());
            if (layerInfo == null) {
                return false;
            }
            tileLayerName = tileLayerName(layerInfo);
        } else if (source instanceof LayerInfo) {
            tileLayerName = tileLayerName((LayerInfo) source);
        } else if (source instanceof LayerGroupInfo) {
            tileLayerName = tileLayerName((LayerGroupInfo) source);
        } else {
            return false;
        }
        return tld.layerExists(tileLayerName);
    }

    /**
     * @param source either a {@link LayerInfo}, {@link ResourceInfo}, or a {@link LayerGroupInfo}
     * @return {@code null}
     * @throws IllegalArgumentException if source is not of a supported type
     */
    public GeoServerTileLayer getTileLayer(CatalogInfo source) {
        final String name;
        if (source instanceof ResourceInfo) {
            name = ((ResourceInfo) source).prefixedName();
        } else if (source instanceof LayerInfo) {
            name = tileLayerName(((LayerInfo) source));
        } else if (source instanceof LayerGroupInfo) {
            name = tileLayerName(((LayerGroupInfo) source));
        } else {
            return null;
        }
        TileLayer tileLayer;
        try {
            tileLayer = tld.getTileLayer(name);
        } catch (GeoWebCacheException notFound) {
            return null;
        }
        if (tileLayer instanceof GeoServerTileLayer) {
            return (GeoServerTileLayer) tileLayer;
        }
        return null;
    }

    /**
     * Verify that a layer is accessible within a certain bounding box using the (secured) catalog
     *
     * @param layerName name of the layer
     * @param boundingBox bounding box
     */
    public void verifyAccessLayer(String layerName, ReferencedEnvelope boundingBox)
            throws ServiceException, SecurityException {
        // get the list of internal layers corresponding to the advertised layer
        List<LayerInfo> layerInfos = null;
        LayerInfo li = getCatalog().getLayerByName(layerName);
        if (li != null) {
            layerInfos = Arrays.asList(li);
        } else {
            // tricky here, first we need to flatten the group, and we also need
            // to make sure we are getting the full layer group, not just part of it
            // otherwise we are going to cache different views for different users
            LayerGroupInfo group = getCatalog().getLayerGroupByName(layerName);
            if (group != null) {
                // use the prefixed name to avoid clashes because the raw catalog is not
                // workspace-filtered
                LayerGroupInfo rawGroup;
                if (group.getWorkspace() != null) {
                    // LocalWorkspace has a NameDequalifyingProxy which will strip off the workspace
                    // if we just call prefixedName
                    rawGroup =
                            rawCatalog.getLayerGroupByName(group.getWorkspace(), group.getName());
                } else {
                    rawGroup = rawCatalog.getLayerGroupByName(group.getName());
                }
                if (rawGroup.layers().size() == group.layers().size()) {
                    layerInfos = group.layers();
                }
            }
        }

        if (layerInfos == null || layerInfos.isEmpty()) {
            throw new ServiceException("Could not find layer " + layerName, "LayerNotDefined");
        }
        if (boundingBox != null) {
            for (LayerInfo layerInfo : layerInfos) {
                // Unwrap potential proxy instances, so the instanceof SecuredLayerInfo check works.
                if (layerInfo instanceof Proxy) {
                    layerInfo =
                            ProxyUtils.unwrap(
                                    layerInfo, Proxy.getInvocationHandler(layerInfo).getClass());
                }

                if (layerInfo instanceof SecuredLayerInfo) {
                    // test layer bbox limits
                    SecuredLayerInfo securedLayerInfo = (SecuredLayerInfo) layerInfo;
                    WrapperPolicy policy = securedLayerInfo.getWrapperPolicy();
                    AccessLimits limits = policy.getLimits();

                    if (limits instanceof DataAccessLimits) {
                        // ensure we are all using the same CRS
                        CoordinateReferenceSystem dataCrs = layerInfo.getResource().getCRS();
                        if (boundingBox.getCoordinateReferenceSystem() != null
                                && !CRS.equalsIgnoreMetadata(
                                        dataCrs, boundingBox.getCoordinateReferenceSystem())) {
                            try {
                                boundingBox = boundingBox.transform(dataCrs, true);
                            } catch (Exception e) {
                                // bboxes not compatible? deny access for all certainty.
                                boundingBox = null;
                            }
                        }
                        Envelope limitBox =
                                new ReferencedEnvelope(ReferencedEnvelope.EVERYTHING, dataCrs);

                        Filter filter = ((DataAccessLimits) limits).getReadFilter();
                        if (filter != null) {
                            // extract filter envelope from filter
                            Envelope box =
                                    (Envelope)
                                            filter.accept(
                                                    ExtractBoundsFilterVisitor.BOUNDS_VISITOR,
                                                    null);
                            if (box != null) {
                                limitBox =
                                        new ReferencedEnvelope(limitBox.intersection(box), dataCrs);
                            }
                        }
                        if (limits instanceof CoverageAccessLimits) {
                            if (((CoverageAccessLimits) limits).getRasterFilter() != null) {
                                Envelope box =
                                        ((CoverageAccessLimits) limits)
                                                .getRasterFilter()
                                                .getEnvelopeInternal();
                                if (box != null) {
                                    limitBox =
                                            new ReferencedEnvelope(
                                                    limitBox.intersection(box), dataCrs);
                                }
                            }
                        }
                        if (limits instanceof WMSAccessLimits) {
                            if (((WMSAccessLimits) limits).getRasterFilter() != null) {
                                Envelope box =
                                        ((WMSAccessLimits) limits)
                                                .getRasterFilter()
                                                .getEnvelopeInternal();
                                if (box != null) {
                                    limitBox =
                                            new ReferencedEnvelope(
                                                    limitBox.intersection(box), dataCrs);
                                }
                            }
                        }

                        if (!limitBox.covers(ReferencedEnvelope.EVERYTHING)
                                && (boundingBox == null || !limitBox.contains(boundingBox))) {
                            throw new SecurityException(
                                    "Access denied to bounding box on layer " + layerName);
                        }
                    }
                }
            }
        }
    }

    CoordinateReferenceSystem getCRSForGridset(GridSubset gridSubset)
            throws NoSuchAuthorityCodeException, FactoryException {
        return CRS.decode(gridSubset.getSRS().toString());
    }

    public CoordinateReferenceSystem getDeclaredCrs(final String geoServerTileLayerName) {
        GeoServerTileLayer layer = (GeoServerTileLayer) getTileLayerByName(geoServerTileLayerName);
        PublishedInfo published = layer.getPublishedInfo();
        if (published instanceof LayerInfo) {
            return ((LayerInfo) published).getResource().getCRS();
        }
        LayerGroupInfo layerGroupInfo = (LayerGroupInfo) published;
        ReferencedEnvelope bounds = layerGroupInfo.getBounds();
        return bounds.getCoordinateReferenceSystem();
    }

    public static String tileLayerName(LayerInfo li) {
        // REVISIT when/if layerinfo.getName gets decoupled from LayerInfo.resource.name
        return li.getResource().prefixedName();
    }

    public static String tileLayerName(LayerGroupInfo lgi) {
        return lgi.prefixedName();
    }

    public static void tryReset() {
        GWC instance = GWC.INSTANCE;
        if (instance != null) {
            GWC.INSTANCE.reset();
        }
    }

    /** Flush caches */
    void reset() {
        CatalogConfiguration c = GeoServerExtensions.bean(CatalogConfiguration.class);
        if (c != null) {
            c.reset();
        }
    }

    public LockProvider getLockProvider() {
        return lockProvider;
    }

    public JDBCConfiguration getJDBCDiskQuotaConfig()
            throws IOException, org.geowebcache.config.ConfigurationException {
        return jdbcConfigurationStorage.getJDBCDiskQuotaConfig();
    }

    /**
     * Checks the JDBC quota store can be instantiated
     *
     * @param jdbcConfiguration The JDBC Quota Store configuration
     * @throws ConfigurationException if the quota store cannot be instantiated
     */
    public void testQuotaConfiguration(JDBCConfiguration jdbcConfiguration)
            throws ConfigurationException, IOException {
        jdbcConfigurationStorage.testQuotaConfiguration(jdbcConfiguration);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.gwcEnvironment = GeoServerExtensions.bean(GeoWebCacheEnvironment.class);

        syncEnv();
    }

    /**
     * Synchronizes environment properties between the {@link GeoServerEnvironment} and the {@link
     * GeoWebCacheEnvironment}. (GeoServer properties will override GeoWebCache properties)
     */
    public void syncEnv() throws IllegalArgumentException {
        if (gsEnvironment != null && gsEnvironment.isStale() && gwcEnvironment != null) {
            if (GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION
                    && gsEnvironment.getProps() != null) {
                Properties gwcProps = gwcEnvironment.getProps();

                if (gwcProps == null) {
                    gwcProps = new Properties();
                }
                gwcProps.putAll(gsEnvironment.getProps());

                gwcEnvironment.setProps(gwcProps);
            }
        }
    }

    /**
     * Returns the list of {@link MimeType#getFormat() MIME Type formats} advertised as valid for
     * caching for the given type of published kind of layer.
     *
     * <p>Handles the case where some tile formats may be appropriate for vector layers but not for
     * raster layers, or vice-versa.
     *
     * <p>Loads all resources in the classpath named {@code
     * /org/geoserver/gwc/advertised_formats.properties} so other modules can contribute advertised
     * formats without introducing unneeded dependencies.
     *
     * <p>{@code /org/geoserver/gwc/advertised_formats.properties} has entries for the following
     * keys, whose values are a comma separated list of GWC MIME format names: {@code
     * formats.vector}, {@code formats.raster}, and {@code formats.group}
     *
     * @param type the kind of geoserver published resource the tile layer is tied to.
     * @return the set of advertised mime types for the given kind of tile layer origin
     */
    public static Set<String> getAdvertisedCachedFormats(final PublishedType type) {

        final String resourceName = "org/geoserver/gwc/advertised_formats.properties";

        try {
            ClassLoader classLoader = GWC.class.getClassLoader();
            List<URL> urls = newArrayList(forEnumeration(classLoader.getResources(resourceName)));
            return GWC.getAdvertisedCachedFormats(type, urls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // visible for testing purposes only
    static Set<String> getAdvertisedCachedFormats(
            final PublishedType type, final Iterable<URL> urls) throws IOException {
        final String formatsKey;

        switch (type) {
            case VECTOR: // local vector layer
            case REMOTE: // remote WFS
                formatsKey = "formats.vector";
                break;
            case RASTER: // local raster layer
            case WMS: // remote WMS raster
            case WMTS: // remote WMTS raster
                formatsKey = "formats.raster";
                break;
            case GROUP:
                formatsKey = "formats.layergroup";
                break;
            default:
                throw new IllegalArgumentException("Unknown published type: " + type);
        }
        Set<String> formats = new TreeSet<String>();

        for (URL url : urls) {
            Properties props = new Properties();
            props.load(url.openStream());
            String commaSeparatedFormats = props.getProperty(formatsKey);
            if (commaSeparatedFormats != null) {
                List<String> splitToList =
                        Splitter.on(",")
                                .omitEmptyStrings()
                                .trimResults()
                                .splitToList(commaSeparatedFormats);
                formats.addAll(splitToList);
            }
        }
        return formats;
    }

    /** @return the list of configured blobstores */
    public List<BlobStoreInfo> getBlobStores() {
        BlobStoreAggregator agg = getBlobStoreAggregator();
        Iterable<BlobStoreInfo> blobStores = agg.getBlobStores();
        if (blobStores instanceof List) {
            return (List<BlobStoreInfo>) blobStores;
        } else {
            ArrayList<BlobStoreInfo> storeInfos =
                    new ArrayList<BlobStoreInfo>(agg.getBlobStoreCount());
            blobStores.forEach(storeInfos::add);
            return storeInfos;
        }
    }

    /**
     * @return the {@link BlobStoreInfo#isDefault() default} blobstore, or {@code null} if there's
     *     no default
     */
    public BlobStoreInfo getDefaultBlobStore() {
        BlobStoreAggregator agg = getBlobStoreAggregator();

        // TODO We should be doing this on the aggregator upstream in GWC

        for (BlobStoreInfo config : agg.getBlobStores()) {
            if (config.isDefault()) {
                return config;
            }
        }
        return null;
    }

    /**
     * Convenience method to add a new blob store, calling {@link #setBlobStores} the extra {@code
     * config}
     */
    public void addBlobStore(BlobStoreInfo info) throws ConfigurationException {
        checkNotNull(info);
        getBlobStoreAggregator().addBlobStore(info);
    }

    /**
     * Convenience method to modify a blobstore; calling {@link #setBlobStores(List)} with the
     * config identified by {@code oldId} repplaced by {@code config}
     */
    public void modifyBlobStore(String oldId, BlobStoreInfo config) throws ConfigurationException {
        checkNotNull(oldId);
        checkNotNull(config);
        BlobStoreAggregator agg = getBlobStoreAggregator();

        if (config.getName().equals(oldId)) {
            agg.modifyBlobStore(config);
        } else {
            synchronized (agg) {
                agg.renameBlobStore(oldId, config.getName());
                agg.modifyBlobStore(config);
            }
        }
    }

    /**
     * Convenience method to remove blobstores by id; a filtered view of the blobstores
     * configuration objects is passed to {@link #setBlobStores(List)}
     *
     * @param blobStoreIds the unique identifiers for the blobstores that will be removed from the
     *     runtime {@link CompositeBlobStore} state and the xml configuration.
     * @see {@link #setBlobStores}
     */
    public void removeBlobStores(Iterable<String> blobStoreIds) throws ConfigurationException {
        checkNotNull(blobStoreIds);

        BlobStoreAggregator agg = getBlobStoreAggregator();

        LinkedList<Exception> exceptions = new LinkedList<>();
        for (String bsName : blobStoreIds) {
            try {
                agg.removeBlobStore(bsName);
            } catch (Exception ex) {
                exceptions.add(ex);
            }
        }
        if (!exceptions.isEmpty()) {
            Exception ex = exceptions.pop();
            exceptions.forEach(ex::addSuppressed);
        }
    }

    /**
     * Replaces the configured {@link BlobStore}s by the provided {@code stores} and saves the
     * configuration.
     *
     * <p>{@link CompositeBlobStore#setBlobStores} is called to replace the blob stores running. If
     * it succeeds, then the configuration is saved. If either replacing the runtime stores or
     * saving the config fails, the original blob stores are re-applied to the runtime configuration
     * and a {@link ConfigurationException} is thrown.
     *
     * @param stores the new set of blob stores
     * @throws ConfigurationException if the running blobstores can't be replaced by the provided
     *     ones or the configuration can't be saved
     * @see CompositeBlobStore#setBlobStores(Iterable)
     */
    void setBlobStores(List<BlobStoreInfo> stores) throws ConfigurationException {
        Preconditions.checkNotNull(stores, "stores is null");

        BlobStoreAggregator agg = getBlobStoreAggregator();

        Collection<String> existingStoreNames = agg.getBlobStoreNames();
        Set<String> toDelete = new TreeSet<>(existingStoreNames);
        try {
            for (BlobStoreInfo info : stores) {
                toDelete.remove(info.getName());
                if (existingStoreNames.contains(info.getName())) {
                    agg.modifyBlobStore(info);
                } else {
                    agg.addBlobStore(info);
                }
            }
            for (String name : toDelete) {
                agg.removeBlobStore(name);
            }
        } catch (ConfigurationPersistenceException ex) {
            throw new ConfigurationException("Error saving config", ex);
        }
    }

    CompositeBlobStore getCompositeBlobStore() {
        CompositeBlobStore compositeBlobStore =
                GeoWebCacheExtensions.bean(CompositeBlobStore.class);
        checkNotNull(compositeBlobStore);
        return compositeBlobStore;
    }

    /** @return the gwcEnvironment */
    public GeoWebCacheEnvironment getGwcEnvironment() {
        return gwcEnvironment;
    }

    /** Returns the list of pending tasks in the tile breeder */
    public Iterator<GWCTask> getPendingTasks() {
        return tileBreeder.getPendingTasks();
    }
}
