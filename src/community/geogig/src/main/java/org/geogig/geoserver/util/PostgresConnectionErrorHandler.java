/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.util;

import java.net.UnknownHostException;
import org.postgresql.util.PSQLException;

/** Utility class for building the best error message for PostgreSQL connection errors. */
public class PostgresConnectionErrorHandler {

    public static String getMessage(final Throwable t) {
        // grab the localized message for the source Throwable. If there or no other causes in the
        // stack, use the top-most message.
        String message = t.getLocalizedMessage();
        // flag indicating if we've uncovered a cause that is an instance of
        // org.postgresql.util.PSQLException.
        boolean psqlCauseFound = false;
        // start digging
        Throwable cause = t;
        while (cause != null) {
            // Get the type of the Throwable
            Class clazz = cause.getClass();
            // If we have an UnknownHostException, we're done
            if (UnknownHostException.class.isAssignableFrom(clazz)) {
                return "UnknownHostException: " + cause.getLocalizedMessage();
            }
            // if we haven't already found a PSQLException, see if this cause is one.
            if (!psqlCauseFound && PSQLException.class.isAssignableFrom(clazz)) {
                // it's a PSQLException, and it's the first one found
                psqlCauseFound = true;
                message = cause.getLocalizedMessage();
            }
            // get the next cause
            cause = cause.getCause();
        }
        return message;
    }
}
