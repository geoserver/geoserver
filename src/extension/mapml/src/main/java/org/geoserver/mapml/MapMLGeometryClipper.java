/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.geoserver.mapml.TaggedPolygon.TaggedCoordinateSequence;
import org.geotools.geometry.jts.GeometryClipper;
import org.locationtech.jts.algorithm.distance.DistanceToPoint;
import org.locationtech.jts.algorithm.distance.PointPairDistance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Clips geometries on a given envelope, and if they are polygons or do contain polygon parts, tags
 * the sides of the polygon to pinpoint the artificial sides introduced by the clipping.
 */
class MapMLGeometryClipper {

    private final double eps;
    private final Geometry original;
    private final GeometryClipper clipper;
    private final Envelope innerEnvelope;
    Envelope clipEnvelope;

    public MapMLGeometryClipper(Geometry original, Envelope clipEnvelope) {
        this.original = original;
        this.clipEnvelope = clipEnvelope;
        this.clipper = new GeometryClipper(clipEnvelope);

        // tolerance used to avoid numerical issues testing for segment on boundary
        Envelope originalBounds = original.getEnvelopeInternal();
        double max =
                Math.max(Math.abs(originalBounds.getMaxX()), Math.abs(originalBounds.getMaxY()));
        this.eps = (Math.nextUp(max) - max) * 10;

        // if a clipped geometry is fully inside this envelope, it cannot contain artificial sides
        this.innerEnvelope = new Envelope(clipEnvelope);
        innerEnvelope.expandBy(-2 * eps);
    }

    public Geometry clipAndTag() {
        // clip and filter
        Geometry clippedGeom = clipper.clipSafe(original, true, 0);
        if (clippedGeom == null || clippedGeom.isEmpty()) return null;

        if (clippedGeom instanceof MultiPolygon) {
            List<Polygon> geometries = new ArrayList<>();
            for (int i = 0; i < clippedGeom.getNumGeometries(); i++) {
                Polygon g = (Polygon) clippedGeom.getGeometryN(i);
                if (!g.isEmpty()) geometries.add(g);
            }
            clippedGeom =
                    clippedGeom
                            .getFactory()
                            .createMultiPolygon(geometries.toArray(n -> new Polygon[n]));
        } else if (clippedGeom instanceof GeometryCollection) {
            List<Geometry> geometries = new ArrayList<>();
            for (int i = 0; i < clippedGeom.getNumGeometries(); i++) {
                Geometry g = clippedGeom.getGeometryN(i);
                if (!g.isEmpty()) geometries.add(g);
            }
            clippedGeom = collectMultiGeometry(clippedGeom, geometries);
        }

        // tag if necessary
        if (original instanceof GeometryCollection || original instanceof Polygon) {
            for (int i = 0; i < clippedGeom.getNumGeometries(); i++) {
                Geometry g = clippedGeom.getGeometryN(i);
                if (g instanceof Polygon && !g.isEmpty()) tag((Polygon) g);
            }
        }
        return clippedGeom;
    }

    private static Geometry collectMultiGeometry(Geometry clippedGeom, List<Geometry> geometries) {
        GeometryFactory fac = clippedGeom.getFactory();
        if (clippedGeom instanceof MultiPolygon) {
            clippedGeom = fac.createMultiPolygon(geometries.toArray(n -> new Polygon[n]));
        } else if (clippedGeom instanceof MultiLineString) {
            clippedGeom = fac.createMultiLineString(geometries.toArray(n -> new LineString[n]));
        } else if (clippedGeom instanceof MultiPoint) {
            clippedGeom = fac.createMultiPoint((Point[]) geometries.toArray(n -> new Point[n]));
        } else {
            clippedGeom = fac.createGeometryCollection(geometries.toArray(n -> new Geometry[n]));
        }
        return clippedGeom;
    }

    private void tag(Polygon clipped) {
        // if the clipped geometry is fully inside the inner envelope, it cannot contain artificial
        // sides, so no need to tag it
        if (innerEnvelope.contains(clipped.getEnvelopeInternal())) return;

        // MapML wants CCW on the boundary, JTS defaults to the opposite, fix
        clipped.normalize();
        Polygon reversed = clipped.reverse();

        // get the boundary lines that can be used to test segment visibility and have a chance
        // of intersecting the clipped geometry
        List<LinearRing> rings = new ArrayList<>();
        original.apply(new PolygonRingsExtractor(rings));
        Envelope clippedEnvelope = reversed.getEnvelopeInternal();
        List<LinearRing> boundaries =
                rings.stream()
                        .filter(b -> b.getEnvelopeInternal().intersects(clippedEnvelope))
                        .collect(Collectors.toList());

        // collect tagged boundary and tagged holes
        TaggedPolygon.TaggedLineString boundary =
                tagLineString(reversed.getExteriorRing(), boundaries);
        List<TaggedPolygon.TaggedLineString> holes =
                IntStream.range(0, reversed.getNumInteriorRing())
                        .mapToObj(i -> tagLineString(reversed.getInteriorRingN(i), boundaries))
                        .collect(Collectors.toList());

        TaggedPolygon tagged = new TaggedPolygon(boundary, holes);
        clipped.setUserData(tagged);
    }

