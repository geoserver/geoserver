/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** Tests the REST API for the Proxy Base Ext */
public class ProxyBaseExtensionRestTest extends ProxyBaseExtensionTestSupport {
    @Test
    public void getGetRulesXML() throws Exception {
        Document dom = getAsDom("/rest/proxy-base-ext/rules", "application/xml", 200);
        print(dom);

        assertXpathExists("/ProxyBaseExtensionRules", dom);
        // three rules
        assertXpathEvaluatesTo("3", "count(/ProxyBaseExtensionRules/ProxyBaseExtensionRule)", dom);
    }

    @Test
    public void getGetRulesJSON() throws Exception {
        JSONObject json =
                (JSONObject) getAsJSON("/rest/proxy-base-ext/rules.json", "application/json", 200);
        JSONObject wrapper = json.getJSONObject("ProxyBaseExtensionRules");
        assertNotNull(wrapper);
        JSONArray array = wrapper.getJSONArray("ProxyBaseExtensionRule");
        assertEquals(3, array.size());
        assertEquals(0, array.getJSONObject(0).get("id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/proxy-base-ext/rules/0.json",
                array.getJSONObject(0).get("href"));
        assertEquals(2, array.getJSONObject(1).get("id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/proxy-base-ext/rules/2.json",
                array.getJSONObject(1).get("href"));
        assertEquals(1, array.getJSONObject(2).get("id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/proxy-base-ext/rules/1.json",
                array.getJSONObject(2).get("href"));
    }

    @Test
    public void testDeleteRule() throws Exception {
        MockHttpServletResponse response = deleteAsServletResponse("/rest/proxy-base-ext/rules/0");
        assertEquals(200, response.getStatus());

        // checking it was removed
        assertEquals(404, getAsServletResponse("/rest/proxy-base-ext/rules/0").getStatus());
        // but the other ones are still there
        assertEquals(200, getAsServletResponse("/rest/proxy-base-ext/rules/1").getStatus());
        assertEquals(200, getAsServletResponse("/rest/proxy-base-ext/rules/2").getStatus());
    }

    @Test
    public void testPutRuleXML() throws Exception {
        String ruleXML =
                "<ProxyBaseExtensionRule id=\"1\"\n"
                        + "          activated=\"true\"\n"
                        + "          position=\"5\"\n"
                        + "          matcher=\"foobar\"\n"
                        + "          transformer=\"abc='$2'\"/>";
        MockHttpServletResponse response =
                putAsServletResponse("/rest/proxy-base-ext/rules/1", ruleXML, "application/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDom("/rest/proxy-base-ext/rules/1", "application/xml", 200);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/ProxyBaseExtensionRule)", dom);
        assertXpathEvaluatesTo("1", "/ProxyBaseExtensionRule/@id", dom);
        assertXpathEvaluatesTo("5", "/ProxyBaseExtensionRule/@position", dom);
        assertXpathEvaluatesTo("foobar", "/ProxyBaseExtensionRule/@matcher", dom);
        assertXpathEvaluatesTo("abc='$2'", "/ProxyBaseExtensionRule/@transformer", dom);
    }

    @Test
    public void testPutRuleJson() throws Exception {
        String ruleJSON =
                "{\"ProxyBaseExtensionRule\": {\n"
                        + "  \"id\": 0,\n"
                        + "  \"activated\": \"true\",\n"
                        + "  \"position\": 5,\n"
                        + "  \"transformer\": \"abc='$2'\",\n"
                        + "  \"matcher\": \"foobar\"\n"
                        + "}}";
        MockHttpServletResponse response =
                putAsServletResponse("/rest/proxy-base-ext/rules/0", ruleJSON, "application/json");
        assertEquals(200, response.getStatus());

        JSONObject json =
                (JSONObject)
                        getAsJSON("/rest/proxy-base-ext/rules/0.json", "application/json", 200);

        // checking it matches the current serialization
        JSONObject param = json.getJSONObject("ProxyBaseExtensionRule");
        assertEquals(0, param.get("id"));
        assertEquals(true, param.get("activated"));
        assertEquals(5, param.get("position"));
        assertEquals("foobar", param.get("matcher"));
        assertEquals("abc='$2'", param.get("transformer"));
    }

    @Test
    public void testPostRuleXML() throws Exception {
        String ruleXML =
                "<ProxyBaseExtensionRule \n"
                        + "          activated=\"true\"\n"
                        + "          position=\"5\"\n"
                        + "          matcher=\"foobar\"\n"
                        + "          transformer=\"abc='$2'\"/>";
        MockHttpServletResponse response =
                postAsServletResponse("/rest/proxy-base-ext/rules", ruleXML, "application/xml");
        checkCreateWithPost(response);
    }

    private void checkCreateWithPost(MockHttpServletResponse response) throws Exception {
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        String location = response.getHeader(HttpHeaders.LOCATION);
        Pattern pattern =
                Pattern.compile("http://localhost:8080/geoserver/rest/proxy-base-ext/rules/(.+)");
        Matcher matcher = pattern.matcher(location);
        assertTrue(matcher.matches());
        String id = matcher.group(1);

        Document dom = getAsDom("/rest/proxy-base-ext/rules/" + id, "application/xml", 200);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/ProxyBaseExtensionRule)", dom);
        assertXpathEvaluatesTo(id, "/ProxyBaseExtensionRule/@id", dom);
        assertXpathEvaluatesTo("5", "/ProxyBaseExtensionRule/@position", dom);
        assertXpathEvaluatesTo("foobar", "/ProxyBaseExtensionRule/@matcher", dom);
        assertXpathEvaluatesTo("abc='$2'", "/ProxyBaseExtensionRule/@transformer", dom);
    }
}
