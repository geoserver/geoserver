/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Files;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test that backs up a catalog containing a <em>real</em> PostGIS store, started with Testcontainers, and
 * verifies the store configuration (connection parameters, tokenised password) is serialized into the backup archive.
 * This exercises the JDBC-store backup path against a live database rather than the in-memory fixtures used by the unit
 * tests.
 *
 * <p>The test extends {@link GeoServerSystemTestSupport} directly (not {@code BackupRestoreTestSupport}) so it does not
 * inherit the auxiliary "foo" H2 store, and is automatically skipped when Docker is not available. The full restore
 * round trip (which reloads the whole GeoServer) is validated against a running server; the in-process harness reloads
 * too slowly to assert on reliably.
 */
public class PostgisBackupRestoreTest extends GeoServerSystemTestSupport {

    private static PostgreSQLContainer POSTGIS;

    private Backup backupFacade;

    @BeforeClass
    public static void startPostgis() {
        Assume.assumeTrue(
                "Docker is required for PostgisBackupRestoreTest",
                DockerClientFactory.instance().isDockerAvailable());
        POSTGIS = new PostgreSQLContainer(
                DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));
        POSTGIS.start();
    }

    @AfterClass
    public static void stopPostgis() {
        if (POSTGIS != null) {
            POSTGIS.stop();
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        backupFacade = (Backup) applicationContext.getBean("backupFacade");
        addPostgisStoreAndLayer();
    }

    /** Registers a PostGIS data store pointing at the container, creates a table and publishes it as a layer. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addPostgisStoreAndLayer() throws Exception {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getDefaultWorkspace();

        DataStoreInfo ds = catalog.getFactory().createDataStore();
        ds.setName("pgstore");
        ds.setWorkspace(ws);
        Map<String, Serializable> params = ds.getConnectionParameters();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
        params.put(PostgisNGDataStoreFactory.HOST.key, POSTGIS.getHost());
        params.put(PostgisNGDataStoreFactory.PORT.key, POSTGIS.getMappedPort(5432));
        params.put(PostgisNGDataStoreFactory.DATABASE.key, POSTGIS.getDatabaseName());
        params.put(PostgisNGDataStoreFactory.USER.key, POSTGIS.getUsername());
        params.put(PostgisNGDataStoreFactory.PASSWD.key, POSTGIS.getPassword());
        catalog.add(ds);

        // create the underlying table and publish a layer for it
        DataStore store = (DataStore) ds.getDataStore(null);
        store.createSchema(DataUtilities.createType("places", "the_geom:Point:srid=4326,name:String"));

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setStore(ds);
        FeatureTypeInfo ft = cb.buildFeatureType(store.getFeatureSource("places"));
        // the table is empty, so give the resource an explicit SRS and bounds (otherwise it is "not
        // fully configured" and gets skipped during backup)
        ft.setSRS("EPSG:4326");
        ft.setNativeCRS(CRS.decode("EPSG:4326"));
        ft.setNativeBoundingBox(new ReferencedEnvelope(9, 13, 44, 48, CRS.decode("EPSG:4326")));
        ft.setLatLonBoundingBox(new ReferencedEnvelope(9, 13, 44, 48, CRS.decode("EPSG:4326")));
        catalog.add(ft);
        catalog.add(cb.buildLayer(ft));
    }

    @Test
    public void testBackupPostgisStore() throws Exception {
        File archive = File.createTempFile("pg-backup", ".zip");
        archive.delete();

        BackupExecutionAdapter backup =
                backupFacade.runBackupAsync(Files.asResource(archive), true, null, null, null, bestEffort());
        waitForCompletion(backup);

        assertEquals(BatchStatus.COMPLETED, backup.getStatus());
        // the serialized catalog must carry the PostGIS store (name + dbtype)
        assertArchiveContainsPostgisStore(archive);
    }

    private Hints bestEffort() {
        Hints hints = new Hints(new HashMap<>(2));
        hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
        return hints;
    }

    private void waitForCompletion(AbstractExecutionAdapter exec) throws InterruptedException {
        int cnt = 0;
        while (cnt < 200 && (exec.getStatus() != BatchStatus.COMPLETED || exec.isRunning())) {
            if (exec.getStatus() == BatchStatus.FAILED || exec.getStatus() == BatchStatus.ABANDONED) {
                break;
            }
            Thread.sleep(100);
            cnt++;
        }
    }

    private void assertArchiveContainsPostgisStore(File archive) throws IOException {
        // backup/restore serializes the catalog into Spring Batch ".dat" files (the stores land in
        // store.dat.1), not the data-directory layout (see RESTBackupTest)
        try (ZipFile zip = new ZipFile(archive)) {
            ZipEntry storeDat = zip.getEntry("store.dat.1");
            assertNotNull("backup archive should contain the serialized stores (store.dat.1)", storeDat);
            String content;
            try (var in = zip.getInputStream(storeDat)) {
                content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertTrue(
                    "store.dat.1 should contain the PostGIS store configuration",
                    content.contains("pgstore") && content.contains("postgis"));
        }
    }
}
