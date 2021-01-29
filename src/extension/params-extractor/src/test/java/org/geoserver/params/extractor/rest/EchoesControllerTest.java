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
import org.geoserver.params.extractor.EchoParametersDao;
import org.geoserver.util.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class EchoesControllerTest extends ParamsExtractorRestTestSupport {

    @Before
    public void prepareConfiguration() throws IOException {
        GeoServerDataDirectory dd = getDataDirectory();

        // setup sample echoes
        try (OutputStream os = dd.get(EchoParametersDao.getEchoParametersPath()).out();
                InputStream is = getClass().getResourceAsStream("/data/echoParameters4.xml")) {
            IOUtils.copy(is, os);
        }
    }

    @Test
    public void testGetEchoesListXML() throws Exception {
        Document dom = getAsDom("/rest/params-extractor/echoes", "application/xml", 200);
        // print(dom);

        assertXpathExists("/EchoParameters", dom);
        // two echo parameters
        assertXpathEvaluatesTo("2", "count(/EchoParameters/EchoParameter)", dom);
        // check the usual "list of things" structure from the GeoServer REST API (yes, the tag
        // used is "name" everywhere, even when the data structure is using id, see importer)
        assertXpathEvaluatesTo("0", "/EchoParameters/EchoParameter[1]/id", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/params-extractor/echoes/0.xml",
                "/EchoParameters/EchoParameter[1]/atom:link/@href",
                dom);
        assertXpathEvaluatesTo("1", "/EchoParameters/EchoParameter[2]/id", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/params-extractor/echoes/1.xml",
                "/EchoParameters/EchoParameter[2]/atom:link/@href",
                dom);
    }

    @Test
    public void testGetEchoesListJSON() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON("/rest/params-extractor/echoes.json", "application/json", 200);
        JSONObject wrapper = json.getJSONObject("EchoParameters");
        assertNotNull(wrapper);
        JSONArray array = wrapper.getJSONArray("EchoParameter");
        assertEquals(2, array.size());
        assertEquals(0, array.getJSONObject(0).get("id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/params-extractor/echoes/0.json",
                array.getJSONObject(0).get("href"));
        assertEquals(1, array.getJSONObject(1).get("id"));
        assertEquals(
                "http://localhost:8080/geoserver/rest/params-extractor/echoes/1.json",
                array.getJSONObject(1).get("href"));
    }

    @Test
    public void testGetEchoXML() throws Exception {
        Document dom = getAsDom("/rest/params-extractor/echoes/0", "application/xml", 200);
        // print(dom);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/EchoParameter)", dom);
        assertXpathEvaluatesTo("true", "/EchoParameter/@activated", dom);
        assertXpathEvaluatesTo("0", "/EchoParameter/@id", dom);
        assertXpathEvaluatesTo("CQL_FILTER", "/EchoParameter/@parameter", dom);
    }

    @Test
    public void testGetEchoJSON() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON("/rest/params-extractor/echoes/0.json", "application/json", 200);
        print(json);
        JSONObject param = json.getJSONObject("EchoParameter");
        assertEquals(0, param.get("id"));
        assertEquals("CQL_FILTER", param.get("parameter"));
        assertEquals(true, param.get("activated"));
    }

    @Test
    public void testDeleteEcho() throws Exception {
        MockHttpServletResponse response =
                deleteAsServletResponse("/rest/params-extractor/echoes/0");
        assertEquals(200, response.getStatus());

        // checking it was removed
        assertEquals(404, getAsServletResponse("/rest/params-extractor/echoes/0").getStatus());
        // but the other one is still there
        assertEquals(200, getAsServletResponse("/rest/params-extractor/echoes/1").getStatus());
    }

    @Test
    public void testPutEchoXML() throws Exception {
        MockHttpServletResponse response =
                putAsServletResponse(
                        "/rest/params-extractor/echoes/0",
                        "<EchoParameter id=\"0\" parameter=\"abcd\" activated=\"false\"/>",
                        "application/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDom("/rest/params-extractor/echoes/0", "application/xml", 200);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/EchoParameter)", dom);
        assertXpathEvaluatesTo("false", "/EchoParameter/@activated", dom);
        assertXpathEvaluatesTo("0", "/EchoParameter/@id", dom);
        assertXpathEvaluatesTo("abcd", "/EchoParameter/@parameter", dom);
    }

    @Test
    public void testPutEchoJSON() throws Exception {
        String jsonBody =
                "{\"EchoParameter\": {\n"
                        + "  \"id\": 1,\n"
                        + "  \"parameter\": \"test123\",\n"
                        + "  \"activated\": true\n"
                        + "}}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        "/rest/params-extractor/echoes/1", jsonBody, "application/json");
        assertEquals(200, response.getStatus());

        Document dom = getAsDom("/rest/params-extractor/echoes/1", "application/xml", 200);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/EchoParameter)", dom);
        assertXpathEvaluatesTo("true", "/EchoParameter/@activated", dom);
        assertXpathEvaluatesTo("1", "/EchoParameter/@id", dom);
        assertXpathEvaluatesTo("test123", "/EchoParameter/@parameter", dom);
    }

    @Test
    public void testPostEchoXML() throws Exception {
        MockHttpServletResponse response =
                postAsServletResponse(
                        "/rest/params-extractor/echoes",
                        "<EchoParameter parameter=\"zxw\" activated=\"true\"/>",
                        "application/xml");
        checkCreateWithPost(response);
    }

    @Test
    public void testPostEchoJSON() throws Exception {
        String jsonBody =
                "{\"EchoParameter\": {\n"
                        + "  \"parameter\": \"zxw\",\n"
                        + "  \"activated\": true\n"
                        + "}}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        "/rest/params-extractor/echoes", jsonBody, "application/json");
        checkCreateWithPost(response);
    }

    private void checkCreateWithPost(MockHttpServletResponse response) throws Exception {
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        String location = response.getHeader(HttpHeaders.LOCATION);
        Pattern pattern =
                Pattern.compile(
                        "http://localhost:8080/geoserver/rest/params-extractor/echoes/(.+)");
        Matcher matcher = pattern.matcher(location);
        assertTrue(matcher.matches());
        String id = matcher.group(1);

        Document dom = getAsDom("/rest/params-extractor/echoes/" + id, "application/xml", 200);

        // checking it matches the current serialization
        assertXpathEvaluatesTo("1", "count(/EchoParameter)", dom);
        assertXpathEvaluatesTo("true", "/EchoParameter/@activated", dom);
        assertXpathEvaluatesTo(id, "/EchoParameter/@id", dom);
        assertXpathEvaluatesTo("zxw", "/EchoParameter/@parameter", dom);
    }
}
