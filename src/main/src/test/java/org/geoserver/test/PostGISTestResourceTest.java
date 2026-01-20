/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import org.geotools.api.data.Transaction;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.ClassRule;
import org.junit.Test;

public class PostGISTestResourceTest {

    @ClassRule
    public static final PostGISTestResource postgis = new PostGISTestResource();

    @Test
    public void testConnectionParametersNotNull() {
        Map<String, Serializable> params = postgis.getConnectionParameters();
        assertNotNull("Connection parameters should not be null", params);
        assertTrue("Connection parameters should contain dbtype", params.containsKey("dbtype"));
        assertEquals("postgis", params.get("dbtype"));
    }

    @Test
    public void testCreateDataStore() throws Exception {
        JDBCDataStore ds = postgis.createDataStore();
        assertNotNull("DataStore should not be null", ds);
        try {
            assertNotNull("DataStore should have a data source", ds.getDataSource());
        } finally {
            ds.dispose();
        }
    }

    @Test
    public void testGetConnection() throws Exception {
        JDBCDataStore ds = postgis.createDataStore();
        try (Connection conn = ds.getConnection(Transaction.AUTO_COMMIT)) {
            assertNotNull("Connection should not be null", conn);
            assertTrue("Connection should be valid", conn.isValid(5));
        } finally {
            ds.dispose();
        }
    }

    @Test
    public void testContainerIsRunning() {
        assertNotNull("Container should not be null", postgis.getContainer());
        assertTrue("Container should be running", postgis.getContainer().isRunning());
    }
}
