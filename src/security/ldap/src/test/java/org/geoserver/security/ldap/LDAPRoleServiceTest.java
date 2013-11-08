/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.SortedSet;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class LDAPRoleServiceTest extends LDAPBaseTest {
    GeoServerRoleService service;
    
    public void createRoleService() throws IOException {
        service = new LDAPRoleService();
        config.setGroupSearchFilter("member=cn={0}");
        service.initializeFromConfig(config);
    }
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }    
    
    @Test
    public void testGetRoles() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl,
                basePath));
        
        checkAllRoles();
    }
    
    @Test
    public void testGetRolesAuthenticated() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(false, ldapServerUrl,
                basePath));
        configureAuthentication();
        checkAllRoles();
    }
    
    @Test
    public void testGetRolesCount() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl,
                basePath));
        
        checkRoleCount();
    }
    
    @Test
    public void testGetRolesCountAuthenticated() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl,
                basePath));
        configureAuthentication();
        checkRoleCount();
    }
    
    @Test
    public void testGetRoleByName() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl,
                basePath));
        
        checkRoleByName();
    }
    
    @Test
    public void testGetRoleByNameAuthenticated() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(false, ldapServerUrl,
                basePath));
        configureAuthentication();
        checkRoleByName();
    }
    
    @Test
    public void testGetAdminRoles() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl,
                basePath));
        
        checkAdminRoles();
    }
    
    @Test
    public void testGetAdminRolesAuthenticated() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(false, ldapServerUrl,
                basePath));
        configureAuthentication();
        checkAdminRoles();
    }
    
    @Test
    public void testGetRolesForUser() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl,
                basePath));
        
        checkUserRoles("admin");
    }
    
    @Test
    public void testGetRolesForUserAuthenticated() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(false, ldapServerUrl,
                basePath));
        
        configureAuthentication();
        checkUserRoles("admin");
    }
    
    @Test
    public void testGetUserNamesForRole() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl,
                basePath));
        
        checkUserNamesForRole("admin", 1);
        checkUserNamesForRole("other", 2);
    }
    
    private void configureAuthentication() {
        ((LDAPRoleServiceConfig)config).setUser("uid=admin,ou=People,dc=example,dc=com");//("uid=admin,ou=People,dc=example,dc=com");
        ((LDAPRoleServiceConfig)config).setPassword("admin");
        config.setBindBeforeGroupSearch(true);
    }

    private void checkAdminRoles() throws IOException {
        config.setAdminGroup("admin");
        config.setGroupAdminGroup("other");
        createRoleService();
        
        assertNotNull(service.getAdminRole());
        assertNotNull(service.getGroupAdminRole());
        
        config.setAdminGroup("dummy1");
        config.setGroupAdminGroup("dummy2");
        createRoleService();
        
        assertNull(service.getAdminRole());
        assertNull(service.getGroupAdminRole());
    }
    
    private void checkUserNamesForRole(String roleName, int expected) throws IOException {
        createRoleService();
        
        SortedSet<String> userNames = service.getUserNamesForRole(new GeoServerRole(roleName));
        assertNotNull(userNames);
        assertEquals(expected, userNames.size());
    }
    
    private void checkRoleByName() throws IOException {
        createRoleService();
        
        assertNotNull(service.getRoleByName("admin"));
        assertNull(service.getRoleByName("dummy"));
    }
    
    private void checkRoleCount() throws IOException {
        createRoleService();
        
        assertTrue(service.getRoleCount() > 0);
    }
    
    private void checkAllRoles() throws IOException {
        createRoleService();
        
        SortedSet<GeoServerRole> roles = service.getRoles();
        assertNotNull(roles);
        assertTrue(roles.size() > 0);
        GeoServerRole role = roles.first();
        assertTrue(role.toString().startsWith("ROLE_"));
        assertEquals(role.toString().toUpperCase(), role.toString());
    }
    
    
    
    private void checkUserRoles(String username) throws IOException {
        createRoleService();
        SortedSet<GeoServerRole> allRoles = service.getRoles();
        SortedSet<GeoServerRole> roles = service.getRolesForUser(username);
        assertNotNull(roles);
        assertTrue(roles.size() > 0);
        assertTrue(roles.size() < allRoles.size());
        GeoServerRole role = roles.first();
        assertTrue(role.toString().startsWith("ROLE_"));
        assertEquals(role.toString().toUpperCase(), role.toString());
    }
    
    @Override
    protected void createConfig()
    {
        config = new LDAPRoleServiceConfig();
    }
}
