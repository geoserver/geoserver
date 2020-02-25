/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.prefs.Preferences;
import org.geotools.util.LineWriter;
import org.geotools.util.logging.Logging;

/**
 * <code>Log4JFormatter</code> looks like:
 *
 * <blockquote>
 *
 * <pre>
 * [core FINE] A log message logged with level FINE from the "org.geotools.core"
 * logger.</pre>
 *
 * </blockquote>
 *
 * A formatter writting log message on a single line. This formatter is used by GeoServer instead of
 * {@link SimpleFormatter}. The main difference is that this formatter use only one line per message
 * instead of two. For example, a message formatted by
 *
 * @author Martin Desruisseaux
 * @author Rob Hranac
 * @version $Id: Log4JFormatter.java,v 1.3 2002/08/19 18:15:30 desruisseaux Exp
 */
public class Log4JFormatter extends Formatter {
    /** The string to write at the begining of all log headers (e.g. "[FINE core]") */
    private static final String PREFIX = "[";

    /**
     * The string to write at the end of every log header (e.g. "[FINE core]"). It should includes
     * the spaces between the header and the message body.
     */
    private static final String SUFFIX = "] ";

    /**
     * The string to write at the end of every log header (e.g. "[FINE core]"). It should includes
     * the spaces between the header and the message body.
     */
    private static long startMillis;

    /**
     * The line separator. This is the value of the "line.separator" property at the time the <code>
     * Log4JFormatter</code> was created.
     */
    private final String lineSeparator = System.getProperty("line.separator", "\n");

    /**
     * The line separator for the message body. This line always begin with {@link #lineSeparator},
     * followed by some amount of spaces in order to align the message.
     */
    private String bodyLineSeparator = lineSeparator;

    /**
     * Buffer for formatting messages. We will reuse this buffer in order to reduce memory
     * allocations.
     */
    private final StringBuffer buffer;

    /**
     * The line writer. This object transform all "\r", "\n" or "\r\n" occurences into a single line
     * separator. This line separator will include space for the marging, if needed.
     */
    private final LineWriter writer;

    /**
     * Construct a <code>Log4JFormatter</code>.
     *
     * @param base The base logger name. This is used for shortening the logger name when formatting
     *     message. For example, if the base logger name is "org.geotools" and a log record come
     *     from the "org.geotools.core" logger, it will be formatted as "[LEVEL core]" (i.e. the
     *     "org.geotools" part is ommited).
     */
    public Log4JFormatter(final String base) {
        Log4JFormatter.startMillis = System.currentTimeMillis();

        final StringWriter str = new StringWriter();
        writer = new LineWriter(str);
        buffer = str.getBuffer();
    }

    /**
     * Format the given log record and return the formatted string.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     * @throws AssertionError Should never occur.
     */
    public synchronized String format(final LogRecord record) {
        final String recordLevel = record.getLevel().getLocalizedName();

        try {
            buffer.setLength(1);

            final Long millis = Long.valueOf(record.getMillis() - startMillis);
            writer.write(millis.toString());
            writer.write(" ");
            writer.write(PREFIX);
            writer.write(recordLevel);
            writer.write(SUFFIX);

            if (record.getSourceClassName() != null) {
                writer.write(record.getSourceClassName());
            }

            writer.write(" - ");

            /*
             * Now format the message. We will use a line separator made of
             * the usual EOL ("\r", "\n", or "\r\n", which is plateform
             * specific) following by some amout of space in order to align
             * message body.
             */
            writer.setLineSeparator(bodyLineSeparator);

            if (record.getMessage() == null) {
                record.setMessage("null");
            }

            writer.write(formatMessage(record));
            writer.setLineSeparator(lineSeparator);
            writer.write('\n');

            if (record.getThrown() != null) {
                try {
                    writer.write(getStackTrace(record.getThrown()));
                } catch (Exception e) {
                    // do not write the exception...
                }
            }

            writer.flush();
        } catch (IOException exception) {
            // Should never happen, since we are writting into a StringBuffer.
            throw new AssertionError(exception);
        }

        return buffer.toString();
    }

    /** Returns the full stack trace of the given exception */
    private String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();

