/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage.layer;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.coverage.CoverageTileComposer;
import org.geoserver.coverage.WCSSourceHelper;
import org.geoserver.coverage.configuration.CoverageConfiguration;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.InterpolationType;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.SeedingPolicy;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.TiffCompression;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.OutsideCoverageException;
import org.geowebcache.locks.LockProvider.Lock;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.util.GWCVars;

/**
 * A tile layer backed by a WCS server
 */
public class CoverageTileLayer extends GeoServerTileLayer {

    private final static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(CoverageTileLayer.class);

    private transient WCSSourceHelper sourceHelper;

    private transient CoverageInfo coverageInfo;

    protected String name;

    protected Map<String, GridSubset> subSets;

    private ImageLayout layout;

    private String workspaceName;

    private ReferencedEnvelope bbox;

    private String coverageName;

    private Double noData = null;

    private SeedingPolicy seedingPolicy = SeedingPolicy.RECURSIVE;

    private Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_NEAREST);

    private CoverageTileLayerInfo coverageTileLayerInfo;

    private OverviewPolicy overviewPolicy = OverviewPolicy.QUALITY;

    private TiffCompression tiffCompression  = TiffCompression.DEFLATE;

    public static final MimeType TIFF_MIME_TYPE;

    static {
        try {
            TIFF_MIME_TYPE = MimeType.createFromExtension("tiff");
        } catch (MimeException e) {
            throw new RuntimeException("Exception occurred while getting TIFF mimetype", e);
        }
    }

    public CoverageTileLayer(CoverageInfo info, GridSetBroker broker, List<GridSubset> gridSubsets,
            GeoServerTileLayerInfo tileLayerInfo, boolean init) throws Exception {
        super(new LayerGroupInfoImpl(), broker, tileLayerInfo);

        subSets = new HashMap<String, GridSubset>();
        for(GridSubset gridSubset : gridSubsets){
            subSets.put(gridSubset.getName(), gridSubset);
        }

        final CoverageStoreInfo storeInfo = info.getStore();
        this.coverageInfo = info;

        List<Double> nullValues = info.getDimensions().get(0).getNullValues();
        if (nullValues != null && !nullValues.isEmpty()) {
            noData = nullValues.get(0);
        }
        workspaceName = storeInfo.getWorkspace().getName();
        coverageName = info.getName();
        name = workspaceName + ":" + coverageName;
        bbox = info.boundingBox();
        sourceHelper = new WCSSourceHelper(this);

        if (tileLayerInfo instanceof CoverageTileLayerInfo){
            this.coverageTileLayerInfo = (CoverageTileLayerInfo) tileLayerInfo;
        } else {
            this.coverageTileLayerInfo = new CoverageTileLayerInfoImpl(tileLayerInfo);
        }
        if (init) {
            coverageTileLayerInfo.setId(info.getId());
            coverageTileLayerInfo.setName(name + CoverageConfiguration.COVERAGE_LAYER_SUFFIX);
            coverageTileLayerInfo.getMimeFormats().add("image/tiff");
        }
        seedingPolicy = coverageTileLayerInfo.getSeedingPolicy();
        tiffCompression = coverageTileLayerInfo.getTiffCompression();
        InterpolationType interpolationType = coverageTileLayerInfo.getInterpolationType();
        interpolation = interpolationType != null ? interpolationType.getInterpolationObject() : null;
        overviewPolicy = coverageTileLayerInfo.getOverviewPolicy();
    }

    @Override
    public Set<String> getGridSubsets() {
        return Collections.unmodifiableSet(this.subSets.keySet());
    }

    @Override
    public GridSubset getGridSubset(String gridSetId) {
        return subSets.get(gridSetId);
    }

    public String getCoverageName() {
        return coverageName;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public ReferencedEnvelope getBbox() {
        return bbox;
    }

    @Override
    public GeoServerTileLayerInfo getInfo() {
        // Wrap the GeoServerTileLayerInfo into a CoverageTileLayerInfo
       return coverageTileLayerInfo;
    }

    public CoverageInfo getCoverageInfo() {
        return coverageInfo;
    }

    public void setLayout(ImageLayout layout) {
        this.layout = layout;
    }

    /**
     * Used for seeding
     */
    public void seedTile(ConveyorTile tile, boolean tryCache) throws GeoWebCacheException,
            IOException {
        GridSubset gridSubset = getGridSubset(tile.getGridSetId());
        long[] index = tile.getTileIndex();
        long zLevel = index[2];
        if (gridSubset.shouldCacheAtZoom(zLevel)) {
            if (seedingPolicy == SeedingPolicy.DIRECT || zLevel >= gridSubset.getZoomStop()) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Seeding tile (DIRECT method): " + tile);
                }
                // Always use metaTiling on seeding since we are implementing our
                // custom GWC layer
                getMetatilingReponse(tile, tryCache, coverageTileLayerInfo.getMetaTilingX(),
                        coverageTileLayerInfo.getMetaTilingY());
            } else {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Seeding tile (recursive method): " + tile);
                }
                recurseTile(tile);
            }
        }
    }

    /**
     * This method returns the requested tile by using a recursive approach.
     * In case the requested tile isn't available in cache, it asks for the
     * 4 tiles from the higher resolution level. Then it computes the current
     * tile from them by applying a scale factor of 0.5 across both dimensions.
     * 
     * @param tile
     * @return
     * @throws GeoWebCacheException
     * @throws IOException
     */
    private ConveyorTile recurseTile(ConveyorTile tile) throws GeoWebCacheException, IOException {
        final GridSubset gridSubset = getGridSubset(tile.getGridSetId());
        final GridSet gridSet = gridSubset.getGridSet();
        final long[] tileIndex = tile.getTileIndex();
        final StorageBroker storageBroker = tile.getStorageBroker();

        // Getting current tile indexes
        final long x = tileIndex[0];
        final long y = tileIndex[1];
        final long originalZ = tileIndex[2];

        // Setting indexes for 4 tiles coming from the higher resolution level
        final long z = originalZ + 1; 
        final long minX = x * 2;
        final long maxX = minX + 1;
        final long minY = y * 2;
        final long maxY = minY + 1;

        final Map<String, String> parameters = tile.getFullParameters();

        // Accessing the 4 tiles from the upper level to obtain this tile
        CoverageTileComposer composer = new CoverageTileComposer(this);
        Map<String, ConveyorTile> cTiles = composer.getTiles(minX, minY, maxX, maxY, z, gridSet, storageBroker, parameters);
        RenderedImage outputTile = composer.buildTileImageFromUpperLevel(cTiles, x, y, originalZ, gridSet, interpolation);

        // Create a tile on top of the generated image and save it to store.
        CoverageMetaTile metaTile = null;
        try {
            // Note we use a fake metaTile of size 1,1 to use the storing machinery
            metaTile = new CoverageMetaTile(this, gridSubset, TIFF_MIME_TYPE, tile.getTileIndex(),
                    1, 1, parameters, 0);
            metaTile.setImage(outputTile);
            saveTiles(metaTile, tile, System.currentTimeMillis());
            return tile;
        } finally {
            metaTile.dispose();
            cleanUpThreadLocals();
            outputTile = null;
        }
    }

    private ConveyorTile getMetatilingReponse(ConveyorTile tile, final boolean tryCache,
            final int metaX, final int metaY) throws GeoWebCacheException, IOException {

        final CoverageMetaTile metaTile = createMetaTile(tile, metaX, metaY);
        Lock lock = null;
        try {
            /** ****************** Acquire lock ******************* */
            lock = GWC.get().getLockProvider().getLock(buildLockKey(tile, metaTile));
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
                            + " submitting request for meta grid location "
                            + Arrays.toString(metaTile.getMetaGridPos()) + " on " + metaTile);
                }
                try {
                    long requestTime = System.currentTimeMillis();
                    sourceHelper.makeRequest(metaTile, tile, interpolation, overviewPolicy, noData);
                    saveTiles(metaTile, tile, requestTime);
                } catch (Exception e) {
                    throw new GeoWebCacheException("Problem communicating with GeoServer", e);
                } finally {
                    cleanUpThreadLocals();
                }
            }
            /** ****************** Return lock and response ****** */
        } finally {
            if (lock != null) {
                lock.release();
            }
            metaTile.dispose();
        }
        return /*finalizeTile(*/tile;/*);*/
    }

    private String buildLockKey(ConveyorTile tile, CoverageMetaTile metaTile) {
        StringBuilder metaKey = new StringBuilder();

        final long[] tileIndex;
        if (metaTile != null) {
            tileIndex = metaTile.getMetaGridPos();
            metaKey.append("meta_");
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

    public boolean tryCacheFetch(ConveyorTile tile) {
        int expireCache = this.getExpireCache((int) tile.getTileIndex()[2]);
        if (expireCache != GWCVars.CACHE_DISABLE_CACHE) {
            try {
                return tile.retrieve(expireCache * 1000L);
            } catch (GeoWebCacheException gwce) {
                LOGGER.severe(gwce.getMessage());
                tile.setErrorMsg(gwce.getMessage());
                return false;
            }
        }
        return false;
    }

    @Override
    public ConveyorTile doNonMetatilingRequest(ConveyorTile tile) throws GeoWebCacheException {
        // We are doing our custom GWC layer implementation for gridCoverage setup
        throw new UnsupportedOperationException();
    }

    @Override
    public ConveyorTile getNoncachedTile(ConveyorTile tile) throws GeoWebCacheException {
        // We are doing our custom GWC layer implementation for gridCoverage setup
        throw new UnsupportedOperationException();
    }

    public void setSourceHelper(WCSSourceHelper source) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Setting sourceHelper on " + this.name);
        }
        this.sourceHelper = source;
    }

    public void cleanUpThreadLocals() {
        WMS_BUFFER.remove();
        WMS_BUFFER2.remove();
    }

    @Override
    public String getStyles() {
        // Styles are ignored since we are dealing with raw coverages on this kind of tileLayer
        return null;
    }

    @Override
    public ConveyorTile getTile(ConveyorTile tile) throws GeoWebCacheException, IOException,
            OutsideCoverageException {

        final String tileGridSetId = tile.getGridSetId();
        final GridSubset gridSubset = getGridSubset(tileGridSetId);
        if (gridSubset == null) {
            throw new IllegalArgumentException("Requested gridset not found: " + tileGridSetId);
        }
        final long[] tileIndex = tile.getTileIndex();
        checkNotNull(tileIndex);
        final int zLevel = (int) tileIndex[2];
        tile.setMetaTileCacheOnly(!gridSubset.shouldCacheAtZoom(zLevel));
        ConveyorTile returnTile = null;
        try {
            if (tryCacheFetch(tile)) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest("Requested tile is available. (x = "
                            + tileIndex[0] + "; y = " + tileIndex[1] + 
                            "; z = " + tileIndex[2] + "\nReturning it");
                }
                return /* finalizeTile( */tile;/* ); */
            }

            final int numLevels = gridSubset.getGridSet().getNumLevels();

            // Final preflight check, throws OutsideCoverageException if necessary
            gridSubset.checkCoverage(tileIndex);

            // Differentiate between direct seeding and recursive one.
            if (seedingPolicy == SeedingPolicy.DIRECT || zLevel == numLevels - 1
                    || zLevel == gridSubset.getZoomStart()) {
                returnTile = getMetatilingReponse(tile, true,
                        coverageTileLayerInfo.getMetaTilingX(),
                        coverageTileLayerInfo.getMetaTilingY());
            } else {
                returnTile = recurseTile(tile);
            }

        } finally {
            // Clean up the buffers
            cleanUpThreadLocals();
        }

        sendTileRequestedEvent(returnTile);
        return returnTile;
    }

    /**
     * Create a metatile given the metaTile size and the reference conveyor tile.
     * @param tile
     * @param metaX
     * @param metaY
     * @return
     */
    private CoverageMetaTile createMetaTile(ConveyorTile tile, final int metaX, final int metaY) {
        CoverageMetaTile metaTile;

        final String tileGridSetId = tile.getGridSetId();
        final GridSubset gridSubset = getGridSubset(tileGridSetId);
        final MimeType responseFormat = tile.getMimeType();
        long[] tileGridPosition = tile.getTileIndex();
        metaTile = new CoverageMetaTile(this, gridSubset, responseFormat, 
                tileGridPosition, metaX, metaY, tile.getFullParameters(), getInfo().getGutter());

        return metaTile;
    }

    public ImageLayout getLayout() {
        return layout;
    }

    public TiffCompression getTiffCompression() {
        return tiffCompression;
    }

    public WCSSourceHelper getSourceHelper() {
        return sourceHelper;
    }

    public Double getNoData() {
        return noData;
    }

    @Override
    public boolean isAdvertised() {
        // Coverage tile layer aren't advertised by default.
        // We won't deal with them as standard GWC layers.
        // They are special ones which aren't exposed
        return false;
    }

    @Override
    public void setAdvertised(boolean advertised) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Note that CoverageTileLayer aren't never advertised. "
                    + "Calling this method has no effects");
        }
        return;
    }

    @Override
    public boolean isTransientLayer() {
        // Coverage tile layer are transient because they are not saved in the GWC catalog
        return true;
    }

    @Override
    public void setTransientLayer(boolean transientLayer) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Note that CoverageTileLayer are always transient. "
                    + "Calling this method has no effects");
        }
        return;
    }
}
