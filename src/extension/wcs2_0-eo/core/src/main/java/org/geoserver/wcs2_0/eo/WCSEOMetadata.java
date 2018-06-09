/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo;

/**
 * Keys for metadata items used by the WCS Earth Observation Extension
 *
 * @author Andrea Aime - GeoSolutions
 */
public enum WCSEOMetadata {
    ENABLED("wcseo.enabled"),
    DATASET("wcseo.dataset"),
    COUNT_DEFAULT("wcseo.describeEoCoverageSet.countDefault");

    public static final String NAMESPACE = "http://www.opengis.net/wcseo/1.0";

    public String key;

    private WCSEOMetadata(String key) {
        this.key = key;
    }
}
