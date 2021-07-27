package org.geoserver.featurestemplating.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateLayerConfig;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class TemplateRuleRestControllerTest extends CatalogRESTTestSupport {

    @Test
    public void testPostGetPutGetDelete() throws Exception {
        TemplateInfo info = new TemplateInfo();
        info.setTemplateName("test-rules");
        info.setExtension("xhtml");
        TemplateInfoDAO.get().saveOrUpdate(info);

        String json =
                "{\n"
                        + "    \"Rule\": {\n"
                        + "        \"priority\": 1,\n"
                        + "        \"templateName\": \"test-rules\",\n"
                        + "        \"outputFormat\": \"HTML\",\n"
                        + "        \"cqlFilter\": \"requestParam('myRequestParam')='true'\",\n"
                        + "    }\n"
                        + "}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/featuretypes/cdf:Fifteen/templaterules",
                        json,
                        MediaType.APPLICATION_JSON_VALUE);
        assertEquals(201, response.getStatus());
        FeatureTypeInfo fifteen = catalog.getFeatureTypeByName("cdf", "Fifteen");
        TemplateLayerConfig templateLayerConfig =
                fifteen.getMetadata()
                        .get(TemplateLayerConfig.METADATA_KEY, TemplateLayerConfig.class);
        TemplateRule rule = new ArrayList<>(templateLayerConfig.getTemplateRules()).get(0);
        String id = rule.getRuleId();
        JSONObject result =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/featuretypes/cdf:Fifteen/templaterules/"
                                        + id
                                        + ".json");
        JSONObject ruleJSON = result.getJSONObject("Rule");
        assertEquals(1, ruleJSON.getInt("priority"));
        assertEquals("test-rules", ruleJSON.getString("templateName"));
        assertEquals("HTML", ruleJSON.getString("outputFormat"));
        assertEquals("requestParam('myRequestParam')='true'", ruleJSON.getString("cqlFilter"));

        String xmlRule =
                " <Rule>\n"
                        + "        <priority>2</priority>\n"
                        + "        <templateName>test-rules</templateName>\n"
                        + "        <outputFormat>HTML</outputFormat>\n"
                        + "        <cqlFilter>requestParam('otherRequestParam')='true'</cqlFilter>\n"
                        + "    </Rule>";

        response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/featuretypes/cdf:Fifteen/templaterules/"
                                + rule.getRuleId(),
                        xmlRule,
                        MediaType.APPLICATION_XML_VALUE);
        assertEquals(201, response.getStatus());

        result =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/featuretypes/cdf:Fifteen/templaterules/"
                                        + id
                                        + ".json");
        ruleJSON = result.getJSONObject("Rule");
        assertEquals(2, ruleJSON.getInt("priority"));
        assertEquals("test-rules", ruleJSON.getString("templateName"));
        assertEquals("HTML", ruleJSON.getString("outputFormat"));
        assertEquals("requestParam('otherRequestParam')='true'", ruleJSON.getString("cqlFilter"));

        response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/featuretypes/cdf:Fifteen/templaterules/"
                                + rule.getRuleId());
        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());

        fifteen = catalog.getFeatureTypeByName("cdf", "Fifteen");
        templateLayerConfig =
                fifteen.getMetadata()
                        .get(TemplateLayerConfig.METADATA_KEY, TemplateLayerConfig.class);
        assertTrue(templateLayerConfig.getTemplateRules().isEmpty());
    }

    @Test
    public void testPostGetPatchGet() throws Exception {
        try {

            TemplateInfo info = new TemplateInfo();
            info.setTemplateName("test-rules2");
            info.setExtension("xhtml");
            TemplateInfoDAO.get().saveOrUpdate(info);

            String json =
                    "{\n"
                            + "    \"Rule\": {\n"
                            + "        \"priority\": 1,\n"
                            + "        \"templateName\": \"test-rules2\",\n"
                            + "        \"outputFormat\": \"HTML\",\n"
                            + "        \"cqlFilter\": \"requestParam('myRequestParam')='true'\",\n"
                            + "    }\n"
                            + "}";
            MockHttpServletResponse response =
                    postAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/featuretypes/cite:Forests/templaterules",
                            json,
                            MediaType.APPLICATION_JSON_VALUE);
            assertEquals(201, response.getStatus());
            FeatureTypeInfo forests = catalog.getFeatureTypeByName("cite", "Forests");
            TemplateLayerConfig templateLayerConfig =
                    forests.getMetadata()
                            .get(TemplateLayerConfig.METADATA_KEY, TemplateLayerConfig.class);
            TemplateRule rule = templateLayerConfig.getTemplateRules().iterator().next();
            String id = rule.getRuleId();
            JSONObject result =
                    (JSONObject)
                            getAsJSON(
                                    RestBaseController.ROOT_PATH
                                            + "/featuretypes/cite:Forests/templaterules/"
                                            + id
                                            + ".json");
            JSONObject ruleJSON = result.getJSONObject("Rule");
            assertEquals(1, ruleJSON.getInt("priority"));
            assertEquals("test-rules2", ruleJSON.getString("templateName"));
            assertEquals("HTML", ruleJSON.getString("outputFormat"));
            assertEquals("requestParam('myRequestParam')='true'", ruleJSON.getString("cqlFilter"));

            String xmlRule =
                    " <Rule>\n"
                            + "        <priority>2</priority>\n"
                            + "        <cqlFilter>requestParam('otherRequestParam')='true'</cqlFilter>\n"
                            + "    </Rule>";

            response =
                    patchAsServletResponse(
                            RestBaseController.ROOT_PATH
                                    + "/featuretypes/cite:Forests/templaterules/"
                                    + rule.getRuleId(),
                            xmlRule,
                            MediaType.APPLICATION_XML_VALUE);
            assertEquals(200, response.getStatus());

            result =
                    (JSONObject)
                            getAsJSON(
                                    RestBaseController.ROOT_PATH
                                            + "/featuretypes/cite:Forests/templaterules/"
                                            + id
                                            + ".json");
            ruleJSON = result.getJSONObject("Rule");
            assertEquals(2, ruleJSON.getInt("priority"));
            assertEquals("test-rules2", ruleJSON.getString("templateName"));
            assertEquals("HTML", ruleJSON.getString("outputFormat"));
            assertEquals(
                    "requestParam('otherRequestParam')='true'", ruleJSON.getString("cqlFilter"));
        } finally {
            cleanup(getCatalog().getFeatureTypeByName("cite", "Forests"));
        }
    }

    @Test
    public void testGetAll() throws Exception {
        try {
            TemplateInfo info = new TemplateInfo();
            info.setTemplateName("test-rules");
            info.setExtension("xhtml");
            TemplateInfoDAO.get().saveOrUpdate(info);

            TemplateInfo info2 = new TemplateInfo();
            info2.setTemplateName("test-rules2");
            info2.setExtension("xml");
            TemplateInfoDAO.get().saveOrUpdate(info2);

            TemplateInfo info3 = new TemplateInfo();
            info3.setTemplateName("test-rules3");
            info3.setExtension("json");
            TemplateInfoDAO.get().saveOrUpdate(info3);

            FeatureTypeInfo places = catalog.getFeatureTypeByName("cite", "NamedPlaces");
            TemplateLayerConfig templateLayerConfig = new TemplateLayerConfig();
            TemplateRule rule = new TemplateRule();
            rule.setTemplateName("test-rule");
            rule.setOutputFormat(SupportedFormat.HTML);
            rule.setTemplateIdentifier(info.getIdentifier());
            templateLayerConfig.addTemplateRule(rule);

            rule = new TemplateRule();
            rule.setTemplateName("test-rule2");
            rule.setOutputFormat(SupportedFormat.GML);
            rule.setTemplateIdentifier(info2.getIdentifier());
            templateLayerConfig.addTemplateRule(rule);

            rule = new TemplateRule();
            rule.setTemplateName("test-rule3");
            rule.setOutputFormat(SupportedFormat.GEOJSON);
            rule.setTemplateIdentifier(info3.getIdentifier());
            templateLayerConfig.addTemplateRule(rule);

            places.getMetadata().put(TemplateLayerConfig.METADATA_KEY, templateLayerConfig);
            getCatalog().save(places);

            JSONObject result =
                    (JSONObject)
                            getAsJSON(
                                    RestBaseController.ROOT_PATH
                                            + "/featuretypes/cite:NamedPlaces/templaterules.json");
            JSONArray array = result.getJSONObject("RulesList").getJSONArray("Rules");
            assertEquals(3, array.size());
            for (int i = 0; i < array.size(); i++) {
                JSONObject ruleJSON = array.getJSONObject(i);
                assertRule(ruleJSON);
            }
        } finally {
            cleanup(catalog.getFeatureTypeByName("cite", "NamedPlaces"));
        }
    }

    private void assertRule(JSONObject rule) {
        assertNotNull(rule.getString("templateName"));
        assertNotNull(rule.getString("templateIdentifier"));
        assertNotNull(rule.getString("outputFormat"));
    }

    private void cleanup(FeatureTypeInfo fti) {
        TemplateLayerConfig layerConfig =
                fti.getMetadata().get(TemplateLayerConfig.METADATA_KEY, TemplateLayerConfig.class);
        if (layerConfig != null) {
            layerConfig.setTemplateRules(new HashSet<>());
            fti.getMetadata().put(TemplateLayerConfig.METADATA_KEY, layerConfig);
            getCatalog().save(fti);
        }
    }
}
