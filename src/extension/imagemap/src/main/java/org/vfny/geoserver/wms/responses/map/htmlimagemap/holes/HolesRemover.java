/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap.holes;

import java.util.ArrayList;
import javax.vecmath.GVector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.vfny.geoserver.wms.responses.map.htmlimagemap.utils.CyclicalList;
import org.vfny.geoserver.wms.responses.map.htmlimagemap.utils.IndexableCyclicalLinkedList;

/**
 * Utility class able to transform a polygon with holes into one without holes, cutting the original
 * geometry.
 *
 * <p>HolesRemover is an implementation of Dave Eberly's ear clipping algorithm as described here:
 * http://www.geometrictools.com/Documentation/TriangulationByEarClipping.pdf.
 *
 * <p>The code is taken from Triangulator, by Nick Gravelyn (http://www.nickontech.com) which is
 * open sourced under the terms of the Ms-PL.
 *
 * <p>Microsoft Public License (Ms-PL), which follows: This license governs use of the accompanying
 * software. If you use the software, you accept this license. If you do not accept the license, do
 * not use the software. 1. Definitions The terms "reproduce," "reproduction," "derivative works,"
 * and "distribution" have the same meaning here as under U.S. copyright law. A "contribution" is
 * the original software, or any additions or changes to the software. A "contributor" is any person
 * that distributes its contribution under this license. "Licensed patents" are a contributor's
 * patent claims that read directly on its contribution. 2. Grant of Rights (A) Copyright Grant-
 * Subject to the terms of this license, including the license conditions and limitations in section
 * 3, each contributor grants you a non-exclusive, worldwide, royalty-free copyright license to
 * reproduce its contribution, prepare derivative works of its contribution, and distribute its
 * contribution or any derivative works that you create. (B) Patent Grant- Subject to the terms of
 * this license, including the license conditions and limitations in section 3, each contributor
 * grants you a non-exclusive, worldwide, royalty-free license under its licensed patents to make,
 * have made, use, sell, offer for sale, import, and/or otherwise dispose of its contribution in the
 * software or derivative works of the contribution in the software. 3. Conditions and Limitations
 * (A) No Trademark License- This license does not grant you rights to use any contributors' name,
 * logo, or trademarks. (B) If you bring a patent claim against any contributor over patents that
 * you claim are infringed by the software, your patent license from such contributor to the
 * software ends automatically. (C) If you distribute any portion of the software, you must retain
 * all copyright, patent, trademark, and attribution notices that are present in the software. (D)
 * If you distribute any portion of the software in source code form, you may do so only under this
 * license by including a complete copy of this license with your distribution. If you distribute
 * any portion of the software in compiled or object code form, you may only do so under a license
 * that complies with this license. (E) The software is licensed "as-is." You bear the risk of using
 * it. The contributors give no express warranties, guarantees or conditions. You may have
 * additional consumer rights under your local laws which this license cannot change. To the extent
 * permitted under your local laws, the contributors exclude the implied warranties of
 * merchantability, fitness for a particular purpose and non-infringement.
 *
 * @author m.bartolomeoli
 */
public class HolesRemover {
    // winding directions constants
    public static final int WINDING_COUNTER_CLOCKWISE = 0;
    public static final int WINDING_CLOCKWISE = 1;

    private LineString shapeVerts = null;
    private LineString holeVerts = null;
    private GeometryFactory gFac = null;

    private IndexableCyclicalLinkedList polygonVertices = new IndexableCyclicalLinkedList();
    private CyclicalList convexVertices = new CyclicalList();
    private CyclicalList reflexVertices = new CyclicalList();

    // minimum area to consider a hole;
    // holes with a lesser area will be skipped
    private static final double HOLE_AREA_TOLERANCE = 100.0;

    protected HolesRemover(LineString boundary, LineString hole, GeometryFactory fac) {
        super();
        this.shapeVerts = boundary;
        this.holeVerts = hole;
        gFac = fac;
    }

    /** Gets a new polygon without holes from the given polygon. */
    public static Polygon removeHoles(Polygon poly, double scale) {
        GeometryFactory gFac = new GeometryFactory(poly.getPrecisionModel(), poly.getSRID());
        // extracts the exterior ring that will be used as
        // a starting polygon from which holes will be cut
        LineString result = poly.getExteriorRing();
        // cut every hole from the exterior ring
        for (int holeCount = 0; holeCount < poly.getNumInteriorRing(); holeCount++) {
            LineString hole = poly.getInteriorRingN(holeCount);
            if (!skipHole(hole, scale)) {
                // call holes remover to cut the current hole
                HolesRemover remover = new HolesRemover(result, hole, gFac);
                result = remover.cutHole();
            }
        }
        // return a new polygon from the new boundary
        LinearRing resultRing = gFac.createLinearRing(result.getCoordinates());
        return gFac.createPolygon(resultRing, new LinearRing[] {});
    }

