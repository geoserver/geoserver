/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
 *    
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *    
 */
package org.geoserver.wps.jts;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

/**
 * A set of static functions powering the {@link GeometryProcessFactory}
 */
public class GeometryFunctions {

    /**
     * Maps the enumeration into a set of
     * 
     * @author Andrea Aime - OpenGeo
     * 
     */
    enum BufferCapStyle {
        Round(BufferParameters.CAP_ROUND), Flat(BufferParameters.CAP_FLAT), Square(
                BufferParameters.CAP_SQUARE);
        int value;

        private BufferCapStyle(int value) {
            this.value = value;
        }
    };

    @DescribeProcess(title = "Geometry containment check", description = "Checks if a contains b")
    static public boolean contains(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.contains(b);
    }

    @DescribeProcess(title = "Geometry emptiness check", description = "Checks if the provided geometry is empty")
    static public boolean isEmpty(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.isEmpty();
    }

    @DescribeProcess(title = "Geometry length", description = "Returns the geometry perimeters, computed using cartesian geometry expressions in the same unit of measure as the geometry (will not return a valid perimeter for geometries expressed geographic coordinates")
    static public double length(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.getLength();
    }

    @DescribeProcess(title = "Geometry intersection test", description = "Returns true if the two geometries intersect, false otherwise")
    static public boolean intersects(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.intersects(b);
    }

    @DescribeProcess(title = "Geometry validity test", description = "Returns true if the geometry is topologically valid, false otherwise")
    static public boolean isValid(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.isValid();
    }

    @DescribeProcess(title = "Geometry type detector", description = "Returns the type of the geometry (POINT,LINE,POLYGON,MULTIPOINT,MULTILINE,MULTIPOLYGON,GEOMETRY COLLECTION)")
    static public String geometryType(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.getGeometryType();
    }

    @DescribeProcess(title = "Point counter", description = "Returns the number of points in the geometry")
    static public int numPoints(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.getNumPoints();
    }

    @DescribeProcess(title = "Geometry simplicity test", description = "Returns true if the geometry is simple")
    static public boolean isSimple(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.isSimple();
    }

    @DescribeProcess(title = "Geometry distance", description = "Returns the minimum distance between a and b")
    static public double distance(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.distance(b);
    }

    @DescribeProcess(title = "Proximity check", description = "Returns true if the distance between the two geomeries is less than the specified value")
    static public boolean isWithinDistance(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b,
            @DescribeParameter(name = "distance") double distance) {
        return a.isWithinDistance(b, distance);
    }

    @DescribeProcess(title = "Geometry area", description = "Computes the geometry area (in a cartesian plane, using the same unit of measure as the geometry coordinates, don't use with geometries expressed in geographic coordinates)")
    static public double area(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.getArea();
    }

    @DescribeProcess(title = "Centroid", description = "Extracts a geometry centroid")
    static public Geometry centroid(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.getCentroid();
    }

    @DescribeProcess(title = "Interior point", description = "Returns a point that lies inside the geometry, or at most is located on its boundary")
    static public Geometry interiorPoint(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.getInteriorPoint();
    }

    @DescribeProcess(title = "Geometry dimension", description = "Returns 0 for points, 1 for lines, 2 for polygons")
    static public int dimension(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.getDimension();
    }

    @DescribeProcess(title = "Boundary", description = "Returns a geometry boundary, or an empty geometry if there is no boundary")
    static public Geometry boundary(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.getBoundary();
    }

    @DescribeProcess(title = "Envelope", description = "Returns the geometry envelope either as a Polygon, or a Point if the input is a Point")
    static public Geometry envelope(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.getEnvelope();
    }

    @DescribeProcess(title = "Disjoint check", description = "Returns true if the two geometries have no points in common")
    static public boolean disjoint(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.disjoint(b);
    }

    @DescribeProcess(title = "Touch check", description = "Returns true if the two geometries touch each other")
    static public boolean touches(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.touches(b);
    }

    @DescribeProcess(title = "Crossing check", description = "Returns true if the two geometries cross each other")
    static public boolean crosses(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.crosses(b);
    }

    @DescribeProcess(title = "Within check", description = "Returns true if a is within b")
    static public boolean within(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.within(b);
    }

    @DescribeProcess(title = "Overlap check", description = "Returns true if a overlaps with b")
    static public boolean overlaps(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.overlaps(b);
    }

    @DescribeProcess(title = "Relate check", description = "Returns true if a and b DE-9IM intersection matrix matches the provided pattern")
    static public boolean relatePattern(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b, String pattern) {
        return a.relate(b, pattern);
    }

    @DescribeProcess(title = "Relate", description = "Returns the DE-9IM intersection matrix of the two geometries")
    static public String relate(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.relate(b).toString();
    }

