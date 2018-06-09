/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.geogig.geoserver.config.LogStore.PROP_DRIVER_CLASS;
import static org.geogig.geoserver.config.LogStore.PROP_ENABLED;
import static org.geogig.geoserver.config.LogStore.PROP_MAX_CONNECTIONS;
import static org.geogig.geoserver.config.LogStore.PROP_PASSWORD;
import static org.geogig.geoserver.config.LogStore.PROP_RUN_SCRIPT;
import static org.geogig.geoserver.config.LogStore.PROP_SCRIPT;
import static org.geogig.geoserver.config.LogStore.PROP_URL;
import static org.geogig.geoserver.config.LogStore.PROP_USER;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import com.google.common.reflect.Reflection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/**
 * Helper class to create the default {@code <data-dir>/geogig/config/security/logstore.properties}
 * config file and the {@code <data-dir>/geogig/config/security/securitylogs.db} SQLite database
 * where to write the log entries to.
 *
 * <p>It also copies a couple sql init scripts for oher database engines to the same directory.
 */
class LogStoreInitializer {

    private static final Logger LOGGER = Logging.getLogger(LogStoreInitializer.class);

    static void dispose(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        } else if (dataSource instanceof SingleConnectionDataSource) {
            try {
                ((SingleConnectionDataSource) dataSource).conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static DataSource newDataSource(final Properties properties, final Resource configResource) {
        final String driverName = properties.getProperty(PROP_DRIVER_CLASS);
        checkNotNull(
                driverName,
                "driverName not provided in properties resource %s",
                configResource.path());
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    String.format("JDBC Driver '%s' does not exist in the classpath", driverName));
        }

        final String jdbcUrl = properties.getProperty(PROP_URL);
        checkArgument(
                jdbcUrl != null,
                "url not provided in properties resource %s",
                configResource.path());
        final String username = properties.getProperty(PROP_USER);
        final String password = properties.getProperty(PROP_PASSWORD);
        final String maxConnectionsProp = properties.getProperty(PROP_MAX_CONNECTIONS);
        int maxConnections = 10;
        if (maxConnectionsProp != null) {
            try {
                maxConnections = Integer.parseInt(maxConnectionsProp);
                checkArgument(
                        maxConnections > 0,
                        "maxConnections must be an integer > 0: %s",
                        maxConnections);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Can't parse maxConnections as an int: " + maxConnectionsProp, e);
            }
        }

        DataSource dataSource;
        if (jdbcUrl.startsWith("jdbc:sqlite")) {
            // sqlite must be used with a single connection shared among all threads
            Connection connection;
            try {
                connection = DriverManager.getConnection(jdbcUrl);
            } catch (SQLException e) {
                throw Throwables.propagate(e);
            }
            dataSource = new SingleConnectionDataSource(connection);
        } else {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(maxConnections);

            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    static void createDefaultConfig(final Resource propertiesResource) throws IOException {
        final Resource configDirectory = propertiesResource.parent();
        final Resource dbFile = configDirectory.get("securitylogs.db");
        final String driverClassName = "org.sqlite.JDBC";
        final String jdbcUrl = "jdbc:sqlite:" + dbFile.file().getAbsolutePath();

        createDefaultPropertiesFile(propertiesResource, driverClassName, jdbcUrl);
    }

    private static void createDefaultPropertiesFile(
            final Resource propertiesResource, final String driverClassName, final String jdbcUrl) {
        Properties props = new Properties();
        props.setProperty(PROP_ENABLED, "true");
        props.setProperty(PROP_DRIVER_CLASS, driverClassName);
        props.setProperty(PROP_URL, jdbcUrl);
        props.setProperty(PROP_USER, "");
        props.setProperty(PROP_PASSWORD, "");
        props.setProperty(PROP_MAX_CONNECTIONS, "1");
        props.setProperty(PROP_SCRIPT, "sqlite.sql");
        props.setProperty(PROP_RUN_SCRIPT, "true");

        saveConfig(props, propertiesResource);
    }

    static void saveConfig(Properties props, Resource propertiesResource) {
        String comments = configComments();

        try (Writer writer = new OutputStreamWriter(propertiesResource.out(), Charsets.UTF_8)) {
            props.store(writer, comments);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private static String configComments() {
        String comments =
                new StringBuilder(
                                "Connection information for the geogig security logs database.\n#") //
                        .append("enabled true|false whether to enable security logging\n#")
                        .append(PROP_DRIVER_CLASS) //
                        .append(": JDBC Driver class name\n#")
                        .append(PROP_URL) //
                        .append(": JDBC URL for the connections\n#")
                        .append(PROP_USER) //
                        .append(": database user name\n#") //
                        .append(PROP_PASSWORD) //
                        .append(": database user password\n#") //
                        .append(PROP_MAX_CONNECTIONS) //
                        .append(": max number of connections in the pool\n#") //
                        .append(PROP_SCRIPT) //
                        .append(": Database initialization DDL script file\n#") //
                        .append(PROP_RUN_SCRIPT) //
                        .append(
                                ": Boolean indicating whether to execute the init script. If true, and succeeded, its value will automatically be set to false afterwards\n#") //
                        .append("If using SQLite, the ") //
                        .append(PROP_MAX_CONNECTIONS) //
                        .append(
                                " option has no effect and a single connection is used among all threads.\n") //
                        .append(
                                "If not using SQLite (for which the tables are created automatically), make sure to first run the\n#") //
                        .append(
                                "appropriate DDL script on the database. Some sample ones accompany this file. There are\n#") //
                        .append(
                                "more init scripts at https://github.com/qos-ch/logback/tree/master/logback-classic/src/main/resources/ch/qos/logback/classic/db/script") //
                        .toString();
        return comments;
    }

    static void copySampleInitSript(Resource configDirectory, String scriptName)
            throws IOException {
        Resource resource = configDirectory.get(scriptName);
        if (!resource.getType().equals(Resource.Type.UNDEFINED)) {
            return;
        }
        try (OutputStream out = resource.out()) {
            Resources.copy(LogStoreInitializer.class.getResource(scriptName), out);
        }
    }

    static void runScript(DataSource ds, Resource script) {
        List<String> statements = parseStatements(script);

        try {
            try (Connection connection = ds.getConnection()) {
                LOGGER.info("Running script " + script.name());
                for (String sql : statements) {
                    try (Statement st = connection.createStatement()) {
                        LOGGER.fine(sql);
                        st.execute(sql);
                    } catch (SQLException e) {
                        throw Throwables.propagate(e);
                    }
                }
            }
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    private static List<String> parseStatements(Resource script) {
        List<String> lines;
        try {
            StringWriter sw = new StringWriter();
            IOUtils.copy(script.in(), sw);
            String scriptContents = sw.toString();
            lines = CharStreams.readLines(new StringReader(scriptContents));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        List<String> statements = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") || line.startsWith("-") || line.isEmpty()) {
                continue;
            }
            sb.append(line).append('\n');
            if (line.endsWith(";")) {
                statements.add(sb.toString());
                sb.setLength(0);
            }
        }

        return statements;
    }

    private static class SingleConnectionDataSource implements DataSource {

        private Connection conn;

        public SingleConnectionDataSource(Connection conn) {
            this.conn = Unclosable.proxyFor(conn);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {}

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {}

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return conn;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return conn;
        }
    }

    private static class Unclosable implements InvocationHandler {
        private Connection c;

        public Unclosable(Connection c) {
            this.c = c;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                return null;
            }
            return method.invoke(c, args);
        }

        public static Connection proxyFor(Connection c) {
            Connection proxy = Reflection.newProxy(Connection.class, new Unclosable(c));
            return proxy;
        }
    }
}
