/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Literal;
import org.geotools.filter.IsGreaterThanImpl;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Ignore;
import org.junit.Test;

public class SearchTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        copyTemplate("/mandatoryLinks.json");
        copyTemplate("/items-LANDSAT8.json");
        // these 3 needed for SAS1 to work
        copyTemplate("/items-SAS1.json");
        copyTemplate("/box.json");
        copyTemplate("/parentLink.json");
        copyTemplate("/items-SENTINEL2.json");
    }

    @Test
    public void testCollectionsGet() throws Exception {
        // two SAS1, one Landsat
        DocumentContext doc = getAsJSONPath("ogc/stac/v1/search?collections=SAS1,LANDSAT8", 200);
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
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);
        checkCollections(doc, true);
    }

    public void checkCollections(DocumentContext doc, boolean post) {
        checkCollectionsSinglePage(
                doc, 4, containsInAnyOrder("LANDSAT8", "LANDSAT8", "SAS1", "SAS1"));

        // expecting only a self link
        DocumentContext link = readSingleContext(doc, "links");
        assertEquals("self", link.read("rel"));
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, link.read("type"));
        if (post) {
            assertEquals("http://localhost:8080/geoserver/ogc/stac/v1/search", link.read("href"));
            assertTrue(link.read("merge"));
        } else {
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/stac/v1/search?collections=SAS1%2CLANDSAT8",
                    link.read("href"));
        }
    }

    @Test
    public void testCollectionsCqlGet() throws Exception {
        // two SAS1, one Landsat, but the filter matches constellation to landsat8 only
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?collections=SAS1,LANDSAT8"
                                + "&filter=constellation='landsat8'&filter-lang=cql2-text",
                        200);
        checkCollectionsSinglePage(doc, 2, containsInAnyOrder("LANDSAT8", "LANDSAT8"));
    }

    @Test
    public void testCollectionsCqlGetWorkspace() throws Exception {
        DocumentContext docMatchingWorkspace =
                getAsJSONPath(
                        "sf/ogc/stac/v1/search?"
                                + "&filter=collection='SENTINEL2'&filter-lang=cql2-text",
                        200);
        assertEquals(Integer.valueOf(10), docMatchingWorkspace.read("numberReturned"));
        DocumentContext docMismatchingWorkspace =
                getAsJSONPath(
                        "cite/ogc/stac/v1/search?"
                                + "&filter=collection='SENTINEL2'&filter-lang=cql2-text",
                        200);
        assertEquals(Integer.valueOf(0), docMismatchingWorkspace.read("numberReturned"));
        DocumentContext docMismatchedButNullInArray =
                getAsJSONPath(
                        "ogc/stac/v1/search?"
                                + "&filter=collection='LANDSAT8'&filter-lang=cql2-text",
                        200);
        assertEquals(Integer.valueOf(2), docMismatchedButNullInArray.read("numberReturned"));
    }

    @Test
    public void testCollectionsCqlGetQueryableNotInList() throws Exception {
        // constellation is queryable only for LANDSAT8 not SENTINEL2
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?collections=SENTINEL2,LANDSAT8"
                                + "&filter=constellation='landsat8'&filter-lang=cql2-text",
                        200);
        assertEquals(new Integer(2), doc.read("numberMatched", Integer.class));
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
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);
        checkCollectionsSinglePage(doc, 2, containsInAnyOrder("LANDSAT8", "LANDSAT8"));
    }

    @Test
    public void testCollectionsCql2TextPost() throws Exception {
        // two SAS1, one Landsat, but the filter matches constellation to landsat8 only
        String request =
                "{\n"
                        + "  \"collections\": [\n"
                        + "    \"SAS1\",\n"
                        + "    \"LANDSAT8\"\n"
                        + "  ],\n"
                        + "  \"filter\": \"constellation='landsat8'\",\n"
                        + "  \"filter-lang\": \"cql2-text\"\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);
        checkCollectionsSinglePage(doc, 2, containsInAnyOrder("LANDSAT8", "LANDSAT8"));
    }

    @Test
    public void testCollectionsCqlPostSort() throws Exception {
        // two SAS1, two Landsat, sorted in descending order
        String request =
                "{\n"
                        + "  \"collections\": [\n"
                        + "    \"SAS1\",\n"
                        + "    \"LANDSAT8\"\n"
                        + "  ],\n"
                        + "  \"sortby\": [{\"field\":\"constellation\",\"direction\":\"desc\"}]\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);
        checkCollectionsSinglePage(doc, 4, contains("SAS1", "SAS1", "LANDSAT8", "LANDSAT8"));
        // the two landsat8 should be first
        String request2 =
                "{\n"
                        + "  \"collections\": [\n"
                        + "    \"SAS1\",\n"
                        + "    \"LANDSAT8\"\n"
                        + "  ],\n"
                        + "  \"sortby\": [{\"field\":\"constellation\",\"direction\":\"asc\"}]\n"
                        + "}";
        DocumentContext doc2 = postAsJSONPath("ogc/stac/v1/search", request2, 200);
        checkCollectionsSinglePage(doc2, 4, contains("LANDSAT8", "LANDSAT8", "SAS1", "SAS1"));
    }

    @Test
    public void testCollectionsCql2JsonPost() throws Exception {
        // two SAS1, one Landsat, but the filter matches constellation to landsat8 only
        String request =
                "{\n"
                        + "  \"collections\": [\n"
                        + "    \"SAS1\",\n"
                        + "    \"LANDSAT8\"\n"
                        + "  ],\n"
                        + "  \"filter\":{\"op\":\"=\",\"args\":[{\"property\":\"constellation\"},\"landsat8\"]},\n"
                        + "  \"filter-lang\": \"cql2-json\"\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);
        checkCollectionsSinglePage(doc, 2, containsInAnyOrder("LANDSAT8", "LANDSAT8"));
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
        DocumentContext doc = getAsJSONPath("ogc/stac/v1/search?bbox=16,42,17,43", 200);

        checkCollectionsItemsSinglePage(
                doc,
                6,
                containsInAnyOrder(
                        "LANDSAT8", "LANDSAT8", "SAS1", "SAS1", "SENTINEL2", "SENTINEL2"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWG_N02.01",
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01",
                        "LS8_TEST.02",
                        "SAS1_20180226102021.01",
                        "SAS1_20180227102021.02",
                        "JSONB_TEST.02"));
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
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);

        checkCollectionsItemsSinglePage(
                doc,
                6,
                containsInAnyOrder(
                        "LANDSAT8", "LANDSAT8", "SAS1", "SAS1", "SENTINEL2", "SENTINEL2"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWG_N02.01",
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01",
                        "LS8_TEST.02",
                        "SAS1_20180226102021.01",
                        "SAS1_20180227102021.02",
                        "JSONB_TEST.02"));
    }

    @Test
    public void testBBOXAsTextFilterPost() throws Exception {
        // two sentinel, one landsat, one sas
        String request = "{\"bbox\":\"16,42,17,43\"}";
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);

        checkCollectionsItemsSinglePage(
                doc,
                6,
                containsInAnyOrder(
                        "LANDSAT8", "LANDSAT8", "SAS1", "SAS1", "SENTINEL2", "SENTINEL2"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWG_N02.01",
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01",
                        "LS8_TEST.02",
                        "SAS1_20180226102021.01",
                        "SAS1_20180227102021.02",
                        "JSONB_TEST.02"));
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
                        "ogc/stac/v1/search?intersects={\"type\": \"Point\", \"coordinates\": [16.5, 42.5]}",
                        200);

        checkCollectionsItemsSinglePage(
                doc,
                4,
                containsInAnyOrder("LANDSAT8", "LANDSAT8", "SAS1", "SAS1"),
                containsInAnyOrder(
                        "LS8_TEST.02",
                        "SAS1_20180226102021.01",
                        "SAS1_20180227102021.02",
                        "JSONB_TEST.02"));
    }

    @Test
    public void testAllProductsNoDisabled() throws Exception {
        // only SAS and LANDSAT intersecting this point
        DocumentContext doc = getAsJSONPath("ogc/stac/v1/search?limit=50", 200);

        // had to inline as "CoreMatches.not" has poor generics management
        assertEquals(Integer.valueOf(24), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(24), doc.read("numberReturned"));
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
                        + "  \"intersects\": {\n"
                        + "    \"type\": \"Point\",\n"
                        + "    \"coordinates\": [\n"
                        + "      16.5,\n"
                        + "      42.5\n"
                        + "    ]\n"
                        + "  }\n"
                        + "}";
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);

        checkCollectionsItemsSinglePage(
                doc,
                4,
                containsInAnyOrder("LANDSAT8", "LANDSAT8", "SAS1", "SAS1"),
                containsInAnyOrder(
                        "LS8_TEST.02",
                        "SAS1_20180226102021.01",
                        "SAS1_20180227102021.02",
                        "JSONB_TEST.02"));
    }

    @Test
    public void testPagingLinksFirst() throws Exception {
        String requestPath =
                "ogc/stac/v1/search?collections=SAS1%2CLANDSAT8"
                        + "&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1";
        DocumentContext doc = getAsJSONPath(requestPath, 200);
        assertEquals(Integer.valueOf(4), doc.read("numberMatched"));
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
                "http://localhost:8080/geoserver/ogc/stac/v1/search?collections=SAS1%2CLANDSAT8&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=1",
                next.read("href"));
    }

    @Test
    public void testPagingLinksSecond() throws Exception {
        String requestPath =
                "ogc/stac/v1/search?collections=SAS1%2CLANDSAT8"
                        + "&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=1";
        DocumentContext doc = getAsJSONPath(requestPath, 200);
        assertEquals(Integer.valueOf(4), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // three links expected, prev, self and next
        assertEquals(Integer.valueOf(3), doc.read("links.length()"));

        // prev link (order should be stable, linked hash maps all around)
        DocumentContext prev = readSingleContext(doc, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/search?collections=SAS1%2CLANDSAT8&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=0",
                prev.read("href"));

        // self link
        DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals("http://localhost:8080/geoserver/" + requestPath, self.read("href"));

        // next link (order should be stable, linked hash maps all around)
        DocumentContext next = readSingleContext(doc, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/search?collections=SAS1%2CLANDSAT8&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=2",
                next.read("href"));
    }

    @Test
    public void testPagingLinksLast() throws Exception {
        String requestPath =
                "ogc/stac/v1/search?collections=SAS1%2CLANDSAT8"
                        + "&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=2";
        DocumentContext doc = getAsJSONPath(requestPath, 200);
        assertEquals(Integer.valueOf(4), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // two links expected, prev, self
        assertEquals(Integer.valueOf(3), doc.read("links.length()"));

        // prev link (order should be stable, linked hash maps all around)
        DocumentContext prev = readSingleContext(doc, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/search?collections=SAS1%2CLANDSAT8&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=1",
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
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);
        assertEquals(Integer.valueOf(4), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // two links expected, self and next
        assertEquals(Integer.valueOf(2), doc.read("links.length()"));

        // self link
        DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/v1/search", self.read("href"));
        assertEquals("POST", self.read("method"));
        assertThrows(PathNotFoundException.class, () -> self.read("body"));

        // next link
        DocumentContext next = readSingleContext(doc, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/v1/search", next.read("href"));
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
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);
        assertEquals(Integer.valueOf(4), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // three links expected, prev, self and next
        assertEquals(Integer.valueOf(3), doc.read("links.length()"));

        // prev link (order should be stable, linked hash maps all around)
        DocumentContext prev = readSingleContext(doc, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/v1/search", prev.read("href"));
        assertEquals("POST", prev.read("method"));
        assertEquals(Integer.valueOf(1), prev.read("body.limit"));
        assertEquals(Integer.valueOf(0), prev.read("body.startIndex"));

        // self link
        DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/v1/search", self.read("href"));
        assertEquals("POST", self.read("method"));
        assertThrows(PathNotFoundException.class, () -> self.read("body"));

        // next link
        DocumentContext next = readSingleContext(doc, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/v1/search", next.read("href"));
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
        DocumentContext doc = postAsJSONPath("ogc/stac/v1/search", request, 200);
        assertEquals(Integer.valueOf(4), doc.read("numberMatched"));
        assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

        // two links expected, prev, self
        assertEquals(Integer.valueOf(3), doc.read("links.length()"));

        // prev link (order should be stable, linked hash maps all around)
        DocumentContext prev = readSingleContext(doc, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/v1/search", prev.read("href"));
        assertEquals("POST", prev.read("method"));
        assertEquals(Integer.valueOf(1), prev.read("body.limit"));
        assertEquals(Integer.valueOf(1), prev.read("body.startIndex"));

        // self link
        DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals("http://localhost:8080/geoserver/ogc/stac/v1/search", self.read("href"));
        assertEquals("POST", self.read("method"));
        assertThrows(PathNotFoundException.class, () -> self.read("body"));
    }

    @Test
    public void testSentinelItemsHTML() throws Exception {
        Document doc = getAsJSoup("ogc/stac/v1/search?f=html&collections=SENTINEL2");
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
        assertTextContains(s2Body, "[data-tid='gbounds']", "-119.174, 33.333, -117.969, 34.338.");
        assertTextContains(s2Body, "[data-tid='ccover']", "7");
    }

    @Test
    public void testSearchAllMixedTemplates() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/stac/v1/search?f=json", 200);

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
    @Ignore // we changed lookups so that queryables and filter back-mapping match 1-1
    // gsd was not considered a queryable, so it's not filterable upon... we might get this
    // back if gsd is considered a valid queryable too
    public void testLandsat8Gsd() throws Exception {
        // gsd is statically set to 30, default template misses is, only landsat8 should come back
        // only one feature matching.
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/search?filter=gsd = 30&filter-lang=cql-text", 200);

        assertEquals(Integer.valueOf(1), json.read("numberMatched"));
        assertEquals(Integer.valueOf(1), json.read("numberReturned"));
        assertThat(json.read("features[*].id"), containsInAnyOrder("LS8_TEST.02"));
    }

    @Test
    public void testGeometryQueryable() throws Exception {
        // two sentinel, one landsat, one sas
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?filter=S_INTERSECTS(geometry, ENVELOPE(16,42,17,43))",
                        200);

        checkCollectionsItemsSinglePage(
                doc,
                6,
                containsInAnyOrder(
                        "LANDSAT8", "LANDSAT8", "SAS1", "SAS1", "SENTINEL2", "SENTINEL2"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWG_N02.01",
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01",
                        "LS8_TEST.02",
                        "SAS1_20180226102021.01",
                        "SAS1_20180227102021.02",
                        "JSONB_TEST.02"));
    }

    @Test
    public void testCollectionQueryable() throws Exception {
        // using the "collection" queryable
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?&filter=collection='LANDSAT8'&filter-lang=cql2-text",
                        200);
        checkCollectionsSinglePage(doc, 2, containsInAnyOrder("LANDSAT8", "LANDSAT8"));
    }

    @Test
    public void testIdQueryable() throws Exception {
        DocumentContext doc =
                getAsJSONPath("ogc/stac/v1/search?filter=id='SAS1_20180226102021.01'", 200);

        checkCollectionsItemsSinglePage(
                doc, 1, containsInAnyOrder("SAS1"), containsInAnyOrder("SAS1_20180226102021.01"));
    }

    @Test
    public void testDateTimeQueryable() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')",
                        200);

        checkCollectionsItemsSinglePage(
                doc,
                3,
                containsInAnyOrder("SENTINEL2", "SENTINEL2", "gsTestCollection"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04",
                        "GS_TEST_PRODUCT.01"));
    }

    @Test
    public void testSearchSortByTimeAscending() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')&sortby=datetime",
                        200);

        checkCollectionsItemsSinglePage(
                doc,
                3,
                containsInAnyOrder("SENTINEL2", "SENTINEL2", "gsTestCollection"),
                contains(
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04",
                        "GS_TEST_PRODUCT.01",
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04"));
    }

    @Test
    public void testSearchSortByTimeDescending() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')&sortby=-datetime",
                        200);

        checkCollectionsItemsSinglePage(
                doc,
                3,
                containsInAnyOrder("SENTINEL2", "SENTINEL2", "gsTestCollection"),
                contains(
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        "GS_TEST_PRODUCT.01",
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04"));
    }

    @Test
    public void testSearchSortByCloudCover() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')&sortby=eo:cloud_cover",
                        200);

        checkCollectionsItemsSinglePage(
                doc,
                3,
                containsInAnyOrder("SENTINEL2", "SENTINEL2", "gsTestCollection"),
                contains(
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04",
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        "GS_TEST_PRODUCT.01"));
    }

    @Test
    public void testSearchSortById() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')&sortby=id",
                        200);

        checkCollectionsItemsSinglePage(
                doc,
                3,
                containsInAnyOrder("SENTINEL2", "SENTINEL2", "gsTestCollection"),
                contains(
                        "GS_TEST_PRODUCT.01",
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04"));
    }

    @Test
    public void testQueryByDynamicProperty() throws Exception {
        // s2:datastrip_id is in a dynamically included JSON
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SAS1/items?filter=s2:datastrip_id = 'S2A_OPER_MSI_L2A_DS_VGS1_20201206T095713_S20201206T074838_N02.14'",
                        200);

        List<String> ids = doc.read("features[*].id");
        assertEquals(1, ids.size());
        assertThat(ids, contains("SAS1_20180226102021.01"));
    }

    @Test
    public void testSearchPropertySelection() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?fields=properties,-properties.sat:absolute_orbit,-properties.instruments,collection&filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')&sortby=id",
                        200);

        checkCollectionsItemsSinglePage(
                doc,
                3,
                containsInAnyOrder("SENTINEL2", "SENTINEL2", "gsTestCollection"),
                contains(
                        "GS_TEST_PRODUCT.01",
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04"));

        JSONArray array = doc.read("features[*].properties");
        for (int i = 0; i < array.size(); i++) {
            Map<String, Object> props = (Map<String, Object>) array.get(i);
            assertFalse(props.containsKey("sat:absolute_orbit"));
            assertFalse(props.containsKey("instruments"));
            // more then just mandatory props
            assertTrue(props.size() > 2);
        }
    }

    @Test
    public void testSearchPropertySelectionStaticJsonObj() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?collections=SENTINEL2&fields=properties,-properties.SENTINEL2.fullStaticObject.staticAttr1,-properties.SENTINEL2.fullStaticObject.staticAttr3.nestedStatic1&filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')&sortby=id",
                        200);

        JSONArray array = doc.read("features[?(@.id != 'GS_TEST_PRODUCT.01')].properties");
        for (int i = 0; i < array.size(); i++) {
            Map<String, Object> props = (Map<String, Object>) array.get(i);
            Map<String, Object> sentinelObject = (Map<String, Object>) props.get("SENTINEL2");
            Map<String, Object> staticValues =
                    (Map<String, Object>) sentinelObject.get("fullStaticObject");
            assertFalse(staticValues.containsKey("staticAttr1"));
            assertEquals("staticValue2", staticValues.get("staticAttr2"));
            Map<String, Object> staticValues3 =
                    (Map<String, Object>) staticValues.get("staticAttr3");
            assertFalse(staticValues3.containsKey("nestedStatic1"));
            assertEquals("nestedStaticVal2", staticValues3.get("nestedStatic2"));
        }
    }

    @Test
    public void testSearchPropertySelectionTopLevel() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/search?collections=SENTINEL2&fields=-links,-assets&filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')&sortby=id",
                        200);

        String featurePath = "features[?(@.id != 'GS_TEST_PRODUCT.01')]";
        assertEquals(0, doc.read(featurePath + ".assets", List.class).size());
        assertEquals(0, doc.read(featurePath + ".links", List.class).size());
    }

    public void testIndexOptimizerVisitorConvertsDouble() throws Exception {
        List<String> collections = Arrays.asList("SAS1");
        Filter filter = getStacService().parseFilter(collections, "s1:ipf_version>2", null);
        assertEquals(2.0, ((Literal) ((IsGreaterThanImpl) filter).getExpression2()).getValue());
    }

    @Test
    public void testPagingLinksProxyBase() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("https://thehost/geoserver");
        getGeoServer().save(global);

        try {
            String requestPath =
                    "ogc/stac/v1/search?collections=SAS1%2CLANDSAT8"
                            + "&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=1";
            DocumentContext doc = getAsJSONPath(requestPath, 200);
            assertEquals(Integer.valueOf(4), doc.read("numberMatched"));
            assertEquals(Integer.valueOf(1), doc.read("numberReturned"));

            // three links expected, prev, self and next
            assertEquals(Integer.valueOf(3), doc.read("links.length()"));

            // prev link (order should be stable, linked hash maps all around)
            DocumentContext prev = readSingleContext(doc, "links[?(@.rel=='prev')]");
            assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
            assertEquals(
                    "https://thehost/geoserver/ogc/stac/v1/search?collections=SAS1%2CLANDSAT8&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=0",
                    prev.read("href"));

            // self link
            DocumentContext self = readSingleContext(doc, "links[?(@.rel=='self')]");
            assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
            assertEquals("https://thehost/geoserver/" + requestPath, self.read("href"));

            // next link (order should be stable, linked hash maps all around)
            DocumentContext next = readSingleContext(doc, "links[?(@.rel=='next')]");
            assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
            assertEquals(
                    "https://thehost/geoserver/ogc/stac/v1/search?collections=SAS1%2CLANDSAT8&filter=eo%3Acloud_cover%3D0&filter-lang=cql-text&limit=1&startIndex=2",
                    next.read("href"));

        } finally {
            global.getSettings().setProxyBaseUrl(null);
            getGeoServer().save(global);
        }
    }
}
