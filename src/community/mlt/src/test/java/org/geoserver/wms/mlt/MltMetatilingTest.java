/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.mlt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.catalog.LayerInfo;
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
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.io.WKTReader;
import org.maplibre.mlt.data.Feature;
import org.maplibre.mlt.data.Layer;
import org.maplibre.mlt.decoder.MltDecoder;
import org.springframework.mock.web.MockHttpServletResponse;

/** Checks MapLibre tiles served and cached through GeoWebCache, with and without metatiling. */
public class MltMetatilingTest extends GeoServerSystemTestSupport {

    private static final String LAYER_NAME = "cite:BasicPolygons";

    private static final String GRIDSET_ID = "EPSG:4326";

    private static final String FEATURE_ID = "BasicPolygons.1107531493630";

    /**
     * The geometry expected in each tile of the 2x2 block, in the 0 to 256 tile space the Mapbox tiles of the vector
     * tiles extension use, indexed by row and column.
     */
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

    /** MLT tiles are encoded at the oversampled extent, so the expected coordinates scale up by this factor. */
    private static final int OVERSAMPLE = 16;

    /** Vertices are rounded to whole tile coordinates, so allow a coordinate of slack. */
    private static final double TOLERANCE = 1.0;

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

    @Test
    public void testMetatilingDisabled() throws Exception {
        configureGwcLayer(1, 1);
        assertTiles("MISS", "MISS", "MISS", "MISS");
    }

    /** With a 2x2 metatile the first request builds all four tiles, so the other three are cache hits. */
    @Test
    public void testMetatilingEnabled() throws Exception {
        configureGwcLayer(2, 2);
        assertTiles("MISS", "HIT", "HIT", "HIT");
    }

    private void assertTiles(String... expectedCacheResults) throws Exception {
        int z = 9, x = 510, y = 254;
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                MockHttpServletResponse response = getTile(z, x + column, y + row);
                assertEquals(expectedCacheResults[row * 2 + column], cacheResult(response));
                assertTile(response, row, column);
            }
        }
    }

    /** Decodes one tile and checks it holds the expected feature, in the expected place. */
    private void assertTile(MockHttpServletResponse response, int row, int column) throws Exception {
        List<Layer> layers =
                MltDecoder.decodeMlTile(response.getContentAsByteArray()).layers();
        assertEquals(1, layers.size());
        Layer polygons = layers.get(0);
        assertEquals("BasicPolygons", polygons.name());
        assertEquals(1, polygons.features().size());

        Feature feature = polygons.features().get(0);
        assertEquals(Long.parseLong(FEATURE_ID.substring(FEATURE_ID.indexOf('.') + 1)), feature.id());
        // the MLT encoder writes polygon rings with the winding its spec asks for, so compare normalized
        Geometry expected = oversampled(EXPECTED_GEOMETRIES[row][column]).norm();
        Geometry actual = feature.geometry().norm();
        assertTrue(
                "tile " + row + "," + column + " holds " + actual + " instead of " + expected,
                expected.equalsExact(actual, TOLERANCE));
    }

    /** Scales a geometry from the 0 to 256 tile space up to the oversampled MLT extent. */
    private static Geometry oversampled(String wkt) throws Exception {
        Geometry geometry = new WKTReader().read(wkt);
        geometry.apply(AffineTransformation.scaleInstance(OVERSAMPLE, OVERSAMPLE));
        geometry.geometryChanged();
        return geometry;
    }

    private MockHttpServletResponse getTile(int z, int x, int y) throws Exception {
        String url = "gwc/service/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0"
                + "&LAYER=" + LAYER_NAME
                + "&STYLE="
                + "&TILEMATRIXSET=" + GRIDSET_ID
                + "&TILEMATRIX=" + GRIDSET_ID + ":" + z
                + "&TILEROW=" + y
                + "&TILECOL=" + x
                + "&FORMAT=" + MltTileBuilderFactory.MIME_TYPE;
        MockHttpServletResponse response = getAsServletResponse(url);
        assertEquals(200, response.getStatus());
        assertEquals(MltTileBuilderFactory.MIME_TYPE, response.getContentType());
        return response;
    }

    private static String cacheResult(MockHttpServletResponse response) {
        String header = response.getHeader("geowebcache-cache-result");
        assertNotNull("No cache result header, got " + response.getHeaderNames(), header);
        return header.trim().toUpperCase();
    }

    /** Publishes the layer through GeoWebCache in MLT format, with the given metatiling factors. */
    private void configureGwcLayer(int metaWidth, int metaHeight) throws Exception {
        GWCConfig config = gwc.getConfig();
        config.setDirectWMSIntegrationEnabled(true);
        gwc.saveConfig(config);

        LayerInfo layerInfo = getCatalog().getLayerByName(LAYER_NAME);
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) gwc.getTileLayerByName(LAYER_NAME);
        if (tileLayer == null) {
            gwc.add(new GeoServerTileLayer(layerInfo, gwc.getConfig(), gwc.getGridSetBroker()));
            tileLayer = (GeoServerTileLayer) gwc.getTileLayerByName(LAYER_NAME);
        }

        GeoServerTileLayerInfo info = tileLayer.getInfo();
        info.setEnabled(true);
        info.getMimeFormats().clear();
        info.getMimeFormats().add(MltTileBuilderFactory.MIME_TYPE);
        info.setMetaTilingX(metaWidth);
        info.setMetaTilingY(metaHeight);
        info.setGutter(0);
        gwc.save(tileLayer);

        TileLayer layer = gwc.getTileLayerByName(LAYER_NAME);
        assertNotNull(layer);
        gwc.layerRemoved(LAYER_NAME);
    }
}
