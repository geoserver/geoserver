/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.geotools.api.data.DataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.factory.GeoTools;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostGISTestResource extends ExternalResource {

    private PostgreSQLContainer<?> container;
    private DataStore dataStore;
    private String jdbcUrl;
    private String username;
    private String password;

    @Override
    protected void before() throws Exception {
        DockerImageName postgisImage =
                DockerImageName.parse("postgis/postgis:15-3.3-alpine").asCompatibleSubstituteFor("postgres");

        container = new PostgreSQLContainer<>(postgisImage)
                .withDatabaseName("geofence")
                .withUsername("geofence")
                .withPassword("geofence");

        container.start();

        jdbcUrl = container.getJdbcUrl();
        username = container.getUsername();
        password = container.getPassword();

        setupPostGIS();
    }

    private void setupPostGIS() throws SQLException {
        try (Connection conn = container.createConnection("")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE EXTENSION IF NOT EXISTS postgis;");
            }
        }
    }

    @Override
    protected void after() {
        if (dataStore != null) {
            dataStore.dispose();
        }
        if (container != null) {
            container.stop();
        }
    }

    public DataStore getDataStore() throws IOException {
        if (dataStore == null) {
            Map<String, Object> params = new HashMap<>();
            params.put(JDBCDataStoreFactory.DBTYPE.key, "postgis");
            params.put(JDBCDataStoreFactory.HOST.key, container.getHost());
            params.put(JDBCDataStoreFactory.PORT.key, container.getMappedPort(5432));
            params.put(JDBCDataStoreFactory.DATABASE.key, "geofence");
            params.put(JDBCDataStoreFactory.USER.key, username);
            params.put(JDBCDataStoreFactory.PASSWD.key, password);
            params.put(JDBCDataStoreFactory.EXPOSE_PK.key, true);

            JDBCDataStoreFactory factory = new JDBCDataStoreFactory();
            factory.setBaseDirectory(GeoTools.getDefaultHints());
            dataStore = factory.createDataStore(params);
        }
        return dataStore;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return container.getHost();
    }

    public Integer getPort() {
        return container.getMappedPort(5432);
    }

    public String getDatabaseName() {
        return "geofence";
    }
}
