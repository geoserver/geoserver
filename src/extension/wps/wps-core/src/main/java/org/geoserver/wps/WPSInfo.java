/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import org.geoserver.config.ServiceInfo;

/**
 * Configuration related
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public interface WPSInfo extends ServiceInfo {

    /**
     * Returns the connection timeout (in seconds). It represents the timeout to be used 
     * during WPS execute requests, when opening the connection/reading through it.  
     * 
     * @return the timeout, or -1 if infinite timeout.
     */
    double getConnectionTimeout();

    /**
     * Sets the connection timeout (in seconds) to be used in WPS execute requests. -1 for infinite timeout 
     */
    void setConnectionTimeout(double timeout);
    
}