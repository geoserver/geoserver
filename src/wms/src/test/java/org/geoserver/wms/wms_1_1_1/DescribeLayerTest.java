/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.*;

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
    }
}
