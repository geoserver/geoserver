package org.geoserver.security;

import java.util.Arrays;
import java.util.List;

import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.security.authentication.TestingAuthenticationToken;

public class GeoServerSecurityManagerTest extends GeoServerSecurityTestSupport {

    public void testChangeAdminRole() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        TestingAuthenticationToken auth = new TestingAuthenticationToken("admin", "geoserver", 
            (List) Arrays.asList(GeoServerRole.ADMIN_ROLE));
        auth.setAuthenticated(true);
        assertTrue(secMgr.checkAuthenticationForAdminRole(auth));

        GeoServerRoleService roleService = secMgr.getActiveRoleService();
        GeoServerRoleStore roleStore = roleService.createStore();
        roleStore.addRole(new GeoServerRole("ROLE_FOO"));
        roleStore.store();

        SecurityRoleServiceConfig config = secMgr.loadRoleServiceConfig(roleService.getName());
        config.setAdminRoleName("ROLE_FOO");
        secMgr.saveRoleService(config);
        
        assertFalse(secMgr.checkAuthenticationForAdminRole(auth));
        auth = new TestingAuthenticationToken("admin", "geoserver", "ROLE_FOO");
        auth.setAuthenticated(true);
        assertTrue(secMgr.checkAuthenticationForAdminRole(auth));

    }
}
