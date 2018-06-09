/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.sequence;

/**
 * Like an iterator, but with the simplification that null values are used to mark the end of the
 * sequence
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
public interface Sequence<T> {

    /** Returns the next item, or null if the sequence is completed */
    T next();
}
