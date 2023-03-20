/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import java.io.IOException;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateFileManager;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.featurestemplating.configuration.TemplateRuleService;
import org.geoserver.test.AbstractAppSchemaMockData;
import org.geoserver.test.AbstractAppSchemaTestSupport;
import org.geoserver.test.FeatureChainingMockData;
import org.springframework.mock.web.MockHttpServletResponse;

public abstract class TemplateComplexTestSupport extends AbstractAppSchemaTestSupport {

    protected JSON getJsonLd(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(response.getContentType(), "application/ld+json");
        return json(response);
    }

    protected JSON postJsonLd(String path, String xml) throws Exception {
        MockHttpServletResponse response = postAsServletResponse(path, xml);
        assertEquals(response.getContentType(), "application/ld+json");
        return json(response);
    }

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new FeatureChainingMockData();
    }

    protected JSON getJson(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        String contentType = response.getContentType();
        // in case of GEOSJSON response with ogcapi, the output format is not
        // set to MockHttpServlet request, so skipping
        if (contentType != null)
            assertThat(
                    contentType,
                    anyOf(
                            equalTo("application/json"),
                            equalTo("application/geo+json"),
                            equalTo("application/json;charset=UTF-8")));
        return json(response);
    }

    protected JSON postJson(String path, String xml) throws Exception {
        MockHttpServletResponse response = postAsServletResponse(path, xml);
        String contentType = response.getContentType();
        // in case of GEOSJSON response with ogcapi, the output format is not
        // set to MockHttpServlet request, so skipping
        if (contentType != null) assertEquals(contentType, "application/json");
        return json(response);
    }

    protected void checkAdditionalInfo(JSONObject result) {
        assertNotNull(result.get("numberReturned"));
        assertNotNull(result.get("timeStamp"));
        if (result.has("crs")) {
            JSONObject crs = result.getJSONObject("crs");
            JSONObject props = crs.getJSONObject("properties");
            assertNotNull(props);
            assertNotNull(props.getString("name"));
        }
        if (result.has("links")) {
            JSONArray links = result.getJSONArray("links");
            assertTrue(links.size() > 0);
        }
    }

    protected void checkContext(Object context) {
        if (context instanceof JSONArray) {
            int size = ((JSONArray) context).size();
            assertTrue(size > 0);
        }
        if (context instanceof JSONObject) {
            assertFalse(((JSONObject) context).isEmpty());
        }
    }

    protected void setUpTemplate(
            String cqlRuleCondition,
            SupportedFormat outputFormat,
            String templateFileName,
            String templateName,
            String templateExtension,
            String workspace,
            FeatureTypeInfo ft)
            throws IOException {
        setUpTemplate(
                cqlRuleCondition,
                null,
                outputFormat,
                templateFileName,
                templateName,
                templateExtension,
                workspace,
                ft);
    }

    protected void setUpTemplate(
            String cqlRuleCondition,
            String profile,
            SupportedFormat outputFormat,
            String templateFileName,
            String templateName,
            String templateExtension,
            String workspace,
            FeatureTypeInfo ft)
            throws IOException {
        String rawTemplate =
                IOUtils.toString(getClass().getResourceAsStream(templateFileName), Charsets.UTF_8);
        TemplateInfo info = new TemplateInfo();
        info.setExtension(templateExtension);
        info.setTemplateName(templateName);
        info.setWorkspace(workspace);
        info.setFeatureType(ft.getNativeName());
        TemplateInfoDAO.get().saveOrUpdate(info);
        TemplateFileManager.get().saveTemplateFile(info, rawTemplate);
        TemplateRule rule = new TemplateRule();
        rule.setTemplateName(info.getFullName());
        rule.setCqlFilter(cqlRuleCondition);
        rule.setProfileFilter(profile);
        rule.setOutputFormat(outputFormat);
        rule.setTemplateIdentifier(info.getIdentifier());
        TemplateRuleService ruleService = new TemplateRuleService(ft);
        ruleService.saveRule(rule);
    }

    protected void checkMappedFeatureJSON(JSONObject feature) {
        assertNotNull(feature);
        String id = feature.getString("@id");
        assertNotNull(id);
        JSONObject geom = (JSONObject) feature.get("geometry");
        assertNotNull(geom);
        assertEquals(String.valueOf(geom.get("@type")), "Polygon");
        assertNotNull(geom.get("wkt"));
        checkSimpleArrayWithDynamicValues(feature);
        if ("mf5".equals(id)) return; // this feature lacks the whole setup
        JSONObject geologicUnit = feature.getJSONObject("gsml:GeologicUnit");
        String geologicUnitDescr = geologicUnit.getString("description");
        assertNotNull(geologicUnitDescr);
        JSONArray composition = geologicUnit.getJSONArray("gsml:composition");
        assertTrue(composition.size() > 0);
        for (int i = 0; i < composition.size(); i++) {
            JSONObject compositionObj = composition.getJSONObject(i);

            String previousContextEl = compositionObj.getString("previousContextValue");
            // check an ${../xpath} expression to be equal to the one
            // acquired previously
            assertEquals(geologicUnitDescr, previousContextEl);
            JSONArray compositionPart =
                    (JSONArray) ((JSONObject) composition.get(i)).get("gsml:compositionPart");
            assertTrue(compositionPart.size() > 0);
            for (int j = 0; j < compositionPart.size(); j++) {
                JSONObject role = compositionPart.getJSONObject(j).getJSONObject("gsml:role");
                assertNotNull(role);
                JSONObject proportion =
                        compositionPart.getJSONObject(j).getJSONObject("proportion");
                assertNotNull(proportion);
                JSONArray lithology = (JSONArray) compositionPart.getJSONObject(j).get("lithology");
                assertTrue(lithology.size() > 0);
            }
        }
    }

    protected void checkSimpleArrayWithDynamicValues(JSONObject feature) {
        JSONArray arrayWithDynamic =
                feature.getJSONObject("gsml:positionalAccuracy").getJSONArray("valueArray");
        String value = feature.getJSONObject("gsml:positionalAccuracy").getString("value");

        assertEquals(value, arrayWithDynamic.getString(0));
        assertEquals("someStaticVal", arrayWithDynamic.getString(1));
        assertEquals("duplicated value: " + value, arrayWithDynamic.getString(2));
    }

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

    protected void checkMappedFeatureGeoJSON(JSONObject feature) {
        assertNotNull(feature);
        String id = feature.getString("@id");
        assertNotNull(id);
        JSONObject geom = (JSONObject) feature.get("geometry");
        assertNotNull(geom);
        assertEquals(String.valueOf(geom.get("type")), "Polygon");
        assertNotNull(geom.get("coordinates"));
        if ("mf5".equals(id)) return; // mf5 does not have all the structure expected
        JSONObject geologicUnit = feature.getJSONObject("gsml:GeologicUnit");
        JSONArray composition = geologicUnit.getJSONArray("gsml:composition");
        assertTrue(composition.size() > 0);
        for (int i = 0; i < composition.size(); i++) {
            JSONArray compositionPart =
                    (JSONArray) ((JSONObject) composition.get(i)).get("gsml:compositionPart");
            assertTrue(compositionPart.size() > 0);
            for (int j = 0; j < compositionPart.size(); j++) {
                JSONObject role = compositionPart.getJSONObject(j).getJSONObject("gsml:role");
                assertNotNull(role);
                JSONObject proportion =
                        compositionPart.getJSONObject(j).getJSONObject("proportion");
                assertNotNull(proportion);
                JSONArray lithology = (JSONArray) compositionPart.getJSONObject(j).get("lithology");
                assertTrue(lithology.size() > 0);
            }
        }
    }
}
