/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.junit.Test;

public class FlatGeoJSONComplexFeaturesResponseAPITest
        extends FlatGeoJSONComplexFeaturesResponseTest {

    @Test
    public void testGeoJSONResponseOGCAPI() throws Exception {
        String requestParam = "testGeoJSONResponseOGCAPI";
        String condition = "requestParam('" + requestParam + "')='true'";
        setUpTemplate(
                condition,
                SupportedFormat.GEOJSON,
                "FlatGeoJSONMappedFeature.json",
                requestParam,
                ".json",
                "gsml",
                mappedFeature);
        String path =
                "ogc/features/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fgeo%2Bjson";
        path += "&" + requestParam + "=true";
        JSONObject result = (JSONObject) getJson(path);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(4, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            checkInspireMappedFeature(feature);
        }
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryOGCAPI() throws Exception {
        String requestParam = "FlatGeoJSONComplexFeaturesResponseAPITestGeoJSONQueryOGCAPI";
        String condition = "requestParam('" + requestParam + "')='true'";
        setUpTemplate(
                condition,
                SupportedFormat.GEOJSON,
                "FlatGeoJSONMappedFeature.json",
                requestParam,
                ".json",
                "gsml",
                mappedFeature);
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fgeo%2Bjson")
                        .append("&filter-lang=cql-text")
                        .append(
                                "&filter= features.properties.gsml:GeologicUnit_gsml:composition.gsml:compositionPart.lithology.name")
                        .append(" = 'name_2' ")
                        .append("&")
                        .append(requestParam)
                        .append("=true");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
        checkInspireMappedFeature(features.getJSONObject(0));
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONResponseWithCustomSeparator() throws Exception {
        String requestParam = "testGeoJSONResponseWithCustomSeparator";
        String condition = "requestParam('" + requestParam + "')='true'";
        setUpTemplate(
                condition,
                SupportedFormat.GEOJSON,
                "FlatGeoJSONMappedFeature.json",
                requestParam,
                ".json",
                "gsml",
                mappedFeature);
        String path =
                "ogc/features/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fgeo%2Bjson";
        path += "&" + requestParam + "=true";
        path += "&separator=.";
        JSONObject result = (JSONObject) getJson(path);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(4, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            JSONObject properties = feature.getJSONObject("properties");
            Set<String> keys = properties.keySet();
            for (String key : keys) {
                boolean keyWithSep =
                        !key.equals("@id")
                                && !key.equals("description")
                                && !key.equals("gsml:geologicUnitType");
                if (keyWithSep) {
                    String[] arKey = key.split("\\.");
                    assertTrue(arKey.length > 0);
                }
            }
        }
    }
}
