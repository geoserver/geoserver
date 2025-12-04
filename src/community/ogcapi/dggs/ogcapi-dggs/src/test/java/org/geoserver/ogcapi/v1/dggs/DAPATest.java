/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.geotools.dggs.datastore.DGGSStoreFactory;
import org.geotools.feature.NameImpl;
import org.junit.Test;

public class DAPATest extends OGCApiTestSupport {

    private static final String DATASTORE = "h3props";
    private static final String H3_DGGS_STORE_NAME = "h3dggs";
    private static final String H3_FEATURE_TYPE = "h3temps"; // from h3temps.properties

    private static final String LONG_SUFFIX = "Long";
    private static final String DATASTORE_LONG = DATASTORE + LONG_SUFFIX;
    private static final String H3_DGGS_STORE_NAME_LONG = H3_DGGS_STORE_NAME + LONG_SUFFIX;
    private static final String H3_FEATURE_TYPE_LONG = H3_FEATURE_TYPE + LONG_SUFFIX;

    public static final double DELTA = 1E-6;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // Setup a h3 store that has the zone identifier column as String
        setupDelegatePropertyStore(DATASTORE, H3_FEATURE_TYPE + ".properties", testData);
        setUpH3DggsStore(H3_DGGS_STORE_NAME, DATASTORE, "h3indexstr");
        setUpH3DggsLayer(H3_DGGS_STORE_NAME, H3_FEATURE_TYPE);

