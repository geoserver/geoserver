/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.IOException;

import junit.framework.Assert;

import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.password.PasswordValidator;

public class MemoryUserGroupServiceTest extends AbstractUserGroupServiceTest {



    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        store.clear();
    }

    
    @Override
    public GeoServerUserGroupService createUserGroupService(String name) throws Exception {
        MemoryUserGroupServiceConfigImpl config = (MemoryUserGroupServiceConfigImpl ) createConfigObject(name);         
        getSecurityManager().saveUserGroupService(config);
        return getSecurityManager().loadUserGroupService(name);
   }
    
    @Override
    protected SecurityUserGroupServiceConfig createConfigObject( String name ) {
        MemoryUserGroupServiceConfigImpl config = new MemoryUserGroupServiceConfigImpl();
        config.setClassName(MemoryUserGroupService.class.getName());
         config.setName(name);        
         config.setPasswordEncoderName(getPBEPasswordEncoder().getName());
         config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        return config;

     }



    public void testInsert() {
        super.testInsert();
        try {
            for (GeoServerUser user : store.getUsers()) {
                assertTrue(user.getClass()==MemoryGeoserverUser.class);
            }
            for (GeoServerUserGroup group : store.getUserGroups()) {
                assertTrue(group.getClass()==MemoryGeoserverUserGroup.class);
            }

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
    

}
