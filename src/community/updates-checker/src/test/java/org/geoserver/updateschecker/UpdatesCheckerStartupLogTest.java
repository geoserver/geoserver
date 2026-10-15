/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/** The startup check as the application wires it: the event reaches the listener, which fetches and logs one line. */
public class UpdatesCheckerStartupLogTest extends GeoServerSystemTestSupport {

    private static TestFeedServer feedServer;

    private static final String APPENDER = "updates-checker-test-capture";

    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());

    private final java.util.logging.Handler julHandler = new java.util.logging.Handler() {
        @Override
        public void publish(java.util.logging.LogRecord record) {
            lines.add(record.getMessage());
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    };

    private java.util.logging.Logger julLogger;

    private java.util.logging.Level julLevel;

    @BeforeClass
    public static void startFeedServer() throws Exception {
        System.setProperty(UpdatesCheckerService.FORCE_VERSION_PROPERTY, "2.28.3");
        feedServer = TestFeedServer.startForApplication(TestFeeds.feed(
                "",
                TestFeeds.series("3.0.x", "stable", "3.0.1", false),
                TestFeeds.series("2.28.x", "maintenance", "2.28.5", false)));
    }

    @AfterClass
    public static void stopFeedServer() {
        feedServer.stop();
        System.clearProperty(UpdatesCheckerService.FORCE_VERSION_PROPERTY);
    }

    @Before
    public void captureTheLogOnceTheApplicationCheckIsOver() throws InterruptedException {
        // the application's own startup check must be over, so the only one counted is the one published below
        awaitStartupThreads();
        captureTheLog();
    }

    // the Log4j context is the application's own and is not ours to close
    @SuppressWarnings("PMD.CloseResource")
    @After
    public void restoreDefaults() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        julLogger.removeHandler(julHandler);
        julLogger.setLevel(julLevel);
        context.getConfiguration().removeLogger(UpdatesCheckerService.LOGGER_NAME);
        context.updateLoggers();
    }

    /** A GeoServer application sends java.util.logging to Log4j, so the lines are collected with a Log4j appender. */
    @SuppressWarnings("PMD.CloseResource") // the Log4j context is the application's own
    private void captureTheLog() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        AbstractAppender appender = new AbstractAppender(APPENDER, null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                lines.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        // the test configuration logs little, so the module's logger gets a configuration of its own for the test
        LoggerConfig loggerConfig = new LoggerConfig(UpdatesCheckerService.LOGGER_NAME, Level.ALL, true);
        loggerConfig.addAppender(appender, null, null);
        configuration.addLogger(UpdatesCheckerService.LOGGER_NAME, loggerConfig);
        context.updateLoggers();
        // when an earlier test class loaded the service before any application redirected logging, the service still
        // writes to the plain java.util.logging logger of that name, which a real server never does
        julLogger = java.util.logging.Logger.getLogger(UpdatesCheckerService.LOGGER_NAME);
        julLevel = julLogger.getLevel();
        julLogger.setLevel(java.util.logging.Level.ALL);
        julLogger.addHandler(julHandler);
    }

    private static void awaitStartupThreads() throws InterruptedException {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (UpdatesCheckerStartupChecker.THREAD_NAME.equals(thread.getName())) {
                thread.join(10_000);
            }
        }
    }

    @Test
    public void theContextLoadedEventMakesOneStartupCheckAndLogsItsResult() throws Exception {
        assertNotNull(GeoServerExtensions.bean(UpdatesCheckerStartupChecker.class));
        int before = feedServer.getRequests();

        applicationContext.publishEvent(new ContextLoadedEvent(applicationContext));
        awaitStartupThreads();

        assertEquals("one download", before + 1, feedServer.getRequests());
        String line = null;
        synchronized (lines) {
            for (String l : lines) {
                if (l.startsWith("Startup update check:")) {
                    line = l;
                }
            }
        }
        assertNotNull("the startup result line is in the log: " + lines, line);
        assertTrue(line, line.contains("running 2.28.3") && line.contains("2.28.5 is available"));
    }
}
