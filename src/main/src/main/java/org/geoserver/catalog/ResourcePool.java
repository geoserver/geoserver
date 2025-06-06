/* (c) 2014 - 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static java.util.Collections.emptyList;

import java.awt.RenderingHints;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDParticle;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.XSDTypeDefinition;
import org.eclipse.xsd.util.XSDSchemaLocator;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.util.CoverageStoreUtils;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.feature.retype.RetypingFeatureSource;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.DataAccessFactory.Param;
import org.geotools.api.data.DataAccessFinder;
import org.geotools.api.data.DataSourceException;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Join;
import org.geotools.api.data.Repository;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.metadata.citation.Citation;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.SingleCRS;
import org.geotools.api.referencing.cs.CoordinateSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.brewer.styling.builder.StyleBuilder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.data.store.ContentState;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.GML;
import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPClientFinder;
import org.geotools.http.HTTPConnectionPooling;
import org.geotools.http.SimpleHttpClient;
import org.geotools.measure.Measure;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wms.xml.WMSSchema;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.model.WMTSCapabilities;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.util.CanonicalSet;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.URLs;
import org.geotools.util.Utilities;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.geotools.xml.DocumentFactory;
import org.geotools.xml.XMLHandlerHints;
import org.geotools.xml.handlers.DocumentHandler;
import org.geotools.xsd.Schemas;
import org.springframework.context.ApplicationContext;
import org.vfny.geoserver.global.GeoServerFeatureLocking;
import org.vfny.geoserver.global.GeoServerFeatureSource;
import org.vfny.geoserver.global.GeoserverComplexFeatureSource;
import org.vfny.geoserver.util.DataStoreUtils;
import org.xml.sax.EntityResolver;
import si.uom.NonSI;
import si.uom.SI;

/**
 * Provides access to resources such as datastores, coverage readers, and feature types.
 *
 * <p>Provides caches for:
 *
 * <ul>
 *   <li>{@link #crsCache} - quick lookup of CoorrdinateReferenceSystem by srs name
 *   <li>{@link #dataStoreCache} - live {@link DataAccess} connections. Responsible for maintaining lifecycle with an
 *       appropriate call to {@link DataAccess#dispose()} when no longer in use.
 *   <li>{@link #featureTypeCache}
 *   <li>{@link #featureTypeAttributeCache}
 *   <li>{@link #wmsCache}
 *   <li>{@link #hintCoverageReaderCache}
 *   <li>{@link #sldCache}
 *   <li>{@link #styleCache}
 *
 * @author Justin Deoliveira, Boundless
 */
public class ResourcePool {

    /** OGC "cylindrical earth" model, we'll use it to translate meters to degrees (yes, it's ugly) */
    static final double OGC_DEGREE_TO_METERS = 6378137.0 * 2.0 * Math.PI / 360;

    static final double OGC_METERS_TO_DEGREES = 1 / OGC_DEGREE_TO_METERS;

    private static final String PROJECTION_POLICY_SEPARATOR = "_pp_";

    /** Hint to specify if reprojection should occur while loading a resource. */
    public static Hints.Key REPROJECT = new Hints.Key(Boolean.class);

    /** Hint to specify additional joined attributes when loading a feature type */
    public static Hints.Key JOINS = new Hints.Key(List.class);

    public static Hints.Key MAP_CRS = new Hints.Key(CoordinateReferenceSystem.class);

    /** logging */
    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog");

    /** Default number of hard references */
    static int FEATURETYPE_CACHE_SIZE_DEFAULT = 100;

    static String CRS_NOT_FOUND = "CRS_NOT_FOUND";
    static Map<CoordinateReferenceSystem, String> crsIdentifierCache = new SoftValueHashMap<>();

    Catalog catalog;
    Map<String, CoordinateReferenceSystem> crsCache;

    DataStoreCache dataStoreCache;
    Map<String, FeatureType> featureTypeCache;
    Map<String, List<AttributeTypeInfo>> featureTypeAttributeCache;
    Map<String, WebMapServer> wmsCache;
    Map<String, WebMapTileServer> wmtsCache;
    Map<CoverageHintReaderKey, GridCoverageReader> hintCoverageReaderCache;
    Map<String, StyledLayerDescriptor> sldCache;
    Map<String, Style> styleCache;

    List<Listener> listeners;
    ThreadPoolExecutor coverageExecutor;
    CatalogRepository repository;
    EntityResolverProvider entityResolverProvider;

    /**
     * Applies attributes customization. It's a {@link RetypeFeatureTypeCallback}, it needs to be applied sooner in the
     * process of setting up a FeatureSource, so it does not fit exactly the extension point lifecycle.
     */
    TransformFeatureTypeCallback transformer = new TransformFeatureTypeCallback();

    /** Holds the keys for all the cache having String keys. By ensuring identity allows to synchronize on id values. */
    private CanonicalSet<String> cacheKeys;

    /** Holds the key for the CoverageHintReaderCache. By ensuring identity allows to synchronize on the key. */
    private CanonicalSet<CoverageHintReaderKey> coverageCacheKeys;

    /** Creates a new instance of the resource pool explicitly supplying the application context. */
    public static ResourcePool create(Catalog catalog, ApplicationContext appContext) {
        // look for an implementation in spring context
        ResourcePool pool = appContext == null
                ? GeoServerExtensions.bean(ResourcePool.class)
                : GeoServerExtensions.bean(ResourcePool.class, appContext);
        if (pool == null) {
            pool = new ResourcePool();
        }
        pool.setCatalog(catalog);
        return pool;
    }

