/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.Name;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

/**
 * Encoder class to encode SimpleFeatureCollection to GPX The encoder uses only a XMLStreamWriter for simplicity and
 * performance sake.
 */
public class GpxEncoder {
    boolean writeExtendedData = false;

    Map<String, Class<?>> trkAttributes = new HashMap<>();

    Map<String, Class<?>> wptAttributes = new HashMap<>();

    Map<String, Class<?>> rteAttributes = new HashMap<>();

    String creator = "GeoServer";

    String link = "http://www.geoserver.org";

    DecimalFormat format;

    public GpxEncoder(boolean writeExtendedData) {
        this.writeExtendedData = writeExtendedData;
        this.format = new DecimalFormat("#.######");
        this.format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
        trkAttributes.put("name", String.class);
        trkAttributes.put("desc", String.class);
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void encode(OutputStream lFileOutputStream, SimpleFeatureCollection collection)
            throws XMLStreamException, FactoryException {

        CRSAuthorityFactory crsFactory = CRS.getAuthorityFactory(true);

        CoordinateReferenceSystem targetCRS = crsFactory.createCoordinateReferenceSystem("EPSG:4326");
        collection = new ReprojectingFeatureCollection(collection, targetCRS);

        XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = xmlFactory.createXMLStreamWriter(lFileOutputStream);
        writer.writeStartDocument();
        writer.writeStartElement("gpx");
        writer.writeAttribute("xmlns", "http://www.topografix.com/GPX/1/1");
        if (link != null) {
            writer.writeAttribute("xmlns:att", link);
        }
        writer.writeAttribute("version", "1.1");

        if (creator != null) {
            writer.writeAttribute("creator", creator);
        }

        writer.writeStartElement("metadata");

        if (link != null && creator != null) {
            writer.writeStartElement("link");
            writer.writeAttribute("href", link);
            writer.writeStartElement("text");
            writer.writeCharacters(creator);
            writer.writeEndElement();
            writer.writeEndElement();
        }

        writer.writeStartElement("time");
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        writer.writeCharacters(sdf.format(calendar.getTime()));
        writer.writeEndElement();
        writer.writeEndElement(); // metadata

        try (FeatureIterator<SimpleFeature> iter = collection.features()) {
            while (iter.hasNext()) {
                SimpleFeature f = iter.next();

                Geometry g = (Geometry) f.getDefaultGeometry();
                if (g instanceof MultiLineString mls) {
                    int numGeometries = mls.getNumGeometries();
                    writer.writeStartElement("trk");
                    if (writeExtendedData) {
                        writeData(writer, f);
                    }
                    for (int i = 0; i < numGeometries; i++) {
                        LineString ls = (LineString) mls.getGeometryN(i);
                        writeTrkSeg(writer, ls);
                    }
                    writer.writeEndElement();
                } else if (g instanceof LineString string) {
                    writeRte(writer, string, f);
                } else if (g instanceof MultiPoint mpt) {
                    int numGeometries = mpt.getNumGeometries();
                    for (int i = 0; i < numGeometries; i++) {
                        Point pt = (Point) mpt.getGeometryN(i);
                        writeWpt(writer, pt, f);
                    }
                } else if (g instanceof Point point) {
                    writeWpt(writer, point, f);
                } else {
                    throw new IllegalArgumentException(
                            "Unsupported geometry type: " + g.getClass().getSimpleName());
                }
            }
        }

        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    private void writeCoordinates(XMLStreamWriter writer, String ptElementName, LineString ls)
            throws XMLStreamException {
        Coordinate[] coordinates = ls.getCoordinates();
        for (Coordinate coordinate : coordinates) {
            writeWpt(writer, ptElementName, coordinate.x, coordinate.y, coordinate.z);
        }
    }

    private void writeWpt(XMLStreamWriter writer, String ptElementName, double x, double y, double z)
            throws XMLStreamException {
        writer.writeStartElement(ptElementName);
        writer.writeAttribute("lat", format.format(y));
        writer.writeAttribute("lon", format.format(x));
        if (!Double.isNaN(z)) {
            writer.writeAttribute("ele", format.format(z));
        }
        writer.writeEndElement();
    }

    private void writeTrkSeg(XMLStreamWriter writer, LineString ls) throws XMLStreamException {
        writer.writeStartElement("trkseg");
        writeCoordinates(writer, "trkpt", ls);
        writer.writeEndElement();
    }

    private void writeRte(XMLStreamWriter writer, LineString ls, SimpleFeature f) throws XMLStreamException {
        writer.writeStartElement("rte");
        if (writeExtendedData) {
            writeData(writer, f);
        }
        writeCoordinates(writer, "rtept", ls);
        writer.writeEndElement();
    }

    private void writeWpt(XMLStreamWriter writer, Point pt, SimpleFeature f) throws XMLStreamException {
        writer.writeStartElement("wpt");
        Coordinate c = pt.getCoordinate();
        writer.writeAttribute("lon", format.format(c.x));
        writer.writeAttribute("lat", format.format(c.y));
        if (!Double.isNaN(c.z)) {
            writer.writeAttribute("ele", format.format(c.z));
        }
        if (writeExtendedData) {
            writeData(writer, f);
        }
        writer.writeEndElement();
    }

    private void writeData(XMLStreamWriter writer, SimpleFeature f) throws XMLStreamException {
        writer.writeStartElement("extensions");
        for (Property p : f.getProperties()) {
            Name name = p.getName();
            if (!(p.getValue() instanceof Geometry) && p.getValue() != null) {
                writer.writeStartElement("att:" + name.getLocalPart());
                writer.writeCharacters(p.getValue().toString());
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }
}
