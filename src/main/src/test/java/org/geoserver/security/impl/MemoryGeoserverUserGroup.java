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
public class MemoryGeoserverUserGroup extends GeoServerUserGroup {

    /** */
    private static final long serialVersionUID = 1L;

    public MemoryGeoserverUserGroup(String name) {
        super(name);
    }
}
