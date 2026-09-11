/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Smoke tester for {@code geofence-server} (the embedded engine). Unlike the base {@code geofence} plugin (a REST
 * client stub, no live connection needed to boot), geofence-server's Spring context builds a real
 * {@code DataSource}/{@code EntityManagerFactory} eagerly at context-refresh time - it can't start at all without a
 * real database. Starts an ephemeral Testcontainers Postgres/PostGIS instance (same image geofence_39's own tests use)
 * and points a {@code geofence-datasource.properties} file at it before GeoServer starts, mirroring
 * {@link WPSJDBCTester}'s {@code prepareTestDirectory} pattern for the JDBC status store - the difference is this one
 * needs a real container rather than a file-based database, and so also needs to stop it afterwards.
 */
public class GeofenceServerTester extends DefaultPluginTester {

    private static final String POSTGRES_IMAGE = "postgis/postgis:15-3.4";

    private PostgreSQLContainer container;

    @Override
    protected void prepareTestDirectory(Path testWorkDir) throws Exception {
        container = new PostgreSQLContainer(
                        DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("geofence")
                .withUsername("geofence")
                .withPassword("geofence");
        container.start();

        Path geofenceDir = testWorkDir.resolve("data_dir").resolve("geofence");
        Files.createDirectories(geofenceDir);

        Properties props = new Properties();
        props.setProperty("geofence.datasource.url", container.getJdbcUrl());
        props.setProperty("geofence.datasource.username", container.getUsername());
        props.setProperty("geofence.datasource.password", container.getPassword());
        props.setProperty("geofence.datasource.driver", "org.postgresql.Driver");

        Path configFile = geofenceDir.resolve("geofence-datasource.properties");
        try (OutputStream os = Files.newOutputStream(configFile)) {
            props.store(os, "Testcontainers Postgres for the assembly smoke test");
        }
    }

    @Override
    protected void cleanupTestDirectory(Path testWorkDir) {
        if (container != null) {
            container.stop();
        }
    }
}
