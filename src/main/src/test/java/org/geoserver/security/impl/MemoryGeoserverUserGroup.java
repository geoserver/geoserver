/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.impl;

/**
 * Needed to test if subclassing works
 * 
 * @author christian
 *
 */
public class MemoryGeoserverUserGroup extends GeoServerUserGroup {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public MemoryGeoserverUserGroup(String name) {
        super(name);
        
    }

}
