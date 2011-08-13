package org.geoserver.gwc.layer;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.ows.Response;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.MetaTile;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.mime.MimeType;
import org.springframework.util.Assert;

public class GeoServerMetaTile extends MetaTile {

    private WebMap webMap;

    private final String layer;

    private final CatalogConfiguration mediator;

    public GeoServerMetaTile(final String layer, GridSubset gridSubset, MimeType responseFormat,
            FormatModifier formatModifier, long[] tileGridPosition, int metaX, int metaY,
            Integer gutter, CatalogConfiguration mediator) {
        super(gridSubset, responseFormat, formatModifier, tileGridPosition, metaX, metaY, gutter);
        this.layer = layer;
        this.mediator = mediator;
    }

    public void setWebMap(WebMap webMap) {
        this.webMap = webMap;
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
     * 
     */
    @Override
    public boolean writeTileToStream(final int tileIdx, Resource target) throws IOException {
        Assert.notNull(webMap, "webMap is not set");
        if (!(webMap instanceof RenderedImageMap)) {
            throw new IllegalArgumentException("Only RenderedImageMaps are supported so far: "
                    + webMap.getClass().getName());
        }
        final RenderedImageMapResponse mapEncoder;
        {
            final Response responseEncoder = mediator.getResponseEncoder(responseFormat, webMap);
            mapEncoder = (RenderedImageMapResponse) responseEncoder;
        }

        RenderedImageMap tileMap = (RenderedImageMap) webMap;
        if (this.tiles.length > 1) {
            final Rectangle tileDim = this.tiles[tileIdx];
            final RenderedImage tile = createTile(tileDim.x, tileDim.y, tileDim.width,
                    tileDim.height);
            final String mimeType = webMap.getMimeType();
            final WMSMapContext tileContext;
            {
                final WMSMapContext metaTileContext = ((RenderedImageMap) webMap).getMapContext();
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
            tileMap = new RenderedImageMap(tileContext, tile, mimeType);
        }

        try {
            OutputStream outStream = target.getOutputStream();
            try {
                mapEncoder.write(tileMap, outStream, null);
                return true;
            } finally {
                outStream.close();
            }
        } finally {
            tileMap.dispose();
        }
    }

    public void dispose() {
        if (webMap != null) {
            webMap.dispose();
            webMap = null;
        }
    }
}
