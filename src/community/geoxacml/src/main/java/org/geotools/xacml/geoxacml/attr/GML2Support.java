/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
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
 */

package org.geotools.xacml.geoxacml.attr;

import java.util.List;
import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * 
 * Class for supporting GML2 encoding and GML2 Parsing
 * 
 * @author Christian Mueller
 * 
 */
public class GML2Support extends GMLSupport {

    public final static GMLSupport Singleton = new GML2Support();

    public final static String GML_POINT = "http://www.opengis.net/gml#Point";

    public final static String GML_BOX = "http://www.opengis.net/gml#Box";

    public final static String GML_LINESTRING = "http://www.opengis.net/gml#LineString";

    public final static String GML_LINEARRING = "http://www.opengis.net/gml#LinearRing";

    public final static String GML_POLYGON = "http://www.opengis.net/gml#Polygon";

    public final static String GML_MULTIPOINT = "http://www.opengis.net/gml#MultiPoint";

    public final static String GML_MULTILINESTRING = "http://www.opengis.net/gml#MultiLineString";

    public final static String GML_MULTIPOLYGON = "http://www.opengis.net/gml#MultiPolygon";

    GeometryAttribute buildFromGML(Node gmlNode) throws Exception {

        NamedNodeMap attrMap = gmlNode.getAttributes();
        Node n = attrMap.getNamedItem("gid");
        String gid = n == null ? null : n.getTextContent();
        n = attrMap.getNamedItem("srsName");
        String srsName = n == null ? null : n.getTextContent();

        if ("Box".equals(gmlNode.getLocalName())) {
            Polygon p = parseBox(gmlNode);
            GeometryAttribute attr = new GeometryAttribute(p, srsName, gid, GMLVersion.Version2,
                    GML_BOX);
            return attr;
        }

        if ("Point".equals(gmlNode.getLocalName())) {
            Point p = parsePoint(gmlNode);
            GeometryAttribute attr = new GeometryAttribute(p, srsName, gid, GMLVersion.Version2,
                    GML_POINT);
            return attr;
        }

        if ("LineString".equals(gmlNode.getLocalName())) {
            LineString ls = parseLineString(gmlNode);
            GeometryAttribute attr = new GeometryAttribute(ls, srsName, gid, GMLVersion.Version2,
                    GML_LINESTRING);
            return attr;
        }

        if ("LinearRing".equals(gmlNode.getLocalName())) {
            LinearRing lr = parseLinearRing(gmlNode);
            GeometryAttribute attr = new GeometryAttribute(lr, srsName, gid, GMLVersion.Version2,
                    GML_LINEARRING);
            return attr;
        }

        if ("Polygon".equals(gmlNode.getLocalName())) {
            Polygon poly = parsePolygon(gmlNode);
            GeometryAttribute attr = new GeometryAttribute(poly, srsName, gid, GMLVersion.Version2,
                    GML_POLYGON);
            return attr;
        }

        if ("MultiPoint".equals(gmlNode.getLocalName())) {
            MultiPoint mp = parseMultiPoint(gmlNode);
            GeometryAttribute attr = new GeometryAttribute(mp, srsName, gid, GMLVersion.Version2,
                    GML_MULTIPOINT);
            return attr;
        }

        if ("MultiLineString".equals(gmlNode.getLocalName())) {
            MultiLineString mls = parseMultiLineString(gmlNode);
            GeometryAttribute attr = new GeometryAttribute(mls, srsName, gid, GMLVersion.Version2,
                    GML_MULTILINESTRING);
            return attr;
        }

        if ("MultiPolygon".equals(gmlNode.getLocalName())) {
            MultiPolygon mp = parseMultiPolygon(gmlNode);
            GeometryAttribute attr = new GeometryAttribute(mp, srsName, gid, GMLVersion.Version2,
                    GML_MULTIPOLYGON);
            return attr;
        }

        return null;
    }

