/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.media.jai.operator.TranslateDescriptor;

import org.geoserver.coverage.layer.CoverageMetaTile;
import org.geoserver.coverage.layer.CoverageTileLayer;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.OutsideCoverageException;
import org.geowebcache.storage.StorageBroker;
import org.jaitools.imageutils.ImageLayout2;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Composing class. It allows to retrieve ConveyorTiles from a specific indexes subset.
 * Moreover, it does the tiles composition.
 */
public class CoverageTileComposer {

    private static final float ZERO = 0f;

    private static final float HALF_FACTOR = 0.5f;

    private final static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(CoverageTileComposer.class);

    /** A GridCoverageFactory instance used to instantiate GridCoverages */
    static final GridCoverageFactory gcf = new GridCoverageFactory();

    private CoverageTileLayer coverageTileLayer;

    public CoverageTileComposer(CoverageTileLayer coverageTileLayer) {
        this.coverageTileLayer = coverageTileLayer;
    }

    public Map<String, ConveyorTile> getTiles(final long[] tiles, final GridSet gridSet,
            final StorageBroker storageBroker, final Map<String, String> filteringParameters)
            throws GeoWebCacheException, IOException {
        final int minX = (int) tiles[0];
        final int minY = (int) tiles[1];
        final int maxX = (int) tiles[2];
        final int maxY = (int) tiles[3];
        final int level = (int) tiles[4];
        return getTiles(minX, minY, maxX, maxY, level, gridSet, storageBroker, filteringParameters);
    }