    @DescribeProcess(title = "Geometry buffer", description = "Buffers a geometry using a certain distance")
    @DescribeResult(description = "The buffered geometry")
    static public Geometry buffer(
            @DescribeParameter(name = "geom", description = "The geometry to be buffered") Geometry geom,
            @DescribeParameter(name = "distance", description = "The distance (same unit of measure as the geometry)") double distance,
            @DescribeParameter(name = "quadrantSegments", description = "Number of quadrant segments. Use > 0 for round joins, 0 for flat joins, < 0 for mitred joins", min = 0) Integer quadrantSegments,
            @DescribeParameter(name = "capStyle", description = "The buffer cap style, round, flat, square", min = 0) BufferCapStyle capStyle) {
        if (quadrantSegments == null)
            quadrantSegments = BufferParameters.DEFAULT_QUADRANT_SEGMENTS;
        if (capStyle == null)
            capStyle = BufferCapStyle.Round;
        return geom.buffer(distance, quadrantSegments, capStyle.value);
    }

    @DescribeProcess(title = "Convex hull", description = "Returns the convex hull of the specified geometry")
    static public Geometry convexHull(@DescribeParameter(name = "geom") Geometry geom) {
        return geom.convexHull();
    }

    @DescribeProcess(title = "Intersection", description = "Returns the intersectoin between a and b (eventually an empty collection if there is no intersection)")
    static public Geometry intersection(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.intersection(b);
    }

    @DescribeProcess(title = "Union", description = "Performs the geometric union of two or more geometries")
    @DescribeResult(description = "The union of all input geometries")
    static public Geometry union(
            @DescribeParameter(name = "geom", description = "The geometries to be united", min = 2) Geometry... geoms) {
        Geometry result = null;
        for (Geometry g : geoms) {
            if (result == null) {
                result = g;
            } else {
                result = result.union(g);
            }
        }
        return result;
    }

    @DescribeProcess(title = "Difference", description = "Returns the difference between a and b (all the points that are in a but not in b)")
    static public Geometry difference(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.difference(b);
    }

    @DescribeProcess(title = "Symmetrical difference", description = "Returns a geometry made of points that are in a or b, but not in both")
    static public Geometry symDifference(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.symDifference(b);
    }

    @DescribeProcess(title = "EqualsExactTolerance", description = "Returns true if the two geometries are exactly the same, minus small differences in coordinate values")
    static public boolean equalsExactTolerance(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b, double tolerance) {
        return a.equalsExact(b, tolerance);
    }

    @DescribeProcess(title = "EqualsExact", description = "Returns true if the two geometries are exactly the same")
    static public boolean equalsExact(@DescribeParameter(name = "a") Geometry a,
            @DescribeParameter(name = "b") Geometry b) {
        return a.equalsExact(b);
    }

    @DescribeProcess(title = "Geometry count", description = "Returns the number of elements in the geometry collection, or one if it's not a collection")
    static public int numGeometries(Geometry collection) {
        return collection.getNumGeometries();
    }

    @DescribeProcess(title = "N-th geometry", description = "Returns the n-th geomery in the collection")
    static public Geometry getGeometryN(GeometryCollection collection, int index) {
        return collection.getGeometryN(index);
    }

    @DescribeProcess(title = "GetX", description = "Returns the X ordinate of the point")
    static public double getX(Point point) {
        return point.getX();
    }

    @DescribeProcess(title = "GetY", description = "Returns the Y ordinate of the point")
    static public double getY(Point point) {
        return point.getY();
    }

    @DescribeProcess(title = "Closed", description = "Returns true if the line is closed")
    static public boolean isClosed(LineString line) {
        return line.isClosed();
    }

    @DescribeProcess(title = "N-th point", description = "Returns the n-th point in the line")
    static public Point pointN(LineString line, int index) {
        return line.getPointN(index);
    }

    @DescribeProcess(title = "Start point", description = "Returns the start point of the line")
    static public Point startPoint(LineString line) {
        return line.getStartPoint();
    }

    @DescribeProcess(title = "End point", description = "Returns the end point of the line")
    static public Point endPoint(LineString line) {
        return line.getEndPoint();
    }

    @DescribeProcess(title = "Ring", description = "Returns true if the line is a ring")
    static public boolean isRing(LineString line) {
        return line.isRing();
    }

    @DescribeProcess(title = "Exterior ring", description = "Returns the exterior ring of the polygon")
    static public Geometry exteriorRing(Polygon polygon) {
        return polygon.getExteriorRing();
    }

    @DescribeProcess(title = "Interior ring count", description = "Returns the number of interior rings in the polygon")
    static public int numInteriorRing(Polygon polygon) {
        return polygon.getNumInteriorRing();
    }

    @DescribeProcess(title = "N-th interorior ring", description = "Returns the n-th interior ring in the polygon")
    static public Geometry interiorRingN(Polygon polygon, int index) {
        return polygon.getInteriorRingN(index);
    }

    @DescribeProcess(title = "Simplify", description = "Simplifies the geometry using the specified distance using the Douglas-Peuker algorithm")
    static public Geometry simplify(@DescribeParameter(name = "geom") Geometry geom,
            @DescribeParameter(name = "distance") double distance) {
        return DouglasPeuckerSimplifier.simplify(geom, distance);
    }

    @DescribeProcess(title = "Densify", description = "Densifies the geometry using the specified distance")
    static public Geometry densify(@DescribeParameter(name = "geom") Geometry geom,
            @DescribeParameter(name = "distance") double distance) {
        return Densifier.densify(geom, distance);
    }
}
