/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * A subclass of {@link DefaultPluginTester} that can set up the JDBC status store configuration file (the plugin fails
 * on startup without one)
 */
public class WPSJDBCTester extends DefaultPluginTester {

    @Override
    protected void prepareTestDirectory(Path testWorkDir) throws Exception {
        Path dataDir = testWorkDir.resolve("data_dir");
        Path database = dataDir.resolve("jdbcstatusstore.gpkg");
        Properties fixture = new Properties();
        fixture.put("database", database.toAbsolutePath().toString());
        fixture.put("dbtype", "geopkg");
        fixture.put("read_only", "false");
        Path configuration = dataDir.resolve("jdbcstatusstore.props");
        try (OutputStream os = Files.newOutputStream(configuration)) {
            fixture.store(os, "JDBCStatusStore configuration");
        }
    }
}
