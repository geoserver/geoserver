/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNotEquals;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.SeedRequest;
import org.geowebcache.seed.TileBreeder;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.StorageObject;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/*
 * Tests Integrated GWC seeding against GeoServer's security subsystem
 */
public class GWCSeedingSecurityIntegrationTest extends GeoServerSystemTestSupport {

    // Workspaces
    static final String PUB_URI = "https://pub.org";
    static final String PUB_PREFIX = "pub";
    static final String SEC_URI = "https://sec.com";
    static final String SEC_PREFIX = "sec";
    // Layers
    static final QName SEC_BRIDGES = new QName(SEC_URI, "Bridges", SEC_PREFIX);
    static final QName SEC_BUILDINGS = new QName(SEC_URI, "Buildings", SEC_PREFIX);
    static final QName PUB_LAKES = new QName(PUB_URI, "Lakes", PUB_PREFIX);
    static final QName PUB_STREAMS = new QName(PUB_URI, "Streams", PUB_PREFIX);

    private TileBreeder tileBreeder = null;
    private Catalog rawCatalog = null;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpSecurity();
        // Don't set up default layers, we'll use our own
    }

    /*
     * Test Data overrides the normal system test support configuraiton
     * Instead, there are two workspaces:
     * * "pub" - publicly readable. Contains layers:
     *   * "Lakes"
     *   * "Streams"
     * * "sec" - requires authentication. Contains layers:
     *   * "Bridges"
     *   * "Buildings"
     */
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        Catalog catalog = getCatalog();
        testData.addWorkspace(PUB_PREFIX, PUB_URI, catalog);
        testData.addWorkspace(SEC_PREFIX, SEC_URI, catalog);

        testData.addVectorLayer(SEC_BRIDGES, Collections.EMPTY_MAP, SystemTestData.class, catalog);
        testData.addVectorLayer(
                SEC_BUILDINGS, Collections.EMPTY_MAP, SystemTestData.class, catalog);
        testData.addVectorLayer(PUB_STREAMS, Collections.EMPTY_MAP, SystemTestData.class, catalog);
        testData.addVectorLayer(PUB_LAKES, Collections.EMPTY_MAP, SystemTestData.class, catalog);

        DataAccessRuleDAO dao =
                GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
        GWC.get().getConfig().setSecurityEnabled(true);

        addLayerAccessRule(PUB_PREFIX, "*", AccessMode.READ, "*");
        addLayerAccessRule(SEC_PREFIX, "*", AccessMode.READ, "ROLE_ADMINISTRATOR");
        addLayerAccessRule("*", "*", AccessMode.ADMIN, "ROLE_ADMINISTRATOR");
    }

    @Before
    public void setUp() {
        tileBreeder = applicationContext.getBean(TileBreeder.class);
        rawCatalog = (Catalog) GeoServerExtensions.bean("rawCatalog");
    }

    @After
    public void cleanUp() throws GeoWebCacheException, InterruptedException {
        logout();
        // Truncate all cached layers
        executeSeedRequest(createSeedRequest(PUB_STREAMS, GWCTask.TYPE.TRUNCATE));
        executeSeedRequest(createSeedRequest(PUB_LAKES, GWCTask.TYPE.TRUNCATE));
        executeSeedRequest(createSeedRequest(SEC_BRIDGES, GWCTask.TYPE.TRUNCATE));
        executeSeedRequest(createSeedRequest(SEC_BUILDINGS, GWCTask.TYPE.TRUNCATE));

        waitForSeedingToFinish();
    }

    @Test
    public void testSeedPublic()
            throws GeoWebCacheException, InterruptedException, StorageException {

        // Assert cache empty
        TileObject tileObject = getSampleTile(PUB_LAKES);
        assertEquals(
                "Cache should be empty at test start",
                StorageObject.Status.MISS,
                tileObject.getStatus());
        assertNull("Cache should be empty at test start", tileObject.getBlob());

        SeedRequest sr = createSeedRequest(PUB_LAKES);
        GWCTask[] tasks = executeSeedRequest(sr);

        // verify the tasks have been queued
        assertTrue(
                "Failed to initialize seeding tasks",
                tileBreeder.getRunningAndPendingTasks().hasNext());

        waitForSeedingToFinish();
        assertFalse(
                "Failed to complete seeding after 120 seconds",
                tileBreeder.getRunningAndPendingTasks().hasNext());

        // expect success, assert nonempty
        tileObject = getSampleTile(PUB_LAKES);
        assertNotEquals(
                "Cache should not be empty after seeding",
                StorageObject.Status.MISS,
                tileObject.getStatus());
        assertNotNull("Cache should not be empty after seeding", tileObject.getBlob());
    }

    @Test
    public void testSeedSecuredAsAnonymous()
            throws StorageException, GeoWebCacheException, InterruptedException {
        // Assert cache empty
        TileObject tileObject = getSampleTile(SEC_BUILDINGS);
        assertEquals(
                "Cache should be empty at test start",
                StorageObject.Status.MISS,
                tileObject.getStatus());
        assertNull("Cache should be empty at test start", tileObject.getBlob());

        SeedRequest sr = createSeedRequest(SEC_BUILDINGS);
        GWCTask[] tasks = executeSeedRequest(sr);

        // verify the tasks have been queued
        assertTrue(
                "Failed to initialize seeding tasks",
                tileBreeder.getRunningAndPendingTasks().hasNext());

        waitForSeedingToFinish();
        assertFalse(
                "Failed to complete seeding after 120 seconds",
                tileBreeder.getRunningAndPendingTasks().hasNext());

        // expect failure, assert empty
        tileObject = getSampleTile(SEC_BUILDINGS);
        assertEquals(
                "Cache should be empty after seeding",
                StorageObject.Status.MISS,
                tileObject.getStatus());
        assertNull("Cache should be empty after seeding", tileObject.getBlob());
    }

    @Test
    public void testSeedSecuredAsAuthenticated()
            throws StorageException, GeoWebCacheException, InterruptedException {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        // Assert cache empty
        TileObject tileObject = getSampleTile(SEC_BUILDINGS);
        assertEquals(
                "Cache should be empty at test start",
                StorageObject.Status.MISS,
                tileObject.getStatus());
        assertNull("Cache should be empty at test start", tileObject.getBlob());

        SeedRequest sr = createSeedRequest(SEC_BUILDINGS);
        GWCTask[] tasks = executeSeedRequest(sr);

        // verify the tasks have been queued
        assertTrue(
                "Failed to initialize seeding tasks",
                tileBreeder.getRunningAndPendingTasks().hasNext());

        waitForSeedingToFinish();
        assertFalse(
                "Failed to complete seeding after 120 seconds",
                tileBreeder.getRunningAndPendingTasks().hasNext());

        // expect success, assert nonempty
        tileObject = getSampleTile(SEC_BUILDINGS);
        assertNotEquals(
                "Cache should not be empty after seeding",
                StorageObject.Status.MISS,
                tileObject.getStatus());
        assertNotNull("Cache should not be empty after seeding", tileObject.getBlob());
    }

    protected SeedRequest createSeedRequest(QName layerName) {
        return createSeedRequest(layerName, GWCTask.TYPE.SEED);
    }

    protected SeedRequest createSeedRequest(QName layerName, GWCTask.TYPE taskType) {
        GWC gwc = GWC.get();

        String prefixedName = layerName.getPrefix() + ":" + layerName.getLocalPart();
        // Use the raw catalog to bypass security when setting up requests
        LayerInfo layerInfo = rawCatalog.getLayerByName(prefixedName);
        TileLayer tileLayer = gwc.getTileLayer(layerInfo);

        SeedRequest seedRequest =
                new SeedRequest(
                        prefixedName,
                        tileLayer.getGridSubset("EPSG:4326").getOriginalExtent(),
                        "EPSG:4326",
                        1,
                        0,
                        2,
                        "image/png",
                        taskType,
                        Collections.EMPTY_MAP);
        return seedRequest;
    }

    protected GWCTask[] executeSeedRequest(SeedRequest sr) throws GeoWebCacheException {
        TileLayer tl = tileBreeder.findTileLayer(sr.getLayerName());
        TileRange tr = tileBreeder.createTileRange(sr, tl);
        GWCTask[] tasks =
                tileBreeder.createTasks(
                        tr, tl, sr.getType(), sr.getThreadCount(), sr.getFilterUpdate());
        tileBreeder.dispatchTasks(tasks);
        return tasks;
    }

    protected TileObject getSampleTile(QName layerName) throws StorageException {
        GWC gwc = GWC.get();
        String prefixedName = layerName.getPrefix() + ":" + layerName.getLocalPart();

        TileObject tileObject =
                TileObject.createQueryTileObject(
                        prefixedName,
                        new long[] {0L, 0L, 1L},
                        "EPSG:4326",
                        "image/png",
                        Collections.EMPTY_MAP);

        gwc.getCompositeBlobStore().get(tileObject);

        return tileObject;
    }

    protected void waitForSeedingToFinish() throws InterruptedException {
        int abort = 0;
        do {
            Thread.sleep(1000);
            abort++;
        } while (tileBreeder.getRunningAndPendingTasks().hasNext() && abort < 120);
    }
}
