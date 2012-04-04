package org.geoserver.security.impl;


public class ReadOnlyUGService extends MemoryUserGroupService {

    public ReadOnlyUGService() {
        super();            
    }
    
    @Override
    public boolean canCreateStore() {
        return false;
    }
}