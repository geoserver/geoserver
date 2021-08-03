/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.writers;

import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

class GML2DialectManager extends GMLDialectManager {

    GML2DialectManager(XMLStreamWriter streamWriter) {
        super(streamWriter, "coordinates");
    }

    @Override
    void writeNumberReturned(String numberReturned) throws XMLStreamException {
        streamWriter.writeAttribute("numberOfFeature", numberReturned);
    }

    @Override
    void writeNumberMatched(String numberMatched) throws XMLStreamException {}

    @Override
    void writeBoundingBox(ReferencedEnvelope envelope, CoordinateReferenceSystem crs)
            throws IOException {
        super.writeBoundingBox(envelope, crs, false);
    }

    @Override
    void writeMultiPolygon(MultiPolygon multiPolygon, String gmlNsUri) throws XMLStreamException {
        int numGeom = multiPolygon.getNumGeometries();
        streamWriter.writeStartElement(GML_PREFIX, "MultiPolygon", gmlNsUri);
        for (int i = 0; i < numGeom; i++) {
            streamWriter.writeStartElement(GML_PREFIX, "polygonMember", gmlNsUri);
            writePolygon((Polygon) multiPolygon.getGeometryN(i), gmlNsUri);
            streamWriter.writeEndElement();
        }
        streamWriter.writeEndElement();
    }

    @Override
    void writePolygon(Polygon polygon, String gmlNsUri) throws XMLStreamException {
        LinearRing exteriorRing = polygon.getExteriorRing();
        Coordinate[] coordinates = exteriorRing.getCoordinates();
        streamWriter.writeStartElement(GML_PREFIX, "Polygon", gmlNsUri);
        writePolygonRing("outerBoundaryIs", coordinates, gmlNsUri);
        int numInterior = polygon.getNumInteriorRing();
        for (int i = 0; i < numInterior; i++) {
            coordinates = polygon.getInteriorRingN(i).getCoordinates();
            writePolygonRing("innerBoundaryIs", coordinates, gmlNsUri);
        }
        streamWriter.writeEndElement();
    }

    @Override
    String getWfsNsUri() {
        return "http://www.opengis.net/wfs";
    }

    @Override
    String getGmlNsUri() {
        return "http://www.opengis.net/gml";
    }

    @Override
    void startFeatureMember() throws XMLStreamException {
        streamWriter.writeStartElement(GML_PREFIX, "featureMember", getGmlNsUri());
    }
}
