/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DescribeLayerTest extends WMSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
    }

    @Test
    public void testDescribeLayerVersion111() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request = "wms?service=wms&version=1.1.1&request=DescribeLayer&layers=" + layer;
        assertEquals(
                "src/test/resources/geoserver",
                getGeoServer().getGlobal().getSettings().getProxyBaseUrl());
        Document dom = getAsDOM(request, true);

        assertEquals(
                "1.1.1",
                dom.getDocumentElement().getAttributes().getNamedItem("version").getNodeValue());
    }

    //    @Test
    //    public void testDescribeLayerVersion110() throws Exception {
    //        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
    //        String request = "wms?service=wms&version=1.1.0&request=DescribeLayer&layers=" +
    // layer;
    //        Document dom = getAsDOM(request);
    //        assertEquals("1.1.0",
    // dom.getDocumentElement().getAttributes().getNamedItem("version").getNodeValue());
    //    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        Document dom =
                getAsDOM(
                        "cite/wms?service=wms&version=1.1.1&request=DescribeLayer"
                                + "&layers=PrimitiveGeoFeature",
                        true);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());

        dom =
                getAsDOM(
                        "sf/wms?service=wms&version=1.1.1&request=DescribeLayer"
                                + "&layers=PrimitiveGeoFeature",
                        true);
        // print(dom);
        assertEquals("WMS_DescribeLayerResponse", dom.getDocumentElement().getNodeName());

        Element e = (Element) dom.getElementsByTagName("LayerDescription").item(0);
        String attribute = e.getAttribute("owsURL");
        assertTrue(attribute.contains("sf/wfs"));

        attribute = e.getAttribute("owsType");
        assertEquals("WFS", attribute);

        e = (Element) dom.getElementsByTagName("Query").item(0);
        String typeName = e.getAttribute("typeName");
        assertEquals("unexpected fully qualified typename", "sf:PrimitiveGeoFeature", typeName);
    }

    @Test
    public void testWorkspaceNonQualifiedUrl() throws Exception {
        Document dom =
                getAsDOM(
                        "wms?service=wms&version=1.1.1&request=DescribeLayer"
                                + "&layers=PrimitiveGeoFeature",
                        true);
        // print(dom);
        assertEquals("WMS_DescribeLayerResponse", dom.getDocumentElement().getNodeName());

        Element e = (Element) dom.getElementsByTagName("LayerDescription").item(0);
        String attribute = e.getAttribute("owsURL");
        assertTrue(attribute.contains("wfs"));

        attribute = e.getAttribute("owsType");
        assertEquals("WFS", attribute);

        e = (Element) dom.getElementsByTagName("Query").item(0);
        String typeName = e.getAttribute("typeName");
        assertEquals("unexpected fully qualified typename", "sf:PrimitiveGeoFeature", typeName);
    }

    @Test
    public void testImageWMSLayer() throws Exception {
        Document dom =
                getAsDOM(
                        "wms?service=wms&version=1.1.1&request=DescribeLayer&layers=wcs:World",
                        true);
        // print(dom);
        assertEquals("WMS_DescribeLayerResponse", dom.getDocumentElement().getNodeName());

        Element e = (Element) dom.getElementsByTagName("LayerDescription").item(0);
        String attribute = e.getAttribute("owsURL");
        assertTrue(attribute.contains("wcs"));
        attribute = e.getAttribute("owsType");
        assertEquals("WCS", attribute);

        e = (Element) dom.getElementsByTagName("Query").item(0);
        String typeName = e.getAttribute("typeName");
        assertEquals("unexpected typename", "wcs:World", typeName);
    }

    @Test
    public void testImageWMSLayerWorkspaceQualified() throws Exception {
        Document dom =
                getAsDOM(
                        "wcs/wms?service=wms&version=1.1.1&request=DescribeLayer&layers=World",
                        true);
        // print(dom);
        assertEquals("WMS_DescribeLayerResponse", dom.getDocumentElement().getNodeName());

        Element e = (Element) dom.getElementsByTagName("LayerDescription").item(0);
        String attribute = e.getAttribute("owsURL");
        assertTrue(attribute.contains("wcs"));
        attribute = e.getAttribute("owsType");
        assertEquals("WCS", attribute);

        e = (Element) dom.getElementsByTagName("Query").item(0);
        String typeName = e.getAttribute("typeName");
        // NOTE / TODO
        //  we would have expected a simple name instead, but the WCS capabilities documents, even
        // when workspace qualified,
        //  are using fully qualified names (weird and incorrect I believe, should eventually be
        // fixed).
        //  see: https://github.com/geoserver/geoserver/pull/6015#discussion_r912808282
        assertEquals("unexpected typename", "wcs:World", typeName);
    }
}
