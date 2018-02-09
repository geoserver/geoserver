/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.georss;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.Query;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AtomGeoRSSTransformerTest extends WMSTestSupport {
    static WMSMapContent map;

    @Before
    public void initializeMap() throws Exception {

        map = new WMSMapContent(createGetMapRequest(MockData.BASIC_POLYGONS));
        map.addLayer(createMapLayer(MockData.BASIC_POLYGONS));
    }

    @org.junit.Test
    public void testLatLongInternal() throws Exception {
        AtomGeoRSSTransformer tx = new AtomGeoRSSTransformer(getWMS());
        tx.setGeometryEncoding(AtomGeoRSSTransformer.GeometryEncoding.LATLONG);
        tx.setIndentation(2);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(map, output);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        Element element = document.getDocumentElement();
        assertEquals("feed", element.getNodeName());

        NodeList entries = element.getElementsByTagName("entry");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("geo:lat").getLength());
            assertEquals(1, entry.getElementsByTagName("geo:long").getLength());
        }
    }

    @org.junit.Test
    public void testLatLongWMS() throws Exception {
        Document document = getAsDOM(
                "wms/reflect?format_options=encoding:latlong&format=application/atom+xml&layers="
                        + MockData.BASIC_POLYGONS.getPrefix() + ":"
                        + MockData.BASIC_POLYGONS.getLocalPart());

        Element element = document.getDocumentElement();
        assertEquals("feed", element.getNodeName());

        NodeList entries = element.getElementsByTagName("entry");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("geo:lat").getLength());
            assertEquals(1, entry.getElementsByTagName("geo:long").getLength());
        }
    }

    @org.junit.Test
    public void testSimpleInternal() throws Exception {
        AtomGeoRSSTransformer tx = new AtomGeoRSSTransformer(getWMS());
        tx.setGeometryEncoding(AtomGeoRSSTransformer.GeometryEncoding.SIMPLE);
        tx.setIndentation(2);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        tx.transform(map, output);

        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        Element element = document.getDocumentElement();
        assertEquals("feed", element.getNodeName());

        NodeList entries = element.getElementsByTagName("entry");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("georss:where").getLength());
            assertEquals(1, entry.getElementsByTagName("georss:polygon").getLength());
        }
    }

    @org.junit.Test
    public void testSimpleWMS() throws Exception {
        Document document = getAsDOM(
                "wms/reflect?format_options=encoding:simple&format=application/atom+xml&layers="
                        + MockData.BASIC_POLYGONS.getPrefix() + ":"
                        + MockData.BASIC_POLYGONS.getLocalPart());

        Element element = document.getDocumentElement();
        assertEquals("feed", element.getNodeName());

        NodeList entries = element.getElementsByTagName("entry");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("georss:where").getLength());
            assertEquals(1, entry.getElementsByTagName("georss:polygon").getLength());
        }
    }

    /**
     * Check for errors in concurrent output from WMS, such as in templated fields. This is a
     * best-effort test that will usually, but not always, fail in the presence of bugs.
     * 
     * @throws Exception
     */
    @Test
    public void testConcurrentWMS() throws Exception {
        Callable<Document> getter = () -> getAsDOM(
                "wms/reflect?format_options=encoding:simple&format=application/atom+xml&layers="
                        + MockData.BASIC_POLYGONS.getPrefix() + ":"
                        + MockData.BASIC_POLYGONS.getLocalPart());
        StringWriter writer = new StringWriter();
        Transformer t = TransformerFactory.newInstance().newTransformer();

        // Filter timestamps to prevent time-based errors in test
        Document document = filterTimestamps(getter.call());
        t.transform(new DOMSource(document), new StreamResult(writer));
        String expected = writer.toString();

        int calls = 100;
        CompletionService<Document> cs = new ExecutorCompletionService<>(
                Executors.newFixedThreadPool(calls));
        for (int i = 0; i < calls; i++) {
            cs.submit(getter);
        }

        for (int i = 0; i < calls; i++) {
            writer = new StringWriter();
            t.transform(new DOMSource(filterTimestamps(cs.take().get())),
                    new StreamResult(writer));

            assertEquals(expected, writer.toString());
        }
    }

    @org.junit.Test
    public void testGmlWMS() throws Exception {
        Document document = getAsDOM(
                "wms/reflect?format_options=encoding:gml&format=application/atom+xml&layers="
                        + MockData.BASIC_POLYGONS.getPrefix() + ":"
                        + MockData.BASIC_POLYGONS.getLocalPart());

        Element element = document.getDocumentElement();
        assertEquals("feed", element.getNodeName());

        NodeList entries = element.getElementsByTagName("entry");

        int n = getFeatureSource(MockData.BASIC_POLYGONS).getCount(Query.ALL);

        assertEquals(n, entries.getLength());

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            assertEquals(1, entry.getElementsByTagName("georss:where").getLength());
            assertEquals(1, entry.getElementsByTagName("gml:Polygon").getLength());
        }
    }
}
