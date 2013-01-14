/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

/**
 * Marker interface for {@link GeoServerSecurityFilter} implementations that implement an 
 * authentication scheme.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public interface GeoServerAuthenticationFilter {
    
    /**
     * returns <code>true</code> if the filter is
     * applicable for GUI logins. Such a filter 
     * can be put into a chain doing authentication for
     * a web interface.  
     * 
     * @return
     */
    public boolean applicableForHtml ();
    /**
     * returns <code>true</code> if the filter is applicable
     * for services (NO GUI). Such a filter can be put into
     * a chain doing authentication for  services.
     * 
     * @return
     */
    public boolean applicableForServices ();
    
}
