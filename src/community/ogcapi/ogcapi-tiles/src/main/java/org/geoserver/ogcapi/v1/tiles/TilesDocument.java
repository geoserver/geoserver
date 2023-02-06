/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.wms.WMS;
import org.geowebcache.layer.TileLayer;

public class TilesDocument extends AbstractDocument {

    private final Tileset.DataType dataType;
    private final List<Tileset> tilesets;
    private final String styleId;

    public TilesDocument(WMS wms, TileLayer tileLayer, Tileset.DataType dataType) {
        this(wms, tileLayer, dataType, null);
    }

    public TilesDocument(WMS wms, TileLayer tileLayer, Tileset.DataType dataType, String styleId) {
        this.tilesets =
                tileLayer.getGridSubsets().stream()
                        .map(
                                subsetId ->
                                        new Tileset(
                                                wms, tileLayer, dataType, subsetId, styleId, false))
                        .collect(Collectors.toList());
        this.id =
                tileLayer instanceof GeoServerTileLayer
                        ? ((GeoServerTileLayer) tileLayer).getContextualName()
                        : tileLayer.getName();
        this.styleId = styleId;
        this.dataType = dataType;

        // links depend on the data type
        if (dataType == Tileset.DataType.vector) {
            addSelfLinks("ogc/tiles/v1/collections/" + id + "/tiles");
        } else if (dataType == Tileset.DataType.map) {
            if (styleId != null) {
                addSelfLinks(
                        "ogc/tiles/v1/collections/" + id + "/styles/" + styleId + "/map/tiles");
            } else {
                addSelfLinks("ogc/tiles/v1/collections/" + id + "/map/tiles");
            }

        } else {
            throw new IllegalArgumentException(
                    "Tiles of this type are not yet supported: " + dataType);
        }
    }

    @JsonIgnore
    public String getStyleId() {
        return styleId;
    }

    @JsonIgnore
    public Tileset.DataType getDataType() {
        return dataType;
    }

    public List<Tileset> getTilesets() {
        return tilesets;
    }
}
