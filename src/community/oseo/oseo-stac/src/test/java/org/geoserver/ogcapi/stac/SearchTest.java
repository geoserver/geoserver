/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

public class SearchTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        copyTemplate("/mandatoryLinks.json");
        copyTemplate("/items-LANDSAT8.json");
    }

    @Test
    public void testCollectionsGet() throws Exception {
        // two SAS1, one Landsat
        DocumentContext doc = getAsJSONPath("ogc/stac/search?collections=SAS1,LANDSAT8", 200);
        checkCollections(doc, false);
    }

    @Test
    public void testCollectionsPost() throws Exception {
        // two SAS1, one Landsat
        String request =
                "{\n"
                        + "  \"collections\": [\n"
                        + "    \"SAS1\",\n"
                        + "    \"LANDSAT8\"\n"
                        + "  ]\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/search", request, 200);
        checkCollections(doc, true);
    }

    public void checkCollections(DocumentContext doc, boolean post) {
        checkCollectionsSinglePage(doc, 3, containsInAnyOrder("LANDSAT8", "SAS1", "SAS1"));

        // expecting only a self link
        DocumentContext link = readSingleContext(doc, "links");
        assertEquals("self", link.read("rel"));
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, link.read("type"));
        if (post) {
            assertEquals("http://localhost:8080/geoserver/ogc/stac/search", link.read("href"));
            assertTrue(link.read("merge"));
        } else {
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/stac/search?collections=SAS1,LANDSAT8",
                    link.read("href"));
        }
    }

    @Test
    public void testCollectionsCqlGet() throws Exception {
        // two SAS1, one Landsat, but the filter matches constellation to landsat8 only
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/search?collections=SAS1,LANDSAT8"
                                + "&filter=constellation='landsat8'&filter-lang=cql-text",
                        200);
        checkCollectionsSinglePage(doc, 1, containsInAnyOrder("LANDSAT8"));
    }

    @Test
    public void testCollectionsCqlPost() throws Exception {
        // two SAS1, one Landsat, but the filter matches constellation to landsat8 only
        String request =
                "{\n"
                        + "  \"collections\": [\n"
                        + "    \"SAS1\",\n"
                        + "    \"LANDSAT8\"\n"
                        + "  ],\n"
                        + "  \"filter\": \"constellation='landsat8'\",\n"
                        + "  \"filter-lang\": \"cql-text\"\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/search", request, 200);
        checkCollectionsSinglePage(doc, 1, containsInAnyOrder("LANDSAT8"));
    }

    private void checkCollectionsSinglePage(
            DocumentContext doc,
            int matched,
            Matcher<Iterable<? extends String>> collectionsMatcher) {
        assertEquals(Integer.valueOf(matched), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(matched), doc.read("numberReturned"));
        assertThat(doc.read("features[*].collection"), collectionsMatcher);
    }

    @Test
    public void testBBOXFilterGet() throws Exception {
        // two sentinel, one landsat, one sas
        DocumentContext doc = getAsJSONPath("ogc/stac/search?bbox=16,42,17,43", 200);

        checkCollectionsItemsSinglePage(
                doc,
                5,
                containsInAnyOrder("LANDSAT8", "SAS1", "SAS1", "SENTINEL2", "SENTINEL2"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWG_N02.01",
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01",
                        "LS8_TEST.02",
                        "SAS1_20180226102021.01",
                        "SAS1_20180227102021.02"));
    }

    @Test
    public void testBBOXFilterPost() throws Exception {
        // two sentinel, one landsat, one sas
        String request =
                "{\n"
                        + "  \"bbox\": [\n"
                        + "    16,\n"
                        + "    42,\n"
                        + "    17,\n"
                        + "    43\n"
                        + "  ]\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/search", request, 200);

        checkCollectionsItemsSinglePage(
                doc,
                5,
                containsInAnyOrder("LANDSAT8", "SAS1", "SAS1", "SENTINEL2", "SENTINEL2"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWG_N02.01",
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01",
                        "LS8_TEST.02",
                        "SAS1_20180226102021.01",
                        "SAS1_20180227102021.02"));
    }

    public void checkCollectionsItemsSinglePage(
            DocumentContext doc,
            int matched,
            Matcher<Iterable<? extends String>> collectionsMatcher,
            Matcher<Iterable<? extends String>> itemsMatcher) {
        checkCollectionsSinglePage(doc, matched, collectionsMatcher);
        assertThat((List<String>) doc.read("features[*].id"), itemsMatcher);
    }

    @Test
    public void testGeometryIntersectionGet() throws Exception {
        // only SAS and LANDSAT intersecting this point
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/search?intersects={\"type\": \"Point\", \"coordinates\": [16.5, 42.5]}",
                        200);

        checkCollectionsItemsSinglePage(
                doc,
                3,
                containsInAnyOrder("LANDSAT8", "SAS1", "SAS1"),
                containsInAnyOrder(
                        "LS8_TEST.02", "SAS1_20180226102021.01", "SAS1_20180227102021.02"));
    }

    @Test
    public void testAllProductsNoDisabled() throws Exception {
        // only SAS and LANDSAT intersecting this point
        DocumentContext doc = getAsJSONPath("ogc/stac/search?limit=50", 200);

        // had to inline as "CoreMatches.not" has poor generics management
        assertEquals(Integer.valueOf(23), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(23), doc.read("numberReturned"));
        assertThat(doc.read("features[*].collection"), not(hasItem("DISABLED_COLLECTION")));
        assertThat(
                (List<String>) doc.read("features[*].id"),
                not(hasItem("PRODUCT.IN.DISABLED.COLLECTION")));
    }

    @Test
    public void testGeometryIntersectionPost() throws Exception {
        // only SAS and LANDSAT intersecting this point
        String request =
                "{\n"
                        + "  \"intersection\": {\n"
                        + "    \"type\": \"Point\",\n"
                        + "    \"coordinates\": [\n"
                        + "      16.5,\n"
                        + "      42.5\n"
                        + "    ]\n"
                        + "  }\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/search", request, 200);

        checkCollectionsItemsSinglePage(
                doc,
                3,
                containsInAnyOrder("LANDSAT8", "SAS1", "SAS1"),
                containsInAnyOrder(
                        "LS8_TEST.02", "SAS1_20180226102021.01", "SAS1_20180227102021.02"));
    }

    @Test
    public void testPagingLinksFirst() throws Exception {
        String requestPath =
                "ogc/stac/search?collections=SAS1,LANDSAT8"
                        + "&filter=eo:cloud_cover=0&filter-lang=cql-text&limit=1";
        DocumentContext doc = getAsJSONPath(requestPath, 200);
        assertEquals(Integer.valueOf(3), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // two links expected, self and next
        assertEquals(Integer.valueOf(2), doc.read("links.length()"));

        // self link
        DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals("http://localhost:8080/geoserver/" + requestPath, self.read("href"));

        // next link (order should be stable, linked hash maps all around)
        DocumentContext next = readSingleContext(doc, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/search?collections=SAS1%2CLANDSAT8&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=1",
                next.read("href"));
    }

    @Test
    public void testPagingLinksSecond() throws Exception {
        String requestPath =
                "ogc/stac/search?collections=SAS1,LANDSAT8"
                        + "&filter=eo:cloud_cover=0&filter-lang=cql-text&limit=1&startIndex=1";
        DocumentContext doc = getAsJSONPath(requestPath, 200);
        assertEquals(Integer.valueOf(3), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // three links expected, prev, self and next
        assertEquals(Integer.valueOf(3), doc.read("links.length()"));

        // prev link (order should be stable, linked hash maps all around)
        DocumentContext prev = readSingleContext(doc, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/search?collections=SAS1%2CLANDSAT8&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=0",
                prev.read("href"));

        // self link
        DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals("http://localhost:8080/geoserver/" + requestPath, self.read("href"));

        // next link (order should be stable, linked hash maps all around)
        DocumentContext next = readSingleContext(doc, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/search?collections=SAS1%2CLANDSAT8&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=2",
                next.read("href"));
    }

    @Test
    public void testPagingLinksLast() throws Exception {
        String requestPath =
                "ogc/stac/search?collections=SAS1,LANDSAT8"
                        + "&filter=eo:cloud_cover=0&filter-lang=cql-text&limit=1&startIndex=2";
        DocumentContext doc = getAsJSONPath(requestPath, 200);
        assertEquals(Integer.valueOf(3), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // two links expected, prev, self
        assertEquals(Integer.valueOf(2), doc.read("links.length()"));

        // prev link (order should be stable, linked hash maps all around)
        DocumentContext prev = readSingleContext(doc, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/search?collections=SAS1%2CLANDSAT8&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=1",
                prev.read("href"));

        // self link
        DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals("http://localhost:8080/geoserver/" + requestPath, self.read("href"));
    }

    @Test
    public void testPagingLinksPostFirst() throws Exception {
        String request =
                "{\n"
                        + "  \"collections\": [\n"
                        + "    \"SAS1\",\n"
                        + "    \"LANDSAT8\"\n"
                        + "  ],\n"
                        + "  \"filter\": \"eo:cloud_cover=0\",\n"
                        + "  \"filter-lang\": \"cql-text\",\n"
                        + "  \"limit\": 1\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/search", request, 200);
        assertEquals(Integer.valueOf(3), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // two links expected, self and next
        assertEquals(Integer.valueOf(2), doc.read("links.length()"));

        // self link
        DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/search", self.read("href"));
        assertEquals("POST", self.read("method"));
        assertThrows(PathNotFoundException.class, () -> self.read("body"));

        // next link
        DocumentContext next = readSingleContext(doc, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/search", next.read("href"));
        assertEquals("POST", next.read("method"));
        assertEquals(Integer.valueOf(1), next.read("body.limit"));
        assertEquals(Integer.valueOf(1), next.read("body.startIndex"));
    }

    @Test
    public void testPagingLinksPostSecond() throws Exception {
        String request =
                "{\n"
                        + "  \"collections\": [\n"
                        + "    \"SAS1\",\n"
                        + "    \"LANDSAT8\"\n"
                        + "  ],\n"
                        + "  \"filter\": \"eo:cloud_cover=0\",\n"
                        + "  \"filter-lang\": \"cql-text\",\n"
                        + "  \"limit\": 1,\n"
                        + "  \"startIndex\": 1\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/search", request, 200);
        assertEquals(Integer.valueOf(3), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // three links expected, prev, self and next
        assertEquals(Integer.valueOf(3), doc.read("links.length()"));

        // prev link (order should be stable, linked hash maps all around)
        DocumentContext prev = readSingleContext(doc, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/search", prev.read("href"));
        assertEquals("POST", prev.read("method"));
        assertEquals(Integer.valueOf(1), prev.read("body.limit"));
        assertEquals(Integer.valueOf(0), prev.read("body.startIndex"));

        // self link
        DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/search", self.read("href"));
        assertEquals("POST", self.read("method"));
        assertThrows(PathNotFoundException.class, () -> self.read("body"));

        // next link
        DocumentContext next = readSingleContext(doc, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/search", next.read("href"));
        assertEquals("POST", next.read("method"));
        assertEquals(Integer.valueOf(1), next.read("body.limit"));
        assertEquals(Integer.valueOf(2), next.read("body.startIndex"));
    }

    @Test
    public void testPagingLinksPostLast() throws Exception {
        String request =
                "{\n"
                        + "  \"collections\": [\n"
                        + "    \"SAS1\",\n"
                        + "    \"LANDSAT8\"\n"
                        + "  ],\n"
                        + "  \"filter\": \"eo:cloud_cover=0\",\n"
                        + "  \"filter-lang\": \"cql-text\",\n"
                        + "  \"limit\": 1,\n"
                        + "  \"startIndex\": 2\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/search", request, 200);
        assertEquals(Integer.valueOf(3), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // two links expected, prev, self
        assertEquals(Integer.valueOf(2), doc.read("links.length()"));

        // prev link (order should be stable, linked hash maps all around)
        DocumentContext prev = readSingleContext(doc, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/search", prev.read("href"));
        assertEquals("POST", prev.read("method"));
        assertEquals(Integer.valueOf(1), prev.read("body.limit"));
        assertEquals(Integer.valueOf(1), prev.read("body.startIndex"));

        // self link
        DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/search", self.read("href"));
        assertEquals("POST", self.read("method"));
        assertThrows(PathNotFoundException.class, () -> self.read("body"));
    }

    @Test
    public void testSentinelItemsHTML() throws Exception {
        Document doc = getAsJSoup("ogc/stac/search?f=html&collections=SENTINEL2");
        assertEquals("STAC search", doc.select("#title").text());

        // the item identifiers
        Set<String> titles =
                doc.select("div.card-header h2").stream()
                        .map(e -> e.text())
                        .collect(Collectors.toSet());
        assertThat(titles, Matchers.everyItem(Matchers.startsWith("S2A_OPER_MSI")));

        // test the Sentinel2 entry
        Elements s2Body =
                doc.select(
                        "div.card-header:has(a:contains(S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04)) ~ div.card-body");
        assertTextContains(s2Body, "[data-tid='gbounds']", "-119,174, 33,333, -117,969, 34,338.");
        assertTextContains(s2Body, "[data-tid='ccover']", "7");
    }

    @Test
    public void testSearchAllMixedTemplates() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/stac/search?f=json", 200);

        // this one has a custom template
        DocumentContext l8Sample = readSingleContext(doc, "features[?(@.id == 'LS8_TEST.02')]");
        checkLandsat8_02(l8Sample);

        // this one uses the standard template
        DocumentContext s2Sample =
                readSingleContext(
                        doc,
                        "features[?(@.id == 'S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04')]");
        checkSentinel2Sample(s2Sample);
    }

    @Test
    public void testLandsat8Gsd() throws Exception {
        // gsd is statically set to 30, default template misses is, only landsat8 should come back
        // only one feature matching.
        DocumentContext json =
                getAsJSONPath("ogc/stac/search?filter=gsd = 30&filter-lang=cql-text", 200);

        assertEquals(Integer.valueOf(1), json.read("numberMatched"));
        assertEquals(Integer.valueOf(1), json.read("numberReturned"));
        assertThat(json.read("features[*].id"), containsInAnyOrder("LS8_TEST.02"));
    }
}
