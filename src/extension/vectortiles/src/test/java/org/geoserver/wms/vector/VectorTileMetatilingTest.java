/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import no.ecc.vectortile.VectorTileDecoder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.util.factory.Hints;
import org.geowebcache.layer.TileLayer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.mock.web.MockHttpServletResponse;

/** Test vector tiles cached via GWC with metatiling enabled/disabled. */
public class VectorTileMetatilingTest extends GeoServerSystemTestSupport {

    enum CacheResult {
        HIT,
        MISS
    }

    private static final String LAYER_NAME = "cite:BasicPolygons";
    private static final String GRIDSET_ID = "EPSG:4326";
    private static final String MVT_FORMAT = "application/vnd.mapbox-vector-tile";
    private static final String GEOJSON_FORMAT = "application/json;type=geojson";
    private static final String[][] EXPECTED_GEOMETRIES = {
        {
            "POLYGON ((258.75 37.0625, 258.75 258.75, 37.0625 258.75, 258.75 37.0625))",
            "POLYGON ((42.5625 -2.75, 258.75 -2.75, 258.75 258.75, -2.75 258.75, -2.75 42.5625, 42.5625 -2.75))"
        },
        {
            "POLYGON ((42.5625 -2.75, 258.75 -2.75, 258.75 258.75, -2.75 258.75, -2.75 42.5625, 42.5625 -2.75))",
            "POLYGON ((-2.75 -2.75, 258.75 -2.75, 258.75 258.75, -2.75 258.75, -2.75 -2.75))"
        }
    };
    private static final CacheResult[][] EXPECTED_CACHE_RESULTS_WITH_METATILING = {
        {CacheResult.MISS, CacheResult.HIT},
        {CacheResult.HIT, CacheResult.HIT}
    };
    private static final CacheResult[][] EXPECTED_CACHE_RESULTS_WITHOUT_METATILING = {
        {CacheResult.MISS, CacheResult.MISS},
        {CacheResult.MISS, CacheResult.MISS}
    };

