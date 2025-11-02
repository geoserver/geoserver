/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.featureinfo.GML2FeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.GML3FeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.TextFeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.XML2FeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.XML311FeatureInfoOutputFormat;
import org.geoserver.wms.wms_1_3.GetMapIntegrationTest;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyleFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetFeatureInfoTest extends WMSTestSupport {

    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName SQUARES = new QName(MockData.CITE_URI, "squares", MockData.CITE_PREFIX);
    public static QName CUSTOM = new QName(MockData.CITE_URI, "custom", MockData.CITE_PREFIX);

    public static QName POINT_TEST_2D = new QName(MockData.CITE_URI, "point_test_2d", MockData.CITE_PREFIX);
    public static QName POINT_TEST_3D = new QName(MockData.CITE_URI, "point_test_3d", MockData.CITE_PREFIX);

    public static QName STATES = new QName(MockData.SF_URI, "states", MockData.SF_PREFIX);

    protected static QName TIMESERIES = new QName(MockData.SF_URI, "timeseries", MockData.SF_PREFIX);

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);
    private static final String RAIN_RT_STYLE = "filteredRain";
    private static final String RAIN_RT_2_STYLE = "filteredRainTransformEnabled";
    private static final String RAIN_RT_3_STYLE = "filteredRainTransformDisabled";
    private static final String RAIN_CONTOUR_STYLE = "contourRain";
    private static final String RAIN_CONTOUR_2_STYLE = "contourRainTransformEnabled";
    private static final String RAIN_CONTOUR_3_STYLE = "contourRainTransformDisabled";
    private static final String FOOTPRINTS_STYLE = "footprints";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);

        setupOpaqueGroup(getCatalog());

        // setup buffer
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.setMaxBuffer(50);
        getGeoServer().save(wmsInfo);

        // force feature bounding in WFS
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        wfsInfo.setFeatureBounding(true);
        getGeoServer().save(wfsInfo);

        // add a wms store too, if possible
        if (RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            // setup the wms store, resource and layer
            CatalogBuilder cb = new CatalogBuilder(getCatalog());
            WMSStoreInfo wms = cb.buildWMSStore("demo");
            wms.setCapabilitiesURL(RemoteOWSTestSupport.WMS_SERVER_URL + "service=WMS&request=GetCapabilities");
            getCatalog().save(wms);
            cb.setStore(wms);
            WMSLayerInfo states = cb.buildWMSLayer("topp:states");
            states.setName("rstates");
            getCatalog().add(states);
            LayerInfo layer = cb.buildLayer(states);
            getCatalog().add(layer);
        }
        Catalog catalog = getCatalog();
        testData.addStyle("thickStroke", "thickStroke.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle("paramStroke", "paramStroke.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle("raster", "raster.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle("rasterScales", "rasterScales.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle("squares", "squares.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle("point_test", "point_test.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle("scaleBased", "scaleBased.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle("stacker", "stacker.sld", GetFeatureInfoTest.class, catalog);
        testData.addVectorLayer(
                SQUARES, Collections.emptyMap(), "squares.properties", GetFeatureInfoTest.class, catalog);
        Map<LayerProperty, Object> propertyMap = new HashMap<>();
        propertyMap.put(LayerProperty.STYLE, "raster");
        testData.addRasterLayer(TASMANIA_BM, "tazbm.tiff", "tiff", propertyMap, SystemTestData.class, catalog);
        testData.addRasterLayer(
                new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX),
                "raster-filter-test.zip",
                null,
                propertyMap,
                SystemTestData.class,
                catalog);
        testData.addRasterLayer(CUSTOM, "custom.zip", null, propertyMap, GetFeatureInfoTest.class, catalog);
        testData.addRasterLayer(TIMESERIES, "timeseries.zip", null, null, SystemTestData.class, catalog);
        setupRasterDimension(TIMESERIES, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        Map<LayerProperty, Object> properties = new HashMap<>();
        properties.put(
                LayerProperty.LATLON_ENVELOPE,
                new ReferencedEnvelope(
                        130.875825803896,
                        130.898939990319,
                        -16.4491956225999,
                        -16.4338185791628,
                        CRS.decode("EPSG:4326")));
        properties.put(
                LayerProperty.ENVELOPE,
                new ReferencedEnvelope(
                        130.875825803896,
                        130.898939990319,
                        -16.4491956225999,
                        -16.4338185791628,
                        CRS.decode("EPSG:4326")));
        properties.put(LayerProperty.SRS, 4326);

        testData.addVectorLayer(
                POINT_TEST_2D, properties, "point_test_2d.properties", GetFeatureInfoTest.class, catalog);

        properties = new HashMap<>();
        properties.put(
                LayerProperty.LATLON_ENVELOPE,
                new ReferencedEnvelope(
                        130.875825803896,
                        130.898939990319,
                        -16.4491956225999,
                        -16.4338185791628,
                        CRS.decode("EPSG:4326")));
        properties.put(
                LayerProperty.ENVELOPE,
                new ReferencedEnvelope3D(
                        130.875825803896,
                        130.898939990319,
                        -16.4491956225999,
                        -16.4338185791628,
                        95.1442741322517,
                        98.1069524121285,
                        CRS.decode("EPSG:4326")));
        properties.put(LayerProperty.SRS, 4939);
        testData.addVectorLayer(
                POINT_TEST_3D, properties, "point_test_3d.properties", GetFeatureInfoTest.class, catalog);

        // set up a non-querable layer.
        testData.addStyle("Population", "Population.sld", GetFeatureInfoTest.class, catalog);
        testData.addVectorLayer(STATES, Collections.emptyMap(), "states.properties", GetFeatureInfoTest.class, catalog);
        LayerInfo layer = catalog.getLayerByName(getLayerId(STATES));
        layer.setQueryable(false);
        catalog.save(layer);

        // add global rain and style
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
        testData.addStyle(RAIN_RT_STYLE, "filteredRain.sld", GetMapIntegrationTest.class, catalog);
        testData.addStyle(RAIN_RT_2_STYLE, "filteredRain2.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle(RAIN_RT_3_STYLE, "filteredRain3.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle(RAIN_CONTOUR_STYLE, "rainContour.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle(RAIN_CONTOUR_2_STYLE, "rainContour2.sld", GetFeatureInfoTest.class, catalog);
        testData.addStyle(RAIN_CONTOUR_3_STYLE, "rainContour3.sld", GetFeatureInfoTest.class, catalog);

        // footprints extraction tx
        testData.addStyle(FOOTPRINTS_STYLE, "footprints.sld", GetFeatureInfoTest.class, catalog);
    }

    @After
    public void resetSettings() {
        setTransformFeatureInfoDisabled(false);
    }

    private void setTransformFeatureInfoDisabled(boolean disabled) {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        if (wms.isTransformFeatureInfoDisabled() != disabled) {
            wms.setTransformFeatureInfoDisabled(disabled);
            getGeoServer().save(wms);
        }
    }

    /**
     * Tests GML output does not break when asking for an area that has no data with GML feature bounding enabled
     *
     * @param contentType Content-type (MIME-type) to test on.
     * @throws Exception When an XPath Exception occurs.
     */
    private void testGMLNoData(String contentType) throws Exception {
        String layer = getLayerId(MockData.PONDS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                + "&info_format="
                + contentType
                + "&request=GetFeatureInfo&layers="
                + layer
                + "&query_layers="
                + layer
                + "&width=20&height=20&x=20&y=20";
        Document dom = getAsDOM(request);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", dom);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//gml:featureMember)", dom);
    }

    /**
     * Tests GML output does not break when asking for an area that has no data with GML feature bounding enabled. This
     * method tests GML 2 with Content-Type: <code>application/vnd.ogc.gml
     * </code>.
     */
    @Test
    public void testGMLNoData() throws Exception {
        this.testGMLNoData(GML2FeatureInfoOutputFormat.FORMAT);
    }

    /**
     * Tests GML output does not break when asking for an area that has no data with GML feature bounding enabled. This
     * method tests GML 2 with Content-Type: <code>text/xml</code>.
     */
    @Test
    public void testXMLNoData() throws Exception {
        this.testGMLNoData(XML2FeatureInfoOutputFormat.FORMAT);
    }

    /**
     * Tests GML output does not break when asking for an area that has no data with GML feature bounding enabled. This
     * method tests GML 3.1.1 with Content-Type: <code>
     * text/xml; subtype=gml/3.1.1</code>.
     */
    @Test
    public void testXML311NoData() throws Exception {
        this.testGMLNoData(XML311FeatureInfoOutputFormat.FORMAT);
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
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                + "&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer
                + "&query_layers="
                + layer
                + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);

        // check mime type not allowed
        request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                + "&info_format="
                + GML3FeatureInfoOutputFormat.FORMAT
                + "&request=GetFeatureInfo&layers="
                + layer
                + "&query_layers="
                + layer
                + "&width=20&height=20&x=10&y=10";

        result = getAsString(request);
        assertTrue(result.indexOf("ForbiddenFormat") > 0);

        wms.getGetFeatureInfoMimeTypes().clear();
        wms.setGetFeatureInfoMimeTypeCheckingEnabled(false);
        getGeoServer().save(wms);

        request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                + "&info_format="
                + GML3FeatureInfoOutputFormat.FORMAT
                + "&request=GetFeatureInfo&layers="
                + layer
                + "&query_layers="
                + layer
                + "&width=20&height=20&x=10&y=10";

        result = getAsString(request);
        assertTrue(result.indexOf("Green Forest") > 0);
    }

    /** Tests that FEATURE_COUNT is respected globally, not just per layer */
    @Test
    public void testTwoLayersFeatureCount() throws Exception {
        // this request hits on two overlapping features, a lake and a forest
        String layer = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String request = "wms?REQUEST=GetFeatureInfo&EXCEPTIONS=application%2Fvnd.ogc.se_xml&"
                + "BBOX=-0.002356%2C-0.004819%2C0.005631%2C0.004781&SERVICE=WMS&VERSION=1.1.0&X=267&Y=325"
                + "&INFO_FORMAT=application/vnd.ogc.gml"
                + "&QUERY_LAYERS="
                + layer
                + "&Layers="
                + layer
                + " &Styles=&WIDTH=426&HEIGHT=512"
                + "&format=image%2Fpng&srs=EPSG%3A4326";
        // no feature count, just one should be returned
        Document dom = getAsDOM(request);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Forests)", dom);

        // feature count set to 2, both features should be there
        dom = getAsDOM(request + "&FEATURE_COUNT=2");
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("2", "count(//gml:featureMember)", dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Forests)", dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Lakes)", dom);
    }

    @Test
    public void testCoverageGML() throws Exception {
        // https://osgeo-org.atlassian.net/browse/GEOS-3996
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1"
                + "&layers="
                + layer
                + "&styles=&bbox=146.5,-44.5,148,-43&width=600&height=600"
                + "&info_format=application/vnd.ogc.gml&query_layers="
                + layer
                + "&x=300&y=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        // print(dom);

        XMLAssert.assertXpathEvaluatesTo(
                "26.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:RED_BAND", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "70.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:GREEN_BAND", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "126.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:BLUE_BAND", dom);
    }

    @Test
    public void testGMLWithPostFilter() throws Exception {
        // we need to create a situation where a post filter is setup, simple way is to change the
        // style so that its filter is an or with more than 20 children
        Catalog cat = getCatalog();
        LayerInfo l = cat.getLayerByName(getLayerId(MockData.NAMED_PLACES));

        StyleInfo style = l.getDefaultStyle();
        Style s = style.getStyle();

        FeatureTypeStyle fts = s.featureTypeStyles().get(0);
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        StyleFactory sf = CommonFactoryFinder.getStyleFactory();
        for (int i = 0; i < 21; i++) {
            Filter f = ff.equals(ff.literal(1), ff.literal(1));
            Rule r = sf.createRule();
            r.setFilter(f);
            r.symbolizers().add(sf.createPolygonSymbolizer());
            fts.rules().add(r);
        }

        cat.getResourcePool().writeStyle(style, s);
        cat.save(style);

        String layer = getLayerId(MockData.NAMED_PLACES);

        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1"
                + "&layers="
                + layer
                + "&styles=&bbox=0.000004,-0.00285,0.005596,0.00415&width=409&height=512"
                + "&info_format=application/vnd.ogc.gml&query_layers="
                + layer
                + "&x=194&y=229&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
    }
}
