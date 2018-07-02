/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.gwc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import java.util.Map;
import org.geogig.geoserver.GeoGigTestData;
import org.geogig.geoserver.GeoGigTestData.CatalogBuilder;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.conveyor.Conveyor.CacheResult;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.simple.SimpleFeature;

/** Integration test for GeoServer cached layers using the GWC REST API */
@TestSetup(run = TestSetupFrequency.ONCE)
public class GWCIntegrationTest extends GeoServerSystemTestSupport {

    private static GWC mediator;

    private static GeoServerTileLayer pointsLayer;

    private static GeoServerTileLayer linesLayer;

    private static StorageBroker storageBroker;

    @Rule public GeoGigTestData geogigData = new GeoGigTestData();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        geogigData
                .init() //
                .config("user.name", "gabriel") //
                .config("user.email", "gabriel@test.com") //
                .createTypeTree("lines", "geom:LineString:srid=4326") //
                .createTypeTree("points", "geom:Point:srid=4326") //
                .add() //
                .commit("created type trees") //
                .get();

        geogigData.insert(
                "points", //
                "p1=geom:POINT(0 0)", //
                "p2=geom:POINT(1 1)", //
                "p3=geom:POINT(2 2)");

        geogigData.insert(
                "lines", //
                "l1=geom:LINESTRING(-10 0, 10 0)", //
                "l2=geom:LINESTRING(0 0, 180 0)");

        geogigData.add().commit("Added test features");

        mediator = GWC.get();
        assertNotNull(mediator);
        storageBroker = GeoWebCacheExtensions.bean(StorageBroker.class);
        assertNotNull(storageBroker);

        GWCConfig config = mediator.getConfig();
        config.setCacheLayersByDefault(true);
        mediator.saveConfig(config);

        Catalog catalog = getCatalog();
        CatalogBuilder catalogBuilder = geogigData.newCatalogBuilder(catalog);
        catalogBuilder.addAllRepoLayers().build();

        String layerName = catalogBuilder.workspaceName() + ":points";
        LayerInfo pointLayerInfo = catalog.getLayerByName(layerName);
        assertNotNull(pointLayerInfo);
        pointsLayer = mediator.getTileLayer(pointLayerInfo);
        assertNotNull(pointsLayer);
        pointsLayer.getInfo().setExpireCache(10 * 1000);
        mediator.save(pointsLayer);

        layerName = catalogBuilder.workspaceName() + ":lines";
        LayerInfo lineLayerInfo = catalog.getLayerByName(layerName);
        assertNotNull(lineLayerInfo);
        linesLayer = mediator.getTileLayer(lineLayerInfo);
        assertNotNull(lineLayerInfo);
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        getCatalog().dispose();
    }

    /** Override so that default layers are not added */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        //
    }

    @Test
    public void testRemoveSingleFeature() throws Exception {

        ConveyorTile tile = createTileProto(pointsLayer);

        SimpleFeature feature = geogigData.getFeature("points/p2"); // POINT(1 1)

        Envelope bounds = (Envelope) feature.getBounds();
        org.geowebcache.grid.BoundingBox featureBounds =
                new org.geowebcache.grid.BoundingBox(
                        bounds.getMinX(), bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());

        GridSubset gridSubset = pointsLayer.getGridSubset(tile.getGridSetId());

        long[][] featureCoverages = gridSubset.getCoverageIntersections(featureBounds);
        int level = 4;
        long[] levelCoverageIntersection = featureCoverages[level];

        final long tileX = levelCoverageIntersection[0];
        final long tileY = levelCoverageIntersection[1];

        long[] xyz = tile.getStorageObject().getXYZ();
        xyz[0] = tileX;
        xyz[1] = tileY;
        xyz[2] = level;

        ConveyorTile result = pointsLayer.getTile(tile);
        CacheResult cacheResult = result.getCacheResult();
        assertEquals(CacheResult.MISS, cacheResult);

        result = pointsLayer.getTile(tile);
        cacheResult = result.getCacheResult();
        assertEquals(CacheResult.HIT, cacheResult);

        geogigData
                .update("points/p1", "geom", "POINT(-1 -1)") // 9570
                .add() //
                .commit("moved POINT(1 1) to POINT(-1 -1)") //
                .update("lines/l1", "geom", "LINESTRING(0 10, 0 -10)") //
                .add() //
                .commit("moved LINESTRING(-10 0, 10 0) to LINESTRING(0 10, 0 -10)");

        // give the hook some time to run
        Thread.sleep(100);
        result = pointsLayer.getTile(tile);
        cacheResult = result.getCacheResult();
        assertEquals(CacheResult.MISS, cacheResult);
    }

    public ConveyorTile createTileProto(GeoServerTileLayer tileLayer) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        String layerName = tileLayer.getName();

        MimeType mimeType = tileLayer.getDefaultMimeType();
        String gridsetId = tileLayer.getGridSubsets().iterator().next();
        Map<String, String> filteringParameters = null;

        long[] tileIndex = new long[3];
        ConveyorTile tile =
                new ConveyorTile(
                        storageBroker,
                        layerName,
                        gridsetId,
                        tileIndex,
                        mimeType,
                        filteringParameters,
                        req,
                        resp);
        return tile;
    }
}
