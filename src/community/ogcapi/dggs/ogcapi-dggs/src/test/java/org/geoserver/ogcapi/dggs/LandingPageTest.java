/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.dggs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
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
        DocumentContext json = getAsJSONPath("ogc/dggs", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs?f=json", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageWorkspaceSpecific() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs", 200);
        checkJSONLandingPage(json);
    }

    @Test
    @Ignore
    public void testLandingPageXML() throws Exception {
        Document dom = getAsDOM("ogc/dggs?f=application/xml");
        print(dom);
        // TODO: add actual tests in here
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("ogc/dggs?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json,
                "links[?(@.type == 'application/x-yaml' && @.href =~ /.*ogc\\/dggs\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/x-yaml' && @.href =~ /.*ogc\\/dggs\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/dggs?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/conformance?f=text%2Fhtml",
                document.select("#htmlConformanceLink").attr("href"));
    }

    @Test
    public void testLandingPageHTMLInWorkspace() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("sf/ogc/dggs?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/dggs/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/dggs/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/dggs/conformance?f=text%2Fhtml",
                document.select("#htmlConformanceLink").attr("href"));
    }

    void checkJSONLandingPage(DocumentContext json) {
        assertEquals(12, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/dggs\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/dggs\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    void checkJSONLandingPageShared(DocumentContext json) {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/dggs\\/openapi.*/)].rel",
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DOC);
        // check API with right API mime type
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0",
                readSingle(
                        json,
                        "links[?(@.type=='"
                                + OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE
                                + "')].href"));
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/dggs\\/conformance.*/)].rel",
                Link.REL_CONFORMANCE_URI,
                Link.REL_CONFORMANCE_URI,
                Link.REL_CONFORMANCE_URI);
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/dggs\\/collections.*/)].rel",
                Link.REL_DATA,
                Link.REL_DATA,
                Link.REL_DATA);
        // check title
        assertEquals("DGGS 1.0 server", json.read("title"));
        // check description
        assertEquals("", json.read("description"));
    }
}
