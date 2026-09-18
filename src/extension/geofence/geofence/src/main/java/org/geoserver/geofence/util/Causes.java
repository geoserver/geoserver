/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.util;

/** Exception helpers for log messages that report a cause without dumping a stack trace. */
public final class Causes {

    private Causes() {}

    /**
     * The deepest cause that carries a message. The layers wrapping the real problem (Spring bean creation, Hikari pool
     * init, Guava's cache loader) repeat each other, and the innermost one is sometimes message-less, so neither end of
     * the chain reliably says what went wrong.
     */
    public static String rootCauseMessage(Throwable thrown) {
        String message = thrown.toString();
        Throwable cause = thrown;
        for (int depth = 0; cause != null && depth < 20; cause = cause.getCause(), depth++) {
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
            }
        }
        return message;
    }
}
