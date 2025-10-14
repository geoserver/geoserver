/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.platform.Service;
import org.geoserver.wcs.WCSInfo;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class LandingPageTest extends CoveragesTestSupport {

    @Test
    public void testServiceDescriptor() {
        Service service = getService("Coverages", new Version("1.0.0"));
        assertNotNull(service);
        assertEquals("Coverages", service.getId());
        assertEquals(new Version("1.0.0"), service.getVersion());
        assertThat(service.getService(), CoreMatchers.instanceOf(CoveragesService.class));
        assertThat(
                service.getOperations(),
                Matchers.containsInAnyOrder(
                        "getLandingPage",
                        "getCollections",
                        "describeCollection",
                        "getConformanceDeclaration",
                        "getCoverage",
                        "getApi",
                        "getCoverageDomainSet"));
    }

    @Test
    public void testLandingPageNoSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/coverages/v1", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/coverages/v1/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/coverages/v1?f=json", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageWorkspaceSpecific() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/coverages/v1", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("ogc/coverages/v1?f=application/yaml");
        // System.out.println(yaml);
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json,
                "links[?(@.type == 'application/yaml' && @.href =~ /.*ogc\\/coverages\\/v1\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/yaml' && @.href =~ /.*ogc\\/coverages\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/coverages/v1?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/coverages/v1/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/coverages/v1/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/coverages/v1/conformance?f=text%2Fhtml",
                document.select("#htmlConformanceLink").attr("href"));
    }

    @Test
    public void testLandingPageHTMLInWorkspace() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("sf/ogc/coverages/v1?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/coverages/v1/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/coverages/v1/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }

    void checkJSONLandingPage(DocumentContext json) {
        assertEquals(12, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/coverages\\/v1\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/coverages\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    void checkJSONLandingPageShared(DocumentContext json) {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/coverages\\/v1\\/openapi.*/)].rel",
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DOC);
        // check API with right API mime type
        assertEquals(
                "http://localhost:8080/geoserver/ogc/coverages/v1/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0",
                readSingle(json, "links[?(@.type=='" + OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE + "')].href"));
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/coverages\\/v1\\/conformance.*/)].rel",
                Link.REL_CONFORMANCE,
                Link.REL_CONFORMANCE,
                Link.REL_CONFORMANCE);
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/coverages\\/v1\\/collections.*/)].rel",
                Link.REL_DATA,
                Link.REL_DATA,
                Link.REL_DATA);
        // check title
        assertEquals("Coverages 1.0 server", json.read("title"));
        // check description
        assertEquals("", json.read("description"));
    }

    @Test
    public void testDisabledService() throws Exception {
        GeoServer gs = getGeoServer();
        WCSInfo service = gs.getService(WCSInfo.class);
        service.setEnabled(false);
        gs.save(service);
        try {
            MockHttpServletResponse httpServletResponse = getAsMockHttpServletResponse("ogc/coverages/v1", 404);
            assertEquals("Service Coverages is disabled", httpServletResponse.getErrorMessage());
        } finally {
            service.setEnabled(true);
            gs.save(service);
        }
    }
}
