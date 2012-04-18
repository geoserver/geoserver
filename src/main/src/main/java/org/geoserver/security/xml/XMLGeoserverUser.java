/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */


package org.geoserver.security.xml;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;

/**
 * Needed to test if subclassing works
 * 
 * @author christian
 *
 */
public class XMLGeoserverUser extends GeoServerUser {

    private static final long serialVersionUID = 1L;

    public XMLGeoserverUser(String username) {
        super(username);
    }

    public XMLGeoserverUser(XMLGeoserverUser other) {
        super(other);
    }

    @Override
    public GeoServerUser copy() {
        return new XMLGeoserverUser(this);
    }
    
    @Override
    public void eraseCredentials() {
        // do nothing
    }
}
