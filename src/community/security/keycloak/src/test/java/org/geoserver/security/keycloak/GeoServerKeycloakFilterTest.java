/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Tests for {@link GeoServerKeycloakFilter}. Focus is on the 4 possible valid responses handled in
 * {@link GeoServerKeycloakFilter#getNewAuthn(HttpServletRequest, HttpServletResponse)}.
 */
public class GeoServerKeycloakFilterTest {

    // name shortening
    public static final String AEP_HEADER =
            GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER;

    // identifiers for the auth context
    public static final String REALM = "master";
    public static final String CLIENT_ID = "nginx-authn";

    // locations for useful resources
    public static final String APP_URL = "http://localhost:8080/app";
    public static final String AUTH_URL = "https://cas.core.maui.mda.ca:8040/auth";
    public static final String OPENID_URL = AUTH_URL + "/realms/" + REALM;

    // some pre-generated data from keycloak that should work until the year 2037
    public static final String PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzkRIC4ow7QqXed+4WICpF5gU2AqXrKT2lPBZOyG6NETv7X"
                    + "g2FmlGA5KIPxcweexgJCcRY1oFEpulBhVo8zc7WVKX1gc8myXvqvdOMHTUMZ0C4l8Q8ls4fE8B4FiALv/48u"
                    + "T1YWXKKvsaBPSeh3QTINwtYsAxIrqTjW5wJVaH8L+EazeKep+JSKPvworT9Q8K4u0XURI9MZi983LEx4Wufc"
                    + "iTPqhD8v6h7Yr+Iy6H/vHHBulwIHZ4MnQBod1aiKuOhM8bsD+FPBVcKCanATVhz6pZoaZXv7j2ZnVSvh6iGi"
                    + "qP80DknLOyY3IqVST9w8KP1UG0upQ+Zsk8ohCg4Qlm6QIDAQAB";
    public static final String JWT_2018_2037 =
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJqS2RPZS0zNmhrLVI2R1puQk5tb2JfTFdtMUZJQU"
                    + "tWVXlKblEzTnNuU21RIn0.eyJqdGkiOiIzNTc5MDQ5MS0yNzI5LTRiNTAtOGIwOC1kYzNhYTM1NDE0ZjgiLC"
                    + "JleHAiOjIxMjE4MTY5OTYsIm5iZiI6MCwiaWF0IjoxNTE3MDE2OTk2LCJpc3MiOiJodHRwczovL2Nhcy5jb3"
                    + "JlLm1hdWkubWRhLmNhOjgwNDAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoibmdpbngtYXV0aG4iLCJzdW"
                    + "IiOiIxMDM3NzU0OC04OTZhLTQwODUtODY2OC0zNmM4OWQzYzU0OTMiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOi"
                    + "JuZ2lueC1hdXRobiIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6IjY5MWQwOTZiLTkzNjctNDdlZi"
                    + "04OGEyLTQ1ZjIwZGI4ZjMxNCIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3"
                    + "MiOnsicm9sZXMiOlsiY3JlYXRlLXJlYWxtIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3"
                    + "VyY2VfYWNjZXNzIjp7Im1hc3Rlci1yZWFsbSI6eyJyb2xlcyI6WyJ2aWV3LWlkZW50aXR5LXByb3ZpZGVycy"
                    + "IsInZpZXctcmVhbG0iLCJtYW5hZ2UtaWRlbnRpdHktcHJvdmlkZXJzIiwiaW1wZXJzb25hdGlvbiIsImNyZW"
                    + "F0ZS1jbGllbnQiLCJtYW5hZ2UtdXNlcnMiLCJxdWVyeS1yZWFsbXMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLC"
                    + "JxdWVyeS1jbGllbnRzIiwicXVlcnktdXNlcnMiLCJtYW5hZ2UtZXZlbnRzIiwibWFuYWdlLXJlYWxtIiwidm"
                    + "lldy1ldmVudHMiLCJ2aWV3LXVzZXJzIiwidmlldy1jbGllbnRzIiwibWFuYWdlLWF1dGhvcml6YXRpb24iLC"
                    + "JtYW5hZ2UtY2xpZW50cyIsInF1ZXJ5LWdyb3VwcyJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYW"
                    + "Njb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwicHJlZmVycmVkX3VzZX"
                    + "JuYW1lIjoiYWRtaW4ifQ.deouu-Gqb1MNmfMYARKtkIaM4ztP2tDowG_X0yRxPPSefhQd0rUjLgUl_FS9yiM"
                    + "wJoZBCIYBEvgqBlQW1836SfDTiPXSUlhQRQElJwoXWCS1UaO8neVa-vt8uGo2vBBsOv8pGVM1dsunA3-BMF7"
                    + "P-MX9y0ZmMp4T5VOe4iK3K_uP1teTDyGg455WlL18CsVxKKSvOIrd2xF4M2qNny2fgU7Ca1s-7Jo555VB7fs"
                    + "Uu4nLYvoELb0f_4U4H3Yui_J4m2FplsGoqY7RgM_yTBZ9ZvS-W7ddEjpjyM_D1aFaSByzMYVA6yvnqWIsAVZ"
                    + "e4sZnjoVZM0sMCQtXtNQaUk7Rbg";

    // common test inputs
    private GeoServerKeycloakFilterConfig config;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    // do setup before each test
    @Before
    public void before() throws IOException {
        AdapterConfig aConfig = new AdapterConfig();
        aConfig.setRealm(REALM);
        aConfig.setResource(CLIENT_ID);
        aConfig.setAuthServerUrl(AUTH_URL);
        config = new GeoServerKeycloakFilterConfig();
        config.writeAdapterConfig(aConfig);
        request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer(APP_URL));
        when(request.getHeaders(anyString())).thenReturn(Collections.emptyEnumeration());
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    // remove any possible side-effects to avoid interfering with the next test
    @After
    public void after() {
        SecurityContextHolder.getContext().setAuthentication(null);
        config = null;
        request = null;
        response = null;
        chain = null;
    }

    // AuthOutcome.NOT_ATTEMPTED
    @Test
    public void testNoAuth() throws Exception {
        // set up the object under test
        GeoServerKeycloakFilter filter = new GeoServerKeycloakFilter();
        filter.initializeFromConfig(config);

        // set up the test inputs
        when(response.getStatus()).thenReturn(HttpStatus.MOVED_PERMANENTLY.value());

        // run the test
        filter.doFilter(request, response, chain);

        // simulate execution of the AEP
        ArgumentCaptor<AuthenticationEntryPoint> aep =
                ArgumentCaptor.forClass(AuthenticationEntryPoint.class);
        verify(request).setAttribute(eq(AEP_HEADER), aep.capture());
        aep.getValue().commence(request, response, null);

        // check the results
        verify(chain).doFilter(request, response);
        ArgumentCaptor<Integer> status = ArgumentCaptor.forClass(Integer.class);
        verify(response).setStatus(status.capture());
        assertTrue(HttpStatus.valueOf(status.getValue()).is3xxRedirection());
        verify(response)
                .setHeader(
                        eq(HttpHeaders.LOCATION), and(startsWith(OPENID_URL), contains(CLIENT_ID)));
    }

    // AuthOutcome.NOT_ATTEMPTED + bearer-only
    @Test
    public void testNoAuthBearerOnly() throws Exception {
        // set up the object under test
        AdapterConfig aConfig = config.readAdapterConfig();
        aConfig.setBearerOnly(true);
        config.writeAdapterConfig(aConfig);
        GeoServerKeycloakFilter filter = new GeoServerKeycloakFilter();
        filter.initializeFromConfig(config);

        // set up the test inputs
        when(response.getStatus()).thenReturn(HttpStatus.FORBIDDEN.value());

        // run the test
        filter.doFilter(request, response, chain);

        // simulate execution of the AEP
        ArgumentCaptor<AuthenticationEntryPoint> aep =
                ArgumentCaptor.forClass(AuthenticationEntryPoint.class);
        verify(request).setAttribute(eq(AEP_HEADER), aep.capture());
        aep.getValue().commence(request, response, null);

        // verify the results
        verify(chain).doFilter(request, response);
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        Authentication authn = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authn);
    }

    // AuthOutcome.FAILED
    @Test
    public void testBadAuth() throws Exception {
        // set up the object under test
        GeoServerKeycloakFilter filter = new GeoServerKeycloakFilter();
        filter.initializeFromConfig(config);

        // set up the test inputs
        String auth_header = "bearer this.is.not.a.valid.token";
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(auth_header);
        when(request.getHeaders(HttpHeaders.AUTHORIZATION))
                .thenReturn(Collections.enumeration(Collections.singleton(auth_header)));
        when(response.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED.value());

        // run the test
        filter.doFilter(request, response, chain);

        // simulate execution of the AEP
        ArgumentCaptor<AuthenticationEntryPoint> aep =
                ArgumentCaptor.forClass(AuthenticationEntryPoint.class);
        verify(request).setAttribute(eq(AEP_HEADER), aep.capture());
        aep.getValue().commence(request, response, null);

        // verify the results
        verify(chain).doFilter(request, response);
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        Authentication authn = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authn);
    }

    // AuthOutcome.AUTHENTICATED
    @Test
    public void testGoodAuth() throws Exception {
        // set up the object under test
        AdapterConfig aConfig = config.readAdapterConfig();
        aConfig.setRealmKey(PUBLIC_KEY);
        config.writeAdapterConfig(aConfig);
        GeoServerKeycloakFilter filter = new GeoServerKeycloakFilter();
        filter.initializeFromConfig(config);

        // set up the test inputs
        String auth_header = "bearer " + JWT_2018_2037;
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(auth_header);
        when(request.getHeaders(HttpHeaders.AUTHORIZATION))
                .thenReturn(Collections.enumeration(Collections.singleton(auth_header)));
        when(response.getStatus()).thenReturn(HttpStatus.OK.value());

        // run the test
        filter.doFilter(request, response, chain);

        // verify that we successfully authenticated
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
        verify(response, never()).setHeader(anyString(), anyString());
        verify(response, never()).addCookie(any());
        Authentication authn = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authn);
        assertTrue(authn instanceof UsernamePasswordAuthenticationToken);
        assertFalse(authn.getAuthorities().isEmpty());
        for (GrantedAuthority a : authn.getAuthorities()) {
            assertTrue(a.getAuthority().startsWith("ROLE_"));
        }
    }
}
