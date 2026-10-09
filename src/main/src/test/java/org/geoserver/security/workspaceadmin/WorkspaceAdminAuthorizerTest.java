/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WorkspaceAccessLimits;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** Unit tests for {@link WorkspaceAdminAuthorizer}. */
public class WorkspaceAdminAuthorizerTest {

    private WorkspaceAdminRESTAccessRuleDAO dao;
    private GeoServerSecurityManager securityManager;
    private SecureCatalogImpl secureCatalog;
    private ResourceAccessManager accessManager;
    private Catalog catalog;
    private GeoServer geoServer;

    private WorkspaceAdminAuthorizer authorizer;

    @Before
    public void setUp() {
        dao = mock(WorkspaceAdminRESTAccessRuleDAO.class);
        securityManager = mock(GeoServerSecurityManager.class);
        secureCatalog = mock(SecureCatalogImpl.class);
        accessManager = mock(ResourceAccessManager.class);
        catalog = mock(Catalog.class);
        geoServer = mock(GeoServer.class);

        when(secureCatalog.getResourceAccessManager()).thenReturn(accessManager);
        when(geoServer.getCatalog()).thenReturn(catalog);

        GeoServerExtensionsHelper.singleton("secureCatalog", secureCatalog, SecureCatalogImpl.class);
        GeoServerExtensionsHelper.singleton(
                "geoServerSecurityManager", securityManager, GeoServerSecurityManager.class);
        GeoServerExtensionsHelper.singleton("geoServer", geoServer, GeoServer.class);

        authorizer = new WorkspaceAdminAuthorizer(dao);
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        GeoServerExtensionsHelper.init(null);
    }

    @Test
    public void isFullyAuthenticated_nullAuth() {
        assertFalse(authorizer.isFullyAuthenticated(null));
    }

