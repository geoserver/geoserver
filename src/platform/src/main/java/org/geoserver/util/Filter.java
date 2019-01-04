/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2015 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

/**
 * Simple generic Filter
 *
 * @author Niels Charlier
 * @param <T>
 */
@FunctionalInterface
public interface Filter<T> {

    public boolean accept(T obj);
}
