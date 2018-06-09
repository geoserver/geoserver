/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.sequence;

/**
 * Builds a new Sequence
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
public interface SequenceFactory<T> {

    Sequence<T> newSequence();
}
