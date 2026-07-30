/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.data.test.MockData;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class CollectionsTest extends MapsTestSupport {
    @Before
    public void revertChanges() throws IOException {
        revertLayer(MockData.BUILDINGS);
    }

    @Test
    public void testCollectionsJsonDefault() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections", 200);
        testCollectionsJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections?f=json", 200);
        testCollectionsJson(json, MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/maps/v1/collections/?f=application/yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testCollectionsJson(json, MediaType.parseMediaType("application/yaml"));
    }

    @Test
    public void testSkipMisconfigured() throws Exception {
        // enable skipping of misconfigured layers
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);
        // not misconfigured yet
        FeatureTypeInfo misconfigured = getCatalog().getFeatureTypeByName(getLayerId(MockData.BUILDINGS));

        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections", 200);

        assertEquals(37, (int) json.read("collections.length()", Integer.class));

        // make it misconfigured
        misconfigured.setLatLonBoundingBox(null);
        getCatalog().save(misconfigured);

        DocumentContext json2 = getAsJSONPath("ogc/maps/v1/collections", 200);
        // expect one fewer layers due to skipping
        assertEquals(36, (int) json2.read("collections.length()", Integer.class));
    }

    private void testCollectionsJson(DocumentContext json, MediaType defaultFormat) throws Exception {
        assertEquals(getNumberOfLayers(), (int) json.read("collections.length()", Integer.class));

        // check we have the expected number of links and they all use the right "rel" relation
        Collection<MediaType> formats = GeoServerExtensions.bean(
                        APIDispatcher.class, GeoServerSystemTestSupport.applicationContext)
                .getProducibleMediaTypes(CollectionsDocument.class, true);
        formats.forEach(format -> {
            // check rel
            List items = json.read("collections[0].links[?(@.type=='" + format + "')]", List.class);
            Map item = (Map) items.get(0);
            if (defaultFormat.equals(format)) {
                assertEquals("self", item.get("rel"));
            } else {
                assertEquals("alternate", item.get("rel"));
            }
        });
    }

    /**
     * The supported CRSs are listed once at the root of the document, each collection pointing at that list, unless it
     * has a storage CRS of its own to add.
     */
    @Test
    public void testCollectionsCrsList() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections", 200);
        assertEquals("http://www.opengis.net/def/crs/OGC/1.3/CRS84", json.read("$.crs[0]"));
        // no SRS list is configured, so every code the referencing database knows is advertised
        assertThat(json.read("$.crs.length()", Integer.class), greaterThan(1000));
        List<String> crs = json.read("$.crs");
        assertThat(crs, hasItem("http://www.opengis.net/def/crs/EPSG/0/32615"));

        // a collection stored in CRS84 only references the shared list; the filter returns one match per
        // collection, each holding that collection own list
        assertEquals(List.of(List.of("#/crs")), json.read("$.collections[?(@.id=='cite:Lakes')].crs", List.class));
        // one stored in a projected CRS adds it, since the reference cannot carry it
        assertEquals(
                List.of(List.of("#/crs", "http://www.opengis.net/def/crs/EPSG/0/32615")),
                json.read("$.collections[?(@.id=='cgf:Polygons')].crs", List.class));
    }

    @Test
    public void testCollectionsHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/maps/v1/collections?f=html");
        // This may need update if the layout is styled
        assertEquals(getNumberOfLayers(), document.select("#content h2 a[href]").size());
        // the storage CRS belongs to the single collection page, the list would just repeat it
        assertTrue(document.select("#cgf__Polygons_storageCrs").isEmpty());
    }

    @Test
    public void testVersionHeader() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("ogc/maps/v1/collections?f=html");
        assertTrue(headerHasValue(response, "API-Version", "1.0.1"));
    }

    private int getNumberOfLayers() {
        return getCatalog().getLayers().size();
    }
}
