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
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.data.test.MockData;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

public class CollectionsTest extends MapsTestSupport {

    private static final String CONTAINER_GROUP = "containerGroup";

    @Before
    public void revertChanges() throws IOException {
        revertLayer(MockData.BUILDINGS);
        revertLayer(MockData.FIFTEEN);
        revertLayer(MockData.SEVEN);
        LayerGroupInfo group = getCatalog().getLayerGroupByName(CONTAINER_GROUP);
        if (group != null) getCatalog().remove(group);
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

        assertEquals(36, (int) json.read("collections.length()", Integer.class));

        // make it misconfigured
        misconfigured.setLatLonBoundingBox(null);
        getCatalog().save(misconfigured);

        DocumentContext json2 = getAsJSONPath("ogc/maps/v1/collections", 200);
        // expect one fewer layers due to skipping
        assertEquals(35, (int) json2.read("collections.length()", Integer.class));
    }

    private void testCollectionsJson(DocumentContext json, MediaType defaultFormat) throws Exception {
        assertEquals(getNumberOfCollections(), (int) json.read("collections.length()", Integer.class));

        // check we have the expected number of links and they all use the right "rel" relation
        Collection<MediaType> formats = GeoServerExtensions.bean(
                        APIDispatcher.class, GeoServerSystemTestSupport.applicationContext)
                .getProducibleMediaTypes(CollectionsDocument.class, true);
        formats.forEach(format -> {
            // check rel
            // a collection has more than one link of the same type (map and item links too), so look
            // for the document link among them
            List<String> rels = json.read("collections[0].links[?(@.type=='" + format + "')].rel");
            assertThat(rels, hasItem(defaultFormat.equals(format) ? "self" : "alternate"));
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
        assertEquals(List.of("#/crs"), readSingle(json, "$.collections[?(@.id=='cite:Lakes')].crs"));
        // one stored in a projected CRS adds it, since the reference cannot carry it
        assertEquals(
                List.of("#/crs", "http://www.opengis.net/def/crs/EPSG/0/32615"),
                readSingle(json, "$.collections[?(@.id=='cgf:Polygons')].crs"));
    }

    @Test
    public void testCollectionsHTML() throws Exception {
        Document document = getAsJSoup("ogc/maps/v1/collections?f=html");
        // This may need update if the layout is styled
        assertEquals(
                getNumberOfCollections(), document.select("#content h2 a[href]").size());
        // the storage CRS belongs to the single collection page, the list would just repeat it
        assertTrue(document.select("#cgf__Polygons_storageCrs").isEmpty());
    }

    /**
     * A collection the service cannot offer is not listed: a geometryless layer cannot be drawn, and a disabled or
     * unadvertised one is no more published here than it is in WMS.
     */
    @Test
    public void testUnmappableLayersNotListed() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections", 200);
        assertEquals(getNumberOfCollections(), (int) json.read("collections.length()", Integer.class));
        assertEquals(List.of(), json.read("collections[?(@.id=='cite:Geometryless')]", List.class));

        Catalog catalog = getCatalog();
        // both flags live on the resource, the layer only delegates to it
        ResourceInfo disabled =
                catalog.getLayerByName(getLayerId(MockData.FIFTEEN)).getResource();
        disabled.setEnabled(false);
        catalog.save(disabled);
        ResourceInfo hidden = catalog.getLayerByName(getLayerId(MockData.SEVEN)).getResource();
        hidden.setAdvertised(false);
        catalog.save(hidden);

        json = getAsJSONPath("ogc/maps/v1/collections", 200);
        assertEquals(getNumberOfCollections() - 2, (int) json.read("collections.length()", Integer.class));
        assertEquals(List.of(), json.read("collections[?(@.id=='cdf:Fifteen')]", List.class));
        assertEquals(List.of(), json.read("collections[?(@.id=='cdf:Seven')]", List.class));
    }

    /**
     * A {@code CONTAINER} layer group has no name to ask a map for, so it is not a collection, while its members are
     * collections of their own.
     */
    @Test
    public void testContainerGroupNotListed() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(CONTAINER_GROUP);
        group.setMode(LayerGroupInfo.Mode.CONTAINER);
        group.getLayers().add(catalog.getLayerByName(getLayerId(MockData.LAKES)));
        group.getStyles().add(null);
        new CatalogBuilder(catalog).calculateLayerGroupBounds(group);
        catalog.add(group);

        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections", 200);
        assertEquals(getNumberOfCollections(), (int) json.read("collections.length()", Integer.class));
        assertEquals(List.of(), json.read("collections[?(@.id=='" + CONTAINER_GROUP + "')]", List.class));
        // its member is still one, on its own name
        assertEquals("cite:Lakes", readSingle(json, "collections[?(@.id=='cite:Lakes')].id"));
    }

    @Test
    public void testVersionHeader() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("ogc/maps/v1/collections?f=html");
        assertTrue(headerHasValue(response, "API-Version", "1.0.1"));
    }

    /** Every layer of the test data is a collection, apart from the geometryless one, which cannot be drawn. */
    private int getNumberOfCollections() {
        return getCatalog().getLayers().size() - 1;
    }
}
