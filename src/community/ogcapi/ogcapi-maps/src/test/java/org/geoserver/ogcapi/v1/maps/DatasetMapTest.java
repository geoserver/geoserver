/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static java.util.Comparator.comparing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.jayway.jsonpath.DocumentContext;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.APIException;
import org.geoserver.wms.WMSInfo;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * The "Dataset Map" and "Collection Selection" conformance classes: {@code /conf/dataset-map/landingpage},
 * {@code /conf/dataset-map/desc-extent}, {@code /conf/dataset-map/desc-crs}, {@code /conf/dataset-map/operation},
 * {@code /conf/collections-selection/collections-parameter} and
 * {@code /conf/collections-selection/collections-response}.
 */
public class DatasetMapTest extends MapsTestSupport {

    /** A window on the CITE data, where several of the test layers hold features. */
    private static final String WINDOW = "bbox=-0.002,-0.003,0.005,0.002&width=100&height=100";

    /** A pixel inside a lake, opaque in the default Lakes style, see {@link EncodingsTest}. */
    private static final int LAKE_X = 50;

    private static final int LAKE_Y = 64;

    private static final String DATASET_MAP = "ogc/maps/v1/map?f=image/png&" + WINDOW;

    static final String NATURE_GROUP = "nature";

    private static final String PARENT_GROUP = "parentGroup";

    private static final String CHILD_GROUP = "childGroup";

    private static final String CONTAINER_GROUP = "containerGroup";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // a layer group, to check that a group counts as one collection of the dataset
        Catalog catalog = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forests = layerWithBounds(catalog, cb, MockData.FORESTS);
        LayerInfo ponds = layerWithBounds(catalog, cb, MockData.PONDS);
        LayerInfo bridges = layerWithBounds(catalog, cb, MockData.BRIDGES);
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(NATURE_GROUP);
        group.getLayers().add(lakes);
        group.getLayers().add(forests);
        group.getStyles().add(null);
        group.getStyles().add(null);
        cb.calculateLayerGroupBounds(group);
        catalog.add(group);

        // a NAMED group holding another group, to check that the nested one is not a collection of its own
        LayerGroupInfo child = layerGroup(catalog, cb, CHILD_GROUP, LayerGroupInfo.Mode.NAMED, ponds);
        layerGroup(catalog, cb, PARENT_GROUP, LayerGroupInfo.Mode.NAMED, child);

