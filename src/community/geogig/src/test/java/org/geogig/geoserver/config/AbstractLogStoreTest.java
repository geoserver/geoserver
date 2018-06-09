/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static org.geogig.geoserver.config.LogEvent.Severity.DEBUG;
import static org.geogig.geoserver.config.LogEvent.Severity.ERROR;
import static org.geogig.geoserver.config.LogEvent.Severity.INFO;
import static org.geogig.geoserver.config.LogStore.PROP_DRIVER_CLASS;
import static org.geogig.geoserver.config.LogStore.PROP_ENABLED;
import static org.geogig.geoserver.config.LogStore.PROP_MAX_CONNECTIONS;
import static org.geogig.geoserver.config.LogStore.PROP_PASSWORD;
import static org.geogig.geoserver.config.LogStore.PROP_RUN_SCRIPT;
import static org.geogig.geoserver.config.LogStore.PROP_SCRIPT;
import static org.geogig.geoserver.config.LogStore.PROP_URL;
import static org.geogig.geoserver.config.LogStore.PROP_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.geogig.geoserver.HeapResourceStore;
import org.geogig.geoserver.config.LogEvent.Severity;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.security.core.context.SecurityContextHolder;

@Ignore
public class AbstractLogStoreTest {

    @Rule public TemporaryFolder tmpDir = new TemporaryFolder();

    @Rule public ExpectedException thrown = ExpectedException.none();

    protected ResourceStore resourceStore;

    protected LogStore logStore;

    protected String repoUrl = "file:/home/testuser/repos/myrepo";

    @Before
    public void before() {
        resourceStore = getResourceStore();
        logStore = new LogStore(resourceStore);
        SecurityContextHolder.clearContext();
        setUpConfigFile();
    }

    protected ResourceStore getResourceStore() {
        return new HeapResourceStore();
    }

    @After
    public void after() throws Exception {
        if (logStore != null) {
            logStore.destroy();
        }
    }

