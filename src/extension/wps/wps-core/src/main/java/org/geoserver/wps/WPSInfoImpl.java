/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import org.geoserver.config.impl.ServiceInfoImpl;

/**
 * WPS information implementation
 *
 * @author Lucas Reed, Refractions Research Inc
 */
@SuppressWarnings("unchecked")
public class WPSInfoImpl extends ServiceInfoImpl implements WPSInfo {
    static final double DEFAULT_CONNECTION_TIMEOUT = 30;
    
    @Override
    public String getTitle() {
        return "Prototype GeoServer WPS";
    }
    
    /** 
     * Connection timeout in seconds. 
     * Using a double to allows fractional values, like 
     * as an instance, half a second ==> 0.5
     */
    double connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    /**
     * Returns the connection timeout (in seconds). It represents the timeout to be used 
     * during WPS execute requests, when opening the connection/reading through it.  
     * 
     * @return the timeout, or -1 if infinite timeout.
     */
    public double getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the connection timeout (in seconds) to be used in WPS execute requests. -1 for infinite timeout 
     */
    public void setConnectionTimeout(double connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    
}