/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.TranslateDescriptor;

import org.geoserver.coverage.layer.CoverageMetaTile;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.jaitools.imageutils.ImageLayout2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ConveyorTilesRenderedImage extends PlanarImage implements RenderedImage {

    private final static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(ConveyorTilesRenderedImage.class);

    private ReferencedEnvelope envelope;

    private Map<String, ConveyorTile> cTiles;

    private int cTileStartX;

    private long cTileStartY;

    private final boolean axisOrderingTopDown;

    public ReferencedEnvelope getEnvelope() {
        return envelope;
    }

    public ConveyorTilesRenderedImage(Map<String, ConveyorTile> cTiles, ImageLayout layout,
            boolean axisOrderingTopDown, int wTiles, int hTiles, Integer zoomLevel,
            GridSet gridSet, GridSubset subset, CoordinateReferenceSystem crs) throws IOException {
        super(prepareLayout(layout, cTiles, axisOrderingTopDown, gridSet, zoomLevel, wTiles, hTiles),null,null);

        // Value used for defining the Mosaicked image Y origin
        long minYImage = Integer.MAX_VALUE;
        // Maximum Y value for the gridset at the level defined by the zoomlevel variable
        long maxYTotal = gridSet.getGrid(zoomLevel).getNumTilesHigh() - 1;

        // Setting axis ordering
        this.axisOrderingTopDown = axisOrderingTopDown;
        // Definition of the minimum value used for translating the tiles 
        if (axisOrderingTopDown) {
            minYImage = 0;
        }

        final int localMinX = layout.getMinX(null);
        final int localMinY = layout.getMinY(null);

        long currentMinY = minYImage;
        // Definition of the extent
        BoundingBox extent = null;
        double minBBX = Double.POSITIVE_INFINITY;
        double minBBY = Double.POSITIVE_INFINITY;
        double maxBBX = Double.NEGATIVE_INFINITY;
        double maxBBY = Double.NEGATIVE_INFINITY;
        Set<String> keys = cTiles.keySet();

        this.cTiles = cTiles;
        ConveyorTile tile = null;

        // Setting up the boundingBox
        for (String key : keys) {
            tile = cTiles.get(key);
            long[] tileIndex = tile.getTileIndex();
            extent = subset.boundsFromIndex(tileIndex);
            minBBX = Math.min(minBBX, extent.getMinX());
            minBBY = Math.min(minBBY, extent.getMinY());
            maxBBX = Math.max(maxBBX, extent.getMaxX());
            maxBBY = Math.max(maxBBY, extent.getMaxY());
            currentMinY = (axisOrderingTopDown ? tileIndex[1] : (maxYTotal - tileIndex[1]));
            if (!axisOrderingTopDown && currentMinY < minYImage) {
                minYImage = currentMinY;
            }
        }
        // Extent definition
        envelope = new ReferencedEnvelope(minBBX, maxBBX, minBBY, maxBBY, crs);
        // Values used for referencing the input Tiles respectively to the upper left pixel
        cTileStartX = localMinX;
        cTileStartY = !axisOrderingTopDown ? localMinY : minYImage; 
    }

    private static ImageLayout prepareLayout(ImageLayout layout, Map<String, ConveyorTile> cTiles,
            boolean axisOrderingTopDown, GridSet gridSet, int zoomLevel, int wTiles, int hTiles) {

        // Value used for defining the Mosaicked image Y origin
        long minYImage = Integer.MAX_VALUE;
        // Maximum Y value for the gridset at the level defined by the zoomlevel variable
        long maxYTotal = gridSet.getGrid(zoomLevel).getNumTilesHigh() - 1;

        // Setting axis ordering
        if (axisOrderingTopDown) {
            minYImage = 0;
        }

        final int localMinX = layout.getMinX(null);
        final int localMinY = layout.getMinY(null);

        long currentMinY = minYImage;

        int tileWidth = layout.getTileWidth(null);
        int tileHeight = layout.getTileHeight(null);
        Set<String> keys = cTiles.keySet();

        ConveyorTile tile = null;

        // Setting up the boundingBox
        for (String key : keys) {
            tile = cTiles.get(key);
            long[] tileIndex = tile.getTileIndex();
            currentMinY = (axisOrderingTopDown ? tileIndex[1] : (maxYTotal - tileIndex[1]));
            if (!axisOrderingTopDown && currentMinY < minYImage) {
                minYImage = currentMinY;
            }
        }
        // Definition of the parameters for the ImageLayout
        int minX = localMinX * tileWidth;
        int minY = (int) (axisOrderingTopDown ? localMinY : minYImage) * tileHeight;
        int width = tileWidth * wTiles;
        int height = tileHeight * hTiles;

        SampleModel inputModel = layout.getSampleModel(null);
        SampleModel sampleModel = inputModel.createCompatibleSampleModel(width, height);
        ColorModel colorModel = layout.getColorModel(null);

        ImageLayout2 l = new ImageLayout2();
        l.setColorModel(colorModel);
        l.setSampleModel(sampleModel);
        l.setHeight(height);
        l.setWidth(width);
        l.setMinX(minX);
        l.setMinY(minY);
        l.setTileWidth(tileWidth);
        l.setTileHeight(tileHeight);
        l.setTileGridXOffset(minX);
        l.setTileGridYOffset(minY);

        return l;
    }

    public ConveyorTilesRenderedImage(Map<String, ConveyorTile> cTiles, GridSet gridSet,
            GridSubset subset, ImageLayout layout) throws NoSuchAuthorityCodeException,
            IOException, FactoryException {
        this(cTiles, extractLayout(cTiles, gridSet, layout), axisOrderingTopDown(gridSet), 2, 2,
                (int) extractZoomeLevel(cTiles), gridSet, subset, CRS.decode("EPSG:"
                        + gridSet.getSrs().getNumber()));
    }

    @Override
    public Raster getTile(int tileX, int tileY) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Getting tile: " + tileX + " , " + tileY);
        }
        final int x = (int) (cTileStartX + tileX);
        final int y = (int) (cTileStartY + (axisOrderingTopDown ? tileY : getNumYTiles() - 1 - tileY));
        ConveyorTile tile = cTiles.get(x + "_" + y);
        try {
            RenderedImage resource = CoverageMetaTile.getResource(tile);
            float xTrans = getMinX() + tileX * getTileWidth();
            float yTrans = getMinY() + tileY * getTileHeight();
            RenderedOp result = TranslateDescriptor.create(resource, xTrans, yTrans,
                    Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
            return result.getData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * This method checks if the Gridset Y axis order increases from top to bottom.
     * 
     * @param gridSet
    * @return
     */
    private static boolean axisOrderingTopDown(GridSet gridSet) {
        int level = 2;
        GridSubset subset = GridSubsetFactory.createGridSubSet(gridSet,
                gridSet.getOriginalExtent(), level, level);
        BoundingBox b1 = subset.boundsFromIndex(new long[] { 0, 0, level });
        BoundingBox b2 = subset.boundsFromIndex(new long[] { 0, 1, level });
        return b2.getMinX() < b1.getMinX();
    }

    private static long extractZoomeLevel(Map<String, ConveyorTile> cTiles) {
        String key = cTiles.keySet().iterator().next();
        ConveyorTile tile = cTiles.get(key);
        return tile.getTileIndex()[2];
    }

    private static ImageLayout extractLayout(Map<String, ConveyorTile> cTiles, GridSet gridSet,
            ImageLayout input) {

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (String key : cTiles.keySet()) {
            ConveyorTile tile = cTiles.get(key);
            long[] tileIndex = tile.getTileIndex();
            if (tileIndex[0] < minX) {
                minX = (int) tileIndex[0];
            }
            if (tileIndex[1] < minY) {
                minY = (int) tileIndex[1];
            }
        }

        final int wTiles = 2;
        final int hTiles = 2;
        final int tileHeight = gridSet.getTileHeight();
        final int tileWidth = gridSet.getTileWidth();

        ImageLayout layout = new ImageLayout2(minX, minY, tileWidth * wTiles, tileHeight * hTiles,
                0, 0, tileWidth, tileHeight, input.getSampleModel(null), null);

        return layout;
    }
}
