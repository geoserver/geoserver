/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.junit.Test;

public class FlatGeoJSONComplexFeaturesResponseAPITest extends TemplateComplexTestSupport {

    private static final String FLAT_MF_TEMPLATE = "FlatGeoJSONMappedFeature";

    private static final String FLAT_MF_RULE_CQL =
            "requestParam('" + FLAT_MF_TEMPLATE + "')='true'";

    private static final String FLAT_MF_PARAM = "&FlatGeoJSONMappedFeature=true";

    @Override
    public void onSetUp(SystemTestData testData) throws IOException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo mappedFeature = catalog.getFeatureTypeByName("gsml", "MappedFeature");
        String templateMappedFeature = "FlatGeoJSONMappedFeature.json";
        setUpTemplate(
                FLAT_MF_RULE_CQL,
                SupportedFormat.GEOJSON,
                templateMappedFeature,
                FLAT_MF_TEMPLATE,
                ".json",
                "gsml",
                mappedFeature);
    }

    @Test
    public void testGeoJSONResponseOGCAPI() throws Exception {
        String condition = "requestParam('" + FLAT_MF_TEMPLATE + "')='true'";
        String path =
                "ogc/features/v1/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fgeo%2Bjson";
        path += FLAT_MF_PARAM;
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
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fgeo%2Bjson")
                        .append("&filter-lang=cql-text")
                        .append(
                                "&filter= features.properties.gsml:GeologicUnit_gsml:composition.gsml:compositionPart.lithology.name")
                        .append(" = 'name_2' ")
                        .append(FLAT_MF_PARAM);
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
        checkInspireMappedFeature(features.getJSONObject(0));
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONResponseWithCustomSeparator() throws Exception {
        String path =
                "ogc/features/v1/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fgeo%2Bjson";
        path += FLAT_MF_PARAM;
        path += "&separator=.";
        JSONObject result = (JSONObject) getJson(path);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());
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
