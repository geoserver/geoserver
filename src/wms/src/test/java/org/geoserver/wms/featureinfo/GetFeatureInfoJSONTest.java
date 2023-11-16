/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.geoserver.data.test.MockData.TASMANIA_DEM;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import javax.xml.namespace.QName;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.tiffspy.GeoTIFFSpyFormat;
import org.geoserver.wms.tiffspy.GeoTIFFSpyReader;
import org.geoserver.wms.wms_1_1_1.GetFeatureInfoTest;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.Hints;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetFeatureInfoJSONTest extends GetFeatureInfoTest {

    public static final QName TEMPORAL_DATA =
            new QName(CiteTestData.CITE_URI, "TemporalData", CiteTestData.CITE_PREFIX);

    public static final String LABEL_IN_FEATURE_INFO_STYLE_DEM = "labelInFeatureInfoTazDem";
    public static final String LABEL_CUSTOM_NAME_STYLE_DEM = "labelCustomNameTazDem";
    public static final String LABEL_IN_FEATURE_INFO_DEM_REPLACE =
            "labelInFeatureInfoTazDemReplace";
    public static final String LABEL_IN_FEATURE_INFO_DEM_NONE = "labelInFeatureInfoTazDemNone";
    public static final String LABEL_IN_FEATURE_INFO_DEM_VALUES =
            "labelInFeatureInfoTazDemColorMapValues";
    public static final String LABEL_IN_FEATURE_INFO_MULTIPLE_SYMBOLIZERS =
            "labelInFeatureInfoTazDemMultipleSymbolizers";
    public static final String LABEL_IN_FEATURE_INFO_STYLE_BM = "labelInFeatureInfoTazBm";
    public static final String LABEL_IN_FEATURE_INFO_STYLE_MULTIPLE_SYMBLOZERS2 =
            "labelInFeatureInfoTazBmMultipleSymbolizers";

    public static final String RASTER_VECTOR = "rasterVector";

    public static final String FOOTPRINT_RASTER = "footprintsRaster";

    public static final String JIFFLE_CONDITION = "jiffleCondition";

    public static final QName TASMANIA_SPY = new QName(WCS_URI, "BlueMarbleSpy", WCS_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addVectorLayer(
                TEMPORAL_DATA,
                Collections.emptyMap(),
                "TemporalData.properties",
                SystemTestData.class,
                catalog);
        testData.addStyle(LABEL_IN_FEATURE_INFO_STYLE_DEM, getClass(), catalog);
        testData.addStyle(LABEL_CUSTOM_NAME_STYLE_DEM, getClass(), catalog);
        testData.addStyle(LABEL_IN_FEATURE_INFO_DEM_REPLACE, getClass(), catalog);
        testData.addStyle(LABEL_IN_FEATURE_INFO_DEM_NONE, getClass(), catalog);
        testData.addStyle(LABEL_IN_FEATURE_INFO_DEM_VALUES, getClass(), catalog);
        testData.addStyle(LABEL_IN_FEATURE_INFO_MULTIPLE_SYMBOLIZERS, getClass(), catalog);
        testData.addStyle(LABEL_IN_FEATURE_INFO_STYLE_BM, getClass(), catalog);
        testData.addStyle(LABEL_IN_FEATURE_INFO_STYLE_MULTIPLE_SYMBLOZERS2, getClass(), catalog);
        testData.addStyle(RASTER_VECTOR, getClass(), catalog);
        testData.addStyle(JIFFLE_CONDITION, getClass(), catalog);
        testData.addStyle(FOOTPRINT_RASTER, getClass(), catalog);
        Map<SystemTestData.LayerProperty, Object> propertyMap = new HashMap<>();
        propertyMap.put(SystemTestData.LayerProperty.STYLE, "raster");
        testData.addRasterLayer(
                TASMANIA_DEM, "tazdem.tiff", "tiff", propertyMap, SystemTestData.class, catalog);
        testData.addRasterLayer(
                TASMANIA_SPY, "tazbm.tiff", "tiff", propertyMap, SystemTestData.class, catalog);
    }

    /** Tests JSONP outside of expected polygon */
    @Test
    public void testSimpleJSONP() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.jsonp;

        // JSONP
        JSONType.setJsonpEnabled(true);
        MockHttpServletResponse response = getAsServletResponse(request, "");
        JSONType.setJsonpEnabled(false);

        // MimeType
        assertEquals(JSONType.jsonp, getBaseMimeType(response.getContentType()));

        // Check if the character encoding is the one expected
        assertEquals(UTF_8.name(), response.getCharacterEncoding());

        // Content
        String result = response.getContentAsString();

        assertNotNull(result);

        assertTrue(result.startsWith(JSONType.CALLBACK_FUNCTION));
        assertTrue(result.endsWith(")"));
        assertTrue(result.indexOf("Green Forest") > 0);

        result = result.substring(0, result.length() - 1);
        result = result.substring(JSONType.CALLBACK_FUNCTION.length() + 1, result.length());

        JSONObject rootObject = JSONObject.fromObject(result);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"), "the_geom");
    }

    /** Tests jsonp with custom callback function */
    @Test
    public void testCustomJSONP() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.jsonp
                        + "&format_options="
                        + JSONType.CALLBACK_FUNCTION_KEY
                        + ":custom";
        // JSONP
        JSONType.setJsonpEnabled(true);
        MockHttpServletResponse response = getAsServletResponse(request, "");
        JSONType.setJsonpEnabled(false);

        // MimeType
        assertEquals(JSONType.jsonp, getBaseMimeType(response.getContentType()));

        // Check if the character encoding is the one expected
        assertEquals(UTF_8.name(), response.getCharacterEncoding());

        // Content
        String result = response.getContentAsString();
        // System.out.println(result);
        assertNotNull(result);

        assertTrue(result.startsWith("custom("));
        assertTrue(result.endsWith(")"));
        assertTrue(result.indexOf("Green Forest") > 0);

        result = result.substring(0, result.length() - 1);
        result = result.substring("custom".length() + 1, result.length());

        JSONObject rootObject = JSONObject.fromObject(result);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"), "the_geom");
    }

    /** Tests JSON outside of expected polygon */
    @Test
    public void testSimpleJSON() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.json;

        // JSON
        MockHttpServletResponse response = getAsServletResponse(request, "");

        // MimeType
        assertEquals(JSONType.json, getBaseMimeType(response.getContentType()));

        // Check if the character encoding is the one expected
        assertEquals(UTF_8.name(), response.getCharacterEncoding());

        // Content
        String result = response.getContentAsString();

        assertNotNull(result);

        JSONObject rootObject = JSONObject.fromObject(result);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"), "the_geom");
    }

    @Test
    public void testPropertySelection() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?service=wms&version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.json
                        + "&propertyName=NAME";

        // JSON
        MockHttpServletResponse response = getAsServletResponse(request, "");

        // MimeType
        assertEquals(JSONType.json, getBaseMimeType(response.getContentType()));

        // Check if the character encoding is the one expected
        assertEquals(UTF_8.name(), response.getCharacterEncoding());

        // Content
        String result = response.getContentAsString();

        assertNotNull(result);

        JSONObject rootObject = JSONObject.fromObject(result);
        // print(rootObject);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertTrue(aFeature.getJSONObject("geometry").isNullObject());
        JSONObject properties = aFeature.getJSONObject("properties");
        assertTrue(properties.getJSONObject("FID").isNullObject());
        assertEquals("Green Forest", properties.get("NAME"));
    }

    @Test
    public void testReprojectedLayer() throws Exception {
        String layer = getLayerId(MockData.MPOLYGONS);

        String request =
                "wms?version=1.1.1&bbox=500525,500025,500575,500050&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.json;

        // JSON
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONObject feature = (JSONObject) json.getJSONArray("features").get(0);
        JSONObject geom = feature.getJSONObject("geometry");
        // unroll the geometry and get the first coordinate
        JSONArray coords =
                geom.getJSONArray("coordinates").getJSONArray(0).getJSONArray(0).getJSONArray(0);
        assertTrue(
                new NumberRange<>(Double.class, 500525d, 500575d)
                        .contains((Number) coords.getDouble(0)));
        assertTrue(
                new NumberRange<>(Double.class, 500025d, 500050d)
                        .contains((Number) coords.getDouble(1)));
    }

    /** Tests CQL filter */
    @Test
    public void testCQLFilter() throws Exception {
        String layer = getLayerId(MockData.FORESTS);

        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.json;

        JSONObject json = (JSONObject) getAsJSON(request);
        JSONArray features = json.getJSONArray("features");
        assertTrue(features.size() > 0);

        // Add CQL filter
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(layer);
        try {
            info.setCqlFilter("NAME LIKE 'Red%'");
            getCatalog().save(info);
            json = (JSONObject) getAsJSON(request);
            features = json.getJSONArray("features");
            assertEquals(0, features.size());
        } finally {
            info = getCatalog().getFeatureTypeByName(layer);
            info.setCqlFilter(null);
            getCatalog().save(info);
        }
    }

    @Test
    public void testDateTimeFormattingEnabled() throws Exception {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-05:00"));
        try {
            System.getProperties().remove("org.geotools.dateTimeFormatHandling");
            System.setProperty("org.geotools.localDateTimeHandling", "true");
            Hints.scanSystemProperties();
            String layer = getLayerId(TEMPORAL_DATA);
            String request =
                    "wms?version=1.1.1&bbox=39.73245,2.00342,39.732451,2.003421&styles=&format=jpeg"
                            + "&request=GetFeatureInfo&layers="
                            + layer
                            + "&query_layers="
                            + layer
                            + "&width=10&height=10&x=5&y=5"
                            + "&info_format="
                            + JSONType.json;

            // JSON
            MockHttpServletResponse response = getAsServletResponse(request, "");

            // MimeType
            assertEquals(JSONType.json, getBaseMimeType(response.getContentType()));

            // Check if the character encoding is the one expected
            assertEquals(UTF_8.name(), response.getCharacterEncoding());

            // Content
            String result = response.getContentAsString();
            assertNotNull(result);

            JSONObject rootObject = JSONObject.fromObject(result);
            assertEquals(rootObject.get("type"), "FeatureCollection");
            JSONArray featureCol = rootObject.getJSONArray("features");
            JSONObject aFeature = featureCol.getJSONObject(0);
            JSONObject properties = aFeature.getJSONObject("properties");
            assertNotNull(properties);
            assertEquals("2006-06-27T22:00:00-05:00", properties.getString("dateTimeProperty"));
            assertEquals("2006-12-12", properties.getString("dateProperty"));
        } finally {
            TimeZone.setDefault(defaultTimeZone);
            System.getProperties().remove("org.geotools.localDateTimeHandling");
        }
    }

    @Test
    public void testDateTimeFormattingDisabled() throws Exception {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT-05:00"));
            System.setProperty("org.geotools.dateTimeFormatHandling", "false");
            System.setProperty("org.geotools.localDateTimeHandling", "true");
            Hints.scanSystemProperties();
            String layer = getLayerId(TEMPORAL_DATA);
            String request =
                    "wms?version=1.1.1&bbox=39.73245,2.00342,39.732451,2.003421&styles=&format=jpeg"
                            + "&request=GetFeatureInfo&layers="
                            + layer
                            + "&query_layers="
                            + layer
                            + "&width=10&height=10&x=5&y=5"
                            + "&info_format="
                            + JSONType.json;

            // JSON
            MockHttpServletResponse response = getAsServletResponse(request, "");

            // MimeType
            assertEquals(JSONType.json, getBaseMimeType(response.getContentType()));

            // Check if the character encoding is the one expected
            assertEquals(UTF_8.name(), response.getCharacterEncoding());

            // Content
            String result = response.getContentAsString();
            assertNotNull(result);

            JSONObject rootObject = JSONObject.fromObject(result);
            assertEquals(rootObject.get("type"), "FeatureCollection");
            JSONArray featureCol = rootObject.getJSONArray("features");
            JSONObject aFeature = featureCol.getJSONObject(0);
            JSONObject properties = aFeature.getJSONObject("properties");
            assertNotNull(properties);
            assertEquals("2006-06-28T03:00:00Z", properties.getString("dateTimeProperty"));
            assertEquals("2006-12-12", properties.getString("dateProperty"));
        } finally {
            System.getProperties().remove("org.geotools.dateTimeFormatHandling");
            System.getProperties().remove("org.geotools.localDateTimeHandling");
            Hints.scanSystemProperties();
            TimeZone.setDefault(defaultTimeZone);
        }
    }

    /** Tests json output mediated by a free marker template */
    @Test
    public void testJSONFreeMarkerTemplate() throws Exception {
        URL contentUrl = getClass().getResource("../content_json.ftl");
        URL headerUrl = getClass().getResource("../header_json.ftl");
        URL footerUrl = getClass().getResource("../footer_json.ftl");
        GeoServerResourceLoader loader = getDataDirectory().getResourceLoader();
        Resource resource =
                loader.get(
                        Paths.path(
                                "workspaces",
                                TEMPORAL_DATA.getPrefix(),
                                "cite",
                                TEMPORAL_DATA.getLocalPart()));
        Resource workspace = loader.get(Paths.path("workspaces", TEMPORAL_DATA.getPrefix()));
        File fileHeader = new File(workspace.dir(), "header_json.ftl");
        File fileFooter = new File(workspace.dir(), "footer_json.ftl");
        File fileContent = new File(resource.dir(), "content_json.ftl");
        FileUtils.copyURLToFile(headerUrl, fileHeader);
        FileUtils.copyURLToFile(contentUrl, fileContent);
        FileUtils.copyURLToFile(footerUrl, fileFooter);
        GeoJSONFeatureInfoResponse geoJsonResp =
                new GeoJSONFeatureInfoResponse(
                        getWMS(), getCatalog().getResourceLoader(), "application/json");
        FeatureTypeInfo ft =
                getCatalog()
                        .getFeatureTypeByName(
                                TEMPORAL_DATA.getPrefix(), TEMPORAL_DATA.getLocalPart());

        List<MapLayerInfo> queryLayers = new ArrayList<>();
        LayerInfo layerInfo = getCatalog().getLayerByName(TEMPORAL_DATA.getLocalPart());
        MapLayerInfo mapLayerInfo = new MapLayerInfo(layerInfo);
        queryLayers.add(mapLayerInfo);
        GetFeatureInfoRequest getFeatureInfoRequest = new GetFeatureInfoRequest();
        getFeatureInfoRequest.setQueryLayers(queryLayers);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LAYER", mapLayerInfo.getName());
        Request request = new Request();
        request.setKvp(parameters);
        Dispatcher.REQUEST.set(request);
        FeatureCollection fc = ft.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        FeatureCollectionType fct = getFeatureCollectionType(fc);
        geoJsonResp.write(fct, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());
        JSONObject response = JSONObject.fromObject(result);
        // got header ftl
        assertEquals(response.get("header"), "this is the header");
        JSONArray featuresInfo = response.getJSONArray("features");
        JSONObject featureInfo = (JSONObject) featuresInfo.get(0);
        // got content ftl
        assertEquals(featureInfo.get("content"), "this is the content");
        assertEquals(featureInfo.get("type"), "Feature");
        assertEquals(featureInfo.get("id"), "Points.0");
        assertNotNull(featureInfo.get("geometry"));
        JSONObject props = featureInfo.getJSONObject("properties");
        assertEquals(props.get("id"), "t0000");
        assertEquals(props.get("altitude"), "500");
        assertNotNull(props.get("dateTimeProperty"));
        assertNotNull(props.get("dateProperty"));
        // got footer ftl
        assertEquals(response.get("footer"), "this is the footer");
        fileHeader.delete();
        fileContent.delete();
        fileFooter.delete();
    }

    @SuppressWarnings("unchecked")
    private FeatureCollectionType getFeatureCollectionType(FeatureCollection fc) {
        FeatureCollectionType fct = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fct.getFeature().add(fc);
        return fct;
    }

    /** Test json output with two layers having both template * */
    @Test
    public void testJSONFreeMarkerTemplateLayerGroup() throws Exception {
        URL contentUrl = getClass().getResource("../content_json.ftl");
        URL headerUrl = getClass().getResource("../header_json.ftl");
        URL footerUrl = getClass().getResource("../footer_json.ftl");
        GeoServerResourceLoader loader = getDataDirectory().getResourceLoader();
        Resource templates = loader.get(Paths.path("templates"));
        Resource resForest =
                loader.get(
                        Paths.path(
                                "workspaces",
                                MockData.FORESTS.getPrefix(),
                                "cite",
                                MockData.FORESTS.getLocalPart()));
        Resource resLake =
                loader.get(
                        Paths.path(
                                "workspaces",
                                MockData.LAKES.getPrefix(),
                                "cite",
                                MockData.LAKES.getLocalPart()));
        File fileHeader = new File(templates.dir(), "header_json.ftl");
        File fileFooter = new File(templates.dir(), "footer_json.ftl");
        // configure content template for both layers
        File fileContentForest = new File(resForest.dir(), "content_json.ftl");
        File fileContentLake = new File(resLake.dir(), "content_json.ftl");

        FileUtils.copyURLToFile(headerUrl, fileHeader);
        FileUtils.copyURLToFile(contentUrl, fileContentForest);
        FileUtils.copyURLToFile(contentUrl, fileContentLake);
        FileUtils.copyURLToFile(footerUrl, fileFooter);
        GeoJSONFeatureInfoResponse geoJsonResp =
                new GeoJSONFeatureInfoResponse(
                        getWMS(), getCatalog().getResourceLoader(), "application/json");

        List<MapLayerInfo> queryLayers = new ArrayList<>();
        LayerGroupInfo lgInfo = getCatalog().getLayerGroupByName("nature");
        List<LayerInfo> layers = new ArrayList<>();
        for (PublishedInfo info : lgInfo.getLayers()) {
            layers.add((LayerInfo) info);
            queryLayers.add(new MapLayerInfo((LayerInfo) info));
        }
        GetFeatureInfoRequest getFeatureInfoRequest = new GetFeatureInfoRequest();
        getFeatureInfoRequest.setQueryLayers(queryLayers);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LAYER", lgInfo.getName());
        Request request = new Request();
        request.setKvp(parameters);
        Dispatcher.REQUEST.set(request);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        FeatureCollectionType fct = WfsFactory.eINSTANCE.createFeatureCollectionType();
        for (LayerInfo l : layers) {
            FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(l.getName());
            @SuppressWarnings("unchecked")
            EList<FeatureCollection> feature = fct.getFeature();
            feature.add(fti.getFeatureSource(null, null).getFeatures());
        }
        geoJsonResp.write(fct, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());
        JSONObject response = JSONObject.fromObject(result);
        // got header ftl
        assertEquals(response.get("header"), "this is the header");
        JSONArray featuresInfo = response.getJSONArray("features");
        // check the first layer
        JSONObject fiLake = (JSONObject) featuresInfo.get(0);
        assertEquals(fiLake.get("content"), "this is the content");
        assertEquals(fiLake.get("type"), "Feature");
        assertEquals(fiLake.get("id"), "Lakes.1107531835962");
        JSONObject geomLake = fiLake.getJSONObject("geometry");
        // check the geometry attribute
        assertEquals(geomLake.get("type"), "MultiPolygon");
        assertNotNull(geomLake.getJSONArray("coordinates"));
        JSONObject lakeProps = fiLake.getJSONObject("properties");
        assertEquals(lakeProps.get("NAME"), "Blue Lake");
        assertEquals(lakeProps.get("FID"), "101");

        // check the second layer
        JSONObject fiForest = (JSONObject) featuresInfo.get(1);
        // got content ftl
        assertEquals(fiForest.get("content"), "this is the content");
        assertEquals(fiForest.get("type"), "Feature");
        assertEquals(fiForest.get("id"), "Forests.1107531798144");
        JSONObject geomForest = fiForest.getJSONObject("geometry");
        assertEquals(geomForest.get("type"), "MultiPolygon");
        assertNotNull(geomForest.getJSONArray("coordinates"));
        JSONObject forestProps = fiForest.getJSONObject("properties");
        assertEquals(forestProps.get("NAME"), "Green Forest");
        assertEquals(forestProps.get("FID"), "109");
        // got footer ftl
        assertEquals(response.get("footer"), "this is the footer");
        fileHeader.delete();
        fileContentForest.delete();
        fileContentLake.delete();
        fileFooter.delete();
    }

    /** Test Json output with two layers, one without template * */
    @Test
    public void testJSONFreeMarkerTemplateLayerGroupMixed() throws Exception {
        URL contentUrl = getClass().getResource("../content_json.ftl");
        URL headerUrl = getClass().getResource("../header_json.ftl");
        URL footerUrl = getClass().getResource("../footer_json.ftl");
        GeoServerResourceLoader loader = getDataDirectory().getResourceLoader();
        Resource templates = loader.get(Paths.path("templates"));
        Resource resource =
                loader.get(
                        Paths.path(
                                "workspaces",
                                MockData.FORESTS.getPrefix(),
                                "cite",
                                MockData.FORESTS.getLocalPart()));
        File fileHeader = new File(templates.dir(), "header_json.ftl");
        File fileFooter = new File(templates.dir(), "footer_json.ftl");
        File fileContent = new File(resource.dir(), "content_json.ftl");
        FileUtils.copyURLToFile(headerUrl, fileHeader);
        FileUtils.copyURLToFile(contentUrl, fileContent);
        FileUtils.copyURLToFile(footerUrl, fileFooter);
        GeoJSONFeatureInfoResponse geoJsonResp =
                new GeoJSONFeatureInfoResponse(
                        getWMS(), getCatalog().getResourceLoader(), "application/json");

        List<MapLayerInfo> queryLayers = new ArrayList<>();
        LayerGroupInfo lgInfo = getCatalog().getLayerGroupByName("nature");
        List<LayerInfo> layers = new ArrayList<>();
        for (PublishedInfo info : lgInfo.getLayers()) {
            layers.add((LayerInfo) info);
            queryLayers.add(new MapLayerInfo((LayerInfo) info));
        }
        GetFeatureInfoRequest getFeatureInfoRequest = new GetFeatureInfoRequest();
        getFeatureInfoRequest.setQueryLayers(queryLayers);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LAYER", lgInfo.getName());
        Request request = new Request();
        request.setKvp(parameters);
        Dispatcher.REQUEST.set(request);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        FeatureCollectionType fct = WfsFactory.eINSTANCE.createFeatureCollectionType();
        for (LayerInfo l : layers) {
            FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(l.getName());
            @SuppressWarnings("unchecked")
            EList<FeatureCollection> feature = fct.getFeature();
            feature.add(fti.getFeatureSource(null, null).getFeatures());
        }
        geoJsonResp.write(fct, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());
        JSONObject response = JSONObject.fromObject(result);
        // got header ftl
        assertEquals(response.get("header"), "this is the header");
        JSONArray featuresInfo = response.getJSONArray("features");

        // check the first feature normally encoded
        JSONObject fiLake = (JSONObject) featuresInfo.get(0);
        // didn't get the content template
        assertNull(fiLake.get("content"));
        assertEquals(fiLake.get("type"), "Feature");
        assertEquals(fiLake.get("id"), "Lakes.1107531835962");
        // check the geometry
        JSONObject geomLake = fiLake.getJSONObject("geometry");
        assertEquals(geomLake.get("type"), "MultiPolygon");
        assertNotNull(geomLake.getJSONArray("coordinates"));
        JSONObject lakeProps = fiLake.getJSONObject("properties");
        assertEquals(lakeProps.get("NAME"), "Blue Lake");
        assertEquals(lakeProps.get("FID"), "101");

        // second feature used template
        JSONObject fiForest = (JSONObject) featuresInfo.get(1);
        // got content ftl
        assertEquals(fiForest.get("content"), "this is the content");
        assertEquals(fiForest.get("type"), "Feature");
        assertEquals(fiForest.get("id"), "Forests.1107531798144");
        // check the geometry
        JSONObject geomForest = fiForest.getJSONObject("geometry");
        assertEquals(geomForest.get("type"), "MultiPolygon");
        assertNotNull(geomForest.getJSONArray("coordinates"));
        JSONObject forestProps = fiForest.getJSONObject("properties");
        assertEquals(forestProps.get("NAME"), "Green Forest");
        assertEquals(forestProps.get("FID"), "109");
        // got footer ftl
        assertEquals(response.get("footer"), "this is the footer");
        fileHeader.delete();
        fileContent.delete();
        fileFooter.delete();
    }

    /** Tests json output when not all ftl templates are present */
    @Test
    public void testJSONWithFreeMarkerWithMissingTemplate() throws Exception {
        URL contentUrl = getClass().getResource("../content_json.ftl");
        URL footerUrl = getClass().getResource("../footer_json.ftl");
        GeoJSONFeatureInfoResponse geoJsonResp =
                new GeoJSONFeatureInfoResponse(
                        getWMS(), getCatalog().getResourceLoader(), "application/json");
        FeatureTypeInfo ft =
                getCatalog()
                        .getFeatureTypeByName(
                                TEMPORAL_DATA.getPrefix(), TEMPORAL_DATA.getLocalPart());

        GeoServerResourceLoader loader = getDataDirectory().getResourceLoader();
        Resource resource =
                loader.get(
                        Paths.path(
                                "workspaces",
                                TEMPORAL_DATA.getPrefix(),
                                "cite",
                                TEMPORAL_DATA.getLocalPart()));
        Resource workspace = loader.get(Paths.path("workspaces", TEMPORAL_DATA.getPrefix()));
        File fileFooter = new File(workspace.dir(), "footer_json.ftl");
        File fileContent = new File(resource.dir(), "content_json.ftl");
        FileUtils.copyURLToFile(contentUrl, fileContent);
        FileUtils.copyURLToFile(footerUrl, fileFooter);
        List<MapLayerInfo> queryLayers = new ArrayList<>();
        LayerInfo layerInfo = getCatalog().getLayerByName(TEMPORAL_DATA.getLocalPart());
        MapLayerInfo mapLayerInfo = new MapLayerInfo(layerInfo);
        queryLayers.add(mapLayerInfo);
        GetFeatureInfoRequest getFeatureInfoRequest = new GetFeatureInfoRequest();
        getFeatureInfoRequest.setQueryLayers(queryLayers);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LAYER", mapLayerInfo.getName());
        Request request = new Request();
        request.setKvp(parameters);
        Dispatcher.REQUEST.set(request);
        FeatureCollection fc = ft.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        FeatureCollectionType fct = getFeatureCollectionType(fc);
        geoJsonResp.write(fct, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());
        JSONObject response = JSONObject.fromObject(result);
        // didn't get the header
        assertNull(response.get("header"));
        JSONArray featuresInfo = response.getJSONArray("features");
        JSONObject featureInfo = (JSONObject) featuresInfo.get(0);
        // didn't get the content
        assertNull(featureInfo.get("content"));
        assertNotNull(featureInfo.get("id"));
        JSONObject properties = (JSONObject) featureInfo.get("properties");
        assertNotNull(properties.get("altitude"));
        assertNotNull(properties.get("dateTimeProperty"));
        assertNotNull(properties.get("dateProperty"));
        // didn't get the footer
        assertNull(response.get("footer"));
        fileFooter.delete();
        fileContent.delete();
    }

    /**
     * Verifies that templates can be executed if resulting data contains multiple collections
     * although only one layer was queried
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testJSONFreeMarkerTemplateMultipleFeatureCollectionsPerQueryLayer()
            throws Exception {
        URL contentUrl = getClass().getResource("../content_json.ftl");
        URL headerUrl = getClass().getResource("../header_json.ftl");
        URL footerUrl = getClass().getResource("../footer_json.ftl");
        GeoServerResourceLoader loader = getDataDirectory().getResourceLoader();
        Resource templates = loader.get(Paths.path("templates"));

        File fileHeader = new File(templates.dir(), "header_json.ftl");
        File fileFooter = new File(templates.dir(), "footer_json.ftl");
        File fileContent = new File(templates.dir(), "content_json.ftl");
        FileUtils.copyURLToFile(headerUrl, fileHeader);
        FileUtils.copyURLToFile(contentUrl, fileContent);
        FileUtils.copyURLToFile(footerUrl, fileFooter);
        GeoJSONFeatureInfoResponse geoJsonResp =
                new GeoJSONFeatureInfoResponse(
                        getWMS(), getCatalog().getResourceLoader(), "application/json");
        FeatureTypeInfo ft =
                getCatalog()
                        .getFeatureTypeByName(
                                TEMPORAL_DATA.getPrefix(), TEMPORAL_DATA.getLocalPart());

        List<MapLayerInfo> queryLayers = new ArrayList<>();
        LayerInfo layerInfo = getCatalog().getLayerByName(TEMPORAL_DATA.getLocalPart());
        MapLayerInfo mapLayerInfo = new MapLayerInfo(layerInfo);
        queryLayers.add(mapLayerInfo);
        GetFeatureInfoRequest getFeatureInfoRequest = new GetFeatureInfoRequest();
        getFeatureInfoRequest.setQueryLayers(queryLayers);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("LAYER", mapLayerInfo.getName());
        Request request = new Request();
        request.setKvp(parameters);
        Dispatcher.REQUEST.set(request);
        FeatureCollection fc = ft.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        FeatureCollectionType fct = getFeatureCollectionType(fc);

        FeatureTypeInfo squaresTypeInfo =
                getCatalog().getFeatureTypeByName(SQUARES.getPrefix(), SQUARES.getLocalPart());
        FeatureCollection squaresCollection =
                squaresTypeInfo.getFeatureSource(null, null).getFeatures();

        // further featureCollection in result data
        fct.getFeature().add(squaresCollection);

        geoJsonResp.write(fct, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());

        // make sure features of both types where written
        // do not repeat verification of JSON structure, done by other test already
        assertTrue(result.contains("Points.0"));
        assertTrue(result.contains("squares.1"));

        fileHeader.delete();
        fileContent.delete();
        fileFooter.delete();
    }

    @Test
    public void testLabelInFeatureInfoColorMapRamp() throws Exception {
        // tests that with vendorOption <VendorOption name="labelInFeatureInfo">add</VendorOption>
        // in the RasterSymbolizer the matched ColorMapEntry label is added to the getFeatureInfo
        // response
        Catalog cat = getCatalog();
        LayerInfo tazDem =
                cat.getLayerByName(
                        new NameImpl(
                                MockData.TASMANIA_DEM.getPrefix(),
                                MockData.TASMANIA_DEM.getLocalPart()));
        StyleInfo style = cat.getStyleByName(LABEL_IN_FEATURE_INFO_STYLE_DEM);
        tazDem.getStyles().add(style);
        cat.save(tazDem);
        String layerId = getLayerId(MockData.TASMANIA_DEM);
        String request =
                "wms?version=1.1.1"
                        + "&styles="
                        + LABEL_IN_FEATURE_INFO_STYLE_DEM
                        + "&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layerId
                        + "&query_layers="
                        + layerId
                        + "&X=50&Y=50"
                        + "&SRS=EPSG:4326"
                        + "&WIDTH=101&HEIGHT=101"
                        + "&BBOX=144.9566345277708,-42.23886111751199,145.23403931292705,-41.96145633235574"
                        + "&info_format="
                        + JSONType.json;
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONObject properties =
                json.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
        assertTrue(properties.has("Label_GRAY_INDEX"));
        assertEquals(55537, properties.getInt("GRAY_INDEX"));
        assertEquals("55537", properties.getString("Label_GRAY_INDEX"));
    }

    @Test
    public void testLabelInFeatureInfoWitCustomAttributeName() throws Exception {
        // tests that with vendor options
        // <VendorOption name="labelInFeatureInfo">add</VendorOption>
        // <VendorOption name="labelAttributeName">custom name</VendorOption>
        // the matching ColorMap entry label is added to the output format with
        // custom attribute name
        Catalog cat = getCatalog();
        LayerInfo tazDem =
                cat.getLayerByName(
                        new NameImpl(
                                MockData.TASMANIA_DEM.getPrefix(),
                                MockData.TASMANIA_DEM.getLocalPart()));
        StyleInfo style = cat.getStyleByName(LABEL_CUSTOM_NAME_STYLE_DEM);
        tazDem.getStyles().add(style);
        cat.save(tazDem);
        String layerId = getLayerId(MockData.TASMANIA_DEM);
        String request =
                "wms?version=1.1.1"
                        + "&styles="
                        + LABEL_CUSTOM_NAME_STYLE_DEM
                        + "&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layerId
                        + "&query_layers="
                        + layerId
                        + "&X=50&Y=50"
                        + "&SRS=EPSG:4326"
                        + "&WIDTH=101&HEIGHT=101"
                        + "&BBOX=144.9566345277708,-42.23886111751199,145.23403931292705,-41.96145633235574"
                        + "&info_format="
                        + JSONType.json;
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONObject properties =
                json.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
        assertEquals(55537, properties.getInt("GRAY_INDEX"));
        assertEquals("55537", properties.getString("custom name"));
    }

    @Test
    public void testLabelInFeatureInfoReplaceWithColorMapIntervals() throws Exception {
        // Tests that with a vendor option <VendorOption
        // name="labelInFeatureInfo">replace</VendorOption>
        // the label of the matching ColorMapEntry in a ColorMap of type intervals is replacing
        // the pixel value
        Catalog cat = getCatalog();
        LayerInfo tazDem =
                cat.getLayerByName(
                        new NameImpl(
                                MockData.TASMANIA_DEM.getPrefix(),
                                MockData.TASMANIA_DEM.getLocalPart()));
        StyleInfo style = cat.getStyleByName(LABEL_IN_FEATURE_INFO_DEM_REPLACE);
        tazDem.getStyles().add(style);
        cat.save(tazDem);
        String layerId = getLayerId(MockData.TASMANIA_DEM);
        String request =
                "wms?version=1.1.1"
                        + "&styles="
                        + LABEL_IN_FEATURE_INFO_DEM_REPLACE
                        + "&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layerId
                        + "&query_layers="
                        + layerId
                        + "&X=50&Y=50"
                        + "&SRS=EPSG:4326"
                        + "&WIDTH=101&HEIGHT=101"
                        + "&BBOX=145.41806031949818,-42.16195682063699,145.69546510465443,-41.88455203548074"
                        + "&info_format="
                        + JSONType.json;
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONObject properties =
                json.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
        // we have replace size should be one
        assertEquals(1, properties.size());
        assertEquals(">= 308.142116 AND < 752.166285", properties.getString("Label_GRAY_INDEX"));
    }

    @Test
    public void testLabelInFeatureInfoNone() throws Exception {
        // Tests that with a vendor option <VendorOption
        // name="labelInFeatureInfo">none</VendorOption>
        // no ColorMapEntry label is added to the GetFeatureInfo output
        Catalog cat = getCatalog();
        LayerInfo tazDem =
                cat.getLayerByName(
                        new NameImpl(
                                MockData.TASMANIA_DEM.getPrefix(),
                                MockData.TASMANIA_DEM.getLocalPart()));
        StyleInfo style = cat.getStyleByName(LABEL_IN_FEATURE_INFO_DEM_NONE);
        tazDem.getStyles().add(style);
        cat.save(tazDem);
        String layerId = getLayerId(MockData.TASMANIA_DEM);
        String request =
                "wms?version=1.1.1"
                        + "&styles="
                        + LABEL_IN_FEATURE_INFO_DEM_NONE
                        + "&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layerId
                        + "&query_layers="
                        + layerId
                        + "&X=50&Y=50"
                        + "&SRS=EPSG:4326"
                        + "&WIDTH=101&HEIGHT=101"
                        + "&BBOX=145.41806031949818,-42.16195682063699,145.69546510465443,-41.88455203548074"
                        + "&info_format="
                        + JSONType.json;
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONObject properties =
                json.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
        assertFalse(properties.has("Label_GRAY_INDEX"));
        assertTrue(properties.has("GRAY_INDEX"));
    }

    @Test
    public void testLabelInFeatureInfoReplaceWithValuesColorMap() throws Exception {
        // Tests vendor option <VendorOption name="labelInFeatureInfo">replace</VendorOption>
        // with a ColorMap of type values
        Catalog cat = getCatalog();
        LayerInfo tazDem =
                cat.getLayerByName(
                        new NameImpl(
                                MockData.TASMANIA_DEM.getPrefix(),
                                MockData.TASMANIA_DEM.getLocalPart()));
        StyleInfo style = cat.getStyleByName(LABEL_IN_FEATURE_INFO_DEM_VALUES);
        tazDem.getStyles().add(style);
        cat.save(tazDem);
        String layerId = getLayerId(MockData.TASMANIA_DEM);
        String request =
                "wms?version=1.1.1"
                        + "&styles="
                        + LABEL_IN_FEATURE_INFO_DEM_VALUES
                        + "&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layerId
                        + "&query_layers="
                        + layerId
                        + "&X=50&Y=50"
                        + "&SRS=EPSG:4326"
                        + "&WIDTH=101&HEIGHT=101"
                        + "&BBOX=145.11703491210938,-42.28939821012318,145.39443969726562,-42.01199342496693"
                        + "&info_format="
                        + JSONType.json;
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONObject properties =
                json.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
        // we have replace size should be 1
        assertEquals(1, properties.size());
        assertEquals("value is 1", properties.getString("Label_GRAY_INDEX"));
    }

    @Test
    public void testLabelInFeatureInfoMultipleSymbolizers() throws Exception {
        // Tests the labelInFeatureInfo functionality with two Raster Symbolizer in the same rule
        // having respectively the following VendorOptions
        // <VendorOption name="labelAttributeName">first symbolizer</VendorOption>
        // <VendorOption name="labelInFeatureInfo">replace</VendorOption>
        //
        // <VendorOption name="labelInFeatureInfo">replace</VendorOption>
        Catalog cat = getCatalog();
        LayerInfo tazDem =
                cat.getLayerByName(
                        new NameImpl(
                                MockData.TASMANIA_DEM.getPrefix(),
                                MockData.TASMANIA_DEM.getLocalPart()));
        StyleInfo style = cat.getStyleByName(LABEL_IN_FEATURE_INFO_MULTIPLE_SYMBOLIZERS);
        tazDem.getStyles().add(style);
        cat.save(tazDem);
        String layerId = getLayerId(MockData.TASMANIA_DEM);
        String request =
                "wms?version=1.1.1"
                        + "&styles="
                        + LABEL_IN_FEATURE_INFO_MULTIPLE_SYMBOLIZERS
                        + "&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layerId
                        + "&query_layers="
                        + layerId
                        + "&X=50&Y=50"
                        + "&SRS=EPSG:4326"
                        + "&WIDTH=101&HEIGHT=101"
                        + "&BBOX=145.11703491210938,-42.28939821012318,145.39443969726562,-42.01199342496693"
                        + "&info_format="
                        + JSONType.json;
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONObject properties =
                json.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
        // we have replace size should be 2
        assertEquals(2, properties.size());
        assertEquals(">= 1 AND < 124.811736", properties.getString("first symbolizer"));
        assertEquals("value is 1", properties.getString("Label_GRAY_INDEX"));
    }

    @Test
    public void testLabelInFeatureInfoMultiBandColorMapRamp() throws Exception {
        // Test that with a MultiBand raster the vendor options  <VendorOption
        // name="labelInFeatureInfo">add</VendorOption>
        // add the label only for the band being used in the rule
        Catalog cat = getCatalog();
        LayerInfo tazDem =
                cat.getLayerByName(
                        new NameImpl(
                                MockData.TASMANIA_BM.getPrefix(),
                                MockData.TASMANIA_BM.getLocalPart()));
        StyleInfo style = cat.getStyleByName(LABEL_IN_FEATURE_INFO_STYLE_BM);
        tazDem.getStyles().add(style);
        cat.save(tazDem);
        String layerId = getLayerId(MockData.TASMANIA_BM);
        String request =
                "wms?version=1.1.1"
                        + "&styles="
                        + LABEL_IN_FEATURE_INFO_STYLE_BM
                        + "&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layerId
                        + "&query_layers="
                        + layerId
                        + "&X=50&Y=50"
                        + "&SRS=EPSG:4326&WIDTH=101&HEIGHT=101"
                        + "&BBOX=147.22476194612682,-44.045562744140625,147.50216673128307,-43.768157958984375"
                        + "&info_format="
                        + JSONType.json;
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONObject properties =
                json.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
        assertTrue(properties.has("Label_RED_BAND"));
        assertEquals(26, properties.getInt("RED_BAND"));
        assertEquals("21", properties.getString("Label_RED_BAND"));
    }

    @Test
    public void testLabelInFeatureInfoMultiBandMultipleSymbolizers() throws Exception {
        // Tests the labelInFeatureInfo functionality with a MultiBand raster and a style
        // with two Raster Symbolizer in two different
        // FeatureTypeStyle having both a vendor option
        // <VendorOption name="labelInFeatureInfo">add</VendorOption>
        Catalog cat = getCatalog();
        LayerInfo tazDem =
                cat.getLayerByName(
                        new NameImpl(
                                MockData.TASMANIA_BM.getPrefix(),
                                MockData.TASMANIA_BM.getLocalPart()));
        StyleInfo style = cat.getStyleByName(LABEL_IN_FEATURE_INFO_STYLE_MULTIPLE_SYMBLOZERS2);
        tazDem.getStyles().add(style);
        cat.save(tazDem);
        String layerId = getLayerId(MockData.TASMANIA_BM);
        String request =
                "wms?version=1.1.1"
                        + "&styles="
                        + LABEL_IN_FEATURE_INFO_STYLE_MULTIPLE_SYMBLOZERS2
                        + "&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layerId
                        + "&query_layers="
                        + layerId
                        + "&X=50&Y=50"
                        + "&SRS=EPSG:4326&WIDTH=101&HEIGHT=101"
                        + "&BBOX=147.14566041715443,-44.49600223917514,147.42306520231068,-44.21859745401889"
                        + "&info_format="
                        + JSONType.json;
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONObject properties =
                json.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
        assertEquals("13", properties.getString("Label1_RED_BAND"));
        assertEquals("value is 13", properties.getString("Label2_RED_BAND"));
    }

    /**
     * Checks that a style that displays the same data as both raster and vector gets identified
     * twice, once for the raster information, and once for the vector one (with a rendering
     * transformation in the mix)
     */
    @Test
    public void testRasterAndVectorInfo() throws Exception {
        String layerId = getLayerId(MockData.TASMANIA_BM);
        String request =
                "wms?version=1.1.1"
                        + "&styles="
                        + RASTER_VECTOR
                        + "&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layerId
                        + "&query_layers="
                        + layerId
                        + "&X=50&Y=50"
                        + "&SRS=EPSG:4326&WIDTH=100&HEIGHT=100"
                        + "&BBOX=147.14566041715443,-44.49600223917514,147.42306520231068,-44.21859745401889"
                        + "&info_format="
                        + JSONType.json
                        + "&buffer=1";
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONArray features = json.getJSONArray("features");
        assertEquals(2, features.size());

        // raster as point collection result. Has a geometry and the band values
        JSONObject vector = features.getJSONObject(0);
        assertNotNull(vector.getString("id"));
        assertNotNull(vector.getJSONObject("geometry"));
        JSONObject vectorProps = vector.getJSONObject("properties");
        assertEquals(13, vectorProps.getInt("RED_BAND"));
        assertEquals(36, vectorProps.getInt("GREEN_BAND"));
        assertEquals(76, vectorProps.getInt("BLUE_BAND"));

        // direct raster identify. No geometry, empty id, but has band values
        JSONObject raster = features.getJSONObject(1);
        assertEquals("", raster.getString("id"));
        assertTrue(raster.getJSONObject("geometry").isNullObject());
        JSONObject rasterProps = raster.getJSONObject("properties");
        assertEquals(13, rasterProps.getInt("RED_BAND"));
        assertEquals(36, rasterProps.getInt("GREEN_BAND"));
        assertEquals(76, rasterProps.getInt("BLUE_BAND"));
    }

    @Test
    public void testMosaicFootprintRaster() throws Exception {
        // both footprint extraction and raster identification at the same time
        String url =
                "wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES="
                        + FOOTPRINT_RASTER
                        + "&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetFeatureInfo&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150"
                        + "&transparent=false&CQL_FILTER=location like 'green%25' + "
                        + "&query_layers=sf:mosaic&x=10&y=10"
                        + "&info_format="
                        + JSONType.json;
        JSONObject json = (JSONObject) getAsJSON(url);
        JSONArray features = json.getJSONArray("features");
        assertEquals(2, features.size());

        // footprint extraction,
        JSONObject vector = features.getJSONObject(0);
        assertNotNull(vector.getString("id"));
        assertNotNull(vector.getJSONObject("geometry"));
        assertEquals("MultiPolygon", vector.getJSONObject("geometry").getString("type"));
        JSONObject vectorProps = vector.getJSONObject("properties");
        assertEquals("green_00000002T0000000Z.tiff", vectorProps.getString("location"));
        assertEquals("0002-12-02T00:00:00Z", vectorProps.getString("ingestion"));

        // direct raster identify. No geometry, empty id, but has band values
        JSONObject raster = features.getJSONObject(1);
        assertEquals("", raster.getString("id"));
        assertTrue(raster.getJSONObject("geometry").isNullObject());
        JSONObject rasterProps = raster.getJSONObject("properties");
        assertEquals(0, rasterProps.getInt("RED_BAND"));
        assertEquals(255, rasterProps.getInt("GREEN_BAND"));
        assertEquals(0, rasterProps.getInt("BLUE_BAND"));
    }

    @Test
    public void testFeatureInfoRTMultiband() throws Exception {
        // use the request as is, ping one point in the middle where the condition applies
        // and one in the top/left corner where it doesn't
        checkFeatureInfoRTMultiband(r -> r, 25);
        checkFeatureInfoRTMultiband(r -> r.replace("&X=50&Y=50", "&X=0&Y=0"), 0);
    }

    @Test
    public void testFeatureInfoRTMultibandSelection() throws Exception {
        // add a property selection (the code turns the original band names into band indexes,
        // has no way to know the result will be named in a different way... a fix for another day)
        checkFeatureInfoRTMultiband(r -> r + "&propertyNames=RED_BAND", 25);
        checkFeatureInfoRTMultiband(
                r -> (r + "&propertyNames=RED_BAND").replace("&X=50&Y=50", "&X=0&Y=0"), 0);
    }

    private void checkFeatureInfoRTMultiband(
            Function<String, String> requestCustomizer, int expected) throws Exception {
        // set up the layer to use the spy format
        GeoTIFFSpyFormat.ENABLED = true;
        Catalog catalog = getCatalog();
        CoverageStoreInfo spyStore =
                catalog.getCoverageStoreByName(
                        TASMANIA_SPY.getPrefix(), TASMANIA_SPY.getLocalPart());
        spyStore.setType(GeoTIFFSpyFormat.NAME);
        catalog.save(spyStore);
        catalog.getResourcePool().clear(spyStore);

        try {
            String layerId = getLayerId(TASMANIA_SPY);
            String request =
                    "wms?version=1.1.1"
                            + "&styles="
                            + JIFFLE_CONDITION
                            + "&format=jpeg"
                            + "&request=GetFeatureInfo&layers="
                            + layerId
                            + "&query_layers="
                            + layerId
                            + "&X=50&Y=50"
                            + "&SRS=EPSG:4326&WIDTH=101&HEIGHT=101"
                            + "&BBOX=146.5,-44.5,148,-43"
                            + "&info_format="
                            + JSONType.json;
            request = requestCustomizer.apply(request);
            JSONObject json = (JSONObject) getAsJSON(request);

            JSONObject properties =
                    json.getJSONArray("features").getJSONObject(0).getJSONObject("properties");
            assertTrue(properties.has("jiffle"));
            assertEquals(expected, properties.getInt("jiffle"));

            // the read is gathering all bands required
            GeneralParameterValue[] params = GeoTIFFSpyReader.getLastParams();
            GeneralParameterValue bands = getParameterValue(params, AbstractGridFormat.BANDS);
            assertNotNull(bands);
            assertThat(bands, CoreMatchers.instanceOf(ParameterValue.class));
            assertThat(((ParameterValue) bands).intValueList(), equalTo(new int[] {0, 2}));
        } finally {
            GeoTIFFSpyFormat.ENABLED = false;
        }
    }

    /**
     * Returns the matching {@link GeneralParameterValue} from the given array of parameters
     *
     * @param params
     * @param bands
     * @return
     */
    private GeneralParameterValue getParameterValue(
            GeneralParameterValue[] params, DefaultParameterDescriptor<?> parameter) {
        for (GeneralParameterValue param : params) {
            if (param.getDescriptor().equals(parameter)) {
                return param;
            }
        }
        return null;
    }
}
