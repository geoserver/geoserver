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
import org.junit.Ignore;
import org.junit.Test;

public class FlatGeoJSONComplexFeaturesResponseAPITest
        extends FlatGeoJSONComplexFeaturesResponseTest {

    @Test
    public void testGeoJSONResponseOGCAPI() throws Exception {
        setUpMappedFeature("FlatGeoJSONMappedFeature.json");
        String path =
                "ogc/features/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fgeo%2Bjson";
        JSONObject result = (JSONObject) getJson(path);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            checkInspireMappedFeature(feature);
        }
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryOGCAPI() throws Exception {
        setUpMappedFeature("FlatGeoJSONMappedFeature.json");
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fgeo%2Bjson")
                        .append("&filter-lang=cql-text")
                        .append(
                                "&filter= features.properties.gsml:GeologicUnit_gsml:composition.gsml:compositionPart.lithology.name")
                        .append(" = 'name_2' ");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
        checkInspireMappedFeature(features.getJSONObject(0));
        checkAdditionalInfo(result);
    }

    @Test
    @Ignore // does not work! TODO: fix
    public void testGeoJSONResponseWithCustomSeparator() throws Exception {
        setUpComplex("FlatGeologicUnitWithSeparator.json", geologicUnit);
        String path =
                "ogc/features/collections/"
                        + "gsml:GeologicUnit"
                        + "/items?f=application%2Fgeo%2Bjson";
        JSONObject result = (JSONObject) getJson(path);
        print(result);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(3, features.size());
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
