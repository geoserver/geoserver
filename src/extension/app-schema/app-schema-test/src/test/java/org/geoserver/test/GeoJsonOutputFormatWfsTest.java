/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

/** Validates JSON output format for complex features. */
public final class GeoJsonOutputFormatWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected StationsMockData createTestData() {
        // instantiate our custom complex types
        return new MockData();
    }

    /** Helper class that will setup custom complex feature types using the stations data set. */
    private static final class MockData extends StationsMockData {

        @Override
        public void addContent() {
            // add GML 3.1 namespaces
            putNamespace(STATIONS_PREFIX_GML31, STATIONS_URI_GML31);
            putNamespace(MEASUREMENTS_PREFIX_GML31, MEASUREMENTS_URI_GML31);
            // add GML 3.2 namespaces
            putNamespace(STATIONS_PREFIX_GML32, STATIONS_URI_GML32);
            putNamespace(MEASUREMENTS_PREFIX_GML32, MEASUREMENTS_URI_GML32);
            // add GML 3.1 feature types
            addAppSchemaFeatureType(
                    STATIONS_PREFIX_GML31,
                    "gml31",
                    "Station_gml31",
                    "/test-data/stations/geoJson/stations.xml",
                    getGml31StandardParamaters(),
                    "/test-data/stations/geoJson/measurements.xml",
                    "/test-data/stations/geoJson/stations.xsd",
                    "/test-data/stations/geoJson/stations.properties",
                    "/test-data/stations/geoJson/measurements.properties");
            // add GML 3.2 feature types
            addAppSchemaFeatureType(
                    STATIONS_PREFIX_GML32,
                    "gml32",
                    "Station_gml32",
                    "/test-data/stations/geoJson/stations.xml",
                    getGml32StandardParamaters(),
                    "/test-data/stations/geoJson/measurements.xml",
                    "/test-data/stations/geoJson/stations.xsd",
                    "/test-data/stations/geoJson/stations.properties",
                    "/test-data/stations/geoJson/measurements.properties");
            // add borehole
            new Gsml32BoreholeMockData().getNamespaces().forEach((k, v) -> putNamespace(k, v));
            addFeatureType(
                    Gsml32BoreholeMockData.GSMLBH_PREFIX,
                    "Borehole",
                    "Gsml32Borehole.xml",
                    "Gsml32Borehole.properties");
            // tricky, the above registered GSML in a different URI and here we override
            // works, but be on the lookout for issues when modifying the test
            putNamespace(GSML_PREFIX, GSML_URI);
            addFeatureType(GSML_PREFIX, "Borehole", "Borehole.xml", "Borehole.properties");
        }
    }

    @Test
    public void testGetGeoJsonResponseWfs11() throws Exception {
        // execute the WFS 1.1.0 request
        JSON response =
                getAsJSON(
                        "wfs?request=GetFeature&version=1.1.0"
                                + "&typename=st_gml31:Station_gml31&outputFormat=application/json");
        // validate the obtained response
        checkStation1Exists(response);
    }

    @Test
    public void testGetGeoJsonResponseWfs20() throws Exception {
        // execute the WFS 2.0 request
        JSON response =
                getAsJSON(
                        "wfs?request=GetFeature&version=2.0"
                                + "&typenames=st_gml32:Station_gml32&outputFormat=application/json");
        // validate the obtained response
        checkStation1Exists(response);
        // check complex types with simple content that miss their value do not get a datatype
        JSONObject station = getFeaturePropertiesById(response, "st.2");
        assertThat(station, notNullValue());
        JSONObject contact = station.getJSONObject("contact");
        assertThat(contact.size(), is(3));
        assertThat(contact.get("@mail"), is("st2@stations.org"));
        JSONObject phone = contact.getJSONObject("phone");
        assertThat(phone.size(), is(1));
        assertFalse(phone.has("value"));
        assertThat(phone.get("@timeZone"), is(""));
        // check the linked features have been kept separate despite the shared element type
        // A and B have max multiplicity > 1, C is ensured to be single
        JSONObject featureLinkA = station.getJSONArray("featureLinkA").getJSONObject(0);
        assertEquals("http://www.geoserver.org/featureA", featureLinkA.getString("@href"));
        JSONObject featureLinkB = station.getJSONArray("featureLinkB").getJSONObject(0);
        assertEquals("http://www.geoserver.org/featureB", featureLinkB.getString("@href"));
        JSONObject featureLinkC = station.getJSONObject("featureLinkC");
        assertEquals("http://www.geoserver.org/featureC", featureLinkC.getString("@href"));
    }

    @Test
    public void testGetGeoJsonResponseWfs20WithNullGeometryAttribute() throws Exception {
        // tests that with a null geometry value and minOccurs 0 in mappings
        // conf, ComplexGeoJSONWriter doesn't throw npe but encode a geometry:null attribute
        JSON response =
                getAsJSON(
                        "wfs?request=GetFeature&version=2.0"
                                + "&typenames=st_gml32:Station_gml32&outputFormat=application/json");
        // validate the obtained response
        checkStation1Exists(response);
        // get station number 3 which should have null geometry value
        JSONObject station3 = (JSONObject) ((JSONObject) response).getJSONArray("features").get(2);
        JSONObject geometry = station3.getJSONObject("geometry");
        // check we got the geometry key encoded
        assertTrue(station3.containsKey("geometry"));
        // check we got the null geometry value encoded
        assertTrue(geometry.isNullObject());
    }

    /** Helper method that station 1 exists and was correctly encoded in the GeoJSON response. */
    private void checkStation1Exists(JSON geoJson) {
        // get the station from the response
        JSONObject station = getFeaturePropertiesById(geoJson, "st.1");
        assertThat(station, notNullValue());
        // validate the station name
        JSONObject name = station.getJSONObject("name");
        assertThat(name.size(), is(2));
        assertThat(name.get("value"), is("station1"));
        assertThat(name.get("@code"), is("st1"));
        // validate the station contact
        JSONObject contact = station.getJSONObject("contact");
        assertThat(contact.size(), is(3));
        assertThat(contact.get("@mail"), is("st1@stations.org"));
        JSONObject phone = contact.getJSONObject("phone");
        assertThat(phone.size(), is(2));
        assertThat(phone.get("value"), is("95482156"));
        assertThat(phone.get("@timeZone"), is("CET"));
        // check the x-links for measurements exist
        JSONArray measurements = station.getJSONArray("measurements");
        assertThat(measurements.size(), is(2));
        assertThat(
                measurements.getJSONObject(0).getString("@href"),
                containsString("http://www.stations.org/ms."));
        assertThat(
                measurements.getJSONObject(1).getString("@href"),
                containsString("http://www.stations.org/ms."));
    }

    @Test
    public void testSimpleContentTimeEncoding() throws Exception {
        String path = "wfs?request=GetFeature&typename=gsmlbh:Borehole&outputFormat=json";
        JSON json = getAsJSON(path);
        JSONObject properties = getFeaturePropertiesById(json, "borehole.GA.17322");
        assertThat(properties, is(notNullValue()));
        JSONObject timeInstant =
                getNestedObject(
                        properties,
                        "relatedSamplingFeature",
                        "relatedSamplingFeature",
                        "properties",
                        "samplingTime",
                        "TimeInstant");
        // property file uses a java.util.Date, but the database uses a java.sql.Date, hence
        // different encodings
        assertThat(
                timeInstant.getString("timePosition"),
                CoreMatchers.anyOf(is("2014-07-02T00:00:00Z"), is("2014-07-02Z")));
    }

    @Test
    public void testOneDimensionalEncoding() throws Exception {
        String path = "wfs?request=GetFeature&typename=gsmlbh:Borehole&outputFormat=json";
        JSON json = getAsJSON(path);
        JSONObject properties = getFeaturePropertiesById(json, "borehole.GA.17322");
        assertThat(properties, is(notNullValue()));
        JSONObject samplingLocation =
                getNestedObject(
                        properties, "relatedSamplingFeature", "relatedSamplingFeature", "geometry");
        JSONArray coordinates = samplingLocation.getJSONArray("coordinates");
        assertThat(coordinates.size(), is(2));
        JSONArray c1 = coordinates.getJSONArray(0);
        assertThat(c1.size(), is(1));
        assertEquals(57.9, c1.getDouble(0), 0.1);
        JSONArray c2 = coordinates.getJSONArray(1);
        assertThat(c2.size(), is(1));
        assertEquals(66.5, c2.getDouble(0), 0.1);
    }

    @Test
    public void testNestedFeatureEncoding() throws Exception {
        String path = "wfs?request=GetFeature&typename=gsml:Borehole&outputFormat=json";
        JSON json = getAsJSON(path);
        JSONObject properties = getFeaturePropertiesById(json, "BOREHOLE.WTB5");
        assertThat(properties, is(notNullValue()));

        // check the featureType attribute is there
        assertEquals("Borehole", properties.getString("@featureType"));

        // get the nested feature
        JSONObject collar = getNestedObject(properties, "collarLocation");
        assertEquals("BOREHOLE.COLLAR.WTB5", collar.getString("id"));
        assertEquals("Feature", collar.getString("type"));
        JSONObject collarGeometry = collar.getJSONObject("geometry");
        JSONArray coordinates = collarGeometry.getJSONArray("coordinates");
        assertThat(coordinates.size(), is(2));
        assertEquals(-28.4139, coordinates.getDouble(0), 0.1);
        assertEquals(121.142, coordinates.getDouble(1), 0.1);

        JSONObject collarProperties = collar.getJSONObject("properties");
        assertEquals("BoreholeCollar", collarProperties.getString("@featureType"));
        JSONObject indexData = properties.getJSONObject("indexData");
        assertEquals("BoreholeDetails", indexData.getString("@dataType"));
        assertEquals(
                "BoundingShape", indexData.getJSONObject("coredInterval").getString("@dataType"));

        // get the sampled feature, which is a linked one
        JSONArray sampledFeatures = properties.getJSONArray("sampledFeature");
        assertEquals(1, sampledFeatures.size());
        JSONObject sampledFeature = sampledFeatures.getJSONObject(0);
        assertEquals(
                "http://www.opengis.net/def/nil/OGC/0/unknown", sampledFeature.getString("@href"));
        assertEquals(
                "http://www.geosciml.org/geosciml/2.0/doc/GeoSciML/GeologicUnit/GeologicUnit.html",
                sampledFeature.getString("@role"));
        assertEquals("unknown", sampledFeature.getString("@title"));
    }
}
