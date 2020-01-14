/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Streams;
import com.jayway.jsonpath.DocumentContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import org.geoserver.api.APIDispatcher;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.layer.TileLayer;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;

public class CollectionsTest extends TilesTestSupport {

    public static final String BASIC_POLYGONS_TITLE = "Basic polygons";
    public static final String BASIC_POLYGONS_DESCRIPTION = "I love basic polygons!";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        FeatureTypeInfo basicPolygons =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        basicPolygons.setTitle(BASIC_POLYGONS_TITLE);
        basicPolygons.setAbstract(BASIC_POLYGONS_DESCRIPTION);
        getCatalog().save(basicPolygons);
    }

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/tiles/collections", 200);
        testCollectionsJson(json, MediaType.APPLICATION_JSON);
    }

    private void testCollectionsJson(DocumentContext json, MediaType defaultFormat)
            throws Exception {
        int expected = getGWC().getTileLayerNames().size();
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));

        // check we have the expected number of links and they all use the right "rel" relation
        Collection<MediaType> formats =
                GeoServerExtensions.bean(
                                APIDispatcher.class, GeoServerSystemTestSupport.applicationContext)
                        .getProducibleMediaTypes(TiledCollectionDocument.class, true);
        assertThat(
                formats.size(),
                lessThanOrEqualTo((int) json.read("collections[0].links.length()", Integer.class)));
        for (MediaType format : formats) {
            // check title and rel
            List items = json.read("collections[0].links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            if (defaultFormat.equals(format)) {
                assertEquals("self", item.get("rel"));
            } else {
                assertEquals("alternate", item.get("rel"));
            }
        }
    }

    @Test
    public void testCollectionsWorkspaceSpecificJson() throws Exception {
        DocumentContext json = getAsJSONPath("cdf/ogc/tiles/collections", 200);
        long expected =
                Streams.stream(getGWC().getTileLayers())
                        .filter(tl -> tl.getName().startsWith("cdf:"))
                        .count();
        // check the filtering
        assertEquals(expected, (int) json.read("collections.length()", Integer.class));
        // check the workspace prefixes have been removed
        assertThat(json.read("collections[?(@.id=='Deletes')]"), not(empty()));
        assertThat(json.read("collections[?(@.id=='cdf__Deletes')]"), empty());
        // check the url points to a ws qualified url
        final String deleteHrefPath =
                "collections[?(@.id=='Deletes')].links[?(@.rel=='self' && @.type=='application/json')].href";
        assertEquals(
                "http://localhost:8080/geoserver/cdf/ogc/tiles/collections/Deletes?f=application%2Fjson",
                ((JSONArray) json.read(deleteHrefPath)).get(0));
    }

    @Test
    @Ignore
    public void testCollectionsXML() throws Exception {
        Document dom = getAsDOM("ogc/tiles/collections?f=application/xml");
        print(dom);
        // TODO: add actual tests
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/tiles/collections/?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testCollectionsJson(json, MediaType.parseMediaType("application/x-yaml"));
    }

    @Test
    public void testCollectionsHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/tiles/collections?f=html");

        // check collection links
        for (TileLayer tl : getGWC().getTileLayers()) {
            String htmlId = tl.getName().replace(":", "__");
            assertNotNull(document.select("#html_" + htmlId + "_link"));
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/tiles/collections/"
                            + ResponseUtils.urlEncode(tl.getName())
                            + "?f=text%2Fhtml",
                    document.select("#html_" + htmlId + "_link").attr("href"));
        }

        // go and check a specific collection title and description
        FeatureTypeInfo basicPolygons =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.BASIC_POLYGONS));
        String basicPolygonsName = basicPolygons.prefixedName().replace(":", "__");
        assertEquals(
                BASIC_POLYGONS_TITLE, document.select("#" + basicPolygonsName + "_title").text());
        assertEquals(
                BASIC_POLYGONS_DESCRIPTION,
                document.select("#" + basicPolygonsName + "_description").text());
    }
}
