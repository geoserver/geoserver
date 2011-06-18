/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.georss;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.Query;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class AtomGeoRSSTransformerTest extends WMSTestSupport {
    static WMSMapContext map;
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new AtomGeoRSSTransformerTest());
    }

    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        map = new WMSMapContext(createGetMapRequest(MockData.BASIC_POLYGONS));
        map.addLayer(createMapLayer(MockData.BASIC_POLYGONS));
    }

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

    public void testLatLongWMS() throws Exception {
        Document document = getAsDOM(
                "wms/reflect?format_options=encoding:latlong&format=application/atom+xml&layers=" 
                + MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart()
                );

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

    public void testSimpleWMS() throws Exception {
        Document document = getAsDOM(
                "wms/reflect?format_options=encoding:simple&format=application/atom+xml&layers=" 
                + MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart()
                );

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
    
    public void testGmlWMS() throws Exception {
        Document document = getAsDOM(
                "wms/reflect?format_options=encoding:gml&format=application/atom+xml&layers=" 
                + MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart()
                );

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
