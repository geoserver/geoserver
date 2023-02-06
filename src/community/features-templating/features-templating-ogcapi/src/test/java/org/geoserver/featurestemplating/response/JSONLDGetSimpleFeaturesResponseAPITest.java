/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Test;

public class JSONLDGetSimpleFeaturesResponseAPITest extends JSONLDGetSimpleFeaturesResponseTest {

    @Test
    public void testJsonLdResponseOGCAPI() throws Exception {
        setUpSimple("NamedPlaces.json");
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("cite:NamedPlaces")
                        .append("/items?f=application%2Fld%2Bjson");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 2);
        checkFeature((JSONObject) features.get(0));
    }

    @Test
    public void testJsonLdResponseOGCAPIWithFilter() throws Exception {
        setUpSimple("NamedPlaces.json");
        StringBuilder path =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("cite:NamedPlaces")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&filter= features.id = '118'")
                        .append("&filter-lang=cql-text");
        JSONObject result = (JSONObject) getJsonLd(path.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 1);
        JSONObject feature = (JSONObject) features.get(0);
        assertEquals(feature.getString("id"), "118");
        assertNotNull(feature.getString("name"), "Goose Island");
        JSONObject geometry = (JSONObject) feature.get("geometry");
        assertEquals(geometry.getString("@type"), "MultiPolygon");
        assertNotNull(geometry.getString("wkt"));
    }

    @Test
    public void testJsonLdResponseOGCAPISingleFeature() throws Exception {
        setUpSimple("NamedPlaces.json");
        StringBuilder path =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("cite:NamedPlaces")
                        .append("/items/NamedPlaces.1107531895891?f=application%2Fld%2Bjson");
        JSONObject result = (JSONObject) getJsonLd(path.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        assertEquals("118", result.getString("id"));
        assertEquals("Goose Island", result.getString("name"));
        assertTrue(result.has("@type"));
        assertTrue(result.has("geometry"));
    }
}
