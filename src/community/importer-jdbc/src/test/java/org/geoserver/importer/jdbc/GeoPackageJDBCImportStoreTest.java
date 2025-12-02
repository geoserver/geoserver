/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.jdbc;

import java.util.Properties;

public class GeoPackageJDBCImportStoreTest extends AbstractJDBCImportStoreTest {

    @Override
    String getFixtureId() {
        return "importer-gpkg";
    }

    @Override
    protected Properties getFixture() {
        Properties fixture = new Properties();
        fixture.put("dbtype", "geopkg");
        fixture.put("database", "/tmp/importer-store.gpkg");
        fixture.put("read_only", "false");
        return fixture;
    }

    @Override
    protected Properties createExampleFixture() {
        return getFixture();
    }
}
