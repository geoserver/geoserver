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
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
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

/**
 * Helper class that allow the GMLTemplateWriter to write the output according to the gml version
 * requested.
 */
abstract class GMLDialectManager {

    protected XMLStreamWriter streamWriter;

    private CRS.AxisOrder axisOrder = CRS.AxisOrder.EAST_NORTH;

    protected CoordinateReferenceSystem crs;

    private String coordElementName;

    protected String typeName;

    protected Map<String, String> namespaces = new HashMap<>();

    static final String GML_PREFIX = "gml";
    static final String WFS_PREFIX = "wfs";

    long currentFeatureNumber = 1L;

    GMLDialectManager(XMLStreamWriter streamWriter, String coorElementName) {
        this.streamWriter = streamWriter;
        this.coordElementName = coorElementName;
        this.namespaces.put(GML_PREFIX, getGmlNsUri());
        this.namespaces.put(WFS_PREFIX, getWfsNsUri());
    }

    void writeGeometry(Geometry geometry) throws XMLStreamException {
        String gmlNsUri = namespaces.get(GML_PREFIX);
        if (geometry instanceof Point) {
            writePoint((Point) geometry, gmlNsUri);
        } else if (geometry instanceof MultiPoint) {
            writeMultiPoint((MultiPoint) geometry, gmlNsUri);
        } else if (geometry instanceof LineString) {
            writeLineString((LineString) geometry, gmlNsUri);
        } else if (geometry instanceof MultiLineString) {
            writeMultiLineString((MultiLineString) geometry, gmlNsUri);
        } else if (geometry instanceof Polygon) {
            writePolygon((Polygon) geometry, gmlNsUri);
        } else if (geometry instanceof MultiPolygon) {
            writeMultiPolygon((MultiPolygon) geometry, gmlNsUri);
        }
    }

    void writePoint(Point point, String gmlNsUri) throws XMLStreamException {
        writePoint(point, gmlNsUri, 0);
    }

    void writePoint(Point point, String gmlNsUri, int index) throws XMLStreamException {
        streamWriter.writeStartElement(GML_PREFIX, "Point", gmlNsUri);
        writeGeometryAttributes(index);
        streamWriter.writeStartElement(
                GML_PREFIX,
                coordElementName.equals("coordinates") ? coordElementName : "pos",
                gmlNsUri);
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

    void writeMultiPoint(MultiPoint multiPoint, String gmlNsUri) throws XMLStreamException {
        int nPoints = multiPoint.getNumPoints();
        streamWriter.writeStartElement(GML_PREFIX, "MultiPoint", gmlNsUri);
        for (int i = 0; i < nPoints; i++) {
            streamWriter.writeStartElement(GML_PREFIX, "pointMember", gmlNsUri);
            writeGeometryAttributes(0);
            writePoint((Point) multiPoint.getGeometryN(i), gmlNsUri, i);
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    void writePolygon(Polygon polygon, String gmlNsUri) throws XMLStreamException {
        writePolygon(polygon, gmlNsUri, 0);
    }

    void writePolygon(Polygon polygon, String gmlNsUri, int index) throws XMLStreamException {
        LinearRing exteriorRing = polygon.getExteriorRing();
        Coordinate[] coordinates = exteriorRing.getCoordinates();
        streamWriter.writeStartElement(GML_PREFIX, "Surface", gmlNsUri);
        writeGeometryAttributes(index);
        writePolygonRing("exterior", coordinates, gmlNsUri);
        int numInterior = polygon.getNumInteriorRing();
        for (int i = 0; i < numInterior; i++) {
            coordinates = polygon.getInteriorRingN(i).getCoordinates();
            writePolygonRing("interior", coordinates, gmlNsUri);
        }
        streamWriter.writeEndElement();
    }

    void writePolygonRing(String ringName, Coordinate[] coordinates, String gmlNsUri)
            throws XMLStreamException {
        streamWriter.writeStartElement(GML_PREFIX, ringName, gmlNsUri);
        streamWriter.writeStartElement(GML_PREFIX, "LinearRing", gmlNsUri);
        streamWriter.writeStartElement(GML_PREFIX, coordElementName, gmlNsUri);
        writeCoordinates(coordinates);
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    void writeLineString(LineString lineString, String gmlNsUri) throws XMLStreamException {
        writeLineString(lineString, gmlNsUri, 0);
    }

    void writeLineString(LineString lineString, String gmlNsUri, int index)
            throws XMLStreamException {
        Coordinate[] coordinates = lineString.getCoordinates();
        streamWriter.writeStartElement(GML_PREFIX, "LineString", gmlNsUri);
        writeGeometryAttributes(index);
        streamWriter.writeStartElement(GML_PREFIX, coordElementName, gmlNsUri);
        writeCoordinates(coordinates);
        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    void writeMultiLineString(MultiLineString lineString, String gmlNsUri)
            throws XMLStreamException {
        int numGeom = lineString.getNumGeometries();
        streamWriter.writeStartElement(GML_PREFIX, "MultiLineString", gmlNsUri);
        writeGeometryAttributes(0);
        for (int i = 0; i < numGeom; i++) {
            streamWriter.writeStartElement(GML_PREFIX, "LineStringMember", gmlNsUri);
            writeLineString((LineString) lineString.getGeometryN(i), gmlNsUri);
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    void writeMultiPolygon(MultiPolygon multiPolygon, String gmlNsUri) throws XMLStreamException {
        int numGeom = multiPolygon.getNumGeometries();
        boolean isMultiSurface = multiPolygon instanceof MultiSurface;
        streamWriter.writeStartElement(
                GML_PREFIX, isMultiSurface ? "MultiSurface" : "MultiPolygon", gmlNsUri);
        writeGeometryAttributes(0);
        for (int i = 0; i < numGeom; i++) {
            streamWriter.writeStartElement(
                    GML_PREFIX, isMultiSurface ? "surfaceMember" : "polygonMember", gmlNsUri);
            writePolygon((Polygon) multiPolygon.getGeometryN(i), gmlNsUri);
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

    void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    void setAxisOrder(CRS.AxisOrder axisOrder) {
        this.axisOrder = axisOrder;
    }

    abstract String getWfsNsUri();

    abstract String getGmlNsUri();

    Map<String, String> getNamespaces() {
        return this.namespaces;
    }

    abstract void startFeatureMember() throws XMLStreamException;

    void endFeatureMember() throws XMLStreamException {
        streamWriter.writeEndElement();
        currentFeatureNumber++;
    }

    void writeGeometryAttributes(int geometryIndex) throws XMLStreamException {
        if (crs != null) streamWriter.writeAttribute("srsName", CRS.toSRS(this.crs));
    }

    void setTypeName(String typeName) {
        this.typeName = typeName;
        this.currentFeatureNumber = 1L;
    }
}
