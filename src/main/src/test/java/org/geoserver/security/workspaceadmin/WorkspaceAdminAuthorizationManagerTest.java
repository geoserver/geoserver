/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import static org.geoserver.security.filter.GeoServerSecurityInterceptorFilter.ACCESS_ABSTAIN;
import static org.geoserver.security.filter.GeoServerSecurityInterceptorFilter.ACCESS_DENIED;
import static org.geoserver.security.filter.GeoServerSecurityInterceptorFilter.ACCESS_GRANTED;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;

/** Unit tests for {@link WorkspaceAdminAuthorizationManager}. */
public class WorkspaceAdminAuthorizationManagerTest {

    private SecurityMetadataSource metadataSource;
    private WorkspaceAdminAuthorizer authorizer;
    private Authentication authentication;
    private HttpServletRequest request;
    private Supplier<Authentication> authSupplier;

    private WorkspaceAdminAuthorizationManager authManager;

    @Before
    public void setUp() {
        metadataSource = mock(SecurityMetadataSource.class);
        authentication = mock(Authentication.class);
        authSupplier = () -> authentication;
        authorizer = mock(WorkspaceAdminAuthorizer.class);

        // Setup WorkspaceAdminAuthorizer.get() to return our mock
        GeoServerExtensionsHelper.singleton("workspaceAdminAuthorization", authorizer, WorkspaceAdminAuthorizer.class);

        authManager = new WorkspaceAdminAuthorizationManager(metadataSource);

        request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("/geoserver");
    }

    @After
    public void tearDown() {
        GeoServerExtensionsHelper.init(null);
    }

    @Test
    public void testCheckWhenAuthorizerNotAvailable() {
        // Test when WorkspaceAdminAuthorizer is not available
        GeoServerExtensionsHelper.clear();

        // Should abstain when authorizer not available
        AuthorizationDecision decision = authManager.check(authSupplier, request);
        assertEquals(ACCESS_ABSTAIN, decision);
    }

    @Test
    public void testCheckWhenNotFullyAuthenticated() {
        // Test when user is not fully authenticated
        when(authorizer.isFullyAuthenticated(authentication)).thenReturn(false);

        // Should deny access when not fully authenticated
        AuthorizationDecision decision = authManager.check(authSupplier, request);
        assertEquals(ACCESS_DENIED, decision);
    }

    @Test
    public void testCheckWhenNoMatchingRules() {
        when(request.getRequestURI()).thenReturn("/geoserver/rest/logging");

        // Setup authentication and request
        when(authorizer.isFullyAuthenticated(authentication)).thenReturn(true);

        // No matching rules
        when(metadataSource.getAttributes(request)).thenReturn(Collections.emptyList());

        // Should abstain when no matching rules
        AuthorizationDecision decision = authManager.check(authSupplier, request);
        assertEquals(ACCESS_ABSTAIN, decision);
    }

    @Test
    public void testCheckWhenMatchingRuleButNotWorkspaceAdmin() {
        // Setup authentication and request
        when(authorizer.isFullyAuthenticated(authentication)).thenReturn(true);

        // Create a matching rule
        WorkspaceAdminRestAccessRule rule = new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/topp", Set.of(GET));
        when(metadataSource.getAttributes(request)).thenReturn(List.of(rule));
        when(request.getRequestURI()).thenReturn("/geoserver/rest/workspaces/topp");

        // But user is not a workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(false);

        // Should abstain when matching rule but not workspace admin
        AuthorizationDecision decision = authManager.check(authSupplier, request);
        assertEquals(ACCESS_ABSTAIN, decision);
    }

    @Test
    public void testCheckWhenMatchingRuleAndWorkspaceAdmin() {
        // Setup authentication and request
        when(authorizer.isFullyAuthenticated(authentication)).thenReturn(true);

        // Create a matching rule
        WorkspaceAdminRestAccessRule rule = new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/topp", Set.of(GET));
        when(metadataSource.getAttributes(request)).thenReturn(List.of(rule));

        when(request.getRequestURI()).thenReturn("/geoserver/rest/workspaces/topp");

        // User is a workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // Should grant access when matching rule and workspace admin
        AuthorizationDecision decision = authManager.check(authSupplier, request);
        assertEquals(ACCESS_GRANTED, decision);
    }
}
