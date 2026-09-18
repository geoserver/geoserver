/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import static org.geoserver.security.filter.GeoServerSecurityInterceptorFilter.ACCESS_ABSTAIN;
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
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

/** Unit tests for {@link WorkspaceAdminAuthorizationManager}. */
@SuppressWarnings("deprecation")
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
        // remove the authorizer bean, the manager should abstain without it
        GeoServerExtensionsHelper.clear();

        @Nullable AuthorizationResult decision = authManager.authorize(authSupplier, request);
        assertEquals(ACCESS_ABSTAIN, decision);
    }

    @Test
    public void testCheckAbstainsWhenNotFullyAuthenticated() {
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(false);

        // abstains, letting other authorization managers handle it
        @Nullable AuthorizationResult decision = authManager.authorize(authSupplier, request);
        assertEquals(ACCESS_ABSTAIN, decision);
    }

    @Test
    public void testCheckWhenNoMatchingRules() {
        when(request.getRequestURI()).thenReturn("/geoserver/rest/logging");

        when(metadataSource.getAttributes(request)).thenReturn(Collections.emptyList());

        @Nullable AuthorizationResult decision = authManager.authorize(authSupplier, request);
        assertEquals(ACCESS_ABSTAIN, decision);
    }

    @Test
    public void testCheckWhenMatchingRuleButNotWorkspaceAdmin() {
        WorkspaceAdminRestAccessRule rule = new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/topp", Set.of(GET));
        when(metadataSource.getAttributes(request)).thenReturn(List.of(rule));
        when(request.getRequestURI()).thenReturn("/geoserver/rest/workspaces/topp");

        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(false);

        @Nullable AuthorizationResult decision = authManager.authorize(authSupplier, request);
        assertEquals(ACCESS_ABSTAIN, decision);
    }

    @Test
    public void testCheckWhenMatchingRuleAndWorkspaceAdmin() {
        WorkspaceAdminRestAccessRule rule = new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/topp", Set.of(GET));
        when(metadataSource.getAttributes(request)).thenReturn(List.of(rule));
        when(request.getRequestURI()).thenReturn("/geoserver/rest/workspaces/topp");

        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        @Nullable AuthorizationResult decision = authManager.authorize(authSupplier, request);
        assertEquals(ACCESS_GRANTED, decision);
    }
}
