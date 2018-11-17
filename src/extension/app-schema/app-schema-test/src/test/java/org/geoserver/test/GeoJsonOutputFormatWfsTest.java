/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Objects;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
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
    }

    /** Helper method that station 1 exists and was correctly encoded in the GeoJSON response. */
    private void checkStation1Exists(JSON geoJson) {
        // get the station from the response
        JSONObject station = getStationPropertiesById(geoJson, "st.1");
        assertThat(station, notNullValue());
        // validate the station contact
        JSONArray contact = station.getJSONArray("contact");
        assertThat(contact.size(), is(2));
        JSONArray phone = contact.getJSONObject(0).getJSONArray("phone");
        assertThat(phone.size(), is(2));
        assertThat(phone.getString(0), is("95482156"));
        JSONObject timezone = phone.getJSONObject(1);
        assertThat(timezone.get("timeZone"), is("CET"));
        // check the x-links for measurements exist
        JSONArray measurements = station.getJSONArray("measurements");
        assertThat(measurements.size(), is(2));
        assertThat(
                measurements.getJSONObject(0).getString("href"),
                containsString("http://www.stations.org/ms."));
        assertThat(
                measurements.getJSONObject(1).getString("href"),
                containsString("http://www.stations.org/ms."));
    }

    /**
     * Helper method that just extracts \ looks for a station in the provided GeoJSON response based
     * on its ID.
     */
    private JSONObject getStationPropertiesById(JSON geoJson, String id) {
        assertThat(geoJson, instanceOf(JSONObject.class));
        JSONObject json = (JSONObject) geoJson;
        JSONArray features = json.getJSONArray("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            if (Objects.equals(id, feature.get("id"))) {
                // we found the feature we are looking for
                return feature.getJSONObject("properties");
            }
        }
        // feature matching the provided ID not found
        return null;
    }
}
