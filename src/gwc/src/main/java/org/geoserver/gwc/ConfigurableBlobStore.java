/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.BlobStoreListener;
import org.geowebcache.storage.BlobStoreListenerList;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.blobstore.file.FileBlobStore;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.geowebcache.storage.blobstore.memory.CacheStatistics;
import org.geowebcache.storage.blobstore.memory.MemoryBlobStore;
import org.geowebcache.storage.blobstore.memory.NullBlobStore;
import org.geowebcache.storage.blobstore.memory.guava.GuavaCacheProvider;

/**
 * {@link MemoryBlobStore} implementation used for changing {@link CacheProvider} and wrapped {@link
 * BlobStore} at runtime. An instance of this class requires to call the setChanged() method for
 * modifying its configuration.
 *
 * @author Nicola Lagomarsini Geosolutions
 */
public class ConfigurableBlobStore implements BlobStore {

    /** Logger instance for the class */
    private static final Logger LOGGER = Logging.getLogger(ConfigurableBlobStore.class);

    /** Delegate Object to use for executing the operations */
    private BlobStore delegate;

    /** {@link MemoryBlobStore} used for in memory caching */
    private MemoryBlobStore memoryStore;

    /** Cache provider to add to the {@link MemoryBlobStore} */
    private CacheProvider cache;

    /** {@link NullBlobStore} used for avoiding persistence */
    private NullBlobStore nullStore;

    /** {@link FileBlobStore} used as default by GWC */
    private BlobStore defaultStore;

    /** Atomic counter used for keeping into account how many operations are executed in parallel */
    private AtomicLong actualOperations;

    /** Atomic boolean indicating if the BlobStore has been configured */
    private AtomicBoolean configured;

    /** Map containing mapping for {@link CacheConfiguration}s associated to each CacheProvider */
    private Map<String, CacheConfiguration> internalCacheConfigs;

    /** Map containing mapping for {@link CacheProvider} names */
    private Map<String, String> cacheProvidersNames;

    /** Map containing mapping for {@link CacheProvider}s */
    private Map<String, CacheProvider> cacheProviders;

    /** Save the listeners to re-apply them to the delegate blobstore upon config changes */
    private BlobStoreListenerList listeners = new BlobStoreListenerList();

    public ConfigurableBlobStore(
            BlobStore defaultStore, MemoryBlobStore memoryStore, NullBlobStore nullStore) {
        // Initialization
        configured = new AtomicBoolean(false);
        actualOperations = new AtomicLong(0);
        this.delegate = defaultStore;
        this.defaultStore = defaultStore;
        this.memoryStore = memoryStore;
        this.nullStore = nullStore;
        // Creating three maps:
        // 1 containing a mapping key-cacheProvider
        // 2 containing a mapping key-cacheProvider description
        // 3 containing a mapping key-cacheConfiguration
        // where key is the cacheProvider classname
        HashMap<String, CacheProvider> cacheProviders = new HashMap<String, CacheProvider>();
        HashMap<String, String> cacheProvidersNames = new HashMap<String, String>();
        List<CacheProvider> extensions = GeoServerExtensions.extensions(CacheProvider.class);
        for (CacheProvider provider : extensions) {
            if (provider.isAvailable()) {
                cacheProviders.put(provider.getClass().toString(), provider);
                cacheProvidersNames.put(provider.getClass().toString(), provider.getName());
            }
        }

        this.cacheProviders = Collections.unmodifiableMap(cacheProviders);
        this.cacheProvidersNames = Collections.unmodifiableMap(cacheProvidersNames);
        this.internalCacheConfigs = new HashMap<String, CacheConfiguration>();
    }

    @Override
    public boolean delete(String layerName) throws StorageException {
        // NOTE that if the blobstore has already been configured, the user must
        // always call setConfig() for
        // setting the new configuration
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Delete the selected Layer
                return delegate.delete(layerName);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return true;
    }

