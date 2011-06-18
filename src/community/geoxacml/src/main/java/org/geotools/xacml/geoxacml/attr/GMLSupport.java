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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequenceFactory;

/**
 * Abstract base class for supporting GML encoding and GML Parsing
 * 
 * 
 * @author Christian Mueller
 * 
 */
public abstract class GMLSupport {
    CoordinateSequenceFactory csFactory;

    GeometryFactory gf;

    public static final String GMLNS = "http://www.opengis.net/gml";

    public static final String GMLNS_PREFIX = "gml";

    protected static String LINESTRING_ERROR = "LineString must have >2 coodinates";

    protected static String LINEARRING_ERROR = "LinearRing must have >=4 coodinates";

    protected GMLSupport() {
        csFactory = CoordinateArraySequenceFactory.instance();
        gf = new GeometryFactory();

    }

    protected Node getChild(Node node, String elemName) {
        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (elemName.equals(n.getLocalName())) {
                String namespace = node.getNamespaceURI();
                if (namespace == null || (GMLSupport.GMLNS.equals(namespace))) {
                    return n;
                }
            }
        }
        return null;
    }

    protected List<Node> getChildren(Node node, String elemName) {
        List<Node> result = new ArrayList<Node>();

        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (elemName.equals(n.getLocalName())) {
                String namespace = node.getNamespaceURI();
                if (namespace == null || (GMLSupport.GMLNS.equals(namespace))) {
                    result.add(n);
                }
            }
        }
        return result;
    }

    protected Polygon toGeometry(Envelope e) {

        return gf.createPolygon(gf.createLinearRing(new Coordinate[] {
                new Coordinate(e.getMinX(), e.getMinY()), new Coordinate(e.getMaxX(), e.getMinY()),
                new Coordinate(e.getMaxX(), e.getMaxY()), new Coordinate(e.getMinX(), e.getMaxY()),
                new Coordinate(e.getMinX(), e.getMinY()) }), null);
    }

    protected void encodeStartTag(String elemName, StringBuffer buff,
            Map<String, String> attributeMap) {
        buff.append("<").append(GMLNS_PREFIX).append(":").append(elemName);
        if (attributeMap != null) {
            for (String name : attributeMap.keySet()) {
                buff.append(" ").append(name).append("=\"");
                buff.append(attributeMap.get(name));
                buff.append("\"");
            }
        }
        buff.append(">");
    }

    protected Map<String, String> abstractGeometryAttribueMap(GeometryAttribute attr) {
        Map<String, String> res = new HashMap<String, String>();
        if (attr.getSrsName() != null)
            res.put("srsName", attr.getSrsName());
        if (attr.getGid() != null)
            res.put("gid", attr.getGid());

        if (attr.getSrsDimension() > 0)
            res.put("srsDimension", new Integer(attr.getSrsDimension()).toString());
        return res;
    }

    protected void encodeEndTag(String elemName, StringBuffer buff) {
        buff.append("</").append(GMLNS_PREFIX).append(":").append(elemName).append(">");
    }

    abstract GeometryAttribute buildFromGML(Node node) throws Exception;

    abstract void encodeASGML(GeometryAttribute attr, StringBuffer buff);

    protected Coordinate parseCoordGML2(Node node) throws GMLException {
        int dimension = 1;
        double x;
        double y;
        double z;
        x = y = z = Double.NaN;

        Node xNode = getChild(node, "X");
        if (xNode == null)
            throw new GMLException("Missing X");

        x = Double.valueOf(xNode.getTextContent());

        Node yNode = getChild(node, "Y");
        if (yNode != null) {
            dimension++;
            y = Double.valueOf(yNode.getTextContent());
        }

        Node zNode = getChild(node, "Z");
        if (zNode != null) {
            dimension++;
            z = Double.valueOf(zNode.getTextContent());
        }

        return new Coordinate(x, y, z);
    }

    protected CoordinateSequence parseCoordinatesGML2(Node node) throws GMLException {
        // get the coordinate and tuple seperators
        String decimal = ".";
        String cs = ",";
        String ts = " ";

        NamedNodeMap attributeMap = node.getAttributes();

        Node attribute = attributeMap.getNamedItem("decimal");
        if (attribute != null) {
            decimal = attribute.getTextContent();
        }

        attribute = attributeMap.getNamedItem("cs");
        if (attribute != null) {
            cs = attribute.getTextContent();
        }

        attribute = attributeMap.getNamedItem("ts");
        if (attribute != null) {
            ts = attribute.getTextContent();
        }

        // do the parsing
        String text = node.getTextContent();

        // eliminate newlines, repeated spaces, etc
        final String anyBlankSeq = "\\s+";
        final String singleSpace = " ";
        text = text.replaceAll(anyBlankSeq, singleSpace).trim();

        // first tokenize by tuple seperators
        StringTokenizer tuples = new StringTokenizer(text, ts);
        CoordinateSequence seq = null;
        int i = 0;
        int ncoords = tuples.countTokens(); // number of coordinates

        while (tuples.hasMoreTokens()) {
            String tuple = tuples.nextToken();

            // next tokenize by coordinate seperator
            StringTokenizer oords = new StringTokenizer(tuple, cs);
            // String[] oords = tuple.split(cs);

            // next tokenize by decimal
            String x = null;

            // next tokenize by decimal
            String y = null;

            // next tokenize by decimal
            String z = null;

            // must be at least 1D
            String tmp = oords.nextToken();
            int count = 1;
            x = ".".equals(decimal) ? tmp : tmp.replaceAll(decimal, ".");

            if (oords.hasMoreTokens()) {
                tmp = oords.nextToken();
                count++;
                y = ".".equals(decimal) ? tmp : tmp.replaceAll(decimal, ".");
            }

            if (oords.hasMoreTokens()) {
                tmp = oords.nextToken();
                count++;
                z = ".".equals(decimal) ? tmp : tmp.replaceAll(decimal, ".");
            }

            if (seq == null) {
                seq = csFactory.create(ncoords, count);
            }

            seq.setOrdinate(i, CoordinateSequence.X, Double.parseDouble(x));

            if (y != null) {
                seq.setOrdinate(i, CoordinateSequence.Y, Double.parseDouble(y));
            }

            if (z != null) {
                seq.setOrdinate(i, CoordinateSequence.Z, Double.parseDouble(z));
            }

            i++;
        }

        return seq;
    }

    protected LinearRing parseLinearRingGML2(Node node) throws GMLException {

        List<Node> coordinates = getChildren(node, "coord");

        if (!coordinates.isEmpty() && (coordinates.size() >= 4)) {
            Coordinate[] coordArray = new Coordinate[coordinates.size()];
            for (int i = 0; i < coordinates.size(); i++) {
                coordArray[i] = parseCoordGML2(coordinates.get(i));
            }
            return gf.createLinearRing(coordArray);
        }

        if (!coordinates.isEmpty()) {
            throw new GMLException(LINEARRING_ERROR);
        }

        Node coords = getChild(node, "coordinates");
        if (coords != null) {
            CoordinateSequence cs = parseCoordinatesGML2(coords);

            if (cs.size() < 4) {
                throw new GMLException(LINEARRING_ERROR);
            }

            return gf.createLinearRing(cs);
        }

        return null;
    }

    protected LineString parseLineStringGML2(Node node) throws GMLException {

        List<Node> coordinates = getChildren(node, "coord");

        if (!coordinates.isEmpty() && (coordinates.size() >= 2)) {
            Coordinate[] coordArray = new Coordinate[coordinates.size()];
            for (int i = 0; i < coordinates.size(); i++) {
                coordArray[i] = parseCoordGML2(coordinates.get(i));
            }
            return gf.createLineString(coordArray);
        }

        if (!coordinates.isEmpty()) {
            throw new GMLException(LINESTRING_ERROR);
        }

        Node coords = getChild(node, "coordinates");
        if (coords != null) {
            CoordinateSequence cs = parseCoordinatesGML2(coords);

            if (cs.size() < 2) {
                throw new GMLException(LINESTRING_ERROR);
            }

            return gf.createLineString(cs);
        }

        return null;
    }

    protected Point parsePointGML2(Node node) throws GMLException {

        Node coord = getChild(node, "coord");

        if (coord != null) {
            Coordinate c = parseCoordGML2(coord);
            return gf.createPoint(c);
        }

        Node coordinates = getChild(node, "coordinates");
        if (coordinates != null) {
            CoordinateSequence seq = parseCoordinatesGML2(coordinates);
            return gf.createPoint(seq);
        }

        return null;
    }

}
