/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.response;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.wfstemplating.configuration.TemplateIdentifier;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FlatGeoJSONComplexFeaturesResponseTest extends TemplateJSONComplexTestSupport {

    @Test
    public void testGeoJSONResponse() throws Exception {
        setUpMappedFeature("FlatGeoJSONMappedFeature.json");
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application/json");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 4);
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            checkInspireMappedFeature(feature);
        }
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONResponseOGCAPI() throws Exception {
        setUpMappedFeature("FlatGeoJSONMappedFeature.json");
        String path =
                "ogc/features/collections/"
                        + "gsml:MappedFeature"
                        + "/items?f=application%2Fgeo%2Bjson";
        JSONObject result = (JSONObject) getJson(path);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 4);
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            checkInspireMappedFeature(feature);
        }
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryWithGET() throws Exception {
        setUpMappedFeature("FlatGeoJSONMappedFeature.json");
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application/json")
                        .append(
                                "&cql_filter=features.gsml:GeologicUnit.description = 'Olivine basalt'");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
        checkInspireMappedFeature(features.getJSONObject(0));
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
                                "&filter= features.gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology.name")
                        .append(" = 'name_2' ");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
        checkInspireMappedFeature(features.getJSONObject(0));
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryPointingToExpr() throws Exception {
        setUpMappedFeature("FlatGeoJSONMappedFeature.json");
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application/json")
                        .append(
                                "&cql_filter= features.properties.name = 'FeatureName: MURRADUC BASALT'");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        JSONObject feature = features.getJSONObject(0);
        JSONObject properties = feature.getJSONObject("properties");
        assertEquals("FeatureName: MURRADUC BASALT", properties.getString("name"));
        checkInspireMappedFeature(feature);
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryWithPOST() throws Exception {
        setUpMappedFeature("FlatGeoJSONMappedFeature.json");
        StringBuilder xml =
                new StringBuilder("<wfs:GetFeature ")
                        .append(" service=\"WFS\" ")
                        .append(" outputFormat=\"application/json\" ")
                        .append(" version=\"1.0.0\" ")
                        .append(" xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML:2.0\" ")
                        .append(" xmlns:wfs=\"http://www.opengis.net/wfs\" ")
                        .append(" xmlns:ogc=\"http://www.opengis.net/ogc\" ")
                        .append(">")
                        .append(" <wfs:Query typeName=\"gsml:MappedFeature\">")
                        .append(" <ogc:Filter><ogc:PropertyIsEqualTo> ")
                        .append(
                                "<ogc:PropertyName>features.gsml:GeologicUnit.description</ogc:PropertyName>")
                        .append("<ogc:Literal>Olivine basalt</ogc:Literal>")
                        .append("</ogc:PropertyIsEqualTo></ogc:Filter></wfs:Query>")
                        .append("</wfs:GetFeature>");
        JSONObject result = (JSONObject) postJson(xml.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
        checkInspireMappedFeature(features.getJSONObject(0));
        checkAdditionalInfo(result);
    }

    private void checkInspireMappedFeature(JSONObject feature) {
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
        assertNotNull(properties.getString("gsml:GeologicUnit.description"));
        assertNotNull(properties.getString("gsml:GeologicUnit.gsml:geologicUnitType"));
        if (id.equals("mf1")) {
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.gsml:role.value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.gsml:role.@codeSpace"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.proportion.@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.proportion.CGI_TermValue.@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.proportion.CGI_TermValue.value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology.name"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology.vocabulary"));
        } else if (id.equals("mf2") || id.equals("mf3")) {
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_1.gsml:compositionPart.gsml:role.value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_1.gsml:compositionPart.gsml:role.@codeSpace"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_1.gsml:compositionPart.proportion.@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_1.gsml:compositionPart.proportion.CGI_TermValue.@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_1.gsml:compositionPart.proportion.CGI_TermValue.value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_1.gsml:compositionPart.lithology.name"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_1.gsml:compositionPart.lithology.vocabulary"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_2.gsml:compositionPart.gsml:role.value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_2.gsml:compositionPart.gsml:role.@codeSpace"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_2.gsml:compositionPart.proportion.@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_2.gsml:compositionPart.proportion.CGI_TermValue.@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_2.gsml:compositionPart.proportion.CGI_TermValue.value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_2.gsml:compositionPart.lithology.name"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition_2.gsml:compositionPart.lithology.vocabulary"));
        } else if (id.equals("mf4")) {
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.gsml:role.value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.gsml:role.@codeSpace"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.proportion.@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.proportion.CGI_TermValue.@dataType"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.proportion.CGI_TermValue.value"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_1.name_1"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_1.name_2"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_1.name_3"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_1.vocabulary"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_2.name"));
            assertNotNull(
                    properties.getString(
                            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_2.vocabulary"));
        }
    }

    @Override
    protected String getTemplateFileName() {
        return TemplateIdentifier.JSON.getFilename();
    }
}
