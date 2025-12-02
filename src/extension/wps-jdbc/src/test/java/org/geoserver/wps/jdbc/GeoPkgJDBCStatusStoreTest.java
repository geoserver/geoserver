/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.jdbc;

import java.util.Properties;

public class GeoPkgJDBCStatusStoreTest extends AbstractJDBCStatusStoreTest {

    @Override
    String getFixtureId() {
        return "wps-geopkg";
    }

    @Override
    protected Properties getFixture() {
        Properties fixture = new Properties();
        fixture.put("database", "target/wps-store");
        fixture.put("dbtype", "geopkg");
        fixture.put("read_only", "false");
        return fixture;
    }
}