    void encodeASGML(GeometryAttribute attr, StringBuffer buff) {

        Map<String, String> abstractGeometryAttributeMap = abstractGeometryAttribueMap(attr);
        abstractGeometryAttributeMap.put("xmlns:" + GMLNS_PREFIX, GMLNS);

        if (GML_POINT.equals(attr.getGmlType()))
            encodePoint((Point) attr.getGeometry(), buff, abstractGeometryAttributeMap);
        if (GML_BOX.equals(attr.getGmlType()))
            encodeBox((Polygon) attr.getGeometry(), buff, abstractGeometryAttributeMap);
        if (GML_LINESTRING.equals(attr.getGmlType()))
            encodeLineString((LineString) attr.getGeometry(), buff, abstractGeometryAttributeMap);
        if (GML_LINEARRING.equals(attr.getGmlType()))
            encodeLinearRing((LinearRing) attr.getGeometry(), buff, abstractGeometryAttributeMap);
        if (GML_POLYGON.equals(attr.getGmlType()))
            encodePolygon((Polygon) attr.getGeometry(), buff, abstractGeometryAttributeMap);
        if (GML_MULTIPOINT.equals(attr.getGmlType()))
            encodeMultiPoint((MultiPoint) attr.getGeometry(), buff, abstractGeometryAttributeMap);
        if (GML_MULTILINESTRING.equals(attr.getGmlType()))
            encodeMultiLineString((MultiLineString) attr.getGeometry(), buff,
                    abstractGeometryAttributeMap);
        if (GML_MULTIPOLYGON.equals(attr.getGmlType()))
            encodeMultiPolygon((MultiPolygon) attr.getGeometry(), buff,
                    abstractGeometryAttributeMap);

    }

    protected void encodeCoordinates(CoordinateSequence coordinates, StringBuffer buf) {

        encodeStartTag("coordinates", buf, null);

        for (int i = 0; i < coordinates.size(); i++) {
            Coordinate c = coordinates.getCoordinate(i);
            buf.append(c.x);

            boolean y = (coordinates.getDimension() > 1) && !new Double(c.y).isNaN();
            if (y)
                buf.append("," + c.y);

            boolean z = y && (coordinates.getDimension() > 2) && !new Double(c.z).isNaN();
            if (z)
                buf.append("," + c.z);

            if (i < (coordinates.size() - 1))
                buf.append(" ");
        }

        encodeEndTag("coordinates", buf);
        ;
    }

    public Polygon parseBox(Node node) throws GMLException {

        List<Node> coordinates = getChildren(node, "coord");

        if (!coordinates.isEmpty() && (coordinates.size() == 2)) {
            Node n1 = (Node) coordinates.get(0);
            Node n2 = (Node) coordinates.get(1);
            Coordinate c1 = parseCoordGML2(n1);
            Coordinate c2 = parseCoordGML2(n2);

            return toGeometry(new Envelope(c1.x, c2.x, c1.y, c2.y));
        }

        if (!coordinates.isEmpty()) {
            throw new GMLException("Envelope can have only two coordinates");
        }

        Node coords = getChild(node, "coordinates");
        if (coords != null) {
            CoordinateSequence cs = parseCoordinatesGML2(coords);

            if (cs.size() != 2) {
                throw new GMLException("Envelope can have only two coordinates");
            }

            return toGeometry(new Envelope(cs.getX(0), cs.getX(1), cs.getY(0), cs.getY(1)));
        }

        throw new GMLException("Could not find coordinates for envelope");
    }

    public LineString parseLineString(Node node) throws GMLException {

        LineString ls = parseLineStringGML2(node);
        if (ls == null)
            throw new GMLException("Could not find \"coord\" or \"coordinates\"  for LineString");
        return ls;
    }

    public Polygon parsePolygon(Node node) throws GMLException {

        Node outer = getChild(node, "outerBoundaryIs");
        Node lr = getChild(outer, "LinearRing");
        LinearRing extRing = parseLinearRing(lr);

        List<Node> innerList = getChildren(node, "innerBoundaryIs");
        LinearRing[] innerRings = new LinearRing[innerList.size()];
        for (int i = 0; i < innerRings.length; i++) {
            lr = getChild(innerList.get(i), "LinearRing");
            innerRings[i] = parseLinearRing(lr);
        }

        return gf.createPolygon(extRing, innerRings);
    }

    public LinearRing parseLinearRing(Node node) throws GMLException {
        LinearRing lr = parseLinearRingGML2(node);
        if (lr == null)
            throw new GMLException("Could not find \"coord\" or \"coordinates\"  for LinearRing");
        return lr;
    }

    public void encodeBox(Polygon poly, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap) {
        encodeStartTag("Box", buff, abstractGeometryAttributeMap);
        Envelope env = poly.getEnvelopeInternal();
        Coordinate[] array = new Coordinate[] { new Coordinate(env.getMinX(), env.getMinY()),
                new Coordinate(env.getMaxX(), env.getMaxY()) };
        CoordinateSequence sequence = csFactory.create(array);
        encodeCoordinates(sequence, buff);
        encodeEndTag("Box", buff);
    }

    public void encodeLineString(LineString ls, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap) {
        encodeStartTag("LineString", buff, abstractGeometryAttributeMap);
        encodeCoordinates(ls.getCoordinateSequence(), buff);
        encodeEndTag("LineString", buff);
    }

