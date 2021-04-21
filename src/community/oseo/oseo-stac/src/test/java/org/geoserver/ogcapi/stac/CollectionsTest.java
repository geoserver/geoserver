/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.Query;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

public class CollectionsTest extends STACTestSupport {

    @Test
    public void testTemplatesCopy() throws Exception {
        GeoServerDataDirectory dd = getDataDirectory();
        Resource templates = dd.get("templates/ogc/stac");
        assertEquals(Resource.Type.RESOURCE, templates.get("collections.json").getType());
        assertEquals(Resource.Type.RESOURCE, templates.get("items.json").getType());
    }

    @Test
    public void testCollectionsHTML() throws Exception {
        Document document = getAsJSoup("ogc/stac/collections?f=html");

        // page title (it's the right page?)
        assertEquals("GeoServer STAC Collections", document.select("#title").text());

        // the collection titles
        Set<String> titles =
                document.select("div.card-header h2")
                        .stream()
                        .map(e -> e.text())
                        .collect(Collectors.toSet());
        assertThat(
                titles,
                Matchers.containsInAnyOrder(
                        "ATMTEST", "SENTINEL2", "SENTINEL1", "LANDSAT8", "GS_TEST"));

        // test the Sentinel2 entry
        Elements s2Body =
                document.select("div.card-header:has(a:contains(SENTINEL2)) ~ div.card-body");
        testSentinel2HTML(s2Body);
    }

    @Test
    public void testCollectionsJSON() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/collections?f=json", 200);
        // all collections are accounted for (one is disabled
        OpenSearchAccess osa = getOpenSearchAccess();
        Integer collectionCount = osa.getCollectionSource().getCount(Query.ALL) - 1;
        assertEquals(collectionCount, json.read("$.collections.length()"));

        // concentrate on the Sentinel2 object
        DocumentContext s2 = readSingleContext(json, "collections[?(@.id == 'SENTINEL2')]");
        testSentinel2JSON(s2);

        // check Sentinel 1 providers too
        DocumentContext s1 = readSingleContext(json, "collections[?(@.id == 'SENTINEL1')]");
        assertEquals(2, (int) s1.read("providers.length()", Integer.class));
        assertEquals(
                1,
                s1.read("providers[?(@.name == 'European Union/ESA/Copernicus')]", List.class)
                        .size());
        assertEquals(1, s1.read("providers[?(@.name == 'GeoServer')].length()", List.class).size());

        // and the Landsat ones
        DocumentContext l8 = readSingleContext(json, "collections[?(@.id == 'LANDSAT8')]");
        assertEquals(2, (int) l8.read("providers.length()", Integer.class));
        assertEquals(1, l8.read("providers[?(@.name == 'USGS')].length()", List.class).size());
        assertEquals(1, l8.read("providers[?(@.name == 'GeoServer')].length()", List.class).size());
    }

    @Test
    public void testCollectionHTML() throws Exception {
        Document document = getAsJSoup("ogc/stac/collections/SENTINEL2?f=html");
        // page title (it's the right page?)
        assertEquals("SENTINEL2", document.select("#title").text());

        testSentinel2HTML(document.getAllElements());
    }

    private void testSentinel2HTML(Elements elements) {
        assertTextContains(elements, "[data-tid='title']", "The Sentinel-2 mission");
        assertTextContains(
                elements,
                "[data-tid='description']",
                "The SENTINEL-2 mission is a land monitoring constellation");
        assertTextContains(elements, "[data-tid='gbounds']", "-179, -89, 179, 89");
        assertTextContains(elements, "[data-tid='tbounds']", "2015-07-01");
        assertTextContains(elements, "[data-tid='tbounds']", "2016-02-26");
    }

    @Test
    public void testCollectionJSON() throws Exception {
        DocumentContext s2 = getAsJSONPath("ogc/stac/collections/SENTINEL2?f=json", 200);

        testSentinel2JSON(s2);
    }

    public void testSentinel2JSON(DocumentContext s2) {
        assertEquals("The Sentinel-2 mission", s2.read("title"));
        assertThat(
                s2.read("description"),
                CoreMatchers.containsString(
                        "The SENTINEL-2 mission is a land monitoring constellation of two "
                                + "satellites"));
        assertEquals(STACService.STAC_VERSION, s2.read("stac_version"));
        assertEquals("CC-BY-NC-ND-3.0-IGO", s2.read("license"));
        // Sentinel 2 bounding box
        DocumentContext s2bbox = readContext(s2, "extent.spatial.bbox");
        assertEquals(-179, s2bbox.read("$[0][0]"), 0d);
        assertEquals(-89, s2bbox.read("$[0][1]"), 0d);
        assertEquals(179, s2bbox.read("$[0][2]"), 0d);
        assertEquals(89, s2bbox.read("$[0][3]"), 0d);
        // Sentinel 2 temporal range
        DocumentContext s2time = readContext(s2, "extent.temporal.interval");
        assertEquals("2015-07-01T08:20:21.000+00:00", s2time.read("$[0][0]"));
        assertEquals("2016-02-26T09:20:21.000+00:00", s2time.read("$[0][1]"));
        // the providers for sentinel2
        assertEquals(2, (int) s2.read("providers.length()", Integer.class));
        assertEquals(
                1,
                s2.read("providers[?(@.name == 'European Union/ESA/Copernicus')]", List.class)
                        .size());
        assertEquals(1, s2.read("providers[?(@.name == 'GeoServer')].length()", List.class).size());
        // the links for sentinel2
        assertEquals(5, (int) s2.read("links.length()", Integer.class));
        assertEquals(
                "http://localhost:8080/geoserver/ogcapi/stac/collections/SENTINEL2",
                readSingle(s2, "links[?(@.rel == 'self')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogcapi/stac",
                readSingle(s2, "links[?(@.rel == 'parent')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogcapi/stac",
                readSingle(s2, "links[?(@.rel == 'root')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogcapi/stac/collections/SENTINEL2/items",
                readSingle(s2, "links[?(@.rel == 'items')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogcapi/stac/collections/SENTINEL2/queryables",
                readSingle(s2, "links[?(@.rel == '" + Queryables.REL + "')].href"));
    }

    @Test
    public void testDisabledItem() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/collections/DISABLED_COLLECTION", 404);

        assertEquals("InvalidParameterValue", json.read("code"));
        assertEquals("Collection not found: DISABLED_COLLECTION", json.read("description"));
    }
}