    @Override
    public boolean deleteByGridsetId(String layerName, String gridSetId) throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Delete the TileObjects related to the selected gridset
                return delegate.deleteByGridsetId(layerName, gridSetId);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return true;
    }

    @Override
    public boolean delete(TileObject obj) throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Deletes the single TileObject
                return delegate.delete(obj);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return true;
    }

    @Override
    public boolean delete(TileRange obj) throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Deletes this TileRange
                return delegate.delete(obj);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return true;
    }

    @Override
    public boolean get(TileObject obj) throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Get a TileObject
                return delegate.get(obj);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return false;
    }

    @Override
    public void put(TileObject obj) throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Put the TileObject
                delegate.put(obj);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void clear() throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Clear the BlobStore
                delegate.clear();
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    @SuppressWarnings("PMD.EmptyWhileStmt")
    public synchronized void destroy() {
        if (configured.getAndSet(false)) {
            // Avoid to call the While cycle before having started an operation
            // with configured == true
            actualOperations.incrementAndGet();
            actualOperations.decrementAndGet();
            // Wait until all the operations are finished
            while (actualOperations.get() > 0) {}
            // Destroy all
            defaultStore.destroy();
            memoryStore.destroy();
            cache.reset();
        }
    }

    @Override
    public void addListener(BlobStoreListener listener) {
        // save it in case of further config changes
        this.listeners.addListener(listener);
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Add a new Listener to the NullBlobStore
                delegate.addListener(listener);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public boolean removeListener(BlobStoreListener listener) {
        // remove it from the local backup
        this.listeners.removeListener(listener);
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Remove a Listener from the BlobStore
                return delegate.removeListener(listener);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return true;
    }

    @Override
    public boolean rename(String oldLayerName, String newLayerName) throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Rename a Layer
                return delegate.rename(oldLayerName, newLayerName);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return false;
    }

    @Override
    public String getLayerMetadata(String layerName, String key) {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Get The Layer metadata
                return delegate.getLayerMetadata(layerName, key);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return null;
    }

    @Override
    public void putLayerMetadata(String layerName, String key, String value) {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Put the Layer metadata
                delegate.putLayerMetadata(layerName, key, value);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public boolean layerExists(String layerName) {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Get a TileObject
                return delegate.layerExists(layerName);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return false;
    }

    @Override
    public Map<String, Optional<Map<String, String>>> getParametersMapping(String layerName) {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Get a TileObject
                return delegate.getParametersMapping(layerName);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return Collections.emptyMap();
    }

    public CacheStatistics getCacheStatistics() {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Get Cache Statistics
                return cache.getStatistics();
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        // Not configured, returns an empty statistics
        return new CacheStatistics();
    }

    public void clearCache() {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Clear the cache
                cache.clear();
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    public CacheProvider getCache() {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Returns the cache object used
                return cache;
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return null;
    }

    /**
     * Returns a map of all the cache provider instances, where the key is the {@link CacheProvider}
     * class.
     *
     * @return a Map containing all the CacheProvider instances
     */
    public Map<String, CacheProvider> getCacheProviders() {
        return cacheProviders;
    }

    /**
     * Returns a map of all the cache provider description, where the key is the {@link
     * CacheProvider} class.
     *
     * @return a Map containing all the CacheProvider descriptions
     */
    public Map<String, String> getCacheProvidersNames() {
        return cacheProvidersNames;
    }

    /**
     * This method changes the {@link ConfigurableBlobStore} configuration. It can be used for
     * changing cache configuration or the blobstore used.
     */
    public synchronized void setChanged(GWCConfig gwcConfig, boolean initialization) {
        // Change the blobstore configuration
        configureBlobStore(gwcConfig, initialization);
    }

    @SuppressWarnings("PMD.EmptyWhileStmt")
    private void configureBlobStore(GWCConfig gwcConfig, boolean initialization) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Configuring BlobStore");
        }
        // reset the configuration
        configured.getAndSet(false);

        // Avoid to call the While cycle before having started an operation with
        // configured == true
        actualOperations.incrementAndGet();
        actualOperations.decrementAndGet();
        // Wait until all the operations are finished
        while (actualOperations.get() > 0) {}

        // Getting the cache provider to use
        String cacheProvider = gwcConfig.getCacheProviderClass();

        // Check if it is present, else use the GuavaCacheProvider as default
        if (!getCacheProviders().containsKey(cacheProvider)) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Wrong CacheProvider defined, using default one");
            }
            cacheProvider = GuavaCacheProvider.class.toString();
            if (!initialization) {
                gwcConfig.setCacheProviderClass(cacheProvider);
                try {
                    GWC.get().saveConfig(gwcConfig);
                } catch (IOException e) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }
        // Getting Cache configuration for the CacheProvider
        CacheConfiguration cacheConfiguration =
                gwcConfig.getCacheConfigurations().get(cacheProvider);
        // Add the internal Cache configuration for the first time
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Configuring cache");
        }

        // Setting cache
        cache = getCacheProviders().get(cacheProvider);

        // Modify configuration only if the CacheProvider can be modified
        if (!cache.isImmutable()) {
            CacheConfiguration internalCacheConfig = internalCacheConfigs.get(cacheProvider);
            if (internalCacheConfig == null) {
                internalCacheConfig = new CacheConfiguration();
                internalCacheConfig.setConcurrencyLevel(cacheConfiguration.getConcurrencyLevel());
                internalCacheConfig.setEvictionTime(cacheConfiguration.getEvictionTime());
                internalCacheConfig.setHardMemoryLimit(cacheConfiguration.getHardMemoryLimit());
                internalCacheConfig.setPolicy(cacheConfiguration.getPolicy());
                cache.configure(cacheConfiguration);
                internalCacheConfigs.put(cacheProvider, internalCacheConfig);
            } else if (!internalCacheConfig.equals(cacheConfiguration)) {
                internalCacheConfig.setConcurrencyLevel(cacheConfiguration.getConcurrencyLevel());
                internalCacheConfig.setEvictionTime(cacheConfiguration.getEvictionTime());
                internalCacheConfig.setHardMemoryLimit(cacheConfiguration.getHardMemoryLimit());
                internalCacheConfig.setPolicy(cacheConfiguration.getPolicy());
                cache.configure(internalCacheConfig);
            }

            // If GWC has been already configured, we must cycle on all the
            // layers in order to check
            // which must not be cached
            if (!initialization) {
                Iterable<GeoServerTileLayer> geoServerTileLayers =
                        GWC.get().getGeoServerTileLayers();

                for (GeoServerTileLayer layer : geoServerTileLayers) {
                    if (layer.getInfo().isEnabled() && !layer.getInfo().isInMemoryCached()) {
                        cache.addUncachedLayer(layer.getName());
                    }
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Configuring BlobStore delegate");
        }
        // BlobStore configuration

        // remove listeners from old delegate
        for (BlobStoreListener listener : listeners.getListeners()) {
            delegate.removeListener(listener);
        }
        if (gwcConfig.isInnerCachingEnabled()) {
            memoryStore.setCacheProvider(cache);
            if (!gwcConfig.isPersistenceEnabled()) {
                memoryStore.setStore(nullStore);
            } else {
                memoryStore.setStore(defaultStore);
            }
            delegate = memoryStore;
        } else {
            delegate = defaultStore;
        }
        // apply listeners to new delegate
        for (BlobStoreListener listener : listeners.getListeners()) {
            delegate.addListener(listener);
        }

        // Update the configured parameter
        configured.getAndSet(true);
    }

    /** @return the used {@link BlobStore} for testing purpose */
    BlobStore getDelegate() {
        return delegate;
    }

    /** Setter for the Tests */
    void setCache(CacheProvider cache) {
        // Setting cache provider
        Map<String, CacheProvider> provs = new HashMap<String, CacheProvider>(cacheProviders);
        provs.put(cache.getClass().toString(), cache);
        cacheProviders = provs;
        this.cache = cache;
        memoryStore.setCacheProvider(cache);
    }

    @Override
    public boolean deleteByParameters(String layerName, Map<String, String> parameters)
            throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Get a TileObject
                return delegate.deleteByParameters(layerName, parameters);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return false;
    }

    @Override
    public boolean deleteByParametersId(String layerName, String parametersId)
            throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Get a TileObject
                return delegate.deleteByParametersId(layerName, parametersId);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return false;
    }

    @Override
    public Set<String> getParameterIds(String layerName) throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Get a TileObject
                return delegate.getParameterIds(layerName);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return Collections.emptySet();
    }

    @Override
    public boolean purgeOrphans(TileLayer layer) throws StorageException {
        // Check if the blobstore has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Get a TileObject
                return delegate.purgeOrphans(layer);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return false;
    }
}
