/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

/**
 * Tiled web maps differ from regular web maps in that they are built by composing multiple tiles, rather than being
 * rendered as a single image. This interface provides access to the individual encoded tiles within such a tiled map.
 */
public interface TiledWebMap {

    /** Returns the tile at the given tile coordinates within the metatile. */
    byte[] getTile(int dx, int dy);

    /** Returns the tile at the given tile index within the metatile. */
    byte[] getTile(int tileIndex);
}