        // Setup a h3 store that has the zone identifier column as a Number (Long)
        setupDelegatePropertyStore(DATASTORE_LONG, H3_FEATURE_TYPE_LONG + ".properties", testData);
        setUpH3DggsStore(H3_DGGS_STORE_NAME_LONG, DATASTORE_LONG, "h3index");
        setUpH3DggsLayer(H3_DGGS_STORE_NAME_LONG, H3_FEATURE_TYPE_LONG);
    }

    private void setUpH3DggsLayer(String h3DggsStoreName, String h3FeatureType) throws Exception {
        Catalog catalog = getCatalog();
        DataStoreInfo dggsStore = catalog.getDataStoreByName(h3DggsStoreName);
        if (dggsStore == null) {
            throw new IllegalStateException("DGGS store " + h3DggsStoreName + " was not found in the catalog");
        }

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getDefaultWorkspace());
        cb.setStore(dggsStore);
        FeatureTypeInfo ft = cb.buildFeatureType(new NameImpl(h3FeatureType));
        cb.setupBounds(ft);
        DimensionInfoImpl time = new DimensionInfoImpl();
        time.setAttribute("time");
        time.setEnabled(true);
        ft.getMetadata().put(ResourceInfo.TIME, time);
        catalog.add(ft);
        LayerInfo li = cb.buildLayer(ft);
        catalog.add(li);

        // disable feature bounding
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.setFeatureBounding(false);
        gs.save(wfs);
    }

    private void setupDelegatePropertyStore(String delegateStoreName, String propertyFile, SystemTestData testData)
            throws IOException {
        File dataRoot = testData.getDataDirectoryRoot();
        File dir = new File(dataRoot, delegateStoreName);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory " + dir);
        }

        File propsFile = new File(dir, propertyFile);
        getCatalog().getResourceLoader().copyFromClassPath(propertyFile, propsFile, getClass());

        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getDefaultWorkspace();

        DataStoreInfo store = catalog.getFactory().createDataStore();
        store.setName(delegateStoreName);
        store.setWorkspace(ws);
        store.setEnabled(true);
        store.setType("Property");

        Map<String, Serializable> params = store.getConnectionParameters();
        params.put("directory", dir.getAbsolutePath());
        catalog.add(store);
    }

    private void setUpH3DggsStore(String storeName, String delegateDatastoreName, String zoneIdAttribute) {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getDefaultWorkspace();
        DataStoreInfo dggs = catalog.getFactory().createDataStore();
        dggs.setName(storeName);
        dggs.setWorkspace(ws);
        dggs.setEnabled(true);
        dggs.setType("DGGS Datastore");

        Map<String, Serializable> params = dggs.getConnectionParameters();
        // These parameter names will depend on your DGGSDataStoreFactory
        params.put(DGGSStoreFactory.DGGS_FACTORY_ID.key, "H3");
        params.put(DGGSStoreFactory.STORE_NAME.key, delegateDatastoreName);
        params.put(DGGSStoreFactory.ZONE_ID_COLUMN_NAME.key, zoneIdAttribute);
        catalog.add(dggs);
    }

    @Test
    public void testVariables() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/dggs/v1/collections/h3temps/variables", 200);

        // check one field
        assertEquals("Field of type String", readSingle(doc, "variables[?(@.id == 'shape')].description"));
        assertEquals("Field of type Double", readSingle(doc, "variables[?(@.id == 'temperature')].description"));

        // self link
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/v1/collections/h3temps/variables?f=application%2Fjson",
                readSingle(doc, "links[?(@.rel=='self')].href"));
        // collection link
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/v1/collections/h3temps?f=application%2Fjson",
                readSingle(doc, "links[?(@.rel=='collection' && @.type == 'application/json')].href"));
    }

    @Test
    public void testAreaAggregateSpaceTime() throws Exception {
        testAreaAggregateSpaceTime(LONG_SUFFIX);
        testAreaAggregateSpaceTime("");
    }

    private void testAreaAggregateSpaceTime(String zoneIdType) throws Exception {
        DocumentContext doc = getAsJSONPath(
                getBasePath(zoneIdType) + "processes/area:aggregate-space-time?bbox=-100,-100,100,"
                        + "100&datetime=2025-01-01/2025-01-05&functions=min,max&variables=temperature&resolution=2",
                200);
        // geometry is the aggregation one
        JSONArray coordinates = doc.read("features[0].geometry.coordinates", JSONArray.class);
        assertEquals("[[[-100,-100],[100,-100],[100,100],[-100,100],[-100,-100]]]", coordinates.toJSONString());
        // check properties
        JSONObject properties = doc.read("features[0].properties", JSONObject.class);
        // aggregation time
        assertEquals("2025-01-01/2025-01-05", properties.getAsString("phenomenonTime"));
        // the actual results
        assertEquals(7.0, properties.getAsNumber("temperature_min").doubleValue(), DELTA);
        assertEquals(13.1, properties.getAsNumber("temperature_max").doubleValue(), DELTA);
    }

    @Test
    public void testAreaAggregateSpace() throws Exception {
        testAreaAggregateSpace(LONG_SUFFIX);
        testAreaAggregateSpace("");
    }

    private void testAreaAggregateSpace(String zoneIdType) throws Exception {
        DocumentContext doc = getAsJSONPath(
                getBasePath(zoneIdType) + "processes/area:aggregate-space?bbox=-100,-100,100,"
                        + "100&functions=mean&variables=temperature&resolution=3",
                200);
        // geometry is the aggregation one
        JSONArray coordinates = doc.read("features[0].geometry.coordinates", JSONArray.class);
        assertEquals("[[[-100,-100],[100,-100],[100,100],[-100,100],[-100,-100]]]", coordinates.toJSONString());
        // check properties
        JSONObject properties = doc.read("features[0].properties", JSONObject.class);
        // the actual results
        assertEquals(15.7, properties.getAsNumber("temperature_average").doubleValue(), DELTA);

        properties = doc.read("features[1].properties", JSONObject.class);
        assertEquals(16.25, properties.getAsNumber("temperature_average").doubleValue(), DELTA);
    }

    @Test
    public void testAreaAggregateTime() throws Exception {
        testAreaAggregateTime(LONG_SUFFIX);
        testAreaAggregateTime("");
    }

    private void testAreaAggregateTime(String zoneIdType) throws Exception {
        DocumentContext doc = getAsJSONPath(
                getBasePath(zoneIdType) + "processes/area:aggregate-time?bbox=-100,-100,100,"
                        + "100&functions=mean&variables=temperature&resolution=3",
                200);
        // geometry is the aggregation one
        JSONArray coordinates = doc.read("features[0].geometry.coordinates", JSONArray.class);
        assertEquals("[[[-100,-100],[100,-100],[100,100],[-100,100],[-100,-100]]]", coordinates.toJSONString());
        // check properties
        JSONObject properties = doc.read("features[0].properties", JSONObject.class);
        // the actual results
        assertEquals(10.2, properties.getAsNumber("temperature_average").doubleValue(), DELTA);

        properties = doc.read("features[1].properties", JSONObject.class);
        assertEquals(17.2, properties.getAsNumber("temperature_average").doubleValue(), DELTA);
    }

    @Test
    public void testRetrieveInArea() throws Exception {
        testRetrieveInArea(LONG_SUFFIX);
        testRetrieveInArea("");
    }

    private void testRetrieveInArea(String zoneIdType) throws Exception {
        DocumentContext json = getAsJSONPath(
                getBasePath(zoneIdType) + "processes/area:retrieve?bbox=4,35,24,"
                        + "48&datetime=2025-01-01/2025-01-02&resolution=3",
                200);
        List<?> features = json.read("$.features");
        assertThat(features, hasSize(4));

        // Basic feature schema checks
        assertEquals("Feature", json.read("$.features[0].type"));
        // Properties presence
        assertEqualsZoneId(json, "831e84fffffffff", 0, zoneIdType);
        assertEqualsZoneId(json, "831e84fffffffff", 1, zoneIdType);
        assertEqualsZoneId(json, "831e85fffffffff", 2, zoneIdType);
        assertEqualsZoneId(json, "831e85fffffffff", 3, zoneIdType);
        assertEquals("id_r3_t1", json.read("$.features[0].properties.identifier"));
        assertEquals("id_r3_t2", json.read("$.features[1].properties.identifier"));
        assertEquals("id_r3_t1_2", json.read("$.features[2].properties.identifier"));
        assertEquals("id_r3_t2_2", json.read("$.features[3].properties.identifier"));

        assertEquals(
                3,
                json.read("$.features[0].properties.resolution", Integer.class).intValue());
    }

    @Test
    public void testRetrievePosition() throws Exception {
        testRetrievePosition(LONG_SUFFIX);
        testRetrievePosition("");
    }

    private void testRetrievePosition(String zoneIdType) throws Exception {
        DocumentContext json = getAsJSONPath(
                getBasePath(zoneIdType)
                        + "processes/position:retrieve?resolution=3&geom=POINT(43.1 12.4)&datetime=2025-01-01",
                200);
        List<?> features = json.read("$.features");
        assertThat(features, hasSize(1));

        // Basic feature schema checks
        assertEquals("Feature", json.read("$.features[0].type"));
        // Properties presence
        assertEqualsZoneId(json, "831e84fffffffff", 0, zoneIdType);
        assertEquals("id_r3_t1", json.read("$.features[0].properties.identifier"));
        assertEquals(
                3,
                json.read("$.features[0].properties.resolution", Integer.class).intValue());
    }

    @Test
    public void testPositionAggregateTime() throws Exception {
        testPositionAggregateTime(LONG_SUFFIX);
        testPositionAggregateTime("");
    }

    private void testPositionAggregateTime(String zoneIdType) throws Exception {
        DocumentContext json = getAsJSONPath(
                getBasePath(zoneIdType)
                        + "processes/position:aggregate-time?resolution=3&geom=POINT(43.1 12.4)&datetime=2025-01-01/2025-01-04",
                200);
        List<?> features = json.read("$.features");
        assertThat(features, hasSize(1));

        // Basic feature schema checks
        assertEquals("Feature", json.read("$.features[0].type"));
        // Properties presence
        assertEqualsZoneId(json, "831e84fffffffff", 0, zoneIdType);
        assertEquals("id_r3_t1", json.read("$.features[0].properties.identifier_min"));
        assertEquals("id_r3_t4", json.read("$.features[0].properties.identifier_max"));
        assertEquals(13.4, json.read("$.features[0].properties.temperature_max", Double.class), DELTA);
        assertEquals(8.9, json.read("$.features[0].properties.temperature_min", Double.class), DELTA);
        assertEquals(13.4, json.read("$.features[0].properties.temperature_max", Double.class), DELTA);
        assertEquals(
                8,
                json.read("$.features[0].properties.temperature_count", Integer.class)
                        .intValue());
    }

    private void assertEqualsZoneId(DocumentContext json, String zoneId, int i, String zoneIdType) {
        assertEquals(
                getExpectedValue(zoneIdType, zoneId),
                json.read("$.features[" + i + "].properties." + getExpectedAttribute(zoneIdType)));
    }

    private String getBasePath(String zoneIdType) {
        return "ogc/dggs/v1/collections/h3temps" + zoneIdType + "/";
    }

    private String getExpectedAttribute(String zoneIdType) {
        return LONG_SUFFIX.equals(zoneIdType) ? "h3index" : "h3indexstr";
    }

    private Object getExpectedValue(String zoneIdType, String expectedZoneId) {
        return LONG_SUFFIX.equals(zoneIdType) ? Long.parseUnsignedLong(expectedZoneId, 16) : expectedZoneId;
    }
}