    private GWC gwc;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Hints.putSystemDefault(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, true);
    }

    @Before
    public void init() throws Exception {
        gwc = GWC.get();
        assertNotNull(getCatalog().getLayerByName(LAYER_NAME));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    /** Configures the GWC layer for MVT and sets metatiling parameters. */
    private void configureGwcLayer(int metaW, int metaH) throws Exception {
        configureGwcLayer(metaW, metaH, MVT_FORMAT);
    }

    /** Configures the GWC layer for the given format and sets metatiling parameters. */
    private void configureGwcLayer(int metaW, int metaH, String format) throws Exception {
        GWCConfig cfg = gwc.getConfig();
        cfg.setDirectWMSIntegrationEnabled(true);
        gwc.saveConfig(cfg);

        LayerInfo layerInfo = getCatalog().getLayerByName(LAYER_NAME);
        assertNotNull(layerInfo);
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) gwc.getTileLayerByName(LAYER_NAME);
        if (tileLayer == null) {
            gwc.add(new GeoServerTileLayer(layerInfo, gwc.getConfig(), gwc.getGridSetBroker()));
            tileLayer = (GeoServerTileLayer) gwc.getTileLayerByName(LAYER_NAME);
        }

        GeoServerTileLayerInfo info = tileLayer.getInfo();
        info.setEnabled(true);
        info.getMimeFormats().clear();
        info.getMimeFormats().add(format);

        // Metatiling settings
        info.setMetaTilingX(metaW);
        info.setMetaTilingY(metaH);
        info.setGutter(0);
        gwc.save(tileLayer);
        truncateCache();
    }

    private void truncateCache() {
        TileLayer tl = gwc.getTileLayerByName(LAYER_NAME);
        assertNotNull(tl);
        gwc.layerRemoved(LAYER_NAME);
    }

    @Test
    public void testMetatilingDisabled() throws Exception {
        configureGwcLayer(1, 1);
        assertTiles(EXPECTED_CACHE_RESULTS_WITHOUT_METATILING);
    }

    @Test
    public void testMetatilingEnabled() throws Exception {
        configureGwcLayer(2, 2);
        assertTiles(EXPECTED_CACHE_RESULTS_WITH_METATILING);
    }

    private void assertTiles(CacheResult[][] cacheResults) throws Exception {
        int z = 9, x = 510, y = 254;
        VectorTileDecoder decoder = new VectorTileDecoder();

        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < 2; i++) {
                MockHttpServletResponse resp = getTile(z, x + i, y + j);
                assertEquals(200, resp.getStatus());
                byte[] data = resp.getContentAsByteArray();
                assertCacheResult(resp, cacheResults[j][i]);
                assertTrue(data.length > 0);
                assertFeature(decoder, data, i, j);
            }
        }
    }

    private MockHttpServletResponse getTile(int z, int x, int y) throws Exception {
        return getTile(z, x, y, MVT_FORMAT);
    }

    private MockHttpServletResponse getTile(int z, int x, int y, String format) throws Exception {
        String url = "gwc/service/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0"
                + "&LAYER=" + LAYER_NAME
                + "&STYLE="
                + "&TILEMATRIXSET=" + GRIDSET_ID
                + "&TILEMATRIX=" + GRIDSET_ID + ":" + z
                + "&TILEROW=" + y
                + "&TILECOL=" + x
                + "&FORMAT=" + format;

        MockHttpServletResponse resp = getAsServletResponse(url);
        assertEquals(200, resp.getStatus());
        assertNotNull(resp.getContentType());
        String baseFormat = format.split(";")[0];
        assertTrue(
                "Unexpected content-type: " + resp.getContentType(),
                resp.getContentType().startsWith(baseFormat));
        return resp;
    }

    private void assertCacheResult(MockHttpServletResponse resp, CacheResult expected) {
        // Header names vary slightly across versions / servlet containers
        String v = resp.getHeader("geowebcache-cache-result");

        assertNotNull("No cache result header found; available headers: " + resp.getHeaderNames(), v);

        String norm = v.trim().toUpperCase();
        assertEquals("Unexpected cache result header value: " + v, expected.name(), norm);
    }

    private void assertFeature(VectorTileDecoder decoder, byte[] data, int column, int row)
            throws IOException, ParseException {
        VectorTileDecoder.FeatureIterable decoded = decoder.decode(data);
        List<VectorTileDecoder.Feature> list = decoded.asList();
        VectorTileDecoder.Feature feature = list.get(0);
        WKTReader reader = new WKTReader();
        assertTrue(feature.getGeometry().equals(reader.read(EXPECTED_GEOMETRIES[row][column])));
    }

    /**
     * GeoJSON tiles carry map coordinates, so the meta-tile split has to work in map coordinates too. A meta-tiled
     * request must place the same features in the same tiles as a request served one tile at a time.
     */
    @Test
    public void testGeoJsonMetatilingMatchesSingleTiles() throws Exception {
        configureGwcLayer(1, 1, GEOJSON_FORMAT);
        List<String> singleTiles = describeGeoJsonTiles();
        assertEquals(
                List.of(
                        "BasicPolygons.1107531493630 [-0.67,0.33 -0.33,0.67]",
                        "BasicPolygons.1107531493630 [-0.37,0.33 0.02,0.72]",
                        "BasicPolygons.1107531493630 [-0.72,-0.02 -0.33,0.37]",
                        "BasicPolygons.1107531493630 [-0.37,-0.02 0.02,0.37]"),
                singleTiles);

        configureGwcLayer(2, 2, GEOJSON_FORMAT);
        assertEquals(singleTiles, describeGeoJsonTiles());
    }

    /**
     * Describes the four GeoJSON tiles of the 2x2 block used by the other tests, in row major order, as feature id plus
     * rounded geometry bounds. Coordinates are rounded because the meta-tiled path clips against a slightly different
     * buffer than the single tile one.
     */
    private List<String> describeGeoJsonTiles() throws Exception {
        int z = 9, x = 510, y = 254;
        List<String> descriptions = new ArrayList<>();
        for (int j = 0; j < 2; j++) {
            for (int i = 0; i < 2; i++) {
                MockHttpServletResponse resp = getTile(z, x + i, y + j, GEOJSON_FORMAT);
                JsonObject tile =
                        JsonParser.parseString(resp.getContentAsString()).getAsJsonObject();
                for (JsonElement feature : tile.getAsJsonArray("features")) {
                    descriptions.add(describeFeature(feature.getAsJsonObject()));
                }
            }
        }
        return descriptions;
    }

    private String describeFeature(JsonObject feature) {
        Envelope bounds = new Envelope();
        collectCoordinates(feature.getAsJsonObject("geometry").getAsJsonArray("coordinates"), bounds);
        return String.format(
                Locale.ENGLISH,
                "%s [%.2f,%.2f %.2f,%.2f]",
                feature.get("id").getAsString(),
                bounds.getMinX(),
                bounds.getMinY(),
                bounds.getMaxX(),
                bounds.getMaxY());
    }

    /** Walks the nested GeoJSON coordinate arrays, expanding the given envelope with every position found. */
    private void collectCoordinates(JsonArray coordinates, Envelope bounds) {
        if (coordinates.get(0).isJsonPrimitive()) {
            bounds.expandToInclude(
                    coordinates.get(0).getAsDouble(), coordinates.get(1).getAsDouble());
            return;
        }
        for (JsonElement nested : coordinates) {
            collectCoordinates(nested.getAsJsonArray(), bounds);
        }
    }
}