    public void encodeLinearRing(LinearRing lr, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap) {
        encodeStartTag("LinearRing", buff, abstractGeometryAttributeMap);
        encodeCoordinates(lr.getCoordinateSequence(), buff);
        encodeEndTag("LinearRing", buff);
    }

    public Point parsePoint(Node node) throws GMLException {

        Point p = parsePointGML2(node);
        if (p == null)
            throw new GMLException("Could not find a \"coordinate\" odr \"coord\" for Point");
        return p;
    }

    public MultiPoint parseMultiPoint(Node node) throws GMLException {
        List<Node> pointMemberList = getChildren(node, "pointMember");
        Point[] array = new Point[pointMemberList.size()];

        for (int i = 0; i < pointMemberList.size(); i++) {
            Node pointNode = getChild(pointMemberList.get(i), "Point");
            array[i] = parsePoint(pointNode);
        }

        return gf.createMultiPoint(array);
    }

    public MultiLineString parseMultiLineString(Node node) throws GMLException {
        List<Node> lsMemberList = getChildren(node, "lineStringMember");
        LineString[] array = new LineString[lsMemberList.size()];

        for (int i = 0; i < lsMemberList.size(); i++) {
            Node lsNode = getChild(lsMemberList.get(i), "LineString");
            array[i] = parseLineString(lsNode);
        }

        return gf.createMultiLineString(array);
    }

    public MultiPolygon parseMultiPolygon(Node node) throws GMLException {
        List<Node> polygonMemberList = getChildren(node, "polygonMember");
        Polygon[] array = new Polygon[polygonMemberList.size()];

        for (int i = 0; i < polygonMemberList.size(); i++) {
            Node polygonNode = getChild(polygonMemberList.get(i), "Polygon");
            array[i] = parsePolygon(polygonNode);
        }

        return gf.createMultiPolygon(array);
    }

    public void encodePoint(Point p, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap) {
        encodeStartTag("Point", buff, abstractGeometryAttributeMap);
        CoordinateSequence sequence = csFactory.create(p.getCoordinates());
        encodeCoordinates(sequence, buff);
        encodeEndTag("Point", buff);
    }

    public void encodePolygon(Polygon poly, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap) {
        encodeStartTag("Polygon", buff, abstractGeometryAttributeMap);

        encodeStartTag("outerBoundaryIs", buff, null);
        encodeStartTag("LinearRing", buff, null);
        encodeCoordinates(poly.getExteriorRing().getCoordinateSequence(), buff);
        encodeEndTag("LinearRing", buff);
        encodeEndTag("outerBoundaryIs", buff);

        int countInner = poly.getNumInteriorRing();
        for (int i = 0; i < countInner; i++) {
            encodeStartTag("innerBoundaryIs", buff, null);
            encodeStartTag("LinearRing", buff, null);
            encodeCoordinates(poly.getInteriorRingN(i).getCoordinateSequence(), buff);
            encodeEndTag("LinearRing", buff);
            encodeEndTag("innerBoundaryIs", buff);
        }
        encodeEndTag("Polygon", buff);
    }

    public void encodeMultiPoint(MultiPoint mp, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap) {
        encodeStartTag("MultiPoint", buff, abstractGeometryAttributeMap);
        for (int i = 0; i < mp.getNumGeometries(); i++) {
            encodeStartTag("pointMember", buff, null);
            Point p = (Point) mp.getGeometryN(i);
            encodePoint(p, buff, null);
            encodeEndTag("pointMember", buff);
        }
        encodeEndTag("MultiPoint", buff);
    }

    public void encodeMultiLineString(MultiLineString mls, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap) {
        encodeStartTag("MultiLineString", buff, abstractGeometryAttributeMap);
        for (int i = 0; i < mls.getNumGeometries(); i++) {
            encodeStartTag("lineStringMember", buff, null);
            LineString ls = (LineString) mls.getGeometryN(i);
            encodeLineString(ls, buff, null);
            encodeEndTag("lineStringMember", buff);
        }
        encodeEndTag("MultiLineString", buff);
    }

    public void encodeMultiPolygon(MultiPolygon mp, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap) {
        encodeStartTag("MultiPolygon", buff, abstractGeometryAttributeMap);
        for (int i = 0; i < mp.getNumGeometries(); i++) {
            encodeStartTag("polygonMember", buff, null);
            Polygon p = (Polygon) mp.getGeometryN(i);
            encodePolygon(p, buff, null);
            encodeEndTag("polygonMember", buff);
        }
        encodeEndTag("MultiPolygon", buff);
    }

}
