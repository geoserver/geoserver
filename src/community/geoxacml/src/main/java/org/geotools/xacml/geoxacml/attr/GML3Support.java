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
public class GML3Support extends GMLSupport {

    public final static String GML_POINT = "http://www.opengis.net/gml#Point";

    public final static String GML_LINESTRING = "http://www.opengis.net/gml#LineString";

    public final static String GML_LINEARRING = "http://www.opengis.net/gml#LinearRing";

    public final static String GML_POLYGON = "http://www.opengis.net/gml#Polygon";

    public final static String GML_ENVELOPE = "http://www.opengis.net/gml#Envelope";

    public final static String GML_MULTIPOINT = "http://www.opengis.net/gml#MultiPoint";

    public final static String GML_MULTICURVE = "http://www.opengis.net/gml#MultiCurve";

    public final static String GML_MULTISURFACE = "http://www.opengis.net/gml#MultiSurface";

    public final static GML3Support Singleton = new GML3Support();

    GeometryAttribute buildFromGML(Node gmlNode) throws Exception {

        NamedNodeMap attrMap = gmlNode.getAttributes();
        Node n = attrMap.getNamedItem("gid");
        String gid = n == null ? null : n.getTextContent();
        n = attrMap.getNamedItem("srsName");
        String srsName = n == null ? null : n.getTextContent();

        n = attrMap.getNamedItem("srsDimension");
        int srsDimension = n == null ? 2 : Integer.valueOf(n.getTextContent());

        if ("Point".equals(gmlNode.getLocalName())) {
            Point p = parsePoint(gmlNode, srsDimension);
            GeometryAttribute attr = new GeometryAttribute(p, srsName, gid, GMLVersion.Version3,
                    GML_POINT);
            attr.setSrsDimension(srsDimension);
            return attr;
        }

        if ("LineString".equals(gmlNode.getLocalName())) {
            LineString ls = parseLineString(gmlNode, srsDimension);
            GeometryAttribute attr = new GeometryAttribute(ls, srsName, gid, GMLVersion.Version3,
                    GML_LINESTRING);
            attr.setSrsDimension(srsDimension);
            return attr;
        }

        if ("LinearRing".equals(gmlNode.getLocalName())) {
            LinearRing lr = parseLinearRing(gmlNode, srsDimension);
            GeometryAttribute attr = new GeometryAttribute(lr, srsName, gid, GMLVersion.Version3,
                    GML_LINEARRING);
            attr.setSrsDimension(srsDimension);
            return attr;
        }

        if ("Polygon".equals(gmlNode.getLocalName())) {
            Polygon poly = parsePolygon(gmlNode, srsDimension);
            GeometryAttribute attr = new GeometryAttribute(poly, srsName, gid, GMLVersion.Version3,
                    GML_POLYGON);
            attr.setSrsDimension(srsDimension);
            return attr;
        }

        if ("Envelope".equals(gmlNode.getLocalName())) {
            Polygon poly = parseEnvelope(gmlNode, srsDimension);
            GeometryAttribute attr = new GeometryAttribute(poly, srsName, gid, GMLVersion.Version3,
                    GML_ENVELOPE);
            attr.setSrsDimension(srsDimension);
            return attr;
        }

        if ("MultiPoint".equals(gmlNode.getLocalName())) {
            MultiPoint mp = parseMultiPoint(gmlNode, srsDimension);
            GeometryAttribute attr = new GeometryAttribute(mp, srsName, gid, GMLVersion.Version3,
                    GML_MULTIPOINT);
            attr.setSrsDimension(srsDimension);
            return attr;
        }

        if ("MultiCurve".equals(gmlNode.getLocalName())) {
            MultiLineString mp = parseMultiCurve(gmlNode, srsDimension);
            GeometryAttribute attr = new GeometryAttribute(mp, srsName, gid, GMLVersion.Version3,
                    GML_MULTICURVE);
            attr.setSrsDimension(srsDimension);
            return attr;
        }

        if ("MultiSurface".equals(gmlNode.getLocalName())) {
            MultiPolygon mp = parseMultiSurface(gmlNode, srsDimension);
            GeometryAttribute attr = new GeometryAttribute(mp, srsName, gid, GMLVersion.Version3,
                    GML_MULTISURFACE);
            attr.setSrsDimension(srsDimension);
            return attr;
        }

        return null;
    }

