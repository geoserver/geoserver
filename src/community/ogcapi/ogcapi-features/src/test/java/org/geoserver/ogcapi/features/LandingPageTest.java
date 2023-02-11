/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.features;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class LandingPageTest extends FeaturesTestSupport {

    @Test
    public void testServiceDescriptor() {
        Service service = getService("Features", new Version("1.0.1"));
        assertNotNull(service);
        assertEquals("Features", service.getId());
        assertEquals(new Version("1.0.1"), service.getVersion());
        assertThat(service.getService(), CoreMatchers.instanceOf(FeatureService.class));
        assertThat(
                service.getOperations(),
                Matchers.containsInAnyOrder(
                        "getApi",
                        "describeCollection",
                        "getCollections",
                        "getConformanceDeclaration",
                        "getFeature",
                        "getFeatures",
                        "getLandingPage",
                        "getQueryables",
                        "getFunctions"));
    }

    @Test
    public void testLandingPageNoSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features?f=json", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageWorkspaceSpecific() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features", 200);
        checkJSONLandingPage(json);
    }

    @Test
    @Ignore
    public void testLandingPageXML() throws Exception {
        Document dom = getAsDOM("ogc/features?f=application/xml");
        print(dom);
        // TODO: add actual tests in here
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("ogc/features?f=application/x-yaml");
        // System.out.println(yaml);
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json,
                "links[?(@.type == 'application/x-yaml' && @.href =~ /.*ogc\\/features\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/x-yaml' && @.href =~ /.*ogc\\/features\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/features?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/conformance?f=text%2Fhtml",
                document.select("#htmlConformanceLink").attr("href"));
    }

    @Test
    public void testLandingPageHTMLInWorkspace() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("sf/ogc/features?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/features/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/features/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }

    void checkJSONLandingPage(DocumentContext json) {
        assertEquals(15, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/features\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/features\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    void checkJSONLandingPageShared(DocumentContext json) {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/openapi.*/)].rel",
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DOC);
        // check API with right API mime type
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0",
                readSingle(
                        json,
                        "links[?(@.type=='"
                                + OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE
                                + "')].href"));
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/conformance.*/)].rel",
                Link.REL_CONFORMANCE,
                Link.REL_CONFORMANCE,
                Link.REL_CONFORMANCE);
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/collections.*/)].rel",
                Link.REL_DATA,
                Link.REL_DATA,
                Link.REL_DATA);
        // check title
        assertEquals("Features 1.0 server", json.read("title"));
        // check description
        assertEquals("", json.read("description"));
    }

    @Test
    public void testLandingPageHeaders() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/features", 200);
        List<String> link = response.getHeaders("Link");
        assertThat(
                link,
                hasItems(
                        "<http://localhost:8080/geoserver/ogc/features/?f=application%2Fx-yaml>; rel=\"alternate\"; type=\"application/x-yaml\"; title=\"This document as application/x-yaml\"",
                        "<http://localhost:8080/geoserver/ogc/features/?f=application%2Fjson>; rel=\"self\"; type=\"application/json\"; title=\"This document\""));
    }
}
