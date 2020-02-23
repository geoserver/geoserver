/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.mapml.MapMLConstants;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

public class TiledCRS {
    private static final Logger LOGGER = Logging.getLogger("org.geoserver.mapml.tcrs");

    private final Transformation transformation;
    private final Projection projection;
    private final double[] scales;
    private final String name;
    private final int TILESIZE = 256;
    private final Point TILE_ORIGIN;
    private int pageSize = MapMLConstants.PAGESIZE;
    private final String code;

    // the max.x,max.y of the Bounds member are integer numbers (though the defn of Bounds.* are
    // double)
    // representing the maximum tile ordinate for the zoom level (the key).  min.x,
    // min.y (should/will) always be 0.
    private final HashMap<Integer, Bounds> tileBounds = new HashMap<>();

    private final Bounds bounds;

    public TiledCRS(String name) {
        TiledCRSParams parameters = TiledCRSConstants.tiledCRSDefinitions.get(name);
        if (parameters == null) {
            throw new RuntimeException("Definition for Tiled CRS not found: " + name);
        }
        // the bounds are in projected, but not transformed units.
        this.bounds = parameters.getBounds();
        // the tile origin is in projected, but not transformed units.
        this.TILE_ORIGIN = parameters.getOrigin();
        this.transformation =
                new Transformation(
                        1, (-1 * parameters.getOrigin().x), -1, parameters.getOrigin().y);
        // the projection below is the geotools / EPSG definition
        this.projection = new Projection(parameters.getCode());
        // the 'scales' are the reciprocal of the resolution of the zoom level
        // the resolution is in projected units, thus the scales are pixels per
        // projected unit.
        this.scales = parameters.getScales();
        // the name is the name of the TiledCRS, which is equal to the name of
        // the 'projection' in the MapML projection registry*.
        this.name = name;

        this.code = parameters.getCode();
        // calculate the maximum tile coordinates on a per-zoom-level basis.
        init();
    }

    public String getCode() {
        return this.code;
    }

    public CoordinateReferenceSystem getCRS() {
        return this.projection.getCRS();
    }

    private void init() {
        // establish the maximum tile coordinates for each zoom value
        // we know that the minimum x and y are 0 at 85.0511D North and 180.0 West

        for (int zoom = 0; zoom < this.scales.length; zoom++) {
            // consider if the min should be automatically bumped up to 0,0 if
            // it is below that.  If it is below that, it might indicate an
            // error in the TiledCRSConstants parameter for BOUNDS, possibly
            // due to converting from a theoretical boundary in lat long to projected
            // coordinates e.g. -90,-180 90,180 instead of -85.011,-180.0 85.011,180

            // in any case at the current time the tileBounds values are only used
            // as an upper limit (i.e. only the tb.max is used (0 is taken to be min).
            this.tileBounds.put(zoom, getTileBoundsForProjectedBounds(zoom, this.bounds));
        }
    }

    public Bounds getPixelBoundsForProjectedBounds(int zoom, Bounds projectedBounds) {
        Bounds pb =
                new Bounds(
                        this.transformation.transform(projectedBounds.min, this.scales[zoom]),
                        this.transformation.transform(projectedBounds.max, this.scales[zoom]));
        return pb;
    }

    public Bounds getTileBoundsForProjectedBounds(int zoom, Bounds projectedBounds) {
        Bounds pb = getPixelBoundsForProjectedBounds(zoom, projectedBounds);
        Bounds tb =
                new Bounds(pb.min.divideBy(TILESIZE).floor(), pb.max.divideBy(TILESIZE).floor());
        return tb;
    }
    /**
     * Transforms the point (in projected units) into a bounds centred on that point, in projected
     * units, matching the size of the display bounds
     *
     * @param zoom the zoom at which the bounds will be calculated
     * @param projectedCentre the projected coordinates of the centre of the new bounds
     * @param displayBounds a rectangle with origin at 0,0 of the size of display in px
     */
    public Bounds getProjectedBoundsForDisplayBounds(
            int zoom, Point projectedCentre, Bounds displayBounds) {
        Point tcrsCentre = transform(projectedCentre, zoom);
        Point tcrsMin =
                tcrsCentre.subtract(
                        new Point(displayBounds.getWidth() / 2, displayBounds.getHeight() / 2));
        Point tcrsMax =
                tcrsCentre.add(
                        new Point(displayBounds.getWidth() / 2, displayBounds.getHeight() / 2));
        return new Bounds(untransform(tcrsMin, zoom), untransform(tcrsMax, zoom));
    }

