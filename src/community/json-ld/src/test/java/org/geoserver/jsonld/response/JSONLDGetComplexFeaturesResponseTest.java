/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.response;

import static org.junit.Assert.*;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JSONLDGetComplexFeaturesResponseTest extends JSONLDComplexTestSupport {

    @Test
    public void testJsonLdResponse() throws Exception {
        setUpComplex();
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 4);
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            checkMappedFeatureJSON(feature);
        }
    }

    @Test
    public void testJsonLdResponseWithoutTemplate() throws Exception {
        setUpComplex();
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=ex:FirstParentFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        MockHttpServletResponse response = getAsServletResponse(sb.toString());
        assertTrue(
                response.getContentAsString()
                        .contains("No Json-Ld template found for feature type"));
    }

    @Test
    public void testJsonLdQueryWithGET() throws Exception {
        setUpComplex();
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append(
                                "&cql_filter=features.gsml:GeologicUnit.description = 'Olivine basalt'");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
    }

    @Test
    public void testJsonLdQueryOGCAPI() throws Exception {
        setUpComplex();
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("gsml:MappedFeature")
                        .append("/items?f=application%2Fld%2Bjson")
                        .append("&filter-lang=cql-text")
                        .append(
                                "&filter= features.gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology.name.value")
                        .append(" = 'name_2' ");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
    }

    @Test
    public void testJsonLdQueryPointingToExpr() throws Exception {
        setUpComplex();
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append("&cql_filter= features.geometry.wkt IS NULL");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 0);
    }

    @Test
    public void testJsonLdQueryWithPOST() throws Exception {
        setUpComplex();
        StringBuilder xml =
                new StringBuilder("<wfs:GetFeature ")
                        .append(" service=\"WFS\" ")
                        .append(" outputFormat=\"application/ld+json\" ")
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
        JSONObject result = (JSONObject) postJsonLd(xml.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
    }

    @Test
    public void testInvalidTemplateResponse() throws Exception {
        setUpComplex("FirstParentFeature_invalid.json", "ex", parentFeature);
        StringBuilder sb = new StringBuilder("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=ex:FirstParentFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        MockHttpServletResponse response = getAsServletResponse(sb.toString());
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "Failed to validate json-ld template for feature type FirstParentFeature. "
                                        + "Failing attribute is Key: @id Value: &amp;quot;invalid/id&amp;quot;"));
    }

    @Test
    public void testInvalidTemplateResponse2() throws Exception {
        // check that validation fails for an invalid attribute down in the template
        // the failing attribute also point to a previous context attribute (../)
        setUpComplex("GeologicUnit_invalid.json", geologicUnit);
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:GeologicUnit&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        MockHttpServletResponse resp = getAsServletResponse(sb.toString());
        assertTrue(
                resp.getContentAsString()
                        .contains(
                                "Failed to validate json-ld template for feature type GeologicUnit. "
                                        + "Failing attribute is Key: invalidAttr Value: &amp;quot;gsml:notExisting&amp;quot;"));
    }

    private void checkMappedFeatureJSON(JSONObject feature) {
        assertNotNull(feature);
        assertNotNull(feature.getString("@id"));
        JSONObject geom = (JSONObject) feature.get("geometry");
        assertNotNull(geom);
        assertEquals(String.valueOf(geom.get("@type")), "Polygon");
        assertNotNull(geom.get("wkt"));
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
}
