package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Assume;

public class GeoServerOpenSearchTestSupport {

    /** Sets up a PostGIS based OpenSearchAccess and configures OpenSearch for EO to use it */
    public static void setupBasicOpenSearch(
            SystemTestData testData, Catalog cat, GeoServer gs, boolean populateGranulesTable)
            throws IOException, SQLException {
        // create the plain database
        DataStoreInfo jdbcDs = cat.getFactory().createDataStore();
        jdbcDs.setName("oseo_jdbc");
        WorkspaceInfo ws = cat.getDefaultWorkspace();
        jdbcDs.setWorkspace(ws);
        jdbcDs.setEnabled(true);

        Map params = jdbcDs.getConnectionParameters();
        params.putAll(JDBCOpenSearchAccessTest.getFixture());
        cat.add(jdbcDs);

        JDBCDataStore h2 = (JDBCDataStore) jdbcDs.getDataStore(null);
        JDBCOpenSearchAccessTest.populateTestDatabase(h2, populateGranulesTable);

        // create the OpenSeach wrapper store
        DataStoreInfo osDs = cat.getFactory().createDataStore();
        osDs.setName("oseo");
        osDs.setWorkspace(ws);
        osDs.setEnabled(true);

        params = osDs.getConnectionParameters();
        params.put("dbtype", "opensearch-eo-jdbc");
        params.put("database", jdbcDs.getWorkspace().getName() + ":" + jdbcDs.getName());
        params.put("store", jdbcDs.getWorkspace().getName() + ":" + jdbcDs.getName());
        params.put("repository", null);
        cat.add(osDs);

        // configure opensearch for EO to use it
        OSEOInfo service = gs.getService(OSEOInfo.class);
        service.setOpenSearchAccessStoreId(osDs.getId());
        String queryables = "id,geometry,collection";
        service.setGlobalQueryables(queryables);
        gs.save(service);

        // configure contact info
        GeoServerInfo global = gs.getGlobal();
        global.getSettings().getContact().setContactOrganization("GeoServer");
        gs.save(global);
    }

    /** This method runs an Assume verifying the test fixture is available */
    public static void checkOnLine() {
        Assume.assumeNotNull(JDBCOpenSearchAccessTest.getFixture());
    }
}
