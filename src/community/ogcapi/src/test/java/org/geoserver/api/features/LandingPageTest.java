/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

public class LandingPageTest extends FeaturesTestSupport {

    @Test
    public void testServiceDescriptor() {
        Service service = getService("Features", new Version("1.0"));
        assertNotNull(service);
        assertEquals("Features", service.getId());
        assertEquals(new Version("1.0"), service.getVersion());
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
                        "getLandingPage"));
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
                "alternate",
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
                "http://localhost:8080/geoserver/ogc/features/api?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }

    @Test
    @Ignore // workspace specific services not working yet
    public void testLandingPageHTMLInWorkspace() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("sf/ogc/features?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/features/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/features/api?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }

    static void checkJSONLandingPage(DocumentContext json) {
        assertEquals(20, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/features\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/features\\/\\?.*/)].rel",
                "alternate",
                "alternate",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    static void checkJSONLandingPageShared(DocumentContext json) {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/api.*/)].rel",
                "service",
                "service",
                "service",
                "service",
                "service");
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/conformance.*/)].rel",
                "conformance",
                "conformance",
                "conformance",
                "conformance",
                "conformance");
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/collections.*/)].rel",
                "data",
                "data",
                "data",
                "data",
                "data");
        // check title
        assertEquals("Features 1.0 server", json.read("title"));
        // check description
        assertEquals("", json.read("description"));
    }

    static <T> void assertJSONList(DocumentContext json, String path, T... expected) {
        List<T> selfRels = json.read(path);
        assertThat(selfRels, Matchers.containsInAnyOrder(expected));
    }
}
