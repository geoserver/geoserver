/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import org.geoserver.wms.TiledWebMap;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

/** A WebMap implementation that holds multiple vector tiles (already encoded) as a result of metatiling. */
public class VectorTileMetatilingWebMap extends WebMap implements TiledWebMap {
    private final byte[][] tiles;
    private final int metaX;

    public VectorTileMetatilingWebMap(WMSMapContent map, int w, byte[][] tiles) {
        super(map);
        this.metaX = w;
        this.tiles = tiles;
    }

    @Override
    public byte[] getTile(int dx, int dy) {
        return tiles[dy * metaX + dx];
    }

    @Override
    public byte[] getTile(int tileIndex) {
        return tiles[tileIndex];
    }
}
