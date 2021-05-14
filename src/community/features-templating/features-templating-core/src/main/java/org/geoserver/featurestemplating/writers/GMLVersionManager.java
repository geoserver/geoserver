/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.writers;

import static org.geoserver.featurestemplating.writers.TemplateOutputWriter.getCRSIdentifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml3.MultiSurface;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Helper class that allow the GMLTemplateWriter to write the output according to the gml version
 * requested.
 */
abstract class GMLVersionManager {

    protected XMLStreamWriter streamWriter;

    private CRS.AxisOrder axisOrder = CRS.AxisOrder.EAST_NORTH;

    private String coordElementName;

    private String exteriorElementName;

    private String interiorElementName;

    protected Map<String, String> namespaces = new HashMap<>();

    static final String GML_PREFIX = "gml";
    static final String WFS_PREFIX = "wfs";

    GMLVersionManager(
            XMLStreamWriter streamWriter,
            String coorElementName,
            String exteriorElementName,
            String interiorElementName) {
        this.streamWriter = streamWriter;
        this.coordElementName = coorElementName;
        this.exteriorElementName = exteriorElementName;
        this.interiorElementName = interiorElementName;
    }

    void writeGeometry(Geometry geometry) throws XMLStreamException {
        if (geometry instanceof Point) {
            writePoint((Point) geometry);
        } else if (geometry instanceof MultiPoint) {
            writeMultiPoint((MultiPoint) geometry);
        } else if (geometry instanceof LineString) {
            writeLineString((LineString) geometry);
        } else if (geometry instanceof MultiLineString) {
            writeMultiLineString((MultiLineString) geometry);
        } else if (geometry instanceof Polygon) {
            writePolygon((Polygon) geometry);
        } else if (geometry instanceof MultiPolygon) {
            writeMultiPolygon((MultiPolygon) geometry);
        }
    }

