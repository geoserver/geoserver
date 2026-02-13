/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpRequestResponseHolder;

/** Tests for {@link BearerAwareSecurityContextRepository}. */
public class BearerAwareSecurityContextRepositoryTest {

    private BearerAwareSecurityContextRepository repository;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setUp() {
        repository = new BearerAwareSecurityContextRepository();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== isBearerRequest tests ====================

    @Test
    public void testIsBearerRequestWithNullRequest() {
        assertFalse(BearerAwareSecurityContextRepository.isBearerRequest(null));
    }

    @Test
    public void testIsBearerRequestWithNoAuthorizationHeader() {
        // No Authorization header set
        assertFalse(BearerAwareSecurityContextRepository.isBearerRequest(request));
    }

    @Test
    public void testIsBearerRequestWithValidBearerToken() {
        request.addHeader("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...");
        assertTrue(BearerAwareSecurityContextRepository.isBearerRequest(request));
    }

    @Test
    public void testIsBearerRequestWithLowercaseBearer() {
        // RFC 6750 allows case-insensitive scheme
        request.addHeader("Authorization", "bearer mytoken123");
        assertTrue(BearerAwareSecurityContextRepository.isBearerRequest(request));
    }

    @Test
    public void testIsBearerRequestWithMixedCaseBearer() {
        request.addHeader("Authorization", "BeArEr mytoken123");
        assertTrue(BearerAwareSecurityContextRepository.isBearerRequest(request));
    }

    @Test
    public void testIsBearerRequestWithUppercaseBearer() {
        request.addHeader("Authorization", "BEARER mytoken123");
        assertTrue(BearerAwareSecurityContextRepository.isBearerRequest(request));
    }

    @Test
    public void testIsBearerRequestWithBasicAuth() {
        request.addHeader("Authorization", "Basic dXNlcm5hbWU6cGFzc3dvcmQ=");
        assertFalse(BearerAwareSecurityContextRepository.isBearerRequest(request));
    }

    @Test
    public void testIsBearerRequestWithDigestAuth() {
        request.addHeader("Authorization", "Digest username=\"user\", realm=\"realm\"");
        assertFalse(BearerAwareSecurityContextRepository.isBearerRequest(request));
    }

    @Test
    public void testIsBearerRequestWithEmptyAuthorizationHeader() {
        request.addHeader("Authorization", "");
        assertFalse(BearerAwareSecurityContextRepository.isBearerRequest(request));
    }

    @Test
    public void testIsBearerRequestWithBearerNoSpace() {
        // "Bearer" without the space should not match
        request.addHeader("Authorization", "Bearertoken123");
        assertFalse(BearerAwareSecurityContextRepository.isBearerRequest(request));
    }

    @Test
    public void testIsBearerRequestWithOnlyBearerKeyword() {
        request.addHeader("Authorization", "Bearer ");
        assertTrue(BearerAwareSecurityContextRepository.isBearerRequest(request));
    }

    // ==================== loadDeferredContext tests ====================

    @Test
    public void testLoadDeferredContextWithBearerToken() {
        request.addHeader("Authorization", "Bearer token123");

        DeferredSecurityContext deferredContext = repository.loadDeferredContext(request);

        assertNotNull(deferredContext);
        // The context should be empty initially (no authentication set)
        SecurityContext context = deferredContext.get();
        assertNotNull(context);
    }

    @Test
    public void testLoadDeferredContextWithoutBearerToken() {
        // No Authorization header - should use session repository
        DeferredSecurityContext deferredContext = repository.loadDeferredContext(request);

        assertNotNull(deferredContext);
        SecurityContext context = deferredContext.get();
        assertNotNull(context);
    }

    @Test
    public void testLoadDeferredContextWithBasicAuth() {
        request.addHeader("Authorization", "Basic dXNlcm5hbWU6cGFzc3dvcmQ=");

        DeferredSecurityContext deferredContext = repository.loadDeferredContext(request);

        assertNotNull(deferredContext);
        // Should use session repository for non-bearer auth
    }

    // ==================== loadContext (deprecated) tests ====================

    @Test
    @SuppressWarnings("deprecation")
    public void testLoadContextWithBearerToken() {
        request.addHeader("Authorization", "Bearer token123");
        HttpRequestResponseHolder holder = new HttpRequestResponseHolder(request, response);

        SecurityContext context = repository.loadContext(holder);

        assertNotNull(context);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLoadContextWithoutBearerToken() {
        HttpRequestResponseHolder holder = new HttpRequestResponseHolder(request, response);

        SecurityContext context = repository.loadContext(holder);

        assertNotNull(context);
    }

    // ==================== saveContext tests ====================

    @Test
    public void testSaveContextWithBearerToken() {
        request.addHeader("Authorization", "Bearer token123");
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        // Should save to request attributes, not session
        repository.saveContext(context, request, response);

        // Verify context can be retrieved
        assertTrue(repository.containsContext(request));
    }

    @Test
    public void testSaveContextWithoutBearerToken() {
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        // Should save to session
        repository.saveContext(context, request, response);

        // Context should be saved (in session)
        assertTrue(repository.containsContext(request));
    }

    @Test
    public void testSaveContextWithBasicAuth() {
        request.addHeader("Authorization", "Basic dXNlcm5hbWU6cGFzc3dvcmQ=");
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        // Should save to session (Basic auth is not Bearer)
        repository.saveContext(context, request, response);

        assertTrue(repository.containsContext(request));
    }

    // ==================== containsContext tests ====================

    @Test
    public void testContainsContextWithBearerTokenNoContext() {
        request.addHeader("Authorization", "Bearer token123");

        // No context saved yet
        assertFalse(repository.containsContext(request));
    }

    @Test
    public void testContainsContextWithBearerTokenAfterSave() {
        request.addHeader("Authorization", "Bearer token123");
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        repository.saveContext(context, request, response);

        assertTrue(repository.containsContext(request));
    }

    @Test
    public void testContainsContextWithoutBearerTokenNoContext() {
        // No Authorization header, no session
        assertFalse(repository.containsContext(request));
    }

    @Test
    public void testContainsContextWithoutBearerTokenAfterSave() {
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        repository.saveContext(context, request, response);

        assertTrue(repository.containsContext(request));
    }

    // ==================== Integration/Behavior tests ====================

    @Test
    public void testBearerRequestStaysStateless() {
        request.addHeader("Authorization", "Bearer token123");
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("bearerUser", "token"));

        repository.saveContext(context, request, response);

        // Verify the context is stored in request attributes
        assertTrue(repository.containsContext(request));

        // Create a new request (simulating a new HTTP request) with same session
        MockHttpServletRequest newRequest = new MockHttpServletRequest();
        newRequest.setSession(request.getSession(false));
        newRequest.addHeader("Authorization", "Bearer token123");

        // The new request should NOT have the context (it was request-scoped)
        assertFalse(repository.containsContext(newRequest));
    }

    @Test
    public void testNonBearerRequestUsesSession() {
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("sessionUser", "password"));

        repository.saveContext(context, request, response);

        // Verify the context is stored
        assertTrue(repository.containsContext(request));

        // Create a new request with the same session
        MockHttpServletRequest newRequest = new MockHttpServletRequest();
        newRequest.setSession(request.getSession(false));

        // The new request SHOULD have the context (it was session-scoped)
        assertTrue(repository.containsContext(newRequest));
    }

    @Test
    public void testMixedRequestsAreSeparated() {
        // First, a session-based request
        SecurityContext sessionContext = new SecurityContextImpl();
        sessionContext.setAuthentication(new UsernamePasswordAuthenticationToken("sessionUser", "password"));
        repository.saveContext(sessionContext, request, response);

        // Then, a bearer request (should not affect session)
        MockHttpServletRequest bearerRequest = new MockHttpServletRequest();
        bearerRequest.addHeader("Authorization", "Bearer token123");
        bearerRequest.setSession(request.getSession(false));

        SecurityContext bearerContext = new SecurityContextImpl();
        bearerContext.setAuthentication(new UsernamePasswordAuthenticationToken("bearerUser", "token"));
        repository.saveContext(bearerContext, bearerRequest, response);

        // Session context should still be accessible on non-bearer requests
        MockHttpServletRequest anotherSessionRequest = new MockHttpServletRequest();
        anotherSessionRequest.setSession(request.getSession(false));

        DeferredSecurityContext loaded = repository.loadDeferredContext(anotherSessionRequest);
        SecurityContext loadedContext = loaded.get();
        assertNotNull(loadedContext.getAuthentication());
        assertEquals("sessionUser", loadedContext.getAuthentication().getName());
    }
}
