/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.Queryables;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class QueryablesTest extends MapsTestSupport {

    private static final String LAKES_QUERYABLES = "ogc/maps/v1/collections/" + MockData.LAKES.getPrefix() + ":"
            + MockData.LAKES.getLocalPart() + "/queryables";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        addWaterTemp(testData);
    }

    @Test
    public void testLakesQueryables() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse(LAKES_QUERYABLES, 200);
        assertEquals("application/schema+json", getBaseMimeType(response.getContentType()));

        DocumentContext json = getAsJSONPath(response);
        assertEquals("object", json.read("type"));
        assertEquals(Queryables.JSON_SCHEMA_DRAFT_2020_12, readSingle(json, ".$schema"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/maps/v1/collections/cite%3ALakes/queryables",
                readSingle(json, ".$id"));

        // the Lakes attributes: a polygon geometry, a string and a numeric identifier
        assertEquals("geometry-multipolygon", json.read("properties.the_geom.format"));
        assertEquals("string", json.read("properties.NAME.type"));
        assertEquals("string", json.read("properties.FID.type"));
        assertEquals(Integer.valueOf(3), json.read("properties.length()", Integer.class));
    }

    @Test
    public void testQueryablesHTML() throws Exception {
        Document document = getAsJSoup(LAKES_QUERYABLES + "?f=html");
        assertEquals(
                "the_geom: MultiPolygon",
                document.select("#queryables li:eq(0)").text());
    }

    /** The collection document advertises the resource, so a client can find it without guessing the path. */
    @Test
    public void testCollectionLink() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/cite:Lakes", 200);
        String href =
                readSingle(json, "links[?(@.rel=='" + Queryables.REL + "' && @.type=='application/schema+json')].href");
        assertThat(href, endsWith("/ogc/maps/v1/collections/cite:Lakes/queryables?f=application%2Fschema%2Bjson"));
    }

    /** The collection page links the resource; the collections listing stays compact and does not. */
    @Test
    public void testCollectionHTMLLink() throws Exception {
        Document collection = getAsJSoup("ogc/maps/v1/collections/cite:Lakes?f=html");
        assertThat(
                collection.select("#html_cite__Lakes_queryables").attr("href"),
                endsWith("/collections/cite:Lakes/queryables?f=text%2Fhtml"));

        Document collections = getAsJSoup("ogc/maps/v1/collections?f=html");
        assertThat(collections.select("#html_cite__Lakes_queryables"), empty());
    }

    /** A simple raster has no attributes to filter on, and each layer group member has its own set of them. */
    @Test
    public void testRasterQueryables() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/maps/v1/collections/" + getLayerId(MockData.TASMANIA_DEM) + "/queryables", 404);
        assertEquals(APIException.NOT_FOUND, json.read("type"));
        assertThat(json.read("title"), containsString("does not expose queryables"));
    }

    /** A structured reader takes a filter on its granule index, so the index attributes are the queryables. */
    @Test
    public void testStructuredCoverageQueryables() throws Exception {
        String path = "ogc/maps/v1/collections/" + getLayerId(WATER_TEMP) + "/queryables";
        DocumentContext json = getAsJSONPath(path, 200);
        assertEquals("object", json.read("type"));
        assertEquals("string", json.read("properties.location.type"));
        assertEquals("string", json.read("properties.ingestion.type"));
        assertEquals("date-time", json.read("properties.ingestion.format"));
        assertEquals("integer", json.read("properties.elevation.type"));
        assertEquals("geometry-multipolygon", json.read("properties.the_geom.format"));
        assertEquals(Integer.valueOf(4), json.read("properties.length()", Integer.class));

        // and the collection advertises the resource, like a vector one
        DocumentContext collection = getAsJSONPath("ogc/maps/v1/collections/" + getLayerId(WATER_TEMP), 200);
        assertThat(
                readSingle(
                        collection,
                        "links[?(@.rel=='" + Queryables.REL + "' && @.type=='application/schema+json')].href"),
                endsWith("/collections/sf:watertemp/queryables?f=application%2Fschema%2Bjson"));
    }

    @Test
    public void testQueryablesDisabled() throws Exception {
        withConformance(MapsConformance::setQueryables, false, () -> assertNoQueryables());
    }

    /** Queryables only describe what the filter accepts, so they go away with filtering. */
    @Test
    public void testQueryablesGoneWithFiltering() throws Exception {
        withConformance(MapsConformance::setMapFilter, false, () -> assertNoQueryables());
    }

    private void assertNoQueryables() throws Exception {
        DocumentContext json = getAsJSONPath(LAKES_QUERYABLES, 404);
        assertEquals(APIException.NOT_FOUND, json.read("type"));
        assertThat(json.read("title"), containsString("Queryables are not enabled"));
        // the collection document must not advertise a resource that is not there
        DocumentContext collection = getAsJSONPath("ogc/maps/v1/collections/cite:Lakes", 200);
        List<String> links = collection.read("links[?(@.rel=='" + Queryables.REL + "')].href");
        assertThat(links, empty());
    }
}