        return sw.toString();
    }

    /**
     * Setup a <code>Log4JFormatter</code> for the specified logger and its children. This method
     * search for all instances of {@link ConsoleHandler} using the {@link SimpleFormatter}. If such
     * instances are found, they are replaced by a single instance of <code>Log4JFormatter</code>
     * writting to the {@linkPlain System#out standard output stream} (instead of the {@linkPlain
     * System#err standard error stream}). This action has no effect on any loggers outside the
     * <code>base</code> namespace.
     *
     * @param base The base logger name to apply the change on (e.g. "org.geotools").
     * @param filterLevel The level to log at - overrides user prefs.
     */
    public static void init(final String base, Level filterLevel) {
        Formatter log4j = null;

        final Logger logger = org.geotools.util.logging.Logging.getLogger(base);

        // This little routine may be a bit buggy, but it's the best I've got
        // to make the levels change as we reload the dto's.  Feel free to
        // improve.  ch
        if (!logger.getUseParentHandlers()) {
            logger.setLevel(filterLevel);

            if (logger.getHandlers().length > 0) {
                Handler handler = logger.getHandlers()[0];

                // this should be the right handler, if set with geoserver.
                if (handler != null) {
                    handler.setLevel(filterLevel);
                }
            }
        }

        for (Logger parent = logger; parent.getUseParentHandlers(); ) {
            parent = parent.getParent();

            if (parent == null) {
                break;
            }

            final Handler[] handlers = parent.getHandlers();

            if (handlers != null) {
                for (int i = 0; i < handlers.length; i++) {
                    /*
                     * Search for a ConsoleHandler. Search is performed in the target
                     * handler and all its parent loggers. When a ConsoleHandler is
                     * found, it will be replaced by the Stdout handler for 'logger'
                     * only.
                     */
                    Handler handler = handlers[i];

                    if (handler.getClass().equals(ConsoleHandler.class)) {
                        final Formatter formatter = handler.getFormatter();

                        if (formatter.getClass().equals(SimpleFormatter.class)) {
                            if (log4j == null) {
                                log4j = new Log4JFormatter(base);
                            }

                            try {
                                logger.removeHandler(handler);
                                handler = new Stdout(handler, log4j);
                                handler.setLevel(filterLevel);
                            } catch (UnsupportedEncodingException exception) {
                                unexpectedException(exception);
                            } catch (SecurityException exception) {
                                unexpectedException(exception);
                            }
                        }
                    }

                    if (handler.getClass().equals(Stdout.class)) {
                        handler.setLevel(filterLevel);
                    }

                    logger.addHandler(handler);
                    logger.setLevel(filterLevel);
                }
            }
        }

        // Artie Konin suggested fix (see GEOS-366)
        if (0 == logger.getHandlers().length) // seems that getHandlers() cannot return null
        {
            log4j = new Log4JFormatter(base);

            Handler handler = new Stdout();
            handler.setFormatter(log4j);
            handler.setLevel(filterLevel);

            logger.addHandler(handler);
        }

        logger.setUseParentHandlers(false);
    }

    /**
     * Invoked when an error occurs during the initialization.
     *
     * @param e the error that occured.
     */
    private static void unexpectedException(final Exception e) {
        Logging.unexpectedException(Log4JFormatter.class, "init", e);
    }

    /**
     * Set the header width. This is the default value to use for {@link #margin} for next {@link
     * Log4JFormatter} to be created.
     *
     * @param margin the size of the margin to set.
     */
    static void setHeaderWidth(final int margin) {
        Preferences.userNodeForPackage(Log4JFormatter.class).putInt("logging.header", margin);
    }

    /**
     * A {@link ConsoleHandler} sending output to {@link System#out} instead of {@link System#err}
     * This handler will use a {@link Log4JFormatter} writting log message on a single line.
     *
     * @task TODO: This class should subclass {@link ConsoleHandler}. Unfortunatly, this is
     *     currently not possible because {@link ConsoleHandler#setOutputStream} close {@link
     *     System#err}. If this bug get fixed, then {@link #close} no longer need to be overriden.
     */
    private static final class Stdout extends StreamHandler {
        public Stdout() {
            super();
        }

        /**
         * Construct a handler.
         *
         * @param handler The handler to copy properties from.
         * @param formatter The formatter to use.
         * @throws UnsupportedEncodingException if the encoding is not valid.
         */
        public Stdout(final Handler handler, final Formatter formatter)
                throws UnsupportedEncodingException {
            super(System.out, formatter);
            setErrorManager(handler.getErrorManager());
            setFilter(handler.getFilter());
            setLevel(handler.getLevel());
            setEncoding(handler.getEncoding());
        }

        /**
         * Publish a {@link LogRecord} and flush the stream.
         *
         * @param record the log record to publish.
         */
        public void publish(final LogRecord record) {
            super.publish(record);
            flush();
        }

        /**
         * Override {@link StreamHandler#close} to do a flush but not to close the output stream.
         * That is, we do <b>not</b> close {@link System#out}.
         */
        public void close() {
            flush();
        }
    }
}
