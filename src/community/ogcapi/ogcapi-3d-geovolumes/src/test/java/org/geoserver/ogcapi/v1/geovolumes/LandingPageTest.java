/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.ogcapi.Link;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;

public class LandingPageTest extends GeoVolumesTestSupport {

    @Test
    public void testServiceDescriptor() {
        Service service = getService("3D-GeoVolumes", new Version("1.0.0"));
        assertNotNull(service);
        assertEquals("3D-GeoVolumes", service.getId());
        assertEquals(new Version("1.0.0"), service.getVersion());
        assertThat(service.getService(), CoreMatchers.instanceOf(GeoVolumesService.class));
        assertThat(
                service.getOperations(),
                Matchers.containsInAnyOrder(
                        "getLandingPage",
                        "getApi",
                        "getConformanceDeclaration",
                        "describe3DContainers",
                        "describe3DContainer",
                        "getCesiumViewer",
                        "geti3sViewer",
                        "getResource"));
    }

    @Test
    public void testLandingPageNoSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/3dgeovolumes/v1", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/3dgeovolumes/v1/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/3dgeovolumes/v1?f=json", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("ogc/3dgeovolumes/v1?f=application/x-yaml");
        // System.out.println(yaml);
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json,
                "links[?(@.type == 'application/x-yaml' && @.href =~ /.*ogc\\/3dgeovolumes\\/v1\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/x-yaml' && @.href =~ /.*ogc\\/3dgeovolumes\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/3dgeovolumes/v1?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/3dgeovolumes/v1/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/3dgeovolumes/v1/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/3dgeovolumes/v1/conformance?f=text%2Fhtml",
                document.select("#htmlConformanceLink").attr("href"));
    }

    void checkJSONLandingPage(DocumentContext json) {
        assertEquals(12, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/3dgeovolumes\\/v1\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/3dgeovolumes\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    void checkJSONLandingPageShared(DocumentContext json) {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/3dgeovolumes\\/v1\\/openapi.*/)].rel",
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DOC);
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/3dgeovolumes\\/v1\\/conformance.*/)].rel",
                Link.REL_CONFORMANCE_URI,
                Link.REL_CONFORMANCE_URI,
                Link.REL_CONFORMANCE_URI);
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/3dgeovolumes\\/v1\\/collections.*/)].rel",
                Link.REL_DATA_URI,
                Link.REL_DATA_URI,
                Link.REL_DATA_URI);
        // check title
        assertEquals("3D GeoVolumes Service", json.read("title"));
        // check description
        assertTrue(((String) json.read("description")).contains("3D GeoVolumes"));
    }
}
