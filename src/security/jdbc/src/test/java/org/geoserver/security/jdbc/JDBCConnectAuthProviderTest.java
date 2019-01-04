/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class JDBCConnectAuthProviderTest extends AbstractAuthenticationProviderTest {

    protected JDBCConnectAuthProviderConfig createAuthConfg(
            String name, String userGroupServiceName) {
        JDBCConnectAuthProviderConfig config = new JDBCConnectAuthProviderConfig();
        config.setName(name);
        config.setClassName(JDBCConnectAuthProvider.class.getName());
        config.setUserGroupServiceName(userGroupServiceName);
        config.setConnectURL("jdbc:h2:target/h2/security");
        config.setDriverClassName("org.h2.Driver");
        return config;
    }

    @Test
    public void testAuthentificationWithoutUserGroupService() throws Exception {
        JDBCConnectAuthProviderConfig config = createAuthConfg("jdbc1", null);
        getSecurityManager().saveAuthenticationProvider(config);
        GeoServerAuthenticationProvider provider =
                getSecurityManager().loadAuthenticationProvider("jdbc1");

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("sa", "");
        token.setDetails("details");
        assertTrue(provider.supports(token.getClass()));
        assertTrue(!provider.supports(RememberMeAuthenticationToken.class));

        Authentication auth = provider.authenticate(token);
        assertNotNull(auth);
        assertEquals("sa", auth.getPrincipal());
        assertNull(auth.getCredentials());

        assertEquals("details", auth.getDetails());
        assertEquals(1, auth.getAuthorities().size());
        checkForAuthenticatedRole(auth);

        token = new UsernamePasswordAuthenticationToken("abc", "def");
        boolean fail = false;
        try {
            if (provider.authenticate(token) == null) fail = true;
        } catch (BadCredentialsException ex) {
            fail = true;
        }
        assertTrue(fail);
    }

    @Test
    public void testAuthentificationWithUserGroupService() throws Exception {
        GeoServerRoleService roleService = createRoleService("jdbc2");
        GeoServerUserGroupService ugService = createUserGroupService("jdbc2");
        JDBCConnectAuthProviderConfig config = createAuthConfg("jdbc2", ugService.getName());
        getSecurityManager().saveAuthenticationProvider(config);
        GeoServerAuthenticationProvider provider =
                getSecurityManager().loadAuthenticationProvider("jdbc2");

        GeoServerUserGroupStore ugStore = ugService.createStore();
        GeoServerUser sa = ugStore.createUserObject("sa", "", true);
        ugStore.addUser(sa);
        ugStore.store();

        GeoServerRoleStore roleStore = roleService.createStore();
        roleStore.addRole(GeoServerRole.ADMIN_ROLE);
        roleStore.associateRoleToUser(GeoServerRole.ADMIN_ROLE, sa.getUsername());
        roleStore.store();
        getSecurityManager().setActiveRoleService(roleService);

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("sa", "");
        token.setDetails("details");
        assertTrue(provider.supports(token.getClass()));
        assertFalse(provider.supports(RememberMeAuthenticationToken.class));

        Authentication auth = provider.authenticate(token);
        assertNotNull(auth);
        assertEquals("sa", auth.getPrincipal());
        assertNull(auth.getCredentials());
        assertEquals("details", auth.getDetails());
        assertEquals(2, auth.getAuthorities().size());
        checkForAuthenticatedRole(auth);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));

        // Test disabled user
        ugStore = ugService.createStore();
        sa.setEnabled(false);
        ugStore.updateUser(sa);
        ugStore.store();

        assertNull(provider.authenticate(token));

        // test invalid user
        token = new UsernamePasswordAuthenticationToken("abc", "def");
        boolean fail = false;
        try {
            if (provider.authenticate(token) == null) fail = true;
        } catch (BadCredentialsException ex) {
            fail = true;
        } catch (UsernameNotFoundException ex) {
            fail = true;
        }

        assertTrue(fail);
    }

    @Test
    public void testAuthentificationWithRoleAssociation() throws Exception {
        GeoServerRoleService roleService = createRoleService("jdbc3");
        JDBCConnectAuthProviderConfig config = createAuthConfg("jdbc3", null);
        getSecurityManager().saveAuthenticationProvider(config);
        GeoServerAuthenticationProvider provider =
                getSecurityManager().loadAuthenticationProvider("jdbc3");

        GeoServerRoleStore roleStore = roleService.createStore();
        roleStore.addRole(GeoServerRole.ADMIN_ROLE);
        roleStore.associateRoleToUser(GeoServerRole.ADMIN_ROLE, "sa");
        roleStore.store();
        getSecurityManager().setActiveRoleService(roleService);

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("sa", "");
        token.setDetails("details");
        assertTrue(provider.supports(token.getClass()));
        assertFalse(provider.supports(RememberMeAuthenticationToken.class));

        Authentication auth = provider.authenticate(token);
        assertNotNull(auth);
        assertEquals("sa", auth.getPrincipal());
        assertNull(auth.getCredentials());
        assertEquals("details", auth.getDetails());
        assertEquals(2, auth.getAuthorities().size());
        checkForAuthenticatedRole(auth);
        assertTrue(auth.getAuthorities().contains(GeoServerRole.ADMIN_ROLE));

        // test invalid user
        token = new UsernamePasswordAuthenticationToken("abc", "def");
        boolean fail = false;
        try {
            if (provider.authenticate(token) == null) fail = true;
        } catch (BadCredentialsException ex) {
            fail = true;
        } catch (UsernameNotFoundException ex) {
            fail = true;
        }

        assertTrue(fail);
    }
}
