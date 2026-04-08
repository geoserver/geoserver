/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wms.GetMapTest;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.featureinfo.GML3FeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.TextFeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.XML311FeatureInfoOutputFormat;
import org.geotools.filter.v1_1.OGC;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.w3c.dom.Document;

/** A GetFeatureInfo 1.3.0 integration test suite covering both spec mandates and geoserver specific features. */
public class GetFeatureInfoIntegrationTest extends WMSTestSupport {

    public static String WCS_PREFIX = "wcs";

    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";

    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);

    public static QName SQUARES = new QName(MockData.CITE_URI, "squares", MockData.CITE_PREFIX);

    public static QName CUSTOM = new QName(MockData.CITE_URI, "custom", MockData.CITE_PREFIX);

    public static QName SAMPLEGRIB = new QName(WCS_URI, "sampleGrib", WCS_PREFIX);

    public static QName GENERIC_LINES = new QName(MockData.DEFAULT_URI, "genericLines", MockData.DEFAULT_PREFIX);

    public static QName STATES = new QName(MockData.SF_URI, "states", MockData.SF_PREFIX);

    private static final QName TIMESERIES = new QName(MockData.SF_URI, "timeseries", MockData.SF_PREFIX);
    private static final QName V_TIME_ELEVATION = new QName(MockData.SF_URI, "TimeElevation", MockData.SF_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpWcs10RasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("iau", "http://geoserver.org/iau");
        namespaces.put(WCS_PREFIX, WCS_URI);

        NamespaceContext ctx = new SimpleNamespaceContext(namespaces);
        XMLUnit.setXpathNamespaceContext(ctx);

        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.setMaxBuffer(50);
        getGeoServer().save(wmsInfo);

        Catalog catalog = getCatalog();

        testData.addStyle("thickStroke", "thickStroke.sld", GetMapTest.class, catalog);
        testData.addStyle("raster", "raster.sld", GetMapTest.class, catalog);
        testData.addStyle("rasterScales", "rasterScales.sld", GetMapTest.class, catalog);
        testData.addStyle("squares", "squares.sld", GetMapTest.class, catalog);
        testData.addStyle("forestsManyRules", "ForestsManyRules.sld", GetMapTest.class, catalog);
        testData.addVectorLayer(SQUARES, Collections.emptyMap(), "squares.properties", GetMapTest.class, catalog);
        Map<LayerProperty, Object> propertyMap = new HashMap<>();
        propertyMap.put(LayerProperty.STYLE, "raster");
        testData.addRasterLayer(TASMANIA_BM, "tazbm.tiff", "tiff", propertyMap, SystemTestData.class, catalog);
        testData.addRasterLayer(
                SAMPLEGRIB, "sampleGrib.tif", null, propertyMap, GetFeatureInfoIntegrationTest.class, catalog);
        testData.addRasterLayer(CUSTOM, "custom.zip", null, propertyMap, GetMapTest.class, catalog);

        // this data set contain lines strings but with geometry type set as geometry
        testData.addVectorLayer(
                GENERIC_LINES, Collections.emptyMap(), "genericLines.properties", getClass(), getCatalog());
        testData.addStyle("genericLinesStyle", "genericLines.sld", getClass(), getCatalog());

        // set up a non-querable layer.
        testData.addStyle("Population", "Population.sld", GetMapTest.class, catalog);
        testData.addVectorLayer(STATES, Collections.emptyMap(), "states.properties", GetMapTest.class, catalog);
        LayerInfo layer = catalog.getLayerByName(getLayerId(STATES));
        layer.setQueryable(false);
        catalog.save(layer);

        testData.addRasterLayer(TIMESERIES, "timeseries.zip", null, getCatalog());
        setupRasterDimension(TIMESERIES, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        testData.addVectorLayer(V_TIME_ELEVATION, getCatalog());
        setupVectorDimension(
                V_TIME_ELEVATION.getLocalPart(),
                ResourceInfo.TIME,
                "time",
                DimensionPresentation.LIST,
                null,
                null,
                null);
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
        request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format="
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

        request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format="
                + GML3FeatureInfoOutputFormat.FORMAT
                + "&request=GetFeatureInfo&layers="
                + layer
                + "&query_layers="
                + layer
                + "&width=20&height=20&i=10&j=10";
        result = getAsString(request);
        assertTrue(result.indexOf("Green Forest") > 0);

        // GML 3.1.1 as text/xml; subtype=gml/3.1.1
        request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format="
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
    public void testCoverageGML() throws Exception {
        // https://osgeo-org.atlassian.net/browse/GEOS-3996
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                + "&layers="
                + layer
                + "&styles=&bbox=-44.5,146.5,-43,148&width=600&height=600"
                + "&info_format=application/vnd.ogc.gml&query_layers="
                + layer
                + "&i=300&j=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        // print(dom);

        assertXpathEvaluatesTo("26.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:RED_BAND", dom);
        assertXpathEvaluatesTo("70.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:GREEN_BAND", dom);
        assertXpathEvaluatesTo("126.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:BLUE_BAND", dom);
    }

    @Test
    public void testCoverageGML31() throws Exception {
        // https://osgeo-org.atlassian.net/browse/GEOS-3996
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
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

        assertXpathEvaluatesTo("26.0", "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:RED_BAND", dom);
        assertXpathEvaluatesTo("70.0", "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:GREEN_BAND", dom);
        assertXpathEvaluatesTo("126.0", "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:BLUE_BAND", dom);
    }

    /** Test that a GetFeatureInfo request shifted plus 360 degrees east has the same results. */
    @Test
    public void testCoverageGML31Plus360() throws Exception {
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                + "&layers="
                + layer
                + "&styles=&bbox=-44.5,506.5,-43,508&width=600&height=600"
                + "&info_format="
                + GML3FeatureInfoOutputFormat.FORMAT
                + "&query_layers="
                + layer
                + "&i=300&j=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("26.0", "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:RED_BAND", dom);
        assertXpathEvaluatesTo("70.0", "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:GREEN_BAND", dom);
        assertXpathEvaluatesTo("126.0", "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:BLUE_BAND", dom);
    }

    /** Test that a GetFeatureInfo request shifted minus 360 degrees east has the same results. */
    @Test
    public void testCoverageGML31Minus360() throws Exception {
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?version=1.3.0&service=wms&request=GetFeatureInfo"
                + "&layers="
                + layer
                + "&styles=&bbox=-44.5,-213.5,-43,-212&width=600&height=600"
                + "&info_format="
                + GML3FeatureInfoOutputFormat.FORMAT
                + "&query_layers="
                + layer
                + "&i=300&j=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("26.0", "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:RED_BAND", dom);
        assertXpathEvaluatesTo("70.0", "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:GREEN_BAND", dom);
        assertXpathEvaluatesTo("126.0", "//wfs:FeatureCollection/gml:featureMembers/wcs:BlueMarble/wcs:BLUE_BAND", dom);
    }

    /** Test GetFeatureInfo for a coverage with longitudes greater than 300 degrees east. */
    @Test
    public void testSampleGrib() throws Exception {
        String layer = getLayerId(SAMPLEGRIB);
        String request = "wms?service=WMS&version=1.3.0&request=GetFeatureInfo&styles=&layers="
                + layer
                + "&query_layers="
                + layer
                + "&info_format="
                + GML3FeatureInfoOutputFormat.FORMAT
                + "&width=300&height=400&i=150&j=100"
                + "&crs=EPSG:4326&bbox=2,302,10,308";
        Document dom = getAsDOM(request);
        // print(dom);
        assertXpathEvaluatesTo(
                "-0.095",
                "substring(//wfs:FeatureCollection/gml:featureMembers/wcs:sampleGrib/wcs:GRAY_INDEX,1,6)",
                dom);
    }

    /**
     * Test GetFeatureInfo for a coverage with longitudes greater than 300 degrees east, with a request shifted 360
     * degrees west.
     */
    @Test
    public void testSampleGribWest() throws Exception {
        String layer = getLayerId(SAMPLEGRIB);
        String request = "wms?service=WMS&version=1.3.0&request=GetFeatureInfo&styles=&layers="
                + layer
                + "&query_layers="
                + layer
                + "&info_format="
                + GML3FeatureInfoOutputFormat.FORMAT
                + "&width=300&height=400&i=150&j=100"
                + "&crs=EPSG:4326&bbox=2,-58,10,-52";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo(
                "-0.095",
                "substring(//wfs:FeatureCollection/gml:featureMembers/wcs:sampleGrib/wcs:GRAY_INDEX,1,6)",
                dom);
    }

    /**
     * Test GetFeatureInfo for a coverage with longitudes greater than 300 degrees east, with a request shifted 360
     * degrees west, using the Web Mercator projection.
     */
    @Test
    public void testSampleGribWebMercator() throws Exception {
        String layer = getLayerId(SAMPLEGRIB);
        String request = "wms?service=WMS&version=1.3.0&request=GetFeatureInfo&styles=&layers="
                + layer
                + "&query_layers="
                + layer
                + "&info_format="
                + GML3FeatureInfoOutputFormat.FORMAT
                + "&width=300&height=400&i=150&j=100"
                + "&crs=EPSG:3857"
                + "&bbox=-6456530.466009867,222684.20850554455,-5788613.521250226,1118889.9748579597";
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo(
                "-0.095",
                "substring(//wfs:FeatureCollection/gml:featureMembers/wcs:sampleGrib/wcs:GRAY_INDEX,1,6)",
                dom);
    }

    /**
     * Test GetFeatureInfo operation with lines styled with a line symbolizer. GenericLines layer geometry type is not
     * defined so this use case will force the styles rendering machinery to deal with a generic geometry.
     */
    @Test
    public void testGetFeatureInfoOnLineStringsWithGenericGeometry() throws Exception {
        // perform the get feature info request
        String layer = getLayerId(GENERIC_LINES);
        String request = "wms?"
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
        String request = "wms?version=1.3.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format="
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
    public void testIAUVectorFeatureInfo() throws Exception {
        String layerId = getLayerId(SystemTestData.MARS_POI);
        String url = "wms?&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.3.0"
                + "&REQUEST=GetFeatureInfo&CRS=IAU:49900&BBOX=-90,-180,90,180"
                + "&WIDTH=20&HEIGHT=20&x=10&y=10&buffer=20"
                + "&LAYERS="
                + layerId
                + "&query_layers="
                + layerId
                + "&info_format="
                + GML3FeatureInfoOutputFormat.FORMAT;
        Document dom = getAsDOM(url);
        // print(dom);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:IAU:49900", "//iau:MarsPoi[@gml:id = 'mars.1']/iau:geom/gml:Point/@srsName", dom);
        assertXpathEvaluatesTo("-27.2282 -36.897", "//iau:MarsPoi[@gml:id = 'mars.1']/iau:geom/gml:Point/gml:pos", dom);
    }
}
