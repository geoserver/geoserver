/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
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
        JDBCOpenSearchAccessTest.createTables(h2);

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

    @AfterClass
    public static void tearDownStore() {
        osAccess.dispose();
        h2.dispose();
    }

    /**
     * Takes the postgis.sql creation script, adapts it and runs it on H2
     */
    static void createTables(JDBCDataStore h2) throws SQLException, IOException {
        // grab all non comment, non empty lines
        List<String> lines = Files.lines(Paths.get("src/test/resources/postgis.sql"))
                .map(l -> l.trim()).filter(l -> !l.startsWith("--") && !l.isEmpty())
                .collect(Collectors.toList());
        // regroup them into statements
        List<String> statements = new ArrayList<String>();
        String buffer = null;
        for (String line : lines) {
            if (buffer == null) {
                buffer = line;
            } else {
                buffer = buffer + "\n" + line;
            }
            if (line.contains(";")) {
                statements.add(buffer);
                buffer = null;
            }
        }
        try (Connection conn = h2.getConnection(Transaction.AUTO_COMMIT);
                Statement st = conn.createStatement();) {
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

    @Test
    public void testCollectionFeatureType() throws Exception {
        // check expected name
        Name name = osAccess.getCollectionName();
        assertEquals(TEST_NAMESPACE, name.getNamespaceURI());
        assertThat(name.getLocalPart(), equalToIgnoringCase("collection"));

        // get the schema
        FeatureType schema = osAccess.getSchema(name);
        PropertyDescriptor wl = schema.getDescriptor("wavelength");
        assertNotNull(wl);
        assertEquals(OpenSearchAccess.EO_NAMESPACE, wl.getName().getNamespaceURI()); 

    }
}
