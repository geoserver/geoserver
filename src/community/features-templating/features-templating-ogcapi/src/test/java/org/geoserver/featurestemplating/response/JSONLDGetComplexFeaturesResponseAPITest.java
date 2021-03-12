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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JSONLDGetComplexFeaturesResponseAPITest extends JSONLDGetComplexFeaturesResponseTest {

    @Test
    public void testJsonLdResponseOGCAPI() throws Exception {
        setUpMappedFeature();
        String path =
                "ogc/features/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fld%2Bjson";
        JSONObject result = (JSONObject) getJsonLd(path);
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            checkMappedFeatureJSON(feature);
        }
    }

    @Test
    public void testJsonLdQueryOGCAPI() throws Exception {
        setUpMappedFeature();
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&filter-lang=cql-text")
                        .append(
                                "&filter= features.gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology.name.value")
                        .append(" = 'name_2' ");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
    }

    @Test
    public void testJsonLdQueryEnvSubstitutionOnAttributeName() throws Exception {
        setUpMappedFeature();
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&env=id:envId");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertTrue(feature.has("envId"));
        }
    }

    @Test
    public void testJsonLdQueryEnvSubstitutionOnSource() throws Exception {
        setUpMappedFeature();
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&env=source:NotMappedFeature");
        MockHttpServletResponse response = getAsServletResponse(sb.toString());
        // source changed validation should fail
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "Failed to validate template for feature type MappedFeature. "
                                        + "Failing attribute is Source: gsml:NotMappedFeature"));
    }

    @Test
    public void testJsonLdQueryEnvSubstitutionOnAttribute() throws Exception {
        setUpMappedFeature();
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&env=positionalAccuracyType:CGI_NotNumericValue");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            String typeValue = feature.getJSONObject("gsml:positionalAccuracy").getString("type");
            assertEquals("CGI_NotNumericValue", typeValue);
        }
    }

    @Test
    public void testJsonLdQueryEnvSubstitutionOnXpath() throws Exception {
        setUpMappedFeature();
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&env=previous:@id");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            // mf5 setup is not complete, skip it
            if ("mf5".equals(feature.get("@id"))) continue;
            JSONObject geologicUnit = feature.getJSONObject("gsml:GeologicUnit");
            String geologicUnitId = geologicUnit.getString("@id");
            assertNotNull(geologicUnitId);
            JSONArray composition = geologicUnit.getJSONArray("gsml:composition");
            assertTrue(composition.size() > 0);
            for (int j = 0; j < composition.size(); j++) {
                JSONObject compositionObj = composition.getJSONObject(j);
                assertEquals(geologicUnitId, compositionObj.getString("previousContextValue"));
            }
        }
    }

    @Test
    public void testJsonLdQueryOGCAPIFailsIfNotExisting() throws Exception {
        setUpMappedFeature();
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&filter-lang=cql-text")
                        .append("&filter= features.notexisting")
                        .append(" = 'name_2' ");
        MockHttpServletResponse result = getAsServletResponse(sb.toString());
        assertTrue(
                result.getContentAsString()
                        .contains(
                                "Failed to resolve filter features.notexisting = 'name_2' against the template. "
                                        + "Check the path specified in the filter."));
    }

    @Test
    public void testJsonLdQueryPointingToArray() throws Exception {
        setUpMappedFeature();
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&filter-lang=cql-text")
                        .append("&filter= features.gsml:positionalAccuracy.valueArray1 > 120");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONArray features = result.getJSONArray("features");
        assertEquals(3, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject f = features.getJSONObject(i);
            JSONArray values =
                    f.getJSONObject("gsml:positionalAccuracy").getJSONArray("valueArray");
            assertTrue(values.getInt(0) > 120);
        }
    }

    @Test
    public void testJsonLdContextValidationFails() throws Exception {
        setUpMappedFeature();
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&validation=true");
        MockHttpServletResponse result = getAsServletResponse(sb.toString());
        String strResult = result.getContentAsString();
        assertTrue(
                strResult.contains(
                        "Validation failed. Unable to resolve the following fields against the @context: proportion,previousContextValue,lithology,valueArray,type,value,@codeSpace."));
    }
}
