/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.hib;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;

public class MonitoringDataSource extends BasicDataSource implements DisposableBean {

    static Logger LOGGER = Logging.getLogger(Monitor.class);

    MonitorConfig config;
    GeoServerDataDirectory dataDirectory;

    public void setConfig(MonitorConfig config) {
        this.config = config;
    }

    public void setDataDirectory(GeoServerDataDirectory dataDir) {
        this.dataDirectory = dataDir;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            if (getDriverClassName() == null) {
                synchronized (this) {
                    if (getDriverClassName() == null) {
                        initializeDataSource();
                    }
                }
            }
            return super.getConnection();
        } catch (Exception e) {
            // LOGGER.log(Level.WARNING, "Database connection error", e);
            config.setError(e);
            config.setEnabled(false);

            if (e instanceof SQLException) {
                throw (SQLException) e;
            }

            throw (SQLException) new SQLException().initCause(e);
        }
    }

    void initializeDataSource() throws Exception {
        Resource monitoringDir = dataDirectory.get("monitoring");
        Resource dbprops = monitoringDir.get("db.properties");
        if (Resources.exists(dbprops)) {
            LOGGER.info("Configuring monitoring database from: " + dbprops.path());

            // attempt to configure
            try {
                configureDataSource(dbprops, monitoringDir);
            } catch (SQLException e) {
                // configure failed, try db1.properties
                dbprops = monitoringDir.get("db1.properties");
                if (Resources.exists(dbprops)) {
                    try {
                        configureDataSource(dbprops, monitoringDir);

                        // secondary file worked, return
                        return;
                    } catch (SQLException e1) {
                        // secondary file failed as well, try for third
                        dbprops = monitoringDir.get("db2.properties");
                        if (Resources.exists(dbprops)) {
                            try {
                                configureDataSource(dbprops, monitoringDir);

                                // third file worked, return
                                return;
                            } catch (SQLException e2) {
                            }
                        }
                    }
                }

                throw e;
            }
        } else {
            // no db.properties file, use internal default
            configureDataSource(null, monitoringDir);
        }
    }

    void configureDataSource(Resource dbprops, Resource monitoringDir) throws Exception {
        Properties db = new Properties();

        if (dbprops == null) {
            dbprops = monitoringDir.get("db.properties");

            // use a default, and copy the template over
            try (InputStream in = getClass().getResourceAsStream("db.properties");
                    OutputStream out = dbprops.out()) {
                IOUtils.copy(in, out);
            }

            try (InputStream in = getClass().getResourceAsStream("db.properties")) {
                db.load(in);
            }
        } else {
            try (InputStream in = dbprops.in()) {
                db.load(in);
            }
        }

        logDbProperties(db);

        // TODO: check for nulls
        setDriverClassName(db.getProperty("driver"));
        setUrl(getURL(db));

        if (db.containsKey("username")) {
            setUsername(db.getProperty("username"));
        }
        if (db.containsKey("password")) {
            setPassword(db.getProperty("password"));
        }

        setDefaultAutoCommit(Boolean.valueOf(db.getProperty("defaultAutoCommit", "true")));

        // TODO: make other parameters configurable
        setMinIdle(1);
        setMaxActive(4);

        // test the connection
        super.getConnection();
    }

    void logDbProperties(Properties db) {
        if (LOGGER.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer("Monitoring database connection info:\n");
            for (Map.Entry e : db.entrySet()) {
                sb.append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
            }
            LOGGER.fine(sb.toString());
        }
    }

    String getURL(Properties db) {
        return db.getProperty("url")
                .replace("${GEOSERVER_DATA_DIR}", dataDirectory.root().getAbsolutePath());
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void destroy() throws Exception {
        super.close();
    }
}
