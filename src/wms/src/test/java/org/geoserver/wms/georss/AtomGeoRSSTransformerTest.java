/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.georss;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.Query;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
        Document document =
                getAsDOM(
                        "wms/reflect?format_options=encoding:latlong&format=application/atom+xml&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
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
        Document document =
                getAsDOM(
                        "wms/reflect?format_options=encoding:simple&format=application/atom+xml&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
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

    @org.junit.Test
    public void testGmlWMS() throws Exception {
        Document document =
                getAsDOM(
                        "wms/reflect?format_options=encoding:gml&format=application/atom+xml&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
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