    private static boolean skipHole(LineString hole, double scale) {
        GeometryFactory gFac = new GeometryFactory(hole.getPrecisionModel(), hole.getSRID());
        LinearRing ext = gFac.createLinearRing(hole.getCoordinates());
        Polygon holePoly = gFac.createPolygon(ext, new LinearRing[] {});
        // if hole area is less than the tolerance, skip it
        if (holePoly.getArea() < HOLE_AREA_TOLERANCE * scale * scale) return true;

        return false;
    }

    /** Cuts the configured polygon with the hole. */
    private LineString cutHole() {
        // boundary must be counterclockwise
        shapeVerts = ensureWindingOrder(shapeVerts, WINDING_COUNTER_CLOCKWISE);
        // hole must be clockwise
        holeVerts = ensureWindingOrder(holeVerts, WINDING_CLOCKWISE);

        // generate the cyclical list of vertices in the polygon
        for (int i = 0; i < shapeVerts.getNumPoints(); i++)
            polygonVertices.addLast(new Vertex(shapeVerts.getCoordinateN(i), i));

        // generate the cyclical list of vertices in the hole
        CyclicalList holePolygon = new CyclicalList();
        for (int i = 0; i < holeVerts.getNumPoints(); i++)
            holePolygon.add(new Vertex(holeVerts.getCoordinateN(i), i + polygonVertices.size()));

        // calc list of convex and reflex vertices
        findConvexAndReflexVertices();

        // find the hole vertex with the largest X value
        Vertex rightMostHoleVertex = (Vertex) holePolygon.get(0);
        for (int count = 0; count < holePolygon.size(); count++) {
            Vertex v = (Vertex) holePolygon.get(count);
            if (v.getPosition().x > rightMostHoleVertex.getPosition().x) rightMostHoleVertex = v;
        }
        // construct a list of all line segments where at least one vertex
        // is to the right of the rightmost hole vertex with one vertex
        // above the hole vertex and one below
        ArrayList segmentsToTest = new ArrayList();
        for (int i = 0; i < polygonVertices.size(); i++) {
            Vertex a = (Vertex) polygonVertices.get(i);
            Vertex b = (Vertex) polygonVertices.get(i + 1);

            if ((a.getPosition().x > rightMostHoleVertex.getPosition().x
                            || b.getPosition().x > rightMostHoleVertex.getPosition().x)
                    && ((a.getPosition().y >= rightMostHoleVertex.getPosition().y
                                    && b.getPosition().y <= rightMostHoleVertex.getPosition().y)
                            || (a.getPosition().y <= rightMostHoleVertex.getPosition().y
                                    && b.getPosition().y >= rightMostHoleVertex.getPosition().y)))
                segmentsToTest.add(new LineSegment(a, b));
        }

        // now we try to find the closest intersection point heading to the right from
        // our hole vertex.
        Float closestPoint = null;
        LineSegment closestSegment = new LineSegment();
        for (int count = 0; count < segmentsToTest.size(); count++) {
            LineSegment segment = (LineSegment) segmentsToTest.get(count);
            Float intersection =
                    segment.intersectsWithRay(
                            rightMostHoleVertex.getPosition(), new Coordinate(1.0, 0.0));
            if (intersection != null) {
                if (closestPoint == null || closestPoint.floatValue() > intersection.floatValue()) {
                    closestPoint = intersection;
                    closestSegment = segment;
                }
            }
        }

        // if closestPoint is null, there were no collisions (likely from improper input data),
        // but we'll just return without doing anything else
        if (closestPoint == null) return shapeVerts;

        // otherwise we can find our mutually visible vertex to split the polygon

        Coordinate I = rightMostHoleVertex.getPosition();
        GVector i = new GVector(new double[] {I.x, I.y});
        i.add(new GVector(new double[] {closestPoint.floatValue(), 0.0}));

        Vertex P =
                (closestSegment.A.getPosition().x > closestSegment.B.getPosition().x)
                        ? closestSegment.A
                        : closestSegment.B;

        // construct triangle MIP
        Triangle mip = new Triangle(rightMostHoleVertex, new Vertex(I, 1), P);
        // see if any of the reflex vertices lie inside of the MIP triangle
        ArrayList interiorReflexVertices = new ArrayList();
        for (int count = 0; count < reflexVertices.size(); count++) {
            Vertex v = (Vertex) reflexVertices.get(count);
            if (mip.ContainsPoint(v)) interiorReflexVertices.add(v);
        }

        // if there are any interior reflex vertices, find the one that, when connected
        // to our rightMostHoleVertex, forms the line closest to Vector2.UnitX
        if (interiorReflexVertices.size() > 0) {
            float closestDot = -1f;
            for (int count = 0; count < interiorReflexVertices.size(); count++) {
                Vertex v = (Vertex) interiorReflexVertices.get(count);
                GVector n = new GVector(new double[] {v.getPosition().x, v.getPosition().y});
                n.sub(
                        new GVector(
                                new double[] {
                                    rightMostHoleVertex.getPosition().x,
                                    rightMostHoleVertex.getPosition().y
                                }));
                n.normalize();
                GVector m = new GVector(new double[] {1.0, 0.0});
                float dot = (float) m.dot(n);

                // if this line is the closest we've found
                if (dot > closestDot) {
                    // save the value and save the vertex as P
                    closestDot = dot;
                    P = v;
                }
            }
        }

        // now we just form our output array by injecting the hole vertices into place
        // we know we have to inject the hole into the main array after point P going from
        // rightMostHoleVertex around and then back to P.
        int mIndex = holePolygon.indexOf(rightMostHoleVertex);
        int injectPoint = polygonVertices.indexOf(P) + 1;

        for (int count = mIndex; count <= mIndex + holePolygon.size(); count++) {

            polygonVertices.add(injectPoint++, holePolygon.get(count));
        }
        polygonVertices.add(injectPoint, P);

        Coordinate[] newShapeVerts = new Coordinate[polygonVertices.size()];
        for (int count = 0; count < polygonVertices.size(); count++)
            newShapeVerts[count] = ((Vertex) polygonVertices.get(count)).getPosition();

        return gFac.createLineString(newShapeVerts);
    }

