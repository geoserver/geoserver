/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.util;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.geotools.util.logging.LoggerAdapter;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

/**
 * JUnit Rule to test that logging occurs. The instrumentation will be removed when complete and any
 * changes to log level will be reverted.
 *
 * <p>Note: that this works by adding a Handler to the Logger.
 *
 * @author Kevin Smith, Boundless
 */
public class LoggerRule extends java.util.logging.Handler implements TestRule {
    private Logger log;
    private LinkedList<LogRecord> records = new LinkedList<>();
    private Level newLevel = null;
    private Level oldLevel = null;

    /**
     * Test logging for the given logger
     *
     * @param log Logger to monitor
     */
    public LoggerRule(Logger log) {
        super();
        this.log = log;
    }

    /**
     * Test logging for the given logger
     *
     * @param log Logger to monitor
     * @param level Set the log level for the tests
     */
    public LoggerRule(Logger log, Level level) {
        super();
        this.log = log;
        super.setLevel(level);
        this.newLevel = level;
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return true;
    }

    @Override
    public Statement apply(final Statement base, final org.junit.runner.Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                oldLevel = log.getLevel();
                if (newLevel != null) {
                    log.setLevel(newLevel);
                }
                log.addHandler(LoggerRule.this);
                try {
                    base.evaluate();
                } finally {
                    log.removeHandler(LoggerRule.this);
                    log.setLevel(oldLevel);
                    oldLevel = null;
                }
            }
        };
    }

    @Override
    public void publish(LogRecord record) {
        records.add(record);
    }

    @Override
    public void flush() {
        // Do Nothing
    }

    @Override
    public void close() throws SecurityException {
        // Do Nothing
    }

    /** Clear all recorded log records */
    public void clear() {
        records.clear();
    }

    private void assumeCaptureWorks() {
        // FIXME: LoggerAdapter overrides addHandler to do nothing which prevents LoggerRule from
        // capturing records.
        Assume.assumeFalse(
                "LoggerRule can't capture logs for LoggerAdapter", log instanceof LoggerAdapter);
    }

    /** Get the captured log records */
    public List<LogRecord> records() {
        assumeCaptureWorks();
        return records;
    }

    /** Set the level of the logger. Will be reverted when the test is complete. */
    public void setTestLevel(Level testLevel) {
        log.setLevel(testLevel);
        newLevel = testLevel;
        super.setLevel(testLevel);
    }

    /**
     * Assert that a record was logged that matches the given conditions
     *
     * @param matcher Condition to match against
     */
    public void assertLogged(Matcher<? super LogRecord> matcher) {
        assertLogged("", matcher);
    }

    /**
     * Assert that a record was logged that matches the given conditions
     *
     * @param reason Message to add to the failure report
     * @param matcher Condition to match against
     */
    public void assertLogged(String reason, Matcher<? super LogRecord> matcher) {
        assumeCaptureWorks();
        for (LogRecord r : records) {
            if (matcher.matches(r)) {
                return;
            }
        }
        Description desc = new StringDescription();
        desc.appendText(reason);
        desc.appendText("\nExpected record: ");
        desc.appendDescriptionOf(matcher);
        desc.appendText("\n\tbut it was not logged");
        throw new AssertionError(desc.toString());
    }
}