    private void writePoint(Point point) throws XMLStreamException {
        streamWriter.writeStartElement("gml", "Point", "http://www.opengis.net/gml/3.2");
        streamWriter.writeStartElement(
                "gml",
                coordElementName.equals("coordinates") ? coordElementName : "pos",
                "http://www.opengis.net/gml/3.2");
        double y = point.getY();
        double x = point.getX();
        if (axisOrder == CRS.AxisOrder.NORTH_EAST) {
            streamWriter.writeCharacters(y + " " + x);
        } else {
            streamWriter.writeCharacters(x + " " + y);
        }
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    private void writeMultiPoint(MultiPoint multiPoint) throws XMLStreamException {
        int nPoints = multiPoint.getNumPoints();
        streamWriter.writeStartElement("gml", "MultiPoint", "http://www.opengis.net/gml/3.2");
        for (int i = 0; i < nPoints; i++) {
            streamWriter.writeStartElement("gml", "pointMember", "http://www.opengis.net/gml/3.2");
            writePoint((Point) multiPoint.getGeometryN(i));
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    private void writePolygon(Polygon polygon) throws XMLStreamException {
        LinearRing exteriorRing = polygon.getExteriorRing();
        Coordinate[] coordinates = exteriorRing.getCoordinates();
        streamWriter.writeStartElement("gml", "Polygon", "http://www.opengis.net/gml/3.2");
        writePolygonRing(exteriorElementName, coordinates);
        int numInterior = polygon.getNumInteriorRing();
        for (int i = 0; i < numInterior; i++) {
            coordinates = polygon.getInteriorRingN(i).getCoordinates();
            writePolygonRing(interiorElementName, coordinates);
        }
        streamWriter.writeEndElement();
    }

    private void writePolygonRing(String ringName, Coordinate[] coordinates)
            throws XMLStreamException {
        streamWriter.writeStartElement("gml", ringName, "http://www.opengis.net/gml/3.2");
        streamWriter.writeStartElement("gml", "LinearRing", "http://www.opengis.net/gml/3.2");
        streamWriter.writeStartElement("gml", coordElementName, "http://www.opengis.net/gml/3.2");
        writeCoordinates(coordinates);
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    private void writeLineString(LineString lineString) throws XMLStreamException {
        Coordinate[] coordinates = lineString.getCoordinates();
        streamWriter.writeStartElement("gml", "LineString", "http://www.opengis.net/gml/3.2");
        streamWriter.writeStartElement("gml", coordElementName, "http://www.opengis.net/gml/3.2");
        writeCoordinates(coordinates);
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    private void writeMultiLineString(MultiLineString lineString) throws XMLStreamException {
        int numGeom = lineString.getNumGeometries();
        streamWriter.writeStartElement("gml", "MultiLineString", "http://www.opengis.net/gml/3.2");
        for (int i = 0; i < numGeom; i++) {
            streamWriter.writeStartElement(
                    "gml", "LineStringMember", "http://www.opengis.net/gml/3.2");
            writeLineString((LineString) lineString.getGeometryN(i));
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    private void writeMultiPolygon(MultiPolygon multiPolygon) throws XMLStreamException {
        int numGeom = multiPolygon.getNumGeometries();
        boolean isMultiSurface = multiPolygon instanceof MultiSurface;
        streamWriter.writeStartElement(
                "gml",
                isMultiSurface ? "MultiSurface" : "MultiPolygon",
                "http://www.opengis.net/gml/3.2");
        for (int i = 0; i < numGeom; i++) {
            streamWriter.writeStartElement(
                    "gml",
                    isMultiSurface ? "surfaceMember" : "polygonMember",
                    "http://www.opengis.net/gml/3.2");
            writePolygon((Polygon) multiPolygon.getGeometryN(i));
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    private void writeCoordinates(Coordinate[] coordinates) throws XMLStreamException {
        for (int i = 0; i < coordinates.length; i++) {
            Coordinate coor = coordinates[i];
            double y = coor.getX();
            double x = coor.getY();
            String textString;
            if (axisOrder == CRS.AxisOrder.NORTH_EAST) {
                textString = y + " " + x;
            } else {
                textString = x + " " + y;
            }
            if (i != coordinates.length - 1) textString += " ";
            streamWriter.writeCharacters(textString);
        }
    }

    abstract void writeNumberReturned(String numberReturned) throws XMLStreamException;

    abstract void writeNumberMatched(String numberMatched) throws XMLStreamException;

    void writeBoundingBox(
            ReferencedEnvelope envelope, CoordinateReferenceSystem crs, boolean useWFSPrefix)
            throws IOException {
        try {
            streamWriter.writeStartElement(
                    useWFSPrefix ? WFS_PREFIX : GML_PREFIX,
                    "boundedBy",
                    namespaces.get(GML_PREFIX));
            streamWriter.writeStartElement(GML_PREFIX, "Envelope", namespaces.get(GML_PREFIX));
            writeCrs(crs);
            streamWriter.writeStartElement(GML_PREFIX, "lowerCorner", namespaces.get(GML_PREFIX));
            streamWriter.writeCharacters(envelope.getMinY() + " " + envelope.getMinX());
            streamWriter.writeEndElement();
            streamWriter.writeStartElement(GML_PREFIX, "upperCorner", namespaces.get(GML_PREFIX));
            streamWriter.writeCharacters(envelope.getMaxX() + " " + envelope.getMaxX());
            streamWriter.writeEndElement();
            streamWriter.writeEndElement();
            streamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    abstract void writeBoundingBox(ReferencedEnvelope envelope, CoordinateReferenceSystem crs)
            throws IOException;

    void writeCrs(CoordinateReferenceSystem crs) throws XMLStreamException, IOException {
        if (crs != null) {
            streamWriter.writeAttribute(
                    "srsDimension", String.valueOf(crs.getCoordinateSystem().getDimension()));
            String crsIdentifier = getCRSIdentifier(crs);
            streamWriter.writeAttribute("srsName", crsIdentifier);
        }
    }

    void addNamespaces(Map<String, String> namespaces) {
        this.namespaces.putAll(namespaces);
    }

    void setAxisOrder(CRS.AxisOrder axisOrder) {
        this.axisOrder = axisOrder;
    }
}
