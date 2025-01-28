/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test that validates the 8 scenarios (A-H) from the format options matrix representing different combinations of
 * mapmlusefeatures, mapmlusemultiextents, and mapmlusetiles.
 *
 * <p>Based on the requirements table, each scenario tests specific combinations of: - mapmlusefeatures (false/true) -
 * mapmlusemultiextents (false/true) - mapmlusetiles (false/true)
 */
public class MapMLFormatOptionsMatrixTest extends MapMLTestSupport {

    // Web Mercator full extent bounds for OSMTILE
    private static final String BBOX = "-20037508.342787,-20037508.342787,20037508.342787,20037508.342787";
    private static final String SRS = "MapML:OSMTILE";
    private static final String WIDTH = "768";
    private static final String HEIGHT = "768";

    // Layer names
    private static final String RASTER_LAYER = "wcs:World";
    private static final String MIXED_GROUP_CACHED = "mixedGroupCached";
    private static final String MIXED_GROUP_NOCACHE = "mixedGroupNoCache";

    // Format options boolean values
    private static final boolean USE_FEATURES_TRUE = true;
    private static final boolean USE_FEATURES_FALSE = false;
    private static final boolean USE_MULTI_EXTENTS_TRUE = true;
    private static final boolean USE_MULTI_EXTENTS_FALSE = false;
    private static final boolean USE_TILES_TRUE = true;
    private static final boolean USE_TILES_FALSE = false;

