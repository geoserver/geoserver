/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.geofence.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/***
 * 
 * @author Niels Charlier
 *
 */
public class InternalUserResolverTest extends GeoServerSystemTestSupport {
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        
        addUser("pipo", "clown", null, Arrays.asList("CIRCUS", "KLINIEK", "ZEVER"));
        addUser("jantje", "jantje", null, Arrays.asList("MOPJES", "ZEVER"));
        
        GeoServerSecurityManager secMgr = getSecurityManager();
        GeoServerRoleService roleService = secMgr.loadRoleService("default");
        
        GeoServerRoleStore rolesStore = roleService.createStore();
        
        GeoServerRole roleTest = rolesStore.createRoleObject("ROLE_TEST");
        rolesStore.addRole(roleTest);
        rolesStore.associateRoleToUser(roleTest, "role_user_test");
        rolesStore.store();
    }

    @Test
    public void testInternalUserResolver() {
        InternalUserResolver resolver = new InternalUserResolver(getSecurityManager());
        
        assertTrue(resolver.existsUser("pipo"));
        assertTrue(resolver.existsUser("jantje"));
        
        assertTrue(resolver.existsUser("role_user_test"));
        
        assertTrue(resolver.existsRole("ZEVER"));
        assertTrue(resolver.existsRole("CIRCUS"));
        assertTrue(resolver.existsRole("MOPJES"));
        assertTrue(resolver.existsRole("KLINIEK"));
        
        assertTrue(resolver.existsRole("ROLE_TEST"));
        
        Set<String> roles = resolver.getRoles("pipo");
        assertEquals(3, roles.size());
        assertTrue(roles.contains("CIRCUS"));
        assertTrue(roles.contains("ZEVER"));
        assertTrue(roles.contains("KLINIEK"));
        
        roles = resolver.getRoles("jantje");
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ZEVER"));
        assertTrue(roles.contains("MOPJES"));
        
        roles = resolver.getRoles("role_user_test");
        assertEquals(1, roles.size());
        assertTrue(roles.contains("ROLE_TEST"));
    }
}
