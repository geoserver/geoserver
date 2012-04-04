package org.geoserver.security.impl;


public class ReadOnlyRoleService extends MemoryRoleService {

    public ReadOnlyRoleService() {
        super();            
    }

    @Override
    public boolean canCreateStore() {
        return false;
    }
}