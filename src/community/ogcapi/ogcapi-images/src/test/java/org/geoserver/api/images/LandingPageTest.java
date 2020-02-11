/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.api.Link;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;

public class LandingPageTest extends ImagesTestSupport {

    @Test
    public void testServiceDescriptor() {
        Service service = getService("Images", new Version("1.0"));
        assertNotNull(service);
        assertEquals("Images", service.getId());
        assertEquals(new Version("1.0"), service.getVersion());
        assertThat(service.getService(), CoreMatchers.instanceOf(ImagesService.class));
        assertThat(
                service.getOperations(),
                Matchers.containsInAnyOrder(
                        "getApi",
                        "describeCollection",
                        "getCollections",
                        "getLandingPage",
                        "getConformanceDeclaration",
                        "getImages",
                        "getImage",
                        "getAsset",
                        "addImage",
                        "deleteImage",
                        "updateImage"));
    }

    @Test
    public void testLandingPageNoSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/images", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/images/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/images?f=json", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageWorkspaceSpecific() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/images", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("ogc/images?f=application/x-yaml");
        // System.out.println(yaml);
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json,
                "links[?(@.type == 'application/x-yaml' && @.href =~ /.*ogc\\/images\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/x-yaml' && @.href =~ /.*ogc\\/images\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/images?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/images/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/images/api?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }

    @Test
    public void testLandingPageInWrkspace() throws Exception {
        DocumentContext json = getAsJSONPath("gs/ogc/images?f=json", 200);
        // check the URLs are still workspace specific
        assertThat(
                readSingle(json, "links[?(@.rel=='data' && @.type=='application/json')].href"),
                equalTo(
                        "http://localhost:8080/geoserver/gs/ogc/images/collections?f=application%2Fjson"));
    }

    void checkJSONLandingPage(DocumentContext json) {
        assertEquals(12, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/images\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/images\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    void checkJSONLandingPageShared(DocumentContext json) {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/images\\/api.*/)].rel",
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DOC);
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/images\\/conformance.*/)].rel",
                Link.REL_CONFORMANCE,
                Link.REL_CONFORMANCE,
                Link.REL_CONFORMANCE);
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/images\\/collections.*/)].rel",
                "data",
                "data",
                "data");
        // check title
        assertEquals("Image mosaicks discovery and management interface", json.read("title"));
        // check description
        assertEquals("", json.read("description"));
    }
}