    public int getMaxZoom() {
        return Collections.max(tileBounds.keySet());
    }
    /**
     * Returns a zoom at which bounds when rendered at the specified display width and height will
     * fit, when centered on bounds' centre point.
     *
     * @return zoom
     */
    public int fitLatLngBoundsToDisplay(LatLngBounds bounds, int width, int height) {
        int zoom = 0;
        try {
            for (int i = 0; i < this.getMaxZoom(); i++) {
                zoom = i;
                Bounds b = this.getPixelBounds(bounds, i);
                if (!(width >= b.getWidth() && height >= b.getHeight())) {
                    zoom = i - 1;
                    break;
                }
            }
        } catch (MismatchedDimensionException | TransformException ex) {
            LOGGER.log(
                    Level.INFO,
                    "Error transforming lat/lon bounds to projected bounds",
                    ex.getMessage());
        }
        return zoom;
    }

    /**
     * Returns a zoom at which the projected bounds when rendered at the specified display width and
     * height will fit, when centered on bounds' centre point.
     *
     * @param prjb - the (projected) bounds to find a zoom that fits for
     * @param dsplyb - the pixel bounds which the projected bounds must fit within
     * @return zoom
     */
    public int fitProjectedBoundsToDisplay(Bounds prjb, Bounds dsplyb) {
        int zoom = 0;
        for (int i = 0; i < this.getMaxZoom(); i++) {
            zoom = i;
            Bounds pxb = this.getPixelBounds(prjb, i);
            if (!(dsplyb.getWidth() >= pxb.getWidth() && dsplyb.getHeight() >= pxb.getHeight())) {
                zoom = i - 1;
                break;
            }
        }
        return zoom;
    }
    /** For testing purposes need to be able to set the pagesize. */
    public void setPageSize(int size) {
        pageSize = size;
    }

    public int getPageSize() {
        return pageSize;
    }

    public String getName() {
        return name;
    }

    public int getTileSize() {
        return this.TILESIZE;
    }

    public Point latLngToPoint(LatLng latlng, int zoom)
            throws MismatchedDimensionException, TransformException {
        Point p = this.projection.project(latlng);
        return this.transformation.transform(p, this.scales[zoom]);
    }

    public LatLng pointToLatLng(Point p, int zoom)
            throws MismatchedDimensionException, TransformException {
        Point untransformedPoint = this.transformation.untransform(p, this.scales[zoom]);
        return this.projection.unproject(untransformedPoint);
    }
    /** @return Point transformed from PCRS units to pixel units at zoom */
    public Point transform(Point p, int zoom) {
        return this.transformation.transform(p, this.scales[zoom]);
    }

    public Point untransform(Point p, int zoom) {
        return this.transformation.untransform(p, this.scales[zoom]);
    }
    /**
     * @param bounds the LatLngBounds that should be transformed to projected, scaled bounds
     * @param zoom the zoom scale at which to transform the bounds
     * @return the projected and transformed LatLngBounds into pixel coordinates.
     */
    public Bounds getPixelBounds(LatLngBounds bounds, int zoom)
            throws MismatchedDimensionException, TransformException {
        LatLng sw = bounds.southWest;
        LatLng ne = bounds.northEast;
        return new Bounds(latLngToPoint(sw, zoom), latLngToPoint(ne, zoom));
    }
    /**
     * @param bounds - projected, but not scaled bounds
     * @param zoom - the scale at which to transform the bounds
     * @return pixel bounds transformation of the given bounds
     */
    public Bounds getPixelBounds(Bounds bounds, int zoom) {
        Point min = this.transformation.transform(bounds.min, this.scales[zoom]).round();
        Point max = this.transformation.transform(bounds.max, this.scales[zoom]).round();
        return new Bounds(min, max);
    }
    // convenience methods
    public Point project(LatLng latLng) throws MismatchedDimensionException, TransformException {
        return this.projection.project(latLng);
    }

