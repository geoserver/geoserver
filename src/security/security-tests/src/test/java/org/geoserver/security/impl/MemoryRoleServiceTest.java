/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.SortedSet;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.test.SystemTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class MemoryRoleServiceTest extends AbstractRoleServiceTest {

    @Override
    public GeoServerRoleService createRoleService(String name) throws IOException {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName(name);
        GeoServerRoleService service = new MemoryRoleService();
        service.initializeFromConfig(config);
        service.setSecurityManager(getSecurityManager());
        return service;
    }

    @Before
    public void init() throws IOException {
        service = createRoleService("test");
        store = service.createStore();
    }
    //    @After
    //    public void clearRoleService() throws IOException {
    //        store.clear();
    //    }

    @Test
    public void testInsert() throws Exception {
        super.testInsert();
        for (GeoServerRole role : store.getRoles()) {
            assertTrue(role.getClass() == MemoryGeoserverRole.class);
        }
    }

    @Test
    public void testMappedAdminRoles() throws Exception {
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName("testAdminRole");
        config.setAdminRoleName("adminRole");
        config.setGroupAdminRoleName("groupAdminRole");
        config.setClassName(MemoryRoleService.class.getName());
        GeoServerRoleService service = new MemoryRoleService();
        service.initializeFromConfig(config);
        GeoServerSecurityManager manager = GeoServerExtensions.bean(GeoServerSecurityManager.class);
        service.setSecurityManager(manager);
        manager.setActiveRoleService(service);
        manager.saveRoleService(config);

        GeoServerRoleStore store = service.createStore();
        GeoServerRole adminRole = store.createRoleObject("adminRole");
        GeoServerRole groupAdminRole = store.createRoleObject("groupAdminRole");
        GeoServerRole role1 = store.createRoleObject("role1");
        store.addRole(adminRole);
        store.addRole(groupAdminRole);
        store.addRole(role1);

        store.associateRoleToUser(adminRole, "user1");
        store.associateRoleToUser(groupAdminRole, "user1");
        store.associateRoleToUser(adminRole, "user2");
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
        assertTrue(roles.size() == 4);
        assertTrue(roles.contains(adminRole));
        assertTrue(roles.contains(GeoServerRole.ADMIN_ROLE));
        assertTrue(roles.contains(groupAdminRole));
        assertTrue(roles.contains(GeoServerRole.GROUP_ADMIN_ROLE));

        roles = calc.calculateRoles(ugService.createUserObject("user2", "abc", true));
        assertTrue(roles.size() == 2);
        assertTrue(roles.contains(adminRole));
        assertTrue(roles.contains(GeoServerRole.ADMIN_ROLE));

        roles = calc.calculateRoles(ugService.createUserObject("user3", "abc", true));
        assertTrue(roles.size() == 1);
        assertTrue(roles.contains(role1));
    }
}
