/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;

/**
 * WPS information implementation
 *
 * @author Lucas Reed, Refractions Research Inc
 */
@SuppressWarnings("unchecked")
public class WPSInfoImpl extends ServiceInfoImpl implements WPSInfo {
    
    static final String KEY_CONNECTION_TIMEOUT = "connectionTimeout";
    
    static final Double DEFAULT_CONNECTION_TIMEOUT = 30.0;
    
    static final String KEY_RESOURCE_EXPIRATION_TIMEOUT = "resourceExpirationTimeout";
    
    static final int DEFAULT_RESOURCE_EXPIRATION_TIMEOUT = 60 * 5;
    
    static final String KEY_MAX_SYNCH = "maxSynchronousProcesses";
    
    static final int DEFAULT_MAX_SYNCH = Runtime.getRuntime().availableProcessors();
    
    static final String KEY_MAX_ASYNCH = "maxAsynchronousProcesses";
    
    static final int DEFAULT_MAX_ASYNCH = Runtime.getRuntime().availableProcessors();
    
    @Override
    public String getTitle() {
        return "Prototype GeoServer WPS";
    }
    
    /** 
     * Connection timeout in seconds. 
     * Using a double allows for fractional values, like 
     * as an instance, half a second ==> 0.5
     */
    Double connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    
    /**
     * Resource expiration timeout in seconds.
     */
    Integer resourceExpirationTimeout = DEFAULT_RESOURCE_EXPIRATION_TIMEOUT;
    
    /**
     * Maximum number of synchronous requests running in parallel
     */
    Integer maxSynchronousProcesses = DEFAULT_MAX_SYNCH;
    
    /**
     * Maximum number of asynchronous requests running in parallel
     */
    Integer maxAsynchronousProcesses = DEFAULT_MAX_ASYNCH;

    /**
     * List of process groups/factories.
     */
    List<ProcessGroupInfo> processGroups = new ArrayList<ProcessGroupInfo>();
    
    public WPSInfoImpl() {
        
    }

    /**
     * Returns the connection timeout (in seconds). It represents the timeout to be used 
     * during WPS execute requests, when opening the connection/reading through it.  
     * 
     * @return the timeout, or -1 if infinite timeout.
     */
    public double getConnectionTimeout() {
        if(connectionTimeout == null) {
            // check the metadata map for backwards compatibility with 2.1.x series
            MetadataMap md = getMetadata();
            if(md == null) {
                return DEFAULT_CONNECTION_TIMEOUT;
            }
            Double timeout = md.get(KEY_CONNECTION_TIMEOUT, Double.class);
            if(timeout == null) {
                return DEFAULT_CONNECTION_TIMEOUT;
            }
            connectionTimeout = timeout;
        } 
        
        return connectionTimeout;
    }

    /**
     * Sets the connection timeout (in seconds) to be used in WPS execute requests. -1 for infinite timeout 
     */
    public void setConnectionTimeout(double connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getResourceExpirationTimeout() {
        if(resourceExpirationTimeout == null) {
            // check the metadata map for backwards compatibility with 2.1.x series
            MetadataMap md = getMetadata();
            if(md == null) {
                return DEFAULT_RESOURCE_EXPIRATION_TIMEOUT;
            }
            Integer timeout = md.get(KEY_RESOURCE_EXPIRATION_TIMEOUT, Integer.class);
            if(timeout == null) {
                return DEFAULT_RESOURCE_EXPIRATION_TIMEOUT;
            }
            resourceExpirationTimeout = timeout;
        } 
        
        return resourceExpirationTimeout;
    }

    public void setResourceExpirationTimeout(int resourceExpirationTimeout) {
        this.resourceExpirationTimeout = resourceExpirationTimeout;
    }

    public int getMaxSynchronousProcesses() {
        if(maxSynchronousProcesses == null) {
            // check the metadata map for backwards compatibility with 2.1.x series
            MetadataMap md = getMetadata();
            if(md == null) {
                return DEFAULT_MAX_SYNCH;
            }
            Integer max = md.get(KEY_MAX_SYNCH, Integer.class);
            if(max == null) {
                return DEFAULT_MAX_SYNCH;
            }
            maxSynchronousProcesses = max;
        } 
        
        return maxSynchronousProcesses;
    }

    public void setMaxSynchronousProcesses(int maxSynchronousProcesses) {
        this.maxSynchronousProcesses = maxSynchronousProcesses;
    }

    public int getMaxAsynchronousProcesses() {
        if(maxAsynchronousProcesses == null) {
            // check the metadata map for backwards compatibility with 2.1.x series
            MetadataMap md = getMetadata();
            if(md == null) {
                return DEFAULT_MAX_ASYNCH;
            }
            Integer max = md.get(KEY_MAX_ASYNCH, Integer.class);
            if(max == null) {
                return DEFAULT_MAX_ASYNCH;
            }
            maxAsynchronousProcesses = max;
        } 
        
        return maxAsynchronousProcesses;
    }

    public void setMaxAsynchronousProcesses(int maxAsynchronousProcesses) {
        this.maxAsynchronousProcesses = maxAsynchronousProcesses;
    }

    @Override
    public List<ProcessGroupInfo> getProcessGroups() {
        return processGroups;
    }

    public void setProcessGroups(List<ProcessGroupInfo> processGroups) {
        this.processGroups = processGroups;
    }
}
