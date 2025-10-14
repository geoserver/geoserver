/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.geotools.util.logging.Log4J2Logger;
import org.geotools.util.logging.LoggerAdapter;

class LoggingCapturer {

    private Logger logger;
    private List<String> messages = List.of();

    private CapturingAdaptor capturer;

    private static interface CapturingAdaptor {
        void startCapturing();

        void stopCapturing();
    }

    public LoggingCapturer(Logger logger) {
        this.logger = logger;
    }

    public LoggingCapturer start() {
        stop();
        capturer = createCapturer();
        messages = new CopyOnWriteArrayList<>();
        capturer.startCapturing();
        return this;
    }

    /** Unregisters the handler to capture the log events */
    public List<String> stop() {
        if (null != capturer) {
            capturer.stopCapturing();
        }
        capturer = null;
        return getMessages();
    }

    public List<String> getMessages() {
        return List.copyOf(messages);
    }

    private CapturingAdaptor createCapturer() {
        if (logger instanceof LoggerAdapter) {
            if (logger instanceof Log4J2Logger) {
                return new Log4j2Capturer();
            }
            throw new UnsupportedOperationException(
                    "There's capturing adapter for " + logger.getClass().getCanonicalName());
        }

        return new JULCapturer();
    }

    private class JULCapturer extends java.util.logging.Handler implements CapturingAdaptor {

        @Override
        public void startCapturing() {
            logger.addHandler(this);
        }

        @Override
        public void stopCapturing() {
            logger.removeHandler(this);
        }

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() throws SecurityException {
            // no-op
        }
    }

    private class Log4j2Capturer extends AbstractAppender implements CapturingAdaptor {

        org.apache.logging.log4j.core.Logger coreLogger;

        public Log4j2Capturer() {
            super("TestCapturingAppender", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void startCapturing() {
            String name = logger.getName();
            org.apache.logging.log4j.Logger log4jLogger = LogManager.getLogger(name);
            // Get the root logger's configuration
            coreLogger = (org.apache.logging.log4j.core.Logger) log4jLogger;
            coreLogger.addAppender(this);
            super.start(); // Start the custom appender
        }

        @Override
        public void stopCapturing() {
            if (null != coreLogger) {
                coreLogger.removeAppender(this);
                coreLogger = null;
            }
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }
    }
}