    private void findConvexAndReflexVertices() {
        for (int i = 0; i < polygonVertices.size(); i++) {
            Vertex v = (Vertex) polygonVertices.get(i);

            if (isConvex(v)) {
                convexVertices.add(v);
            } else {
                reflexVertices.add(v);
            }
        }
    }

    private boolean isConvex(Vertex c) {
        Vertex p = (Vertex) polygonVertices.get(polygonVertices.indexOf(c) - 1);
        Vertex n = (Vertex) polygonVertices.get(polygonVertices.indexOf(c) + 1);
        Coordinate cc = c.getPosition();
        Coordinate pc = p.getPosition();
        Coordinate nc = n.getPosition();
        GVector d1 = new GVector(new double[] {cc.x, cc.y});
        d1.sub(new GVector(new double[] {pc.x, pc.y}));
        d1.normalize();

        GVector d2 = new GVector(new double[] {nc.x, nc.y});
        d2.sub(new GVector(new double[] {cc.x, cc.y}));
        d2.normalize();

        GVector n2 = new GVector(new double[] {-d2.getElement(1), d2.getElement(0)});
        return d1.dot(n2) <= 0.0;
    }

    public static LineString ensureWindingOrder(LineString vertices, int windingOrder) {

        if (determineWindingOrder(vertices) != windingOrder) {

            return reverseWindingOrder(vertices);
        }

        return vertices;
    }

    private static LineString reverseWindingOrder(LineString vertices) {
        return (LineString) vertices.reverse();
    }

    private static int determineWindingOrder(LineString vertices) {
        int clockWiseCount = 0;
        int counterClockWiseCount = 0;

        Coordinate p1 = vertices.getCoordinateN(0);

        for (int i = 1; i < vertices.getNumPoints(); i++) {
            Coordinate p2 = vertices.getCoordinateN(i);
            Coordinate p3 = vertices.getCoordinateN((i + 1) % vertices.getNumPoints());
            GVector e1 = new GVector(new double[] {p1.x, p1.y});
            e1.sub(new GVector(new double[] {p2.x, p2.y}));
            GVector e2 = new GVector(new double[] {p3.x, p3.y});
            e2.sub(new GVector(new double[] {p2.x, p2.y}));

            if (e1.getElement(0) * e2.getElement(1) - e1.getElement(1) * e2.getElement(0) >= 0)
                clockWiseCount++;
            else counterClockWiseCount++;

            p1 = p2;
        }

        return (clockWiseCount > counterClockWiseCount)
                ? WINDING_CLOCKWISE
                : WINDING_COUNTER_CLOCKWISE;
    }
}
