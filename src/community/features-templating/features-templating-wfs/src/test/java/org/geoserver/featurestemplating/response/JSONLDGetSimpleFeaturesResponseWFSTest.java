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

public class JSONLDGetSimpleFeaturesResponseWFSTest extends JSONLDGetSimpleFeaturesResponseTest {

    @Test
    public void testJsonLdResponse() throws Exception {
        setUpSimple("NamedPlaces.json");
        StringBuilder sb = new StringBuilder("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=cite:NamedPlaces&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 2);
        checkFeature((JSONObject) features.get(0));
    }

    @Test
    public void testJsonLdQueryPointingToExpr() throws Exception {
        setUpSimple("NamedPlaces.json");
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=cite:NamedPlaces&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append("&cql_filter= features.geometry.wkt IS NULL ");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 0);
    }

    @Test
    public void testJsonLdResponseWithFilter() throws Exception {
        setUpSimple("NamedPlaces.json");
        StringBuilder path =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=cite:NamedPlaces")
                        .append("&outputFormat=application%2Fld%2Bjson")
                        .append("&cql_filter= features.id = '118'");
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
}
