/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.jdbc;

import java.util.Properties;

public class H2JDBCImportStoreTest extends AbstractJDBCImportStoreTest {

    @Override
    String getFixtureId() {
        return "importer-h2";
    }

    @Override
    protected Properties getFixture() {
        Properties fixture = new Properties();
        fixture.put("driver", "org.h2.Driver");
        fixture.put("url", "jdbc:h2:target/h2-store");
        fixture.put("user", "geotools");
        fixture.put("password", "geotools");
        fixture.put("dbtype", "h2");
        return fixture;
    }

    @Override
    protected Properties createExampleFixture() {
        return getFixture();
    }
}
