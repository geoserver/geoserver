/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

/**
 * Needed to test if subclassing works
 *
 * @author christian
 */
public class MemoryGeoserverRole extends GeoServerRole {

    /** */
    private static final long serialVersionUID = 1L;

    public MemoryGeoserverRole(String role) {
        super(role);
    }
}
