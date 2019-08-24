/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import java.util.List;
import java.util.stream.Collectors;
import org.geowebcache.layer.TileLayer;

public class TilesDocument {

    List<TileMatrixSetLink> tileMatrixSetLinks;

    public TilesDocument(TileLayer tileLayer) {
        this.tileMatrixSetLinks =
                tileLayer
                        .getGridSubsets()
                        .stream()
                        .map(id -> new TileMatrixSetLink(tileLayer.getGridSubset(id)))
                        .collect(Collectors.toList());
    }

    public List<TileMatrixSetLink> getTileMatrixSetLinks() {
        return tileMatrixSetLinks;
    }
}
