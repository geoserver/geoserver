/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.urlchecks.AbstractURLCheck;
import org.geoserver.security.urlchecks.RegexURLCheck;
import org.geoserver.security.urlchecks.URLCheckDAO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class UrlCheckControllerTest extends SecurityRESTTestSupport {

    private URLCheckDAO urlCheckDao;

    @Before
    public void setUp() throws Exception {

        urlCheckDao = GeoServerExtensions.bean(URLCheckDAO.class, applicationContext);

        RegexURLCheck check1 = new RegexURLCheck("check1", "regex check 1", "check1://.*");
        RegexURLCheck check2 = new RegexURLCheck("check2", "regex check 2", "check2://.*");
        RegexURLCheck check3 = new RegexURLCheck("check3", "regex check 3", "check3://.*");

        check2.setEnabled(false);

        urlCheckDao.saveChecks(List.of(check1, check2, check3));
    }

    @Test
    public void testGetAllAsXml() throws Exception {

        Document dom = getAsDOM(ROOT_PATH + "/urlchecks.xml");

        assertXpathEvaluatesTo("3", "count(//urlCheck)", dom);

        assertXpathEvaluatesTo("check1", "//urlCheck[1]/name", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/urlchecks/check1.xml",
                "//urlCheck[1]/atom:link/@href",
                dom);

        assertXpathEvaluatesTo("check2", "//urlCheck[2]/name", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/urlchecks/check2.xml",
                "//urlCheck[2]/atom:link/@href",
                dom);

        assertXpathEvaluatesTo("check3", "//urlCheck[3]/name", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/urlchecks/check3.xml",
                "//urlCheck[3]/atom:link/@href",
                dom);
    }

    @Test
    public void testGetAllAsJson() throws Exception {

        JSON json = getAsJSON(ROOT_PATH + "/urlchecks.json");

        JSONArray urlChecks =
                ((JSONObject) json).getJSONObject("urlChecks").getJSONArray("urlCheck");

        assertEquals(3, urlChecks.size());

        JSONObject check1 = urlChecks.getJSONObject(0);
        JSONObject check2 = urlChecks.getJSONObject(1);
        JSONObject check3 = urlChecks.getJSONObject(2);

        assertEquals("check1", check1.getString("name"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/urlchecks/check1.json",
                check1.getString("href"));
        assertEquals("check2", check2.getString("name"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/urlchecks/check2.json",
                check2.getString("href"));
        assertEquals("check3", check3.getString("name"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/urlchecks/check3.json",
                check3.getString("href"));
    }

    @Test
    public void testGetAllAsHtml() throws Exception {

        Document dom = getAsDOM(ROOT_PATH + "/urlchecks.html");

        assertXpathEvaluatesTo("3", "count(//html:li)", dom);

        assertXpathEvaluatesTo("check1", "//html:li[1]", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/urlchecks/check1.html",
                "//html:li[1]/html:a/@href",
                dom);

        assertXpathEvaluatesTo("check2", "//html:li[2]", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/urlchecks/check2.html",
                "//html:li[2]/html:a/@href",
                dom);

        assertXpathEvaluatesTo("check3", "//html:li[3]", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/urlchecks/check3.html",
                "//html:li[3]/html:a/@href",
                dom);
    }

    @Test
    public void testGetAsXml() throws Exception {

        Document dom = getAsDOM(ROOT_PATH + "/urlchecks/check1.xml");

        assertXpathEvaluatesTo("1", "count(/regexUrlCheck)", dom);

        assertXpathEvaluatesTo("check1", "/regexUrlCheck/name", dom);
        assertXpathEvaluatesTo("regex check 1", "/regexUrlCheck/description", dom);
        assertXpathEvaluatesTo("true", "/regexUrlCheck/enabled", dom);
        assertXpathEvaluatesTo("check1://.*", "/regexUrlCheck/regex", dom);
    }

    @Test
    public void testGetAsJson() throws Exception {

        JSON json = getAsJSON(ROOT_PATH + "/urlchecks/check2.json");

        JSONObject urlCheck = ((JSONObject) json).getJSONObject("regexUrlCheck");

        assertFalse(urlCheck.isNullObject());

        assertEquals("check2", urlCheck.getString("name"));
        assertEquals("regex check 2", urlCheck.getString("description"));
        assertEquals("false", urlCheck.getString("enabled"));
        assertEquals("check2://.*", urlCheck.getString("regex"));
    }

    @Test
    public void testGetAsHtml() throws Exception {

        Document dom = getAsDOM(ROOT_PATH + "/urlchecks/check3.html");

        assertXpathEvaluatesTo("1", "count(//html:ul)", dom);

        assertXpathEvaluatesTo("Name: check3", "//html:li[1]", dom);
        assertXpathEvaluatesTo("Description: regex check 3", "//html:li[2]", dom);
        assertXpathEvaluatesTo("Configuration: check3://.*", "//html:li[3]", dom);
        assertXpathEvaluatesTo("Enabled: true", "//html:li[4]", dom);
    }

    @Test
    public void testGetUnknown() throws Exception {

        String checkName = "unknown";

        String requestPath = ROOT_PATH + "/urlchecks/" + checkName + ".html";
        MockHttpServletResponse response = getAsServletResponse(requestPath);

        assertEquals(404, response.getStatus());
        assertEquals("No such URL check found: '" + checkName + "'", response.getContentAsString());
    }

    @Test
    public void testPost() throws Exception {

        String checkJson =
                "{"
                        + "    \"regexUrlCheck\": {"
                        + "        \"name\": \"check\","
                        + "        \"description\": \"this is another check\","
                        + "        \"enabled\": \"true\","
                        + "        \"regex\": \"http://example.com/.*\""
                        + "    }"
                        + "}";

        String requestPath = ROOT_PATH + "/urlchecks";
        MockHttpServletResponse response =
                postAsServletResponse(requestPath, checkJson, APPLICATION_JSON_VALUE);

        assertEquals(201, response.getStatus());
        assertEquals("check", response.getContentAsString());
        assertEquals(
                "http://localhost:8080/geoserver/rest/urlchecks/check",
                response.getHeader("location"));

        AbstractURLCheck check = urlCheckDao.getCheckByName("check");
        assertEquals("check", check.getName());
        assertEquals("this is another check", check.getDescription());
        assertTrue(check.isEnabled());
        assertEquals("http://example.com/.*", check.getConfiguration());
    }

    @Test
    public void testPostWhenAlreadyExists() throws Exception {

        String checkJson =
                "{"
                        + "    \"regexUrlCheck\": {"
                        + "        \"name\": \"check\","
                        + "        \"description\": \"this is another check\","
                        + "        \"enabled\": \"true\","
                        + "        \"regex\": \"http://example.com/.*\""
                        + "    }"
                        + "}";

        String requestPath = ROOT_PATH + "/urlchecks";
        postAsServletResponse(requestPath, checkJson, APPLICATION_JSON_VALUE);

        MockHttpServletResponse response =
                postAsServletResponse(requestPath, checkJson, APPLICATION_JSON_VALUE);

        assertEquals(409, response.getStatus());
        assertEquals("URL check 'check' already exists", response.getContentAsString());
    }

    @Test
    public void testPostWithoutName() throws Exception {

        String checkJson =
                "{"
                        + "    \"regexUrlCheck\": {"
                        + "        \"description\": \"this is another check\","
                        + "        \"enabled\": \"true\","
                        + "        \"regex\": \"http://example.com/.*\""
                        + "    }"
                        + "}";

        String requestPath = ROOT_PATH + "/urlchecks";
        MockHttpServletResponse response =
                postAsServletResponse(requestPath, checkJson, APPLICATION_JSON_VALUE);

        assertEquals(400, response.getStatus());
        assertEquals("The URL check name is required", response.getContentAsString());
    }

    @Test
    public void testPostWithoutConfiguration() throws Exception {

        String checkJson =
                "{"
                        + "    \"regexUrlCheck\": {"
                        + "        \"name\": \"check\","
                        + "        \"description\": \"this is another check\","
                        + "        \"enabled\": \"true\""
                        + "    }"
                        + "}";

        String requestPath = ROOT_PATH + "/urlchecks";
        MockHttpServletResponse response =
                postAsServletResponse(requestPath, checkJson, APPLICATION_JSON_VALUE);

        assertEquals(400, response.getStatus());
        assertEquals("The URL check configuration is required", response.getContentAsString());
    }

    @Test
    public void testPostWithoutEnabled() throws Exception {

        String checkJson =
                "{"
                        + "    \"regexUrlCheck\": {"
                        + "        \"name\": \"check\","
                        + "        \"description\": \"this is another check\","
                        + "        \"regex\": \"http://example.com/.*\""
                        + "    }"
                        + "}";

        String requestPath = ROOT_PATH + "/urlchecks";
        MockHttpServletResponse response =
                postAsServletResponse(requestPath, checkJson, APPLICATION_JSON_VALUE);

        assertEquals(201, response.getStatus());
        assertEquals("check", response.getContentAsString());
        assertEquals(
                "http://localhost:8080/geoserver/rest/urlchecks/check",
                response.getHeader("location"));

        AbstractURLCheck check = urlCheckDao.getCheckByName("check");
        assertFalse(check.isEnabled());
    }

    @Test
    public void testPut() throws Exception {

        String editCheckJson =
                "{"
                        + "    \"regexUrlCheck\": {"
                        + "        \"name\": \"new-check\","
                        + "        \"description\": \"new description\","
                        + "        \"enabled\": \"false\","
                        + "        \"regex\": \"new regex\""
                        + "    }"
                        + "}";

        String requestPath = ROOT_PATH + "/urlchecks/check1";
        MockHttpServletResponse response =
                putAsServletResponse(requestPath, editCheckJson, APPLICATION_JSON_VALUE);

        assertEquals(200, response.getStatus());

        AbstractURLCheck check = urlCheckDao.getCheckByName("new-check");
        assertEquals("new-check", check.getName());
        assertEquals("new description", check.getDescription());
        assertFalse(check.isEnabled());
        assertEquals("new regex", check.getConfiguration());
    }

    @Test
    public void testPutUnknown() throws Exception {

        String checkName = "unknown";
        String editCheckJson = "{\"regexUrlCheck\": {}}";

        String requestPath = ROOT_PATH + "/urlchecks/" + checkName;
        MockHttpServletResponse response =
                putAsServletResponse(requestPath, editCheckJson, APPLICATION_JSON_VALUE);

        assertEquals(404, response.getStatus());
        assertEquals(
                "Can't change a non existent URL check (" + checkName + ")",
                response.getContentAsString());
    }

    @Test
    public void testPutWithEmptyConfiguration() throws Exception {

        String editCheckJson = "{\"regexUrlCheck\": {\"regex\": \"\"}}";

        String requestPath = ROOT_PATH + "/urlchecks/check1";
        MockHttpServletResponse response =
                putAsServletResponse(requestPath, editCheckJson, APPLICATION_JSON_VALUE);

        assertEquals(400, response.getStatus());
        assertEquals("The URL check configuration is required", response.getContentAsString());
    }

    @Test
    public void testPutNoChanges() throws Exception {

        String editCheckJson = "{\"regexUrlCheck\": {}}";

        String requestPath = ROOT_PATH + "/urlchecks/check1";
        MockHttpServletResponse response =
                putAsServletResponse(requestPath, editCheckJson, APPLICATION_JSON_VALUE);

        assertEquals(200, response.getStatus());

        AbstractURLCheck check = urlCheckDao.getCheckByName("check1");
        assertEquals("check1", check.getName());
        assertEquals("regex check 1", check.getDescription());
        assertFalse(check.isEnabled());
        assertEquals("check1://.*", check.getConfiguration());
    }

    @Test
    public void testDelete() throws Exception {

        /* needed to avoid UnsupportedOperationException */
        Thread.sleep(1000);

        String requestPath = ROOT_PATH + "/urlchecks/check1";
        MockHttpServletResponse response = deleteAsServletResponse(requestPath);

        assertEquals(200, response.getStatus());

        AbstractURLCheck check = urlCheckDao.getCheckByName("check1");
        assertNull(check);
    }
}