    @Test
    public void isFullyAuthenticated_notAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        assertFalse(authorizer.isFullyAuthenticated(auth));
    }

    @Test
    public void isFullyAuthenticated_anonymous() {
        AnonymousAuthenticationToken auth = mock(AnonymousAuthenticationToken.class);
        when(auth.isAuthenticated()).thenReturn(true);
        assertFalse(authorizer.isFullyAuthenticated(auth));
    }

    @Test
    public void isFullyAuthenticated_fullyAuthenticated() {
        Authentication auth = new TestingAuthenticationToken("user", "password", "ROLE_USER");
        auth.setAuthenticated(true);
        assertTrue(authorizer.isFullyAuthenticated(auth));
    }

    @Test
    public void getAccessRules() {
        WorkspaceAdminRestAccessRule rule1 = new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/**", Set.of(GET));
        WorkspaceAdminRestAccessRule rule2 =
                new WorkspaceAdminRestAccessRule(2, "/rest/namespaces/**", Set.of(GET, POST));
        when(dao.getRules()).thenReturn(List.of(rule1, rule2));

        List<WorkspaceAdminRestAccessRule> rules = authorizer.getAccessRules();
        assertEquals(2, rules.size());
        assertTrue(rules.contains(rule1));
        assertTrue(rules.contains(rule2));
    }

    @Test
    public void findMatchingRule_match() {
        WorkspaceAdminRestAccessRule rule = new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/**", Set.of(GET));
        when(dao.getRules()).thenReturn(List.of(rule));

        Optional<WorkspaceAdminRestAccessRule> result = authorizer.findMatchingRule("/rest/workspaces/topp", GET);
        assertTrue(result.isPresent());
        assertEquals(rule, result.get());
    }

    @Test
    public void findMatchingRule_noMatch() {
        WorkspaceAdminRestAccessRule rule = new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/**", Set.of(GET));
        when(dao.getRules()).thenReturn(List.of(rule));

        // POST doesn't match GET-only rule
        Optional<WorkspaceAdminRestAccessRule> result = authorizer.findMatchingRule("/rest/workspaces/topp", POST);
        assertFalse(result.isPresent());
    }

    @Test
    public void isWorkspaceAdmin_notFullyAuthenticated() {
        // null auth should return false
        assertFalse(authorizer.isWorkspaceAdmin(null));
    }

    @Test
    public void isWorkspaceAdmin_anonymous() {
        AnonymousAuthenticationToken auth = mock(AnonymousAuthenticationToken.class);
        when(auth.isAuthenticated()).thenReturn(true);
        assertFalse(authorizer.isWorkspaceAdmin(auth));
    }

    @Test
    public void isWorkspaceAdmin_delegatesToResourceAccessManager() {
        Authentication auth = new TestingAuthenticationToken("wsadmin", "password", "ROLE_USER");
        auth.setAuthenticated(true);

        when(accessManager.isWorkspaceAdmin(same(auth), any(Catalog.class))).thenReturn(true);

        assertTrue(authorizer.isWorkspaceAdmin(auth));
        verify(accessManager).isWorkspaceAdmin(same(auth), any(Catalog.class));
    }

    @Test
    public void isWorkspaceAdmin_notWorkspaceAdmin() {
        Authentication auth = new TestingAuthenticationToken("user", "password", "ROLE_USER");
        auth.setAuthenticated(true);

        when(accessManager.isWorkspaceAdmin(same(auth), any(Catalog.class))).thenReturn(false);

        assertFalse(authorizer.isWorkspaceAdmin(auth));
    }

    @Test
    public void isWorkspaceAdmin_cachesResultInRequestScope() {
        Authentication auth = new TestingAuthenticationToken("wsadmin", "password", "ROLE_USER");
        auth.setAuthenticated(true);

        // set up request attributes for caching
        RequestContextHolder.setRequestAttributes(new SimpleRequestAttributes());

        when(accessManager.isWorkspaceAdmin(same(auth), any(Catalog.class))).thenReturn(true);

        // call twice
        assertTrue(authorizer.isWorkspaceAdmin(auth));
        assertTrue(authorizer.isWorkspaceAdmin(auth));

        // should only have called the access manager once due to caching
        verify(accessManager, times(1)).isWorkspaceAdmin(same(auth), any(Catalog.class));
    }

    @Test
    public void canAccess_adminAlwaysGranted() {
        Authentication auth = new TestingAuthenticationToken("admin", "password", "ROLE_ADMINISTRATOR");
        auth.setAuthenticated(true);

        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(true);

        // admin should have access regardless of rules
        assertTrue(authorizer.canAccess(auth, "/rest/anything", GET));
    }

    @Test
    public void canAccess_workspaceAdminWithMatchingRule() {
        Authentication auth = new TestingAuthenticationToken("wsadmin", "password", "ROLE_USER");
        auth.setAuthenticated(true);

        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(false);

        WorkspaceAdminRestAccessRule rule =
                new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/**", Set.of(GET, PUT));
        when(dao.getRules()).thenReturn(List.of(rule));
        when(accessManager.isWorkspaceAdmin(eq(auth), any(Catalog.class))).thenReturn(true);

        assertTrue(authorizer.canAccess(auth, "/rest/workspaces/topp", GET));
        assertTrue(authorizer.canAccess(auth, "/rest/workspaces/topp", PUT));
    }

    @Test
    public void canAccess_workspaceAdminNoMatchingRule() {
        Authentication auth = new TestingAuthenticationToken("wsadmin", "password", "ROLE_USER");
        auth.setAuthenticated(true);

        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(false);

        WorkspaceAdminRestAccessRule rule = new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/**", Set.of(GET));
        when(dao.getRules()).thenReturn(List.of(rule));
        when(accessManager.isWorkspaceAdmin(eq(auth), any(Catalog.class))).thenReturn(true);

        // DELETE doesn't match GET-only rule
        assertFalse(authorizer.canAccess(auth, "/rest/workspaces/topp", DELETE));
    }

    @Test
    public void canAccess_notWorkspaceAdmin() {
        Authentication auth = new TestingAuthenticationToken("user", "password", "ROLE_USER");
        auth.setAuthenticated(true);

        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(false);

        WorkspaceAdminRestAccessRule rule = new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/**", Set.of(GET));
        when(dao.getRules()).thenReturn(List.of(rule));
        when(accessManager.isWorkspaceAdmin(eq(auth), any(Catalog.class))).thenReturn(false);

        // rule matches but user is not a workspace admin
        assertFalse(authorizer.canAccess(auth, "/rest/workspaces/topp", GET));
    }

    @Test
    public void getWorkspaceAccessLimits_workspaceExists() {
        Authentication auth = new TestingAuthenticationToken("wsadmin", "password", "ROLE_USER");
        WorkspaceInfo workspace = mock(WorkspaceInfo.class);
        WorkspaceAccessLimits limits = mock(WorkspaceAccessLimits.class);

        when(catalog.getWorkspaceByName("topp")).thenReturn(workspace);
        when(accessManager.getAccessLimits(auth, workspace)).thenReturn(limits);

        WorkspaceAccessLimits result = authorizer.getWorkspaceAccessLimits(auth, "topp");
        assertNotNull(result);
        assertEquals(limits, result);
    }

    @Test
    public void getWorkspaceAccessLimits_workspaceNotFound() {
        Authentication auth = new TestingAuthenticationToken("wsadmin", "password", "ROLE_USER");

        when(catalog.getWorkspaceByName("nonexistent")).thenReturn(null);

        WorkspaceAccessLimits result = authorizer.getWorkspaceAccessLimits(auth, "nonexistent");
        assertNull(result);
    }

    @Test
    public void get_returnsAuthorizerWhenAvailable() {
        GeoServerExtensionsHelper.singleton("workspaceAdminAuthorization", authorizer, WorkspaceAdminAuthorizer.class);
        Optional<WorkspaceAdminAuthorizer> result = WorkspaceAdminAuthorizer.get();
        assertTrue(result.isPresent());
    }

    @Test
    public void get_returnsEmptyWhenNotAvailable() {
        GeoServerExtensionsHelper.clear();
        Optional<WorkspaceAdminAuthorizer> result = WorkspaceAdminAuthorizer.get();
        assertFalse(result.isPresent());
    }

    /**
     * Minimal {@link RequestAttributes} implementation backed by a map, sufficient for testing the request-scope
     * caching in {@link WorkspaceAdminAuthorizer}.
     */
    private static class SimpleRequestAttributes implements RequestAttributes {
        private final Map<String, Object> attributes = new HashMap<>();

        @Override
        public Object getAttribute(String name, int scope) {
            return attributes.get(name);
        }

        @Override
        public void setAttribute(String name, Object value, int scope) {
            attributes.put(name, value);
        }

        @Override
        public void removeAttribute(String name, int scope) {
            attributes.remove(name);
        }

        @Override
        public String[] getAttributeNames(int scope) {
            return attributes.keySet().toArray(new String[0]);
        }

        @Override
        public void registerDestructionCallback(String name, Runnable callback, int scope) {
            // no-op
        }

        @Override
        public Object resolveReference(String key) {
            return null;
        }

        @Override
        public String getSessionId() {
            return "test-session";
        }

        @Override
        public Object getSessionMutex() {
            return this;
        }
    }
}
