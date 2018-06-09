/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Map;
import javax.xml.namespace.QName;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.wms_1_1_1.GetMapIntegrationTest;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class UTFGridIntegrationTest extends WMSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpWcs11RasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        testData.addStyle("thin_line", "thin_line.sld", UTFGridIntegrationTest.class, catalog);
        testData.addStyle("dotted", "dotted.sld", UTFGridIntegrationTest.class, catalog);
        testData.addStyle("circle", "circle.sld", UTFGridIntegrationTest.class, catalog);
        testData.addStyle(
                "polygonExtract", "polygonExtract.sld", UTFGridIntegrationTest.class, catalog);
        testData.addStyle(
                "decoratedCircle", "decoratedCircle.sld", UTFGridIntegrationTest.class, catalog);
        testData.addStyle("population", "Population.sld", GetMapIntegrationTest.class, catalog);
        testData.addVectorLayer(
                new QName(MockData.SF_URI, "states", MockData.SF_PREFIX),
                Collections.EMPTY_MAP,
                "states.properties",
                GetMapIntegrationTest.class,
                catalog);
    }

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
    }

    /** The UTF grid format shows up in the caps document. The format name is freeform */
    @Test
    public void testCapabilities11() throws Exception {
        Document dom = getAsDOM("wms?service=WMS&request=GetCapabilities&version=1.1.0");
        // print(dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals("1", xpath.evaluate("count(//GetMap[Format='utfgrid'])", dom));
    }

    /**
     * The UTF grid format shows up in the caps document. WMS 1.3 requires the usage of mime types
     * that will match the result content type
     */
    @Test
    public void testCapabilities13() throws Exception {
        Document dom = getAsDOM("wms?service=WMS&request=GetCapabilities&version=1.3.0");
        // print(dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//wms:GetMap[wms:Format='application/json;type=utfgrid'])", dom));
    }

    @Test
    public void testEmptyOutput() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:Forests"
                                + "&styles=&bbox=-10.0028,-0.0028,-9.0048,0.0048&width=256&height=256&srs=EPSG:4326&format=utfgrid");
        assertEquals(1, tester.getKeyCount());
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                tester.assertGridPixel(' ', i, j);
            }
        }
    }

    @Test
    public void testPolygonGraphicFill() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:Forests"
                                + "&styles=&bbox=-0.0028,-0.0028,0.0048,0.0048&width=256&height=256&srs=EPSG:4326&format=utfgrid");
        // sample some pixels
        tester.assertGridPixel(' ', 10, 20);
        tester.assertGridPixel('!', 60, 20);
        JSONObject f = tester.getFeature('!');
        assertEquals("Green Forest", f.getString("NAME"));
    }

    private UTFGridTester getAsGridTester(String request, int width, int height, int resolution)
            throws Exception {
        MockHttpServletResponse response = getAsServletResponse(request);
        if (!response.getContentType().startsWith("application/json")) {
            System.out.println(response.getContentAsString());
            fail("Expected json but got " + response.getContentType());
        }
        JSON json = json(response);
        // print(json);
        UTFGridTester tester = new UTFGridTester(json, width, height, resolution);
        return tester;
    }

    private UTFGridTester getAsGridTester(String request) throws Exception {
        return getAsGridTester(request, 256, 256, 4);
    }

    @Test
    public void testPolygonReproject() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:Forests"
                                + "&styles=polygon&bbox=-280,-280,480,480&width=256&height=256&srs=EPSG:3857&format=utfgrid");
        // sample some pixels
        tester.assertGridPixel(' ', 10, 10);
        tester.assertGridPixel('!', 60, 10);
        JSONObject f = tester.getFeature('!');
        assertEquals("Green Forest", f.getString("NAME"));
    }

    @Test
    public void testAlternateMimetype() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:RoadSegments&styles=line"
                                + "&bbox=-0.0042,-0.0042,0.0042,0.0042&width=256&height=256&srs=EPSG:4326&format=application/json;type=utfgrid");
        checkRoadSegments(tester);
    }

    /**
     * Using a color classified style. Should not make any different to UTFGrid, as long as we paint
     * all features
     */
    @Test
    public void testLineSymbolizerClassified() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:RoadSegments&styles="
                                + "&bbox=-0.0042,-0.0042,0.0042,0.0042&width=256&height=256&srs=EPSG:4326&format=utfgrid");
        checkRoadSegments(tester);
    }

    @Test
    public void testLineSymbolizer() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:RoadSegments&styles=line"
                                + "&bbox=-0.0042,-0.0042,0.0042,0.0042&width=256&height=256&srs=EPSG:4326&format=utfgrid");
        checkRoadSegments(tester);
    }

    /** Check we get a usable result even with super-thin lines */
    @Test
    public void testThinLineSymbolizer() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:RoadSegments&styles=thin_line"
                                + "&bbox=-0.0042,-0.0042,0.0042,0.0042&width=256&height=256&srs=EPSG:4326&format=utfgrid");
        checkRoadSegments(tester);
    }

    /** Check we get a correct result with graphic stroked + dash array */
    @Test
    public void testDotted() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:RoadSegments&styles=dotted"
                                + "&bbox=-0.0042,-0.0042,0.0042,0.0042&width=256&height=256&srs=EPSG:4326&format=utfgrid");
        checkRoadSegments(tester);
    }

    @Test
    public void testFilteredLineSymbolizer() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:RoadSegments&styles=line"
                                + "&bbox=-0.0042,-0.0042,0.0042,0.0042&width=256&height=256&srs=EPSG:4326&format=utfgrid"
                                + "&CQL_FILTER=NAME%3D%27Main Street%27");
        tester.assertGridPixel(' ', 15, 54);
        tester.assertGridPixel('!', 22, 49);
        JSONObject f = tester.getFeature('!');
        assertEquals("105", f.getString("FID"));
        assertEquals("Main Street", f.getString("NAME"));
        tester.assertGridPixel(' ', 36, 1);
        tester.assertGridPixel(' ', 36, 21);
    }

    private void checkRoadSegments(UTFGridTester tester) {
        tester.assertGridPixel('!', 15, 54);
        JSONObject f = tester.getFeature('!');
        assertEquals("103", f.getString("FID"));
        assertEquals("Route 5", f.getString("NAME"));
        tester.assertGridPixel('#', 22, 49);
        f = tester.getFeature('#');
        assertEquals("105", f.getString("FID"));
        assertEquals("Main Street", f.getString("NAME"));
        tester.assertGridPixel('$', 36, 1);
        f = tester.getFeature('$');
        assertEquals("102", f.getString("FID"));
        assertEquals("Route 5", f.getString("NAME"));
        tester.assertGridPixel('%', 36, 21);
        f = tester.getFeature('%');
        assertEquals("106", f.getString("FID"));
        assertEquals("Dirt Road by Green Forest", f.getString("NAME"));
    }

    @Test
    public void testSolidFillAndRuleWithTextSymbolizerOnly() throws Exception {
        // used to blow up due to the text symbolizer alone
        UTFGridTester tester =
                getAsGridTester(
                        "wms?LAYERS=sf%3Astates&STYLES=population&FORMAT=utfgrid"
                                + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&BBOX=-95.8506355,24.955967,-66.969849,53.8367535&WIDTH=256&HEIGHT=256");
    }

    @Test
    public void testCircle() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:Bridges&styles=circle&bbox=0,0.0005,0.0004,0.0009&width=256&height=256&srs=EPSG:4326&format=utfgrid");
        tester.assertGridPixel(' ', 25, 30);
        tester.assertGridPixel('!', 32, 32);
        JSONObject f = tester.getFeature('!');
        assertEquals("110", f.getString("FID"));
        assertEquals("Cam Bridge", f.getString("NAME"));
    }

    @Test
    public void testLargeCircle() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:Bridges&styles=circle&bbox=0,0.0005,0.0004,0.0009&width=256&height=256&srs=EPSG:4326&format=utfgrid"
                                + "&env=radius:64");
        tester.assertGridPixel('!', 25, 30);
        tester.assertGridPixel('!', 32, 32);
        JSONObject f = tester.getFeature('!');
        assertEquals("110", f.getString("FID"));
        assertEquals("Cam Bridge", f.getString("NAME"));
    }

    @Test
    public void testDecoratedCircle() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap&layers=cite:Bridges&styles=circle&bbox=0,0.0005,0.0004,0.0009&width=256&height=256&srs=EPSG:4326&format=utfgrid"
                                + "&env=radius:64");
        tester.assertGridPixel('!', 25, 30);
        tester.assertGridPixel('!', 32, 32);
        JSONObject f = tester.getFeature('!');
        assertEquals("110", f.getString("FID"));
        assertEquals("Cam Bridge", f.getString("NAME"));
    }

    @Test
    public void testMultiLayer() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap"
                                + "&layers=cite:Forests,cite:Lakes,cite:Ponds,cite:DividedRoutes,cite:RoadSegments,cite:Buildings,cite:Streams,cite:Bridges"
                                + "&styles=&bbox=-0.003,-0.003,0.003,0.003&width=256&height=256&srs=EPSG:4326&format=utfgrid");
        tester.assertGridPixel('!', 6, 4);
        JSONObject f = tester.getFeature('!');
        assertEquals("119", f.getString("FID"));
        assertEquals("Route 75", f.getString("NAME"));
        assertEquals(0, f.getInt("NUM_LANES"));

        tester.assertGridPixel('#', 6, 27);
        f = tester.getFeature('#');
        assertEquals("111", f.getString("FID"));
        assertEquals("Cam Stream", f.getString("NAME"));

        tester.assertGridPixel('%', 10, 12);
        f = tester.getFeature('%');
        assertEquals("120", f.getString("FID"));
        assertEquals(" ", f.getString("NAME"));
        assertEquals("Stock Pond", f.getString("TYPE"));

        tester.assertGridPixel('$', 10, 62);
        f = tester.getFeature('$');
        assertEquals("103", f.getString("FID"));
        assertEquals("Route 5", f.getString("NAME"));

        tester.assertGridPixel('(', 22, 56);
        f = tester.getFeature('(');
        assertEquals("114", f.getString("FID"));
        assertEquals("215 Main Street", f.getString("ADDRESS"));

        tester.assertGridPixel(')', 24, 33);
        f = tester.getFeature(')');
        assertEquals("110", f.getString("FID"));
        assertEquals("Cam Bridge", f.getString("NAME"));

        tester.assertGridPixel('&', 24, 35);
        f = tester.getFeature('&');
        assertEquals("105", f.getString("FID"));
        assertEquals("Main Street", f.getString("NAME"));

        tester.assertGridPixel('+', 24, 43);
        f = tester.getFeature('+');
        assertEquals("113", f.getString("FID"));
        assertEquals("123 Main Street", f.getString("ADDRESS"));

        tester.assertGridPixel('-', 45, 48);
        f = tester.getFeature('-');
        assertEquals("101", f.getString("FID"));
        assertEquals("Blue Lake", f.getString("NAME"));

        tester.assertGridPixel(',', 35, 17);
        f = tester.getFeature(',');
        assertEquals("106", f.getString("FID"));
        assertEquals("Dirt Road by Green Forest", f.getString("NAME"));

        tester.assertGridPixel('\'', 38, 25);
        f = tester.getFeature('\'');
        assertEquals("109", f.getString("FID"));
        assertEquals("Green Forest", f.getString("NAME"));

        tester.assertGridPixel('*', 32, 9);
        f = tester.getFeature('*');
        assertEquals("102", f.getString("FID"));
        assertEquals("Route 5", f.getString("NAME"));
    }

    @Test
    public void testMultiLayerForestOnTop() throws Exception {
        UTFGridTester tester =
                getAsGridTester(
                        "wms?service=WMS&version=1.1.0&request=GetMap"
                                + "&layers=cite:Lakes,cite:Ponds,cite:DividedRoutes,cite:RoadSegments,cite:Buildings,cite:Streams,cite:Bridges,cite:Forests"
                                + "&styles=&bbox=-0.003,-0.003,0.003,0.003&width=256&height=256&srs=EPSG:4326&format=utfgrid");
        tester.assertGridPixel('!', 6, 4);
        JSONObject f = tester.getFeature('!');
        assertEquals("119", f.getString("FID"));
        assertEquals("Route 75", f.getString("NAME"));
        assertEquals(0, f.getInt("NUM_LANES"));

        tester.assertGridPixel('#', 6, 27);
        f = tester.getFeature('#');
        assertEquals("111", f.getString("FID"));
        assertEquals("Cam Stream", f.getString("NAME"));

        tester.assertGridPixel('%', 10, 12);
        f = tester.getFeature('%');
        assertEquals("120", f.getString("FID"));
        assertEquals(" ", f.getString("NAME"));
        assertEquals("Stock Pond", f.getString("TYPE"));

        tester.assertGridPixel('$', 10, 62);
        f = tester.getFeature('$');
        assertEquals("103", f.getString("FID"));
        assertEquals("Route 5", f.getString("NAME"));

        tester.assertGridPixel('\'', 23, 33);
        f = tester.getFeature('\'');
        assertEquals("110", f.getString("FID"));
        assertEquals("Cam Bridge", f.getString("NAME"));

        tester.assertGridPixel('&', 22, 56);
        tester.assertGridPixel('&', 24, 35);
        tester.assertGridPixel('&', 24, 43);
        tester.assertGridPixel('&', 45, 48);
        tester.assertGridPixel('&', 35, 17);
        tester.assertGridPixel('&', 24, 33);
        f = tester.getFeature('&');
        assertEquals("109", f.getString("FID"));
        assertEquals("Green Forest", f.getString("NAME"));

        tester.assertGridPixel('(', 32, 9);
        f = tester.getFeature('(');
        assertEquals("102", f.getString("FID"));
        assertEquals("Route 5", f.getString("NAME"));
    }

    @Test
    public void testPolygonExtractionFromRaster() throws Exception {
        String url =
                "wms?LAYERS="
                        + getLayerId(MockData.TASMANIA_DEM)
                        + "&styles=polygonExtract&"
                        + "FORMAT=utfgrid&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG%3A4326"
                        + "&BBOX=145,-43,146,-41&WIDTH=100&HEIGHT=200";
        UTFGridTester tester = getAsGridTester(url, 100, 200, 4);
        assertTrue(tester.getKeyCount() > 0);
    }
}
