/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** Collects what the module writes to its log, so a test can assert on the lines. */
public final class LogCapture extends Handler {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.updates-checker");

    private final List<LogRecord> records = Collections.synchronizedList(new ArrayList<>());

    /** Starts collecting. */
    public static LogCapture start() {
        LogCapture capture = new LogCapture();
        LOGGER.addHandler(capture);
        return capture;
    }

    /** Stops collecting. */
    public void stop() {
        LOGGER.removeHandler(this);
    }

    @Override
    public void publish(LogRecord record) {
        records.add(record);
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}

    /** @return a copy of everything collected so far, oldest first. */
    public List<LogRecord> records() {
        synchronized (records) {
            return new ArrayList<>(records);
        }
    }

    /** @return whether a WARNING record contains the text. */
    public boolean warned(String text) {
        for (LogRecord r : records()) {
            if (r.getLevel() == Level.WARNING && r.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }
}
