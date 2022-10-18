/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.DOTALL;
import static org.geoserver.logging.GeoServerXMLConfiguration.attributeGet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.MemoryLockProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;

/**
 * Test logging configuration.
 *
 * <p>To trouble shoot start with:<code>
 *     mvn test -Dtest=LoggingStartupContextListenerTest -Dorg.apache.logging.log4j.simplelog.StatusLogger.level=DEBUG
 * </code>
 */
public class LoggingStartupContextListenerTest {

    static Locale systemLocale;

    @BeforeClass
    public static void initialize() {
        systemLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        // reset static state leftover from other tests
        System.clearProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL);
        LoggingUtils.relinquishLog4jControl = false;
    }

    @AfterClass
    public static void reset() {
        Locale.setDefault(systemLocale);
    }

    @Test
    public void testUpdateBuiltInLoggingProfile() throws Exception {

        final File TARGET_DIR = new File("./target");
        final File LOGS_DIR = new File(TARGET_DIR, "logs");
        final File DEFAULT_LOGGING_FILE = new File(LOGS_DIR, "DEFAULT_LOGGING.xml");

        FileUtils.deleteQuietly(LOGS_DIR);
        GeoServerResourceLoader loader = new GeoServerResourceLoader(TARGET_DIR);

        // make it copy the log files
        LoggingUtils.initLogging(loader, "DEFAULT_LOGGING.xml", false, true, null);
        assertTrue("DEFAULT_LOGGING.xml created", DEFAULT_LOGGING_FILE.exists());

        // customize with a comment
        try (FileWriter fileWriter = new FileWriter(DEFAULT_LOGGING_FILE, true);
                BufferedWriter buffered = new BufferedWriter(fileWriter);
                PrintWriter writer = new PrintWriter(buffered)) {
            writer.println("# Hello World");
        }
        try (Stream<String> stream = Files.lines(Paths.get(DEFAULT_LOGGING_FILE.getPath()))) {
            boolean found = stream.anyMatch(lines -> lines.contains("# Hello World"));
            assertTrue("default logging customized", found);
        }

        // Use UPDATE_BUILT_IN_LOGGING_PROFILES policy to restore built-in definition
        try {
            LoggingUtils.updateBuiltInLoggingProfiles = true;

            // update built-in DEFAULT_LOGGING.xml
            LoggingUtils.initLogging(loader, "DEFAULT_LOGGING.xml", false, true, null);
        } finally {
            LoggingUtils.updateBuiltInLoggingProfiles = false;
        }

        try (Stream<String> stream = Files.lines(Paths.get(DEFAULT_LOGGING_FILE.getPath()))) {
            boolean found = stream.anyMatch(lines -> lines.contains("# Hello World"));
            assertFalse("default logging customized", found);
        }
    }

    @Test
    public void testAvoidCustomizationOverwrite() throws Exception {
        final File target = new File("./target/logs-customize");
        File logs = new File(target, "logs");
        FileUtils.deleteQuietly(logs);
        GeoServerResourceLoader loader = new GeoServerResourceLoader(target);
        FileSystemResourceStore store = (FileSystemResourceStore) loader.getResourceStore();
        store.setLockProvider(new MemoryLockProvider());

        // make it copy the log files
        LoggingUtils.initLogging(loader, "VERBOSE_LOGGING.xml", false, false, null);

        // customize the verbose logging profile
        File verbose = new File(logs, "VERBOSE_LOGGING.xml");
        String xml = FileUtils.readFileToString(verbose, StandardCharsets.UTF_8);
        String xmlUpdated = xml.replace("trace", "debug");
        FileUtils.writeStringToFile(verbose, xmlUpdated, StandardCharsets.UTF_8);
        long lastModified = verbose.lastModified();

        // switch to that logging profile, this will generate some logs
        LoggingUtils.initLogging(loader, "VERBOSE_LOGGING.xml", false, false, null);
        try (TestAppender appender = TestAppender.createAppender("override-test", null)) {
            appender.startRecording("org.geoserver.logging");

            // file was not modified
            assertEquals(lastModified, verbose.lastModified());

            // two log messages, one above, one below threshold
            Logger logger = LogManager.getLogger("org.geoserver.logging");
            logger.debug("Should appear");
            logger.trace("Should not appear");

            appender.stopRecording("org.geoserver.logging");

            // check the messages have been filtered by level as expected
            appender.assertTrue("Should appear");
            appender.assertFalse("Should not appear");
        }
    }

    @Test
    public void testLogLocationFromServletContext() throws Exception {
        File tmp = File.createTempFile("log", "tmp", new File("target"));
        tmp.delete();
        tmp.mkdirs();

        File logs = new File(tmp, "logs");
        assertTrue(logs.mkdirs());

        FileUtils.copyURLToFile(
                LoggingStartupContextListenerTest.class.getResource("logging.xml"),
                new File(tmp, "logging.xml"));

        MockServletContext context = new MockServletContext();
        context.setInitParameter("GEOSERVER_DATA_DIR", tmp.getPath());
        context.setInitParameter(
                "GEOSERVER_LOG_LOCATION", new File(tmp, "foo.log").getAbsolutePath());

        // Lookup Log4J Core configuration
        {
            @SuppressWarnings({
                "resource",
                "PMD.CloseResource"
            }) // current context, no need to enforce AutoClosable
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

            Configuration configuration = ctx.getConfiguration();

            Appender appender = configuration.getAppender("geoserverlogfile");
            assertNull("Expected geoserverlogfile to be null.  But was: " + appender, appender);

            ctx.close();
        }

        // System.setProperty("log4j2.debug", "true");
        String restore = System.getProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL);
        try {
            System.setProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, "false");
            new LoggingStartupContextListener()
                    .contextInitialized(new ServletContextEvent(context));
        } finally {
            if (restore == null) {
                System.getProperties().remove(LoggingUtils.RELINQUISH_LOG4J_CONTROL);
            } else {
                System.setProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, restore);
            }
            // System.getProperties().remove("log4j2.debug");
        }

        // verify change
        {
            String expectedLogfile = new File(tmp, "foo.log").getCanonicalPath();

            @SuppressWarnings({
                "resource",
                "PMD.CloseResource"
            }) // current context, no need to enforce AutoClosable
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);

            Configuration configuration = ctx.getConfiguration();

            if (configuration.getProperties().containsKey("GEOSERVER_LOG_LOCATION")) {
                // double check GEOSERVER_LOG_LOCATION property setup with expectedLogFile
                assertEquals(
                        "Property logfile",
                        expectedLogfile,
                        configuration.getProperties().get("GEOSERVER_LOG_LOCATION"));

                // double check geoserverlogfile setup to use ${GEOSERVER_LOG_LOCATION}
                Appender appender = configuration.getAppender("geoserverlogfile");
                assertNotNull("geoserverlogfile expected", appender);
                assertTrue(appender instanceof RollingFileAppender);

                RollingFileAppender fileAppender = (RollingFileAppender) appender;
                assertTrue(
                        "fileName property substitution",
                        fileAppender.getFileName().contains("${GEOSERVER_LOG_LOCATION}"));
            } else {
                // double check file appender setup with expectedLogFile
                Appender appender = configuration.getAppender("geoserverlogfile");
                assertNotNull("geoserverlogfile expected", appender);
                assertTrue(appender instanceof RollingFileAppender);

                RollingFileAppender fileAppender = (RollingFileAppender) appender;
                assertEquals(
                        "fileName property substitution",
                        expectedLogfile,
                        fileAppender.getFileName());
            }
        }
    }

    @Test
    public void testLogLocationFromEmptyContext() throws Exception {
        File tmp = File.createTempFile("log", "tmp", new File("target"));
        tmp.delete();
        tmp.mkdirs();

        File logs = new File(tmp, "logs");
        assertTrue(logs.mkdirs());

        MockServletContext context = new MockServletContext();
        context.setInitParameter("GEOSERVER_DATA_DIR", tmp.getPath());
        context.setInitParameter(
                "GEOSERVER_LOG_LOCATION", new File(tmp, "foo.log").getAbsolutePath());

        try (TestAppender appender = TestAppender.createAppender("quite", null)) {
            appender.startRecording("org.geoserver.logging");

            appender.trigger("Could not reconfigure LOG4J loggers");

            ServletContextListener listener = new LoggingStartupContextListener();
            listener.contextInitialized(new ServletContextEvent(context));
            listener.contextDestroyed(new ServletContextEvent(context));

            appender.stopRecording("org.geoserver.logging");
        }
    }

    @Test
    public void testInitLoggingLock() throws Exception {

        final File TARGET_DIR = new File("./target");
        final File LOGS_DIR = new File(TARGET_DIR, "logs");
        final File DEFAULT_LOGGING_FILE = new File(LOGS_DIR, "DEFAULT_LOGGING.xml");

        FileUtils.deleteQuietly(LOGS_DIR);
        GeoServerResourceLoader loader = new GeoServerResourceLoader(TARGET_DIR);
        FileSystemResourceStore store = (FileSystemResourceStore) loader.getResourceStore();
        store.setLockProvider(new MemoryLockProvider());

        // make it copy the log files
        LoggingUtils.initLogging(loader, "DEFAULT_LOGGING.xml", false, true, null);
        assertTrue("DEFAULT_LOGGING.xml created", DEFAULT_LOGGING_FILE.exists());

        // init once from default logging
        LoggingUtils.initLogging(loader, "DEFAULT_LOGGING.xml", false, true, null);
        // init twice, here it used to lock up
        LoggingUtils.initLogging(loader, "DEFAULT_LOGGING.xml", false, true, null);

        try (FileWriter fileWriter = new FileWriter(DEFAULT_LOGGING_FILE, true);
                BufferedWriter buffered = new BufferedWriter(fileWriter);
                PrintWriter writer = new PrintWriter(buffered)) {
            writer.println("# Hello World");
        }
        try (Stream<String> stream = Files.lines(Paths.get(DEFAULT_LOGGING_FILE.getPath()))) {
            boolean found = stream.anyMatch(lines -> lines.contains("# Hello World"));
            assertTrue("default logging customized", found);
        }
        try {
            System.getProperties().put("UPDATE_BUILT_IN_LOGGING_PROFILES", "true");

            // update built-in DEFAULT_LOGGING.xml
            LoggingUtils.initLogging(loader, "DEFAULT_LOGGING.xml", false, true, null);
        } finally {
            System.getProperties().remove("UPDATE_BUILT_IN_LOGGING_PROFILES");
        }
    }

    @Test
    public void testFilenameAndFilePattern() throws Exception {
        assertEquals(
                "some/where/logging.txt",
                GeoServerXMLConfiguration.applyPathTemplate(
                        "some/where/logging.txt", "logs/geoserver.log"));
        assertEquals(
                "some/where/logging-%d{MM-dd-yyyy}.log",
                GeoServerXMLConfiguration.applyPathTemplate(
                        "some/where/logging", "logs/geoserver-%d{MM-dd-yyyy}.log"));
        assertEquals(
                "some/where/logging-%i.txt.zip",
                GeoServerXMLConfiguration.applyPathTemplate(
                        "some/where/logging.txt", "logs/geoserver-%i.log.zip"));
        assertEquals(
                "some/where/logging-%d{MM-dd-yyyy}.txt.zip",
                GeoServerXMLConfiguration.applyPathTemplate(
                        "some/where/logging.txt", "logs/geoserver-%d{MM-dd-yyyy}.log.zip"));
        assertEquals(
                "some/where/logging$hostName.txt",
                GeoServerXMLConfiguration.applyPathTemplate(
                        "some/where/logging.txt", "logs/$hostName.log"));
        assertEquals(
                "some/where/logging-$hostName-%i.txt",
                GeoServerXMLConfiguration.applyPathTemplate(
                        "some/where/logging.txt", "logs/geoserver-$hostName-%i.log"));
        // Do not allow log4j parameter substitution from global settings, only logging profile
        assertEquals(
                "logs/geoserver.log",
                GeoServerXMLConfiguration.applyPathTemplate(
                        "${baseDir}/logs/logging.log", "logs/geoserver.log"));
    }

    @Test
    public void testNodeAttributes() {
        Node node = new Node();
        node.getAttributes().put("fileName", "logs/geoserver.log");

        assertNotNull("attribute name lookup", attributeGet(node, "fileName"));
        assertNotNull("attribute case-insensitive lookup", attributeGet(node, "filename"));

        assertEquals("attribute style", "logs/geoserver.log", attributeGet(node, "fileName"));
        assertEquals("element style", "logs/geoserver.log", attributeGet(node, "FileName"));
        assertEquals("quick style", "logs/geoserver.log", attributeGet(node, "filename"));

        node.getAttributes().remove("fileName");
        assertNull("attribute style", attributeGet(node, "fileName"));
        assertNull("element style", attributeGet(node, "FileName"));
        assertNull("quick style", attributeGet(node, "filename"));

        GeoServerXMLConfiguration.attributePut(node, "filename", "history.txt");
        assertEquals("store direct", "history.txt", node.getAttributes().get("filename"));

        GeoServerXMLConfiguration.attributePut(node, "fileName", "geoserver.log");
        assertEquals("store match", "geoserver.log", node.getAttributes().get("filename"));
    }

    @Test
    public void testDateFormat() throws Exception {
        final File target = new File("./target/logs-date");
        File logs = new File(target, "logs");
        FileUtils.deleteQuietly(logs);
        logs.mkdir();
        GeoServerResourceLoader loader = new GeoServerResourceLoader(target);

        testExpectetFormat(loader, "DEFAULT_LOGGING");
        testExpectetFormat(loader, "GEOSERVER_DEVELOPER_LOGGING");
        testExpectetFormat(loader, "PRODUCTION_LOGGING");
        testExpectetFormat(loader, "VERBOSE_LOGGING");
        testExpectetFormat(loader, "GEOTOOLS_DEVELOPER_LOGGING");

        // reset the file logging, disabling it
        LoggingUtils.initLogging(loader, "TEST_LOGGING", true, true, null);
    }

    private static void testExpectetFormat(GeoServerResourceLoader loader, String configuration)
            throws Exception {
        String location = "logs/" + configuration + ".log";
        LoggingUtils.initLogging(loader, configuration, true, false, location);
        Logger logger = LogManager.getLogger("org.geoserver.logging");
        logger.error("Test message");
        File logFile = new File(loader.getBaseDirectory(), location);
        String contents = FileUtils.readFileToString(logFile, UTF_8);
        Pattern pattern = Pattern.compile("\\d{2} \\w{3} \\d{2}:\\d{2}:\\d{2}.*", DOTALL);
        assertTrue(pattern.matcher(contents).matches());
    }
}
