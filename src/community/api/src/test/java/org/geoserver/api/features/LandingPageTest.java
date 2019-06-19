/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

package org.geoserver.api.features;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

public class LandingPageTest extends FeaturesTestSupport {

    @Test
    public void testLandingPageNoSlash() throws Exception {
        DocumentContext json = getAsJSONPath("api/features", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("api/features/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("api/features?f=json", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageWorkspaceSpecific() throws Exception {
        DocumentContext json = getAsJSONPath("api/features", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageXML() throws Exception {
        Document dom = getAsDOM("api/features?f=application/xml");
        print(dom);
        // TODO: add actual tests in here
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("api/features?f=application/x-yaml");
        // System.out.println(yaml);
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json,
                "links[?(@.type == 'application/x-yaml' && @.href =~ /.*api\\/features\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/x-yaml' && @.href =~ /.*api\\/features\\/\\?.*/)].rel",
                "alternate",
                "alternate",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("api/features?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/api/features/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/api/features/api?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }

    @Test
    @Ignore // workspace specific services not working yet
    public void testLandingPageHTMLInWorkspace() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("sf/api/features?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/sf/api/features/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/sf/api/features/api?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }

    static void checkJSONLandingPage(DocumentContext json) {
        assertEquals(20, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*api\\/features\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*api\\/features\\/\\?.*/)].rel",
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
                "links[?(@.href =~ /.*api\\/features\\/api.*/)].rel",
                "service",
                "service",
                "service",
                "service",
                "service");
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*api\\/features\\/conformance.*/)].rel",
                "conformance",
                "conformance",
                "conformance",
                "conformance",
                "conformance");
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*api\\/features\\/collections.*/)].rel",
                "data",
                "data",
                "data",
                "data",
                "data");
    }

    static <T> void assertJSONList(DocumentContext json, String path, T... expected) {
        List<T> selfRels = json.read(path);
        assertThat(selfRels, Matchers.containsInAnyOrder(expected));
    }
}
