/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

/**
 * EO standard style names.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public interface EoStyles {

    public static final String DEFAULT_OUTLINE_STYLE = "red";
    public static final String DEFAULT_BITMASK_STYLE = "yellow";

    public static final String[] EO_STYLE_NAMES =
            new String[] {
                "black", "blue", "brown", "cyan", "green", "magenta", "orange", "red", "white",
                "yellow"
            };
}
