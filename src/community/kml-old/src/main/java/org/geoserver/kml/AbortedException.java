/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;


/**
 * Copy of the exception class defined in the SVG producer.
 * @TODO move up the package hiarachy and use single version from KML and SVG
 */
public class AbortedException extends Exception {
    public AbortedException(String msg) {
        super(msg);
    }
}
