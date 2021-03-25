/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.sf.json.JSONObject;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FlatGeoJSONComplexFeaturesResponseTest extends TemplateJSONComplexTestSupport {

    protected void checkInspireMappedFeature(JSONObject feature) {
        assertNotNull(feature);
        String id = feature.getString("@id");
        assertNotNull(id);
        JSONObject geom = (JSONObject) feature.get("geometry");
        assertNotNull(geom);
        assertEquals(String.valueOf(geom.get("type")), "Polygon");
        assertNotNull(geom.get("coordinates"));
        JSONObject properties = feature.getJSONObject("properties");
        assertNotNull(properties);
        assertNotNull(properties.getString("name"));
        assertNotNull(properties.getString("gsml:GeologicUnit_description"));
        assertNotNull(properties.getString("gsml:GeologicUnit_gsml:geologicUnitType"));
        if (id.equals("mf1")) {
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_gsml:role_value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_gsml:role_@codeSpace"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_proportion_@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_proportion_CGI_TermValue_@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_proportion_CGI_TermValue_value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_name"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_vocabulary"));
        } else if (id.equals("mf2") || id.equals("mf3")) {
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_1_gsml:compositionPart_gsml:role_value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_1_gsml:compositionPart_gsml:role_@codeSpace"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_1_gsml:compositionPart_proportion_@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_1_gsml:compositionPart_proportion_CGI_TermValue_@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_1_gsml:compositionPart_proportion_CGI_TermValue_value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_1_gsml:compositionPart_lithology_name"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_1_gsml:compositionPart_lithology_vocabulary"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_2_gsml:compositionPart_gsml:role_value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_2_gsml:compositionPart_gsml:role_@codeSpace"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_2_gsml:compositionPart_proportion_@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_2_gsml:compositionPart_proportion_CGI_TermValue_@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_2_gsml:compositionPart_proportion_CGI_TermValue_value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_2_gsml:compositionPart_lithology_name"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_2_gsml:compositionPart_lithology_vocabulary"));
        } else if (id.equals("mf4")) {
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_gsml:role_value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_gsml:role_@codeSpace"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_proportion_@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_proportion_CGI_TermValue_@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_proportion_CGI_TermValue_value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_1_name_1"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_1_name_2"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_1_name_3"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_1_vocabulary"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_2_name"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_2_vocabulary"));
        }
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.JSON.getFilename();
    }
}
