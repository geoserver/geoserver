/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap.holes;

/**
 * Triangle geometry representation. It has the 3 points as properties (A,B,C).
 *
 * @author m.bartolomeoli
 */
public class Triangle {
    public Vertex A;
    public Vertex B;
    public Vertex C;

    public Triangle(Vertex a, Vertex b, Vertex c) {
        A = a;
        B = b;
        C = c;
    }

    /** Verifies if the given point is internal for this triangle. */
    public boolean ContainsPoint(Vertex point) {
        // return true if the point to test is one of the vertices
        if (point.equals(A) || point.equals(B) || point.equals(C)) return true;

        boolean oddNodes = false;

        if (checkPointToSegment(C, A, point)) oddNodes = !oddNodes;
        if (checkPointToSegment(A, B, point)) oddNodes = !oddNodes;
        if (checkPointToSegment(B, C, point)) oddNodes = !oddNodes;

        return oddNodes;
    }

    /** Verifies if the given point is internal for the triangle build from (a,b,c). */
    public static boolean ContainsPoint(Vertex a, Vertex b, Vertex c, Vertex point) {
        return new Triangle(a, b, c).ContainsPoint(point);
    }

    static boolean checkPointToSegment(Vertex sA, Vertex sB, Vertex point) {
        if ((sA.getPosition().y < point.getPosition().y
                        && sB.getPosition().y >= point.getPosition().y)
                || (sB.getPosition().y < point.getPosition().y
                        && sA.getPosition().y >= point.getPosition().y)) {
            double x =
                    sA.getPosition().x
                            + (point.getPosition().y - sA.getPosition().y)
                                    / (sB.getPosition().y - sA.getPosition().y)
                                    * (sB.getPosition().x - sA.getPosition().x);

            if (x < point.getPosition().x) return true;
        }

        return false;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Triangle)) return false;
        Triangle t = (Triangle) obj;
        return t.A.equals(A) && t.B.equals(B) && t.C.equals(C);
    }

    public int hashCode() {
        int result = A.hashCode();
        result = (result * 397) ^ B.hashCode();
        result = (result * 397) ^ C.hashCode();
        return result;
    }
}