    void encodeASGML(GeometryAttribute attr, StringBuffer buff) {
        Map<String, String> abstractGeometryAttributeMap = abstractGeometryAttribueMap(attr);
        abstractGeometryAttributeMap.put("xmlns:" + GMLNS_PREFIX, GMLNS);

        if (GML_POINT.equals(attr.getGmlType()))
            encodePoint((Point) attr.getGeometry(), buff, abstractGeometryAttributeMap, attr
                    .getSrsDimension());

        if (GML_LINESTRING.equals(attr.getGmlType()))
            encodeLineString((LineString) attr.getGeometry(), buff, abstractGeometryAttributeMap,
                    attr.getSrsDimension());

        if (GML_LINEARRING.equals(attr.getGmlType()))
            encodeLinearRing((LinearRing) attr.getGeometry(), buff, abstractGeometryAttributeMap,
                    attr.getSrsDimension());

        if (GML_POLYGON.equals(attr.getGmlType()))
            encodePolygon((Polygon) attr.getGeometry(), buff, abstractGeometryAttributeMap, attr
                    .getSrsDimension());

        if (GML_ENVELOPE.equals(attr.getGmlType()))
            encodeEnvelope((Polygon) attr.getGeometry(), buff, abstractGeometryAttributeMap, attr
                    .getSrsDimension());

        if (GML_MULTIPOINT.equals(attr.getGmlType()))
            encodeMultiPoint((MultiPoint) attr.getGeometry(), buff, abstractGeometryAttributeMap,
                    attr.getSrsDimension());

        if (GML_MULTICURVE.equals(attr.getGmlType()))
            encodeMultiCurve((MultiLineString) attr.getGeometry(), buff,
                    abstractGeometryAttributeMap, attr.getSrsDimension());

        if (GML_MULTISURFACE.equals(attr.getGmlType()))
            encodeMultiSurface((MultiPolygon) attr.getGeometry(), buff,
                    abstractGeometryAttributeMap, attr.getSrsDimension());

    }

    private double[] parseDoubleList(Node node) throws GMLException {

        final String anyBlankSeq = "\\s+";
        final String singleSpace = " ";
        String text = node.getTextContent().replaceAll(anyBlankSeq, singleSpace).trim();
        String[] values = text.split(" +");
        double[] doubles = new double[values.length];
        for (int i = 0; i < values.length; i++)
            doubles[i] = Double.valueOf(values[i]);
        return doubles;
    }

    public LinearRing parseLinearRing(Node node, int srsDimension) throws GMLException {

        List<Node> coordinates = getChildren(node, "pos");

        if (!coordinates.isEmpty() && (coordinates.size() >= 4)) {
            Coordinate[] coordArray = new Coordinate[coordinates.size()];
            for (int i = 0; i < coordinates.size(); i++) {
                coordArray[i] = parseDirectPosition(coordinates.get(i), srsDimension);
            }
            return gf.createLinearRing(coordArray);
        }

        if (!coordinates.isEmpty()) {
            throw new GMLException(LINEARRING_ERROR);
        }

        Node coords = getChild(node, "posList");
        if (coords != null) {
            CoordinateSequence cs = parseDirectPositionList(coords, srsDimension);

            if (cs.size() < 4) {
                throw new GMLException(LINEARRING_ERROR);
            }

            return gf.createLinearRing(cs);
        }

        // try GML2 stuff
        LinearRing lr = parseLinearRingGML2(node);
        if (lr == null)
            throw new GMLException(
                    "Could not find \"pos\",\"posList\",\"coords\" or \"coordinates\"  for LinearRing");
        return lr;
    }

