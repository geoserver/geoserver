/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class LandingPageTest extends OGCApiTestSupport {

    @Test
    public void testServiceDescriptor() {
        Service service = getService("DGGS", new Version("1.0.1"));
        assertNotNull(service);
        assertEquals("DGGS", service.getId());
        assertEquals(new Version("1.0.1"), service.getVersion());
        assertThat(service.getService(), CoreMatchers.instanceOf(DGGSService.class));
        assertThat(
                service.getOperations(),
                Matchers.containsInAnyOrder(
                        "getApi",
                        "getCollections",
                        "getConformanceDeclaration",
                        "getLandingPage",
                        "polygon",
                        "point",
                        "getZones",
                        "getZone",
                        "describeCollection",
                        "getChildren",
                        "getNeighbors",
                        "getParents",
                        "dapaDescription",
                        "dapaVariables",
                        "dapaAreaRetrieve",
                        "dapaAreaSpaceTimeAggregate",
                        "dapaAreaSpaceAggregate",
                        "dapaAreaTimeAggregate",
                        "dapaPositionRetrieve",
                        "dapaPositionTimeAggregate"));
    }

    @Test
    public void testLandingPageNoSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs/v1", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs/v1/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs/v1?f=json", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageWorkspaceSpecific() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs/v1", 200);
        checkJSONLandingPage(json);
    }

    @Test
    @Ignore
    public void testLandingPageXML() throws Exception {
        Document dom = getAsDOM("ogc/dggs/v1?f=application/xml");
        print(dom);
        // TODO: add actual tests in here
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("ogc/dggs/v1?f=application/yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json, "links[?(@.type == 'application/yaml' && @.href =~ /.*ogc\\/dggs\\/v1\\/\\?.*/)].rel", "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/yaml' && @.href =~ /.*ogc\\/dggs\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/dggs/v1?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/v1/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/v1/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/v1/conformance?f=text%2Fhtml",
                document.select("#htmlConformanceLink").attr("href"));
    }

    @Test
    public void testLandingPageHTMLInWorkspace() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("sf/ogc/dggs/v1?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/dggs/v1/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/dggs/v1/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/dggs/v1/conformance?f=text%2Fhtml",
                document.select("#htmlConformanceLink").attr("href"));
    }

    void checkJSONLandingPage(DocumentContext json) {
        assertEquals(12, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json, "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/dggs\\/v1\\/\\?.*/)].rel", "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/dggs\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    void checkJSONLandingPageShared(DocumentContext json) {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/dggs\\/v1\\/openapi.*/)].rel",
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DOC);
        // check API with right API mime type
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/v1/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0",
                readSingle(json, "links[?(@.type=='" + OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE + "')].href"));
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/dggs\\/v1\\/conformance.*/)].rel",
                Link.REL_CONFORMANCE_URI,
                Link.REL_CONFORMANCE_URI,
                Link.REL_CONFORMANCE_URI);
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/dggs\\/v1\\/collections.*/)].rel",
                Link.REL_DATA,
                Link.REL_DATA,
                Link.REL_DATA);
        // check title
        assertEquals("Discrete Global Grid Systems Service", json.read("title"));
        // check description
        assertTrue(((String) json.read("description")).contains("OGCAPI-DGGS"));
    }

    @Test
    public void testDisabledService() throws Exception {
        GeoServer gs = getGeoServer();
        DGGSInfo service = gs.getService(DGGSInfo.class);
        service.setEnabled(false);
        gs.save(service);
        try {
            MockHttpServletResponse httpServletResponse = getAsMockHttpServletResponse("ogc/dggs/v1", 404);
            assertEquals("Service DGGS is disabled", httpServletResponse.getErrorMessage());
        } finally {
            service.setEnabled(true);
            gs.save(service);
        }
    }
}
