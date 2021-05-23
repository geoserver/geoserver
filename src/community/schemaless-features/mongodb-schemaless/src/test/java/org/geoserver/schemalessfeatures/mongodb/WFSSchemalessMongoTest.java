package org.geoserver.schemalessfeatures.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings({
    "PMD.JUnit4TestShouldUseAfterAnnotation",
    "PMD.JUnit4TestShouldUseBeforeAnnotation"
})
public class WFSSchemalessMongoTest extends AbstractMongoDBOnlineTestSupport {

    private static final String DATA_STORE_NAME = "stationsMongoWfs";

    private static MongoTestSetup testSetup;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Catalog cat = getCatalog();
        DataStoreInfo storeInfo = cat.getDataStoreByName(DATA_STORE_NAME);
        if (storeInfo == null) {
            WorkspaceInfo wi = cat.getDefaultWorkspace();
            storeInfo = addMongoSchemalessStore(wi, DATA_STORE_NAME);
            addMongoSchemalessLayer(wi, storeInfo, StationsTestSetup.COLLECTION_NAME);
        }
    }

    @Override
    protected MongoTestSetup createTestSetups() {
        StationsTestSetup setup = new StationsTestSetup(databaseName);
        testSetup = setup;
        return setup;
    }

    @AfterClass
    public static void tearDown() {
        if (testSetup != null) testSetup.tearDown();
    }

    @Test
    public void testGetStationFeatures() throws Exception {
        JSON json =
                getAsJSON(
                        "wfs?request=GetFeature&version=1.1.0&typename=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&outputFormat=application/json");
        JSONObject jsonObject = (JSONObject) json;
        JSONArray features = jsonObject.getJSONArray("features");
        assertEquals(12, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            checkStationFeature(feature);
        }
    }

    @Test
    public void testGetStationFeaturesWithFilter() throws Exception {
        JSON json =
                getAsJSON(
                        "wfs?request=GetFeature&version=1.1.0&typename=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&outputFormat=application/json&cql_filter=measurements.values.value > 2000");
        JSONObject jsonObject = (JSONObject) json;
        JSONArray features = jsonObject.getJSONArray("features");
        assertEquals(1, features.size());
        assertEquals("58e5889ce4b02461ad5af091", features.getJSONObject(0).getString("id"));
    }

    @Test
    public void testGetStationFeaturesWithFilterPOST() throws Exception {
        String postContent = readResourceContent("./test-data/stations/query/postQuery.xml");
        JSON json =
                postAsJSON(
                        "wfs?request=GetFeature&version=1.1.0&typename=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&outputFormat=application/json&cql_filter=measurements.values.value > 2000",
                        postContent,
                        "application/json");
        JSONObject jsonObject = (JSONObject) json;
        JSONArray features = jsonObject.getJSONArray("features");
        assertEquals(2, features.size());
        assertEquals("58e5889ce4b02461ad5af080", features.getJSONObject(0).getString("id"));
        assertEquals("58e5889ce4b02461ad5af084", features.getJSONObject(1).getString("id"));
    }

    @Test
    public void testGetFeatureRequestUnsupportedFormatReturnError() throws Exception {
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&version=1.1.0&typename=gs:"
                                + StationsTestSetup.COLLECTION_NAME);
        String respStr = resp.getContentAsString();
        assertTrue(
                respStr.contains(
                        "Schemaless support for GetFeature is not available for text/xml"));
    }

    @Test
    public void testGetStationFeaturesWithFilterNull() throws Exception {
        JSON json =
                getAsJSON(
                        "wfs?request=GetFeature&version=1.1.0&typename=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&outputFormat=application/json&cql_filter=nullableField IS NULL");
        JSONObject jsonObject = (JSONObject) json;
        JSONArray features = jsonObject.getJSONArray("features");
        assertEquals(3, features.size());
    }

    @Test
    public void testGetStationFeaturesWithFilterNull2() throws Exception {
        JSON json =
                getAsJSON(
                        "wfs?request=GetFeature&version=1.1.0&typename=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&outputFormat=application/json&cql_filter=anotherNullableField IS NULL");
        JSONObject jsonObject = (JSONObject) json;
        JSONArray features = jsonObject.getJSONArray("features");
        assertEquals(10, features.size());
    }

    @Test
    public void testGetStationFeaturesWithFilterNotNull() throws Exception {
        JSON json =
                getAsJSON(
                        "wfs?request=GetFeature&version=1.1.0&typename=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&outputFormat=application/json&cql_filter=anotherNullableField IS NOT NULL");
        JSONObject jsonObject = (JSONObject) json;
        JSONArray features = jsonObject.getJSONArray("features");
        assertEquals(2, features.size());
    }

    @Test
    public void testGetStationFeaturesWithFilterNotNull2() throws Exception {
        JSON json =
                getAsJSON(
                        "wfs?request=GetFeature&version=1.1.0&typename=gs:"
                                + StationsTestSetup.COLLECTION_NAME
                                + "&outputFormat=application/json&cql_filter=anotherNullableField.value IS NOT NULL");
        JSONObject jsonObject = (JSONObject) json;
        JSONArray features = jsonObject.getJSONArray("features");
        assertEquals(1, features.size());
    }

    static void checkStationFeature(JSONObject station) {
        JSONObject properties = station.getJSONObject("properties");
        JSONObject geometry = station.getJSONObject("geometry");
        assertNotNull(geometry);
        assertTrue(geometry.has("type"));
        assertTrue(geometry.has("coordinates"));
        assertNotNull(properties.get("id"));
        assertNotNull(properties.get("name"));
        assertNotNull(properties.get("numericValue"));
        JSONObject contact = properties.getJSONObject("contact");
        assertNotNull(contact);
        assertSkippedGeoJSONProperties(contact);
        assertEquals(1, contact.size());
        JSONArray measurements = properties.getJSONArray("measurements");
        assertNotNull(measurements);
        assertTrue(measurements.size() > 0);
        for (int i = 0; i < measurements.size(); i++) {
            JSONObject measurement = measurements.getJSONObject(i);
            assertSkippedGeoJSONProperties(measurement);
        }
    }

    private static void assertSkippedGeoJSONProperties(JSONObject object) {
        assertFalse(object.has("properties"));
        assertFalse(object.has("geometry"));
        assertFalse(object.has("id"));
        assertFalse(object.has("type"));
    }

    private String readResourceContent(String resourcePath) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = getClass().getResourceAsStream(resourcePath)) {
            IOUtils.copy(input, output);
            return new String(output.toByteArray());
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Error reading resource '%s' content.", resourcePath), exception);
        }
    }
}
