/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

/**
 * Represents a 'coordinate' in a tiled coordinate system, where the origin is at the upper left,
 * and x is postive to the right, y is positive down. The left/top edge is the coordinate value.
 */
public class TileCoordinates {
    public int z;
    public long x;
    public long y;

    public TileCoordinates(long x, long y, int zoom) {
        this.z = zoom;
        this.x = x;
        this.y = y;
    }
}
