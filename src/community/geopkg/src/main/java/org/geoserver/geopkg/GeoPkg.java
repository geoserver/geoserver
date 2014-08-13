/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
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
    public static final String EXTENSION = "gpkg";

    /**
     * format mime type
     */
    public static final String MIME_TYPE = "application/x-gpkg";

    /**
     * names/aliases for the format
     */
    public static final Collection<String> NAMES = Lists.newArrayList("geopackage", "geopkg", "gpkg");
}
