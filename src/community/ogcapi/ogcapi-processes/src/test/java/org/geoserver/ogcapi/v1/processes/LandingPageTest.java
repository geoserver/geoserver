/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.platform.Service;
import org.geoserver.wps.WPSInfo;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class LandingPageTest extends OGCApiTestSupport {

    @Test
    public void testServiceDescriptor() {
        Service service = getService("Processes", new Version("1.0.0"));
        assertNotNull(service);
        assertEquals("Processes", service.getId());
        assertEquals(new Version("1.0.0"), service.getVersion());
        assertThat(service.getService(), CoreMatchers.instanceOf(ProcessesService.class));
        assertThat(
                service.getOperations(),
                Matchers.containsInAnyOrder(
                        "getLandingPage",
                        "getApi",
                        "getConformanceDeclaration",
                        "getProcessList",
                        "getProcessDescription",
                        "executeProcessKVP"));
    }

    @Test
    public void testLandingPageNoSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/processes/v1", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/processes/v1/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/processes/v1?f=json", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("ogc/processes/v1?f=application/yaml");
        // System.out.println(yaml);
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json,
                "links[?(@.type == 'application/yaml' && @.href =~ /.*ogc\\/processes\\/v1\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/yaml' && @.href =~ /.*ogc\\/processes\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/processes/v1?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/conformance?f=text%2Fhtml",
                document.select("#htmlConformanceLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes?f=text%2Fhtml",
                document.select("#htmlProcessesLink").attr("href"));
    }

    void checkJSONLandingPage(DocumentContext json) {
        assertEquals(12, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/processes\\/v1\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/processes\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    void checkJSONLandingPageShared(DocumentContext json) {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/processes\\/v1\\/openapi.*/)].rel",
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DOC);
        // check API with right API mime type
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0",
                readSingle(json, "links[?(@.type=='" + OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE + "')].href"));
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/processes\\/v1\\/conformance.*/)].rel",
                Link.REL_CONFORMANCE_URI,
                Link.REL_CONFORMANCE_URI,
                Link.REL_CONFORMANCE_URI);
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/processes\\/v1\\/processes.*/)].rel",
                ProcessesLandingPage.REL_PROCESSES,
                ProcessesLandingPage.REL_PROCESSES,
                ProcessesLandingPage.REL_PROCESSES);

        // check title
        assertEquals("Processes 1.0 server", json.read("title"));
        // check description
        assertEquals("", json.read("description"));
    }

    @Test
    public void testLandingPageHeaders() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/processes/v1", 200);
        List<String> link = response.getHeaders("Link");
        assertThat(
                link,
                hasItems(
                        "<http://localhost:8080/geoserver/ogc/processes/v1/?f=application%2Fyaml>; rel=\"alternate\"; type=\"application/yaml\"; title=\"This document as application/yaml\"",
                        "<http://localhost:8080/geoserver/ogc/processes/v1/?f=application%2Fjson>; rel=\"self\"; type=\"application/json\"; title=\"This document\""));
    }

    @Test
    public void testDisabledService() throws Exception {
        GeoServer gs = getGeoServer();
        WPSInfo service = gs.getService(WPSInfo.class);
        service.setEnabled(false);
        gs.save(service);
        try {
            MockHttpServletResponse httpServletResponse = getAsMockHttpServletResponse("ogc/processes/v1", 404);
            assertEquals("Service Processes is disabled", httpServletResponse.getErrorMessage());
        } finally {
            service.setEnabled(true);
            gs.save(service);
        }
    }
}
