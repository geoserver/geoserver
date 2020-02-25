/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import org.geoserver.security.GeoServerUserGroupService;

/**
 * Needed to test if subclassing works
 *
 * @author christian
 */
public class MemoryGeoserverUser extends GeoServerUser {

    private static final long serialVersionUID = 1L;

    public MemoryGeoserverUser(String username, GeoServerUserGroupService service) {
        super(username);
    }

    public MemoryGeoserverUser(MemoryGeoserverUser other) {
        super(other);
    }

    @Override
    public GeoServerUser copy() {
        return new MemoryGeoserverUser(this);
    }

    @Override
    public void eraseCredentials() {
        // do nothing
    }
}
