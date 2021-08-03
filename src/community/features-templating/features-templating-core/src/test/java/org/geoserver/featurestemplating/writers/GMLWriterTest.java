package org.geoserver.featurestemplating.writers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class GMLWriterTest {

    @Test
    public void testPolygonGML2() throws XMLStreamException, ParseException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GMLTemplateWriter outputWriter = getGmlWriter(TemplateIdentifier.GML2, baos);
        String polygon = "POLYGON((145.5 -41.9, 145.5 -42.1, 145.6 -42, 145.5 -41.9))";
        Geometry geom = new WKTReader().read(polygon);
        outputWriter.writeGeometry(geom);
        outputWriter.close();
        String expected =
                "<gml:Polygon>"
                        + "<gml:outerBoundaryIs>"
                        + "<gml:LinearRing>"
                        + "<gml:coordinates>"
                        + "-41.9 145.5 -42.1 145.5 -42.0 145.6 -41.9 145.5"
                        + "</gml:coordinates>"
                        + "</gml:LinearRing>"
                        + "</gml:outerBoundaryIs>"
                        + "</gml:Polygon>";
        String encodedGeom = new String(baos.toByteArray());
        assertEquals(expected, encodedGeom);
    }

    @Test
    public void testPolygonGML3() throws XMLStreamException, ParseException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GMLTemplateWriter outputWriter = getGmlWriter(TemplateIdentifier.GML31, baos);
        String polygon = "POLYGON((145.5 -41.9, 145.5 -42.1, 145.6 -42, 145.5 -41.9))";
        Geometry geom = new WKTReader().read(polygon);
        outputWriter.writeGeometry(geom);
        outputWriter.close();
        String expected =
                "<gml:Surface>"
                        + "<gml:exterior>"
                        + "<gml:LinearRing>"
                        + "<gml:posList>"
                        + "-41.9 145.5 -42.1 145.5 -42.0 145.6 -41.9 145.5"
                        + "</gml:posList>"
                        + "</gml:LinearRing>"
                        + "</gml:exterior>"
                        + "</gml:Surface>";
        String encodedGeom = new String(baos.toByteArray());
        assertEquals(expected, encodedGeom);
    }

    @Test
    public void testPolygonGML32() throws XMLStreamException, ParseException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GMLTemplateWriter outputWriter = getGmlWriter(TemplateIdentifier.GML32, baos);
        String polygon = "POLYGON((145.5 -41.9, 145.5 -42.1, 145.6 -42, 145.5 -41.9))";
        Geometry geom = new WKTReader().read(polygon);
        outputWriter.writeGeometry(geom);
        outputWriter.close();
        String expected =
                "<gml:Surface>"
                        + "<gml:exterior>"
                        + "<gml:LinearRing>"
                        + "<gml:posList>"
                        + "-41.9 145.5 -42.1 145.5 -42.0 145.6 -41.9 145.5"
                        + "</gml:posList>"
                        + "</gml:LinearRing>"
                        + "</gml:exterior>"
                        + "</gml:Surface>";
        String encodedGeom = new String(baos.toByteArray());
        assertEquals(expected, encodedGeom);
    }

    @Test
    public void testNamespacesGML2() throws XMLStreamException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GMLTemplateWriter outputWriter = getGmlWriter(TemplateIdentifier.GML2, baos);
        outputWriter.startTemplateOutput(null);
        outputWriter.endTemplateOutput(null);
        outputWriter.close();
        String result = baos.toString();
        assertTrue(result.contains("xmlns:wfs=\"http://www.opengis.net/wfs\""));
        assertTrue(result.contains("xmlns:gml=\"http://www.opengis.net/gml\""));
    }

    @Test
    public void testNamespacesGML3() throws XMLStreamException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GMLTemplateWriter outputWriter = getGmlWriter(TemplateIdentifier.GML31, baos);
        outputWriter.startTemplateOutput(null);
        outputWriter.endTemplateOutput(null);
        outputWriter.close();
        String result = baos.toString();
        assertTrue(result.contains("xmlns:wfs=\"http://www.opengis.net/wfs\""));
        assertTrue(result.contains("xmlns:gml=\"http://www.opengis.net/gml\""));
    }

    @Test
    public void testNamespacesGML32() throws XMLStreamException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GMLTemplateWriter outputWriter = getGmlWriter(TemplateIdentifier.GML32, baos);
        outputWriter.startTemplateOutput(null);
        outputWriter.endTemplateOutput(null);
        outputWriter.close();
        String result = baos.toString();
        assertTrue(result.contains("xmlns:wfs=\"http://www.opengis.net/wfs/2.0\""));
        assertTrue(result.contains("xmlns:gml=\"http://www.opengis.net/gml/3.2\""));
    }

    private GMLTemplateWriter getGmlWriter(TemplateIdentifier identifier, OutputStream out)
            throws XMLStreamException {
        XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter xMLStreamWriter = xMLOutputFactory.createXMLStreamWriter(out);
        GMLTemplateWriter outputWriter =
                new GMLTemplateWriter(xMLStreamWriter, identifier.getOutputFormat());
        outputWriter.addNamespaces(new HashMap<>());
        return outputWriter;
    }
}