    public LatLng unproject(Point point) throws MismatchedDimensionException, TransformException {
        return this.projection.unproject(point);
    }
    /**
     * Count the width of the *tile* bounds at the given zoom level in integral tile units.
     *
     * @param zoom integer zoom level at which to calculate the width
     * @param bounds the bounds for which the calculation/conversion should be done
     * @return long the number of tiles wide the bounds is at the zoom level
     */
    protected long tileWidth(int zoom, Bounds bounds) {
        if (zoom == -1 || bounds == null) return 0;
        return (long) bounds.max.x + 1 - (long) bounds.min.x;
    }

    public long tileCount(int zoom, Bounds bounds) {
        if (zoom == -1 || bounds == null) return 0;

        Point min = bounds.min.divideBy(TILESIZE).floor();
        Point max = bounds.max.divideBy(TILESIZE).floor();

        // integer coordinate system, bump max values up to next increment
        long width = (long) (max.x + 1 - min.x);
        long height = (long) (max.y + 1 - min.y);
        return width * height;
    }

    public List<TileCoordinates> getTilesForExtent(Bounds extent, int zoom, long start) {

        // the extent must be expressed in projected, scaled units
        Bounds pb = extent;
        // the min/max in decimal tiles truncated to the next lower integer tile ordinate
        Bounds tb =
                new Bounds(pb.min.divideBy(TILESIZE).floor(), pb.max.divideBy(TILESIZE).floor());
        long width = tileWidth(zoom, tb);
        List<TileCoordinates> tiles = new ArrayList<>();
        for (long i = (start > 0 ? (long) tb.min.y + start / width : (long) tb.min.y);
                i <= tb.max.y;
                i++) {
            for (long j = (start > 0 ? (long) tb.min.x + (start % width) : (long) tb.min.x);
                    j <= tb.max.x;
                    j++) {
                if (tiles.size() < pageSize) {
                    if (i >= 0
                            && i <= tileBounds.get(zoom).max.y
                            && j >= 0
                            && j < tileBounds.get(zoom).max.x) {
                        tiles.add(new TileCoordinates(j, i, zoom));
                    }
                } else {
                    break;
                }
            }
        }
        // the centre of the extent in decimal tiles... not truncated
        Point centre = pb.getCentre().divideBy(TILESIZE);
        Collections.sort(tiles, new TileComparator(centre));
        return tiles;
    }

    public List<TileCoordinates> getTilesForExtent(LatLngBounds extent, int zoom, long start) {

        Bounds pb;
        try {
            pb = getPixelBounds(extent, zoom);
        } catch (MismatchedDimensionException | TransformException ex) {
            throw new RuntimeException(
                    "Error retrieving tiles for lat lon bounds: " + extent.toString(), ex);
        }
        return getTilesForExtent(pb, zoom, start);
    }
    // extent is in projected but not scaled units (e.g. meters)
    public Bounds getTileRoundedPixelBoundsForExtent(Bounds extent, int zoom) {
        Bounds pb = getPixelBounds(extent, zoom);
        Bounds tb = new Bounds(pb.min.divideBy(TILESIZE).floor(), pb.max.divideBy(TILESIZE).ceil());
        return new Bounds(tb.min.multiplyBy(TILESIZE), tb.max.multiplyBy(TILESIZE));
    }

    public double[] getScales() {
        return scales;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public Point getOrigin() {
        return TILE_ORIGIN;
    }
    /**
     * Compares two tile coordinates and ranks them by distance from the constructed center point.
     */
    protected class TileComparator implements Comparator<TileCoordinates> {
        private final Point centre;

        public TileComparator(Point centre) {
            this.centre = centre;
        }

        @Override
        public int compare(TileCoordinates t1, TileCoordinates t2) {
            // add 0.5 to ordinates to calculate distance to tile centres
            Double d1 = this.centre.distanceTo(new Point(t1.x + 0.5, t1.y + 0.5));
            Double d2 = this.centre.distanceTo(new Point(t2.x + 0.5, t2.y + 0.5));
            return d1.compareTo(d2);
        }
    }
}
