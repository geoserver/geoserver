/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.QuickTileCache.MetaTileKey;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.crop.GTCropDescriptor;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;
import org.geotools.resources.image.ImageUtilities;
import org.geotools.util.logging.Logging;

/**
 * Wrapping map producer that performs on the fly meta tiling wrapping another map producer. It will
 * first peek inside a tile cache to see if the requested tile has already been computed, if so,
 * it'll encode and return that one, otherwise it'll build a meta tile, split it, and finally encode
 * just the requested tile, putting the others in the tile cache.
 * 
 * @author Andrea Aime - TOPP
 * @author Simone Giannecchini - GeoSolutions
 */
public final class MetatileMapOutputFormat implements GetMapOutputFormat {
 
    private static final RenderingHints NO_CACHE_HINTS = new RenderingHints(JAI.KEY_TILE_CACHE, null);

    /** A logger for this class. */
    private static final Logger LOGGER = Logging.getLogger(MetatileMapOutputFormat.class);

    /** Small number for double equality comparison */
    public static final double EPS = 1E-6;

    private static QuickTileCache tileCache;

    private GetMapRequest request;

    private RenderedImageMapOutputFormat delegate;
    
    public MetatileMapOutputFormat(GetMapRequest request, RenderedImageMapOutputFormat delegate) {
        if (tileCache == null) {
            tileCache = (QuickTileCache) GeoServerExtensions.bean("metaTileCache");
        }
        this.request = request;
        this.delegate = delegate;
    }

    /**
     * 
     * @see org.geoserver.wms.GetMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent)
     */
    public WebMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
        // get the key that identifies the meta tile. The cache will make sure
        // two threads asking
        // for the same tile will get the same key, and thus will synchronize
        // with each other
        // (the first eventually builds the meta-tile, the second finds it ready
        // to be used)
        QuickTileCache.MetaTileKey key = tileCache.getMetaTileKey(request);

