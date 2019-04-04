/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

/**
 * Assorted utilities for this package
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
class PackagedUtils {

    private PackagedUtils() {}

    /**
     * Ensures that the provided argument is not <code>null</code>.
     *
     * <p>If it <code>null</code> it must throw a {@link NullPointerException}.
     *
     * @param argument argument to check for <code>null</code>.
     */
    static void ensureNotNull(final Object argument) {
        ensureNotNull(argument, "Argument cannot be null");
    }

    /**
     * Ensures that the provided argument is not <code>null</code>.
     *
     * <p>If it <code>null</code> it must throw a {@link NullPointerException}.
     *
     * @param argument argument to check for <code>null</code>.
     * @param message leading message to print out in case the test fails.
     */
    static void ensureNotNull(final Object argument, final String message) {
        if (message == null) throw new NullPointerException("Message cannot be null");
        if (argument == null) throw new NullPointerException(message + " cannot be null");
    }
}