    public LineString parseLineString(Node node, int srsDimension) throws GMLException {

        List<Node> coordinates = getChildren(node, "pos");

        if (!coordinates.isEmpty() && (coordinates.size() >= 2)) {
            Coordinate[] coordArray = new Coordinate[coordinates.size()];
            for (int i = 0; i < coordinates.size(); i++) {
                coordArray[i] = parseDirectPosition(coordinates.get(i), srsDimension);
            }
            return gf.createLineString(coordArray);
        }

        if (!coordinates.isEmpty()) {
            throw new GMLException(LINESTRING_ERROR);
        }

        Node coords = getChild(node, "posList");
        if (coords != null) {
            CoordinateSequence cs = parseDirectPositionList(coords, srsDimension);

            if (cs.size() < 2) {
                throw new GMLException(LINESTRING_ERROR);
            }

            return gf.createLineString(cs);
        }

        // try GML2 stuff
        LineString ls = parseLineStringGML2(node);
        if (ls == null)
            throw new GMLException(
                    "Could not find \"pos\",\"posList\",\"coords\" or \"coordinates\"  for LinearRing");
        return ls;

    }

    protected Coordinate parseDirectPosition(Node node, int srsDimension) throws GMLException {

        double[] oords = parseDoubleList(node);

        if (srsDimension == 1)
            return new Coordinate(oords[0], Double.NaN);
        if (srsDimension == 2)
            return new Coordinate(oords[0], oords[1]);

        if (srsDimension == 3)
            return new Coordinate(oords[0], oords[1], oords[2]);

        throw new GMLException("Cannot parse DirectPosition");
    }

    protected void encodeDirectPostion(Coordinate c, StringBuffer buff, int srsDimension) {
        encodeStartTag("pos", buff, null);
        buff.append(c.x);
        if (srsDimension == 2)
            buff.append(" ").append(c.y);
        if (srsDimension == 3)
            buff.append(" ").append(c.z);
        encodeEndTag("pos", buff);
    }

    protected void encodeDirectPositionList(CoordinateSequence s, StringBuffer buff,
            int srsDimension) {
        encodeStartTag("posList", buff, null);
        for (int i = 0; i < s.size(); i++) {
            Coordinate c = s.getCoordinate(i);
            if (i > 0)
                buff.append(" ");
            buff.append(c.x);
            if (srsDimension == 2)
                buff.append(" ").append(c.y);
            if (srsDimension == 3)
                buff.append(" ").append(c.z);
        }
        encodeEndTag("posList", buff);
    }

    protected CoordinateSequence parseDirectPositionList(Node node, int srsDimension)
            throws GMLException {

        double[] values = parseDoubleList(node);

        // NamedNodeMap attrMap = node.getAttributes();
        // int srsDimension = 2;
        // Node attrNode = attrMap.getNamedItem("srsDimension");
        // if (attrNode != null) {
        // srsDimension = Integer.valueOf(attrNode.getTextContent());
        // }
        //
        // if (srsDimension <1 || srsDimension > 3) {
        // throw new GMLException("crsDimenstion must be one of 1 or 2 or 3");
        // }
        //        
        // int coordinatesCount;
        //        
        // attrNode = attrMap.getNamedItem("count");
        // if (attrNode != null) {
        // coordinatesCount = Integer.valueOf(attrNode.getTextContent());
        // } else {
        // coordinatesCount= values.length / srsDimension;
        // }

        int coordinatesCount = values.length / srsDimension;
        Coordinate[] array = new Coordinate[coordinatesCount];
        for (int i = 0; i < coordinatesCount; i++) {
            int offset = i * srsDimension;
            if (srsDimension == 1)
                array[i] = new Coordinate(values[offset], Double.NaN, Double.NaN);
            if (srsDimension == 2)
                array[i] = new Coordinate(values[offset], values[offset + 1], Double.NaN);
            if (srsDimension == 3)
                array[i] = new Coordinate(values[offset], values[offset + 1], values[offset + 2]);
        }
        return csFactory.create(array);
    }

    public Point parsePoint(Node node, int srsDimension) throws GMLException {

        Node coord = getChild(node, "pos");
        if (coord != null) {
            Coordinate c = parseDirectPosition(coord, srsDimension);
            return gf.createPoint(c);
        }

        // try the depricated GML2 stuff
        // try GML2 stuff
        Point p = parsePointGML2(node);
        if (p == null)
            throw new GMLException("Could not find a \"pos\", \"coord\" or \"coordinates\" element");
        return p;

    }

