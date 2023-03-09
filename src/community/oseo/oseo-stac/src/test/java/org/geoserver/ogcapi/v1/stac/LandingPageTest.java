/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.geoserver.ogcapi.v1.stac.STACLandingPage.REL_SEARCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import java.io.IOException;
import java.util.List;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.ogcapi.Sortables;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.Service;
import org.geotools.data.Query;
import org.geotools.util.Version;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Test;

public class LandingPageTest extends STACTestSupport {

    @Test
    public void testServiceDescriptor() {
        Service service = getService("STAC", new Version("1.0.0"));
        assertNotNull(service);
        assertEquals("STAC", service.getId());
        assertEquals(new Version("1.0.0"), service.getVersion());
        assertThat(service.getService(), CoreMatchers.instanceOf(STACService.class));
        assertThat(
                service.getOperations(),
                Matchers.containsInAnyOrder(
                        "getApi",
                        "getLandingPage",
                        "getConformanceDeclaration",
                        "getCollections",
                        "getCollection",
                        "getItems",
                        "getItem",
                        "searchGet",
                        "searchPost",
                        "getCollectionQueryables",
                        "getCollectionSortables",
                        "getSearchQueryables",
                        "getSearchSortables"));
    }

    @Test
    public void testLandingPageNoSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageSlash() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1?f=json", 200);
        checkJSONLandingPage(json);
    }

    @Test
    public void testLandingPageYaml() throws Exception {
        String yaml = getAsString("ogc/stac/v1?f=application/x-yaml");
        // System.out.println(yaml);
        DocumentContext json = convertYamlToJsonPath(yaml);
        assertJSONList(
                json,
                "links[?(@.type == 'application/x-yaml' && @.href =~ /.*ogc\\/stac\\/v1\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/x-yaml' && @.href =~ /.*ogc\\/stac\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    @Test
    public void testLandingPageHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/stac/v1?f=html");
        // check a couple of links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections?f=text%2Fhtml",
                document.select("#htmlCollectionsLink").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/conformance?f=text%2Fhtml",
                document.select("#htmlConformanceLink").attr("href"));
    }

    void checkJSONLandingPage(DocumentContext json) throws IOException {
        // check landing page links
        assertJSONList(
                json,
                "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/stac\\/v1\\/\\?.*/)].rel",
                "self");
        assertJSONList(
                json,
                "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/stac\\/v1\\/\\?.*/)].rel",
                "alternate",
                "alternate");
        checkJSONLandingPageShared(json);
    }

    void checkJSONLandingPageShared(DocumentContext json) throws IOException {
        // check API links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/stac\\/v1\\/openapi.*/)].rel",
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DESC,
                Link.REL_SERVICE_DOC);
        // check conformance links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/stac\\/v1\\/conformance.*/)].rel",
                Link.REL_CONFORMANCE,
                Link.REL_CONFORMANCE,
                Link.REL_CONFORMANCE);
        // check collection links
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/stac\\/v1\\/collections\\?.*/)].rel",
                Link.REL_DATA,
                Link.REL_DATA);
        // check search links
        assertJSONList(
                json,
                "links[?(@.method == 'GET' && @.href =~ /.*ogc\\/stac\\/v1\\/search.*/)].rel",
                REL_SEARCH,
                REL_SEARCH);
        assertJSONList(
                json,
                "links[?(@.method == 'POST' && @.href =~ /.*ogc\\/stac\\/v1\\/search.*/)].rel",
                REL_SEARCH,
                REL_SEARCH);
        // pySTAC compatibility hack, needs JSON first
        assertEquals(
                OGCAPIMediaTypes.GEOJSON_VALUE,
                json.read("links[?(@.method == 'GET' && @.rel == 'search')].type", List.class)
                        .get(0));
        assertEquals(
                OGCAPIMediaTypes.GEOJSON_VALUE,
                json.read("links[?(@.method == 'POST' && @.rel == 'search')].type", List.class)
                        .get(0));
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/stac\\/v1\\/queryables.*/)].rel",
                Queryables.REL,
                Queryables.REL);
        assertJSONList(
                json,
                "links[?(@.href =~ /.*ogc\\/stac\\/v1\\/sortables.*/)].rel",
                Sortables.REL,
                Sortables.REL);
        // check title
        assertEquals(STAC_TITLE, json.read("title"));
        // check description
        assertEquals(
                "Provides interoperable access, following ISO/OGC interface guidelines, to Earth Observation metadata.",
                json.read("description"));
        // STAC repeats the conformance declaration in the landing page too
        ConformanceTest.checkConformance(json);
        // version, id and type
        assertEquals(STACService.STAC_VERSION, json.read("stac_version"));
        assertEquals(STACLandingPage.LANDING_PAGE_ID, json.read("id"));
        assertEquals(
                "Provides interoperable access, following ISO/OGC interface guidelines, to Earth Observation metadata.",
                json.read("description"));
        assertEquals("Catalog", json.read("type"));
        // check we get the collections links, and no disabled collections (one of them is)
        OpenSearchAccess osa = getOpenSearchAccess();
        int collectionCount = osa.getCollectionSource().getCount(Query.ALL) - 1;
        assertEquals(
                collectionCount, json.read("links[?(@.rel == 'child')].href", List.class).size());
        assertThat(
                json.read("links[?(@.rel == 'child')].href"),
                Matchers.containsInAnyOrder(
                        "http://localhost:8080/geoserver/ogc/stac/v1/collections/LANDSAT8",
                        "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2",
                        "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL1",
                        "http://localhost:8080/geoserver/ogc/stac/v1/collections/GS_TEST",
                        "http://localhost:8080/geoserver/ogc/stac/v1/collections/ATMTEST",
                        "http://localhost:8080/geoserver/ogc/stac/v1/collections/ATMTEST2"));
    }
}
