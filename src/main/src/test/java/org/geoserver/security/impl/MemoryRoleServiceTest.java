/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.IOException;
import java.util.SortedSet;

import junit.framework.Assert;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.password.PasswordValidator;

public class MemoryRoleServiceTest extends AbstractRoleServiceTest {
  

    @Override
    public GeoServerRoleService createRoleService(String name) throws IOException {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName(name);
        config.setAdminRoleName(GeoServerRole.ADMIN_ROLE.getAuthority());
        GeoServerRoleService service = new MemoryRoleService();
        service.initializeFromConfig(config);
        service.setSecurityManager(GeoServerExtensions.bean(GeoServerSecurityManager.class));
        return service;
    }

    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        store.clear();
    }


    public void testInsert() {
        super.testInsert();
        try {
            for (GeoServerRole role : store.getRoles()) {
                assertTrue(role.getClass()==MemoryGeoserverRole.class);
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
    
    public void testMappedAdminRole() throws Exception {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName("testAdminRole");
        config.setAdminRoleName("adminRole");
        config.setClassName(MemoryRoleService.class.getName());
        GeoServerRoleService service = new MemoryRoleService();
        service.initializeFromConfig(config);
        GeoServerSecurityManager manager = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        service.setSecurityManager(manager);
        manager.setActiveRoleService(service);
        manager.saveRoleService(config);
        GeoServerRoleStore store = service.createStore();
        GeoServerRole adminRole = store.createRoleObject("adminRole");
        GeoServerRole role1 = store.createRoleObject("role1");
        store.addRole(adminRole);
        store.addRole(role1);
        
        store.associateRoleToUser(adminRole, "user1");
        store.associateRoleToUser(adminRole, "user2");
        store.associateRoleToUser(GeoServerRole.ADMIN_ROLE, "user2");
        store.associateRoleToUser(role1, "user3");
        store.store();
     
        MemoryUserGroupServiceConfigImpl ugconfig = new MemoryUserGroupServiceConfigImpl();         
        ugconfig.setName("testAdminRole");        
        ugconfig.setClassName(MemoryUserGroupService.class.getName());
        ugconfig.setPasswordEncoderName(getPBEPasswordEncoder().getName());
        ugconfig.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        GeoServerUserGroupService ugService = new MemoryUserGroupService();
        ugService.setSecurityManager(GeoServerExtensions.bean(GeoServerSecurityManager.class));
        ugService.initializeFromConfig(ugconfig);        

        
        RoleCalculator calc = new RoleCalculator(ugService, service);
        SortedSet<GeoServerRole> roles;
        
        roles = calc.calculateRoles(ugService.createUserObject("user1", "abc", true));
        assertTrue(roles.size()==2);
        assertTrue(roles.contains(adminRole));
        assertTrue(roles.contains(GeoServerRole.ADMIN_ROLE));
        
        roles = calc.calculateRoles(ugService.createUserObject("user2", "abc", true));
        assertTrue(roles.size()==2);
        assertTrue(roles.contains(adminRole));
        assertTrue(roles.contains(GeoServerRole.ADMIN_ROLE));

        roles = calc.calculateRoles(ugService.createUserObject("user3", "abc", true));
        assertTrue(roles.size()==1);
        assertTrue(roles.contains(role1));        

    }
}
