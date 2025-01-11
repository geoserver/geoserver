/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2025, Open Source Geospatial Foundation (OSGeo)
 *
 *    This file is hereby placed into the Public Domain. This means anyone is
 *    free to do whatever they wish with this file. Use it well and enjoy!
 */
package org.geotools.dggs.clickhouse;

import java.util.Properties;
import java.util.TimeZone;
import org.apache.commons.dbcp.BasicDataSource;
import org.geotools.jdbc.JDBCDateTestSetup;
import org.geotools.jdbc.JDBCDelegatingTestSetup;

/**
 * Fork of {@link JDBCDateTestSetup} for ClickHouse, which has no support for Time columns, and night never have one
 * (support for it has been marked as won't fix: https://github.com/ClickHouse/ClickHouse/issues/979).
 */
public class ClickHouseDateTestSetup extends JDBCDelegatingTestSetup {

    public ClickHouseDateTestSetup() {
        super(new ClickHouseJDBCTestSetup());
    }

    @Override
    protected void setUpData() throws Exception {
        try {
            dropDateTable();
        } catch (Exception e) {
        }

        createDateTable();
    }

    protected void createDateTable() throws Exception {
        run("CREATE TABLE dates (d DATE, dt DateTime)"
                + "ENGINE = MergeTree() "
                + "ORDER BY \"d\" "
                + "PARTITION by \"d\"");

        // _date('1998/05/31:12:00:00AM', 'yyyy/mm/dd:hh:mi:ssam'));
        TimeZone tz = TimeZone.getDefault();
        String tzId = tz.getID();

        run("INSERT INTO dates VALUES ("
                + "toDate('2009-06-28'), "
                + "parseDateTimeBestEffort('2009-06-28 15:12:41', '" + tzId + "'))");

        run("INSERT INTO dates VALUES ("
                + "toDate('2009-01-15'), "
                + "parseDateTimeBestEffort('2009-01-15 13:10:12', '" + tzId + "'))");

        run("INSERT INTO dates VALUES ("
                + "toDate('2009-09-29'), "
                + "parseDateTimeBestEffort('2009-09-29 17:54:23', '" + tzId + "'))");
    }

    protected void dropDateTable() throws Exception {
        runSafe("DROP TABLE dates");
    }

    @Override
    protected void initializeDataSource(BasicDataSource ds, Properties db) {
        super.initializeDataSource(ds, db);
        ds.addConnectionProperty("typeMappings", "date=java.sql.Date");
    }
}
