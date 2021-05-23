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
public class JSONLDGetComplexFeaturesResponseWFSTest extends JSONLDGetComplexFeaturesResponseTest {

    @Test
    public void testJsonLdResponse() throws Exception {
        setUpMappedFeature();
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
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
    public void testJsonLdResponseWithoutTemplate() throws Exception {
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=ex:SecondParentFeature&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        MockHttpServletResponse response = getAsServletResponse(sb.toString());
        assertTrue(response.getContentAsString().contains("No template found for feature type"));
    }

    @Test
    public void testJsonLdQueryWithGET() throws Exception {
        setUpMappedFeature();
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append(
                                "&cql_filter=features.gsml:GeologicUnit.description = 'Olivine basalt'");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
    }

    @Test
    public void testJsonLdQueryPointingToExpr() throws Exception {
        setUpMappedFeature();
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append("&cql_filter= features.geometry.wkt IS NULL");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        Object context = result.get("@context");
        checkContext(context);
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 0);
    }

    @Test
    public void testJsonLdQueryWithPOST() throws Exception {
        setUpMappedFeature();
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
        Object context = result.get("@context");
        checkContext(context);
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
                                "Failed to validate template for feature type FirstParentFeature. "
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
                                "Failed to validate template for feature type GeologicUnit. "
                                        + "Failing attribute is Key: invalidAttr Value: &amp;quot;gsml:notExisting&amp;quot;"));
    }
}
