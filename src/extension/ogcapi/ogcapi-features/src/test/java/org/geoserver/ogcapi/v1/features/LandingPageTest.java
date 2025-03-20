/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.FunctionsDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OpenAPIMessageConverter;
import org.geoserver.platform.Service;
import org.geoserver.wfs.WFSInfo;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

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
                        "searchFeatures",
                        "getLandingPage",
                        "getQueryables",
                        "getFunctions",
                        "getJSONFGSchemas"));
    }

    @Test
    public void testLandingPageNoSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1?f=json", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageWorkspaceSpecific() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("ogc/features/v1?f=application/yaml");
        // System.out.println(yaml);
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json,
                "links[?(@.type == 'application/yaml' && @.href =~ /.*ogc\\/features\\/v1\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/yaml' && @.href =~ /.*ogc\\/features\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/features/v1?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/conformance?f=text%2Fhtml",
                document.select("#htmlConformanceLink").attr("href"));
    }

    @Test
    public void testLandingPageHTMLInWorkspace() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("sf/ogc/features/v1?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/features/v1/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/sf/ogc/features/v1/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }

    @Test
    public void testLandingPageHTMLInLayer() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("cite/RoadSegments/ogc/features/v1?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/cite/RoadSegments/ogc/features/v1/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/cite/RoadSegments/ogc/features/v1/openapi?f=text%2Fhtml",
                document.select("#htmlApiLink").attr("href"));
    }

    @Test
    public void testLandingPageDisabled() throws Exception {
        LayerInfo layer = getCatalog().getLayerByName("cite:RoadSegments");
        ResourceInfo resource = layer.getResource();
        try {
            // Now you see me
            org.jsoup.nodes.Document document = getAsJSoup("cite/RoadSegments/ogc/features/v1?f=html");
            assertEquals(
                    "http://localhost:8080/geoserver/cite/RoadSegments/ogc/features/v1/collections?f=text%2Fhtml",
                    document.select("#htmlCollectionsLink").attr("href"));

            // now you don't
            resource.setServiceConfiguration(true);
            resource.setDisabledServices(Collections.singletonList("features"));
            getCatalog().save(resource);
            MockHttpServletResponse response = getAsServletResponse("cite/RoadSegments/ogc/features/v1?f=html");
            assertEquals(404, response.getStatus());
        } finally {
            resource.setServiceConfiguration(false);
            resource.setDisabledServices(Collections.emptyList());
            getCatalog().save(resource);
        }
    }

    void checkJSONLandingPage(DocumentContext json) {
        assertEquals(15, (int) json.read("links.length()", Integer.class));
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/features\\/v1\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/features\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    void checkJSONLandingPageShared(DocumentContext json) {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/v1\\/openapi.*/)].rel",
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DOC);
        // check API with right API mime type
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/openapi?f=application%2Fvnd.oai.openapi%2Bjson%3Bversion%3D3.0",
                readSingle(json, "links[?(@.type=='" + OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE + "')].href"));
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/v1\\/conformance.*/)].rel",
                Link.REL_CONFORMANCE,
                Link.REL_CONFORMANCE,
                Link.REL_CONFORMANCE);
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/v1\\/collections.*/)].rel",
                Link.REL_DATA,
                Link.REL_DATA,
                Link.REL_DATA);

        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/features\\/v1\\/functions.*/)].rel",
                FunctionsDocument.REL,
                FunctionsDocument.REL,
                FunctionsDocument.REL);

        // check title
        assertEquals("Features 1.0 server", json.read("title"));
        // check description
        assertEquals("", json.read("description"));
    }

    @Test
    public void testLandingPageHeaders() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse("ogc/features/v1", 200);
        List<String> link = response.getHeaders("Link");
        assertThat(
                link,
                hasItems(
                        "<http://localhost:8080/geoserver/ogc/features/v1/?f=application%2Fyaml>; rel=\"alternate\"; type=\"application/yaml\"; title=\"This document as application/yaml\"",
                        "<http://localhost:8080/geoserver/ogc/features/v1/?f=application%2Fjson>; rel=\"self\"; type=\"application/json\"; title=\"This document\""));
    }

    @Test
    public void testDisabledService() throws Exception {
        GeoServer gs = getGeoServer();
        WFSInfo service = gs.getService(WFSInfo.class);
        service.setEnabled(false);
        gs.save(service);
        try {
            MockHttpServletResponse httpServletResponse = getAsMockHttpServletResponse("ogc/features/v1", 404);
            assertEquals("Service Features is disabled", httpServletResponse.getErrorMessage());
        } finally {
            service.setEnabled(true);
            gs.save(service);
        }
    }
}