    public Polygon parsePolygon(Node node, int srsDimension) throws GMLException {

        Node outer = getChild(node, "exterior");
        Node lr = getChild(outer, "LinearRing");
        LinearRing extRing = parseLinearRing(lr, srsDimension);

        List<Node> innerList = getChildren(node, "interior");
        LinearRing[] innerRings = new LinearRing[innerList.size()];
        for (int i = 0; i < innerRings.length; i++) {
            lr = getChild(innerList.get(i), "LinearRing");
            innerRings[i] = parseLinearRing(lr, srsDimension);
        }
        return gf.createPolygon(extRing, innerRings);
    }

    protected void encodePoint(Point p, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap, int srsDimension) {
        encodeStartTag("Point", buff, abstractGeometryAttributeMap);
        encodeDirectPostion(p.getCoordinate(), buff, srsDimension);
        encodeEndTag("Point", buff);
    }

    public void encodeLineString(LineString ls, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap, int srsDimension) {
        encodeStartTag("LineString", buff, abstractGeometryAttributeMap);
        encodeDirectPositionList(ls.getCoordinateSequence(), buff, srsDimension);
        encodeEndTag("LineString", buff);
    }

    public void encodeLinearRing(LinearRing lr, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap, int srsDimension) {
        encodeStartTag("LinearRing", buff, abstractGeometryAttributeMap);
        encodeDirectPositionList(lr.getCoordinateSequence(), buff, srsDimension);
        encodeEndTag("LinearRing", buff);
    }

    public void encodePolygon(Polygon poly, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap, int srsDimension) {
        encodeStartTag("Polygon", buff, abstractGeometryAttributeMap);

        encodeStartTag("exterior", buff, null);
        encodeStartTag("LinearRing", buff, null);
        encodeDirectPositionList(poly.getExteriorRing().getCoordinateSequence(), buff, srsDimension);
        encodeEndTag("LinearRing", buff);
        encodeEndTag("exterior", buff);

        int countInner = poly.getNumInteriorRing();
        for (int i = 0; i < countInner; i++) {
            encodeStartTag("interior", buff, null);
            encodeStartTag("LinearRing", buff, null);
            encodeDirectPositionList(poly.getInteriorRingN(i).getCoordinateSequence(), buff,
                    srsDimension);
            encodeEndTag("LinearRing", buff);
            encodeEndTag("interior", buff);
        }
        encodeEndTag("Polygon", buff);
    }

    public Polygon parseEnvelope(Node node, int srsDimension) throws GMLException {

        List<Node> coordinates = getChildren(node, "pos");

        if (!coordinates.isEmpty() && (coordinates.size() == 2)) {
            Node n1 = (Node) coordinates.get(0);
            Node n2 = (Node) coordinates.get(1);
            Coordinate c1 = parseDirectPosition(n1, srsDimension);
            Coordinate c2 = parseDirectPosition(n2, srsDimension);

            return toGeometry(new Envelope(c1.x, c2.x, c1.y, c2.y));
        }

        if (!coordinates.isEmpty()) {
            throw new GMLException("Envelope can have only two coordinates");
        }

        Node lower = getChild(node, "lowerCorner");
        Node upper = getChild(node, "upperCorner");
        if (lower != null && upper != null) {
            Coordinate c1 = parseDirectPosition(lower, srsDimension);
            Coordinate c2 = parseDirectPosition(upper, srsDimension);
            return toGeometry(new Envelope(c1.x, c2.x, c1.y, c2.y));
        }

        throw new GMLException("Could not find direct positions for envelope");
    }

    public void encodeEnvelope(Polygon poly, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap, int srsDimension) {
        encodeStartTag("Envelope", buff, abstractGeometryAttributeMap);
        Envelope env = poly.getEnvelopeInternal();
        encodeDirectPostion(new Coordinate(env.getMinX(), env.getMinY()), buff, srsDimension);
        encodeDirectPostion(new Coordinate(env.getMaxX(), env.getMaxY()), buff, srsDimension);
        encodeEndTag("Envelope", buff);
    }

