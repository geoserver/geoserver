/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

public interface ObjectSizeEstimator {

    /** The value returned when the size is unknowns */
    public static long UNKNOWN_SIZE = 0;

    /**
     * Returns the size of the specified object, in bytes, or {@link #UNKNOWN_SIZE} if the
     * estimation can not be performed
     */
    public long getSizeOf(Object object);
}
