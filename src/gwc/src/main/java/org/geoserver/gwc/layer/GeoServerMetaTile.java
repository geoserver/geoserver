package org.geoserver.gwc.layer;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.media.jai.PlanarImage;

import org.geoserver.ows.Response;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.resources.image.ImageUtilities;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.MetaTile;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.mime.ImageMime;
import org.geowebcache.mime.MimeType;
import org.springframework.util.Assert;

public class GeoServerMetaTile extends MetaTile {

    private RenderedImageMap metaTileMap;

    private final String layer;

    private final CatalogConfiguration mediator;

    public GeoServerMetaTile(final String layer, GridSubset gridSubset, MimeType responseFormat,
            FormatModifier formatModifier, long[] tileGridPosition, int metaX, int metaY,
            Integer gutter, CatalogConfiguration mediator) {
        super(gridSubset, responseFormat, formatModifier, tileGridPosition, metaX, metaY, gutter);
        this.layer = layer;
        this.mediator = mediator;
    }

    public void setWebMap(RenderedImageMap webMap) {
        this.metaTileMap = webMap;
        if (webMap instanceof RenderedImageMap) {
            setImage(((RenderedImageMap) webMap).getImage());
        }
    }

    /**
     * Overrides to return {@link WMS#getJPEGNativeAcceleration()} if the requested response format
     * is image/jpeg, otherwise if it's set to false and native JAI is available JPEGMapResponse
     * will assume the WMS setting and create repeated tiles because it will not get a
     * BufferedImage.
     * 
     * @see org.geowebcache.layer.MetaTile#nativeAccelAvailable()
     */
    @Override
    protected boolean nativeAccelAvailable() {
        boolean useNativeAccel = super.nativeAccelAvailable();
        if (useNativeAccel && ImageMime.jpeg.equals(responseFormat)) {
            useNativeAccel = WMS.get().getJPEGNativeAcceleration();
        }
        return useNativeAccel;
    }

    /**
     * Creates the {@link RenderedImage} corresponding to the tile at index {@code tileIdx} and uses
     * a {@link RenderedImageMapResponse} to encode it into the {@link #getResponseFormat() response
     * format}.
     * 
     * @see org.geowebcache.layer.MetaTile#writeTileToStream(int, org.geowebcache.io.Resource)
     * @see RenderedImageMapResponse#write
     * 
     */
    @Override
    public boolean writeTileToStream(final int tileIdx, Resource target) throws IOException {

        Assert.notNull(metaTileMap, "webMap is not set");
        if (!(metaTileMap instanceof RenderedImageMap)) {
            throw new IllegalArgumentException("Only RenderedImageMaps are supported so far: "
                    + metaTileMap.getClass().getName());
        }
        final RenderedImageMapResponse mapEncoder;
        {
            final Response responseEncoder = mediator.getResponseEncoder(responseFormat,
                    metaTileMap);
            mapEncoder = (RenderedImageMapResponse) responseEncoder;
        }

        RenderedImage tile = metaTileMap.getImage();
        WMSMapContext tileContext = metaTileMap.getMapContext();

        if (this.tiles.length > 1) {
            final Rectangle tileDim = this.tiles[tileIdx];
            tile = createTile(tileDim.x, tileDim.y, tileDim.width, tileDim.height);
            {
                final WMSMapContext metaTileContext = metaTileMap.getMapContext();
                // do not create tileContext with metaTileContext.getLayers() as the layer list.
                // It is not needed at this stage and the constructor would force a
                // MapLayer.getBounds() that might fail
                tileContext = new WMSMapContext();
                tileContext.setRequest(metaTileContext.getRequest());
                tileContext.setBgColor(metaTileContext.getBgColor());
                tileContext.setMapWidth(tileDim.width);
                tileContext.setMapHeight(tileDim.height);
                tileContext.setPaletteInverter(metaTileContext.getPaletteInverter());
                tileContext.setTransparent(tileContext.isTransparent());
                long[][] tileIndexes = getTilesGridPositions();
                BoundingBox tileBounds = gridSubset.boundsFromIndex(tileIndexes[tileIdx]);
                ReferencedEnvelope tilebbox = new ReferencedEnvelope(
                        metaTileContext.getCoordinateReferenceSystem());
                tilebbox.init(tileBounds.getMinX(), tileBounds.getMaxX(), tileBounds.getMinY(),
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

    public void dispose() {
        if (metaTileMap != null) {
            RenderedImage image = metaTileMap.getImage();
            // as in RenderedImageMapResponse.write: let go of the image chain as quick as possible
            // to free memory
            if (image instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) image);
            } else if (image instanceof BufferedImage) {
                ((BufferedImage) image).flush();
            }
            metaTileMap.dispose();
            metaTileMap = null;
        }
    }
}