    public MultiPoint parseMultiPoint(Node node, int srsDimension) throws GMLException {

        Node pointMembers = getChild(node, "pointMembers");

        if (pointMembers != null) {
            List<Node> pointMemberList = getChildren(pointMembers, "Point");
            Point[] array = new Point[pointMemberList.size()];

            for (int i = 0; i < pointMemberList.size(); i++) {
                array[i] = parsePoint(pointMemberList.get(i), srsDimension);
            }
            return gf.createMultiPoint(array);
        }

        List<Node> pointMemberList = getChildren(node, "pointMember");
        Point[] array = new Point[pointMemberList.size()];

        for (int i = 0; i < pointMemberList.size(); i++) {
            Node pointNode = getChild(pointMemberList.get(i), "Point");
            array[i] = parsePoint(pointNode, srsDimension);
        }

        return gf.createMultiPoint(array);
    }

    public MultiLineString parseMultiCurve(Node node, int srsDimension) throws GMLException {
        Node curveMembers = getChild(node, "curveMembers");

        if (curveMembers != null) {
            List<Node> lsMemberList = getChildren(curveMembers, "LineString");
            LineString[] array = new LineString[lsMemberList.size()];

            for (int i = 0; i < lsMemberList.size(); i++) {
                array[i] = parseLineString(lsMemberList.get(i), srsDimension);
            }
            return gf.createMultiLineString(array);
        }

        List<Node> curveMemberList = getChildren(node, "curveMember");
        LineString[] array = new LineString[curveMemberList.size()];

        for (int i = 0; i < curveMemberList.size(); i++) {
            Node lsNode = getChild(curveMemberList.get(i), "LineString");
            array[i] = parseLineString(lsNode, srsDimension);
        }

        return gf.createMultiLineString(array);

    }

    public MultiPolygon parseMultiSurface(Node node, int srsDimension) throws GMLException {
        Node surfaceMembers = getChild(node, "surfaceMembers");

        if (surfaceMembers != null) {
            List<Node> polyMemberList = getChildren(surfaceMembers, "Polygon");
            Polygon[] array = new Polygon[polyMemberList.size()];

            for (int i = 0; i < polyMemberList.size(); i++) {
                array[i] = parsePolygon(polyMemberList.get(i), srsDimension);
            }
            return gf.createMultiPolygon(array);
        }

        List<Node> surfaceMemberList = getChildren(node, "surfaceMember");
        Polygon[] array = new Polygon[surfaceMemberList.size()];

        for (int i = 0; i < surfaceMemberList.size(); i++) {
            Node polygonNode = getChild(surfaceMemberList.get(i), "Polygon");
            array[i] = parsePolygon(polygonNode, srsDimension);
        }
        return gf.createMultiPolygon(array);

    }

    public void encodeMultiPoint(MultiPoint mp, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap, int srsDimension) {
        encodeStartTag("MultiPoint", buff, abstractGeometryAttributeMap);
        encodeStartTag("pointMembers", buff, null);
        for (int i = 0; i < mp.getNumGeometries(); i++) {
            Point p = (Point) mp.getGeometryN(i);
            encodePoint(p, buff, null, srsDimension);
        }
        encodeEndTag("pointMembers", buff);
        encodeEndTag("MultiPoint", buff);
    }

    public void encodeMultiCurve(MultiLineString mls, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap, int srsDimension) {
        encodeStartTag("MultiCurve", buff, abstractGeometryAttributeMap);
        encodeStartTag("curveMembers", buff, null);
        for (int i = 0; i < mls.getNumGeometries(); i++) {
            LineString ls = (LineString) mls.getGeometryN(i);
            encodeLineString(ls, buff, null, srsDimension);
        }
        encodeEndTag("curveMembers", buff);
        encodeEndTag("MultiCurve", buff);
    }

    public void encodeMultiSurface(MultiPolygon mp, StringBuffer buff,
            Map<String, String> abstractGeometryAttributeMap, int srsDimension) {
        encodeStartTag("MultiSurface", buff, abstractGeometryAttributeMap);
        encodeStartTag("surfaceMembers", buff, null);
        for (int i = 0; i < mp.getNumGeometries(); i++) {
            Polygon p = (Polygon) mp.getGeometryN(i);
            encodePolygon(p, buff, null, srsDimension);
        }
        encodeEndTag("surfaceMembers", buff);
        encodeEndTag("MultiSurface", buff);
    }

}