    private void setUpConfigFile() {
        Resource dirResource = resourceStore.get(LogStore.CONFIG_DIR_NAME);

        Properties properties = new Properties();
        populateConfigProperties(properties);
        if (!properties.isEmpty()) {
            Resource configFile = dirResource.get(LogStore.CONFIG_FILE_NAME);
            try {
                try (Writer writer = new OutputStreamWriter(configFile.out(), Charsets.UTF_8)) {
                    properties.store(writer, "");
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    protected void populateConfigProperties(Properties properties) {
        final String driverClassName = "org.sqlite.JDBC";
        final File dbFile = new File(tmpDir.getRoot(), "logstore.sqlite");
        final String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        properties.setProperty(PROP_ENABLED, "true");
        properties.setProperty(PROP_DRIVER_CLASS, driverClassName);
        properties.setProperty(PROP_URL, jdbcUrl);
        properties.setProperty(PROP_USER, "");
        properties.setProperty(PROP_PASSWORD, "");
        properties.setProperty(PROP_MAX_CONNECTIONS, "1");
        properties.setProperty(PROP_SCRIPT, "sqlite.sql");
        properties.setProperty(PROP_RUN_SCRIPT, "true");
    }

    @Test
    public void testLogEntries() throws Exception {
        logStore.afterPropertiesSet();

        logStore.debug(repoUrl, "debug message");
        Throwable ex = new RuntimeException("test exception");
        ex.fillInStackTrace();
        logStore.error(repoUrl, "error message", ex);
        logStore.info(repoUrl, "info message");

        List<LogEvent> entries = logStore.getLogEntries(0, 10);
        assertNotNull(entries);
        assertEquals(3, entries.size());

        assertEquals(repoUrl, entries.get(0).getRepositoryURL());
        assertEquals(repoUrl, entries.get(1).getRepositoryURL());
        assertEquals(repoUrl, entries.get(2).getRepositoryURL());

        assertEquals("anonymous", entries.get(0).getUser());
        assertEquals("anonymous", entries.get(1).getUser());
        assertEquals("anonymous", entries.get(2).getUser());
    }

    @Test
    public void testLogEntriesOrderedByTimestampDecreasingOrder() throws Exception {
        logStore.afterPropertiesSet();

        logStore.debug(repoUrl, "debug message");
        Throwable ex = new RuntimeException("test exception");
        ex.fillInStackTrace();
        logStore.error(repoUrl, "error message", ex);
        logStore.info(repoUrl, "info message");

        List<LogEvent> entries = logStore.getLogEntries(0, 10);
        assertNotNull(entries);
        assertEquals(3, entries.size());

        assertEquals("info message", entries.get(0).getMessage());
        assertEquals("error message", entries.get(1).getMessage());
        assertEquals("debug message", entries.get(2).getMessage());
    }

    @Test
    public void testLogEntriesFilterBySeverity() throws Exception {
        logStore.afterPropertiesSet();

        Throwable ex = new RuntimeException("test exception");
        ex.fillInStackTrace();

        logStore.debug(repoUrl, "debug message 1");
        logStore.error(repoUrl, "error message 1", ex);
        logStore.info(repoUrl, "info message 1");
        logStore.debug(repoUrl, "debug message 2");
        logStore.error(repoUrl, "error message 2", ex);
        logStore.info(repoUrl, "info message 2");

        assertEquals(6, logStore.getLogEntries(0, 10, INFO, DEBUG, ERROR).size());
        assertEquals(4, logStore.getLogEntries(0, 10, INFO, DEBUG).size());
        assertEquals(4, logStore.getLogEntries(0, 10, INFO, ERROR).size());
        assertEquals(4, logStore.getLogEntries(0, 10, DEBUG, ERROR).size());
        assertEquals(2, logStore.getLogEntries(0, 10, INFO).size());
        assertEquals(2, logStore.getLogEntries(0, 10, ERROR).size());
        assertEquals(2, logStore.getLogEntries(0, 10, DEBUG).size());
    }

    @Test
    public void testLogEntriesOffsetLimit() throws Exception {
        logStore.afterPropertiesSet();

        Throwable ex = new RuntimeException("test exception");
        ex.fillInStackTrace();

        logStore.debug(repoUrl, "debug message 1");
        logStore.error(repoUrl, "error message 1", ex);
        logStore.info(repoUrl, "info message 1");
        logStore.debug(repoUrl, "debug message 2");
        logStore.error(repoUrl, "error message 2", ex);
        logStore.info(repoUrl, "info message 2");

        assertEquals(6, logStore.getLogEntries(0, 10).size());
        assertEquals(5, logStore.getLogEntries(0, 5).size());
        assertEquals(4, logStore.getLogEntries(2, 5).size());
        assertEquals(3, logStore.getLogEntries(2, 3).size());

        assertEquals(2, logStore.getLogEntries(0, 10, INFO).size());
        assertEquals(1, logStore.getLogEntries(0, 1, INFO, DEBUG).size());
        assertEquals(2, logStore.getLogEntries(2, 4, INFO, DEBUG).size());
        assertEquals(3, logStore.getLogEntries(0, 3, INFO, DEBUG).size());
    }

    @Test
    public void testGetStackTrace() throws Exception {
        logStore.afterPropertiesSet();

        Throwable ex = new RuntimeException("test exception");
        ex.fillInStackTrace();

        logStore.error(repoUrl, "error message 1", ex);
        LogEvent event = logStore.getLogEntries(0, 10).get(0);
        assertEquals(Severity.ERROR, event.getSeverity());

        long eventId = event.getEventId();
        String stackTrace = logStore.getStackTrace(eventId);
        assertNotNull(stackTrace);
        assertTrue(stackTrace, stackTrace.contains("test exception"));

        stackTrace = logStore.getStackTrace(eventId + 1);
        assertNull(stackTrace);
    }

    @Test
    public void testGetFullSize() throws Exception {
        logStore.afterPropertiesSet();

        assertEquals(0, logStore.getFullSize());

        logStore.debug(repoUrl, "debug message");
        assertEquals(1, logStore.getFullSize());

        Throwable ex = new RuntimeException("test exception");
        ex.fillInStackTrace();
        logStore.error(repoUrl, "error message", ex);
        assertEquals(2, logStore.getFullSize());

        logStore.info(repoUrl, "info message");
        assertEquals(3, logStore.getFullSize());
    }

    protected void runScript(
            String driverClassName, String jdbcUrl, URL script, String user, String password) {
        List<String> statements = parseStatements(script);

        Connection connection;
        try {
            Driver d = (Driver) Class.forName(driverClassName).newInstance();
            connection = DriverManager.getConnection(jdbcUrl, user, password);
        } catch (InstantiationException
                | IllegalAccessException
                | ClassNotFoundException
                | SQLException e) {
            throw Throwables.propagate(e);
        }

        System.err.println("Running script " + script.getFile() + " on db " + jdbcUrl);
        for (String sql : statements) {
            try (Statement st = connection.createStatement()) {
                // System.err.println(sql);
                st.execute(sql);
            } catch (SQLException e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private List<String> parseStatements(URL script) {
        List<String> lines;
        try {
            OutputStream to = new ByteArrayOutputStream();
            Resources.copy(script, to);
            String scriptContents = to.toString();
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
}
