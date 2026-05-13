/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.UserProfilePropertyNames;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

/** Unit tests for OIDC principal-construction logic in {@link OpenIdConnectAuthenticationFilter}. */
public class OpenIdConnectAuthenticationFilterTest {

    @Test
    /**
     * Verifies that principal construction remains successful when user/group service access fails, and profile
     * properties are still populated from OIDC claims.
     */
    public void testBuildPrincipalToleratesUserGroupServiceFailures() throws Exception {
        OpenIdConnectFilterConfig config = new OpenIdConnectFilterConfig();
        config.setUserGroupServiceName("default");

        OAuth2RestOperations rest = mock(OAuth2RestOperations.class);
        OAuth2ClientContext context = mock(OAuth2ClientContext.class);
        when(rest.getOAuth2ClientContext()).thenReturn(context);

        TestOpenIdConnectFilter filter = new TestOpenIdConnectFilter(config, mock(RemoteTokenServices.class), rest);
        GeoServerSecurityManager securityManager = mock(GeoServerSecurityManager.class);
        when(securityManager.loadUserGroupService("default")).thenThrow(new RuntimeException("test error"));
        filter.setSecurityManager(securityManager);

        MockHttpServletRequest request = new MockHttpServletRequest();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("given_name", "Andrea");
        userInfo.put("family_name", "Aime");
        userInfo.put("email", "andrea.aime@gmail.com");
        request.setAttribute(GeoServerOAuthAuthenticationFilter.OAUTH2_ACCESS_TOKEN_CHECK_KEY, userInfo);

        Object principal =
                filter.callBuild("andrea.aime@gmail.com", Arrays.asList(GeoServerRole.AUTHENTICATED_ROLE), request);
        assertTrue(principal instanceof GeoServerUser);
        GeoServerUser user = (GeoServerUser) principal;
        assertEquals("andrea.aime@gmail.com", user.getUsername());
        assertEquals("Andrea", user.getProperties().getProperty(UserProfilePropertyNames.FIRST_NAME));
        assertEquals("Aime", user.getProperties().getProperty(UserProfilePropertyNames.LAST_NAME));
        assertEquals("andrea.aime@gmail.com", user.getProperties().getProperty(UserProfilePropertyNames.EMAIL));
    }

    static class TestOpenIdConnectFilter extends OpenIdConnectAuthenticationFilter {
        TestOpenIdConnectFilter(
                OpenIdConnectFilterConfig config, RemoteTokenServices tokenServices, OAuth2RestOperations rest) {
            super(config, tokenServices, null, rest, null);
        }

        Object callBuild(String principal, java.util.Collection<GeoServerRole> roles, MockHttpServletRequest request) {
            return buildAuthenticatedPrincipal(principal, roles, request);
        }
    }
}
