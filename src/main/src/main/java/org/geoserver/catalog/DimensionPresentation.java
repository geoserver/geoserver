/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * The presentation mode for time/elevation lists
 *
 * @author Simone Giannecchini - GeoSolutions
 */
public enum DimensionPresentation {
    /** List of possible values */
    LIST,
    /** Start, end and resolution */
    DISCRETE_INTERVAL,
    /** Start and end, all possible values in between are valid */
    CONTINUOUS_INTERVAL;
}
