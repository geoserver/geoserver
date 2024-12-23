/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
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

import java.io.IOException;
import java.util.Map;
import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.SQLDialect;

public class ClickHouseJDBCDataStoreFactory extends JDBCDataStoreFactory {
    public static final String DATABASE_ID = "clickhouse";
    public static final String DRIVER_CLASSNAME = "com.clickhouse.jdbc.ClickHouseDriver";
    /**
     * Use the Java 11 built-in HTTP client (the default would be Commons HTTPClient, which would cause version
     * conflicts
     */
    public static final String FORCE_HTTP_CLIENT = "http_connection_provider=HTTP_CLIENT";

    // TODO: expose parameters with sensible defaults for port, user, password

    @Override
    protected String getDatabaseID() {
        return DATABASE_ID;
    }

    @Override
    protected String getDriverClassName() {
        return DRIVER_CLASSNAME;
    }

    @Override
    protected SQLDialect createSQLDialect(JDBCDataStore dataStore) {
        return new ClickHouseDialect(dataStore);
    }

    @Override
    protected String getValidationQuery() {
        return "select 1";
    }

    @Override
    public String getDescription() {
        return "Clickhouse alphanumeric datastore";
    }

    @Override
    public BasicDataSource createDataSource(Map<String, ?> params) throws IOException {
        BasicDataSource ds = super.createDataSource(params);
        ds.addConnectionProperty("max_query_size", "1000000");
        ds.addConnectionProperty("socket_timeout", "300000");
        return ds;
    }

    @Override
    protected String getJDBCUrl(Map<String, ?> params) throws IOException {
        return super.getJDBCUrl(params) + "?" + FORCE_HTTP_CLIENT;
    }
}
