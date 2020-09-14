/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.*;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.junit.Test;

public class GeoJSONGetSimpleFeaturesResponseTest extends TemplateJSONSimpleTestSupport {

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
    public void testGeoJSONResponseOGCAPI() throws Exception {
        setUpSimple("NamedPlacesGeoJSON.json");
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("cite:NamedPlaces")
                        .append("/items?f=application/json");
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
    public void testGeoJSONResponseOGCAPIWithFilter() throws Exception {
        setUpSimple("NamedPlacesGeoJSON.json");
        StringBuilder path =
                new StringBuilder("ogc/features/collections/")
                        .append("cite:NamedPlaces")
                        .append("/items?f=application/json")
                        .append("&filter= features.id = '118'")
                        .append("&filter-lang=cql-text");
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

    private void checkFeature(JSONObject feature) {
        assertNotNull(feature.getString("id"));
        assertNotNull(feature.getString("name"));
        JSONObject geometry = (JSONObject) feature.get("geometry");
        assertEquals(geometry.getString("type"), "MultiPolygon");
        JSONArray coordinates = geometry.getJSONArray("coordinates");
        assertNotNull(coordinates);
        assertFalse(coordinates.isEmpty());
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.JSON.getFilename();
    }
}
