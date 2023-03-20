/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Test;

public class GeoJSONGetSimpleFeaturesResponseWFSTest extends GeoJSONGetSimpleFeaturesResponseTest {

    @Override
    protected void setUpSimple(String fileName) throws IOException {
        super.setUpSimple(fileName);
        // force a reset since this test is using multiple templates for the same feature type
        getGeoServer().reset();
    }

    @Test
    public void testGeoJSONResponse() throws Exception {
        setUpSimple("NamedPlacesGeoJSON.json");
        StringBuilder sb = new StringBuilder("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=cite:NamedPlaces&outputFormat=");
        sb.append("application/json");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 2);
        for (int i = 0; i < features.size(); i++) {
            checkFeature(features.getJSONObject(i));
        }
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryPointingToExpr() throws Exception {
        setUpSimple("NamedPlacesGeoJSON.json");
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=cite:NamedPlaces&outputFormat=")
                        .append("application/json")
                        .append("&cql_filter= features.name = 'Name: Goose Island' ");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(features.getJSONObject(0).getString("name"), "Name: Goose Island");
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryPointingToSimpleAttribute() throws Exception {
        setUpSimple("NamedPlacesDifferentAttributesGeoJSON.json");
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=cite:NamedPlaces&outputFormat=")
                        .append("application/json")
                        .append("&cql_filter= NAME = 'Goose Island' ");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(features.getJSONObject(0).getString("name_cod"), "Goose Island");
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONResponseWithFilter() throws Exception {
        setUpSimple("NamedPlacesGeoJSON.json");
        StringBuilder path =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=cite:NamedPlaces")
                        .append("&outputFormat=application/json")
                        .append("&cql_filter= features.id = '118'");
        JSONObject result = (JSONObject) getJson(path.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 1);
        JSONObject feature = (JSONObject) features.get(0);
        assertEquals(feature.getString("id"), "118");
        assertNotNull(feature.getString("name"), "Goose Island");
        JSONObject geometry = (JSONObject) feature.get("geometry");
        assertEquals(geometry.getString("type"), "MultiPolygon");
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONResponseDynamicKey() throws Exception {
        setUpSimple("NamedPlacesDynKeyGeoJSON.json");
        String url =
                "wfs?request=GetFeature&version=2.0"
                        + "&TYPENAME=cite:NamedPlaces&outputFormat=application/json"
                        + "&featureId=NamedPlaces.1107531895891";
        JSONObject result = (JSONObject) getJson(url);
        print(result);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(1, features.size());
        JSONObject feature = features.getJSONObject(0);
        assertNotNull(feature.getString("id"));
        assertEquals("Name: Goose Island", feature.getString("Goose Island"));
    }

    @Test
    public void testGeoJSONResponseFilteredDynamicKey() throws Exception {
        // cannot filter on a dynamic key, but at least check the mapping won't fail
        setUpSimple("NamedPlacesDynKeyGeoJSON.json");
        String url =
                "wfs?request=GetFeature&version=2.0"
                        + "&TYPENAME=cite:NamedPlaces&outputFormat=application/json"
                        + "&CQL_FILTER=id=118";
        JSONObject result = (JSONObject) getJson(url);
        print(result);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(1, features.size());
        JSONObject feature = features.getJSONObject(0);
        assertNotNull(feature.getString("id"));
        assertEquals("Name: Goose Island", feature.getString("Goose Island"));
    }
}
