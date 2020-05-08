/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.*;
import javax.xml.namespace.QName;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.*;
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
import org.geoserver.wms.wms_1_1_1.GetFeatureInfoTest;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.Hints;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetFeatureInfoJSONTest extends GetFeatureInfoTest {

    public static final QName TEMPORAL_DATA =
            new QName(CiteTestData.CITE_URI, "TemporalData", CiteTestData.CITE_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addVectorLayer(
                TEMPORAL_DATA,
                Collections.EMPTY_MAP,
                "TemporalData.properties",
                SystemTestData.class,
                getCatalog());
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
        assertEquals(JSONType.jsonp, response.getContentType());

        // Check if the character encoding is the one expected
        assertTrue("UTF-8".equals(response.getCharacterEncoding()));

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
        assertEquals(JSONType.jsonp, response.getContentType());

        // Check if the character encoding is the one expected
        assertTrue("UTF-8".equals(response.getCharacterEncoding()));

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
        assertEquals(JSONType.json, response.getContentType());

        // Check if the character encoding is the one expected
        assertTrue("UTF-8".equals(response.getCharacterEncoding()));

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
        assertEquals(JSONType.json, response.getContentType());

        // Check if the character encoding is the one expected
        assertTrue("UTF-8".equals(response.getCharacterEncoding()));

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
                new NumberRange<Double>(Double.class, 500525d, 500575d)
                        .contains((Number) coords.getDouble(0)));
        assertTrue(
                new NumberRange<Double>(Double.class, 500025d, 500050d)
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
            assertEquals(JSONType.json, response.getContentType());

            // Check if the character encoding is the one expected
            assertTrue("UTF-8".equals(response.getCharacterEncoding()));

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
            assertEquals(JSONType.json, response.getContentType());

            // Check if the character encoding is the one expected
            assertTrue("UTF-8".equals(response.getCharacterEncoding()));

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
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("LAYER", mapLayerInfo.getName());
        Request request = new Request();
        request.setKvp(parameters);
        Dispatcher.REQUEST.set(request);
        FeatureCollection fc = ft.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        FeatureCollectionType fct = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fct.getFeature().add(fc);
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
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("LAYER", lgInfo.getName());
        Request request = new Request();
        request.setKvp(parameters);
        Dispatcher.REQUEST.set(request);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        FeatureCollectionType fct = WfsFactory.eINSTANCE.createFeatureCollectionType();
        for (LayerInfo l : layers) {
            FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(l.getName());
            fct.getFeature().add(fti.getFeatureSource(null, null).getFeatures());
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
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("LAYER", lgInfo.getName());
        Request request = new Request();
        request.setKvp(parameters);
        Dispatcher.REQUEST.set(request);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        FeatureCollectionType fct = WfsFactory.eINSTANCE.createFeatureCollectionType();
        for (LayerInfo l : layers) {
            FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(l.getName());
            fct.getFeature().add(fti.getFeatureSource(null, null).getFeatures());
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
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("LAYER", mapLayerInfo.getName());
        Request request = new Request();
        request.setKvp(parameters);
        Dispatcher.REQUEST.set(request);
        FeatureCollection fc = ft.getFeatureSource(null, null).getFeatures();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        FeatureCollectionType fct = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fct.getFeature().add(fc);
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
}