        // a CONTAINER group, which WMS advertises without a name, so it cannot be asked for
        layerGroup(catalog, cb, CONTAINER_GROUP, LayerGroupInfo.Mode.CONTAINER, bridges);
    }

    /** Creates and saves a layer group of the given mode, with its bounds worked out from its contents. */
    private LayerGroupInfo layerGroup(
            Catalog catalog, CatalogBuilder cb, String name, LayerGroupInfo.Mode mode, PublishedInfo... contents)
            throws Exception {
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);
        group.setMode(mode);
        for (PublishedInfo content : contents) {
            group.getLayers().add(content);
            group.getStyles().add(null);
        }
        cb.calculateLayerGroupBounds(group);
        catalog.add(group);
        return group;
    }

    /** A layer with its bounds computed, several of the CITE test layers having none in the mock catalog. */
    private LayerInfo layerWithBounds(Catalog catalog, CatalogBuilder cb, QName name) throws Exception {
        LayerInfo layer = catalog.getLayerByName(getLayerId(name));
        cb.setupBounds(layer.getResource());
        catalog.save(layer.getResource());
        return layer;
    }

    private BufferedImage datasetMap(String collections) throws Exception {
        String query = collections == null ? "" : "&collections=" + collections;
        return getAsPNG(DATASET_MAP + query);
    }

    /** /conf/dataset-map/landingpage: a link with the map relation type, pointing at /map. */
    @Test
    public void testLandingPageMapLink() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1", 200);
        List<String> hrefs = json.read("links[?(@.rel == '" + CollectionDocument.REL_MAP + "')].href");
        assertEquals(List.of("http://localhost:8080/geoserver/ogc/maps/v1/map"), hrefs);
    }

    /**
     * /conf/dataset-map/desc-extent: an extent in the landing page, following the collection schema. It covers the
     * whole world, the default raster layers being global.
     */
    @Test
    public void testLandingPageExtent() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1", 200);
        assertEquals("http://www.opengis.net/def/crs/OGC/1.3/CRS84", json.read("extent.spatial.crs"));
        assertEquals(-180.0, json.read("extent.spatial.bbox[0][0]", Double.class), 0d);
        assertEquals(-90.0, json.read("extent.spatial.bbox[0][1]", Double.class), 0d);
        assertEquals(180.0, json.read("extent.spatial.bbox[0][2]", Double.class), 0d);
        assertEquals(90.0, json.read("extent.spatial.bbox[0][3]", Double.class), 0d);
    }

    /** /conf/dataset-map/desc-crs: the CRSs the dataset map supports, CRS84 first. */
    @Test
    public void testLandingPageCrs() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1", 200);
        List<String> crs = json.read("crs");
        assertEquals("http://www.opengis.net/def/crs/OGC/1.3/CRS84", crs.get(0));
        assertThat(crs.size(), greaterThan(1));
    }

    /** /conf/dataset-map/operation: a GET on /map renders a map of the whole dataset in the default styles. */
    @Test
    public void testDatasetMap() throws Exception {
        BufferedImage image = getAsPNG(DATASET_MAP);
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        // global raster layers cover the window, so every pixel comes back opaque
        assertEquals(0, countTransparentPixels(image));
    }

    /** Counts the pixels that are not fully opaque, alpha being the high byte of {@code getRGB}. */
    private int countTransparentPixels(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (new Color(image.getRGB(x, y), true).getAlpha() != 255) count++;
            }
        }
        return count;
    }

    /** The encoding is negotiated as it is for a collection map, so the landing page link, with no f, works. */
    @Test
    public void testDatasetMapNegotiatedEncoding() throws Exception {
        MockHttpServletRequest request = createRequest("ogc/maps/v1/map?" + WINDOW);
        request.setMethod("GET");
        request.addHeader("Accept", "image/jpeg");
        BufferedImage image = readImage(dispatch(request, null), "image/jpeg", "jpeg");
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
    }

    /**
     * /conf/collections-selection/collections-parameter: the parameter takes one or more collection identifiers, and
     * /conf/collections-selection/collections-response A: only those collections are rendered, so a map of one
     * collection is the very same image its own collection map resource returns.
     */
    @Test
    public void testSingleCollection() throws Exception {
        BufferedImage selected = datasetMap("cite:Lakes");
        BufferedImage collectionMap = getAsPNG("ogc/maps/v1/collections/cite:Lakes/map?f=image/png&" + WINDOW);
        assertSameImage(collectionMap, selected);
        // and it really is a map of the lakes only, the global rasters left out
        assertEquals(0xFF4040C0, selected.getRGB(LAKE_X, LAKE_Y));
        assertEquals(0, selected.getRGB(0, 0) >>> 24);
    }

    /** Several collections are rendered together: adding one to the selection changes the map. */
    @Test
    public void testMultipleCollections() throws Exception {
        BufferedImage lakes = datasetMap("cite:Lakes");
        BufferedImage both = datasetMap("cite:Lakes,cite:Forests");
        assertEquals(0xFF4040C0, both.getRGB(LAKE_X, LAKE_Y));
        assertNotEquals(differentPixels(lakes, both), 0);
    }

    /**
     * /conf/collections-selection/collections-response B: the leftmost collection is drawn first, at the bottom, the
     * rightmost last, on top. Each rendering is compared with the map of the collection that must win the pixel, so the
     * test states the stacking order without depending on the colours themselves.
     */
    @Test
    public void testCollectionsOrderIsBottomToTop() throws Exception {
        int lakesOnly = datasetMap("cite:Lakes").getRGB(LAKE_X, LAKE_Y);
        int worldOnly = datasetMap("wcs:World").getRGB(LAKE_X, LAKE_Y);
        // both are opaque here, otherwise the pixel would blend and prove nothing
        assertEquals(255, lakesOnly >>> 24);
        assertEquals(255, worldOnly >>> 24);
        assertNotEquals(lakesOnly, worldOnly);

        assertEquals(lakesOnly, datasetMap("wcs:World,cite:Lakes").getRGB(LAKE_X, LAKE_Y));
        assertEquals(worldOnly, datasetMap("cite:Lakes,wcs:World").getRGB(LAKE_X, LAKE_Y));
    }

    /** A layer group is one collection, and keeps its own internal order. */
    @Test
    public void testLayerGroupCollection() throws Exception {
        BufferedImage group = datasetMap("nature");
        BufferedImage collectionMap = getAsPNG("ogc/maps/v1/collections/nature/map?f=image/png&" + WINDOW);
        assertSameImage(collectionMap, group);
    }

    /**
     * /conf/collections-selection/collections-parameter C: a full URL of a collection resource is accepted besides the
     * bare identifier.
     */
    @Test
    public void testCollectionsAsUrls() throws Exception {
        String url = "http://localhost:8080/geoserver/ogc/maps/v1/collections/cite:Lakes";
        assertSameImage(datasetMap("cite:Lakes"), datasetMap(url));
        // the map resource URL of the collection names the same collection
        assertSameImage(datasetMap("cite:Lakes"), datasetMap(url + "/map"));
    }

    /** An unknown collection is a client error, listing the identifier that could not be found. */
    @Test
    public void testUnknownCollectionRejected() throws Exception {
        DocumentContext json = getAsJSONPath(DATASET_MAP + "&collections=cite:Lakes,notAcollection", 400);
        assertEquals(APIException.INVALID_PARAMETER_VALUE, json.read("type"));
        assertThat(json.read("title"), containsString("notAcollection"));
    }

    /** With the dataset map class disabled the resource does not exist, and neither does its feature info. */
    @Test
    public void testDatasetMapDisabled() throws Exception {
        withConformance(MapsConformance::setDatasetMap, false, () -> {
            assertEquals(404, getAsServletResponse(DATASET_MAP).getStatus());
            assertEquals(
                    404,
                    getAsServletResponse("ogc/maps/v1/map/info?f=application/json&" + WINDOW + "&i=50&j=50")
                            .getStatus());
            // the landing page stops advertising it, and stops describing what it would cover
            DocumentContext json = getAsJSONPath("ogc/maps/v1", 200);
            assertEquals(List.of(), json.read("links[?(@.rel == '" + CollectionDocument.REL_MAP + "')].href"));
            assertEquals(List.of(), json.read("$..extent"));
        });
    }

    /** With the collections selection class disabled the parameter is ignored, not rejected. */
    @Test
    public void testCollectionsSelectionDisabled() throws Exception {
        withConformance(MapsConformance::setCollectionsSelection, false, () -> {
            // the selection is not applied, so the map is the default one, and an unknown value is not looked up
            assertSameImage(getAsPNG(DATASET_MAP), datasetMap("cite:Lakes"));
            assertEquals(
                    200,
                    getAsServletResponse(DATASET_MAP + "&collections=notAcollection")
                            .getStatus());
        });
    }

    /**
     * The HTML preview of the dataset map offers a palette of the collections, the selected list holding the ones being
     * drawn, in drawing order, and the candidate list the rest.
     */
    @Test
    public void testHTMLPreviewPalette() throws Exception {
        Document document = getAsJSoup("ogc/maps/v1/map?f=text/html&collections=wcs:World,cite:Lakes");
        assertEquals(
                List.of("wcs:World", "cite:Lakes"),
                document.select("#selectedCollections option").eachAttr("value"));
        List<String> candidates =
                document.select("#candidateCollections option").eachAttr("value");
        assertThat(candidates, hasItem(NATURE_GROUP));
        assertThat(candidates, not(hasItem("cite:Lakes")));
        // the geometryless layer cannot be drawn, so it is not on offer either
        assertThat(candidates, not(hasItem("cite:Geometryless")));
    }

    /**
     * With no selection the map draws a readable stack rather than the whole catalog: the rasters, the polygon layers,
     * the layer groups, then the lines and the points, each by ascending name
     * ({@code /per/dataset-map/geodata-selection}).
     */
    @Test
    public void testDefaultContentsOrder() throws Exception {
        List<String> contents = defaultContents(30);
        assertEquals(30, contents.size());

        // the rasters are at the bottom, by name, uppercase first as the catalog sorts them
        assertEquals(List.of("wcs:BlueMarble", "wcs:DEM", "wcs:RotatedCad"), contents.subList(0, 3));

        // then the polygon layers, BasicPolygons and Buildings being areas whatever their names suggest
        List<String> polygons = contents.subList(3, contents.indexOf(NATURE_GROUP));
        assertThat(polygons, hasItems("cite:BasicPolygons", "cite:Buildings", "cite:NamedPlaces"));
        assertEquals(sortedByName(polygons), polygons);

        // the layer groups sit above the areas, their contents being curated
        int afterGroups = contents.indexOf(NATURE_GROUP) + 2;
        assertEquals(List.of(NATURE_GROUP, PARENT_GROUP), contents.subList(afterGroups - 2, afterGroups));

        // then the lines, and the points last, so that nothing covers what is thinner than itself
        List<String> lines = contents.subList(afterGroups, contents.indexOf("sf:AggregateGeoFeature"));
        assertThat(lines, hasItems("cite:DividedRoutes", "cgf:Lines", "cite:RoadSegments"));
        assertEquals(sortedByName(lines), lines);
        List<String> points = contents.subList(contents.indexOf("sf:AggregateGeoFeature"), contents.size());
        assertThat(points, hasItems("cite:Bridges", "cgf:MPoints", "cgf:Points"));
        assertEquals(sortedByName(points), points);
    }

    /**
     * The layer group modes decide what a default map is made of: a group nested in another one is drawn as part of its
     * parent, a CONTAINER group cannot be asked for at all so its members are collections of their own, and the members
     * of a group being drawn are not drawn a second time on their own.
     */
    @Test
    public void testDefaultContentsLayerGroups() throws Exception {
        List<String> contents = defaultContents(30);
        assertThat(contents, hasItems(NATURE_GROUP, PARENT_GROUP));
        assertThat(contents, not(hasItem(CHILD_GROUP)));
        assertThat(contents, not(hasItem(CONTAINER_GROUP)));
        // Lakes and Forests are drawn inside the nature group, Ponds inside the group nested in parentGroup
        assertThat(contents, not(hasItem("cite:Lakes")));
        assertThat(contents, not(hasItem("cite:Forests")));
        assertThat(contents, not(hasItem("cite:Ponds")));
        // Bridges is only in the CONTAINER group, which is no collection, so it is drawn on its own
        assertThat(contents, hasItem("cite:Bridges"));
    }

    /** The number of collections a default map draws is capped, the ones at the bottom of the stack winning. */
    @Test
    public void testDefaultContentsCapped() throws Exception {
        // the groups are picked first, then the layers by name, and the three of them are stacked afterwards
        assertEquals(List.of(NATURE_GROUP, PARENT_GROUP, "sf:AggregateGeoFeature"), defaultContents(3));
    }

    /**
     * A group drawn as part of its parent takes up none of the capped slots, childGroup sorting before both of them.
     */
    @Test
    public void testDefaultContentsCappedSkipsNestedGroups() throws Exception {
        assertEquals(List.of(NATURE_GROUP, PARENT_GROUP), defaultContents(2));
    }

    /**
     * The collections a map with no selection draws, in drawing order, read off the palette of its HTML preview. The
     * configured count is restored before returning, so each test sees the default one.
     */
    private List<String> defaultContents(int count) throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.getMetadata().put(MapsSettings.DEFAULT_COLLECTIONS_KEY, count);
        gs.save(wms);
        try {
            Document document = getAsJSoup("ogc/maps/v1/map?f=text/html");
            return document.select("#selectedCollections option").eachAttr("value");
        } finally {
            wms.getMetadata().remove(MapsSettings.DEFAULT_COLLECTIONS_KEY);
            gs.save(wms);
        }
    }

    /** The identifiers sorted by the unprefixed name, the sort key of the WMS capabilities listing. */
    private static List<String> sortedByName(List<String> ids) {
        return ids.stream()
                .sorted(comparing(id -> id.substring(id.indexOf(':') + 1)))
                .toList();
    }

    /**
     * A catalog with more collections than the palette can list is cut, with a warning naming the count: a picker with
     * thousands of entries is of no use, and the collections parameter reaches the ones left out.
     */
    @Test
    public void testHTMLPreviewPaletteCut() throws Exception {
        Document document = getAsJSoup("ogc/maps/v1/map?f=text/html");
        assertEquals(0, document.select("#collectionsCutWarning").size());

        Catalog catalog = getCatalog();
        FeatureTypeInfo lakes = (FeatureTypeInfo)
                catalog.getLayerByName(getLayerId(MockData.LAKES)).getResource();
        List<LayerInfo> added = new ArrayList<>();
        try {
            // one more layer than the palette lists, all sharing the Lakes feature type, no data involved
            while (DatasetCollections.pickable(catalog, MapsService.PALETTE_COLLECTIONS + 1)
                            .size()
                    <= MapsService.PALETTE_COLLECTIONS) {
                added.add(copyOf(catalog, lakes, "copy" + added.size()));
            }
            document = getAsJSoup("ogc/maps/v1/map?f=text/html");
            assertEquals(
                    MapsService.PALETTE_COLLECTIONS,
                    document.select("#candidateCollections option, #selectedCollections option")
                            .size());
            assertThat(
                    document.select("#collectionsCutWarning").text(),
                    containsString("only the first " + MapsService.PALETTE_COLLECTIONS));
        } finally {
            added.forEach(layer -> {
                catalog.remove(layer);
                catalog.remove(layer.getResource());
            });
        }
    }

    /** Publishes another layer on an existing feature type, the cheapest way to grow a catalog in a test. */
    private LayerInfo copyOf(Catalog catalog, FeatureTypeInfo source, String name) {
        FeatureTypeInfo copy = catalog.getFactory().createFeatureType();
        copy.setName(name);
        copy.setNativeName(source.getNativeName());
        copy.setNamespace(source.getNamespace());
        copy.setStore(source.getStore());
        copy.setSRS(source.getSRS());
        copy.setNativeBoundingBox(source.getNativeBoundingBox());
        copy.setLatLonBoundingBox(source.getLatLonBoundingBox());
        copy.setEnabled(true);
        catalog.add(copy);
        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(copy);
        layer.setEnabled(true);
        layer.setDefaultStyle(catalog.getStyleByName("polygon"));
        catalog.add(layer);
        return layer;
    }

    /** The map of a single collection has nothing to choose, so it shows no palette. */
    @Test
    public void testHTMLPreviewNoPaletteOnCollectionMap() throws Exception {
        Document document = getAsJSoup("ogc/maps/v1/collections/cite:Lakes/map?f=text/html");
        assertEquals(0, document.select("#selectedCollections").size());
        assertEquals(0, document.select("#candidateCollections").size());
    }

    /** The HTML landing page links the dataset map preview, and stops doing so when the class is disabled. */
    @Test
    public void testHTMLLandingPageCard() throws Exception {
        Document document = getAsJSoup("ogc/maps/v1?f=html");
        assertEquals(
                "http://localhost:8080/geoserver/ogc/maps/v1/map?f=text%2Fhtml",
                document.select("#htmlDatasetMapLink").attr("href"));
        withConformance(MapsConformance::setDatasetMap, false, () -> {
            assertEquals(
                    0,
                    getAsJSoup("ogc/maps/v1?f=html")
                            .select("#htmlDatasetMapLink")
                            .size());
        });
    }

    /** The GeoServer feature info extension answers for the dataset map too, across the selected collections. */
    @Test
    public void testDatasetFeatureInfo() throws Exception {
        DocumentContext json = getAsJSONPath(datasetInfo("cite:Lakes", null), 200);
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertThat(json.read("features[0].id", String.class), containsString("Lakes"));
        assertEquals("Blue Lake", json.read("features[0].properties.NAME"));
    }

    /** Every selected collection answers, the pixel falling inside both Blue Lake and Green Forest. */
    @Test
    public void testDatasetFeatureInfoMultipleCollections() throws Exception {
        DocumentContext json = getAsJSONPath(datasetInfo("cite:Lakes,cite:Forests", 10), 200);
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals("Blue Lake", json.read("features[0].properties.NAME"));
        assertEquals("Green Forest", json.read("features[1].properties.NAME"));
    }

    /**
     * limit caps the features across all the selected collections, not per collection, and the collections answer in
     * the order they are asked for, so the cut falls at the end of the list.
     */
    @Test
    public void testDatasetFeatureInfoLimit() throws Exception {
        DocumentContext json = getAsJSONPath(datasetInfo("cite:Lakes,cite:Forests", 1), 200);
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals("Blue Lake", json.read("features[0].properties.NAME"));

        json = getAsJSONPath(datasetInfo("cite:Forests,cite:Lakes", 1), 200);
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals("Green Forest", json.read("features[0].properties.NAME"));

        // with no limit at all a single feature comes back, the default being one
        json = getAsJSONPath(datasetInfo("cite:Lakes,cite:Forests", null), 200);
        assertEquals(1, (int) json.read("features.length()", Integer.class));
    }

    /** A limit below one is a client error, naming the parameter. */
    @Test
    public void testDatasetFeatureInfoInvalidLimit() throws Exception {
        DocumentContext json = getAsJSONPath(datasetInfo("cite:Lakes", 0), 400);
        assertEquals(APIException.INVALID_PARAMETER_VALUE, json.read("type"));
        assertThat(json.read("title", String.class), containsString("limit"));
    }

    /** A dataset feature info request on the lake pixel, with an optional limit. */
    private static String datasetInfo(String collections, Integer limit) {
        return "ogc/maps/v1/map/info?f=application/json&" + WINDOW + "&collections=" + collections + "&i=" + LAKE_X
                + "&j=" + LAKE_Y + (limit == null ? "" : "&limit=" + limit);
    }

    /** Fails unless the two images have the same size and the very same pixels. */
    private static void assertSameImage(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        assertEquals(0, differentPixels(expected, actual));
    }

    /** The number of pixels the two images disagree on. */
    private static int differentPixels(BufferedImage a, BufferedImage b) {
        int different = 0;
        for (int x = 0; x < a.getWidth(); x++) {
            for (int y = 0; y < a.getHeight(); y++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) different++;
            }
        }
        return different;
    }
}
