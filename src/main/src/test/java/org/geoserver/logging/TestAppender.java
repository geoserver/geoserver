/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.junit.Assert;

/**
 * Test Appender can be attached to Log4J configuration to verify expected logging output.
 *
 * <p>If you are reading this after failing to clean up a TestAppender: Logging is a really easy way
 * to have a memory leak, and this logger will nom-nom-nom with great enthusiasm. It is very easy to
 * leave one of these attached giving a subsequent test an entertaining failure.
 */
@Plugin(name = "TestAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class TestAppender extends AbstractAppender implements AutoCloseable {
    private final List<LogEvent> log = new ArrayList<>();
    private String trigger;

    /** TestAppender collecting LogEvents for verification */
    public TestAppender(String name, Filter filter) {
        super(name, filter, null, false, null);
    }

    @PluginFactory
    public static TestAppender createAppender(
            @PluginAttribute("name") String name, @PluginElement("Filter") Filter filter) {
        return new TestAppender(name, filter);
    }

    @Override
    public void append(LogEvent event) {
        log.add(event);
        if (trigger != null) {
            String formattedMessage = getMessage(event);
            if (formattedMessage.contains(trigger)) {
                fail("The trigger message '" + trigger + "' is still there!");
            }
        }
    }

    private String getMessage(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();
        if (StringUtils.isBlank(message)) {
            // Fallback looking for a message in the exception
            Throwable t = event.getThrown();
            if (t != null) {
                message = t.getMessage();
            }
        }
        return message;
    }

    /** Add appender to configuration and start listening for events. */
    public void startRecording() {
        @SuppressWarnings({
            "resource",
            "PMD.CloseResource"
        }) // current context, no need to enforce AutoClosable
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = ctx.getConfiguration();

        Appender check = configuration.getAppender(getName());
        if (check == null) {
            configuration.addAppender(this);
            this.start();
        } else if (check == this) {
            return; // already configured, so that is okay then
        } else {
            throw new IllegalStateException(
                    "Unable to configure '"
                            + getName()
                            + "' appender, as "
                            + check
                            + " is already configured as '"
                            + getName()
                            + "'.");
        }
    }

    public void startRecording(String name) {
        startRecording();
        Logger logger = (Logger) LogManager.getLogger(name);
        logger.addAppender(this);
    }

    /**
     * Check if logger noticed provided snippet occuring in a logging message.
     *
     * @param snippet expected snippert of logging message.
     */
    public void assertTrue(String snippet) {
        for (LogEvent event : this.log) {
            String formattedMessage = event.getMessage().getFormattedMessage();
            if (formattedMessage.contains(snippet)) {
                return;
            }
        }
        fail("Log does not contain '" + snippet + "'");
    }
    /**
     * Check if logger noticed provided snippet occurring in a logging message.
     *
     * @param snippet expected snippert of logging message.
     */
    public void assertTrue(String message, String snippet) {
        for (LogEvent event : this.log) {
            String formattedMessage = getMessage(event);
            if (formattedMessage.contains(snippet)) {
                return;
            }
        }
        if (message != null) {
            fail(message);
        } else {
            fail("Log does not contain '" + snippet + "'");
        }
    }

    public void assertFalse(String expectedSnippet) {
        for (LogEvent event : this.log) {
            String formattedMessage = getMessage(event);
            Assert.assertFalse(formattedMessage.contains(expectedSnippet));
        }
    }

    public void assertFalse(String message, String expectedSnippet) {
        for (LogEvent event : this.log) {
            String formattedMessage = getMessage(event);
            Assert.assertFalse(message, formattedMessage.contains(expectedSnippet));
        }
    }
    /** Remove appender from logging configuration (and stop listening for events). */
    public void stopRecording() {
        @SuppressWarnings({
            "resource",
            "PMD.CloseResource"
        }) // current context, no need to enforce AutoClosable
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = ctx.getConfiguration();

        Appender check = configuration.getAppender(getName());
        if (check == null) {
            return; // already de-configured, so nothing to do
        } else if (check == this) {
            configuration.getAppenders().remove(getName(), this);
            this.stopRecording();
        } else {
            throw new IllegalStateException(
                    "Unable to de-configure '"
                            + getName()
                            + "' appender, as "
                            + check
                            + " is already configured as '"
                            + getName()
                            + "'.");
        }
    }
    /** Remove appender from logging configuration (and stop listening for events). */
    public void stopRecording(String name) {
        Logger logger = (Logger) LogManager.getLogger(name);
        logger.getAppenders().values().remove(this);

        stopRecording();
    }

    @Override
    public void close() throws IOException {
        try {
            @SuppressWarnings({
                "resource",
                "PMD.CloseResource"
            }) // no need to close AutoClosable loggerContext here
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration configuration = ctx.getConfiguration();
            configuration.getAppenders().values().remove(this);
        } finally {
            log.clear();
        }
    }

    /**
     * Setup a logging "bomb" rigged to explode when the warning message we want to eliminate.
     *
     * @param trigger
     */
    public void trigger(String trigger) {
        this.trigger = trigger;
    }
}
