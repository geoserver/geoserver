/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FilteringFlatGeoJSONComplexFeaturesResponseTest
        extends TemplateJSONComplexTestSupport {

    @Test
    public void testFilteredArraysIndex() throws Exception {
        setUpMappedFeature("FilteringFlatGeoJSONMappedFeature.json");
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application/json");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            if (feature.getString("@id").equals("mf4")) {
                JSONObject props = feature.getJSONObject("properties");
                String id =
                        props.getString(
                                "gsml:GeologicUnit_gsml:composition_"
                                        + "gsml:compositionPart_lithology_1_id");
                assertEquals("cc.2", id);
            }
        }
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.JSON.getFilename();
    }
}