        synchronized (key) {
            RenderedImage tile = tileCache.getTile(key, request);
            List<GridCoverage2D> renderedCoverages = null;

            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Looked for meta tile " + key.metaTileCoords.x + ", "
                        + key.metaTileCoords.y + "in cache: " + ((tile != null) ? "hit!" : "miss"));
            }

            if (tile == null) {
                // compute the meta-tile
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Building meta tile " + key.metaTileCoords.x + ", "
                            + key.metaTileCoords.y);
                }

                // alter the map definition so that we build a meta-tile instead
                // of just the tile
                ReferencedEnvelope origEnv = mapContent.getRenderingArea();
                mapContent.getViewport().setBounds(new ReferencedEnvelope(key.getMetaTileEnvelope(),
                        origEnv.getCoordinateReferenceSystem()));
                mapContent.setMapWidth(key.getTileSize() * key.getMetaFactor());
                mapContent.setMapHeight(key.getTileSize() * key.getMetaFactor());
                mapContent.setTileSize(key.getTileSize());

                RenderedImageMap metaTileMap = delegate.produceMap(mapContent);

                RenderedImage metaTile = metaTileMap.getImage();
                RenderedImage[] tiles = split(key, metaTile, mapContent);
                tileCache.storeTiles(key, tiles);
                tile = tileCache.getTile(key, request, tiles);
                renderedCoverages = metaTileMap.getRenderedCoverages();
            }
            RenderedImageMap tileMap = new RenderedImageMap(mapContent, tile, getMimeType());
            tileMap.setRenderedCoverages(renderedCoverages);
            return tileMap;
        }
    }

    /**
     * 
     * @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames()
     */
    public Set<String> getOutputFormatNames() {
        return delegate.getOutputFormatNames();
    }

    /**
     * 
     * @see org.geoserver.wms.GetMapOutputFormat#getMimeType()
     */
    public String getMimeType() {
        return delegate.getMimeType();
    }

    /**
     * True if the request has the tiled hint, is 256x256 image, and the raw delegate is a raster
     * one
     * 
     * @param request
     * @param delegate
     * @return
     */
    public static boolean isRequestTiled(GetMapRequest request, GetMapOutputFormat delegate) {
        boolean tiled = request.isTiled();
        Point2D tilesOrigin = request.getTilesOrigin();
        int width = request.getWidth();
        int height = request.getHeight();
        if (tiled && tilesOrigin != null && width == 256 && height == 256
                && delegate instanceof RenderedImageMapOutputFormat) {
            return true;
        }

        return false;
    }

    /**
     * Splits the tile into a set of tiles, numbered from lower right and going up so that first row
     * is 0,1,2,...,metaTileFactor, and so on. In the case of a 3x3 meta-tile, the layout is as
     * follows:
     * 
     * <pre>
     *    6 7 8
     *    3 4 5
     *    0 1 2
     * </pre>
     * 
     * @param key
     * @param metaTile
     * @param map
     * @return
     */
    private static RenderedImage[] split(MetaTileKey key, RenderedImage metaTile, WMSMapContent map) {
        final int metaFactor = key.getMetaFactor();
        final RenderedImage[] tiles = new RenderedImage[key.getMetaFactor() * key.getMetaFactor()];
        final int tileSize = key.getTileSize();

        // check image type 
        int type = 0;
        if (metaTile instanceof PlanarImage) {
            type = 1;
        } else if (metaTile instanceof BufferedImage) {
            type = 2;
        }

        // now do the splitting
        for (int i = 0; i < metaFactor; i++) {
            for (int j = 0; j < metaFactor; j++) {
                int x = j * tileSize;
                int y = (tileSize * (metaFactor - 1)) - (i * tileSize);

                RenderedImage tile;
                switch (type) {
                case 0:
                    // do a crop, and then turn it into a buffered image so that we can release
                    // the image chain
                    RenderedOp cropped = GTCropDescriptor.create(metaTile, Float.valueOf(x), Float.valueOf(y), Float.valueOf(
                            tileSize), Float.valueOf(tileSize), NO_CACHE_HINTS);
                    tile = cropped.getAsBufferedImage();
                    break;
                case 1:
                    final PlanarImage pImage = (PlanarImage) metaTile;
                    final WritableRaster wTile = WritableRaster.createWritableRaster(
                            pImage.getSampleModel().createCompatibleSampleModel(tileSize, tileSize), 
                            new Point(x, y));
                    Rectangle sourceArea = new Rectangle(x, y, tileSize, tileSize);
                    sourceArea = sourceArea.intersection(pImage.getBounds());
                    
                    // copying the data to ensure we don't have side effects when we clean the cache
                    pImage.copyData(wTile);
                    if(wTile.getMinX()!=0||wTile.getMinY()!=0) {
                        tile = new BufferedImage(pImage.getColorModel(), (WritableRaster) wTile.createTranslatedChild(0, 0), pImage.getColorModel().isAlphaPremultiplied(), null);                    
                    } else {
                        tile = new BufferedImage(pImage.getColorModel(), wTile, pImage.getColorModel().isAlphaPremultiplied(), null);
                    }
                    break;
                case 2:
                    final BufferedImage image = (BufferedImage) metaTile;
                    tile = image.getSubimage(x, y, tileSize, tileSize);                    
                    break;                    
                default:
                    throw new IllegalStateException(Errors.format(ErrorKeys.ILLEGAL_ARGUMENT_$2,"metaTile class",metaTile.getClass().toString()));

                }

                tiles[(i * key.getMetaFactor()) + j] = tile;
            }
        }
        
        // dispose input image if necessary/possible
        if (type < 2) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) metaTile);
        } else {
            BufferedImage image = (BufferedImage) metaTile;
            image.flush();
            image = null;
        }
        return tiles;
    }

    public MapProducerCapabilities getCapabilities(String format) {
        throw new RuntimeException("The meta-tile output format should never be invoked directly!");
    }

}
