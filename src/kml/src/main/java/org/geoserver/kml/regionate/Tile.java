/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.regionate;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.CanonicalSet;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A regionating tile identified by its coordinates
 *
 * @author Andrea Aime
 */
public class Tile {

    public static final CoordinateReferenceSystem WGS84;

    public static final ReferencedEnvelope WORLD_BOUNDS;

    static final double MAX_TILE_WIDTH;

    /**
     * This structure is used to make sure that multiple threads end up using the same table name
     * object, so that we can use it as a synchonization token
     */
    static CanonicalSet<String> canonicalizer = CanonicalSet.newInstance(String.class);

    static {
        try {
            // common geographic info
            WGS84 = CRS.decode("EPSG:4326");
            WORLD_BOUNDS = new ReferencedEnvelope(new Envelope(180.0, -180.0, 90.0, -90.0), WGS84);
            MAX_TILE_WIDTH = WORLD_BOUNDS.getWidth() / 2.0;

            // make sure, once and for all, that H2 is around
            Class.forName("org.h2.Driver");
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize the class constants", e);
        }
    }

    protected long x;

    protected long y;

    protected long z;

    protected ReferencedEnvelope envelope;

    /** Creates a new tile with the given coordinates */
    public Tile(long x, long y, long z) {
        this.x = x;
        this.y = y;
        this.z = z;
        envelope = envelope(x, y, z);
    }

    /**
     * Tile containment check is not trivial due to a couple of issues:
     *
     * <ul>
     *   <li>centroids sitting on the tile borders must be associated to exactly one tile, so we
     *       have to consider only two borders as inclusive in general (S and W) but add on occasion
     *       the other two when we reach the extent of our data set
     *   <li>coordinates going beyond the natural lat/lon range
     * </ul>
     *
     * This code takes care of the first, whilst the second issue remains as a TODO
     */
    public boolean contains(double x, double y) {
        double minx = envelope.getMinX();
        double maxx = envelope.getMaxX();
        double miny = envelope.getMinY();
        double maxy = envelope.getMaxY();
        // standard borders, N and W in, E and S out
        if (x >= minx && x < maxx && y >= miny && y < maxy) return true;

        return false;
    }

    private ReferencedEnvelope envelope(long x, long y, long z) {
        double tileSize = MAX_TILE_WIDTH / Math.pow(2, z);
        double xMin = x * tileSize + WORLD_BOUNDS.getMinX();
        double yMin = y * tileSize + WORLD_BOUNDS.getMinY();
        return new ReferencedEnvelope(xMin, xMin + tileSize, yMin, yMin + tileSize, WGS84);
    }

    /** Builds the best matching tile for the specified envelope */
    public Tile(ReferencedEnvelope wgs84Envelope) {
        z = Math.round(Math.log(MAX_TILE_WIDTH / wgs84Envelope.getWidth()) / Math.log(2));
        x =
                Math.round(
                        ((wgs84Envelope.getMinimum(0) - WORLD_BOUNDS.getMinimum(0))
                                        / MAX_TILE_WIDTH)
                                * Math.pow(2, z));
        y =
                Math.round(
                        ((wgs84Envelope.getMinimum(1) - WORLD_BOUNDS.getMinimum(1))
                                        / MAX_TILE_WIDTH)
                                * Math.pow(2, z));
        envelope = envelope(x, y, z);
    }

    /**
     * Returns the parent of this tile, or null if this tile is (one of) the root of the current
     * dataset
     */
    public Tile getParent() {
        // if we got to one of the root tiles for this data set, just stop
        if (z == 0) return null;
        else return new Tile((long) Math.floor(x / 2.0), (long) Math.floor(y / 2.0), z - 1);
    }

    /** Returns the four direct children of this tile */
    public Tile[] getChildren() {
        Tile[] result = new Tile[4];
        result[0] = new Tile(x * 2, y * 2, z + 1);
        result[1] = new Tile(x * 2 + 1, y * 2, z + 1);
        result[2] = new Tile(x * 2, y * 2 + 1, z + 1);
        result[3] = new Tile(x * 2 + 1, y * 2 + 1, z + 1);
        return result;
    }

    /** Returns the WGS84 envelope of this tile */
    public ReferencedEnvelope getEnvelope() {
        return envelope;
    }

    @Override
    public String toString() {
        return "Tile X: " + x + ", Y: " + y + ", Z: " + z + " (" + envelope + ")";
    }

    public long getX() {
        return x;
    }

    public long getY() {
        return y;
    }

    public long getZ() {
        return z;
    }
}
