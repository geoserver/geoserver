/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.Transaction;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Assume;
import org.junit.rules.ExternalResource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class OSEOPostGISResource extends ExternalResource {

    private final PostgreSQLContainer postgis;
    private final boolean populateGranulesTable;

    public OSEOPostGISResource(boolean populateGranulesTable) {
        DockerImageName image =
                DockerImageName.parse("postgis/postgis:18-3.6-alpine").asCompatibleSubstituteFor("postgres");
        this.populateGranulesTable = populateGranulesTable;
        postgis =
                new PostgreSQLContainer(image); // .withDatabaseName("oseo").withUsername("oseo").withPassword("oseo");
    }

    @Override
    public void before() throws Throwable {
        Assume.assumeTrue(DockerClientFactory.instance().isDockerAvailable());
        postgis.start();
        JDBCDataStore store = createPostGISDataStore();
        try {
            populateTestDatabase(store, populateGranulesTable);
        } finally {
            store.dispose();
        }
    }

    @Override
    public void after() {
        if (postgis != null) {
            postgis.stop();
        }
    }

    public Map<String, Serializable> getPostgisStoreConnectionParameters() {
        Map<String, Serializable> params = new HashMap<>();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
        params.put(PostgisNGDataStoreFactory.HOST.key, "localhost");
        params.put(PostgisNGDataStoreFactory.DATABASE.key, postgis.getDatabaseName());
        params.put(PostgisNGDataStoreFactory.PORT.key, postgis.getMappedPort(5432));
        params.put(PostgisNGDataStoreFactory.USER.key, postgis.getUsername());
        params.put(PostgisNGDataStoreFactory.PASSWD.key, postgis.getPassword());
        params.put(PostgisNGDataStoreFactory.EXPOSE_PK.key, "true");
        return params;
    }

    public JDBCDataStore createPostGISDataStore() throws IOException {
        Map<String, Serializable> params = getPostgisStoreConnectionParameters();
        return (JDBCDataStore) DataStoreFinder.getDataStore(params);
    }

    /** Sets up a PostGIS based OpenSearchAccess and configures OpenSearch for EO to use it */
    public void setupBasicOpenSearch(Catalog cat, GeoServer gs) throws IOException, SQLException {
        // create the plain datastore
        DataStoreInfo jdbcDs = cat.getFactory().createDataStore();
        jdbcDs.setName("oseo_jdbc");
        WorkspaceInfo ws = cat.getDefaultWorkspace();
        jdbcDs.setWorkspace(ws);
        jdbcDs.setEnabled(true);
        Map<String, Serializable> jdbcParams = jdbcDs.getConnectionParameters();
        jdbcParams.putAll(getPostgisStoreConnectionParameters());
        cat.add(jdbcDs);

        // create the OpenSeach wrapper store
        DataStoreInfo osDs = cat.getFactory().createDataStore();
        osDs.setName("oseo");
        osDs.setWorkspace(ws);
        osDs.setEnabled(true);

        Map<String, Serializable> oseoParams = osDs.getConnectionParameters();
        oseoParams.put("dbtype", "opensearch-eo-jdbc");
        oseoParams.put("database", jdbcDs.getWorkspace().getName() + ":" + jdbcDs.getName());
        oseoParams.put("store", jdbcDs.getWorkspace().getName() + ":" + jdbcDs.getName());
        oseoParams.put("repository", null);
        cat.add(osDs);

        // configure opensearch for EO to use it
        OSEOInfo service = gs.getService(OSEOInfo.class);
        service.setOpenSearchAccessStoreId(osDs.getId());
        service.getGlobalQueryables().addAll(Arrays.asList("id", "geometry", "collection"));
        gs.save(service);

        // configure contact info
        GeoServerInfo global = gs.getGlobal();
        global.getSettings().getContact().setContactOrganization("GeoServer");
        gs.save(global);
    }

    void populateTestDatabase(JDBCDataStore store, boolean addGranuleTable) throws SQLException, IOException {
        try (Connection conn = store.getConnection(Transaction.AUTO_COMMIT)) {
            // setup for fast import

            createTables(conn);
            populateCollections(conn);
            populateProducts(conn);
            if (addGranuleTable) {
                populateGranules(conn);
            }
            addCustomProductClass(conn);
        }
    }

    private List<String> loadScriptCommands(String scriptLocation) throws IOException {
        // grab all non comment, non-empty lines
        try (InputStream is = JDBCOpenSearchAccess.class.getResourceAsStream(scriptLocation)) {
            List<String> lines = IOUtils.readLines(is, StandardCharsets.UTF_8).stream()
                    .map(String::trim)
                    .filter(l -> !l.startsWith("--") && !l.isEmpty())
                    .toList();
            // regroup them into statements
            List<String> statements = new ArrayList<>();
            String buffer = null;
            for (String line : lines) {
                if (buffer == null) {
                    buffer = line;
                } else {
                    buffer = buffer + "\n" + line;
                }
                if (line.trim().endsWith(";")) {
                    statements.add(buffer);
                    buffer = null;
                }
            }
            return statements;
        }
    }

    /** Takes the postgis.sql creation script, adapts it and runs it on H2 */
    void createTables(Connection conn) throws SQLException, IOException {
        List<String> statements = loadScriptCommands("/postgis.sql");
        try (Statement st = conn.createStatement(); ) {
            for (String statement : statements) {
                //                /* Skip statements H2 does not support */
                //                if (statement.contains("GIST") || statement.contains("create
                // extension")) {
                //                    continue;
                //                }
                //                if (statement.contains("geography(Polygon, 4326)")) {
                //                    statement = statement.replace("geography(Polygon, 4326)",
                // "POLYGON");
                //                } else if (statement.contains("geometry(Polygon, 4326)")) {
                //                    statement = statement.replace("geometry(Polygon, 4326)",
                // "POLYGON");
                //                }
                //                if (statement.contains("float[]")) {
                //                    statement = statement.replace("float[]", "ARRAY");
                //                }
                //                if (statement.contains("varchar[]")) {
                //                    statement = statement.replace("varchar[]", "ARRAY");
                //                }
                st.execute(statement);
            }
        }
    }

    /** Adds the collection data into the H2 database */
    void populateCollections(Connection conn) throws SQLException, IOException {
        runScript("/collection_test_data.sql", conn);
    }

    /** Adds the product data into the H2 database */
    void populateProducts(Connection conn) throws SQLException, IOException {
        runScript("/product_test_data.sql", conn);
    }

    /** Adds the granules table */
    void populateGranules(Connection conn) throws SQLException, IOException {
        runScript("/granule_test_data.sql", conn);
    }

    void addCustomProductClass(Connection conn) throws SQLException, IOException {
        runScript("/custom_product_class.sql", conn);
    }

    void runScript(String script, Connection conn) throws IOException, SQLException {
        List<String> statements = loadScriptCommands(script);
        try (Statement st = conn.createStatement(); ) {
            for (String statement : statements) {
                try {
                    st.execute(statement);
                } catch (SQLException e) {
                    throw new IOException("Failed to run " + statement, e);
                }
            }
        }
    }
}