    /**
     * Return the ConveyorTile involved by the subset of tile Indexes.
     * 
     * @param minX
     * @param minY
     * @param maxX
     * @param maxY
     * @param level
     * @param gridSet
     * @param filteringParameters
     * @return
     * @throws GeoWebCacheException
     * @throws IOException
     */
    public Map<String, ConveyorTile> getTiles(final long minX, final long minY, final long maxX,
            final long maxY, final long level, final GridSet gridSet,
            final StorageBroker storageBroker, Map<String, String> filteringParameters)
            throws GeoWebCacheException, IOException {
        final String gridSetName = gridSet.getName();

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Getting tiles in the range\n" + "minX:" + minX + "\nminY:" + minY
                    + "maxX:" + maxX + "\nmaxY:" + maxY + "z:" + level);
        }

        String id = coverageTileLayer.getName();
        Map<String, ConveyorTile> cTiles = new HashMap<String, ConveyorTile>();
        ConveyorTile ct;
        long[] indexes = new long[3];
        for (long i = minX; i <= maxX; i++) {
            for (long j = minY; j <= maxY; j++) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest("Getting tile (" + i + "," + j + "," + level + ")");
                }
                indexes[0] = i;
                indexes[1] = j;
                indexes[2] = level;
                ct = new ConveyorTile(storageBroker, id, gridSetName, indexes ,
                        CoverageTileLayer.TIFF_MIME_TYPE, filteringParameters, null, null);
                try {
                    ConveyorTile tile = coverageTileLayer.getTile(ct);
                    String index = i + "_" + j;
                    cTiles.put(index, tile);
                } catch (OutsideCoverageException oce) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Exception occurred while getting tile (" + i + "," + j + ","
                                + level + ") due to " + oce);
                    }
                }
            }
        }
        indexes = null;
        return cTiles;
    }

    /**
     * Compose a RenderedImage for the current Tile level, assuming it should be created
     *  from the 4 tiles coming from the higher resolution level.
     * 
     * @param cTiles
     * @param x
     * @param y
     * @param z
     * @param gridSet
     * @param interpolation
     * @return
     * @throws IOException
     */
    public RenderedImage buildTileImageFromUpperLevel(Map<String, ConveyorTile> cTiles, long x,
            long y, long z, GridSet gridSet, Interpolation interpolation) throws IOException {
        final long minX = x * 2;
        final long minY = y * 2;
        final long maxY = minY + 1;

        final int tileWidth = gridSet.getTileWidth();
        final int tileHeight = gridSet.getTileHeight();

        // setup tile set to satisfy the request
        final Set<String> keys = cTiles.keySet();
        RenderedImage outputTile;
        if (!keys.isEmpty()) {
            int i = 0;
            RenderedImage sources[] = new RenderedImage[4];
            for (String key : keys) {
                final ConveyorTile componentTile = cTiles.get(key);
                final RenderedImage ri = CoverageMetaTile.getResource(componentTile);
                final String indexes[] = key.split("_");
                final int xIndex = Integer.parseInt(indexes[0]);
                final int yIndex = Integer.parseInt(indexes[1]);
                final float translateX = (xIndex - minX) * tileWidth;
                final float translateY = (maxY - yIndex) * tileHeight;

                // Getting the parent tiles and translate them to setup the
                // proper tiles layout before the scaling operation
                sources[i++] = TranslateDescriptor.create(ri, translateX, translateY,
                        Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
            }

            // Mosaic these 4 tiles to get the current tile.
            // -------------------------------------------------------------------------------
            // TODO:We should arrange the ConveyorTilesRenderedImage to delegate the job to it
            // -------------------------------------------------------------------------------
            final RenderedImage mosaicked = MosaicDescriptor.create(sources,
                    MosaicDescriptor.MOSAIC_TYPE_BLEND, null, null, null, null, null);
            // RenderedImage mosaicked = null;
            //
            // try{
            // mosaicked = new ConveyorTilesRenderedImage(cTiles, gridSet, gridSubset, layout);
            // }catch (Exception e){
            // throw new RuntimeException(e);
            // }

            // create the current Tile from the previous 4 using a scale which
            outputTile = ScaleDescriptor.create(mosaicked, HALF_FACTOR, HALF_FACTOR, ZERO, ZERO,
                    interpolation, null);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Creating constant image for tile with coordinates: x = " + x + " y = "
                        + y + " z = " + z);
            }
            outputTile = CoverageMetaTile.createConstantImage(coverageTileLayer.getLayout()
                    .getSampleModel(null), tileWidth, tileHeight, null, coverageTileLayer.getNoData());
        }
        return outputTile;
    }

    /**
     * Compose a GridCoverage on top of the provided tiles for the specifed envelope.
     * 
     * @param coverageName
     * @param tiles
     * @param cTiles
     * @param gridSet
     * @param envelope
     * @param zoomLevel
     * @return
     * @throws IOException
     */
    public GridCoverage2D composeGridCoverage(String coverageName, long[] tiles,
            Map<String, ConveyorTile> cTiles, GridSet gridSet, Envelope envelope, Integer zoomLevel,
            final boolean axisOrderingTopDown)
            throws IOException {
        final int minX = (int) tiles[0];
        final int minY = (int) tiles[1];
        final int maxX = (int) tiles[2];
        final int maxY = (int) tiles[3];

        final int wTiles = maxX - minX + 1;
        final int hTiles = maxY - minY + 1;
        final int tileHeight = gridSet.getTileHeight();
        final int tileWidth = gridSet.getTileWidth();
        final String gridSetName = gridSet.getName();

        GridSubset subset = coverageTileLayer.getGridSubset(gridSetName);
        ImageLayout layout = new ImageLayout2(minX, minY, tileWidth * wTiles, tileHeight * hTiles,
                0, 0, tileWidth, tileHeight, coverageTileLayer.getLayout().getSampleModel(null),
                null);

        // setup tile set to satisfy the request
        CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();

        // Reassembling tiles
        ConveyorTilesRenderedImage finalImage = new ConveyorTilesRenderedImage(cTiles, layout,
                axisOrderingTopDown, wTiles, hTiles, zoomLevel, gridSet, subset, crs);
        ReferencedEnvelope readEnvelope = finalImage.getEnvelope();
        final SampleModel sampleModel = finalImage.getSampleModel();
        final int numBands = sampleModel.getNumBands();
        final GridSampleDimension bands[] = new GridSampleDimension[numBands];

        for (int i = 0; i < numBands; i++) {
            bands[i] = new GridSampleDimension("band" + i);
        }

        // TODO: Check nodata management
        return gcf.create(coverageName, finalImage, readEnvelope, bands, null, null);
    }
}
