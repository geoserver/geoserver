/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.featureinfo.GML3FeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.GetFeatureInfoKvpReader;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.TextFeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.XML311FeatureInfoOutputFormat;
import org.geoserver.wms.wms_1_1_1.CapabilitiesTest;
import org.geotools.filter.v1_1.OGC;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;

/**
 * A GetFeatureInfo 1.3.0 integration test suite covering both spec mandates and geoserver specific
 * features.
 */
public class GetFeatureInfoIntegrationTest extends WMSTestSupport {

    public static String WCS_PREFIX = "wcs";

    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";

    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);

    public static QName SQUARES = new QName(MockData.CITE_URI, "squares", MockData.CITE_PREFIX);

    public static QName CUSTOM = new QName(MockData.CITE_URI, "custom", MockData.CITE_PREFIX);

    public static QName SAMPLEGRIB = new QName(WCS_URI, "sampleGrib", WCS_PREFIX);

    public static QName GENERIC_LINES =
            new QName(MockData.DEFAULT_URI, "genericLines", MockData.DEFAULT_PREFIX);

    public static QName STATES = new QName(MockData.SF_URI, "states", MockData.SF_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpWcs10RasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put(WCS_PREFIX, WCS_URI);

        NamespaceContext ctx = new SimpleNamespaceContext(namespaces);
        XMLUnit.setXpathNamespaceContext(ctx);

        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.setMaxBuffer(50);
        getGeoServer().save(wmsInfo);

        Catalog catalog = getCatalog();

        testData.addStyle("thickStroke", "thickStroke.sld", CapabilitiesTest.class, catalog);
        testData.addStyle("raster", "raster.sld", CapabilitiesTest.class, catalog);
        testData.addStyle("rasterScales", "rasterScales.sld", CapabilitiesTest.class, catalog);
        testData.addStyle("squares", "squares.sld", CapabilitiesTest.class, catalog);
        testData.addStyle(
                "forestsManyRules", "ForestsManyRules.sld", CapabilitiesTest.class, catalog);
        testData.addVectorLayer(
                SQUARES,
                Collections.EMPTY_MAP,
                "squares.properties",
                CapabilitiesTest.class,
                catalog);
        Map propertyMap = new HashMap();
        propertyMap.put(LayerProperty.STYLE, "raster");
        testData.addRasterLayer(
                TASMANIA_BM, "tazbm.tiff", "tiff", propertyMap, SystemTestData.class, catalog);
        testData.addRasterLayer(
                SAMPLEGRIB,
                "sampleGrib.tif",
                null,
                propertyMap,
                GetFeatureInfoIntegrationTest.class,
                catalog);
        testData.addRasterLayer(
                CUSTOM, "custom.zip", null, propertyMap, CapabilitiesTest.class, catalog);

        // this data set contain lines strings but with geometry type set as geometry
        testData.addVectorLayer(
                GENERIC_LINES,
                Collections.emptyMap(),
                "genericLines.properties",
                getClass(),
                getCatalog());
        testData.addStyle("genericLinesStyle", "genericLines.sld", getClass(), getCatalog());

        // set up a non-querable layer.
        testData.addStyle("Population", "Population.sld", CapabilitiesTest.class, catalog);
        testData.addVectorLayer(
                STATES,
                Collections.emptyMap(),
                "states.properties",
                CapabilitiesTest.class,
                catalog);
        LayerInfo layer = catalog.getLayerByName(getLayerId(STATES));
        layer.setQueryable(false);
        catalog.save(layer);
    }

    //    @Override
    //    protected void setUpInternal() throws Exception {
    //        super.setUpInternal();
    //
    //        Map<String, String> namespaces = new HashMap<String, String>();
    //        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
    //        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    //        namespaces.put("wms", "http://www.opengis.net/wms");
    //        namespaces.put("ows", "http://www.opengis.net/ows");
    //        namespaces.put("ogc", "http://www.opengis.net/ogc");
    //        namespaces.put("wfs", "http://www.opengis.net/wfs");
    //        namespaces.put("gml", "http://www.opengis.net/gml");
    //        namespaces.put(WCS_PREFIX, WCS_URI);
    //
    //        NamespaceContext ctx = new SimpleNamespaceContext(namespaces);
    //        XMLUnit.setXpathNamespaceContext(ctx);
    //
    //        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
    //        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
    //        wmsInfo.setMaxBuffer(50);
    //        getGeoServer().save(wmsInfo);
    //    }

    //    @Override
    //    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
    //        super.populateDataDirectory(dataDirectory);
    //        dataDirectory.addWcs10Coverages();
    //        dataDirectory.addStyle("thickStroke", getClass()
    //                .getResource("../wms_1_1_1/thickStroke.sld"));
    //        dataDirectory.addStyle("raster", getClass().getResource("../wms_1_1_1/raster.sld"));
    //        dataDirectory.addStyle("rasterScales",
    //                getClass().getResource("../wms_1_1_1/rasterScales.sld"));
    //        dataDirectory.addCoverage(TASMANIA_BM,
    // getClass().getResource("../wms_1_1_1/tazbm.tiff"),
    //                "tiff", "raster");
    //        dataDirectory.addStyle("squares", getClass().getResource("../wms_1_1_1/squares.sld"));
    //        dataDirectory.addPropertiesType(SQUARES,
    //                getClass().getResource("../wms_1_1_1/squares.properties"), null);
    //    }

    /**
     * As per section 7.4.1, a client shall not issue a GetFeatureInfo request for non queryable
     * layers; yet that section is not too clear with regard to whether an exception should be
     * thrown. I read it like an exception with OperationNotSupported code should be thrown. The
     * full text is:
     *
     * <p><i> GetFeatureInfo is an optional operation. It is only supported for those Layers for
     * which the attribute queryable="1" (true) has been defined or inherited. A client shall not
     * issue a GetFeatureInfo request for other layers. A WMS shall respond with a properly
     * formatted service exception (XML) response (code = OperationNotSupported) if it receives a
     * GetFeatureInfo request but does not support it. </i>
     */
    @Test
    public void testQueryNonQueryableLayer() throws Exception {
        // HACK: fake the WMS facade to inform the layer is non queryable. Looks like we would need
        // a LayerInfo.isQueryable() property
        final WMS wms = (WMS) applicationContext.getBean("wms");
        GetFeatureInfoKvpReader reader =
                (GetFeatureInfoKvpReader) applicationContext.getBean("getFeatureInfoKvpReader");
        try {
            WMS fakeWMS =
                    new WMS(wms.getGeoServer()) {
                        @Override
                        public boolean isQueryable(LayerInfo layer) {
                            if ("Forests".equals(layer.getName())) {
                                return false;
                            }
                            return super.isQueryable(layer);
                        }
                    };

            reader.setWMS(fakeWMS);

            String layer = getLayerId(MockData.FORESTS);
            String request =
                    "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                            + layer
                            + "&query_layers="
                            + layer
                            + "&width=20&height=20&i=10&j=10";
            Document doc = dom(get(request), true);

            XMLAssert.assertXpathEvaluatesTo(
                    "LayerNotQueryable",
                    "/ogc:ServiceExceptionReport/ogc:ServiceException/@code",
                    doc);
        } finally {
            // restore the original wms
            reader.setWMS(wms);
        }
    }

    /**
     * As for section 7.4.3.7, a missing or incorrectly specified pair of I,J parameters shall issue
     * a service exception with {@code InvalidPoint} code.
     */
    @Test
    public void testInvalidPoint() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        // missing I,J parameters
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20";
        Document doc = dom(get(request), true);
        // print(doc);
        XMLAssert.assertXpathEvaluatesTo(
                "InvalidPoint", "/ogc:ServiceExceptionReport/ogc:ServiceException/@code", doc);

        // invalid I,J parameters
        request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=A&j=";
        doc = dom(get(request), true);
        // print(doc);
        XMLAssert.assertXpathEvaluatesTo(
                "InvalidPoint", "/ogc:ServiceExceptionReport/ogc:ServiceException/@code", doc);
    }

    /** Tests a simple GetFeatureInfo works, and that the result contains the expected polygon */
    @Test
    public void testSimple() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=10&j=10";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }

    @Test
    public void testAllowedMimeTypes() throws Exception {

        WMSInfo wms = getWMS().getServiceInfo();
        GetFeatureInfoOutputFormat format = new TextFeatureInfoOutputFormat(getWMS());
        wms.getGetFeatureInfoMimeTypes().add(format.getContentType());
        wms.setGetFeatureInfoMimeTypeCheckingEnabled(true);
        getGeoServer().save(wms);

        // check mime type allowed
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=10&j=10";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);

        // check mime type not allowed
        request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format="
                        + GML3FeatureInfoOutputFormat.FORMAT
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=10&j=10";
        result = getAsString(request);
        assertTrue(result.indexOf("ForbiddenFormat") > 0);

        wms.getGetFeatureInfoMimeTypes().clear();
        wms.setGetFeatureInfoMimeTypeCheckingEnabled(false);
        getGeoServer().save(wms);

        request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format="
                        + GML3FeatureInfoOutputFormat.FORMAT
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=10&j=10";
        result = getAsString(request);
        assertTrue(result.indexOf("Green Forest") > 0);

        // GML 3.1.1 as text/xml; subtype=gml/3.1.1
        request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format="
                        + XML311FeatureInfoOutputFormat.FORMAT
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=10&j=10";
        result = getAsString(request);
        assertTrue(result.indexOf("Green Forest") > 0);
    }

    @Test
    public void testCustomTemplateManyRules() throws Exception {
        // setup custom template
        File root = getTestData().getDataDirectoryRoot();
        File target = new File(root, "workspaces/" + MockData.FORESTS.getPrefix() + "/content.ftl");
        File source = new File("./src/test/resources/org/geoserver/wms/content.ftl");
        try {
            assertTrue(source.exists());
            FileUtils.copyFile(source, target);

            // request with default style, just one rule
            String layer = getLayerId(MockData.FORESTS);
            String request =
                    "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                            + layer
                            + "&query_layers="
                            + layer
                            + "&width=20&height=20&i=10&j=10";
            Document dom = getAsDOM(request);
            // print(dom);

            XMLAssert.assertXpathExists("/html/body/ul/li/b[text() = 'Type: Forests']", dom);

            // request with a style having 21 rules, used to fail, see GEOS-5534
            request =
                    "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=forestsManyRules&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                            + layer
                            + "&query_layers="
                            + layer
                            + "&width=20&height=20&i=10&j=10";
            dom = getAsDOM(request);
            // print(dom);

            XMLAssert.assertXpathExists("/html/body/ul/li/b[text() = 'Type: Forests']", dom);
        } finally {
            FileUtils.deleteQuietly(target);
        }
    }

    /** Tests a simple GetFeatureInfo works, and that the result contains the expected polygon */
    @Test
    public void testSimpleHtml() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=10&j=10";
        Document dom = getAsDOM(request);
        // print(dom);
        // count lines that do contain a forest reference
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[starts-with(.,'Forests.')])", dom);
    }

    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the expected
     * polygon
     */
    @Test
    public void testBuffer() throws Exception {
        // to setup the request and the buffer I rendered BASIC_POLYGONS using GeoServer, then
        // played
        // against the image coordinates
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base =
                "wms?version=1.3.0&bbox=-4.5,-2.,4.5,7&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=300&height=300";
        Document dom = getAsDOM(base + "&i=85&j=230");
        // make sure the document is empty, as we chose an area with no features inside
        XMLAssert.assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the extended buffer, make sure it's in
        dom = getAsDOM(base + "&i=85&j=230&buffer=40");
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);

        // this one would end up catching everything (3 features) if it wasn't that we say the max
        // buffer at 50
        // in the WMS configuration
        dom = getAsDOM(base + "&i=85&j=230&buffer=300");
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }

    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the expected
     * polygon
     */
    @Test
    public void testAutoBuffer() throws Exception {
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base =
                "wms?version=1.3.0&bbox=-4.5,-2.,4.5,7&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=300&height=300&i=111&j=229";
        Document dom = getAsDOM(base + "&styles=");
        // make sure the document is empty, the style we chose has thin lines
        XMLAssert.assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the style with a thick stroke, make
        // sure it's in
        dom = getAsDOM(base + "&styles=thickStroke");
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }

    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the expected
     * polygon
     */
    @Test
    public void testBufferScales() throws Exception {
        String layer = getLayerId(SQUARES);
        String base =
                "wms?version=1.3.0&&format=png&info_format=text/html&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&styles=squares&bbox=0,0,10000,10000&feature_count=10&srs=EPSG:32632";

        // first request, should provide no result, scale is 1:100
        int w = (int) (100.0 / 0.28 * 1000); // dpi compensation
        Document dom = getAsDOM(featureInfoRequest(base, w));
        // print(dom);
        // make sure the document is empty, the style we chose has thin lines
        XMLAssert.assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // second request, should provide oe result, scale is 1:50
        w = (int) (200.0 / 0.28 * 1000); // dpi compensation
        dom = getAsDOM(featureInfoRequest(base, w));
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[starts-with(.,'squares.')])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[. = 'squares.1'])", dom);

        // third request, should provide two result, scale is 1:10
        w = (int) (1000.0 / 0.28 * 1000); // dpi compensation
        dom = getAsDOM(featureInfoRequest(base, w));
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo(
                "2", "count(/html/body/table/tr/td[starts-with(.,'squares.')])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[. = 'squares.1'])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/td[. = 'squares.2'])", dom);
    }

    private String featureInfoRequest(String base, int w) {
        String request = base + "&width=" + w + "&height=" + w + "&i=20&j=" + (w - 20);
        return request;
    }

    /** Tests a GetFeatureInfo againworks, and that the result contains the expected polygon */
    @Test
    public void testTwoLayers() throws Exception {
        String layer = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=10&j=10&info";
        String result = getAsString(request);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
        // GEOS-2603 GetFeatureInfo returns html tables without css style if more than one layer is
        // selected
        assertTrue(result.indexOf("<style type=\"text/css\">") > 0);
    }

    /**
     * Check GetFeatureInfo returns an error if the format is not known, instead of returning the
     * text format as in https://osgeo-org.atlassian.net/browse/GEOS-1924
     */
    @Test
    public void testUknownFormat() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=unknown/format&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=10&j=10";
        Document doc = dom(get(request), true);
        // print(doc);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//ogc:ServiceExceptionReport/ogc:ServiceException)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "InvalidFormat", "/ogc:ServiceExceptionReport/ogc:ServiceException/@code", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "info_format", "/ogc:ServiceExceptionReport/ogc:ServiceException/@locator", doc);
    }

    @Test
    public void testCoverage() throws Exception {
        // https://osgeo-org.atlassian.net/browse/GEOS-2574
        String layer = getLayerId(TASMANIA_BM);
        String request =
                "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=-44.5,146.5,-43,148&width=600&height=600"
                        + "&info_format=text/html&query_layers="
                        + layer
                        + "&i=300&j=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        // print(dom);
        // we also have the charset which may be platf. dep.
        XMLAssert.assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'RED_BAND'])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/th[. = 'GREEN_BAND'])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/th[. = 'BLUE_BAND'])", dom);
    }

    @Test
    public void testCoverageGML() throws Exception {
        // https://osgeo-org.atlassian.net/browse/GEOS-3996
        String layer = getLayerId(TASMANIA_BM);
        String request =
                "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=-44.5,146.5,-43,148&width=600&height=600"
                        + "&info_format=application/vnd.ogc.gml&query_layers="
                        + layer
                        + "&i=300&j=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        // print(dom);

        XMLAssert.assertXpathEvaluatesTo(
                "26.0",
                "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:RED_BAND",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "70.0",
                "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:GREEN_BAND",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "126.0",
                "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:BLUE_BAND",
                dom);
    }

    @Test
    public void testCoverageGML31() throws Exception {
        // https://osgeo-org.atlassian.net/browse/GEOS-3996
        String layer = getLayerId(TASMANIA_BM);
        String request =
                "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=-44.5,146.5,-43,148&width=600&height=600"
                        + "&info_format="
                        + GML3FeatureInfoOutputFormat.FORMAT
                        + "&query_layers="
                        + layer
                        + "&i=300&j=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        // print(dom);

        XMLAssert.assertXpathEvaluatesTo(
                "26.0",
                "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:RED_BAND",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "70.0",
                "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:GREEN_BAND",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "126.0",
                "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:BLUE_BAND",
                dom);
    }

    /** Test that a GetFeatureInfo request shifted plus 360 degrees east has the same results. */
    @Test
    public void testCoverageGML31Plus360() throws Exception {
        String layer = getLayerId(TASMANIA_BM);
        String request =
                "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=-44.5,506.5,-43,508&width=600&height=600"
                        + "&info_format="
                        + GML3FeatureInfoOutputFormat.FORMAT
                        + "&query_layers="
                        + layer
                        + "&i=300&j=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        XMLAssert.assertXpathEvaluatesTo(
                "26.0",
                "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:RED_BAND",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "70.0",
                "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:GREEN_BAND",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "126.0",
                "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:BLUE_BAND",
                dom);
    }

    /** Test that a GetFeatureInfo request shifted minus 360 degrees east has the same results. */
    @Test
    public void testCoverageGML31Minus360() throws Exception {
        String layer = getLayerId(TASMANIA_BM);
        String request =
                "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=-44.5,-213.5,-43,-212&width=600&height=600"
                        + "&info_format="
                        + GML3FeatureInfoOutputFormat.FORMAT
                        + "&query_layers="
                        + layer
                        + "&i=300&j=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        XMLAssert.assertXpathEvaluatesTo(
                "26.0",
                "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:RED_BAND",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "70.0",
                "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:GREEN_BAND",
                dom);
        XMLAssert.assertXpathEvaluatesTo(
                "126.0",
                "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:BLUE_BAND",
                dom);
    }

    @Test
    public void testCoverageScales() throws Exception {
        String layer = getLayerId(TASMANIA_BM);
        String request =
                "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                        + "&layers="
                        + layer
                        + "&styles=rasterScales&bbox=-44.5,146.5,-43,148"
                        + "&info_format=text/html&query_layers="
                        + layer
                        + "&i=300&j=300&srs=EPSG:4326";

        // this one should be blank
        Document dom = getAsDOM(request + "&width=300&height=300");
        XMLAssert.assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th)", dom);

        // this one should draw the coverage
        dom = getAsDOM(request + "&width=600&height=600");
        // we also have the charset which may be platf. dep.
        XMLAssert.assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'RED_BAND'])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/th[. = 'GREEN_BAND'])", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(/html/body/table/tr/th[. = 'BLUE_BAND'])", dom);
    }

    @Test
    public void testOutsideCoverage() throws Exception {
        // a request which is way large on the west side, lots of blank space
        String layer = getLayerId(TASMANIA_BM);
        String request =
                "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                        + "&layers="
                        + layer
                        + "&styles=raster&bbox=0,-90,148,-43"
                        + "&info_format=text/html&query_layers="
                        + layer
                        + "&width=300&height=300&i=10&j=150&srs=EPSG:4326";

        // this one should be blank, but not be a service exception
        Document dom = getAsDOM(request + "");
        XMLAssert.assertXpathEvaluatesTo("1", "count(/html)", dom);
        XMLAssert.assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th)", dom);
    }

    /** Check we report back an exception when query_layer contains layers not part of LAYERS */
    @Test
    public void testUnknownQueryLayer() throws Exception {
        String layers1 = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String layers2 = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.BRIDGES);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                        + layers1
                        + "&query_layers="
                        + layers2
                        + "&width=20&height=20&i=10&j=10&info";

        Document dom = getAsDOM(request + "");
        XMLAssert.assertXpathEvaluatesTo("1", "count(/ogc:ServiceExceptionReport)", dom);
    }

    @Test
    public void testDeriveLayersFromSLD() throws Exception {
        String layers = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);

        String sld =
                "<StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" "
                        + "       xmlns:se=\"http://www.opengis.net/se\" version=\"1.1.0\"> "
                        + " <NamedLayer> "
                        + "  <se:Name>"
                        + getLayerId(MockData.FORESTS)
                        + "</se:Name> "
                        + " </NamedLayer> "
                        + " <NamedLayer> "
                        + "  <se:Name>"
                        + getLayerId(MockData.LAKES)
                        + "</se:Name> "
                        + " </NamedLayer> "
                        + "</StyledLayerDescriptor>";

        // sld present & query_layers null/empty
        String request1 =
                "wms?version=1.3&bbox=146.5,-44.5,148,-43&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&"
                        + "sld_body="
                        + sld.replaceAll("=", "%3D")
                        + "&width=20&height=20&x=10&y=10&info";

        // sld present & query_layers equals layers derived by sld
        String request2 =
                "wms?version=1.3&bbox=146.5,-44.5,148,-43&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&"
                        + "sld_body="
                        + sld.replaceAll("=", "%3D")
                        + "&query_layers="
                        + layers
                        + "&width=20&height=20&x=10&y=10&info";

        // normal request
        String request3 =
                "wms?version=1.3&bbox=146.5,-44.5,148,-43&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&"
                        + "layers="
                        + layers
                        + "&query_layers="
                        + layers
                        + "&width=20&height=20&x=10&y=10&info";

        // sld not present & query_layers null
        String invalidRequest1 =
                "wms?version=1.3&bbox=146.5,-44.5,148,-43&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&"
                        + "layers="
                        + layers
                        + "&width=20&height=20&x=10&y=10&info";

        // sld present & query_layers contains unknown layer
        String invalidRequest2 =
                "wms?version=1.3&bbox=146.5,-44.5,148,-43&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&"
                        + "sld_body="
                        + sld.replaceAll("=", "%3D")
                        + "&query_layers="
                        + getLayerId(MockData.TASMANIA_BM)
                        + "&width=20&height=20&x=10&y=10&info";

        String result1 = getAsString(request1);
        String result2 = getAsString(request2);
        String result3 = getAsString(request3);

        assertEquals(result1, result2);
        assertEquals(result1, result3);

        Document invalidResult1 = getAsDOM(invalidRequest1);
        Document invalidResult2 = getAsDOM(invalidRequest2);
        // print(invalidResult1);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//ogc:ServiceExceptionReport/ogc:ServiceException)", invalidResult1);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//ogc:ServiceExceptionReport/ogc:ServiceException)", invalidResult2);
    }

    @Test
    public void testLayerQualified() throws Exception {
        String layer = "Forests";
        String q =
                "?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=10&j=10";
        String request = "cite/Ponds/wms" + q;
        Document dom = getAsDOM(request);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());

        request = "cite/Forests/wms" + q;
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }

    @Test
    public void testXY() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }

    @Test
    public void testXYGeo() throws Exception {
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String url =
                "wms?styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=292&height=512&x=147&y=360&srs=epsg:4326";

        String request = url + "&VERSION=1.1.1&BBOX=-3.992187,-4.5,3.992188,9.5";
        String result = getAsString(request);
        assertTrue(result.indexOf("the_geom =") > 0);

        request = url + "&VERSION=1.3.0&BBOX=-4.5,-3.992187,9.5,3.992188";
        result = getAsString(request);
        assertTrue(result.indexOf("the_geom =") > 0);
    }

    @Test
    public void testXYProj() throws Exception {
        String layer = getLayerId(MockData.POLYGONS);
        String url =
                "wms?styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&WIDTH=512&HEIGHT=511&X=136&Y=380&srs=epsg:32615";

        String request =
                url + "&VERSION=1.1.1&BBOX=499699.999705,499502.050472,501800.000326,501597.949528";
        String result = getAsString(request);
        // System.out.println(result);
        assertTrue(result.indexOf("polygonProperty =") > 0);

        request =
                url + "&VERSION=1.3.0&BBOX=499699.999705,499502.050472,501800.000326,501597.949528";
        result = getAsString(request);
        assertTrue(result.indexOf("polygonProperty =") > 0);
    }

    @Test
    public void testXYCoverage() throws Exception {
        String layer = getLayerId(MockData.USA_WORLDIMG);
        String url =
                "wms?styles=&format=jpeg&info_format=text/plain&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&WIDTH=512&HEIGHT=408&X=75&Y=132&srs=epsg:4326";

        String request = url + "&VERSION=1.1.1&BBOX=-180,-143.4375,180,143.4375";
        String result = getAsString(request);
        Matcher m =
                Pattern.compile(
                                ".*RED_BAND = (\\d+\\.\\d+).*GREEN_BAND = (\\d+\\.\\d+).*BLUE_BAND = (\\d+\\.\\d+).*",
                                Pattern.DOTALL)
                        .matcher(result);
        assertTrue(m.matches());
        double red = Double.parseDouble(m.group(1));
        double green = Double.parseDouble(m.group(2));
        double blue = Double.parseDouble(m.group(3));

        request = url + "&VERSION=1.3.0&BBOX=-143.4375,-180,143.4375,180";
        result = getAsString(request);

        m =
                Pattern.compile(
                                ".*RED_BAND = (\\d+\\.\\d+).*GREEN_BAND = (\\d+\\.\\d+).*BLUE_BAND = (\\d+\\.\\d+).*",
                                Pattern.DOTALL)
                        .matcher(result);
        assertTrue(m.matches());
        assertEquals(red, Double.parseDouble(m.group(1)), 0.0000001);
        assertEquals(green, Double.parseDouble(m.group(2)), 0.0000001);
        assertEquals(blue, Double.parseDouble(m.group(3)), 0.0000001);
    }

    /** Test GetFeatureInfo for a coverage with longitudes greater than 300 degrees east. */
    @Test
    public void testSampleGrib() throws Exception {
        String layer = getLayerId(SAMPLEGRIB);
        String request =
                "wms?service=WMS&version=1.3.0&request=GetFeatureInfo&styles=&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&info_format="
                        + GML3FeatureInfoOutputFormat.FORMAT
                        + "&width=300&height=400&i=150&j=100"
                        + "&crs=EPSG:4326&bbox=2,302,10,308";
        Document dom = getAsDOM(request);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo(
                "-0.095",
                "substring(//wfs:FeatureCollection/gml:featureMembers/wcs:sampleGrib/wcs:GRAY_INDEX,1,6)",
                dom);
    }

    /**
     * Test GetFeatureInfo for a coverage with longitudes greater than 300 degrees east, with a
     * request shifted 360 degrees west.
     */
    @Test
    public void testSampleGribWest() throws Exception {
        String layer = getLayerId(SAMPLEGRIB);
        String request =
                "wms?service=WMS&version=1.3.0&request=GetFeatureInfo&styles=&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&info_format="
                        + GML3FeatureInfoOutputFormat.FORMAT
                        + "&width=300&height=400&i=150&j=100"
                        + "&crs=EPSG:4326&bbox=2,-58,10,-52";
        Document dom = getAsDOM(request);
        XMLAssert.assertXpathEvaluatesTo(
                "-0.095",
                "substring(//wfs:FeatureCollection/gml:featureMembers/wcs:sampleGrib/wcs:GRAY_INDEX,1,6)",
                dom);
    }

    /**
     * Test GetFeatureInfo for a coverage with longitudes greater than 300 degrees east, with a
     * request shifted 360 degrees west, using the Web Mercator projection.
     */
    @Test
    public void testSampleGribWebMercator() throws Exception {
        String layer = getLayerId(SAMPLEGRIB);
        String request =
                "wms?service=WMS&version=1.3.0&request=GetFeatureInfo&styles=&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&info_format="
                        + GML3FeatureInfoOutputFormat.FORMAT
                        + "&width=300&height=400&i=150&j=100"
                        + "&crs=EPSG:3857"
                        + "&bbox=-6456530.466009867,222684.20850554455,-5788613.521250226,1118889.9748579597";
        Document dom = getAsDOM(request);
        XMLAssert.assertXpathEvaluatesTo(
                "-0.095",
                "substring(//wfs:FeatureCollection/gml:featureMembers/wcs:sampleGrib/wcs:GRAY_INDEX,1,6)",
                dom);
    }

    /**
     * Test GetFeatureInfo operation with lines styled with a line symbolizer. GenericLines layer
     * geometry type is not defined so this use case will force the styles rendering machinery to
     * deal with a generic geometry.
     */
    @Test
    public void testGetFeatureInfoOnLineStringsWithGenericGeometry() throws Exception {
        // perform the get feature info request
        String layer = getLayerId(GENERIC_LINES);
        String request =
                "wms?"
                        + "SERVICE=WMS"
                        + "&VERSION=1.1.1"
                        + "&REQUEST=GetFeatureInfo"
                        + "&FORMAT=image/png"
                        + "&TRANSPARENT=true"
                        + "&STYLES=genericLinesStyle"
                        + "&WIDTH=101"
                        + "&HEIGHT=101"
                        + "&BBOX=0.72235107421875,-1.26617431640625,1.27716064453125,-0.71136474609375"
                        + "&SRS=EPSG:4326"
                        + "&FEATURE_COUNT=50"
                        + "&X=50"
                        + "&Y=50"
                        + "&QUERY_LAYERS="
                        + layer
                        + "&LAYERS="
                        + layer
                        + "&INFO_FORMAT=text/xml"
                        + "&PROPERTYNAME=name";
        Document result = getAsDOM(request, true);
        // xpath engine that will be used to check XML content
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("gs", "http://geoserver.org");
        XpathEngine xpath = XMLUnit.newXpathEngine();
        xpath.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        // let's check the XML response content
        assertThat(
                xpath.evaluate(
                        "boolean(//wfs:FeatureCollection/gml:featureMember/gs:genericLines[@fid='line.2'][gs:name='line2'])",
                        result),
                is("true"));
        assertThat(
                xpath.evaluate(
                        "boolean(//wfs:FeatureCollection/gml:featureMember/gs:genericLines[@fid='line.3'][gs:name='line3'])",
                        result),
                is("true"));
    }

    @Test
    public void testSchemaLeak() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format="
                        + GML3FeatureInfoOutputFormat.FORMAT
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&i=10&j=10";
        // prime system, make sure everything is wired
        getAsDOM(request);

        // count how many imports in the OGC filter schema
        XSDSchema schema = OGC.getInstance().getSchema();
        int expectedImportCounts = schema.getReferencingDirectives().size();

        // now check how many there are after anothe request, should not go up
        getAsDOM(request);
        int actualImportCounts = schema.getReferencingDirectives().size();
        assertEquals(expectedImportCounts, actualImportCounts);
    }

    @Test
    public void testRasterKeepNative() throws Exception {
        // force it to "keep native"
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(CUSTOM));
        ci.setProjectionPolicy(ProjectionPolicy.NONE);
        getCatalog().save(ci);

        // make a first reprojected request on a pixel that's black (0)
        String result =
                getAsString(
                        "wms?REQUEST=GetFeatureInfo&EXCEPTIONS=application%2Fvnd.ogc.se_xml"
                                + "&BBOX=-887430.34934%2C4467316.30601%2C-885862.361705%2C4468893.535223&SERVICE=WMS"
                                + "&INFO_FORMAT=text%2Fplain&QUERY_LAYERS=cite%3Acustom&FEATURE_COUNT=50&Layers=custom"
                                + "&WIDTH=509&HEIGHT=512&format=image%2Fjpeg&styles=&srs=epsg%3A900913&version=1.3.0&i=177&j=225");
        assertTrue(result.contains("0.0"));

        // and now one with actual data, 2
        result =
                getAsString(
                        "wms?REQUEST=GetFeatureInfo&EXCEPTIONS=application%2Fvnd.ogc.se_xml"
                                + "&BBOX=-887430.34934%2C4467316.30601%2C-885862.361705%2C4468893.535223&SERVICE=WMS"
                                + "&INFO_FORMAT=text%2Fplain&QUERY_LAYERS=cite%3Acustom&FEATURE_COUNT=50&Layers=custom"
                                + "&WIDTH=509&HEIGHT=512&format=image%2Fjpeg&styles=&srs=epsg%3A900913&version=1.3.0&i=135&j=223");
        assertTrue(result.contains("2.0"));
    }

    @Test
    public void testRasterReprojectToDeclared() throws Exception {
        // force it to "reproject to declared"
        CoverageInfo ci = getCatalog().getCoverageByName(getLayerId(CUSTOM));
        ci.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
        ci.setSRS("EPSG:900913");
        getCatalog().save(ci);

        // make a first reprojected request on a pixel that's black (0)
        String result =
                getAsString(
                        "wms?REQUEST=GetFeatureInfo&EXCEPTIONS=application%2Fvnd.ogc.se_xml"
                                + "&BBOX=-887430.34934%2C4467316.30601%2C-885862.361705%2C4468893.535223&SERVICE=WMS"
                                + "&INFO_FORMAT=text%2Fplain&QUERY_LAYERS=cite%3Acustom&FEATURE_COUNT=50&Layers=custom"
                                + "&WIDTH=509&HEIGHT=512&format=image%2Fjpeg&styles=&srs=epsg%3A900913&version=1.3.0&i=177&j=225");
        assertTrue(result.contains("0.0"));

        // and now one with actual data, 2
        result =
                getAsString(
                        "wms?REQUEST=GetFeatureInfo&EXCEPTIONS=application%2Fvnd.ogc.se_xml"
                                + "&BBOX=-887430.34934%2C4467316.30601%2C-885862.361705%2C4468893.535223&SERVICE=WMS"
                                + "&INFO_FORMAT=text%2Fplain&QUERY_LAYERS=cite%3Acustom&FEATURE_COUNT=50&Layers=custom"
                                + "&WIDTH=509&HEIGHT=512&format=image%2Fjpeg&styles=&srs=epsg%3A900913&version=1.3.0&i=135&j=223");
        assertTrue(result.contains("2.0"));
    }

    @Test
    public void testQueryableAndNonQueryableLayersWithStyles() throws Exception {
        String states = getLayerId(STATES);
        String forests = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                        + "&info_format=text/plain&request=GetFeatureInfo&width=20&height=20&i=10&j=10"
                        + "&layers="
                        + states
                        + ","
                        + forests
                        + "&query_layers="
                        + states
                        + ","
                        + forests
                        + "&styles=Population,Forests";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }

    @Test
    public void testQueryableAndNonQueryableLayersWithCqlFilter() throws Exception {
        String states = getLayerId(STATES);
        String forests = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                        + "&info_format=text/plain&request=GetFeatureInfo&width=20&height=20&i=10&j=10"
                        + "&layers="
                        + states
                        + ","
                        + forests
                        + "&query_layers="
                        + states
                        + ","
                        + forests
                        + "&styles=&cql_filter=PERSONS>25000000;NAME='Green Forest'";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }

    @Test
    public void testQueryableAndNonQueryableLayersWithFilter() throws Exception {
        String states = getLayerId(STATES);
        String forests = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                        + "&info_format=text/plain&request=GetFeatureInfo&width=20&height=20&i=10&j=10"
                        + "&layers="
                        + states
                        + ","
                        + forests
                        + "&query_layers="
                        + states
                        + ","
                        + forests
                        + "&styles=&filter="
                        + "(%3CFilter%3E%3CPropertyIsGreaterThan%3E%3CPropertyName%3EPERSONS%3C/PropertyName%3E%3CLiteral%3E25000000%3C/Literal%3E%3C/PropertyIsGreaterThan%3E%3C/Filter%3E)"
                        + "(%3CFilter%3E%3CPropertyIsEqualTo%3E%3CPropertyName%3ENAME%3C/PropertyName%3E%3CLiteral%3EGreen%20Forest%3C/Literal%3E%3C/PropertyIsEqualTo%3E%3C/Filter%3E)";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }

    @Test
    public void testClipParam() throws Exception {
        // for simplicity the mask covers 4th quadrant of bbox
        Polygon geom = JTS.toGeometry(new Envelope(0, 0.002, 0, -0.002));
        String wkt = geom.toText();
        String layer = getLayerId(MockData.FORESTS);

        // click outside mask geom
        String insideXY =
                "&x=18&y=18"; // click area inside clip mask and geometry:should return geom
        String outsideXY =
                "&x=5&y=20"; // click area outside clip mask and inside geometry:-should not return
        // geom
        String clipBorderXY = "&x=10&y=10"; // click area bordering clip mask and inside geometry

        CoordinateReferenceSystem crs =
                getCatalog().getLayerByName(MockData.FORESTS.getLocalPart()).getResource().getCRS();
        int srid = CRS.lookupEpsgCode(crs, false);
        String request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&info_format=application/json&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + insideXY
                        + "&srs=EPSG:"
                        + srid
                        + "&clip="
                        + wkt;
        String result = getAsString(request);
        assertNotNull(result);
        // assert a feature was returned
        JSONObject responseJson = JSONObject.fromObject(result);
        assertFalse(responseJson.getJSONArray("features").isEmpty());

        request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&info_format=application/json&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + outsideXY
                        + "&srs=EPSG:"
                        + srid
                        + "&clip="
                        + wkt;
        result = getAsString(request);
        assertNotNull(result);
        // assert no feature was returned
        responseJson = JSONObject.fromObject(result);
        assertTrue(responseJson.getJSONArray("features").isEmpty());

        request =
                "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&info_format=application/json&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + clipBorderXY
                        + "&srs=EPSG:"
                        + srid
                        + "&clip="
                        + wkt;
        result = getAsString(request);
        assertNotNull(result);
        // assert a feature was returned
        responseJson = JSONObject.fromObject(result);
        assertFalse(responseJson.getJSONArray("features").isEmpty());

        // asserting that the returned geometry is clipped
        JSONObject geoJson = responseJson.getJSONArray("features").getJSONObject(0);
        JSONArray coordsArray =
                geoJson.getJSONObject("geometry")
                        .getJSONArray("coordinates")
                        .getJSONArray(0)
                        .getJSONArray(0);
        final GeometryFactory gf = new GeometryFactory();
        Coordinate[] coordinates =
                Arrays.stream(coordsArray.toArray())
                        .map(
                                new Function<Object, Coordinate>() {
                                    @Override
                                    public Coordinate apply(Object t) {
                                        JSONArray cArray = (JSONArray) t;
                                        return new Coordinate(
                                                cArray.getDouble(0), cArray.getDouble(1));
                                    }
                                })
                        .toArray(Coordinate[]::new);

        // the clipped feature geometry
        Polygon clippedPolygon = gf.createPolygon(coordinates);
        // should be empty since clip mask is completely inside clipped polygon
        assertTrue(clippedPolygon.difference(geom).isEmpty());
    }

    @Test
    public void testCoverageClipParam() throws Exception {
        // for simplicity the mask covers 4th quadrant of bbox
        Polygon geom = JTS.toGeometry(new Envelope(147.25, 148.0, -43.75, -44.5));
        String wkt = geom.toText();
        //        String wkt =
        //                "POLYGON((147.50716918865083 -42.73378929337457,148.24325317302583
        // -42.77009194123615,148.18832153240083 -43.395957417642286,147.47421020427583
        // -42.9271576627029,147.50716918865083 -42.73378929337457))";
        String layer = getLayerId(TASMANIA_BM);

        String insideXY = "&i=400&j=400"; // click area inside clip mask, should return results
        String outsideXY = "&i=5&j=5"; // click area outside clip mask , should not return results
        String clipBorderXY =
                "&i=300&j=300"; // click area bordering clip mask should return results

        String request =
                "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=-44.5,146.5,-43,148&width=600&height=600"
                        + "&info_format=application/json&query_layers="
                        + layer
                        + insideXY
                        + "&srs=EPSG:4326"
                        + "&clip="
                        + wkt;
        String json = getAsString(request);
        assertNotNull(json);
        // assert a features was returned
        JSONObject responseJson = JSONObject.fromObject(json);
        assertFalse(responseJson.getJSONArray("features").isEmpty());

        request =
                "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=-44.5,146.5,-43,148&width=600&height=600"
                        + "&info_format=application/json&query_layers="
                        + layer
                        + outsideXY
                        + "&srs=EPSG:4326"
                        + "&clip="
                        + wkt;
        json = getAsString(request);
        assertNotNull(json);
        // assert no features were returned
        responseJson = JSONObject.fromObject(json);
        assertTrue(responseJson.getJSONArray("features").isEmpty());

        request =
                "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                        + "&layers="
                        + layer
                        + "&styles=&bbox=-44.5,146.5,-43,148&width=600&height=600"
                        + "&info_format=application/json&query_layers="
                        + layer
                        + clipBorderXY
                        + "&srs=EPSG:4326"
                        + "&clip="
                        + wkt;
        json = getAsString(request);
        assertNotNull(json);
        // assert a features was returned
        responseJson = JSONObject.fromObject(json);
        assertFalse(responseJson.getJSONArray("features").isEmpty());
    }
}
