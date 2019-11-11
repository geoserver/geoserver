/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.collect.Maps.newConcurrentMap;

import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.*;
import org.geoserver.gwc.GWC;
import org.geoserver.ows.LocalPublished;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.wms.WMS;
import org.geotools.util.logging.Logging;
import org.geowebcache.ReinitializingBean;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A GWC's {@link TileLayerConfiguration} implementation that provides {@link TileLayer}s directly
 * from the GeoServer {@link Catalog}'s {@link LayerInfo}s and {@link LayerGroupInfo}s.
 *
 * <p>The sole responsibility of the class is to provide the {@link GeoServerTileLayer}s out of the
 * geoserver catalog for {@link TileLayerDispatcher}
 *
 * @see CatalogStyleChangeListener
 */
public class CatalogConfiguration implements TileLayerConfiguration {

    /** The configuration lock timeout, in seconds */
    static final int GWC_CONFIGURATION_LOCK_TIMEOUT =
            Integer.getInteger("gwc.configuration.lock.timeout", 60);

    /** {@link GeoServerTileLayer} cache loader */
    private final class TileLayerLoader extends CacheLoader<String, GeoServerTileLayer> {
        private final TileLayerCatalog tileLayerCatalog;

        private TileLayerLoader(TileLayerCatalog tileLayerCatalog) {
            this.tileLayerCatalog = tileLayerCatalog;
        }

        @Override
        public GeoServerTileLayer load(String layerId) throws Exception {
            GeoServerTileLayer tileLayer = null;
            final GridSetBroker gridSetBroker = CatalogConfiguration.this.gridSetBroker;

            lock.acquireReadLock();
            try {
                if (pendingDeletes.contains(layerId)) {
                    throw new IllegalArgumentException("Tile layer '" + layerId + "' was deleted.");
                }
                GeoServerTileLayerInfo tileLayerInfo = pendingModications.get(layerId);
                if (tileLayerInfo == null) {
                    tileLayerInfo = tileLayerCatalog.getLayerById(layerId);
                }
                if (tileLayerInfo == null) {
                    throw new IllegalArgumentException(
                            "GeoServerTileLayerInfo '" + layerId + "' does not exist.");
                }

                tileLayer =
                        new GeoServerTileLayer(
                                geoServerCatalog, layerId, gridSetBroker, tileLayerInfo);
            } finally {
                lock.releaseReadLock();
            }
            if (null == tileLayer) {
                throw new IllegalArgumentException(
                        "GeoServer layer or layer group '" + layerId + "' does not exist");
            }
            return tileLayer;
        }
    }

    private static final Logger LOGGER = Logging.getLogger(CatalogConfiguration.class);

    private TileLayerCatalog tileLayerCatalog;

    private Catalog geoServerCatalog;

    private GridSetBroker gridSetBroker;

    /** Maps pending modifications by {@link GeoServerTileLayerInfo#getId()} */
    private final Map<String, GeoServerTileLayerInfo> pendingModications = newConcurrentMap();

    private final LoadingCache<String, GeoServerTileLayer> layerCache;

    /** Ids of pending deletes */
    private final Set<String> pendingDeletes = new CopyOnWriteArraySet<String>();

    private final TimeoutReadWriteLock lock =
            new TimeoutReadWriteLock(GWC_CONFIGURATION_LOCK_TIMEOUT * 1000, "GWC Configuration");

    public CatalogConfiguration(
            final Catalog catalog,
            final TileLayerCatalog tileLayerCatalog,
            final GridSetBroker gridSetBroker) {
        checkNotNull(catalog);
        checkNotNull(tileLayerCatalog);
        checkNotNull(gridSetBroker);
        this.tileLayerCatalog = tileLayerCatalog;
        this.geoServerCatalog = catalog;
        this.gridSetBroker = gridSetBroker;

        this.layerCache =
                CacheBuilder.newBuilder() //
                        .concurrencyLevel(10) //
                        .expireAfterAccess(10, TimeUnit.MINUTES) //
                        .initialCapacity(10) //
                        .maximumSize(100) //
                        .build(new TileLayerLoader(tileLayerCatalog));

        tileLayerCatalog.addListener(
                new TileLayerCatalogListener() {

                    @Override
                    public void onEvent(String layerId, TileLayerCatalogListener.Type type) {
                        if (type == TileLayerCatalogListener.Type.MODIFY
                                || type == TileLayerCatalogListener.Type.DELETE) {
                            layerCache.invalidate(layerId);
                        }
                    }
                });
    }

