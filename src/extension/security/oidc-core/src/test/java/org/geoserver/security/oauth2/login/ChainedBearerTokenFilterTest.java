/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class ChainedBearerTokenFilterTest {

    private static final String TOKEN_OF_FIRST = "token-of-first";
    private static final String TOKEN_OF_SECOND = "token-of-second";

    /** The chains of the security configuration, as seen by the filters under test. */
    private final List<SecurityFilterChain> securityChains = new ArrayList<>();

    private final GeoServerOAuth2LoginAuthenticationFilter first = oidcFilterAccepting(TOKEN_OF_FIRST, "ROLE_FIRST");
    private final GeoServerOAuth2LoginAuthenticationFilter second = oidcFilterAccepting(TOKEN_OF_SECOND, "ROLE_SECOND");
    private final GeoServerOAuth2LoginAuthenticationFilter withoutBearerTokens = oidcFilter();

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    /** The authentication of the request when it reached the end of the chain, null if it did not. */
    private Authentication served;

    private boolean reachedTheEndOfTheChain;

    @After
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void singleFilterAcceptsItsToken() throws Exception {
        requestThrough(TOKEN_OF_FIRST, first);

        assertRoles("ROLE_FIRST");
    }

    @Test
    public void singleFilterRejectsOtherTokens() throws Exception {
        requestThrough(TOKEN_OF_SECOND, first);

        assertRejected();
    }

    @Test
    public void tokenOfLaterFilterIsAccepted() throws Exception {
        requestThrough(TOKEN_OF_SECOND, first, second);

        assertRoles("ROLE_SECOND");
    }

    // the second filter cannot validate the token and must not reject the request, nor add its roles
    @Test
    public void tokenOfEarlierFilterIsAccepted() throws Exception {
        requestThrough(TOKEN_OF_FIRST, first, second);

        assertRoles("ROLE_FIRST");
    }

    @Test
    public void lastFilterRejectsUnknownToken() throws Exception {
        requestThrough("token-of-nobody", first, second);

        assertRejected();
    }

    @Test
    public void laterFilterWithoutBearerTokens() throws Exception {
        requestThrough(TOKEN_OF_SECOND, first, withoutBearerTokens);

        assertRejected();
    }

    // the second filter validates Bearer tokens, in a chain that does not serve this request
    @Test
    public void laterFilterOfAnotherChain() throws Exception {
        RequestMatcher restRequests = request -> request.getRequestURI().startsWith("/geoserver/rest");
        securityChains.add(new DefaultSecurityFilterChain(restRequests, first, second));

        requestThrough(TOKEN_OF_SECOND, first);

        assertRejected();
    }

    @Test
    public void requestWithoutTokenPassesOn() throws Exception {
        requestThrough(null, first, second);

        assertTrue(reachedTheEndOfTheChain);
        assertNull(served);
    }

    /** Sends a request with the given Bearer token through a chain made of the given filters. */
    private void requestThrough(String token, Filter... chainFilters) throws Exception {
        securityChains.add(new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE, chainFilters));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/geoserver/wms");
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        MockFilterChain chain = new MockFilterChain(endOfTheChain(), chainFilters);
        chain.doFilter(request, response);
    }

    private HttpServlet endOfTheChain() {
        return new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) {
                reachedTheEndOfTheChain = true;
                served = SecurityContextHolder.getContext().getAuthentication();
            }
        };
    }

    private void assertRoles(String... expected) {
        assertTrue("the request did not reach the end of the chain", reachedTheEndOfTheChain);
        List<String> roles = served.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> !authority.startsWith("FACTOR_"))
                .toList();
        assertEquals(List.of(expected), roles);
    }

    private void assertRejected() {
        assertEquals(401, response.getStatus());
        assertTrue(response.getHeader("WWW-Authenticate").startsWith("Bearer"));
        assertFalse(reachedTheEndOfTheChain);
    }

    /** An OIDC filter in resource server mode, whose identity provider issued the given token only. */
    private GeoServerOAuth2LoginAuthenticationFilter oidcFilterAccepting(String token, String role) {
        AuthenticationManager provider = authentication -> authenticate(authentication, token, role);
        BearerTokenAuthenticationFilter springFilter = new BearerTokenAuthenticationFilter(provider);
        // Spring names each filter it builds. Unnamed, the second filter of a chain would consider the request
        // already filtered by the first one, and the test would not exercise it.
        springFilter.setBeanName("bearerTokenFilter-" + role);
        return oidcFilter(new ChainedBearerTokenFilter(springFilter, () -> securityChains));
    }

    private static GeoServerOAuth2LoginAuthenticationFilter oidcFilter(Filter... nestedFilters) {
        GeoServerOAuth2LoginAuthenticationFilter filter = new GeoServerOAuth2LoginAuthenticationFilter();
        filter.setNestedFilters(List.of(nestedFilters));
        return filter;
    }

    private static Authentication authenticate(Authentication request, String acceptedToken, String role) {
        String token = ((BearerTokenAuthenticationToken) request).getToken();
        if (!acceptedToken.equals(token)) {
            throw new InvalidBearerTokenException("no matching key found");
        }
        Jwt jwt =
                Jwt.withTokenValue(token).header("alg", "RS256").subject("user").build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role)));
    }
}
