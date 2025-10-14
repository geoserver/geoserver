/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import java.util.Arrays;
import java.util.Objects;

/** @author prushforth */
public class TiledCRSParams {

    private final String name;
    private final String code;
    private final Bounds bounds;
    private final int TILE_SIZE;
    private final double[] scales;
    private final double[] resolutions;
    private final Point origin;

    /**
     * @param name
     * @param code
     * @param bounds
     * @param tileSize
     * @param origin
     * @param scales
     */
    public TiledCRSParams(String name, String code, Bounds bounds, int tileSize, Point origin, double[] scales) {
        this.name = name;
        this.code = code;
        this.bounds = bounds;
        this.TILE_SIZE = tileSize;
        this.origin = origin;
        this.scales = scales;
        this.resolutions = new double[scales.length];
        for (int i = 0; i < scales.length; i++) {
            resolutions[i] = 1d / scales[i];
        }
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

    public double[] getResolutions() {
        return resolutions;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TiledCRSParams that = (TiledCRSParams) o;
        return TILE_SIZE == that.TILE_SIZE
                && Objects.equals(name, that.name)
                && Objects.equals(code, that.code)
                && Objects.equals(bounds, that.bounds)
                && Objects.deepEquals(scales, that.scales)
                && Objects.equals(origin, that.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, code, bounds, TILE_SIZE, Arrays.hashCode(scales), origin);
    }
}
