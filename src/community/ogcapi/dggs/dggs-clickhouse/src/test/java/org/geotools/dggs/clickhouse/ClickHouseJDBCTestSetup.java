/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2024, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs.clickhouse;

import java.util.Properties;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCTestSetup;

class ClickHouseJDBCTestSetup extends JDBCTestSetup {
    @Override
    protected JDBCDataStoreFactory createDataStoreFactory() {
        return new ClickHouseJDBCDataStoreFactory();
    }

    @Override
    protected Properties createExampleFixture() {
        Properties fixture = new Properties();
        fixture.put("driver", "com.clickhouse.jdbc.ClickHouseDriver");
        fixture.put("url", "jdbc:clickhouse://localhost:8123/test");
        fixture.put("host", "localhost");
        fixture.put("database", "test");
        fixture.put("port", "8123");
        fixture.put("user", "default");
        fixture.put("password", "");
        return fixture;
    }
}
