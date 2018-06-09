/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.jdbc;

import java.util.Properties;

public class H2JDBCStatusStoreTest extends AbstractJDBCStatusStoreTest {

    @Override
    String getFixtureId() {
        return "wps-h2";
    }

    @Override
    protected Properties getFixture() {
        Properties fixture = new Properties();
        fixture.put("driver", "org.h2.Driver");
        fixture.put("url", "jdbc:h2:target/wps-store");
        fixture.put("user", "geotools");
        fixture.put("password", "geotools");
        fixture.put("dbtype", "h2");
        return fixture;
    }
}
