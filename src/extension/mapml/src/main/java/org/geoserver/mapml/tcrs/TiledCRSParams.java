/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

/** @author prushforth */
public class TiledCRSParams {

    private final String name;
    private final String code;
    private final Bounds bounds;
    private final int TILE_SIZE;
    private final double[] scales;
    private final Point origin;

    /**
     * @param name
     * @param code
     * @param bounds
     * @param tileSize
     * @param origin
     * @param scales
     */
    public TiledCRSParams(
            String name, String code, Bounds bounds, int tileSize, Point origin, double[] scales) {
        this.name = name;
        this.code = code;
        this.bounds = bounds;
        this.TILE_SIZE = tileSize;
        this.origin = origin;
        this.scales = scales;
    }

    /** @return */
    public String getName() {
        return name;
    }

    /** @return */
    public String getCode() {
        return code;
    }

    /** @return */
    public Bounds getBounds() {
        return bounds;
    }

    /** @return */
    public int getTILE_SIZE() {
        return TILE_SIZE;
    }

    /** @return */
    public double[] getScales() {
        return scales;
    }

    /** @return */
    public Point getOrigin() {
        return origin;
    }

    /**
     * Returns the MapML full CRS name for these params
     *
     * @return
     */
    public String getSRSName() {
        return TiledCRSFactory.AUTHORITY + ":" + name;
    }
}
