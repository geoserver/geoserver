/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.iterator;

import java.util.Iterator;

/**
 * Builds a new Iterator
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
public interface IteratorFactory<T> {

    Iterator<T> newIterator();
}
