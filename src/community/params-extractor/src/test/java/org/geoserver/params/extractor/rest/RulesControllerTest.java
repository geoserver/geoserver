/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.params.extractor.RulesDao;
import org.geoserver.util.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class RulesControllerTest extends ParamsExtractorRestTestSupport {

    @Before
    public void prepareConfiguration() throws IOException {
        GeoServerDataDirectory dd = getDataDirectory();

        // setup sample rules
        try (OutputStream os = dd.get(RulesDao.getRulesPath()).out();
                InputStream is = getClass().getResourceAsStream("/data/rules5.xml")) {
            IOUtils.copy(is, os);
        }
    }

    @Test
    public void getGetRulesXML() throws Exception {
        Document dom = getAsDom("/rest/params-extractor/rules", "application/xml", 200);
        print(dom);

        assertXpathExists("/Rules", dom);
        // three rules
        assertXpathEvaluatesTo("3", "count(/Rules/Rule)", dom);
        // check the usual "list of things" structure from the GeoServer REST API (yes, the tag
        // used is "name" everywhere, even when the data structure is using id, see importer)
        assertXpathEvaluatesTo("0", "/Rules/Rule[1]/id", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/params-extractor/rules/0.xml",
                "/Rules/Rule[1]/atom:link/@href",
                dom);
        assertXpathEvaluatesTo("1", "/Rules/Rule[2]/id", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/params-extractor/rules/1.xml",
                "/Rules/Rule[2]/atom:link/@href",
                dom);
        assertXpathEvaluatesTo("2", "/Rules/Rule[3]/id", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/params-extractor/rules/2.xml",
                "/Rules/Rule[3]/atom:link/@href",
                dom);
    }

    @Test
    public void getGetRulesJSON() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON("/rest/params-extractor/rules.json", "application/json", 200);
        JSONObject wrapper = json.getJSONObject("Rules");
        assertNotNull(wrapper);
        JSONArray array = wrapper.getJSONArray("Rule");
        assertEquals(3, array.size());
        assertEquals(0, array.getJSONObject(0).get("id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/params-extractor/rules/0.json",
                array.getJSONObject(0).get("href"));
        assertEquals(1, array.getJSONObject(1).get("id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/params-extractor/rules/1.json",
                array.getJSONObject(1).get("href"));
        assertEquals(2, array.getJSONObject(2).get("id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/params-extractor/rules/2.json",
                array.getJSONObject(2).get("href"));
    }

    @Test
    public void testGetRule0XML() throws Exception {
        Document dom = getAsDom("/rest/params-extractor/rules/0", "application/xml", 200);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/Rule)", dom);
        assertXpathEvaluatesTo("0", "/Rule/@id", dom);
        assertXpathEvaluatesTo("cql_filter", "/Rule/@parameter", dom);
        assertXpathEvaluatesTo("1", "/Rule/@remove", dom);
        assertXpathEvaluatesTo("seq='$2'", "/Rule/@transform", dom);
    }

    @Test
    public void testGetRule1XML() throws Exception {
        Document dom = getAsDom("/rest/params-extractor/rules/1", "application/xml", 200);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/Rule)", dom);
        assertXpathEvaluatesTo("1", "/Rule/@id", dom);
        assertXpathEvaluatesTo("^.*?(/([^/]+?))/[^/]+$", "/Rule/@match", dom);
        assertXpathEvaluatesTo("cql_filter", "/Rule/@parameter", dom);
        assertXpathEvaluatesTo("2", "/Rule/@remove", dom);
        assertXpathEvaluatesTo("seq='$2'", "/Rule/@transform", dom);
    }

    @Test
    public void testGetRule2XML() throws Exception {
        Document dom = getAsDom("/rest/params-extractor/rules/2", "application/xml", 200);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/Rule)", dom);
        assertXpathEvaluatesTo("2", "/Rule/@id", dom);
        assertXpathEvaluatesTo("4", "/Rule/@position", dom);
        assertXpathEvaluatesTo("cql_filter", "/Rule/@parameter", dom);
        assertXpathEvaluatesTo("seq='$2'", "/Rule/@transform", dom);
    }

    @Test
    public void testGetRule0JSON() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON("/rest/params-extractor/rules/0.json", "application/json", 200);
        print(json);

        JSONObject param = json.getJSONObject("Rule");
        assertEquals(0, param.get("id"));
        assertEquals(true, param.get("activated"));
        assertEquals(3, param.get("position"));
        assertEquals("cql_filter", param.get("parameter"));
        assertEquals(1, param.get("remove"));
        assertEquals("seq='$2'", param.get("transform"));
    }

    @Test
    public void testGetRule1JSON() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON("/rest/params-extractor/rules/1.json", "application/json", 200);

        JSONObject param = json.getJSONObject("Rule");
        assertEquals(1, param.get("id"));
        assertEquals(true, param.get("activated"));
        assertEquals("^.*?(/([^/]+?))/[^/]+$", param.get("match"));
        assertEquals("cql_filter", param.get("parameter"));
        assertEquals(2, param.get("remove"));
        assertEquals("seq='$2'", param.get("transform"));
    }

    @Test
    public void testGetRule2JSON() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON("/rest/params-extractor/rules/2.json", "application/json", 200);

        JSONObject param = json.getJSONObject("Rule");
        assertEquals(2, param.get("id"));
        assertEquals(true, param.get("activated"));
        assertEquals(4, param.get("position"));
        assertEquals("cql_filter", param.get("parameter"));
        assertEquals("seq='$2'", param.get("transform"));
    }

    @Test
    public void testDeleteRule() throws Exception {
        MockHttpServletResponse response =
                deleteAsServletResponse("/rest/params-extractor/rules/0");
        assertEquals(200, response.getStatus());

        // checking it was removed
        assertEquals(404, getAsServletResponse("/rest/params-extractor/rules/0").getStatus());
        // but the other ones are still there
        assertEquals(200, getAsServletResponse("/rest/params-extractor/rules/1").getStatus());
        assertEquals(200, getAsServletResponse("/rest/params-extractor/rules/2").getStatus());
    }

    @Test
    public void testPutRuleXML() throws Exception {
        String ruleXML =
                "<Rule id=\"0\"\n"
                        + "          position=\"5\"\n"
                        + "          parameter=\"foobar\"\n"
                        + "          remove=\"2\"\n"
                        + "          transform=\"abc='$2'\"/>";
        MockHttpServletResponse response =
                putAsServletResponse("/rest/params-extractor/rules/0", ruleXML, "application/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDom("/rest/params-extractor/rules/0", "application/xml", 200);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/Rule)", dom);
        assertXpathEvaluatesTo("0", "/Rule/@id", dom);
        assertXpathEvaluatesTo("5", "/Rule/@position", dom);
        assertXpathEvaluatesTo("foobar", "/Rule/@parameter", dom);
        assertXpathEvaluatesTo("2", "/Rule/@remove", dom);
        assertXpathEvaluatesTo("abc='$2'", "/Rule/@transform", dom);
    }

    @Test
    public void testPutRuleJson() throws Exception {
        String ruleJSON =
                "{\"Rule\": {\n"
                        + "  \"id\": 0,\n"
                        + "  \"position\": 5,\n"
                        + "  \"parameter\": \"foobar\",\n"
                        + "  \"transform\": \"abc='$2'\",\n"
                        + "  \"remove\": 2\n"
                        + "}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        "/rest/params-extractor/rules/0", ruleJSON, "application/json");
        assertEquals(200, response.getStatus());

        JSONObject json =
                (JSONObject)
                        getAsJSON("/rest/params-extractor/rules/0.json", "application/json", 200);

        // checking it matches the current serialization
        JSONObject param = json.getJSONObject("Rule");
        assertEquals(0, param.get("id"));
        assertEquals(true, param.get("activated"));
        assertEquals(5, param.get("position"));
        assertEquals("foobar", param.get("parameter"));
        assertEquals(2, param.get("remove"));
        assertEquals("abc='$2'", param.get("transform"));
    }

    @Test
    public void testPostRuleXML() throws Exception {
        String ruleXML =
                "<Rule "
                        + "          position=\"5\"\n"
                        + "          parameter=\"foobar\"\n"
                        + "          remove=\"2\"\n"
                        + "          transform=\"abc='$2'\"/>";
        MockHttpServletResponse response =
                postAsServletResponse("/rest/params-extractor/rules", ruleXML, "application/xml");
        checkCreateWithPost(response);
    }

    @Test
    public void testPostEchoJSON() throws Exception {
        String ruleJSON =
                "{\"Rule\": {\n"
                        + "  \"position\": 5,\n"
                        + "  \"parameter\": \"foobar\",\n"
                        + "  \"transform\": \"abc='$2'\",\n"
                        + "  \"remove\": 2\n"
                        + "}}";
        MockHttpServletResponse response =
                postAsServletResponse("/rest/params-extractor/rules", ruleJSON, "application/json");
        checkCreateWithPost(response);
    }

    private void checkCreateWithPost(MockHttpServletResponse response) throws Exception {
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        String location = response.getHeader(HttpHeaders.LOCATION);
        Pattern pattern =
                Pattern.compile("http://localhost:8080/geoserver/rest/params-extractor/rules/(.+)");
        Matcher matcher = pattern.matcher(location);
        assertTrue(matcher.matches());
        String id = matcher.group(1);

        Document dom = getAsDom("/rest/params-extractor/rules/" + id, "application/xml", 200);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/Rule)", dom);
        assertXpathEvaluatesTo(id, "/Rule/@id", dom);
        assertXpathEvaluatesTo("5", "/Rule/@position", dom);
        assertXpathEvaluatesTo("foobar", "/Rule/@parameter", dom);
        assertXpathEvaluatesTo("2", "/Rule/@remove", dom);
        assertXpathEvaluatesTo("abc='$2'", "/Rule/@transform", dom);
    }
}
