/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Example JDBC connection URLs keyed by driver class, used to suggest a starting point in connection URL fields. The
 * placeholders in curly braces are meant to be replaced by the user with the actual host, port and database.
 */
public final class JDBCUrlTemplates {

    /** Driver class name to example URL, in display order. Databases GeoServer ships a JDBC store for, plus HSQLDB. */
    private static final Map<String, String> TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("org.postgresql.Driver", "jdbc:postgresql://{host}:5432/{database}");
        TEMPLATES.put("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@{host}:1521:{sid}");
        TEMPLATES.put(
                "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver://{host}:1433;databaseName={database}");
        TEMPLATES.put("com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:3306/{database}");
        TEMPLATES.put("com.ibm.db2.jcc.DB2Driver", "jdbc:db2://{host}:50000/{database}");
        TEMPLATES.put("com.teradata.jdbc.TeraDriver", "jdbc:teradata://{host}/DATABASE={database}");
        TEMPLATES.put("org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:hsql://{host}:9001/{database}");
    }

    private JDBCUrlTemplates() {}

    /**
     * Example URLs for the JDBC drivers currently registered with {@link DriverManager}, in display order. Falls back
     * to all known templates when no registered driver is recognized, so the field always offers some help.
     */
    public static List<String> forRegisteredDrivers() {
        List<String> registered = new ArrayList<>();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            registered.add(drivers.nextElement().getClass().getName());
        }
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> e : TEMPLATES.entrySet()) {
            if (registered.contains(e.getKey())) result.add(e.getValue());
        }
        return result.isEmpty() ? new ArrayList<>(TEMPLATES.values()) : result;
    }
}
