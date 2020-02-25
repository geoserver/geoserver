/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap.holes;

import javax.vecmath.GVector;
import org.locationtech.jts.geom.Coordinate;

/**
 * Represents a segment. It has the 2 ending points as properties (A,B).
 *
 * @author m.bartolomeoli
 */
public class LineSegment {
    public Vertex A;
    public Vertex B;

    public LineSegment() {}

    public LineSegment(Vertex a, Vertex b) {
        A = a;
        B = b;
    }

    /**
     * Checks if the segment intersects a "ray "starting from the given origin and "going" in the
     * given direction.
     */
    public Float intersectsWithRay(Coordinate origin, Coordinate direction) {
        float largestDistance =
                Math.max(
                                (float) (A.getPosition().x - origin.x),
                                (float) (B.getPosition().x - origin.x))
                        * 2f;
        GVector v = new GVector(new double[] {origin.x, origin.y});
        GVector d = new GVector(new double[] {direction.x, direction.y});
        d.scale(largestDistance);
        v.add(d);
        LineSegment raySegment =
                new LineSegment(
                        new Vertex(origin, 0),
                        new Vertex(new Coordinate(v.getElement(0), v.getElement(1)), 0));
        Coordinate intersection = findIntersection(this, raySegment);
        Float value = null;

        if (intersection != null) {
            v = new GVector(new double[] {origin.x, origin.y});
            v.sub(new GVector(new double[] {intersection.x, intersection.y}));
            double dist = v.norm();
            value = Float.valueOf((float) dist);
        }

        return value;
    }

    /** Gets the intersection point of the 2 given segments (null if they don't intersect). */
    public static Coordinate findIntersection(LineSegment a, LineSegment b) {
        double x1 = a.A.getPosition().x;
        double y1 = a.A.getPosition().y;
        double x2 = a.B.getPosition().x;
        double y2 = a.B.getPosition().y;
        double x3 = b.A.getPosition().x;
        double y3 = b.A.getPosition().y;
        double x4 = b.B.getPosition().x;
        double y4 = b.B.getPosition().y;

        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);

        double uaNum = (x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3);
        double ubNum = (x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3);

        double ua = uaNum / denom;
        double ub = ubNum / denom;

        if (clamp(ua, 0f, 1f) != ua || clamp(ub, 0f, 1f) != ub) return null;
        GVector v = new GVector(new double[] {a.A.getPosition().x, a.A.getPosition().y});
        GVector d = new GVector(new double[] {a.B.getPosition().x, a.B.getPosition().y});
        d.sub(v);
        d.scale(ua);
        d.add(v);
        return new Coordinate(d.getElement(0), d.getElement(1));
    }

    /** Restricts a value to be in the given range (min - max). */
    public static double clamp(double value, double min, double max) {
        if (value > max) return max;
        if (value < min) return min;
        return value;
    }
}
