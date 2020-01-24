/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.MetaTile;

/**
 * Variant of the GeoServerTileLayer that does no metatiling and does not save the tiles on disk,
 * used when doing tile service on non cacheable filtered tile requests (e.g., the original layer
 * does not have CQL_FILTER as the filter parameter)
 */
class VolatileGeoServerTileLayer extends GeoServerTileLayer {

    static final Logger LOGGER = Logging.getLogger(VolatileGeoServerTileLayer.class);

    public VolatileGeoServerTileLayer(GeoServerTileLayer layer) {
        super(layer);
    }

    @Override
    protected ConveyorTile getMetatilingReponse(
            ConveyorTile tile, boolean tryCache, int metaX, int metaY)
            throws GeoWebCacheException, IOException {
        // forces meta tiling factors to 1x1 and disables cache usage
        return super.getMetatilingReponse(tile, false, 1, 1);
    }

    @Override
    protected void saveTiles(MetaTile metaTile, ConveyorTile tileProto, long requestTime)
            throws GeoWebCacheException {
        final long[] gridLoc = tileProto.getTileIndex();
        final GridSubset gridSubset = getGridSubset(tileProto.getGridSetId());
        final long[] gridPos = metaTile.getTilesGridPositions()[0];
        if (!gridSubset.covers(gridPos)) {
            // edge tile outside coverage, do not store it
            return;
        }

        Resource resource = getImageBuffer(WMS_BUFFER2);
        tileProto.setBlob(resource);
        try {
            boolean completed = metaTile.writeTileToStream(0, resource);
            if (!completed) {
                LOGGER.severe("metaTile.writeTileToStream returned false, no tiles saved");
            }
        } catch (IOException ioe) {
            LOGGER.log(
                    Level.SEVERE, "Unable to write image tile to " + "ByteArrayOutputStream", ioe);
        }
    }
}
