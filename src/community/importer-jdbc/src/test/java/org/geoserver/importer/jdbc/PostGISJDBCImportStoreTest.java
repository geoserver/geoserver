/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.jdbc;

import java.util.Properties;

public class PostGISJDBCImportStoreTest extends AbstractJDBCImportStoreTest {

    @Override
    String getFixtureId() {
        return "importer-postgis";
    }

    @Override
    protected Properties createExampleFixture() {
        Properties fixture = new Properties();

        fixture.put("dbtype", "postgis");
        fixture.put("database", "importer-store");
        fixture.put("port", "5432");
        fixture.put("host", "localhost");
        fixture.put("user", "geotools");
        fixture.put("passwd", "geotools");

        return fixture;
    }
}
