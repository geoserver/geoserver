/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Preconditions.checkNotNull;

import it.geosolutions.jaiext.BufferedImageAdapter;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import javax.media.jai.PlanarImage;
import org.geoserver.gwc.GWC;
import org.geoserver.ows.Response;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.metadata.i18n.ErrorKeys;
import org.geotools.metadata.i18n.Errors;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.MetaTile;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.mime.MimeType;

public class GeoServerMetaTile extends MetaTile {

    private WebMap metaTileMap;

    public GeoServerMetaTile(
            GridSubset gridSubset,
            MimeType responseFormat,
            FormatModifier formatModifier,
            long[] tileGridPosition,
            int metaX,
            int metaY,
            Integer gutter) {

        super(gridSubset, responseFormat, formatModifier, tileGridPosition, metaX, metaY, gutter);
    }

    public void setWebMap(WebMap webMap) {
        this.metaTileMap = webMap;
        if (webMap instanceof RenderedImageMap) {
            setImage(((RenderedImageMap) webMap).getImage());
        }
    }

    /**
     * Creates the {@link RenderedImage} corresponding to the tile at index {@code tileIdx} and uses
     * a {@link RenderedImageMapResponse} to encode it into the {@link #getResponseFormat() response
     * format}.
     *
     * @see org.geowebcache.layer.MetaTile#writeTileToStream(int, org.geowebcache.io.Resource)
     * @see RenderedImageMapResponse#write
     */
    @Override
    public boolean writeTileToStream(final int tileIdx, Resource target) throws IOException {

        checkNotNull(metaTileMap, "webMap is not set");

        if (metaTileMap instanceof RawMap) {
            OutputStream outStream = target.getOutputStream();
            try {
                ((RawMap) metaTileMap).writeTo(outStream);
            } finally {
                outStream.close();
            }
            return true;
        }
        if (!(metaTileMap instanceof RenderedImageMap)) {
            throw new IllegalArgumentException(
                    "Only RenderedImageMaps are supported so far: "
                            + metaTileMap.getClass().getName());
        }

        final RenderedImageMap metaTileMap = (RenderedImageMap) this.metaTileMap;
        final RenderedImageMapResponse mapEncoder;
        {
            final GWC mediator = GWC.get();
            final Response responseEncoder =
                    mediator.getResponseEncoder(responseFormat, metaTileMap);
            mapEncoder = (RenderedImageMapResponse) responseEncoder;
        }

        RenderedImage tile = metaTileMap.getImage();
        WMSMapContent tileContext = metaTileMap.getMapContext();

        if (this.tiles.length > 1 || (this.tiles.length == 1 && metaHasGutter())) {
            final Rectangle tileDim = this.tiles[tileIdx];
            tile = createTile(tileDim.x, tileDim.y, tileDim.width, tileDim.height);
            disposeLater(tile);
            {
                final WMSMapContent metaTileContext = metaTileMap.getMapContext();
                // do not create tileContext with metaTileContext.getLayers() as the layer list.
                // It is not needed at this stage and the constructor would force a
                // MapLayer.getBounds() that might fail
                tileContext = new WMSMapContent();
                tileContext.setRequest(metaTileContext.getRequest());
                tileContext.setBgColor(metaTileContext.getBgColor());
                tileContext.setMapWidth(tileDim.width);
                tileContext.setMapHeight(tileDim.height);
                tileContext.setPalette(metaTileContext.getPalette());
                tileContext.setTransparent(tileContext.isTransparent());
                long[][] tileIndexes = getTilesGridPositions();
                BoundingBox tileBounds = gridSubset.boundsFromIndex(tileIndexes[tileIdx]);
                ReferencedEnvelope tilebbox =
                        new ReferencedEnvelope(metaTileContext.getCoordinateReferenceSystem());
                tilebbox.init(
                        tileBounds.getMinX(),
                        tileBounds.getMaxX(),
                        tileBounds.getMinY(),
                        tileBounds.getMaxY());
                tileContext.getViewport().setBounds(tilebbox);
            }
        }

        OutputStream outStream = target.getOutputStream();
        try {
            // call formatImageOuputStream instead of write to avoid disposition of rendered images
            // when processing a tile from a metatile and instead defer it to this class' dispose()
            // method
            mapEncoder.formatImageOutputStream(tile, outStream, tileContext);
            return true;
        } finally {
            outStream.close();
        }
    }

    /** Checks if this meta tile has a gutter, or not */
    private boolean metaHasGutter() {
        if (this.gutter == null) {
            return false;
        }

        for (int element : gutter) {
            if (element > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Overrides to use the same method to slice the tiles than {@code MetatileMapOutputFormat} so
     * the GeoServer settings such as use native accel are leveraged in the same way when calling
     * {@link RenderedImageMapResponse#formatImageOutputStream},
     *
     * @see org.geowebcache.layer.MetaTile#createTile(int, int, int, int)
     */
    @Override
    public RenderedImage createTile(
            final int x, final int y, final int tileWidth, final int tileHeight) {
        // check image type
        final int type;
        if (metaTileImage instanceof PlanarImage) {
            type = 1;
        } else if (metaTileImage instanceof BufferedImage) {
            type = 2;
        } else {
            type = 0;
        }

        // now do the splitting
        RenderedImage tile;
        switch (type) {
            case 0:
                // do a crop, and then turn it into a buffered image so that we can release
                // the image chain
                ImageWorker w = new ImageWorker(metaTileImage);
                w.crop(
                        Float.valueOf(x),
                        Float.valueOf(y),
                        Float.valueOf(tileWidth),
                        Float.valueOf(tileHeight));
                tile = w.getBufferedImage();
                disposeLater(w.getRenderedImage());
                break;
            case 1:
                final PlanarImage pImage = (PlanarImage) metaTileImage;
                final WritableRaster wTile =
                        WritableRaster.createWritableRaster(
                                pImage.getSampleModel()
                                        .createCompatibleSampleModel(tileWidth, tileHeight),
                                new Point(x, y));
                Rectangle sourceArea = new Rectangle(x, y, tileWidth, tileHeight);
                sourceArea = sourceArea.intersection(pImage.getBounds());

                // copying the data to ensure we don't have side effects when we clean the cache
                pImage.copyData(wTile);
                if (wTile.getMinX() != 0 || wTile.getMinY() != 0) {
                    tile =
                            new BufferedImage(
                                    pImage.getColorModel(),
                                    (WritableRaster) wTile.createTranslatedChild(0, 0),
                                    pImage.getColorModel().isAlphaPremultiplied(),
                                    null);
                } else {
                    tile =
                            new BufferedImage(
                                    pImage.getColorModel(),
                                    wTile,
                                    pImage.getColorModel().isAlphaPremultiplied(),
                                    null);
                }
                break;
            case 2:
                final BufferedImage image = (BufferedImage) metaTileImage;
                final BufferedImage subimage = image.getSubimage(x, y, tileWidth, tileHeight);
                tile = new BufferedImageAdapter(subimage);
                break;
            default:
                throw new IllegalStateException(
                        Errors.format(
                                ErrorKeys.ILLEGAL_ARGUMENT_$2,
                                "metaTile class",
                                metaTileImage.getClass().toString()));
        }

        return tile;
    }

    @Override
    public void dispose() {
        if (metaTileMap != null) {
            metaTileMap.dispose();
            metaTileMap = null;
        }
        super.dispose();
    }
}
