/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

public class TiledCRSParams {

    private final String name;
    private final String code;
    private final Bounds bounds;
    private final int TILE_SIZE;
    private final double[] scales;
    private final Point origin;

    public TiledCRSParams(
            String name, String code, Bounds bounds, int tileSize, Point origin, double[] scales) {
        this.name = name;
        this.code = code;
        this.bounds = bounds;
        this.TILE_SIZE = tileSize;
        this.origin = origin;
        this.scales = scales;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public int getTILE_SIZE() {
        return TILE_SIZE;
    }

    public double[] getScales() {
        return scales;
    }

    public Point getOrigin() {
        return origin;
    }
}
