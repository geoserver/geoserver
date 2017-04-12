/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.geoserver.opensearch.eo.store.OpenSearchAccess.EO_NAMESPACE;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.ProductClass.*;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.feature.NameImpl;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;

public class JDBCOpenSearchAccessTest {

    public static final String TEST_NAMESPACE = "http://www.test.com/os/eo";

    private static JDBCDataStore h2;

    private static OpenSearchAccess osAccess;

    @BeforeClass
    public static void setupStore() throws IOException, SQLException {
        Map<String, Serializable> params = new HashMap<>();
        params.put("dbtype", "h2");
        File dbFolder = new File("./target/oseo_db_store_test");
        FileUtils.deleteQuietly(dbFolder);
        dbFolder.mkdir();
        File dbFile = new File(dbFolder, "oseo_db_store_test");
        params.put("database", dbFile.getAbsolutePath());
        h2 = (JDBCDataStore) DataStoreFinder.getDataStore(params);
        JDBCOpenSearchAccessTest.populateTestDatabase(h2);

        Name name = new NameImpl("test", "jdbcStore");
        SerializableDefaultRepository repository = new SerializableDefaultRepository();
        repository.register(name, h2);

        // create the OpenSeach wrapper store
        params = new HashMap<>();
        params.put("dbtype", "opensearch-eo-jdbc");
        params.put("store", "test:jdbcStore");
        params.put("namespace", TEST_NAMESPACE);
        params.put("repository", repository);
        osAccess = (OpenSearchAccess) DataAccessFinder.getDataStore(params);
    }

    public static void populateTestDatabase(JDBCDataStore h2) throws SQLException, IOException {
        try (Connection conn = h2.getConnection(Transaction.AUTO_COMMIT); Statement st = conn.createStatement()) {
            // setup for fast import
            
            // SET CACHE_SIZE (a large cache is faster)
            st.execute("SET LOG 0");
            st.execute("SET LOCK_MODE 0 ");
            st.execute("SET UNDO_LOG 0");
            st.execute("SET CACHE_SIZE 512000");
            JDBCOpenSearchAccessTest.createTables(conn);
            JDBCOpenSearchAccessTest.populateCollections(conn);
            JDBCOpenSearchAccessTest.populateProducts(conn);
        }
    }

    @AfterClass
    public static void tearDownStore() {
        osAccess.dispose();
        h2.dispose();
    }

    private static List<String> loadScriptCommands(String scriptLocation) throws IOException {
        // grab all non comment, non empty lines
        try (InputStream is = JDBCOpenSearchAccess.class.getResourceAsStream(scriptLocation)) {
            List<String> lines = IOUtils.readLines(is).stream().map(l -> l.trim())
                    .filter(l -> !l.startsWith("--") && !l.isEmpty()).collect(Collectors.toList());
            // regroup them into statements
            List<String> statements = new ArrayList<String>();
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

    /**
     * Takes the postgis.sql creation script, adapts it and runs it on H2
     */
    static void createTables(Connection conn) throws SQLException, IOException {
        List<String> statements = loadScriptCommands("/postgis.sql");
        try (Statement st = conn.createStatement();) {
            for (String statement : statements) {
                /* Skip statements H2 does not support */
                if (statement.contains("GIST") || statement.contains("create extension")) {
                    continue;
                }
                if (statement.contains("geography(Polygon, 4326)")) {
                    statement = statement.replace("geography(Polygon, 4326)", "POLYGON");
                }
                st.execute(statement);
            }
            // add spatial indexes
            st.execute(
                    "CALL AddGeometryColumn(SCHEMA(), 'COLLECTION', 'footprint', 4326, 'POLYGON', 2)");
            st.execute("CALL CreateSpatialIndex(SCHEMA(), 'COLLECTION', 'footprint', 4326)");
            st.execute(
                    "CALL AddGeometryColumn(SCHEMA(), 'PRODUCT', 'footprint', 4326, 'POLYGON', 2)");
            st.execute("CALL CreateSpatialIndex(SCHEMA(), 'PRODUCT', 'footprint', 4326)");
        }
    }

    /**
     * Adds the collection data into the H2 database 
     */
    static void populateCollections(Connection conn) throws SQLException, IOException {
        runScript("/collection_h2_data.sql", conn);
    }
    
    /**
     * Adds the product data into the H2 database
     */
    static void populateProducts(Connection conn) throws SQLException, IOException {
        runScript("/product_h2_data.sql", conn);
    }

    private static void runScript(String script, Connection conn) throws IOException, SQLException {
        List<String> statements = loadScriptCommands(script);
        try (Statement st = conn.createStatement();) {
            for (String statement : statements) {
                st.execute(statement);
            }
        }
    }
    

    @Test
    public void testCollectionFeatureType() throws Exception {
        // check expected name
        FeatureType schema = osAccess.getCollectionSource().getSchema();
        Name name = schema.getName();
        assertEquals(TEST_NAMESPACE, name.getNamespaceURI());
        assertThat(name.getLocalPart(), equalToIgnoringCase("collection"));

        // test the schema
        assertPropertyNamespace(schema, "wavelength", EO_NAMESPACE);
    }

    @Test
    public void testProductFeatureType() throws Exception {
        // check expected name
        FeatureType schema = osAccess.getProductSource().getSchema();
        Name name = schema.getName();

        assertEquals(TEST_NAMESPACE, name.getNamespaceURI());
        assertThat(name.getLocalPart(), equalToIgnoringCase("product"));

        // get the schema
        assertPropertyNamespace(schema, "cloudCover", OPTICAL.getNamespace());
        assertPropertyNamespace(schema, "track", EOP_GENERIC.getNamespace());
        assertPropertyNamespace(schema, "polarisationMode", RADAR.getNamespace());
    }

    private void assertPropertyNamespace(FeatureType schema, String name, String namespaceURI) {
        PropertyDescriptor wl = schema.getDescriptor(name);
        assertNotNull(wl);
        assertEquals(namespaceURI, wl.getName().getNamespaceURI());
    }

}
