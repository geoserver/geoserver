/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.MemoryRoleService;
import org.geoserver.security.impl.MemoryRoleStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it" */
public class LDAPAuthenticationProviderTest extends LDAPBaseTest {

    protected LDAPAuthenticationProvider authProvider;

    @Override
    protected void createConfig() {
        config = new LDAPSecurityServiceConfig();
    }

    protected void createAuthenticationProvider() {
        authProvider =
                (LDAPAuthenticationProvider) securityProvider.createAuthenticationProvider(config);
    }

    @RunWith(FrameworkRunner.class)
    @CreateLdapServer(
        transports = {
            @CreateTransport(
                protocol = "LDAP",
                address = "localhost",
                port = LDAPTestUtils.LDAP_SERVER_PORT
            )
        },
        allowAnonymousAccess = true
    )
    @CreateDS(
        name = "myDS",
        partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)}
    )
    @ApplyLdifFiles({"data.ldif"})
    public static class LDAPAuthenticationProviderDataTest extends LDAPAuthenticationProviderTest {

        /**
         * LdapTestUtils Test that bindBeforeGroupSearch correctly enables roles fetching on a
         * server without anonymous access enabled.
         */
        @Test
        public void testBindBeforeGroupSearch() throws Exception {
            getService().setAllowAnonymousAccess(false);

            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setBindBeforeGroupSearch(true);
            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authentication);
            assertNotNull(result);
            assertEquals("admin", result.getName());
            assertEquals(3, result.getAuthorities().size());
        }

        /**
         * Test that without bindBeforeGroupSearch we get an exception during roles fetching on a
         * server without anonymous access enabled.
         */
        @Test
        public void testBindBeforeGroupSearchRequiredIfAnonymousDisabled() throws Exception {
            // no anonymous access
            try {
                getService().setAllowAnonymousAccess(false);
                ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
                // we don't bind
                config.setBindBeforeGroupSearch(false);
                createAuthenticationProvider();
                boolean error = false;
                try {
                    authProvider.authenticate(authentication);
                } catch (Exception e) {
                    error = true;
                }
                assertTrue(error);
            } finally {
                getService().setAllowAnonymousAccess(true);
            }
        }

        /**
         * Test that authentication can be done using the couple userFilter and userFormat instead
         * of userDnPattern.
         */
        @Test
        public void testUserFilterAndFormat() throws Exception {
            getService().setAllowAnonymousAccess(true);
            // filter to extract user data
            ((LDAPSecurityServiceConfig) config).setUserFilter("(telephonenumber=1)");
            // username to bind to
            ((LDAPSecurityServiceConfig) config)
                    .setUserFormat("uid={0},ou=People,dc=example,dc=com");

            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authentication);
            assertEquals(3, result.getAuthorities().size());
        }

        /**
         * Test that authentication can be done using the couple userFilter and userFormat instead
         * of userDnPattern, using placemarks in userFilter.
         */
        @Test
        public void testUserFilterPlacemarks() throws Exception {
            getService().setAllowAnonymousAccess(true);
            // filter to extract user data
            ((LDAPSecurityServiceConfig) config).setUserFilter("(givenName={1})");
            // username to bind to
            ((LDAPSecurityServiceConfig) config)
                    .setUserFormat("uid={0},ou=People,dc=example,dc=com");

            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authentication);
            assertEquals(3, result.getAuthorities().size());

            // filter to extract user data
            ((LDAPSecurityServiceConfig) config).setUserFilter("(cn={0})");
            // username to bind to
            ((LDAPSecurityServiceConfig) config)
                    .setUserFormat("uid={0},ou=People,dc=example,dc=com");

            createAuthenticationProvider();

            result = authProvider.authenticate(authentication);
            assertEquals(3, result.getAuthorities().size());
        }

        /** Test that if and adminGroup is defined, the roles contain ROLE_ADMINISTRATOR */
        @Test
        public void testAdminGroup() throws Exception {
            getService().setAllowAnonymousAccess(true);
            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setAdminGroup("other");

            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authenticationOther);
            boolean foundAdmin = false;
            for (GrantedAuthority authority : result.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRATOR")) {
                    foundAdmin = true;
                }
            }
            assertTrue(foundAdmin);
        }

        /** Test that if and groupAdminGroup is defined, the roles contain ROLE_GROUP_ADMIN */
        @Test
        public void testGroupAdminGroup() throws Exception {
            getService().setAllowAnonymousAccess(true);
            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setGroupAdminGroup("other");

            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authenticationOther);
            boolean foundAdmin = false;
            for (GrantedAuthority authority : result.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase("ROLE_GROUP_ADMIN")) {
                    foundAdmin = true;
                }
            }
            assertTrue(foundAdmin);
        }

        /** Test that active role service is applied in the LDAPAuthenticationProvider */
        @Test
        public void testRoleService() throws Exception {
            getService().setAllowAnonymousAccess(true);
            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");

            createAuthenticationProvider();

            authProvider.setSecurityManager(securityManager);
            securityManager.setProviders(Collections.singletonList(authProvider));
            MemoryRoleStore roleService = new MemoryRoleStore();
            roleService.initializeFromService(new MemoryRoleService());
            roleService.setSecurityManager(securityManager);
            GeoServerRole role = roleService.createRoleObject("MyRole");
            roleService.addRole(role);
            roleService.associateRoleToUser(role, "other");
            securityManager.setActiveRoleService(roleService);

            Authentication result = authProvider.authenticate(authenticationOther);
            assertTrue(result.getAuthorities().contains(role));
            assertEquals(3, result.getAuthorities().size());
        }

        /** Tests LDAP hierarchical nested groups search. */
        @Test
        public void testHierarchicalGroupSearch() throws Exception {
            getService().setAllowAnonymousAccess(true);

            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setBindBeforeGroupSearch(false);
            // activate hierarchical group search
            config.setUseNestedParentGroups(true);
            config.setNestedGroupSearchFilter("member=cn={1}");
            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authenticationNested);
            assertNotNull(result);
            assertEquals("nestedUser", result.getName());
            assertEquals(3, result.getAuthorities().size());
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .anyMatch(x -> "ROLE_NESTED".equals(x.getAuthority())));
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .anyMatch(x -> "ROLE_EXTRA".equals(x.getAuthority())));
        }

        /** Tests LDAP hierarchical nested groups search. */
        @Test
        public void testBindBeforeHierarchicalGroupSearch() throws Exception {
            getService().setAllowAnonymousAccess(false);

            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setBindBeforeGroupSearch(true);
            // activate hierarchical group search
            config.setUseNestedParentGroups(true);
            config.setNestedGroupSearchFilter("member=cn={1}");
            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authenticationNested);
            assertNotNull(result);
            assertEquals("nestedUser", result.getName());
            assertEquals(3, result.getAuthorities().size());
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .anyMatch(x -> "ROLE_NESTED".equals(x.getAuthority())));
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .anyMatch(x -> "ROLE_EXTRA".equals(x.getAuthority())));
        }

        /** Tests LDAP hierarchical nested groups search disabled. */
        @Test
        public void testBindBeforeHierarchicalDisabledGroupSearch() throws Exception {
            getService().setAllowAnonymousAccess(false);

            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");
            config.setBindBeforeGroupSearch(true);
            // activate hierarchical group search
            config.setUseNestedParentGroups(false);
            createAuthenticationProvider();

            Authentication result = authProvider.authenticate(authenticationNested);
            assertNotNull(result);
            assertEquals("nestedUser", result.getName());
            assertEquals(2, result.getAuthorities().size());
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .anyMatch(x -> "ROLE_NESTED".equals(x.getAuthority())));
            assertTrue(
                    result.getAuthorities()
                            .stream()
                            .noneMatch(x -> "ROLE_EXTRA".equals(x.getAuthority())));
        }
    }

    @RunWith(FrameworkRunner.class)
    @CreateLdapServer(
        transports = {
            @CreateTransport(
                protocol = "LDAP",
                address = "localhost",
                port = LDAPTestUtils.LDAP_SERVER_PORT
            )
        },
        allowAnonymousAccess = true
    )
    @CreateDS(
        name = "myDS",
        partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)}
    )
    @ApplyLdifFiles({"data3.ldif"})
    public static class LDAPAuthenticationProviderData3Test extends LDAPAuthenticationProviderTest {

        /**
         * Test that LDAPAuthenticationProvider finds roles even if there is a colon in the password
         */
        @Test
        public void testColonPassword() throws Exception {
            getService().setAllowAnonymousAccess(true);
            ((LDAPSecurityServiceConfig) config).setUserDnPattern("uid={0},ou=People");

            createAuthenticationProvider();

            authentication = new UsernamePasswordAuthenticationToken("colon", "da:da");

            Authentication result = authProvider.authenticate(authentication);
            assertEquals(2, result.getAuthorities().size());
        }
    }
}
