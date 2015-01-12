/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage.configuration;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.coverage.layer.CoverageTileLayer;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geowebcache.config.Configuration;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.layer.TileLayer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class CoverageConfiguration extends CatalogConfiguration implements Configuration {

    /** A suffix used to distinguish between standard GWC layers and coverage rasters */ 
    public static String COVERAGE_LAYER_SUFFIX = "cov";

    private GridSetBroker gwcGridSetBroker;

    private Catalog gsCatalog;

    public CoverageConfiguration(Catalog catalog, TileLayerCatalog tileLayerCatalog,
            GridSetBroker gridSetBroker) {
        super(catalog, tileLayerCatalog, gridSetBroker);
        this.gsCatalog = catalog;
        this.gwcGridSetBroker = gridSetBroker;

        ReadWriteLock lock = null;
        Set<String> pendingDeletes = null;
        Map<String, GeoServerTileLayerInfo> pendingModications = null;
        try {
            // Use reflection
            Field lockRef = this.getClass().getSuperclass().getDeclaredField("lock");
            Field pendingDeletesRef = this.getClass().getSuperclass()
                    .getDeclaredField("pendingDeletes");
            Field pendingModicationsRef = this.getClass().getSuperclass()
                    .getDeclaredField("pendingModications");

            lockRef.setAccessible(true);
            pendingDeletesRef.setAccessible(true);
            pendingModicationsRef.setAccessible(true);

            lock = (ReadWriteLock) lockRef.get(CoverageConfiguration.this);
            pendingDeletes = (Set<String>) pendingDeletesRef.get(CoverageConfiguration.this);
            pendingModications = (Map<String, GeoServerTileLayerInfo>) pendingModicationsRef
                    .get(CoverageConfiguration.this);

        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException
                | IllegalAccessException e) {
            throw new RuntimeException(e);

        }

        LoadingCache<String, GeoServerTileLayer> layerCache = CacheBuilder.newBuilder()//
                .concurrencyLevel(10)//
                .expireAfterAccess(10, TimeUnit.MINUTES)//
                .initialCapacity(10)//
                .maximumSize(100)
                //
                .build(new CoverageTileLayerLoader(tileLayerCatalog, pendingDeletes, gridSetBroker,
                        catalog, pendingModications, lock));

        try {
            Field cache = this.getClass().getSuperclass().getDeclaredField("layerCache");
            cache.setAccessible(true);
            cache.set(CoverageConfiguration.this, layerCache);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
                | SecurityException e) {
            throw new RuntimeException(e);
        }
        // Add the new Listener for the CoverageConfiguration

    }

    /**
     * @return {@code true} only if {@code tl instanceof} {@link CoverageTileLayer} .
     * @see org.geowebcache.config.Configuration#canSave(org.geowebcache.layer.TileLayer)
     */
    @Override
    public boolean canSave(TileLayer tl) {
        return tl instanceof CoverageTileLayer;
    }

    @Override
    public GeoServerTileLayer getTileLayerById(final String layerId) {
        checkNotNull(layerId, "layer id is null");

        GeoServerTileLayer tileLayerById = super.getTileLayerById(layerId);
        CoverageTileLayer layer = null;
        if (!(tileLayerById instanceof CoverageTileLayer)) {
            GeoServerTileLayerInfo info = tileLayerById.getInfo();

            // GridSubset gridSubSet = GridSubsetFactory.createGridSubSet(gwcGridSetBroker.getGridSets()
            // .get(0));
            List<GridSubset> gridSubsets = parseGridSubsets(gwcGridSetBroker, info);

            CoverageInfo coverageInfo = gsCatalog.getCoverageByName(info.getName());

            try {
                layer = new CoverageTileLayer(coverageInfo, gwcGridSetBroker, gridSubsets, info,
                        false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            layer = (CoverageTileLayer) tileLayerById;
        }

        return layer;
    }

    /**
     * {@link GeoServerTileLayer} cache loader
     * 
     */
    static class CoverageTileLayerLoader extends CacheLoader<String, GeoServerTileLayer> {
        private final TileLayerCatalog tileLayerCatalog;

        private final GridSetBroker gridSetBroker;

        private final Catalog geoServerCatalog;

        private final Map<String, GeoServerTileLayerInfo> pendingModications;

        private final Set<String> pendingDeletes;

        private final ReadWriteLock lock;

        private CoverageTileLayerLoader(TileLayerCatalog tileLayerCatalog,
                Set<String> pendingDeletes, GridSetBroker gridSetBroker, Catalog geoServerCatalog,
                Map<String, GeoServerTileLayerInfo> pendingModications, ReadWriteLock lock) {
            this.tileLayerCatalog = tileLayerCatalog;
            this.gridSetBroker = gridSetBroker;
            this.geoServerCatalog = geoServerCatalog;
            this.lock = lock;
            this.pendingDeletes = pendingDeletes;
            this.pendingModications = pendingModications;
        }

        @Override
        public GeoServerTileLayer load(String layerId) throws Exception {
            GeoServerTileLayer tileLayer = null;

            lock.readLock().lock();
            try {
                if (pendingDeletes.contains(layerId)) {
                    throw new IllegalArgumentException("Tile layer '" + layerId + "' was deleted.");
                }
                GeoServerTileLayerInfo tileLayerInfo = pendingModications.get(layerId);
                if (tileLayerInfo == null) {
                    tileLayerInfo = tileLayerCatalog.getLayerById(layerId);
                }
                if (tileLayerInfo == null) {
                    throw new IllegalArgumentException("GeoServerTileLayerInfo '" + layerId
                            + "' does not exist.");
                }
                List<GridSubset> subsets = parseGridSubsets(gridSetBroker, tileLayerInfo);
                CoverageInfo coverage = geoServerCatalog.getCoverage(layerId);
                tileLayer = new CoverageTileLayer(coverage, gridSetBroker, subsets, tileLayerInfo,
                        false);
                ResourcePool pool = geoServerCatalog.getResourcePool();
                Hints hints = new Hints(GeoTools.getDefaultHints());
                hints.add(new Hints(ResourcePool.SKIP_COVERAGE_EXTENSIONS_LOOKUP, true));

                String name = coverage.getNativeCoverageName();
                if (name == null) {
                    name = coverage.getName();

                }
                GridCoverage2DReader reader = (GridCoverage2DReader) pool.getGridCoverageReader(
                        coverage, name, hints);
                ((CoverageTileLayer) tileLayer).setLayout(reader.getImageLayout());

            } finally {
                lock.readLock().unlock();
            }
            if (null == tileLayer) {
                throw new IllegalArgumentException("GeoServer layer or layer group '" + layerId
                        + "' does not exist");
            }
            return tileLayer;
        }

    }

    /**
     * Parse GridSubsets from XML associated to the {@link GeoServerTileLayerInfo} object
     * 
     * @param gridsetBroker
     * @param tileLayerInfo
     * @return
     */
    public static List<GridSubset> parseGridSubsets(GridSetBroker gridsetBroker,
            GeoServerTileLayerInfo tileLayerInfo) {
        Set<XMLGridSubset> subset = tileLayerInfo.getGridSubsets();
        Iterator<XMLGridSubset> subsetIt = subset.iterator();
        List<GridSubset> subSets = new ArrayList<GridSubset>();
        while (subsetIt.hasNext()) {
            final XMLGridSubset element = subsetIt.next();
            final GridSet gridset = gridsetBroker.get(element.getGridSetName());
            subSets.add(GridSubsetFactory.createGridSubSet(gridset, element.getExtent(),
                    element.getZoomStart(), element.getZoomStop()));
        }
        return subSets;
    }
}
