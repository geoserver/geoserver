/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Assume;
import org.junit.Test;
import org.w3c.dom.Document;

public class SVGTest extends WMSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addStyle("multifts", "./polyMultiFts.sld", getClass(), getCatalog());
    }

    @Test
    public void testBasicSvgGenerator() throws Exception {
        getWMS().setSvgRenderer(WMS.SVG_SIMPLE);
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + SVG.MIME_TYPE
                                + "&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&styles="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&featureid=BasicPolygons.1107531493643");

        assertEquals(1, doc.getElementsByTagName("svg").getLength());
        assertEquals(1, doc.getElementsByTagName("g").getLength());
    }

    @Test
    public void testBasicSvgGeneratorMultipleFts() throws Exception {
        getWMS().setSvgRenderer(WMS.SVG_SIMPLE);
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + SVG.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&styles=multifts"
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&featureid=BasicPolygons.1107531493643");

        assertEquals(1, doc.getElementsByTagName("svg").getLength());
        assertEquals(1, doc.getElementsByTagName("g").getLength());
    }

    @Test
    public void testBatikSvgGenerator() throws Exception {
        Assume.assumeTrue(isw3OrgReachable());

        getWMS().setSvgRenderer(WMS.SVG_BATIK);
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + SVG.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&styles="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&featureid=BasicPolygons.1107531493643");

        assertEquals(1, doc.getElementsByTagName("svg").getLength());
        assertTrue(doc.getElementsByTagName("g").getLength() > 1);
    }

    private boolean isw3OrgReachable() {
        // batik includes DTD reference which forces us to be online, skip test
        // in offline case
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) new URL("http://www.w3.org").openConnection();
            connection.setConnectTimeout(5000);
            connection.connect();
            connection.disconnect();
            return true;
        } catch (Exception e) {
            System.out.println("Unable to contact http://www.w3.org - " + e.getMessage());
            return false;
        }
    }

    @Test
    public void testBatikMultipleFts() throws Exception {
        Assume.assumeTrue(isw3OrgReachable());

        getWMS().setSvgRenderer(WMS.SVG_BATIK);
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + SVG.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&styles=multifts"
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&featureid=BasicPolygons.1107531493643");

        assertEquals(1, doc.getElementsByTagName("svg").getLength());
        assertTrue(doc.getElementsByTagName("g").getLength() > 1);
    }
}
