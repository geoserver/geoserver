/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.Serializable;

/**
 * A resource (layer) object, referenced from {@link RequestData#getResources()} 
 * monitored per request.
 * 
 * @author Andrea Aime, OpenGeo
 * @author Justin Deoliveira, OpenGeo
 */
public class ResourceData implements Serializable {

    String name;
    long accessCount;
    
    /**
     * Name of the resource.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets name of the resource.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Number of times the resource has been accessed.
     */
    public long getAccessCount() {
        return accessCount;
    }

    /**
     * Sets the number of times the resource has been accessed.
     */
    public void setAccessCount(long accessCount) {
        this.accessCount = accessCount;
    }
    
}
