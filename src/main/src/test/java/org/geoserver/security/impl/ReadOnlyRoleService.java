/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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