    private TaggedPolygon.TaggedLineString tagLineString(
            LinearRing ring, List<LinearRing> boundaries) {
        // Grab the coordinates. A valid ring has at least 3
        Coordinate[] coordinates = ring.getCoordinates();
        // build first segment and test it
        Coordinate[] segment = new Coordinate[2];
        segment[0] = coordinates[0];
        segment[1] = coordinates[1];
        boolean visible = isSegmentVisible(segment, boundaries);
        List<Coordinate> currentSpan = new ArrayList<>(Arrays.asList(segment[0], segment[1]));
        List<TaggedCoordinateSequence> result = new ArrayList<>();

        // loop over the other segments and build spans
        for (int i = 2; i < coordinates.length; i++) {
            // move to next segment and test it
            segment[0] = segment[1];
            segment[1] = coordinates[i];
            boolean nextVisible = isSegmentVisible(segment, boundaries);
            if (nextVisible == visible) {
                // same visibility, add to current span
                currentSpan.add(segment[1]);
            } else {
                // Currently adding all points with duplications across the visible and non
                // visible lines. The boundary is normally fully drawn this way, but the filling
                // is not working correctly all the time, and the client sometimes draws lines that
                // do not exist. The old code that tried to avoid duplications has been commented
                // out, was failing in case there is a visible segment with only two coordinates,
                // that were also used by two nearby invisible segments. Maybe we should make
                // up a non existing coordinate in the middle in that case?

                // Visibility changed, close current span and start a new one.
                // The invisible span must be fully contained in the map-span, without duplicate
                // ordinate, so remove the last point from the previous span if it's invisible
                // However, if a visible segment is enclosed between two invisible ones, we
                // cannot leave its coordinates completely out, or it won't display at all...
                //                if (!nextVisible && currentSpan.size() > 1) {
                //                    currentSpan.remove(currentSpan.size() - 1);
                //                }
                result.add(new TaggedCoordinateSequence(visible, currentSpan));

                //                if (nextVisible) {
                //                    currentSpan = new ArrayList<>(Arrays.asList(segment[1]));
                //                } else {
                currentSpan = new ArrayList<>(Arrays.asList(segment[0], segment[1]));
                //                }
                visible = nextVisible;
            }
        }
        // close the last span
        result.add(new TaggedCoordinateSequence(visible, currentSpan));

        return new TaggedPolygon.TaggedLineString(result);
    }

    /**
     * For this method, other approaches have been explored and discarded:
     *
     * <ul>
     *   <li>Testing if the segment is in the original geometry boundary with "contains". Normally
     *       works, but some times fails due to floating point precision issues
     *   <li>Create a fixed precision PrecisionModel, pass the original geometry and the segment
     *       through a geometry precision reducer, test for boundary containment. Did not work
     *       (unclear why) and was also modifying in place the Coordinate objects, changing the
     *       output
     *   <li>Buffer the original geometry with the tolerance distance and test for containment. Was
     *       reliable, but too slow with complex geoemtries.
     * </ul>
     *
     * The current approach works in all examples we have and is fast, but should be improved to the
     * segment vs segment rather than linestring vs segment (if the linestring is W shaped the tests
     * could pass even if the segment is not contained in it, but only touches the tips of the W)
     *
     * @param segment
     * @param boundaries
     * @return
     */
    private boolean isSegmentVisible(Coordinate[] segment, List<LinearRing> boundaries) {
        // quick test, the segment must be on the boundary of the clip envelope
        Coordinate first = segment[0];
        Coordinate second = segment[1];
        if (!pointOnClipBoundary(first) || !pointOnClipBoundary(second)) {
            return true;
        }

        // so it's on the clip boundary, was it part of the original geometry or is it new?
        Envelope testEnvelope = new Envelope(first.x, second.x, first.y, second.y);
        PointPairDistance distance0 = new PointPairDistance();
        PointPairDistance distance1 = new PointPairDistance();
        PointPairDistance distanceMid = new PointPairDistance();
        Coordinate mid = new Coordinate((first.x + second.x) / 2, (first.y + second.y) / 2);
        for (LineString boundary : boundaries) {
            if (boundary.getEnvelopeInternal().contains(testEnvelope)) {
                DistanceToPoint.computeDistance(boundary, first, distance0);
                DistanceToPoint.computeDistance(boundary, second, distance1);
                DistanceToPoint.computeDistance(boundary, mid, distanceMid);
                if (distance0.getDistance() < eps
                        && distance1.getDistance() < eps
                        && distanceMid.getDistance() < eps) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean pointOnClipBoundary(Coordinate coordinate) {
        return coordinate.x == clipEnvelope.getMinX()
                || coordinate.x == clipEnvelope.getMaxX()
                || coordinate.y == clipEnvelope.getMinY()
                || coordinate.y == clipEnvelope.getMaxY();
    }
}