    private XpathEngine xpath;

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        setupTestLayers();
    }

    @Before
    public void setUp() throws Exception {
        xpath = XMLUnit.newXpathEngine();
    }

    /** Set up test layers with different caching configurations */
    private void setupTestLayers() {
        try {
            createMixedLayerGroup(MIXED_GROUP_CACHED);
            createMixedLayerGroup(MIXED_GROUP_NOCACHE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test layers", e);
        }
    }

    private void createMixedLayerGroup(String name) throws Exception {
        Catalog catalog = getCatalog();
        CatalogBuilder builder = new CatalogBuilder(catalog);

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName(name);
        lg.getLayers().add(catalog.getLayerByName(MockData.BASIC_POLYGONS.getLocalPart()));
        lg.getLayers().add(catalog.getLayerByName(MockData.WORLD.getLocalPart()));

        builder.calculateLayerGroupBounds(lg, DefaultGeographicCRS.WGS84);
        catalog.add(lg);

        // Note: Tile caching is enabled per test as needed, not during setup
    }

    /**
     * Test Scenario A: mapmlusefeatures=false, mapmlusemultiextents=false, mapmlusetiles=false Expected: Single
     * map-extent with traditional WMS image client, rel=image, hidden="hidden"
     */
    @Test
    public void testScenarioA() throws Exception {
        String xml = getAsString(MIXED_GROUP_NOCACHE, USE_FEATURES_FALSE, USE_MULTI_EXTENTS_FALSE, USE_TILES_FALSE);
        Document doc = XMLUnit.buildTestDocument(xml);

        // Should have exactly one map-extent with hidden="hidden"
        assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
        assertXpathExists("//html:map-extent[@hidden='hidden']", doc);

        // Should have rel="image" link
        assertXpathExists("//html:map-link[@rel='image']", doc);

        // Should NOT have rel="features" or rel="tile" links
        assertXpathNotExists("//html:map-link[@rel='features']", doc);
        assertXpathNotExists("//html:map-link[@rel='tile']", doc);
    }

    /**
     * Test Scenario B: mapmlusefeatures=false, mapmlusemultiextents=true, mapmlusetiles=false Expected: Multiple
     * map-extents (one per layer), traditional WMS image client, rel=image, no hidden attribute
     */
    @Test
    public void testScenarioB() throws Exception {
        String xml = getAsString(MIXED_GROUP_NOCACHE, USE_FEATURES_FALSE, USE_MULTI_EXTENTS_TRUE, USE_TILES_FALSE);
        Document doc = XMLUnit.buildTestDocument(xml);

        // Should have map-extent(s) without hidden="hidden" attribute
        assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
        assertXpathNotExists("//html:map-extent[@hidden]", doc);

        // Should have rel="image" link
        assertXpathExists("//html:map-extent/html:map-link[@rel='image']", doc);
    }

    /**
     * Test Scenario C.1: mapmlusefeatures=false, mapmlusemultiextents=false, mapmlusetiles=true Expected: Single
     * map-extent with tile-shaped WMS GetMap (no tile cache), rel=tile
     */
    @Test
    public void testScenarioC1_NoTileCache() throws Exception {
        String xml = getAsString(MIXED_GROUP_NOCACHE, USE_FEATURES_FALSE, USE_MULTI_EXTENTS_FALSE, USE_TILES_TRUE);
        Document doc = XMLUnit.buildTestDocument(xml);

        // Should have exactly one map-extent
        assertXpathEvaluatesTo("1", "count(//html:map-extent[@hidden])", doc);

        assertXpathExists("//html:map-extent/html:map-link[@rel='tile']", doc);
        assertXpathNotExists("//html:map-extent/html:map-link[@rel='tile'][@type]", doc);
    }

    /**
     * Test Scenario C.2: mapmlusefeatures=false, mapmlusemultiextents=false, mapmlusetiles=true Expected: Single
     * map-extent with GetTile URL template (with tile cache), rel=tile
     */
    @Test
    public void testScenarioC2_WithTileCache() throws Exception {
        enableTileCaching(new QName(MockData.CITE_URI, MIXED_GROUP_CACHED), getCatalog());

        try {
            String xml = getAsString(MIXED_GROUP_CACHED, USE_FEATURES_FALSE, USE_MULTI_EXTENTS_FALSE, USE_TILES_TRUE);
            Document doc = XMLUnit.buildTestDocument(xml);

            // Should have exactly one map-extent
            assertXpathEvaluatesTo("1", "count(//html:map-extent[@hidden])", doc);

            // Should have rel="tile" link with GetTile URL
            assertXpathExists("//html:map-link[@rel='tile']", doc);
            assertXpathNotExists("//html:map-link[@rel='tile'][@type]", doc);

            String tileUrl = xpath.evaluate("//html:map-link[@rel='tile']/@tref", doc);
            assertFalse("Tile URL should not contain mapmlfeatures:true", tileUrl.contains("mapmlfeatures:true"));
            assertTrue(
                    "Tile URL for cached layer (group) should contain GetTile request template",
                    tileUrl.contains("GetTile"));
        } finally {
            disableTileCaching(new QName(MockData.CITE_URI, MIXED_GROUP_CACHED), getCatalog());
        }
    }

    /**
     * Test Scenario D.1: mapmlusefeatures=false, mapmlusemultiextents=true, mapmlusetiles=true Expected: Multiple
     * map-extents, tile-shaped WMS GetMap (no tile cache), no hidden attribute
     */
    @Test
    public void testScenarioD1_NoTileCache() throws Exception {
        String xml = getAsString(MIXED_GROUP_NOCACHE, USE_FEATURES_FALSE, USE_MULTI_EXTENTS_TRUE, USE_TILES_TRUE);
        Document doc = XMLUnit.buildTestDocument(xml);

        // Should have map-extent(s) without hidden="hidden" attribute
        assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
        assertXpathNotExists("//html:map-extent[@hidden]", doc);

        // Should have tile-related links
        assertXpathExists("//html:map-extent/html:map-link[@rel='tile']", doc);
        // image tiles don't require the type attribute, they are the default
        assertXpathNotExists("//html:map-extent/html:map-link[@rel='tile'][@type]", doc);

        // Should NOT have rel="features" link
        assertXpathNotExists("//html:map-link[@rel='features']", doc);
        String tileUrl = xpath.evaluate("//html:map-link[@rel='tile']/@tref", doc);
        assertFalse("Tile URL should not contain mapmlfeatures:true", tileUrl.contains("mapmlfeatures:true"));
        assertTrue(
                "Tile URL for non-cached layer (group) should contain GetMap request template",
                tileUrl.contains("GetMap"));
    }

    /**
     * Test Scenario D.2: mapmlusefeatures=false, mapmlusemultiextents=true, mapmlusetiles=true Expected: Multiple
     * map-extents, GetTile URL template (with tile cache), no hidden attribute
     */
    @Test
    public void testScenarioD2_WithTileCache() throws Exception {
        enableTileCaching(new QName(MockData.CITE_URI, MIXED_GROUP_CACHED), getCatalog());

        try {
            String xml = getAsString(MIXED_GROUP_CACHED, USE_FEATURES_FALSE, USE_MULTI_EXTENTS_TRUE, USE_TILES_TRUE);
            Document doc = XMLUnit.buildTestDocument(xml);

            // Should have map-extent(s) without hidden="hidden" attribute
            assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
            assertXpathNotExists("//html:map-extent[@hidden]", doc);

            // Should have rel="tile" link
            assertXpathExists("//html:map-extent/html:map-link[@rel='tile']", doc);
            // image tiles don't require a type, they are the "default"
            assertXpathNotExists("//html:map-extent/html:map-link[@rel='tile'][@type]", doc);

            // Should NOT have rel="features" link
            assertXpathNotExists("//html:map-extent/html:map-link[@rel='features']", doc);

            String tileUrl = xpath.evaluate("//html:map-link[@rel='tile']/@tref", doc);
            assertFalse("Tile URL should not contain mapmlfeatures:true", tileUrl.contains("mapmlfeatures:true"));
            assertTrue(
                    "Tile URL for cached layer (group) should contain GetTile request template",
                    tileUrl.contains("GetTile"));
        } finally {
            disableTileCaching(new QName(MockData.CITE_URI, MIXED_GROUP_CACHED), getCatalog());
        }
    }

    /**
     * Test Scenario E: mapmlusefeatures=true, mapmlusemultiextents=false, mapmlusetiles=false Expected: Single
     * map-extent with traditional WMS image client + URL template rel=features
     */
    @Test
    public void testScenarioE() throws Exception {
        String xml = getAsString(MIXED_GROUP_NOCACHE, USE_FEATURES_TRUE, USE_MULTI_EXTENTS_FALSE, USE_TILES_FALSE);
        Document doc = XMLUnit.buildTestDocument(xml);

        // Should have exactly one map-extent
        assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
        // and it should be hidden
        assertXpathEvaluatesTo("1", "count(//html:map-extent[@hidden])", doc);

        // Should have rel="features" link
        assertXpathExists("//html:map-extent/html:map-link[@rel='features']", doc);

        // Should NOT have rel="image" link when features are enabled
        assertXpathNotExists("//html:map-link[@rel='image']", doc);
        // Should have rel="features" link for vector layers
        assertXpathExists("//html:map-extent/html:map-link[@rel='features']", doc);

        // URL template should contain mapmlfeatures:true
        String featuresUrl = xpath.evaluate("//html:map-extent/html:map-link[@rel='features']/@tref", doc);
        assertTrue("rel=features URL should contain mapmlfeatures:true", featuresUrl.contains("mapmlfeatures:true"));
        assertTrue("rel=features URL should contain GetMap", featuresUrl.contains("GetMap"));
    }

    /**
     * Test Scenario F.1: mapmlusefeatures=true, mapmlusemultiextents=true, mapmlusetiles=false Expected: Multiple
     * map-extents, raster layers as rel=features tref="...request=GetMap..."
     */
    @Test
    public void testScenarioF1_RasterLayerNoCache() throws Exception {
        String xml = getAsString(RASTER_LAYER, USE_FEATURES_TRUE, USE_MULTI_EXTENTS_TRUE, USE_TILES_FALSE);
        Document doc = XMLUnit.buildTestDocument(xml);

        // Should have map-extent(s) without hidden="hidden" attribute
        assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
        assertXpathNotExists("//html:map-extent[@hidden]", doc);

        // Should have rel="features" link for vector layers
        assertXpathExists("//html:map-extent/html:map-link[@rel='features']", doc);
        // URL template should contain mapmlfeatures:true
        String featuresUrl = xpath.evaluate("//html:map-extent/html:map-link[@rel='features']/@tref", doc);
        assertTrue("rel=features URL should contain mapmlfeatures:true", featuresUrl.contains("mapmlfeatures:true"));
        assertTrue("rel=features URL should contain GetMap", featuresUrl.contains("GetMap"));
    }

    /**
     * Test Scenario F.2: mapmlusefeatures=true, mapmlusemultiextents=true, mapmlusetiles=false Expected: Multiple
     * map-extents, mixed vector/raster handling with features for vectors
     */
    @Test
    public void testScenarioF2_LayerGroupNoCache() throws Exception {
        String xml = getAsString(MIXED_GROUP_NOCACHE, USE_FEATURES_TRUE, USE_MULTI_EXTENTS_TRUE, USE_TILES_FALSE);
        Document doc = XMLUnit.buildTestDocument(xml);

        // Should have map-extent(s) without hidden="hidden" attribute
        assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
        assertXpathNotExists("//html:map-extent[@hidden]", doc);

        // Should have rel="features" link (mixed response for layer group)
        assertXpathExists("//html:map-extent/html:map-link[@rel='features']", doc);
        assertXpathExists("//html:map-extent/html:map-link[@rel='features']", doc);

        // URL template should contain mapmlfeatures:true
        String featuresUrl = xpath.evaluate("//html:map-extent/html:map-link[@rel='features']/@tref", doc);
        assertTrue("rel=features URL should contain mapmlfeatures:true", featuresUrl.contains("mapmlfeatures:true"));
        assertTrue("rel=features URL should contain GetMap", featuresUrl.contains("GetMap"));
    }

    /**
     * Test Scenario G.1: mapmlusefeatures=true, mapmlusemultiextents=false, mapmlusetiles=true Expected: Single
     * map-extent with tile-shaped GetMap + features (no tile cache)
     */
    @Test
    public void testScenarioG1_NoTileCache() throws Exception {
        String xml = getAsString(MIXED_GROUP_NOCACHE, USE_FEATURES_TRUE, USE_MULTI_EXTENTS_FALSE, USE_TILES_TRUE);
        Document doc = XMLUnit.buildTestDocument(xml);

        // Should have exactly one map-extent
        assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);

        // and it should be hidden
        assertXpathEvaluatesTo("1", "count(//html:map-extent[@hidden])", doc);

        // Should have rel="tile" with type="text/mapml"
        assertXpathExists("//html:map-link[@rel='tile'][@type='text/mapml']", doc);

        // URL template should contain mapmlfeatures:true
        String tileUrl = xpath.evaluate("//html:map-link[@rel='tile']/@tref", doc);
        assertTrue("Tile URL should contain mapmlfeatures:true", tileUrl.contains("mapmlfeatures:true"));
        assertTrue("Tile URL should contain tile-shaped GetMap", tileUrl.contains("GetMap"));
    }

    /**
     * Test Scenario G.2: mapmlusefeatures=true, mapmlusemultiextents=false, mapmlusetiles=true Expected: Single
     * map-extent with GetTile + features (with tile cache)
     */
    @Test
    public void testScenarioG2_WithTileCache() throws Exception {
        enableTileCaching(new QName(MockData.CITE_URI, MIXED_GROUP_CACHED), getCatalog());

        try {
            String xml = getAsString(MIXED_GROUP_CACHED, USE_FEATURES_TRUE, USE_MULTI_EXTENTS_FALSE, USE_TILES_TRUE);
            Document doc = XMLUnit.buildTestDocument(xml);

            // Should have exactly one map-extent
            assertXpathEvaluatesTo("1", "count(//html:map-extent[@hidden])", doc);

            // Should have rel="tile" with type="text/mapml"
            assertXpathExists("//html:map-link[@rel='tile'][@type='text/mapml']", doc);

            // URL template should contain GetTile
            String tileUrl = xpath.evaluate("//html:map-link[@rel='tile']/@tref", doc);
            assertTrue("Should use GetTile when tile cache available", tileUrl.contains("GetTile"));

        } finally {
            disableTileCaching(new QName(MockData.CITE_URI, MIXED_GROUP_CACHED), getCatalog());
        }
    }

    /**
     * Test Scenario H.1: mapmlusefeatures=true, mapmlusemultiextents=true, mapmlusetiles=true Expected: Multiple
     * map-extents with tile-shaped requests + features (raster no cache)
     */
    @Test
    public void testScenarioH1_RasterNoCache() throws Exception {
        String xml = getAsString(RASTER_LAYER, USE_FEATURES_TRUE, USE_MULTI_EXTENTS_TRUE, USE_TILES_TRUE);
        Document doc = XMLUnit.buildTestDocument(xml);

        // Should have map-extent(s) without hidden="hidden" attribute
        assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
        assertXpathNotExists("//html:map-extent[@hidden]", doc);
        assertXpathNotExists("//html:map-extent/html:map-link[@rel='image']", doc);
        // For raster layers: should have tile-shaped GetMap
        assertXpathExists("//html:map-extent/html:map-link[@rel='tile'][@type='text/mapml']", doc);

        // URL template should contain mapmlfeatures:true
        String tileUrl = xpath.evaluate("//html:map-link[@rel='tile']/@tref", doc);
        assertTrue("Tile URL should contain mapmlfeatures:true", tileUrl.contains("mapmlfeatures:true"));
        assertTrue("Tile URL should contain tile-shaped GetMap", tileUrl.contains("GetMap"));
    }

    /**
     * Test Scenario H.2: mapmlusefeatures=true, mapmlusemultiextents=true, mapmlusetiles=true Expected: Multiple
     * map-extents with GetTile + features (raster with cache)
     */
    @Test
    public void testScenarioH2_RasterWithCache() throws Exception {
        enableTileCaching(new QName(MockData.CITE_URI, "World"), getCatalog());

        try {
            String xml = getAsString(RASTER_LAYER, USE_FEATURES_TRUE, USE_MULTI_EXTENTS_TRUE, USE_TILES_TRUE);
            Document doc = XMLUnit.buildTestDocument(xml);

            // Should have map-extent(s) without hidden="hidden" attribute
            assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
            assertXpathNotExists("//html:map-extent[@hidden]", doc);

            // For cached raster: should have GetTile URL
            assertXpathExists("//html:map-link[@rel='tile']", doc);

            // Verify that the tile link contains "GetTile" and has type=text/mapml
            String tileUrl = xpath.evaluate("//html:map-link[@rel='tile'][@type='text/mapml']/@tref", doc);
            assertTrue("Tile URL should contain GetTile", tileUrl.contains("GetTile"));

            // Verify that the tile link uses text/mapml for rasters with mapmlusefeatures:true
            assertTrue("Tile URL should use text/mapml for vector tiles", tileUrl.contains("text/mapml"));

        } finally {
            disableTileCaching(new QName(MockData.CITE_URI, "World"), getCatalog());
        }
    }

    /**
     * Test Scenario H.3: mapmlusefeatures=true, mapmlusemultiextents=true, mapmlusetiles=true Expected: Multiple
     * map-extents with tile-shaped requests + features (layer group no cache)
     */
    @Test
    public void testScenarioH3_NoTileCache() throws Exception {
        String xml = getAsString(MIXED_GROUP_NOCACHE, USE_FEATURES_TRUE, USE_MULTI_EXTENTS_TRUE, USE_TILES_TRUE);
        Document doc = XMLUnit.buildTestDocument(xml);

        // Should have map-extent(s) without hidden="hidden" attribute
        assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
        assertXpathNotExists("//html:map-extent[@hidden]", doc);

        // Should have rel="tile" with type="text/mapml"
        assertXpathExists("//html:map-link[@rel='tile'][@type='text/mapml']", doc);

        // URL template should contain mapmlfeatures:true
        String tileUrl = xpath.evaluate("//html:map-link[@rel='tile']/@tref", doc);
        assertTrue("Tile URL should contain mapmlfeatures:true", tileUrl.contains("mapmlfeatures:true"));
    }

    /**
     * Test Scenario H.4: mapmlusefeatures=true, mapmlusemultiextents=true, mapmlusetiles=true Expected: Multiple
     * map-extents with GetTile + features (vector with cache)
     */
    @Test
    public void testScenarioH4_WithTileCache() throws Exception {
        enableTileCaching(new QName(MockData.CITE_URI, MIXED_GROUP_CACHED), getCatalog());

        try {
            String xml = getAsString(MIXED_GROUP_CACHED, USE_FEATURES_TRUE, USE_MULTI_EXTENTS_TRUE, USE_TILES_TRUE);
            Document doc = XMLUnit.buildTestDocument(xml);

            // Should have map-extent(s) without hidden="hidden" attribute
            assertXpathEvaluatesTo("1", "count(//html:map-extent)", doc);
            assertXpathNotExists("//html:map-extent[@hidden]", doc);

            // Should have rel="tile" with type="text/mapml"
            assertXpathExists("//html:map-link[@rel='tile'][@type='text/mapml']", doc);

            // URL should use GetTile and contain proper format
            String tileUrl = xpath.evaluate("//html:map-link[@rel='tile']/@tref", doc);
            assertTrue("Should use GetTile when cache available", tileUrl.contains("GetTile"));

        } finally {
            disableTileCaching(new QName(MockData.CITE_URI, MIXED_GROUP_CACHED), getCatalog());
        }
    }

    private String getAsString(String layers, boolean useFeatures, boolean multiExtent, boolean useTiles)
            throws Exception {
        return new MapMLWMSRequest()
                .name(layers)
                .bbox(BBOX)
                .srs(SRS)
                .width(WIDTH)
                .height(HEIGHT)
                .createFeatureLinks(useFeatures)
                .multiExtent(multiExtent)
                .tile(useTiles)
                .getAsString();
    }
}
