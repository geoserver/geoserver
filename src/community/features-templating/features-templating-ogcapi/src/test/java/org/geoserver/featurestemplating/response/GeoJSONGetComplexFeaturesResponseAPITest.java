/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.junit.Test;

public class GeoJSONGetComplexFeaturesResponseAPITest extends TemplateComplexTestSupport {

    private static final String MF_TEMPLATE_GEO_JSON = "GeoJSONMappedFeature";

    private static final String MF_TEMPLATE_GEO_JSON_RULE_CQL =
            "requestParam('" + MF_TEMPLATE_GEO_JSON + "')='true'";

    private static final String MF_TEMPLATE_PARAM = "&GeoJSONMappedFeature=true";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        String templateMappedFeature = "MappedFeatureGeoJSON.json";
        setUpTemplate(
                MF_TEMPLATE_GEO_JSON_RULE_CQL,
                SupportedFormat.GEOJSON,
                templateMappedFeature,
                MF_TEMPLATE_GEO_JSON,
                ".json",
                "gsml",
                mappedFeature);
    }

    @Test
    public void testGeoJSONResponseOGCAPI() throws Exception {
        String path =
                "ogc/features/v1/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application/json"
                        + MF_TEMPLATE_PARAM;
        JSONObject result = (JSONObject) getJson(path);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            checkMappedFeatureGeoJSON(feature);
        }
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryOGCAPI() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application/json")
                        .append("&filter-lang=cql-text")
                        .append(
                                "&filter= features.gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology.name.value")
                        .append(" = 'name_2' ")
                        .append(MF_TEMPLATE_PARAM);
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
        checkMappedFeatureGeoJSON(features.getJSONObject(0));
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONSingleFeature() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items/mf4?f=application%2Fgeo%2Bjson")
                        .append(MF_TEMPLATE_PARAM);
        JSONObject result = (JSONObject) getJson(sb.toString());
        assertFalse(result.has("features"));
        assertEquals("mf4", result.getString("@id"));
        assertEquals("FeatureName: MURRADUC BASALT", result.getString("name"));
        assertTrue(result.has("@type"));
        assertTrue(result.has("gsml:positionalAccuracy"));
        assertTrue(result.has("gsml:GeologicUnit"));
        assertTrue(result.has("geometry"));
        assertTrue(result.has("links"));
    }
}