    /** @see TileLayerConfiguration#getIdentifier() */
    @Override
    public String getIdentifier() {
        return "GeoServer Catalog Configuration";
    }

    /** @see TileLayerConfiguration#getLayers() */
    @Override
    public Collection<TileLayer> getLayers() {
        lock.acquireReadLock();
        try {
            final Set<String> layerNames = tileLayerCatalog.getLayerNames();

            Function<String, Optional<TileLayer>> lazyLayerFetch =
                    CatalogConfiguration.this::getLayer;

            // removing the NULL results
            // TODO Should deep copy or wrap with modification proxies,
            // see
            // org.geoserver.gwc.layer.CatalogConfigurationLayerConformanceTest.testModifyCallRequiredToChangeInfoFromGetInfo()
            return Lists.newArrayList(
                    layerNames
                            .stream()
                            .map(lazyLayerFetch)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList()));
        } finally {
            lock.releaseReadLock();
        }
    }

    /**
     * Returns a dynamic list of cached layer names out of the GeoServer {@link Catalog}
     *
     * @see TileLayerConfiguration#getLayerNames()
     */
    @Override
    public Set<String> getLayerNames() {
        lock.acquireReadLock();
        try {
            final Set<String> storedNames = tileLayerCatalog.getLayerNames();
            Set<String> names = null;
            if (!pendingDeletes.isEmpty()) {
                names = new HashSet<String>(storedNames);
                for (String id : pendingDeletes) {
                    GeoServerTileLayerInfo old = tileLayerCatalog.getLayerById(id);
                    names.remove(old.getName());
                }
            }
            if (!pendingModications.isEmpty()) {
                for (Map.Entry<String, GeoServerTileLayerInfo> e : pendingModications.entrySet()) {
                    GeoServerTileLayerInfo old = tileLayerCatalog.getLayerById(e.getKey());
                    if (old != null) {
                        // it's a modification, not an addition. Make sure the name is not outdated
                        String oldName = old.getName();
                        String newName = e.getValue().getName();
                        if (!Objects.equal(oldName, newName)) {
                            if (names == null) {
                                names = new HashSet<String>(storedNames);
                            }
                            names.remove(oldName);
                            names.add(newName);
                        }
                    }
                }
            }
            return names == null ? storedNames : Collections.unmodifiableSet(names);
        } finally {
            lock.releaseReadLock();
        }
    }

    @Override
    public boolean containsLayer(String layerName) {
        checkNotNull(layerName, "layer id is null");
        lock.acquireReadLock();
        try {
            if (pendingDeletes.contains(layerName)) {
                return false;
            }
            String layerId = tileLayerCatalog.getLayerId(layerName);
            return layerId != null;
        } finally {
            lock.releaseReadLock();
        }
    }

    private GeoServerTileLayer getTileLayerById(final String layerId) {
        checkNotNull(layerId, "layer id is null");

        GeoServerTileLayer layer;
        lock.acquireReadLock();
        try {
            layer = layerCache.get(layerId);
            // let's see if this a virtual service request
            WorkspaceInfo localWorkspace = LocalWorkspace.get();
            PublishedInfo localPublished = LocalPublished.get();
            if (localWorkspace != null) {
                // yup this is a virtual service request, so we need to filter layers per workspace
                WorkspaceInfo layerWorkspace;
                PublishedInfo publishedInfo = layer.getPublishedInfo();
                if (publishedInfo instanceof LayerInfo) {
                    // this is a normal layer
                    layerWorkspace =
                            ((LayerInfo) publishedInfo).getResource().getStore().getWorkspace();
                } else {
                    // this is a layer group
                    layerWorkspace = ((LayerGroupInfo) publishedInfo).getWorkspace();
                }
                // check if the layer doesn't have an workspace (this is possible for layer groups)
                if (layerWorkspace == null) {
                    // no workspace means that it doesn't belong to this workspace
                    return null;
                }
                // if the layer matches the virtual service workspace we return the layer otherwise
                // NULL is returned
                if (!localWorkspace.getName().equals(layerWorkspace.getName())) {
                    return null;
                }

                // are we in a layer specific case too?

                if (localPublished != null
                        && !localPublished.getName().equals(publishedInfo.getName())) {
                    return null;
                }
            } else if (localPublished != null) {
                // this implies we're looking at a global layer group, there is no such a thing
                // as a global layer
                PublishedInfo publishedInfo = layer.getPublishedInfo();
                if (!(publishedInfo instanceof LayerGroupInfo)) {
                    return null;
                } else {
                    LayerGroupInfo lg = (LayerGroupInfo) publishedInfo;
                    if (lg.getWorkspace() != null
                            || !lg.getName().equals(localPublished.getName())) {
                        return null;
                    }
                }
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (UncheckedExecutionException e) {
            throwIfUnchecked(e.getCause());
            // just to make it compile....
            throw e;
        } finally {
            lock.releaseReadLock();
        }

        return layer;
    }

    /** @see TileLayerConfiguration#getLayer(String) */
    @Override
    public Optional<TileLayer> getLayer(final String layerName) {
        checkNotNull(layerName, "layer name is null");

        final String layerId;

        lock.acquireReadLock();
        try {
            layerId = getLayerId(layerName);
            if (layerId == null) {
                return Optional.ofNullable(null);
            }
        } finally {
            lock.releaseReadLock();
        }
        return Optional.ofNullable(getTileLayerById(layerId));
    }

    private String getLayerId(final String layerName) {

        String storedName = layerName;
        // check pending modifs first in case name changed
        if (!pendingModications.isEmpty()) {
            for (GeoServerTileLayerInfo info : pendingModications.values()) {
                String name = info.getName();
                if (name.equals(layerName)) {
                    storedName = info.getName();
                    break;
                }
            }
        }

        final String layerId = tileLayerCatalog.getLayerId(storedName);
        if (layerId == null || pendingDeletes.contains(layerId)) {
            return null;
        }
        // name changed?
        GeoServerTileLayerInfo modifiedState = pendingModications.get(layerId);
        if (modifiedState != null && !layerName.equals(modifiedState.getName())) {
            return null;
        }
        return layerId;
    }

    private GeoServerTileLayerInfo getTileLayerInfoByName(final String layerName) {
        GeoServerTileLayerInfo tileLayerInfo = null;

        // check pending modifs first in case name changed
        if (!pendingModications.isEmpty()) {
            for (GeoServerTileLayerInfo info : pendingModications.values()) {
                String name = info.getName();
                if (name.equals(layerName)) {
                    tileLayerInfo = info;
                    break;
                }
            }
        }

        if (null == tileLayerInfo) {
            tileLayerInfo = tileLayerCatalog.getLayerByName(layerName);
            if (null == tileLayerInfo) {
                return null;
            }
            if (pendingDeletes.contains(tileLayerInfo.getId())) {
                return null;
            }
            if (pendingModications.containsKey(tileLayerInfo.getId())) {
                // found in catalog but not in pending modifications, means name changed
                return null;
            }
        }
        return tileLayerInfo;
    }

    /** @see TileLayerConfiguration#getLayerCount() */
    @Override
    public int getLayerCount() {
        int count = 0;
        lock.acquireReadLock();
        try {
            Set<String> layerIds = tileLayerCatalog.getLayerIds();
            if (pendingDeletes.isEmpty()) {
                count = layerIds.size();
            } else {
                for (String layerId : layerIds) {
                    if (pendingDeletes.contains(layerId)) {
                        continue;
                    }
                    ++count;
                }
            }
        } finally {
            lock.releaseReadLock();
        }
        return count;
    }

    @Override
    public void afterPropertiesSet() {
        lock.acquireWriteLock();
        try {
            LOGGER.info("Initializing GWC configuration based on GeoServer's Catalog");
            this.layerCache.invalidateAll();
            this.tileLayerCatalog.initialize();
        } finally {
            lock.releaseWriteLock();
        }
    }

    /**
     * @return {@code true} only if {@code tl instanceof} {@link GeoServerTileLayer} .
     * @see TileLayerConfiguration#canSave(TileLayer)
     */
    @Override
    public boolean canSave(TileLayer tl) {
        return tl instanceof GeoServerTileLayer && (!tl.isTransientLayer());
    }

    public static boolean isLayerExposable(LayerInfo layer) {
        assert layer != null;
        return WMS.isWmsExposable(layer);
    }

    @Override
    public synchronized void addLayer(final TileLayer tl) {
        checkNotNull(tl);
        checkArgument(canSave(tl), "Can't save TileLayer of type ", tl.getClass());
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) tl;
        checkNotNull(tileLayer.getInfo(), "GeoServerTileLayerInfo is null");
        checkNotNull(tileLayer.getInfo().getId(), "id is null");
        checkNotNull(tileLayer.getInfo().getName(), "name is null");

        GeoServerTileLayerInfo info = tileLayer.getInfo();

        PublishedInfo publishedInfo = tileLayer.getPublishedInfo();
        if (publishedInfo instanceof LayerInfo && !isLayerExposable((LayerInfo) publishedInfo)) {
            LOGGER.warning(
                    "Requested layer "
                            + publishedInfo.getName()
                            + " has no geometry. Won't create TileLayer");
            return;
        }

        lock.acquireWriteLock();
        try {
            boolean pending = pendingModications.containsKey(info.getId());
            boolean exists = null != tileLayerCatalog.getLayerById(info.getId());
            boolean notExists = !pending && !exists;

            checkArgument(
                    notExists,
                    "A GeoServerTileLayer named '" + info.getName() + "' already exists");
            if (pendingDeletes.remove(info.getId())) {
                LOGGER.finer(
                        "Adding a new layer "
                                + info.getName()
                                + " before saving the deleted one with the same id");
            }
            pendingModications.put(info.getId(), info);
        } finally {
            lock.releaseWriteLock();
        }
        save();
    }

    /** @see TileLayerConfiguration#modifyLayer(TileLayer) */
    @Override
    public synchronized void modifyLayer(TileLayer tl) throws NoSuchElementException {
        checkNotNull(tl, "TileLayer is null");
        checkArgument(canSave(tl), "Can't save TileLayer of type ", tl.getClass());

        GeoServerTileLayer tileLayer = (GeoServerTileLayer) tl;

        checkNotNull(tileLayer.getInfo(), "GeoServerTileLayerInfo is null");
        checkNotNull(tileLayer.getInfo().getId(), "id is null");
        checkNotNull(tileLayer.getInfo().getName(), "name is null");

        final GeoServerTileLayerInfo info = tileLayer.getInfo();
        lock.acquireWriteLock();
        try {
            final String layerId = info.getId();
            // check pendingModifications too to catch unsaved adds
            boolean exists =
                    pendingModications.containsKey(layerId) || tileLayerCatalog.exists(layerId);
            if (!exists) {
                throw new NoSuchElementException(
                        "No GeoServerTileLayer named '" + info.getName() + "' exists");
            }
            pendingModications.put(layerId, info);
            layerCache.invalidate(layerId);
        } finally {
            lock.releaseWriteLock();
        }
        save();
    }

    /** @see TileLayerConfiguration#renameLayer(String, String) */
    @Override
    public synchronized void renameLayer(String oldName, String newName)
            throws NoSuchElementException {
        TileLayer tl =
                getLayer(oldName)
                        .orElseThrow(
                                () ->
                                        new NullPointerException(
                                                "TileLayer " + oldName + " not found"));
        checkArgument(canSave(tl), "Can't rename TileLayer of type ", tl.getClass());

        GeoServerTileLayer tileLayer = (GeoServerTileLayer) tl;

        checkNotNull(tileLayer.getInfo(), "GeoServerTileLayerInfo is null");
        checkNotNull(tileLayer.getInfo().getId(), "id is null");
        checkNotNull(tileLayer.getInfo().getName(), "name is null");

        final GeoServerTileLayerInfo info = tileLayer.getInfo();
        lock.acquireWriteLock();
        try {
            final String layerId = info.getId();
            info.setName(newName);
            // check pendingModifications too to catch unsaved adds
            boolean exists =
                    pendingModications.containsKey(layerId) || tileLayerCatalog.exists(layerId);
            checkArgument(exists, "No GeoServerTileLayer named '" + info.getName() + "' exists");
            pendingModications.put(layerId, info);
            layerCache.invalidate(layerId);
        } finally {
            lock.releaseWriteLock();
        }
        save();
    }

    /**
     * {@link TileLayerDispatcher} is requesting to remove the layer named after {@code layerName}
     *
     * @see TileLayerConfiguration#removeLayer(String)
     */
    @Override
    public void removeLayer(final String layerName) throws NoSuchElementException {
        checkNotNull(layerName);
        lock.acquireWriteLock();
        try {
            GeoServerTileLayerInfo tileLayerInfo = getTileLayerInfoByName(layerName);
            if (tileLayerInfo != null) {
                final String layerId = tileLayerInfo.getId();
                pendingModications.remove(layerId);
                // cache removal must occur before layerId is added to pendingDeletes
                // otherwise brokers and blob stores will not be able to obtain
                // the tile layer information they need to perform cache removal
                // because the CatalogConfiguration will treat the layer as if
                // it no longer exists
                try {
                    GWC.get().layerRemoved(tileLayerInfo.getName());
                } catch (RuntimeException e) {
                    LOGGER.log(
                            Level.SEVERE,
                            "Error deleting tile layer '"
                                    + tileLayerInfo.getName()
                                    + "' from cache",
                            e);
                }
                pendingDeletes.add(layerId);
                layerCache.invalidate(layerId);
            } else {
                throw new NoSuchElementException("Tile layer " + layerName + " does not exist");
            }
        } finally {
            lock.releaseWriteLock();
        }
        save();
    }

    private synchronized void save() {

        final GWC mediator = GWC.get();

        final List<GeoServerTileLayerInfo[ /* old, new */]> modifications = Lists.newLinkedList();

        lock.acquireWriteLock();
        // perform the transaction while holding the write lock, then downgrade to the read lock and
        // issue the modification events (otherwise another thread asking for any changed layer
        // would lock)
        try {
            for (String deletedId : pendingDeletes) {
                try {
                    tileLayerCatalog.delete(deletedId);
                } catch (RuntimeException e) {
                    LOGGER.log(
                            Level.SEVERE,
                            "Error deleting tile layer '" + deletedId + "' from catalog",
                            e);
                }
            }

            for (GeoServerTileLayerInfo modified : pendingModications.values()) {
                final GeoServerTileLayerInfo old;
                try {
                    old = tileLayerCatalog.save(modified);
                    modifications.add(new GeoServerTileLayerInfo[] {old, modified});
                } catch (RuntimeException e) {
                    LOGGER.log(
                            Level.SEVERE,
                            "Error saving tile layer '" + modified.getName() + "'",
                            e);
                }
            }
            this.pendingModications.clear();
            this.pendingDeletes.clear();
        } finally {
            // Downgrade to read
            lock.downgradeToReadLock();
            try {
                // issue notifications
                for (GeoServerTileLayerInfo[] oldNew : modifications) {
                    final GeoServerTileLayerInfo old = oldNew[0];
                    final GeoServerTileLayerInfo modified = oldNew[1];
                    try {
                        if (old == null) {
                            // it's an addition
                            String layerName = modified.getName();
                            mediator.layerAdded(layerName);
                        } else {
                            // it's a modification
                            issueTileLayerInfoChangeNotifications(old, modified);
                        }
                    } catch (RuntimeException e) {
                        LOGGER.log(
                                Level.SEVERE,
                                "Error issuing change events for tile layer "
                                        + modified
                                        + ".  This may result in leaked tiles that will not be truncated.",
                                e);
                    }
                }
            } finally {
                lock.releaseReadLock();
            }
        }
    }

    private void issueTileLayerInfoChangeNotifications(
            final GeoServerTileLayerInfo oldInfo, final GeoServerTileLayerInfo newInfo) {

        checkNotNull(oldInfo);
        checkNotNull(newInfo);
        checkNotNull(oldInfo.getName());
        checkNotNull(newInfo.getName());
        checkNotNull(oldInfo.getId());
        checkNotNull(newInfo.getId());
        checkArgument(equal(oldInfo.getId(), newInfo.getId()));

        final GWC mediator = GWC.get();

        final String oldLayerName = oldInfo.getName();
        final String layerName = newInfo.getName();

        final boolean isRename = !equal(oldLayerName, layerName);
        if (isRename) {
            mediator.layerRenamed(oldLayerName, layerName);
        }
        // FIXME: There should be a way to ask GWC to "truncate redundant caches" rather than doing
        //         all this detective work.

        // First, remove the entire layer cache for any removed gridset
        Set<XMLGridSubset> oldGridSubsets = oldInfo.getGridSubsets();
        Set<XMLGridSubset> newGridSubsets = newInfo.getGridSubsets();

        Set<String> oldGridSubsetNames = gridsetNames(oldGridSubsets);
        Set<String> newGridSubsetNames = gridsetNames(newGridSubsets);

        Set<String> removedGridSets = new HashSet<String>(oldGridSubsetNames);
        removedGridSets.removeAll(newGridSubsetNames);
        for (String removedGridset : removedGridSets) {
            mediator.deleteCacheByGridSetId(layerName, removedGridset);
        }

        // then proceed with any removed cache format/style on the remaining gridsets
        Set<String> oldFormats = new HashSet<String>(oldInfo.getMimeFormats());
        Set<String> newFormats = new HashSet<String>(newInfo.getMimeFormats());
        if (!oldFormats.equals(newFormats)) {
            oldFormats.removeAll(newFormats);
            for (String removedFormat : oldFormats) {
                String styleName = null;
                String gridSetName = null;
                BoundingBox bounds = null;
                mediator.truncate(layerName, styleName, gridSetName, bounds, removedFormat);
            }
        }

        Set<String> oldStyles = oldInfo.cachedStyles();
        Set<String> newStyles = newInfo.cachedStyles();

        if (!newStyles.equals(oldStyles)) {
            oldStyles = new HashSet<String>(oldStyles);
            oldStyles.removeAll(newStyles);
            for (String removedStyle : oldStyles) {
                mediator.truncateByLayerAndStyle(layerName, removedStyle);
            }
        }
    }

    private Set<String> gridsetNames(Set<XMLGridSubset> gridSubsets) {
        Set<String> names = new HashSet<String>();
        for (XMLGridSubset gridSubset : gridSubsets) {
            names.add(gridSubset.getGridSetName());
        }
        return names;
    }

    /** Invalidates the later cache and resets the tile layer catalog. */
    public void reset() {
        lock.acquireWriteLock();
        try {
            this.layerCache.invalidateAll();
            this.tileLayerCatalog.reset();
        } finally {
            lock.releaseWriteLock();
        }
    }

    /**
     * Helper method that will remove the workspace prefix from a layer name. If the layer is not
     * prefixed by an workspace name the layer name will be returned as is.
     */
    public static String removeWorkspacePrefix(String layerName, Catalog catalog) {
        // checking if we have an workspace prefix
        int workspaceSeparatorIndex = layerName.indexOf(":");
        if (workspaceSeparatorIndex >= 0 && workspaceSeparatorIndex + 1 < layerName.length()) {
            // let's check if we really have a workspace name as prefix
            String workspaceName = layerName.substring(0, workspaceSeparatorIndex);
            if (catalog.getWorkspaceByName(workspaceName) != null) {
                // we really have an workspace as prefix so let's remove it
                return layerName.substring(workspaceSeparatorIndex + 1);
            }
        }
        // we are already good
        return layerName;
    }

    /** @see TileLayerConfiguration#getLocation() */
    @Override
    public String getLocation() {
        return this.tileLayerCatalog.getPersistenceLocation();
    }

    /** @see ReinitializingBean#deinitialize() */
    @Override
    public void deinitialize() throws Exception {}

    /**
     * Sets the {@link GridSetBroker} for use by this configuration. Automatically called by spring
     * on context initialization.
     *
     * @param broker The GridSet broker
     */
    @Autowired
    @Override
    public void setGridSetBroker(@Qualifier("gwcGridSetBroker") GridSetBroker broker) {
        this.gridSetBroker = broker;
    }
}
