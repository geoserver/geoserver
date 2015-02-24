/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import java.util.Collection;

import com.google.common.collect.Lists;

/**
 * GeoPackage Utility class.
 * 
 * @author Justin Deoliveira, Boundless
 */
public class GeoPkg {

    /**
     * package file extension
     */
    static final String EXTENSION = "gpkg";

    /**
     * format mime type
     */
    static final String MIME_TYPE = "application/x-sqlite3";

    /**
     * names/aliases for the format
     */
    static final Collection<String> NAMES = Lists.newArrayList("geopackage", "geopkg", "gpkg");
}
