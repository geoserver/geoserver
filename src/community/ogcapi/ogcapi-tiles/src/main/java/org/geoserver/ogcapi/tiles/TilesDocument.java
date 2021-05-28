/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.tiles;

import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.tiles.Tileset.DataType;
import org.geoserver.wms.WMS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;

public class TilesDocument extends AbstractDocument {

    List<Tileset> tilesets;

    public TilesDocument(WMS wms, TileLayer tileLayer, DataType dataType) {
        this.tilesets =
                tileLayer
                        .getGridSubsets()
                        .stream()
                        .map(subsetId -> new Tileset(wms, tileLayer, dataType, subsetId, false))
                        .collect(Collectors.toList());
        this.id =
                tileLayer instanceof GeoServerTileLayer
                        ? ((GeoServerTileLayer) tileLayer).getContextualName()
                        : tileLayer.getName();

        // links depend on the data type
        List<MimeType> tileTypes = tileLayer.getMimeTypes();
        if (dataType == DataType.vector) {
            addSelfLinks("ogc/tiles/collections/" + id + "/tiles");
        } else if (dataType == DataType.map) {
            addSelfLinks("ogc/tiles/collections/" + id + "/map/tiles");

        } else {
            throw new IllegalArgumentException(
                    "Tiles of this type are not yet supported: " + dataType);
        }
    }

    public List<Tileset> getTilesets() {
        return tilesets;
    }
}