    protected ResourcePool() {
        crsCache = createCrsCache();
        dataStoreCache = createDataStoreCache();
        featureTypeCache = createFeatureTypeCache(FEATURETYPE_CACHE_SIZE_DEFAULT);

        featureTypeAttributeCache = createFeatureTypeAttributeCache(FEATURETYPE_CACHE_SIZE_DEFAULT);
        hintCoverageReaderCache = createHintCoverageReaderCache();

        wmsCache = createWmsCache();
        wmtsCache = createWmtsCache();
        sldCache = createSldCache();
        styleCache = createStyleCache();

        cacheKeys = CanonicalSet.newInstance(String.class);
        coverageCacheKeys = CanonicalSet.newInstance(CoverageHintReaderKey.class);
        listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Creates the resource pool.
     *
     * <p>Client code should use {@link ResourcePool#create(Catalog)} instead of calling this constructor directly.
     */
    protected ResourcePool(Catalog catalog) {
        this();
        setCatalog(catalog);
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        this.repository = new CatalogRepository(catalog);

        catalog.removeListeners(CacheClearingListener.class);
        catalog.addListener(new CacheClearingListener(this));
    }

    /**
     * Returns the cache for {@link CoordinateReferenceSystem} objects.
     *
     * <p>The cache key is the CRS identifier (see {@link #getCRS(String)}) for allowable forms.
     *
     * <p>The concrete Map implementation is determined by {@link #createCrsCache()}.
     */
    public Map<String, CoordinateReferenceSystem> getCrsCache() {
        return crsCache;
    }

    protected Map<String, CoordinateReferenceSystem> createCrsCache() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Returns the cache for {@link DataAccess} objects.
     *
     * <p>The cache key is the corresponding DataStoreInfo id ({@link CatalogInfo#getId()}).
     *
     * <p>The concrete Map implementation is determined by {@link #createDataStoreCache()}.
     */
    public Map<String, DataAccess> getDataStoreCache() {
        return dataStoreCache;
    }

    /**
     * DataStoreCache implementation responsible for freeing DataAccess resources when they are no longer in use.
     *
     * @return Cache used to look up DataAccess via id
     * @see #getDataStoreCache()
     */
    protected DataStoreCache createDataStoreCache() {
        return new DataStoreCache();
    }

    /**
     * Returns the cache for {@link FeatureType} objects.
     *
     * <p>The cache key is the corresponding FeatureTypeInfo id ({@link CatalogInfo#getId()}.
     *
     * <p>The concrete Map implementation is determined by {@link #createFeatureTypeCache(int)}.
     */
    public Map<String, FeatureType> getFeatureTypeCache() {
        return featureTypeCache;
    }

    protected Map<String, FeatureType> createFeatureTypeCache(int size) {
        // for each feature type we cache two versions, one with the projection policy applied, one
        // without it
        return new FeatureTypeCache(size * 2);
    }

    /**
     * Returns the cache for {@link AttributeTypeInfo} objects for a particular feature type.
     *
     * <p>The cache key is the corresponding FeatureTypeInfo id ({@link CatalogInfo#getId()}.
     *
     * <p>The concrete Map implementation is determined by {@link #createFeatureTypeAttributeCache(int)}
     */
    public Map<String, List<AttributeTypeInfo>> getFeatureTypeAttributeCache() {
        return featureTypeAttributeCache;
    }

    protected Map<String, List<AttributeTypeInfo>> createFeatureTypeAttributeCache(int size) {
        // for each feature type we cache two versions, one with the projection policy applied, one
        // without it
        return new FeatureTypeAttributeCache(size * 2);
    }

    /**
     * Returns the cache for {@link GridCoverageReader} objects for a particular coverage hint.
     *
     * <p>The concrete Map implementation is determined by {@link #createHintCoverageReaderCache()}
     */
    public Map<CoverageHintReaderKey, GridCoverageReader> getHintCoverageReaderCache() {
        return hintCoverageReaderCache;
    }

    protected Map<CoverageHintReaderKey, GridCoverageReader> createHintCoverageReaderCache() {
        return new CoverageHintReaderCache();
    }

    /**
     * Returns the cache for {@link StyledLayerDescriptor} objects for a particular style.
     *
     * <p>The concrete Map implementation is determined by {@link #createSldCache()}
     */
    public Map<String, StyledLayerDescriptor> getSldCache() {
        return sldCache;
    }

    protected Map<String, StyledLayerDescriptor> createSldCache() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Returns the cache for {@link Style} objects for a particular style.
     *
     * <p>The concrete Map implementation is determined by {@link #createStyleCache()}
     */
    public Map<String, Style> getStyleCache() {
        return styleCache;
    }

    protected Map<String, Style> createStyleCache() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Returns the cache for {@link WebMapServer} objects for a particular {@link WMSStoreInfo}.
     *
     * <p>The cache key is the corresponding {@link WMSStoreInfo} id ({@link CatalogInfo#getId()}.
     *
     * <p>The concrete Map implementation is determined by {@link #createWmsCache()}
     */
    public Map<String, WebMapServer> getWmsCache() {
        return wmsCache;
    }

    public Map<String, WebMapTileServer> getWmtsCache() {
        return wmtsCache;
    }

    protected Map<String, WebMapServer> createWmsCache() {
        return new WMSCache();
    }

    protected Map<String, WebMapTileServer> createWmtsCache() {
        return new WMTSCache();
    }

    /**
     * Sets the size of the feature type cache.
     *
     * <p>A warning that calling this method will blow away the existing cache.
     */
    public void setFeatureTypeCacheSize(int featureTypeCacheSize) {
        synchronized (this) {
            featureTypeCache.clear();
            featureTypeCache = createFeatureTypeCache(featureTypeCacheSize);
            featureTypeAttributeCache.clear();
            featureTypeAttributeCache = createFeatureTypeAttributeCache(featureTypeCacheSize);
        }
    }

    /**
     * Sets the coverage executor used for concurrent processing of files (e.g. in image mosaic, when multi-threaded
     * loading is enabled)
     */
    public synchronized void setCoverageExecutor(ThreadPoolExecutor coverageExecutor) {
        this.coverageExecutor = coverageExecutor;
    }

    /** Returns the coverage executor. See also {@link #setCoverageExecutor(ThreadPoolExecutor)}. */
    public synchronized ThreadPoolExecutor getCoverageExecutor() {
        return this.coverageExecutor;
    }

    /** Adds a pool listener. */
    public void addListener(Listener l) {
        listeners.add(l);
    }

    /** Removes a pool listener. */
    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    /** Sets the entity resolver provider injected in the code doing XML parsing */
    public void setEntityResolverProvider(EntityResolverProvider entityResolverProvider) {
        this.entityResolverProvider = entityResolverProvider;
    }

    /** Returns the entity resolver provider injected in the code doing XML parsing */
    public EntityResolverProvider getEntityResolverProvider() {
        return entityResolverProvider;
    }

    /** Creates a new instance of the resource pool. */
    public static ResourcePool create(Catalog catalog) {
        return create(catalog, null);
    }

    /**
     * Returns a {@link CoordinateReferenceSystem} object based on its identifier caching the result.
     *
     * <p>The <tt>srsName</tt> parameter should have one of the forms:
     *
     * <ul>
     *   <li><authority>:XXXX
     *   <li>http://www.opengis.net/gml/srs/<authority>.xml#XXXX
     *   <li>urn:x-ogc:def:crs:<authority>:XXXX
     * </ul>
     *
     * OR be something parsable by {@link CRS#decode(String)}.
     *
     * @param srsName The coordinate reference system identifier.
     * @throws IOException In the event the srsName can not be parsed or leads to an exception in the underlying call to
     *     CRS.decode.
     */
    public CoordinateReferenceSystem getCRS(String srsName) throws IOException {

        if (srsName == null) return null;
        srsName = cacheKeys.unique(srsName);
        CoordinateReferenceSystem crs = crsCache.get(srsName);
        if (crs == null) {
            synchronized (srsName) {
                crs = crsCache.get(srsName);
                if (crs == null) {
                    try {
                        crs = CRS.decode(srsName);
                        crsCache.put(srsName, crs);
                    } catch (Exception e) {
                        throw (IOException) new IOException().initCause(e);
                    }
                }
            }
        }

        return crs;
    }

    /**
     * Looks up the identifier of a given {@link CoordinateReferenceSystem} object, giving a preference to EPSG codes
     * expressed as <code>EPSG:xyzw</code>, when possible, and returning another code, otherwise. This behavior is
     * specific to GeoServer, thus, it has not been included in the GeoTools {@link CRS} facade.
     *
     * @param crs The coordinate reference system.
     * @param fullScan If {@code true}, an exhaustive full scan against all registered objects will be performed (may be
     *     slow). Otherwise only a fast lookup based on embedded identifiers and names will be performed.
     */
    public static String lookupIdentifier(CoordinateReferenceSystem crs, boolean fullScan) throws FactoryException {
        // full scan can be pretty expensive, cache results
        if (fullScan) {
            String identifier = crsIdentifierCache.get(crs);
            if (identifier != null) {
                return CRS_NOT_FOUND.equals(identifier) ? null : identifier;
            }

            identifier = lookupIdentifierInternal(crs, fullScan);
            crsIdentifierCache.put(crs, identifier == null ? CRS_NOT_FOUND : identifier);
            return identifier;
        } else {
            return lookupIdentifierInternal(crs, fullScan);
        }
    }

    private static String lookupIdentifierInternal(CoordinateReferenceSystem crs, boolean fullScan)
            throws FactoryException {
        // Lookup the first code, it should be the official one for this CRS
        String result = crs.getIdentifiers().stream()
                .filter(id -> id.getAuthority() != null)
                .filter(id -> id.getCode() != null)
                .findFirst()
                .map(id -> id.toString())
                .orElse(null);
        // .. then validate it can be used to lookup the CRS, and that it matches
        if (result != null) {
            try {
                // make sure the identifier is recognized (allows a lookup)
                CoordinateReferenceSystem lookedUp = CRS.decode(result);
                if (lookedUp != null) return result;
            } catch (Exception e) {
                LOGGER.log(
                        Level.FINE,
                        "Failed to lookup the CRS code for "
                                + crs
                                + " as "
                                + result
                                + ", moving on to look up other potential identifiers",
                        e);
            }
        }

        // otherwise look up for EPSG codes first
        Integer code = CRS.lookupEpsgCode(crs, false);
        if (code != null) {
            return "EPSG:" + code;
        }

        // search in other authorities, skipping the alias ones
        final Set<Citation> authorities = new LinkedHashSet<>();
        for (final CRSAuthorityFactory factory : ReferencingFactoryFinder.getCRSAuthorityFactories(null)) {
            authorities.add(factory.getAuthority());
        }
        authorities.remove(Citations.HTTP_OGC);
        authorities.remove(Citations.HTTP_URI_OGC);
        authorities.remove(Citations.AUTO);
        authorities.remove(Citations.URN_OGC);
        for (Citation authority : authorities) {
            result = CRS.lookupIdentifier(authority, crs, fullScan);
            if (result != null) return result;
        }
        return null;
    }

    /**
     * Returns the datastore factory used to create underlying resources for a datastore.
     *
     * <p>This method first uses {@link DataStoreInfo#getType()} to obtain the datastore. In the event of a failure it
     * falls back on {@link DataStoreInfo#getConnectionParameters()}.
     *
     * @param info The data store metadata.
     * @return The datastore factory, or null if no such factory could be found, or the factory is not available.
     * @throws IOException Any I/O errors.
     */
    public DataAccessFactory getDataStoreFactory(DataStoreInfo info) throws IOException {
        DataAccessFactory factory = null;

        DataStoreInfo expandedStore = clone(info, true);

        if (info.getType() != null) {
            factory = DataStoreUtils.aquireFactory(expandedStore.getType());
        }

        if (factory == null && expandedStore.getConnectionParameters() != null) {
            Map<String, Serializable> params =
                    getParams(expandedStore.getConnectionParameters(), catalog.getResourceLoader());
            factory = DataStoreUtils.aquireFactory(params);
        }

        return factory;
    }

    /**
     * Returns the underlying resource for a DataAccess, caching the result.
     *
     * <p>In the result of the resource not being in the cache {@link DataStoreInfo#getConnectionParameters()} is used
     * to create the connection.
     *
     * @param info DataStoreMeta providing id used for cache lookup (and connection paraemters if a connection is
     *     needed)
     * @throws IOException Any errors that occur connecting to the resource.
     */
    @SuppressWarnings("unchecked")
    public DataAccess<? extends FeatureType, ? extends Feature> getDataStore(DataStoreInfo info) throws IOException {

        DataStoreInfo expandedStore = clone(info, true);

        DataAccess<? extends FeatureType, ? extends Feature> dataStore = null;
        try {

            String storeId = info.getId();
            // cache only if the id is not null, no need to cache the stores
            // returned from un-saved DataStoreInfo objects (it would be actually
            // harmful, NPE when trying to dispose of them)
            if (storeId == null) return createDataAccess(info, expandedStore);

            String key = cacheKeys.unique(storeId);
            dataStore = dataStoreCache.get(key);
            if (dataStore == null) {
                synchronized (key) {
                    dataStore = dataStoreCache.get(key);
                    if (dataStore == null) {
                        dataStore = createDataAccess(info, expandedStore);
                        dataStoreCache.put(key, dataStore);
                    }
                }
            }

            return dataStore;
        } catch (Exception e) {
            // if anything goes wrong we have to clean up the store anyways
            if (dataStore != null) {
                try {
                    dataStore.dispose();
                } catch (Exception ex) {
                    // fine, we had to try
                }
            }
            disableStoreInfoIfNeeded(info, DataStoreInfo.class, e);

            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
                throw (IOException) new IOException().initCause(e);
            }
        }
    }

    private void disableStoreInfoIfNeeded(StoreInfo storeInfo, Class<? extends StoreInfo> clazz, Exception e) {
        if (storeInfo.isEnabled() && storeInfo.isDisableOnConnFailure()) {
            LOGGER.warning(
                    "Auto disable option is set to true. Disabling the store due connection error: " + e.getMessage());
            StoreInfo toDisable = catalog.getStoreByName(storeInfo.getWorkspace(), storeInfo.getName(), clazz);
            toDisable.setEnabled(false);
            catalog.save(toDisable);
        }
    }

    protected DataAccess<? extends FeatureType, ? extends Feature> createDataAccess(
            DataStoreInfo info, DataStoreInfo expandedStore) throws IOException {
        DataAccess<? extends FeatureType, ? extends Feature> dataStore = null;
        // create data store
        Map<String, Serializable> connectionParameters = expandedStore.getConnectionParameters();

        // call this method to execute the hack which recognizes
        // urls which are relative to the data directory
        // TODO: find a better way to do this
        connectionParameters = ResourcePool.getParams(connectionParameters, catalog.getResourceLoader());

        // obtain the factory, either using the "type", or if not found, using the parameters
        DataAccessFactory factory = null;
        try {
            factory = getDataStoreFactory(info);
        } catch (IOException e) {
            // ignoring since the error message is the same as for the null factory, see line below
        }
        if (factory == null) {
            throw new IOException("Failed to find the datastore factory for "
                    + info.getName()
                    + ", did you forget to install the store extension jar?");
        }
        Param[] params = factory.getParametersInfo();

        // ensure that the namespace parameter is set for the datastore
        if (!connectionParameters.containsKey("namespace") && params != null) {
            // if we grabbed the factory, check that the factory actually supports a namespace
            // parameter, if we could not get the factory, assume that it does
            boolean supportsNamespace = false;

            for (Param p : params) {
                if ("namespace".equalsIgnoreCase(p.key)) {
                    supportsNamespace = true;
                    break;
                }
            }

            if (supportsNamespace) {
                WorkspaceInfo ws = info.getWorkspace();
                NamespaceInfo ns = info.getCatalog().getNamespaceByPrefix(ws.getName());
                if (ns == null) {
                    ns = info.getCatalog().getDefaultNamespace();
                }
                if (ns != null) {
                    connectionParameters.put("namespace", ns.getURI());
                }
            }
        }

        // see if the store has a repository param, if so, pass the one wrapping the store
        if (params != null) {
            for (Param p : params) {
                if (Repository.class.equals(p.getType())) {
                    connectionParameters.put(p.getName(), repository);
                }
            }
        }

        // see if the store has a entity resolver param, if so, pass it down
        EntityResolver resolver = getEntityResolver();
        if (resolver != null && params != null) {
            for (Param p : params) {
                if (EntityResolver.class.equals(p.getType())) {
                    if (!(resolver instanceof Serializable)) {
                        resolver = new SerializableEntityResolver(resolver);
                    }
                    connectionParameters.put(p.getName(), (Serializable) resolver);
                }
            }
        }

        // use the factory obtained through the lookup first
        try {
            dataStore = DataStoreUtils.getDataAccess(factory, connectionParameters);
        } catch (IOException e) {
            LOGGER.log(
                    Level.INFO,
                    String.format(
                            "Failed to create the store using the configured factory (%s), will try a generic lookup now.",
                            factory.getClass()),
                    e);
            dataStore = DataStoreUtils.getDataAccess(connectionParameters);
        }

        if (dataStore == null) {
            /*
             * Preserve DataStore retyping behaviour by calling
             * DataAccessFinder.getDataStore after the call to
             * DataStoreUtils.getDataStore above.
             *
             * TODO: DataAccessFinder can also find DataStores, and when retyping is
             * supported for DataAccess, we can use a single mechanism.
             */
            dataStore = DataAccessFinder.getDataStore(connectionParameters);
        }

        if (dataStore == null) {
            throw new NullPointerException("Could not acquire data access '" + info.getName() + "'");
        } else if (info.isDisableOnConnFailure()) {
            // do getNames() to force the store to open a connection and eventually to fail
            // for misconfiguration so that autodisable can then work properly.
            dataStore.getNames();
        }
        return dataStore;
    }

    /**
     * Process connection parameters into a synchronized map.
     *
     * <p>This is used to smooth any relative path kind of issues for any file URLS or directory. This code should be
     * expanded to deal with any other context sensitive issues data stores tend to have.
     *
     * <ul>
     *   <li>key ends in URL, and value is a string
     *   <li>value is a URL
     *   <li>key is directory, and value is a string
     * </ul>
     *
     * @return Processed parameters with relative file URLs resolved
     * @param m a map of data store connection parameters
     * @parm loader
     * @task REVISIT: cache these?
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> getParams(Map<K, V> m, GeoServerResourceLoader loader) {
        Map<K, V> params = Collections.synchronizedMap(new HashMap<>(m));

        final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

        for (Entry<K, V> entry : params.entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();

            if (gsEnvironment != null && GeoServerEnvironment.allowEnvParametrization()) {
                value = gsEnvironment.resolveValue(value);
            }

            // TODO: this code is a pretty big hack, using the name to
            // determine if the key is a url, could be named something else
            // and still be a url
            if ((key != null) && key.matches(".* *url") && value instanceof String) {
                String path = (String) value;

                if (path.startsWith("file:")) {
                    File fixedPath =
                            Resources.find(Resources.fromURL(Files.asResource(loader.getBaseDirectory()), path), true);
                    URL url = URLs.fileToUrl(fixedPath);
                    entry.setValue((V) url.toExternalForm());
                }
            } else if (value instanceof URL && ((URL) value).getProtocol().equals("file")) {
                URL url = (URL) value;
                File fixedPath = Resources.find(
                        Resources.fromURL(Files.asResource(loader.getBaseDirectory()), url.toString()), true);
                entry.setValue((V) URLs.fileToUrl(fixedPath));
            } else if ((key != null)
                    && (key.equals("directory") || key.equals("database") || key.equals("file"))
                    && value instanceof String) {
                String path = (String) value;
                // if a url is used for a directory (for example property store), convert it to path

                if (path.startsWith("file:")) {
                    File fixedPath =
                            Resources.find(Resources.fromURL(Files.asResource(loader.getBaseDirectory()), path), true);
                    entry.setValue((V) fixedPath.toString());
                }
            }
        }
        return params;
    }

    /**
     * Clears the cached resource for a data store.
     *
     * @param info The data store metadata.
     */
    public void clear(DataStoreInfo info) {
        String id = info.getId();
        if (id != null) dataStoreCache.remove(id);
        // the new instance of the store might generate new feature types, clear the cache
        for (FeatureTypeInfo ft : catalog.getFeatureTypesByDataStore(info)) {
            clear(ft);
        }
    }

    public List<AttributeTypeInfo> getAttributes(FeatureTypeInfo info) throws IOException {
        // first check the feature type itself
        //      workaround for GEOS-3294, upgrading from 2.0 data directory,
        //      simply ignore any stored attributes
        //      Also check if the bindings has been set, if it's not set it means we're reloading
        //      an old set of attributes, by forcing them to be reloaded the binding and length
        //      will be added into the info classes
        if (info.getAttributes() != null
                && !info.getAttributes().isEmpty()
                && info.getAttributes().get(0).getBinding() != null) {
            return info.getAttributes();
        }
        // cache attributes only if the id is not null -> the feature type is not new
        if (info.getId() == null) return attributeTypeInfos(info);

        // check the cache
        String key = cacheKeys.unique(info.getId());
        List<AttributeTypeInfo> atts = featureTypeAttributeCache.get(key);
        if (atts == null) {
            synchronized (key) {
                atts = featureTypeAttributeCache.get(key);
                if (atts == null) {
                    atts = attributeTypeInfos(info);
                    featureTypeAttributeCache.put(key, atts);
                }
            }
        }
        return atts;
    }

    private List<AttributeTypeInfo> attributeTypeInfos(FeatureTypeInfo info) throws IOException {
        // load from feature type
        List<AttributeTypeInfo> atts = loadAttributes(info);

        // check for a schema override
        try {
            handleSchemaOverride(atts, info);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error occured applying schema override for " + info.getName(), e);
        }
        return atts;
    }

    public List<AttributeTypeInfo> loadAttributes(FeatureTypeInfo info) throws IOException {
        List<AttributeTypeInfo> attributes = new ArrayList<>();
        FeatureType ft = getFeatureType(info);

        for (PropertyDescriptor pd : ft.getDescriptors()) {
            AttributeTypeInfo att = catalog.getFactory().createAttribute();
            att.setFeatureType(info);
            att.setName(pd.getName().getLocalPart());
            att.setMinOccurs(pd.getMinOccurs());
            att.setMaxOccurs(pd.getMaxOccurs());
            att.setNillable(pd.isNillable());
            att.setBinding(pd.getType().getBinding());
            int length = FeatureTypes.getFieldLength(pd);
            if (length > 0) {
                att.setLength(length);
            }
            attributes.add(att);
        }

        return attributes;
    }

    void handleSchemaOverride(List<AttributeTypeInfo> atts, FeatureTypeInfo ft) throws IOException {
        GeoServerDataDirectory dd = new GeoServerDataDirectory(catalog.getResourceLoader());
        File schemaFile = Resources.file(dd.get(ft, "schema.xsd"));
        if (schemaFile == null) {
            // check for the old style schema.xml
            File oldSchemaFile = Resources.file(dd.get(ft, "schema.xml"));
            if (oldSchemaFile != null) {
                schemaFile = new File(oldSchemaFile.getParentFile(), "schema.xsd");
                try (BufferedWriter out =
                        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(schemaFile)))) {
                    out.write("<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'");
                    out.write(" xmlns:gml='http://www.opengis.net/gml'");
                    out.write(">");
                    try (FileInputStream fis = new FileInputStream(oldSchemaFile)) {
                        IOUtils.copy(fis, out, "UTF-8");
                    }
                    out.write("</xs:schema>");
                    out.flush();
                }
            }
        }

        if (schemaFile != null) {
            // TODO: farm this schema loading stuff to some utility class
            // parse the schema + generate attributes from that
            List<XSDSchemaLocator> locators = Arrays.asList(GML.getInstance().createSchemaLocator());
            XSDSchema schema = null;
            try {
                schema = Schemas.parse(
                        schemaFile.getAbsolutePath(), locators, emptyList(), emptyList(), getEntityResolver());
            } catch (Exception e) {
                LOGGER.warning("Unable to parse "
                        + schemaFile.getAbsolutePath()
                        + "."
                        + " Falling back on native feature type");
            }
            if (schema != null) {
                XSDTypeDefinition type = null;
                for (XSDElementDeclaration element : schema.getElementDeclarations()) {
                    if (ft.getName().equals(element.getName())) {
                        type = element.getTypeDefinition();
                        break;
                    }
                }
                if (type == null) {
                    for (XSDTypeDefinition typedef : schema.getTypeDefinitions()) {
                        if ((ft.getName() + "_Type").equals(typedef.getName())) {
                            type = typedef;
                            break;
                        }
                    }
                }

                if (type != null) {
                    List children = Schemas.getChildElementDeclarations(type, true);
                    for (Iterator<AttributeTypeInfo> i = atts.iterator(); i.hasNext(); ) {
                        AttributeTypeInfo at = i.next();
                        boolean found = false;
                        for (Object child : children) {
                            XSDElementDeclaration ce = (XSDElementDeclaration) child;
                            if (at.getName().equals(ce.getName())) {
                                found = true;
                                if (ce.getContainer() instanceof XSDParticle) {
                                    XSDParticle part = (XSDParticle) ce.getContainer();
                                    at.setMinOccurs(part.getMinOccurs());
                                    at.setMaxOccurs(part.getMaxOccurs());
                                }
                                break;
                            }
                        }

                        if (!found) {
                            i.remove();
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the underlying resource for a feature type, caching the result.
     *
     * <p>In the event that the resource is not in the cache the associated data store resource is loaded, and the
     * feature type resource obtained. During loading the underlying feature type resource is "wrapped" to take into
     * account feature type name aliasing and reprojection.
     *
     * @param info The feature type metadata.
     * @throws IOException Any errors that occure while loading the resource.
     */
    public FeatureType getFeatureType(FeatureTypeInfo info) throws IOException {
        return getFeatureType(info, true);
    }

    FeatureType getFeatureType(FeatureTypeInfo info, boolean handleProjectionPolicy) throws IOException {
        try {
            return tryGetFeatureType(info, handleProjectionPolicy);
        } catch (Exception ex) {
            LOGGER.log(
                    Level.WARNING,
                    "Error while getting feature type, flushing cache and retrying: {0}",
                    ex.getMessage());
            LOGGER.log(Level.FINE, "", ex);
            this.clear(info);
            this.flushDataStore(info);
            return tryGetFeatureType(info, handleProjectionPolicy);
        }
    }

    FeatureType tryGetFeatureType(FeatureTypeInfo info, boolean handleProjectionPolicy) throws IOException {
        boolean cacheable = isCacheable(info);
        return cacheable
                ? getCacheableFeatureType(info, handleProjectionPolicy)
                : getNonCacheableFeatureType(info, handleProjectionPolicy);
    }

    FeatureType getCacheableFeatureType(FeatureTypeInfo info, boolean handleProjectionPolicy) throws IOException {

        String id = info.getId();
        if (id == null) return acquireFeatureType(info, handleProjectionPolicy);

        id = getFeatureTypeInfoKey(info, handleProjectionPolicy);
        String key = cacheKeys.unique(id);
        FeatureType ft = featureTypeCache.get(key);
        if (ft == null) {
            synchronized (key) {
                ft = featureTypeCache.get(key);
                if (ft == null) {
                    ft = acquireFeatureType(info, handleProjectionPolicy);
                    featureTypeCache.put(key, ft);
                }
            }
        }
        return ft;
    }

    private FeatureType acquireFeatureType(FeatureTypeInfo info, boolean handleProjectionPolicy) throws IOException {
        // grab the underlying feature type
        DataAccess<? extends FeatureType, ? extends Feature> dataAccess = getDataStore(info.getStore());
        FeatureTypeCallback initializer = getFeatureTypeInitializer(info, dataAccess);
        if (initializer != null) {
            initializer.initialize(info, dataAccess, null);
        }
        // ft = jstore.getSchema(vt.getName());
        FeatureType ft = dataAccess.getSchema(info.getQualifiedNativeName());
        ft = buildFeatureType(info, handleProjectionPolicy, ft);
        return ft;
    }

    private FeatureType getNonCacheableFeatureType(FeatureTypeInfo info, boolean handleProjectionPolicy)
            throws IOException {
        FeatureType ft = null;

        // grab the underlying feature type
        DataAccess<? extends FeatureType, ? extends Feature> dataAccess = getDataStore(info.getStore());

        FeatureTypeCallback initializer = getFeatureTypeInitializer(info, dataAccess);
        Name temporaryName = null;
        if (initializer != null) {
            temporaryName = getTemporaryName(info, dataAccess, initializer);
        }
        ft = dataAccess.getSchema(temporaryName != null ? temporaryName : info.getQualifiedNativeName());
        ft = buildFeatureType(info, handleProjectionPolicy, ft);

        // Remove layer configuration from datastore
        if (initializer != null && temporaryName != null) {
            initializer.dispose(info, dataAccess, temporaryName);
        }

        return ft;
    }

    /**
     * Builds a temporary name for a feature type making sure there is no conflict with other existing type names in the
     * store
     */
    protected Name getTemporaryName(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            FeatureTypeCallback initializer)
            throws IOException {
        Name temporaryName;
        // use a highly random name, we don't want to actually add the
        // virtual table to the store as this feature type is not cacheable,
        // it is "dirty" or un-saved. The renaming below will take care
        // of making the user see the actual name
        // NT 14/8/2012: Removed synchronization on jstore as it blocked query
        // execution and risk of UUID clash is considered acceptable.

        List<Name> typeNames = dataAccess.getNames();
        String nsURI = null;
        if (!typeNames.isEmpty()) {
            nsURI = typeNames.get(0).getNamespaceURI();
        }
        do {
            String name = UUID.randomUUID().toString();
            temporaryName = new NameImpl(nsURI, name);
        } while (Arrays.asList(typeNames).contains(temporaryName));
        if (!initializer.initialize(info, dataAccess, temporaryName)) {
            temporaryName = null;
        }
        return temporaryName;
    }

    /**
     * Looks up a FetureTypeInitializer for this FeatureTypeInfo and DataAccess. FeatureTypeInitializer are used to init
     * and dispose configured feature types (as opposed to ones that natively originate from the source)
     */
    FeatureTypeCallback getFeatureTypeInitializer(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess) {
        List<FeatureTypeCallback> featureTypeInitializers = GeoServerExtensions.extensions(FeatureTypeCallback.class);
        FeatureTypeCallback initializer = null;
        for (FeatureTypeCallback fti : featureTypeInitializers) {
            if (fti.canHandle(info, dataAccess)) {
                initializer = fti;
            }
        }
        return initializer;
    }

    private FeatureType buildFeatureType(FeatureTypeInfo info, boolean handleProjectionPolicy, FeatureType ft)
            throws IOException {
        // TODO: support reprojection for non-simple FeatureType
        if (ft instanceof SimpleFeatureType) {
            // configured attribute customization, execute before projection handling and callbacks
            if (info.getAttributes() != null && !info.getAttributes().isEmpty())
                ft = transformer.retypeFeatureType(info, ft);

            SimpleFeatureType sft = (SimpleFeatureType) ft;
            // create the feature type so it lines up with the "declared" schema
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.init(sft);
            // Handle any aliases defined in info
            tb.setName(info.getName());
            tb.setNamespaceURI(info.getNamespace().getURI());

            // Handle the attributes manually
            tb.setAttributes((AttributeDescriptor[]) null);
            // take this to mean just load all native with projection handling,
            // feature type customization happens later
            for (PropertyDescriptor pd : ft.getDescriptors()) {
                if (!(pd instanceof AttributeDescriptor)) {
                    continue;
                }

                AttributeDescriptor ad = (AttributeDescriptor) pd;
                if (handleProjectionPolicy) {
                    ad = handleDescriptor(ad, info);
                }
                tb.add(ad);
            }
            ft = tb.buildFeatureType();

            // extension point for retyping the feature type
            for (RetypeFeatureTypeCallback callback : GeoServerExtensions.extensions(RetypeFeatureTypeCallback.class)) {
                ft = callback.retypeFeatureType(info, ft);
            }
        } // end special case for SimpleFeatureType

        return ft;
    }

    private String getFeatureTypeInfoKey(FeatureTypeInfo info, boolean handleProjectionPolicy) {
        return info.getId() + PROJECTION_POLICY_SEPARATOR + handleProjectionPolicy;
    }

    /**
     * Returns true if this object is saved in the catalog and not a modified proxy. We don't want to cache the result
     * of computations made against a dirty object, nor the ones made against an object that still haven't been saved
     */
    boolean isCacheable(CatalogInfo info) {
        // saved?
        if (info.getId() == null) {
            return false;
        }

        // dirty?
        if (Proxy.isProxyClass(info.getClass())) {
            Object invocationHandler = Proxy.getInvocationHandler(info);
            if (invocationHandler instanceof ModificationProxy && ((ModificationProxy) invocationHandler).isDirty()) {
                return false;
            }
        }

        return true;
    }

    /*
     * Helper method which overrides geometric attributes based on the reprojection policy.
     */
    AttributeDescriptor handleDescriptor(AttributeDescriptor ad, FeatureTypeInfo info) {

        // force the user specified CRS if the data has no CRS, or reproject it
        // if necessary
        if (ad instanceof GeometryDescriptor) {
            GeometryDescriptor old = (GeometryDescriptor) ad;
            try {
                // if old has no crs, change the projection handlign policy
                // to be the declared
                boolean rebuild = false;

                if (old.getCoordinateReferenceSystem() == null) {
                    // (JD) TODO: this is kind of wierd... we should at least
                    // log something here, and this is not thread safe!!
                    if (info.getProjectionPolicy() != ProjectionPolicy.FORCE_DECLARED) {
                        // modify the actual type info if possible, not the modification
                        // proxy around it
                        if (Proxy.isProxyClass(info.getClass())) {
                            FeatureTypeInfo inner = ModificationProxy.unwrap(info);
                            inner.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
                        } else {
                            info.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
                        }
                    }
                    rebuild = true;
                } else {
                    ProjectionPolicy projPolicy = info.getProjectionPolicy();
                    if (projPolicy == ProjectionPolicy.REPROJECT_TO_DECLARED
                            || projPolicy == ProjectionPolicy.FORCE_DECLARED) {
                        rebuild = true;
                    }
                }

                if (rebuild) {
                    // rebuild with proper crs
                    AttributeTypeBuilder b = new AttributeTypeBuilder();
                    b.init(old);
                    b.setCRS(getCRS(info.getSRS()));
                    ad = b.buildDescriptor(old.getLocalName());
                }
            } catch (Exception e) {
                // log exception
            }
        }

        return ad;
    }

    /**
     * Loads an attribute descriptor from feature type and attribute type metadata.
     *
     * <p>This method returns null if the attribute descriptor could not be loaded.
     */
    public AttributeDescriptor getAttributeDescriptor(FeatureTypeInfo ftInfo, AttributeTypeInfo atInfo)
            throws Exception {

        FeatureType featureType = getFeatureType(ftInfo);
        if (featureType != null) {
            for (PropertyDescriptor pd : featureType.getDescriptors()) {
                if (pd instanceof AttributeDescriptor) {
                    AttributeDescriptor ad = (AttributeDescriptor) pd;
                    if (atInfo.getName().equals(ad.getLocalName())) {
                        return ad;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Clears a feature type resource from the cache.
     *
     * @param info The feature type metadata.
     */
    public void clear(FeatureTypeInfo info) {
        String id = info.getId();
        if (id != null) {
            String id2 = getFeatureTypeInfoKey(info, true);
            String id3 = getFeatureTypeInfoKey(info, false);
            featureTypeCache.remove(id2);
            featureTypeCache.remove(id3);
            featureTypeAttributeCache.remove(id);
            flushDataStore(info);
        }
    }

    /**
     * Clears a coverage resource from cache. In the current implementation, it resets the underlying reader, as it's
     * the only bit that's actually cached, to allow discovering new information potentially available in the source
     * (the reader caches information such as the bounds, native CRS and the image structure, which affect the coverage
     * itself).
     */
    public void clear(CoverageInfo info) {
        String id = info.getId();
        if (id != null && info.getStore() != null) {
            String storeId = info.getStore().getId();
            coverageCacheKeys.stream()
                    .filter(k -> storeId.equals(k.id))
                    .forEach(k -> hintCoverageReaderCache.remove(k));
        }
    }

    /**
     * Loads the feature source for a feature type.
     *
     * <p>The <tt>hints</tt> parameter is used to control how the feature source is loaded. An example is using the
     * {@link #REPROJECT} hint to control if the resulting feature source is reprojected or not.
     *
     * @param info The feature type info.
     * @param hints Any hints to take into account while loading the feature source, may be <code>
     *     null</code>.
     * @throws IOException Any errors that occur while loading the feature source.
     */
    public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource(FeatureTypeInfo info, Hints hints)
            throws IOException {
        DataAccess<? extends FeatureType, ? extends Feature> dataAccess = getDataStore(info.getStore());

        // TODO: support aliasing (renaming), reprojection, and locking for DataAccess
        if (!(dataAccess instanceof DataStore)) {
            return getFeatureSource(dataAccess, info);
        }

        DataStore dataStore = (DataStore) dataAccess;

        // sql view handling
        FeatureTypeCallback initializer = getFeatureTypeInitializer(info, dataAccess);
        if (initializer != null) {
            initializer.initialize(info, dataAccess, null);
        }

        //
        // aliasing and type mapping
        //
        final String nativeName = info.getNativeName();
        final String alias = info.getName();
        final SimpleFeatureType targetFeatureType = (SimpleFeatureType) getFeatureType(info, false);
        SimpleFeatureSource fs = dataStore.getFeatureSource(nativeName);

        // if feature type customization is there, apply it first
        if (info.getAttributes() != null
                && !info.getAttributes().isEmpty()
                && !info.getFeatureType().equals(fs.getSchema())) {
            fs = (SimpleFeatureSource) transformer.wrapFeatureSource(info, fs);
        }
        // then check name, and other customizations (e.g., projection policy)
        if (!nativeName.equals(alias) || DataUtilities.compare(fs.getSchema(), targetFeatureType) != 0) {
            // rename and retype as necessary
            fs = RetypingFeatureSource.getRetypingSource(fs, targetFeatureType);
        }

        //
        // reprojection
        //
        Boolean reproject = Boolean.TRUE;
        if (hints != null) {
            if (hints.get(REPROJECT) != null) {
                reproject = (Boolean) hints.get(REPROJECT);
            }
        }

        // get the reprojection policy
        ProjectionPolicy ppolicy = info.getProjectionPolicy();

        // if projection policy says to reproject, but calling code specified hint
        // not to, respect hint
        if (ppolicy == ProjectionPolicy.REPROJECT_TO_DECLARED && !reproject) {
            ppolicy = ProjectionPolicy.NONE;
        }

        List<AttributeTypeInfo> attributes = info.attributes();
        if (attributes == null || attributes.isEmpty()) {
            return fs;
        } else {
            CoordinateReferenceSystem resultCRS = null;
            GeometryDescriptor gd = fs.getSchema().getGeometryDescriptor();
            CoordinateReferenceSystem nativeCRS = gd != null ? gd.getCoordinateReferenceSystem() : null;

            if (ppolicy == ProjectionPolicy.NONE
                    && info.getNativeCRS() != null
                    && info.getMetadata().get(FeatureTypeInfo.OTHER_SRS) != null) resultCRS = info.getNativeCRS();
            else if (ppolicy == ProjectionPolicy.NONE && nativeCRS != null) {
                resultCRS = nativeCRS;
            } else {
                resultCRS = getCRS(info.getSRS());
                // force remoting re-projection incase of WFS-NG only
                if (hints != null)
                    if (hints.get(ResourcePool.MAP_CRS) != null
                            && info.getMetadata().get(FeatureTypeInfo.OTHER_SRS) != null)
                        resultCRS = (CoordinateReferenceSystem) hints.get(ResourcePool.MAP_CRS);
            }

            // make sure we create the appropriate schema, with the right crs
            // we checked above we are using DataStore/SimpleFeature/SimpleFeatureType (DSSFSFT)
            SimpleFeatureType schema = (SimpleFeatureType) getFeatureType(info);
            try {
                if (!CRS.equalsIgnoreMetadata(resultCRS, schema.getCoordinateReferenceSystem()))
                    schema = FeatureTypes.transform(schema, resultCRS);
            } catch (Exception e) {
                throw new DataSourceException("Problem forcing CRS onto feature type", e);
            }

            // joining, check for join hint which requires us to create a shcema with some
            // additional
            // attributes
            if (hints != null && hints.containsKey(JOINS)) {
                @SuppressWarnings("unchecked")
                List<Join> joins = (List<Join>) hints.get(JOINS);
                SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
                typeBuilder.init(schema);

                for (Join j : joins) {
                    String attName = j.getAlias() != null ? j.getAlias() : j.getTypeName();
                    typeBuilder.add(attName, SimpleFeature.class);
                }
                schema = typeBuilder.buildFeatureType();
            }

            // applying wrappers using implementations of RetypeFeatureTypeCallback
            for (RetypeFeatureTypeCallback callback : GeoServerExtensions.extensions(RetypeFeatureTypeCallback.class)) {
                if (SimpleFeatureSource.class.isAssignableFrom(fs.getClass()))
                    fs = (SimpleFeatureSource) callback.wrapFeatureSource(info, fs);
            }

            // return a normal
            return GeoServerFeatureLocking.create(
                    fs,
                    new GeoServerFeatureSource.Settings(
                            schema,
                            info.filter(),
                            resultCRS,
                            info.getProjectionPolicy().getCode(),
                            getTolerance(info),
                            info.getMetadata()));
        }
    }

    /**
     * Helper method that will search for the feature source corresponding to the provided feature type info on the
     * provided data access. We will first search based on the published name (layer name) and only if this search fails
     * we will search based on the native name.
     */
    @SuppressWarnings("unchecked")
    private FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource(
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess, FeatureTypeInfo info) throws IOException {
        FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
        try {
            // first try to search based on the published name, to avoid any unexpected aliasing
            featureSource = dataAccess.getFeatureSource(info.getQualifiedName());
        } catch (Exception exception) {
            LOGGER.log(
                    Level.FINE,
                    String.format("Error retrieving feature type using published name '%s'.", info.getQualifiedName()));
            // let's try now to search based on the native name
            featureSource = dataAccess.getFeatureSource(info.getQualifiedNativeName());
        }
        // return a decorated feature source, capable of handling the layer definition default CQL
        // filter
        return new GeoserverComplexFeatureSource((FeatureSource<FeatureType, Feature>) featureSource, info);
    }

    private Double getTolerance(FeatureTypeInfo info) {
        // if no curved geometries, do not bother computing a tolerance
        if (!info.isCircularArcPresent()) {
            return null;
        }

        // get the measure, if null, no linearization tolerance is available
        Measure mt = info.getLinearizationTolerance();
        if (mt == null) {
            return null;
        }

        // if the user did not specify a unit of measure, we use it as an absolute value
        if (mt.getUnit() == null) {
            return mt.doubleValue();
        }

        // should not happen, but let's cover all our bases
        CoordinateReferenceSystem crs = info.getCRS();
        if (crs == null) {
            return mt.doubleValue();
        }

        // let's get the target unit
        SingleCRS horizontalCRS = CRS.getHorizontalCRS(crs);
        Unit<?> targetUnit;
        if (horizontalCRS != null) {
            // leap of faith, the first axis is an horizontal one (
            targetUnit = getFirstAxisUnit(horizontalCRS.getCoordinateSystem());
        } else {
            // leap of faith, the first axis is an horizontal one (
            targetUnit = getFirstAxisUnit(crs.getCoordinateSystem());
        }

        if ((targetUnit != null && targetUnit == NonSI.DEGREE_ANGLE)
                || horizontalCRS instanceof GeographicCRS
                || crs instanceof GeographicCRS) {
            // assume we're working against a type of geographic crs, must estimate the degrees
            // equivalent
            // to the measure, we are going to use a very rough estimate (cylindrical earth model)
            // TODO: maybe look at the layer bbox and get a better estimate computed at the center
            // of the bbox
            UnitConverter converter = mt.getUnit().asType(Length.class).getConverterTo(SI.METRE);
            double tolMeters = converter.convert(mt.doubleValue());
            return tolMeters * OGC_METERS_TO_DEGREES;
        } else if (targetUnit != null && targetUnit.isCompatible(SI.METRE)) {
            // ok, we assume the target is not a geographic one, but we might
            // have to convert between meters and feet maybe
            @SuppressWarnings("unchecked") // cannot convert between two Unit<?>....
            UnitConverter converter = mt.getUnit().getConverterTo((Unit) targetUnit);
            return converter.convert(mt.doubleValue());
        } else {
            return mt.doubleValue();
        }
    }

    private Unit<?> getFirstAxisUnit(CoordinateSystem coordinateSystem) {
        if (coordinateSystem == null || coordinateSystem.getDimension() > 0) {
            return null;
        }
        return coordinateSystem.getAxis(0).getUnit();
    }

    public GridCoverageReader getGridCoverageReader(CoverageInfo info, Hints hints) throws IOException {
        return getGridCoverageReader(info, null, hints);
    }

    public GridCoverageReader getGridCoverageReader(CoverageInfo info, String coverageName, Hints hints)
            throws IOException {
        return getGridCoverageReader(info.getStore(), info, coverageName, hints);
    }

    /**
     * Returns a coverage reader, caching the result.
     *
     * @param info The coverage metadata.
     * @param hints Hints to use when loading the coverage, may be <code>null</code>.
     * @throws IOException Any errors that occur loading the reader.
     */
    public GridCoverageReader getGridCoverageReader(CoverageStoreInfo info, Hints hints) throws IOException {
        return getGridCoverageReader(info, null, hints);
    }

    /**
     * Returns a coverage reader, caching the result.
     *
     * @param storeInfo The coverage metadata.
     * @param hints Hints to use when loading the coverage, may be <code>null</code>.
     * @throws IOException Any errors that occur loading the reader.
     */
    public GridCoverageReader getGridCoverageReader(CoverageStoreInfo storeInfo, String coverageName, Hints hints)
            throws IOException {
        return getGridCoverageReader(storeInfo, null, coverageName, hints);
    }

    /**
     * Returns a coverage reader, caching the result.
     *
     * @param info The coverage metadata.
     * @param hints Hints to use when loading the coverage, may be <code>null</code>.
     * @throws IOException Any errors that occur loading the reader.
     */
    private GridCoverageReader getGridCoverageReader(
            CoverageStoreInfo info, CoverageInfo coverageInfo, String coverageName, Hints hints) throws IOException {

        CoverageStoreInfo expandedStore = clone(info, true);

        final AbstractGridFormat gridFormat = info.getFormat();
        if (gridFormat == null) {
            IOException e = new IOException("Could not find the raster plugin for format " + info.getType());
            disableStoreInfoIfNeeded(info, CoverageStoreInfo.class, e);
            throw e;
        }

        // we are going to add the repository anyways, but we don't want to modify the original
        // hints
        // and need to ensure they are not null
        if (hints != null) {
            hints = new Hints(hints);
        } else {
            hints = new Hints();
        }
        hints.add(new RenderingHints(Hints.REPOSITORY, repository));
        if (coverageExecutor != null) {
            hints.add(new RenderingHints(Hints.EXECUTOR_SERVICE, coverageExecutor));
        }
        // look into the cache
        CoverageHintReaderKey key = new CoverageHintReaderKey(info.getId(), hints);
        key = coverageCacheKeys.unique(key);
        GridCoverageReader reader = hintCoverageReaderCache.get(key);

        // if not found in cache, create it
        if (reader == null) {
            synchronized (key) {
                if (key != null) {
                    reader = hintCoverageReaderCache.get(key);
                }
                if (reader == null) {
                    try {
                        /////////////////////////////////////////////////////////
                        //
                        // Getting coverage reader using the format and the real path.
                        //
                        // /////////////////////////////////////////////////////////
                        final String urlString = expandedStore.getURL();
                        Object readObject = getCoverageStoreSource(urlString, coverageInfo, expandedStore, hints);

                        // readers might change the provided hints, pass down a defensive copy
                        reader = gridFormat.getReader(readObject, hints);
                        if (reader == null) {
                            throw new IOException("Failed to create reader from " + urlString + " and hints " + hints);
                        }
                        if (key != null) {
                            hintCoverageReaderCache.put(key, reader);
                        }
                    } catch (Exception e) {
                        disableStoreInfoIfNeeded(info, CoverageStoreInfo.class, e);
                        throw e;
                    }
                }
            }
        }

        if (coverageInfo == null && coverageName != null) {
            coverageInfo = getCoverageInfo(coverageName, info);
        }

        if (coverageInfo != null) {
            MetadataMap metadata = coverageInfo.getMetadata();
            if (metadata != null && metadata.containsKey(CoverageView.COVERAGE_VIEW)) {
                CoverageView coverageView = (CoverageView) metadata.get(CoverageView.COVERAGE_VIEW);
                reader = CoverageViewReader.wrap((GridCoverage2DReader) reader, coverageView, coverageInfo, hints);
            }
        }

        // wrap it if we are dealing with a multi-coverage reader
        if (coverageName != null) {
            // force the result to work against a single coverage, so that the OGC service portion
            // of
            // GeoServer does not need to be updated to the multicoverage stuff
            // (we might want to introduce a hint later for code that really wants to get the
            // multi-coverage reader)

            if (reader.getFormat() instanceof GeoTiffFormat) { //  GEOS-9236
                if ("geotiff_coverage".equalsIgnoreCase(coverageInfo.getNativeCoverageName())) {
                    coverageInfo.setNativeCoverageName(reader.getGridCoverageNames()[0]);
                }
            }

            return CoverageDimensionCustomizerReader.wrap((GridCoverage2DReader) reader, coverageName, coverageInfo);
        } else {
            // In order to deal with Bands customization, we need to get a CoverageInfo.
            // Therefore we won't wrap the reader into a CoverageDimensionCustomizerReader in case
            // we don't have the coverageName and the underlying reader has more than 1 coverage.
            // Indeed, there are cases (as during first initialization) where a multi-coverage
            // reader is requested
            // to the resourcePool without specifying the coverageName: No way to get the proper
            // coverageInfo in
            // that case so returning the simple reader.
            final int numCoverages = reader.getGridCoverageCount();
            if (numCoverages == 1) {
                return CoverageDimensionCustomizerReader.wrap((GridCoverage2DReader) reader, null, coverageInfo);
            }
            // Avoid dimensions wrapping since we have a multi-coverage reader
            // but no coveragename have been specified
            return reader;
        }
    }

    /**
     * Attempted to convert the URL-ish string to a parseable input object for coverage reading purposes, otherwise just
     * returns the string itself
     *
     * @param urlString the url string to parse, which may actually be a path
     * @return an object appropriate for passing to a grid coverage reader
     */
    public static Object getCoverageStoreSource(
            String urlString, CoverageInfo coverageInfo, CoverageStoreInfo coverageStoreInfo, Hints hints) {
        List<CoverageReaderInputObjectConverter> converters =
                GeoServerExtensions.extensions(CoverageReaderInputObjectConverter.class);

        for (CoverageReaderInputObjectConverter converter : converters) {
            Optional<?> convertedValue = converter.convert(urlString, coverageInfo, coverageStoreInfo, hints);
            if (convertedValue.isPresent()) {
                return convertedValue.get();
            }
        }

        return urlString;
    }

    /** Clears any cached readers for the coverage. */
    public void clear(CoverageStoreInfo info) {
        String storeId = info.getId();
        HashSet<CoverageHintReaderKey> keys = new HashSet<>(hintCoverageReaderCache.keySet());
        for (CoverageHintReaderKey key : keys) {
            if (key.id != null && key.id.equals(storeId)) {
                hintCoverageReaderCache.remove(key);
            }
        }
    }

    public GridCoverage getGridCoverage(CoverageInfo info, ReferencedEnvelope env, Hints hints) throws IOException {
        return getGridCoverage(info, (String) null, env, hints);
    }

    /**
     * Loads a grid coverage.
     *
     * <p>
     *
     * @param info The grid coverage metadata.
     * @param coverageName The grid coverage to load
     * @param env The section of the coverage to load.
     * @param hints Hints to use while loading the coverage.
     * @throws IOException Any errors that occur loading the coverage.
     */
    public GridCoverage getGridCoverage(CoverageInfo info, String coverageName, ReferencedEnvelope env, Hints hints)
            throws IOException {
        final GridCoverageReader reader = getGridCoverageReader(info, coverageName, hints);
        if (reader == null) {
            return null;
        }

        return getGridCoverage(info, reader, env, hints);
    }

    /**
     * Loads a grid coverage.
     *
     * <p>
     *
     * @param info The grid coverage metadata.
     * @param env The section of the coverage to load.
     * @param hints Hints to use while loading the coverage.
     * @throws IOException Any errors that occur loading the coverage.
     */
    public GridCoverage getGridCoverage(
            CoverageInfo info, GridCoverageReader reader, ReferencedEnvelope env, Hints hints) throws IOException {

        ReferencedEnvelope coverageBounds;
        try {
            coverageBounds = info.boundingBox();
        } catch (Exception e) {
            throw (IOException) new IOException("unable to calculate coverage bounds").initCause(e);
        }

        GeneralBounds envelope = null;
        if (env == null) {
            envelope = new GeneralBounds(coverageBounds);
        } else {
            envelope = new GeneralBounds(env);
        }

        // /////////////////////////////////////////////////////////
        //
        // Do we need to proceed?
        // I need to check the requested envelope in order to see if the
        // coverage we ask intersect it otherwise it is pointless to load it
        // since its reader might return null;
        // /////////////////////////////////////////////////////////
        final CoordinateReferenceSystem sourceCRS = envelope.getCoordinateReferenceSystem();
        CoordinateReferenceSystem destCRS;
        try {
            destCRS = info.getCRS();
        } catch (Exception e) {
            final IOException ioe = new IOException("unable to determine coverage crs");
            ioe.initCause(e);
            throw ioe;
        }

        if (!CRS.equalsIgnoreMetadata(sourceCRS, destCRS)) {
            try {
                envelope = CRS.transform(envelope, destCRS);
            } catch (TransformException e) {
                throw (IOException) new IOException("error occured transforming envelope").initCause(e);
            }
        }

        // just do the intersection since
        envelope.intersect(coverageBounds);

        if (envelope.isEmpty()) {
            return null;
        }

        envelope.setCoordinateReferenceSystem(destCRS);

        // /////////////////////////////////////////////////////////
        //
        // Reading the coverage
        //
        // /////////////////////////////////////////////////////////

        GridCoverage gc =
                reader.read(CoverageUtils.getParameters(reader.getFormat().getReadParameters(), info.getParameters()));

        if ((gc == null) || !(gc instanceof GridCoverage2D)) {
            throw new IOException("The requested coverage could not be found.");
        }

        return gc;
    }

    /**
     * Returns the format for a coverage.
     *
     * <p>The format is inferred from {@link CoverageStoreInfo#getType()}
     *
     * @param info The coverage metadata.
     * @return The format, or null.
     */
    public AbstractGridFormat getGridCoverageFormat(CoverageStoreInfo info) {
        final int length = CoverageStoreUtils.formats.length;

        for (int i = 0; i < length; i++) {
            if (CoverageStoreUtils.formats[i].getName().equals(info.getType())) {
                return (AbstractGridFormat) CoverageStoreUtils.formats[i];
            }
        }

        return null;
    }

    /**
     * Returns the {@link WebMapServer} for a {@link WMSStoreInfo} object
     *
     * @param info The WMS configuration
     */
    public WebMapServer getWebMapServer(WMSStoreInfo info) throws IOException {

        WMSStoreInfo expandedStore = clone(info, true);

        try {
            EntityResolver entityResolver = getEntityResolver();

            String id = info.getId();
            if (id == null) {
                return createWebMapServer(expandedStore, entityResolver);
            }
            id = cacheKeys.unique(id);
            WebMapServer wms = wmsCache.get(id);
            // if we have a hit but the resolver has been changed, clean and build again
            if (wms != null
                    && wms.getHints() != null
                    && !Objects.equals(wms.getHints().get(XMLHandlerHints.ENTITY_RESOLVER), entityResolver)) {
                wmsCache.remove(id);
                wms = null;
            }
            if (wms == null) {
                synchronized (id) {
                    wms = wmsCache.get(id);
                    if (wms == null) {
                        wms = createWebMapServer(expandedStore, entityResolver);
                        wmsCache.put(id, wms);
                    }
                }
            }

            return wms;
        } catch (IOException ioe) {
            disableStoreInfoIfNeeded(info, WMSStoreInfo.class, ioe);
            throw ioe;
        } catch (Exception e) {
            disableStoreInfoIfNeeded(info, WMSStoreInfo.class, e);
            throw (IOException) new IOException().initCause(e);
        }
    }

    private WebMapServer createWebMapServer(WMSStoreInfo expandedStore, EntityResolver entityResolver)
            throws IOException, org.geotools.ows.ServiceException {
        HTTPClient client = getHTTPClient(expandedStore);
        URL serverURL = new URL(expandedStore.getCapabilitiesURL());
        Map<String, Object> hints = new HashMap<>();
        hints.put(DocumentHandler.DEFAULT_NAMESPACE_HINT_KEY, WMSSchema.getInstance());
        hints.put(DocumentFactory.VALIDATION_HINT, Boolean.FALSE);
        if (entityResolver != null) {
            hints.put(XMLHandlerHints.ENTITY_RESOLVER, entityResolver);
        }
        WebMapServer wms;
        if (StringUtils.isNotEmpty(expandedStore.getHeaderName())
                && StringUtils.isNotEmpty(expandedStore.getHeaderValue())) {
            wms = new WebMapServer(
                    serverURL,
                    client,
                    hints,
                    Collections.singletonMap(expandedStore.getHeaderName(), expandedStore.getHeaderValue()));
        } else {
            wms = new WebMapServer(serverURL, client, hints);
        }
        return wms;
    }

    /**
     * Returns the {@link WebMapTileServer} for a {@link WMTSStoreInfo} object
     *
     * @param info The WMTS configuration
     */
    public WebMapTileServer getWebMapTileServer(WMTSStoreInfo info) throws IOException {

        try {
            EntityResolver entityResolver = getEntityResolver();
            String id = info.getId();
            if (id == null) {
                return createWebMapTileServer(info, entityResolver);
            }
            id = cacheKeys.unique(info.getId());
            WebMapTileServer wmts = wmtsCache.get(id);
            // if we have a hit but the resolver has been changed, clean and build again
            if (wmts != null
                    && wmts.getHints() != null
                    && !Objects.equals(wmts.getHints().get(XMLHandlerHints.ENTITY_RESOLVER), entityResolver)) {
                wmtsCache.remove(id);
                wmts = null;
            }
            if (wmts == null) {
                synchronized (id) {
                    wmts = wmtsCache.get(id);
                    if (wmts == null) {
                        wmts = createWebMapTileServer(info, entityResolver);
                        wmtsCache.put(id, wmts);
                    }
                }
            }

            return wmts;
        } catch (IOException ioe) {
            disableStoreInfoIfNeeded(info, WMTSStoreInfo.class, ioe);
            throw ioe;
        } catch (Exception e) {
            disableStoreInfoIfNeeded(info, WMTSStoreInfo.class, e);
            throw new IOException(e);
        }
    }

    private WebMapTileServer createWebMapTileServer(WMTSStoreInfo info, EntityResolver resolver)
            throws IOException, org.geotools.ows.ServiceException {
        WMTSStoreInfo expandedStore = clone(info, true);

        HTTPClient client = getHTTPClient(expandedStore);
        URL serverURL = new URL(expandedStore.getCapabilitiesURL());
        WebMapTileServer wmts;
        if (StringUtils.isNotEmpty(info.getHeaderName()) && StringUtils.isNotEmpty(info.getHeaderValue())) {
            wmts = new WebMapTileServer(
                    serverURL, client, Collections.singletonMap(info.getHeaderName(), info.getHeaderValue()));
        } else {
            wmts = new WebMapTileServer(serverURL, client);
        }
        return wmts;
    }

    /** Returns the entity resolver from the {@link EntityResolverProvider}, or null if none is configured */
    public EntityResolver getEntityResolver() {
        EntityResolver entityResolver = null;
        if (entityResolverProvider != null) {
            entityResolver = entityResolverProvider.getEntityResolver();
        }
        return entityResolver;
    }

    private HTTPClient getHTTPClient(HTTPStoreInfo info) {
        String capabilitiesURL = info.getCapabilitiesURL();

        // check for mock bindings. Since we are going to run this code in production as well,
        // guard it so that it only triggers if the MockHttpClientProvider has any active binding
        if (TestHttpClientProvider.isTestModeEnabled()
                && capabilitiesURL.startsWith(TestHttpClientProvider.MOCKSERVER)) {
            HTTPClient client = TestHttpClientProvider.get(capabilitiesURL);
            return client;
        }

        HTTPClient client;
        if (info.isUseConnectionPooling()) {
            client = HTTPClientFinder.createClient(HTTPConnectionPooling.class);
            if (info.getMaxConnections() > 0 && client instanceof HTTPConnectionPooling) {
                int maxConnections = info.getMaxConnections();
                @SuppressWarnings("PMD.CloseResource") // wrapped and returned
                HTTPConnectionPooling mtClient = (HTTPConnectionPooling) client;
                mtClient.setMaxConnections(maxConnections);
            }
        } else {
            client = HTTPClientFinder.createClient(new Hints(Hints.HTTP_CLIENT, SimpleHttpClient.class));
        }
        String username = info.getUsername();
        String password = info.getPassword();
        String authKey = info.getAuthKey();
        int connectTimeout = info.getConnectTimeout();
        int readTimeout = info.getReadTimeout();
        client.setUser(username);
        client.setPassword(password);
        if (authKey != null) {
            String[] kv = authKey.split("=");
            if (kv.length == 2) {
                client.setExtraParams(Map.of(kv[0], kv[1]));
            }
        }
        client.setConnectTimeout(connectTimeout);
        client.setReadTimeout(readTimeout);

        return client;
    }

    /** Locates and returns a WMS {@link Layer} based on the configuration stored in WMSLayerInfo */
    public Layer getWMSLayer(WMSLayerInfo info) throws IOException {
        // check which actual name we have to use
        String name = info.getName();
        if (info.getNativeName() != null) {
            name = info.getNativeName();
        }

        WMSCapabilities caps = info.getStore().getWebMapServer(null).getCapabilities();
        for (Layer layer : caps.getLayerList()) {
            if (layer != null && name.equals(layer.getName())) {
                return layer;
            }
        }

        throw new IOException("Could not find layer " + info.getName() + " in the server capabilitiles document");
    }

    /** Locates and returns a WTMS {@link Layer} based on the configuration stored in WMTSLayerInfo */
    public WMTSLayer getWMTSLayer(WMTSLayerInfo info) throws IOException {

        String name = info.getName();
        if (info.getNativeName() != null) {
            name = info.getNativeName();
        }

        WMTSCapabilities caps = info.getStore().getWebMapTileServer(null).getCapabilities();

        for (WMTSLayer layer : caps.getLayerList()) {
            if (layer != null && name.equals(layer.getName())) {
                return layer;
            }
        }

        throw new IOException("Could not find layer " + info.getName() + " in the server capabilitiles document");
    }

    /** Clears the cached resource for a web map server */
    public void clear(WMSStoreInfo info) {
        String id = info.getId();
        if (id != null) wmsCache.remove(id);
    }

    /** Clears the cached resource for a web map server */
    public void clear(WMTSStoreInfo info) {
        String id = info.getId();
        if (id != null) wmtsCache.remove(id);
    }

    /**
     * Returns a style resource, caching the result. Any associated images should also be unpacked onto the local
     * machine. ResourcePool will watch the style for changes and invalidate the cache as needed.
     *
     * <p>The resource is loaded by parsing {@link StyleInfo#getFilename()} as an SLD. The SLD is prepared for direct
     * use by GeoTools, making use of absolute file paths where possible.
     *
     * @param info The style metadata.
     * @throws IOException Any parsing errors.
     */
    public StyledLayerDescriptor getSld(final StyleInfo info) throws IOException {
        String id = info.getId();
        if (id == null) return dataDir().parsedSld(info);
        String key = cacheKeys.unique(id);
        StyledLayerDescriptor sld = sldCache.get(key);
        if (sld == null) {
            synchronized (key) {
                sld = sldCache.get(key);
                if (sld == null) {
                    sld = dataDir().parsedSld(info);

                    sldCache.put(key, sld);

                    final Resource styleResource = dataDir().style(info);
                    styleResource.addListener(new ResourceListener() {
                        @Override
                        public void changed(ResourceNotification notify) {
                            sldCache.remove(key);
                            styleResource.removeListener(this);
                        }
                    });
                }
            }
        }

        return sld;
    }

    /**
     * Returns the first {@link Style} in a style resource, caching the result. Any associated images should also be
     * unpacked onto the local machine. ResourcePool will watch the style for changes and invalidate the cache as
     * needed.
     *
     * <p>The resource is loaded by parsing {@link StyleInfo#getFilename()} as an SLD. The SLD is prepared for direct
     * use by GeoTools, making use of absolute file paths where possible.
     *
     * @param info The style metadata.
     * @throws IOException Any parsing errors.
     */
    public Style getStyle(final StyleInfo info) throws IOException {
        String styleId = info.getId();
        if (styleId == null) return createStyle(info);
        String key = cacheKeys.unique(styleId);
        Style style = styleCache.get(key);
        if (style == null) {
            synchronized (key) {
                style = styleCache.get(key);
                if (style == null) {
                    style = createStyle(info);
                    styleCache.put(key, style);

                    final Resource styleResource = dataDir().style(info);
                    styleResource.addListener(new ResourceListener() {
                        @Override
                        public void changed(ResourceNotification notify) {
                            styleCache.remove(key);
                            styleResource.removeListener(this);
                        }
                    });
                }
            }
        }

        return style;
    }

    private Style createStyle(StyleInfo info) throws IOException {
        Style style = dataDir().parsedStyle(info);

        if (style == null) {
            throw new ServiceException("Could not extract a UserStyle definition from " + info.getName());
        }
        // Make sure we don't change the name of an object in sldCache
        return new StyleBuilder().reset(style).name(info.prefixedName()).buildStyle();
    }

    /**
     * Clears a style resource from the cache.
     *
     * @param info The style metadata.
     */
    public void clear(StyleInfo info) {
        String id = info.getId();
        if (id != null) {
            styleCache.remove(id);
            sldCache.remove(id);
        }
    }

    /**
     * Reads a raw style from persistence.
     *
     * @param style The configuration for the style.
     * @return A reader for the style.
     */
    public BufferedReader readStyle(StyleInfo style) throws IOException {
        Resource styleResource = dataDir().style(style);
        if (styleResource.getType() != Resource.Type.RESOURCE) {
            throw new FileNotFoundException("No such resource: " + style.getFilename());
        }

        return new BufferedReader(new InputStreamReader(styleResource.in()));
    }

    /**
     * Serializes a style to configuration.
     *
     * @param info The configuration for the style.
     * @param style The style object.
     */
    public void writeStyle(StyleInfo info, Style style) throws IOException {
        writeStyle(info, style, false);
    }

    /**
     * Serializes a style to configuration optionally formatting the style when writing it.
     *
     * @param info The configuration for the style.
     * @param style The style object.
     * @param format Whether to format the style
     */
    public void writeStyle(StyleInfo info, Style style, boolean format) throws IOException {
        synchronized (styleCache) {
            writeStyleFile(info, Styles.sld(style), format);
        }
    }

    /**
     * Serializes a style to configuration.
     *
     * @param info The configuration for the style.
     * @param style The style object.
     */
    public void writeSLD(StyleInfo info, StyledLayerDescriptor style) throws IOException {
        writeSLD(info, style, false);
    }

    /**
     * Serializes a style to configuration optionally formatting the style when writing it.
     *
     * @param info The configuration for the style.
     * @param style The style object.
     * @param format Whether to format the style
     */
    public void writeSLD(StyleInfo info, StyledLayerDescriptor style, boolean format) throws IOException {
        synchronized (sldCache) {
            writeStyleFile(info, style, format);
        }
    }

    private void writeStyleFile(StyleInfo info, StyledLayerDescriptor style, boolean format) throws IOException {
        Resource styleFile = dataDir().style(info);

        try (BufferedOutputStream out = new BufferedOutputStream(styleFile.out())) {
            Styles.handler(info.getFormat()).encode(style, info.getFormatVersion(), format, out);

        } catch (Exception e) {
            deleteStyleFile(styleFile);
            throw new IOException("Writing style failed.", e);
        } finally {
            clear(info);
        }
    }

    /**
     * Writes a raw style to configuration.
     *
     * @param style The configuration for the style.
     * @param in input stream representing the raw a style.
     */
    public void writeStyle(StyleInfo style, InputStream in) throws IOException {
        synchronized (styleCache) {
            Resource styleFile = dataDir().style(style);
            writeStyle(in, styleFile);
            clear(style);
        }
    }

    /**
     * Safe write on styleFile the passed inputStream
     *
     * @param in the new stream to write to styleFile
     * @param styleFile file to update
     */
    public static void writeStyle(final InputStream in, final Resource styleFile) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(styleFile.out())) {
            IOUtils.copy(in, out);
            out.flush();
        }
    }

    /**
     * Deletes a style from the configuration.
     *
     * @param style The configuration for the style.
     * @param purgeFile Whether to delete the file from disk.
     */
    public void deleteStyle(StyleInfo style, boolean purgeFile) throws IOException {
        synchronized (styleCache) {
            if (purgeFile) {
                deleteStyleFile(dataDir().style(style));
            }
        }
    }

    private void deleteStyleFile(Resource style) {
        File styleFile = Resources.file(style);
        if (styleFile != null && styleFile.exists()) {
            styleFile.delete();
        }
    }

    GeoServerDataDirectory dataDir() {
        return new GeoServerDataDirectory(catalog.getResourceLoader());
    }

    /** Disposes all cached resources. */
    public void dispose() {
        crsCache.clear();
        dataStoreCache.clear();
        featureTypeCache.clear();
        featureTypeAttributeCache.clear();
        hintCoverageReaderCache.clear();
        wmsCache.clear();
        wmtsCache.clear();
        sldCache.clear();
        styleCache.clear();
        listeners.clear();

        cacheKeys.clear();
        coverageCacheKeys.clear();
    }

    /**
     * Base class for all the resource caches, ensures type safety and provides an easier way to handle with resource
     * disposal
     *
     * @author Andrea Aime
     * @param <K>
     * @param <V>
     */
    abstract class CatalogResourceCache<K, V> extends SoftValueHashMap<K, V> {

        public CatalogResourceCache() {
            this(100);
        }

        public CatalogResourceCache(int hardReferences) {
            super(hardReferences);
            super.cleaner = (ValueCleaner<K, V>) (key, object) -> dispose(key, object);
        }

        @Override
        @SuppressWarnings("unchecked")
        public V remove(Object key) {
            V object = super.remove(key);
            if (object != null) {
                dispose((K) key, object);
            }
            return object;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void clear() {
            for (Entry entry : entrySet()) {
                try {
                    dispose((K) entry.getKey(), (V) entry.getValue());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error dispoing entry: " + entry, e);
                }
            }
            super.clear();
        }

        protected abstract void dispose(K key, V object);
    }

    class FeatureTypeCache extends CatalogResourceCache<String, FeatureType> {

        public FeatureTypeCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void dispose(String key, FeatureType featureType) {
            String id = key.substring(0, key.indexOf(PROJECTION_POLICY_SEPARATOR));
            FeatureTypeInfo info = catalog.getFeatureType(id);
            if (info != null) {
                LOGGER.fine("Disposing feature type '" + info.getName() + "'/" + id);
                fireDisposed(info, featureType);
                if (null != featureTypeAttributeCache.remove(id)) {
                    LOGGER.fine("AttributeType cache cleared for feature type '"
                            + info.getName()
                            + "'/"
                            + id
                            + " as a side effect of its cache disposal");
                }
            }
        }
    }

    /**
     * Custom CatalogResourceCache responsible for disposing of DataAccess instances (allowing the recovery of operating
     * system resources).
     *
     * @see ResourcePool#dataStoreCache
     */
    class DataStoreCache extends CatalogResourceCache<String, DataAccess> {
        /**
         * Ensure data access entry is removed from catalog, and ensure DataAccess dispose is called to return system
         * resources.
         *
         * <p>This method is used when cleaning up a weak reference and will immediately dispose of the indicated
         * dataAccess.
         *
         * @param id DataStore id, or null if not known
         * @param dataAccess DataAccess to dispose
         */
        @Override
        protected void dispose(String id, final DataAccess dataAccess) {
            DataStoreInfo info = catalog.getDataStore(id);
            final String name;
            if (info != null) {
                name = info.getName();
                LOGGER.fine("Disposing datastore '" + name + "'");
                fireDisposed(info, dataAccess);
            } else {
                name = "Untracked";
            }
            final String implementation = dataAccess.getClass().getSimpleName();
            try {
                LOGGER.fine("Dispose data access '" + name + "' " + implementation);
                dataAccess.dispose();
            } catch (Exception e) {
                LOGGER.warning("Error occured disposing data access '" + name + "' " + implementation);
                LOGGER.log(Level.FINE, "", e);
            }
        }
    }

    class CoverageReaderCache extends CatalogResourceCache<String, GridCoverageReader> {

        @Override
        protected void dispose(String id, GridCoverageReader reader) {
            CoverageStoreInfo info = catalog.getCoverageStore(id);
            if (info != null) {
                String name = info.getName();
                LOGGER.fine("Disposing coverage store '" + name + "'");

                fireDisposed(info, reader);
            }
            try {
                reader.dispose();
            } catch (Exception e) {
                LOGGER.warning("Error occured disposing coverage reader '" + info.getName() + "'");
                LOGGER.log(Level.FINE, "", e);
            }
        }
    }

    class CoverageHintReaderCache extends CatalogResourceCache<CoverageHintReaderKey, GridCoverageReader> {

        @Override
        protected void dispose(CoverageHintReaderKey key, GridCoverageReader reader) {
            CoverageStoreInfo info = catalog.getCoverageStore(key.id);
            if (info != null) {
                String name = info.getName();
                LOGGER.fine("Disposing coverage store '" + name + "'");

                fireDisposed(info, reader);
            }
            try {
                reader.dispose();
            } catch (Exception e) {
                LOGGER.warning("Error occured disposing coverage reader '" + info.getName() + "'");
                LOGGER.log(Level.FINE, "", e);
            }
        }
    }

    /**
     * The key in the {@link CoverageHintReaderCache}
     *
     * @author Andrea Aime - GeoSolutions
     */
    public static class CoverageHintReaderKey {
        String id;
        Hints hints;

        public CoverageHintReaderKey(String id, Hints hints) {
            this.id = id;
            this.hints = hints;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((hints == null) ? 0 : hints.hashCode());
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            CoverageHintReaderKey other = (CoverageHintReaderKey) obj;
            if (hints == null) {
                if (other.hints != null) return false;
            } else if (!hints.equals(other.hints)) return false;
            if (id == null) {
                if (other.id != null) return false;
            } else if (!id.equals(other.id)) return false;
            return true;
        }
    }

    class FeatureTypeAttributeCache extends CatalogResourceCache<String, List<AttributeTypeInfo>> {

        FeatureTypeAttributeCache(int size) {
            super(size);
        }

        @Override
        protected void dispose(String key, List<AttributeTypeInfo> object) {
            // nothing to do actually
        }
    }

    class WMSCache extends CatalogResourceCache<String, WebMapServer> {

        @Override
        protected void dispose(String key, WebMapServer server) {
            HTTPClient client = server.getHTTPClient();
            if (client instanceof Closeable) {
                // dispose the client, and the connection pool hosted into it as a consequence
                // the connection pool additionally holds a few threads that are also getting
                // disposed with this call
                @SuppressWarnings("PMD.CloseResource") // actually closing here
                Closeable closeable = (Closeable) client;
                try {
                    closeable.close();
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Failure while disposing the http client for a WMS store", e);
                }
            }
        }
    }

    class WMTSCache extends CatalogResourceCache<String, WebMapTileServer> {

        @Override
        protected void dispose(String key, WebMapTileServer server) {
            HTTPClient client = server.getHTTPClient();
            if (client instanceof Closeable) {
                // dispose the client, and the connection pool hosted into it as a consequence
                // the connection pool additionally holds a few threads that are also getting
                // disposed with this call
                @SuppressWarnings("PMD.CloseResource") // actually closing here
                Closeable closeable = (Closeable) client;
                try {
                    closeable.close();
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Failure while disposing the http client for a WMTS store", e);
                }
            }
        }
    }

    /** Listens to catalog events clearing cache entires when resources are modified. */
    public static class CacheClearingListener extends CatalogVisitorAdapter implements CatalogListener {

        private final ResourcePool pool;

        public CacheClearingListener(ResourcePool pool) {
            Objects.requireNonNull(pool);
            this.pool = pool;
        }

        @Override
        public void handleAddEvent(CatalogAddEvent event) {}

        @Override
        public void handleModifyEvent(CatalogModifyEvent event) {}

        @Override
        public void handlePostModifyEvent(CatalogPostModifyEvent event) {
            CatalogInfo source = event.getSource();
            source.accept(this);
        }

        @Override
        public void handleRemoveEvent(CatalogRemoveEvent event) {
            CatalogInfo source = event.getSource();
            source.accept(this);
        }

        @Override
        public void reloaded() {}

        @Override
        public void visit(DataStoreInfo dataStore) {
            pool.clear(dataStore);
        }

        @Override
        public void visit(CoverageStoreInfo coverageStore) {
            pool.clear(coverageStore);
        }

        @Override
        public void visit(FeatureTypeInfo featureType) {
            pool.clear(featureType);
        }

        @Override
        public void visit(WMSStoreInfo wmsStore) {
            pool.clear(wmsStore);
        }

        @Override
        public void visit(StyleInfo style) {
            pool.clear(style);
        }
    }

    /**
     * Used to clean up any outstanding data store listeners.
     *
     * <p>The DataStore is still active as the listeners are called allowing any required clean up to occur.
     *
     * @param da Data access
     */
    void fireDisposed(DataStoreInfo dataStore, DataAccess da) {
        for (Listener l : listeners) {
            try {
                l.disposed(dataStore, da);
            } catch (Throwable t) {
                LOGGER.warning("Resource pool listener threw error");
                LOGGER.log(Level.INFO, t.getLocalizedMessage(), t);
            }
        }
    }

    void fireDisposed(FeatureTypeInfo featureType, FeatureType ft) {
        for (Listener l : listeners) {
            try {
                l.disposed(featureType, ft);
            } catch (Throwable t) {
                LOGGER.warning("Resource pool listener threw error");
                LOGGER.log(Level.INFO, t.getLocalizedMessage(), t);
            }
        }
    }

    void fireDisposed(CoverageStoreInfo coverageStore, GridCoverageReader gcr) {
        for (Listener l : listeners) {
            try {
                l.disposed(coverageStore, gcr);
            } catch (Throwable t) {
                LOGGER.warning("Resource pool listener threw error");
                LOGGER.log(Level.INFO, t.getLocalizedMessage(), t);
            }
        }
    }

    /**
     * Listener for resource pool events.
     *
     * @author Justin Deoliveira, OpenGeo
     */
    public static interface Listener {

        /** Event fired when a data store is evicted from the resource pool. */
        void disposed(DataStoreInfo dataStore, DataAccess da);

        /** Event fired when a coverage store is evicted from the resource pool. */
        void disposed(CoverageStoreInfo coverageStore, GridCoverageReader gcr);

        /** Event fired when a feature type is evicted from the resource pool. */
        void disposed(FeatureTypeInfo featureType, FeatureType ft);
    }

    /**
     * Flush the feature type held by the data store associated with a FeatureTypeInfo to be safe in case the underlying
     * schema has changed.
     *
     * <p>Implementation note: so far this method only works with {@link ContentDataStore} instances (i.e. all JDBC ones
     * and others, but not all). This is to avoid calling {@link DataStore#dispose()} as other threads may be using it
     * and has proved to result in unpredictable errors. Instead, we're calling the datastore feature type's
     * {@link ContentState#flush()} method which forces re-loading the native type when next used.
     */
    protected void flushDataStore(FeatureTypeInfo ft) {
        DataStoreInfo ds = ft.getStore();
        if (ds == null) {
            return;
        }
        if (!dataStoreCache.containsKey(ds.getId())) {
            return; // don't bother if DataStore not cached
        }
        DataAccess<?, ?> dataStore;
        try {
            dataStore = getDataStore(ds);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to obtain data store '" + ft.getQualifiedNativeName() + "' to flush", e);
            return;
        }
        final int dsFtCount = countFeatureTypesOf(ds);
        if (dsFtCount == 0) {
            // clean up cached DataAccess if no longer in use
            LOGGER.log(Level.FINE, "Feature Type {0} cleared: Disposing DataStore {1} - {2}", new String[] {
                ft.getName(), ds.getName(), "Last Feature Type Disposed"
            });
            clear(ds);
        } else {
            if (dataStore instanceof ContentDataStore) {
                ContentDataStore contentDataStore = (ContentDataStore) dataStore;
                try {
                    // ask ContentDataStore to forget cached column information
                    String nativeName = ft.getNativeName();
                    if (nativeName != null) {
                        flushState(contentDataStore, nativeName);
                        LOGGER.log(Level.FINE, "Feature Type {0} cleared from ContentDataStore {1}", new String[] {
                            ft.getName(), ds.getName()
                        });
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Unable to flush '" + ft.getQualifiedNativeName(), e);
                }
            } else {
                LOGGER.log(
                        Level.FINE,
                        "Unable to clean up cached feature type {0} in data store {1} - not a ContentDataStore",
                        new String[] {ft.getName(), ds.getName()});
            }
        }
    }

    private int countFeatureTypesOf(DataStoreInfo ds) {
        Filter filter = Predicates.equal("store.id", ds.getId());
        int dsTypeCount = catalog.count(FeatureTypeInfo.class, filter);
        return dsTypeCount;
    }

    private void flushState(ContentDataStore contentDataStore, String nativeName) throws IOException {
        ContentFeatureSource featureSource = contentDataStore.getFeatureSource(nativeName);
        featureSource.getState().flush();
    }

    public DataStoreInfo clone(final DataStoreInfo source, boolean allowEnvParametrization) {
        DataStoreInfo target;
        try {
            target = SerializationUtils.clone(source);
            if (target instanceof StoreInfoImpl && target.getCatalog() == null) {
                ((StoreInfoImpl) target).setCatalog(catalog);
            }
        } catch (Exception e) {
            target = catalog.getFactory().createDataStore();
            target.setEnabled(source.isEnabled());
            target.setName(source.getName());
            target.setDescription(source.getDescription());
            target.setWorkspace(source.getWorkspace());
            target.setType(source.getType());
        }

        // Resolve GeoServer Environment placeholders
        final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

        if (source.getConnectionParameters() != null
                && !source.getConnectionParameters().isEmpty()) {
            target.getConnectionParameters().clear();

            if (!allowEnvParametrization) {
                target.getConnectionParameters().putAll(source.getConnectionParameters());
            } else {
                if (source != null && source.getConnectionParameters() != null) {
                    for (Entry<String, Serializable> param :
                            source.getConnectionParameters().entrySet()) {
                        String key = param.getKey();
                        Object value = param.getValue();

                        if (gsEnvironment != null && GeoServerEnvironment.allowEnvParametrization()) {
                            value = gsEnvironment.resolveValue(value);
                        }

                        target.getConnectionParameters().put(key, (Serializable) value);
                    }
                }
            }
        }

        return target;
    }

    public CoverageStoreInfo clone(final CoverageStoreInfo source, boolean allowEnvParametrization) {
        CoverageStoreInfo target;
        try {
            target = SerializationUtils.clone(source);
            if (target instanceof StoreInfoImpl && target.getCatalog() == null) {
                ((StoreInfoImpl) target).setCatalog(catalog);
            }
        } catch (Exception e) {
            target = catalog.getFactory().createCoverageStore();
            target.setDescription(source.getDescription());
            target.setEnabled(source.isEnabled());
            target.setName(source.getName());
            target.setType(source.getType());
            target.setWorkspace(source.getWorkspace());
        }

        // Resolve GeoServer Environment placeholders
        final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

        if (gsEnvironment != null && GeoServerEnvironment.allowEnvParametrization()) {
            target.setURL((String) gsEnvironment.resolveValue(source.getURL()));
        } else {
            target.setURL(source.getURL());
        }

        if (source.getConnectionParameters() != null
                && !source.getConnectionParameters().isEmpty()) {

            if (!allowEnvParametrization) {
                target.setURL(source.getURL());
                target.getConnectionParameters().putAll(source.getConnectionParameters());
            } else {
                for (Entry<String, Serializable> param :
                        source.getConnectionParameters().entrySet()) {
                    String key = param.getKey();
                    Object value = param.getValue();

                    if (gsEnvironment != null && GeoServerEnvironment.allowEnvParametrization()) {
                        value = gsEnvironment.resolveValue(value);
                    }

                    target.getConnectionParameters().put(key, (Serializable) value);
                }
            }
        }

        return target;
    }

    public WMSStoreInfo clone(final WMSStoreInfo source, boolean allowEnvParametrization) {
        WMSStoreInfo target;
        try {
            target = SerializationUtils.clone(source);
            if (target instanceof StoreInfoImpl && target.getCatalog() == null) {
                ((StoreInfoImpl) target).setCatalog(catalog);
            }
        } catch (Exception e) {
            target = catalog.getFactory().createWebMapServer();
            target.setDescription(source.getDescription());
            target.setEnabled(source.isEnabled());
            target.setName(source.getName());
            target.setType(source.getType());
            target.setWorkspace(source.getWorkspace());
        }

        setConnectionParameters(source, target);

        if (allowEnvParametrization) {
            // Resolve GeoServer Environment placeholders
            final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

            if (gsEnvironment != null && GeoServerEnvironment.allowEnvParametrization()) {
                target.setCapabilitiesURL((String) gsEnvironment.resolveValue(source.getCapabilitiesURL()));
                target.setUsername((String) gsEnvironment.resolveValue(source.getUsername()));
                target.setPassword((String) gsEnvironment.resolveValue(source.getPassword()));
            }
        }

        return target;
    }

    public WMTSStoreInfo clone(final WMTSStoreInfo source, boolean allowEnvParametrization) {
        WMTSStoreInfo target;
        try {
            target = SerializationUtils.clone(source);
            if (target instanceof StoreInfoImpl && target.getCatalog() == null) {
                ((StoreInfoImpl) target).setCatalog(catalog);
            }
        } catch (Exception e) {
            target = catalog.getFactory().createWebMapTileServer();
            target.setDescription(source.getDescription());
            target.setEnabled(source.isEnabled());
            target.setName(source.getName());
            target.setType(source.getType());
            target.setWorkspace(source.getWorkspace());
        }

        setConnectionParameters(source, target);

        if (allowEnvParametrization) {
            // Resolve GeoServer Environment placeholders
            final GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);

            if (gsEnvironment != null && GeoServerEnvironment.allowEnvParametrization()) {
                target.setCapabilitiesURL((String) gsEnvironment.resolveValue(source.getCapabilitiesURL()));
                target.setUsername((String) gsEnvironment.resolveValue(source.getUsername()));
                target.setPassword((String) gsEnvironment.resolveValue(source.getPassword()));
            }
        }

        return target;
    }

    /** */
    private void setConnectionParameters(final WMSStoreInfo source, WMSStoreInfo target) {
        target.setCapabilitiesURL(source.getCapabilitiesURL());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setUseConnectionPooling(source.isUseConnectionPooling());
        target.setMaxConnections(source.getMaxConnections());
        target.setConnectTimeout(source.getConnectTimeout());
        target.setReadTimeout(source.getReadTimeout());
    }

    /** */
    private void setConnectionParameters(final WMTSStoreInfo source, WMTSStoreInfo target) {
        target.setCapabilitiesURL(source.getCapabilitiesURL());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setUseConnectionPooling(source.isUseConnectionPooling());
        target.setMaxConnections(source.getMaxConnections());
        target.setConnectTimeout(source.getConnectTimeout());
        target.setReadTimeout(source.getReadTimeout());
    }

    /**
     * Retrieve the proper {@link CoverageInfo} object from the specified {@link CoverageStoreInfo} using the specified
     * coverageName (which may be the native one in some cases). In case of null coverageName being specified, we assume
     * we are dealing with a single coverageStore <-> single coverage relation so we will take the first coverage
     * available on that store.
     *
     * @param storeInfo the storeInfo to be used to access the catalog
     */
    static CoverageInfo getCoverageInfo(String coverageName, CoverageStoreInfo storeInfo) {
        Utilities.ensureNonNull("storeInfo", storeInfo);
        final Catalog catalog = storeInfo.getCatalog();
        CoverageInfo info = null;
        if (coverageName != null) {
            info = catalog.getCoverageByName(coverageName);
        }
        if (info == null) {
            final List<CoverageInfo> coverages = catalog.getCoveragesByStore(storeInfo);
            if (coverageName != null) {
                for (CoverageInfo coverage : coverages) {
                    if (coverage.getNativeName().equalsIgnoreCase(coverageName)) {
                        info = coverage;
                        break;
                    }
                }
            }
            if (info == null && coverages != null && coverages.size() == 1) {
                // Last resort
                info = coverages.get(0);
            }
        }
        return info;
    }

    /**
     * The catalog repository, used to gather store references by name by some GeoTools stores like pre-generalized or
     * image mosaic
     */
    public CatalogRepository getRepository() {
        return repository;
    }
}
