package org.geoserver.catalog.rest;

import org.geoserver.catalog.ResourceInfo;

public class AvailableResource<T extends ResourceInfo>  {
    final String name;
    
    public AvailableResource(String name) {
        super();
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
