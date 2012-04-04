/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.auth;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class GeoServerRootAuthenticationProviderTest extends GeoServerSecurityTestSupport {

    public void testRootProvider() throws Exception {
        
        // Check if the root provider is the first
        AuthenticationProvider first = getSecurityManager().getProviders().get(0);
        assertEquals(GeoServerRootAuthenticationProvider.class, first.getClass());
        
        GeoServerRootAuthenticationProvider provider = new GeoServerRootAuthenticationProvider();
        provider.setSecurityManager(getSecurityManager());
        provider.initializeFromConfig(null);
       
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("abc", null);
        
        assertTrue(provider.supports(token.getClass()));
        assertFalse(provider.supports(RememberMeAuthenticationToken.class));
        
        assertNull(provider.authenticate(token));
        
        token = new UsernamePasswordAuthenticationToken(GeoServerUser.ROOT_USERNAME, null);
        assertNull(provider.authenticate(token));
        
        token = new UsernamePasswordAuthenticationToken(GeoServerUser.ROOT_USERNAME, "abc");
        assertNull(provider.authenticate(token));

        String masterPassword = getMasterPassword();
        token = new UsernamePasswordAuthenticationToken(GeoServerUser.ROOT_USERNAME, masterPassword);
        token.setDetails("hallo");
        UsernamePasswordAuthenticationToken result = (UsernamePasswordAuthenticationToken)
                provider.authenticate(token);
        
        assertNotNull(result);
        assertNull(result.getCredentials());
        assertEquals(GeoServerUser.ROOT_USERNAME,result.getPrincipal());
        assertEquals("hallo",result.getDetails());        
    }

    public void testAuthWithChangedAdminRole() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        GeoServerRoleService roleService = secMgr.getActiveRoleService();
        GeoServerRoleStore roleStore = roleService.createStore();
        roleStore.addRole(new GeoServerRole("ROLE_FOO"));
        roleStore.store();

        SecurityRoleServiceConfig config = secMgr.loadRoleServiceConfig(roleService.getName());
        config.setAdminRoleName("ROLE_FOO");
        secMgr.saveRoleService(config);

        GeoServerRootAuthenticationProvider provider = new GeoServerRootAuthenticationProvider();
        provider.setSecurityManager(secMgr);
        provider.initializeFromConfig(null);

        String masterPassword = getMasterPassword();
        UsernamePasswordAuthenticationToken token 
            = new UsernamePasswordAuthenticationToken(GeoServerUser.ROOT_USERNAME, masterPassword);
        Authentication auth = provider.authenticate(token);
        assertTrue(auth.isAuthenticated());
        assertEquals(GeoServerUser.ROOT_USERNAME, auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new GeoServerRole("ROLE_FOO")));
    }
}
