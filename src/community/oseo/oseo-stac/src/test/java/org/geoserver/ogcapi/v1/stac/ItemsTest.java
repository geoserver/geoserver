/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

public class ItemsTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        copyTemplate("/mandatoryLinks.json");
        copyTemplate("/items-LANDSAT8.json");
        copyTemplate("/items-SENTINEL2.json");
        copyTemplate("/items-SAS1.json");
        copyTemplate("/box.json");
        copyTemplate("/parentLink.json");
    }

    @Test
    public void testSentinelItemsJSON() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/items?limit=50", 200);

        // global properties
        assertEquals("FeatureCollection", json.read("type"));
        assertEquals(Integer.valueOf(19), json.read("numberMatched"));
        assertEquals(Integer.valueOf(19), json.read("numberReturned"));
        assertEquals(STACService.STAC_VERSION, json.read("stac_version"));

        // global links (only one expected for this one, there is no paging)
        DocumentContext link = readSingleContext(json, "links");
        assertEquals("self", link.read("rel"));
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, link.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/items?limit=50",
                link.read("href"));

        // check it's only sentinel2 data
        List<String> collections = json.read("features[*].collection");
        assertThat(new HashSet<>(collections), containsInAnyOrder("SENTINEL2"));
        assertEquals(Integer.valueOf(19), json.read("features.length()", Integer.class));

        // read single reference feature
        DocumentContext s2Sample =
                readSingleContext(
                        json,
                        "features[?(@.id == 'S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02"
                                + ".04')]");
        checkSentinel2Sample(s2Sample);

        // a sample image that does have an actual time range
        DocumentContext s2Range =
                readSingleContext(
                        json,
                        "features[?(@.id == 'S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04')]");
        assertEquals("2017-02-26T10:20:21.026+00:00", s2Range.read("properties.start_datetime"));
        assertEquals("2017-02-26T10:30:00.031+00:00", s2Range.read("properties.end_datetime"));
    }

    @Test
    public void testSentinelItemsJSONWorkspace() throws Exception {
        // Mismatched workspace
        getAsMockHttpServletResponse("cite/ogc/stac/v1/collections/SENTINEL2/items?limit=50", 404);
        // Matched workspace
        DocumentContext docMatchingWorkspace =
                getAsJSONPath("sf/ogc/stac/v1/collections/SENTINEL2/items?limit=50", 200);
        assertEquals(Integer.valueOf(19), docMatchingWorkspace.read("numberReturned"));
        // No match, but has null in workspace array and this is the global workspace
        DocumentContext docNullWorkspace =
                getAsJSONPath("ogc/stac/v1/collections/SAS1/items?limit=50", 200);
        assertEquals(Integer.valueOf(19), docMatchingWorkspace.read("numberReturned"));
    }

    @Test
    public void testSentinelItemJSON() throws Exception {
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SENTINEL2/items/S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        200);

        assertEquals("Feature", json.read("type"));
        checkSentinel2Sample(json);
    }

    @Test
    public void testSentinelItemJSONWorkspace() throws Exception {
        // matched workspace
        DocumentContext json =
                getAsJSONPath(
                        "sf/ogc/stac/v1/collections/SENTINEL2/items/S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        200);

        // Mismatched workspace
        getAsMockHttpServletResponse(
                "cite/ogc/stac/v1/collections/SENTINEL2/items/S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                404);
        // No workspace set
        DocumentContext jsonNoWorkspaceSet =
                getAsJSONPath("sf/ogc/stac/v1/collections/SAS1/items/SAS1_20180227102021.02", 200);
    }

    @Test
    public void testDisabledItem() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/LANDSAT8/items/LS8_TEST.DISABLED", 404);

        assertEquals("InvalidParameterValue", json.read("code"));
        assertEquals("Could not locate item LS8_TEST.DISABLED", json.read("description"));
    }

    @Test
    public void testLandsat8ItemsJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/collections/LANDSAT8/items", 200);

        assertEquals("FeatureCollection", json.read("type"));

        // check it's only landsat8 data
        List<String> collections = json.read("features[*].collection");
        assertThat(new HashSet<>(collections), containsInAnyOrder("LANDSAT8"));
        assertEquals(Integer.valueOf(2), json.read("features.length()", Integer.class));

        // read single reference feature, will test only peculiarities of this
        DocumentContext l8Sample = readSingleContext(json, "features[?(@.id == 'LS8_TEST.02')]");
        checkLandsat8_02(l8Sample);
    }

    @Test
    public void testLandsat8ItemJSON() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/LANDSAT8/items/LS8_TEST.02", 200);

        assertEquals("Feature", json.read("type"));

        // it's the expected feature
        assertEquals("LS8_TEST.02", json.read("id"));
        checkLandsat8_02(json);
    }

    @Test
    public void testSentinelItemsHTML() throws Exception {
        Document doc = getAsJSoup("ogc/stac/v1/collections/SENTINEL2/items?f=html");
        assertEquals("SENTINEL2 items", doc.select("#title").text());

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
        testSentinel2SampleHTML(s2Body);
    }

    private void testSentinel2SampleHTML(Elements elements) {
        assertTextContains(elements, "[data-tid='gbounds']", "-119.174, 33.333, -117.969, 34.338.");
        assertTextContains(elements, "[data-tid='ccover']", "7");
    }

    @Test
    public void testPagingLinksFirstPage() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/items?limit=5", 200);

        // two links expected, self and next
        assertEquals(Integer.valueOf(2), json.read("links.length()"));

        // self link
        DocumentContext self = readSingleContext(json, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/items?limit=5",
                self.read("href"));

        // next link (order should be stable, linked hash maps all around)
        DocumentContext next = readSingleContext(json, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/items?limit=5&startIndex=5",
                next.read("href"));
    }

    @Test
    public void testPagingLinksSecondPage() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/items?startIndex=5&limit=5", 200);

        // three links expected, prev, self and next
        assertEquals(Integer.valueOf(3), json.read("links.length()"));

        // prev link
        DocumentContext prev = readSingleContext(json, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/items?startIndex=0&limit=5",
                prev.read("href"));

        // self link
        DocumentContext self = readSingleContext(json, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/items?startIndex=5&limit=5",
                self.read("href"));

        // next link
        DocumentContext next = readSingleContext(json, "links[?(@.rel=='next')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, next.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/items?startIndex=10&limit=5",
                next.read("href"));
    }

    @Test
    public void testPagingLinksLastPage() throws Exception {
        // assuming 19 items
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/items?startIndex=15&limit=5", 200);

        // two links expected, prev and self
        assertEquals(Integer.valueOf(2), json.read("links.length()"));

        // prev link
        DocumentContext prev = readSingleContext(json, "links[?(@.rel=='prev')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, prev.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/items?startIndex=10&limit=5",
                prev.read("href"));

        // self link
        DocumentContext self = readSingleContext(json, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/items?startIndex=15&limit=5",
                self.read("href"));
    }

    @Test
    public void testPagingLinksLastPageFull() throws Exception {
        // assuming 19 items, and asking a page of 19. There should not be another page
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/items?limit=19", 200);

        // Only self link expected
        assertEquals(Integer.valueOf(1), json.read("links.length()"));

        // self link
        DocumentContext self = readSingleContext(json, "links[?(@.rel=='self')]");
        assertEquals(OGCAPIMediaTypes.GEOJSON_VALUE, self.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/items?limit=19",
                self.read("href"));
    }

    @Test
    public void testTimeFilterInstant() throws Exception {
        // only one feature intersects this point in time
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SENTINEL2/items?limit=19&datetime=2017-02-26T10:25:00Z",
                        200);

        assertEquals(Integer.valueOf(1), json.read("numberMatched"));
        assertEquals(Integer.valueOf(1), json.read("numberReturned"));
        String expectedId = "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04";
        assertThat(json.read("features[*].id"), contains(expectedId));
    }

    @Test
    public void testTimeFilterRange() throws Exception {
        // only two sentinel features available in 2017 feb/march
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SENTINEL2/items?limit=19&datetime=2017-02-25/2017-03-31",
                        200);

        assertEquals(Integer.valueOf(2), json.read("numberMatched"));
        assertEquals(Integer.valueOf(2), json.read("numberReturned"));
        assertThat(
                json.read("features[*].id"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04"));
    }

    @Test
    public void testSpaceFilter() throws Exception {
        // only two features matching
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/items?bbox=16,42,17,43", 200);

        assertEquals(Integer.valueOf(2), json.read("numberMatched"));
        assertEquals(Integer.valueOf(2), json.read("numberReturned"));
        assertThat(
                json.read("features[*].id"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWG_N02.01",
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01"));
    }

    @Test
    public void testCloudCoverFilter() throws Exception {
        // only one feature matching.
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SENTINEL2/items?filter=eo:cloud_cover > 60&filter-lang=cql-text",
                        200);

        assertEquals(Integer.valueOf(1), json.read("numberMatched"));
        assertEquals(Integer.valueOf(1), json.read("numberReturned"));
        assertThat(
                json.read("features[*].id"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TUH_N02.01"));
    }

    @Test
    public void testSpaceCQLFilter() throws Exception {
        // only one feature matching.
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SENTINEL2/items?filter=eo:cloud_cover = 1&filter-lang=cql-text&bbox=16,42,17,43",
                        200);

        assertEquals(Integer.valueOf(1), json.read("numberMatched"));
        assertEquals(Integer.valueOf(1), json.read("numberReturned"));
        assertThat(
                json.read("features[*].id"),
                containsInAnyOrder(
                        "S2A_OPER_MSI_L1C_TL_SGS__20160117T141030_A002979_T33TWH_N02.01"));
    }

    @Test
    public void testSAS1ItemsJSON() throws Exception {
        // this one has a custom template with relative imports for the bbox and a link
        DocumentContext json = getAsJSONPath("ogc/stac/v1/collections/SAS1/items", 200);

        assertEquals("FeatureCollection", json.read("type"));

        // check it's only landsat8 data
        List<String> collections = json.read("features[*].collection");
        assertThat(new HashSet<>(collections), containsInAnyOrder("SAS1"));
        assertEquals(Integer.valueOf(2), json.read("features.length()", Integer.class));

        // read single reference feature, will test only peculiarities of this
        DocumentContext item =
                readSingleContext(json, "features[?(@.id == 'SAS1_20180227102021.02')]");

        // the bbox is in an included template
        assertEquals(Integer.valueOf(-180), item.read("bbox[0]", Integer.class));
        assertEquals(Integer.valueOf(-90), item.read("bbox[1]", Integer.class));
        assertEquals(Integer.valueOf(180), item.read("bbox[2]", Integer.class));
        assertEquals(Integer.valueOf(90), item.read("bbox[3]", Integer.class));

        // the parent link is included as well
        DocumentContext link = readSingleContext(item, "links[?(@.rel == 'collection')]");
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SAS1", link.read("href"));
    }

    @Test
    public void dynamicMergeTest() throws Exception {
        DocumentContext result = getAsJSONPath("ogc/stac/v1/collections/LANDSAT8/items", 200);

        // tests before the dynamic merge with expression on overlay
        String href = result.read("features[1].assets.thumbnail.href");
        String title = result.read("features[1].assets.thumbnail.title");
        String type = result.read("features[1].assets.thumbnail.type");
        int additional = result.read("features[1].assets.thumbnail.additional");

        assertEquals("will replace", href);
        assertEquals("will replace", type);
        assertEquals("Thumbnail", title);
        assertEquals(0, additional);
    }

    @Test
    public void dynamicIncludeFlatTest() throws Exception {
        DocumentContext result = getAsJSONPath("ogc/stac/v1/collections/LANDSAT8/items", 200);

        // tests before the dynamic merge with expression on overlay
        String randomNumber = result.read("features[1].dynamicIncludeFlatTest.randomNumber");
        String href = result.read("features[1].dynamicIncludeFlatTest.thumbnail.href");
        String title = result.read("features[1].dynamicIncludeFlatTest.thumbnail.title");
        String type = result.read("features[1].dynamicIncludeFlatTest.thumbnail.type");
        String thumbnail2 = result.read("features[1].dynamicIncludeFlatTest.thumbnail2");

        assertEquals("23", randomNumber);
        assertEquals(
                "https://landsat-pds.s3.us-west-2.amazonaws.com/c1/L8/218/077/LC08_L1TP_218077_20210511_20210511_01_T1/LC08_L1TP_218077_20210511_20210511_01_T1_thumb_large.jpg",
                href);
        assertEquals("image/jpeg", type);
        assertEquals("Thumbnail", title);
        assertEquals("thumbnail2", thumbnail2);
    }

    @Test
    public void dynamicIncludeFlatTest2() throws Exception {
        // test dynamic flat inclusion with a JSON object with multiple attributes and
        // with a base node with an attributeName equal to one of the attributes in the
        // item json property. The item json property should override the base one.
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/SAS1/items/SAS1_20180226102021.01", 200);

        String thumbnailTitle = json.read("dynamicIncludeFlatTest.thumbnail.title");
        String thumbnailTitle2 = json.read("dynamicIncludeFlatTest.thumbnail2.title");
        assertEquals("the_title", thumbnailTitle);
        assertEquals("the_title2", thumbnailTitle2);
    }

    @Test
    public void dynamicIncludeCQLFilterTest() throws Exception {
        // same as dynamicIncludeFlatTest2 but checking filters are getting mapped
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SAS1/items?filter=sat:absolute_orbit < 17050",
                        200);

        assertEquals(1, json.read("features", List.class).size());
        assertThat(
                json.read("features[0].properties.sat:absolute_orbit", Double.class),
                lessThanOrEqualTo(17050d));

        json =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SAS1/items?filter=sat:absolute_orbit > 17050",
                        200);

        assertEquals(1, json.read("features", List.class).size());
        assertThat(
                json.read("features[0].properties.sat:absolute_orbit", Double.class),
                greaterThanOrEqualTo(17050d));
    }

    @Test
    public void dynamicIncludeSort() throws Exception {
        // sorting up
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/SAS1/items?sortby=sat:absolute_orbit", 200);

        assertEquals(2, json.read("features", List.class).size());
        assertEquals(
                json.read("features..properties.sat:absolute_orbit", List.class),
                Arrays.asList(17030, 17060));

        // sorting down
        json = getAsJSONPath("ogc/stac/v1/collections/SAS1/items?sortby=-sat:absolute_orbit", 200);

        assertEquals(2, json.read("features", List.class).size());
        assertEquals(
                json.read("features..properties.sat:absolute_orbit", List.class),
                Arrays.asList(17060, 17030));
    }

    @Test
    public void testSearchSortByTimeAscending() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SENTINEL2/items?filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')&sortby=datetime",
                        200);

        assertThat(
                (List<String>) doc.read("features[*].id"),
                contains(
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04",
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04"));
    }

    @Test
    public void testSearchSortByTimeDescending() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SENTINEL2/items?filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')&sortby=-datetime",
                        200);

        assertThat(
                (List<String>) doc.read("features[*].id"),
                contains(
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04",
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04"));
    }

    @Test
    public void testSearchSortByCloudCover() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SENTINEL2/items?filter=datetime > DATE('2017-02-25') and datetime < DATE('2017-03-31')&sortby=eo:cloud_cover",
                        200);

        assertThat(
                (List<String>) doc.read("features[*].id"),
                contains(
                        "S2A_OPER_MSI_L1C_TL_SGS__20170226T171842_A008785_T32TPN_N02.04",
                        "S2A_OPER_MSI_L1C_TL_MTI__20170308T220244_A008933_T11SLT_N02.04"));
    }

    @Test
    public void testQueryByDynamicProperty() throws Exception {
        // s2:datastrip_id is in a dynamically included JSON
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SAS1/items?filter=s2:datastrip_id = 'S2A_OPER_MSI_L2A_DS_VGS1_20201206T095713_S20201206T074838_N02.14'",
                        200);

        assertThat((List<String>) doc.read("features[*].id"), contains("SAS1_20180226102021.01"));
    }

    @Test
    public void testPropertySelectionOnDynamicMerge() throws Exception {
        DocumentContext result =
                getAsJSONPath(
                        "ogc/stac/v1/collections/LANDSAT8/items?fields=properties.instruments,-assets,assets.thumbnail.type",
                        200);

        // empty fields param only mandatory attributes should be there.
        String id = result.read("features[1].id");
        String type = result.read("features[1].type");
        Map<String, Object> geom = result.read("features[1].geometry");
        JSONArray bbox = result.read("features[1].bbox");
        Map<String, Object> properties = result.read("features[1].properties");
        Map<String, Object> assets = result.read("features[1].assets");
        String thumbnailType = result.read("features[1].assets.thumbnail.type");
        JSONArray links = result.read("features[1].links");

        assertEquals("LS8_TEST.02", id);
        assertEquals("Feature", type);
        assertEquals(2, geom.size());
        assertEquals(4, bbox.size());
        assertTrue(properties.size() > 2);
        JSONArray array = (JSONArray) properties.get("instruments");
        assertTrue(array.contains("OLI"));
        assertTrue(array.contains("TIRS"));
        assertNotNull(assets);
        // size is one other assets object have been filtered out.
        assertEquals(1, assets.size());
        assertEquals("will replace", thumbnailType);
        assertNotNull(links);
        assertFalse(links.isEmpty());
    }

    @Test
    public void testPropertySelectionDynamicMerge2() throws Exception {
        DocumentContext result =
                getAsJSONPath(
                        "ogc/stac/v1/collections/LANDSAT8/items?fields=properties,-properties.created,-properties.datetime,-assets.thumbnail",
                        200);

        // empty fields param only mandatory attributes should be there.
        String id = result.read("features[1].id");
        String type = result.read("features[1].type");
        Map<String, Object> geom = result.read("features[1].geometry");
        JSONArray bbox = result.read("features[1].bbox");
        Map<String, Object> properties = result.read("features[1].properties");
        Map<String, Object> assets = result.read("features[1].assets");
        JSONArray links = result.read("features[1].links");

        assertEquals("LS8_TEST.02", id);
        assertEquals("Feature", type);
        assertEquals(2, geom.size());
        assertEquals(4, bbox.size());
        // more then just the two mandatory ones.
        assertTrue(properties.size() > 2);
        assertFalse(properties.containsKey("created"));
        assertFalse(properties.containsKey("datetime"));

        // asserts some of the properties value included.
        assertEquals(properties.get("platform"), "LANDSAT_8");
        assertEquals(properties.get("constellation"), "landsat8");
        assertEquals(properties.get("eo:cloud_cover"), 0d);
        assertEquals(properties.get("sat:orbit_state"), "descending");
        assertEquals(properties.get("gsd"), 30);
        assertEquals(properties.get("landsat:orbit"), 65);

        assertNotNull(assets);
        assertFalse(assets.isEmpty());
        assertFalse(assets.containsKey("thumbnail"));
        assertNotNull(links);
        assertFalse(links.isEmpty());
    }

    @Test
    public void testPropertySelectionDynamicIncludeFlat() throws Exception {

        // with a base node with an attributeName equal to one of the attributes in the
        // item json property. The item json property should override the base one.
        DocumentContext json =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SAS1/items/SAS1_20180226102021.01?fields=properties,dynamicIncludeFlatTest,-dynamicIncludeFlatTest.thumbnail2",
                        200);

        String id = json.read("id");
        String type = json.read("type");
        Map<String, Object> geom = json.read("geometry");
        JSONArray bbox = json.read("bbox");
        Map<String, Object> properties = json.read("properties");
        Map<String, Object> assets = json.read("assets");
        JSONArray links = json.read("links");
        Map<String, Object> includeFlatField = json.read("dynamicIncludeFlatTest");

        assertEquals("SAS1_20180226102021.01", id);
        assertEquals("Feature", type);
        assertEquals(2, geom.size());
        assertEquals(4, bbox.size());
        // more then just the two mandatory ones.
        assertTrue(properties.size() > 2);
        // asserts some of the properties value included.
        assertTrue(properties.containsKey("created"));
        assertTrue(properties.containsKey("datetime"));
        assertEquals(properties.get("s1:frame_number"), 218);
        assertEquals(properties.get("s1:start_anxtime"), 1090739);
        assertEquals(properties.get("s1:stop_anxtime"), 1117820);
        assertEquals(properties.get("s1:processing_date"), "2019-07-07T18: 33: 12.592265Z");
        assertEquals(properties.get("s1:processing_site"), "Airbus DS-Newport");
        assertEquals(properties.get("s1:ipf_version"), 3.1);

        assertNotNull(assets);
        assertFalse(assets.isEmpty());
        assertNotNull(links);
        assertFalse(links.isEmpty());

        // we excluded thumbnail2 keeping only thumbnail
        assertFalse(includeFlatField.containsKey("thumbnail2"));
        assertTrue(includeFlatField.containsKey("thumbnail"));
    }

    @Test
    public void testQueryByDynamicPropertyNonQueryable() throws Exception {
        // s2:granule_id is in a dynamically included JSON but is not in the queryables array
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SAS1/items?filter=s2:granule_id = 'S2A_OPER_MSI_L2A_TL_VGS1_20201206T095713_A028503_T37MDU_N02.143'",
                        200);
        assertEquals(new Integer(0), doc.read("numberMatched", Integer.class));

        GeoServer gs = getGeoServer();
        OSEOInfo service = gs.getService(OSEOInfo.class);
        service.getGlobalQueryables()
                .addAll(Arrays.asList("id", "geometry", "collection", "s2:granule_id"));
        gs.save(service);
        DocumentContext doc2 =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SAS1/items?filter=s2:granule_id = 'S2A_OPER_MSI_L2A_TL_VGS1_20201206T095713_A028503_T37MDU_N02.143'",
                        200);
        assertThat((List<String>) doc2.read("features[*].id"), contains("SAS1_20180227102021.02"));

        service.getGlobalQueryables().clear();
        service.getGlobalQueryables().addAll(Arrays.asList("id", "geometry", "collection"));
        gs.save(service);
    }

    @Test
    public void testTopLevelArrayProperty() throws Exception {
        // query with an existing keyword
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SENTINEL2/items?filter=keywords = 'k1'", 200);
        assertEquals(new Integer(1), doc.read("numberMatched", Integer.class));
        assertEquals(
                "S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04",
                readSingleContext(doc, "features").read("id"));

        // query with an existing keyword (
        doc = getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/items?filter=keywords = 'k2'", 200);
        assertEquals(new Integer(1), doc.read("numberMatched", Integer.class));
        assertEquals(
                "S2A_OPER_MSI_L1C_TL_SGS__20160929T154211_A006640_T32TPP_N02.04",
                readSingleContext(doc, "features").read("id"));

        // query with a missing keyword
        doc =
                getAsJSONPath(
                        "ogc/stac/v1/collections/SENTINEL2/items?filter=keywords = 'notAKeyword'",
                        200);
        assertEquals(new Integer(0), doc.read("numberMatched", Integer.class));
    }

    @Test
    public void dynamicIncludeFlatArrayTest() throws Exception {
        DocumentContext result = getAsJSONPath("ogc/stac/v1/collections/LANDSAT8/items", 200);

        JSONArray array = result.read("features[1].includeFlatArray");
        String title = result.read("features[1].includeFlatArray[4].title");
        String titleMTL = result.read("features[1].includeFlatArray[4].titleMTL");
        assertEquals(5, array.size());
        assertTrue(array.contains("thumbnail"));
        assertTrue(array.contains("thumbnail2"));
        assertTrue(array.contains("staticValue"));
        assertTrue(array.contains("staticValue2"));
        assertEquals("title", title);
        assertEquals("MTL Metadata", titleMTL);
    }

    @Test
    public void dynamicIncludeFlatArrayFieldSelectionTest() throws Exception {
        DocumentContext result =
                getAsJSONPath(
                        "ogc/stac/v1/collections/LANDSAT8/items?fields=includeFlatArray,-includeFlatArray.titleMTL",
                        200);

        JSONArray array = result.read("features[1].includeFlatArray");
        assertEquals(5, array.size());
        int i = array.indexOf("thumbnail");
        assertNotEquals(-1, i);
        array.remove(i);
        i = array.indexOf("thumbnail2");
        assertNotEquals(-1, i);
        array.remove(i);
        i = array.indexOf("staticValue");
        assertNotEquals(-1, i);
        array.remove(i);
        i = array.indexOf("staticValue2");
        assertNotEquals(-1, i);
        array.remove(i);

        Map<String, Object> map = (Map<String, Object>) array.get(0);
        assertEquals(1, map.size());
        assertFalse(map.containsKey("titleMTL"));
    }

    @Test
    public void testJSONBKeySort() throws Exception {
        // assetsb object inserted into test table as
        // '{"g":1,"m":2,"f":3,"h":4,"c":5,"a":{"hello":6,"archive":7,"meh":{"working":8,"aver":9}},"opt:cloudCover":34}'
        DocumentContext json =
                getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/items/JSONB_TEST.02", 200);
        assertEquals(
                "{a={archive=7, hello=6, meh={aver=9, working=8}}, c=5, f=3, g=1, h=4, m=2, opt:cloudCover=34}",
                json.read("assetsb").toString());
        // assets object is json, not jsonb, so it is not sorted
        assertEquals(
                "{g=1, m=2, f=3, h=4, c=5, a={hello=6, archive=7, meh={working=8, aver=9}}, opt:cloudCover=34, thumbnail={additional=0, href=will replace, type=will replace}}",
                json.read("assets").toString());
    }
}
