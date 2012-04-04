/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

/**
 * Event listener for {@link GeoServerSecurityManager}.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public interface SecurityManagerListener {

    /**
     * Event fired after a configuration change has been made.
     */
    void handlePostChanged(GeoServerSecurityManager securityManager);
}
