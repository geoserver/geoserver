/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Test;

public class GeoJSONGetSimpleFeaturesResponseAPITest extends GeoJSONGetSimpleFeaturesResponseTest {

    @Test
    public void testGeoJSONResponseOGCAPI() throws Exception {
        setUpSimple("NamedPlacesGeoJSON.json");
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
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
    public void testGeoJSONResponseOGCAPIWithFilter() throws Exception {
        setUpSimple("NamedPlacesGeoJSON.json");
        StringBuilder path =
                new StringBuilder("ogc/features/v1/collections/")
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
}
