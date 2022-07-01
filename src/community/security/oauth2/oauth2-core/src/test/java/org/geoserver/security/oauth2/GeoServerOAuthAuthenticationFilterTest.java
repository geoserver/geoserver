package org.geoserver.security.oauth2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.servlet.DispatcherServlet;

public class GeoServerOAuthAuthenticationFilterTest {

    @Test
    public void testAnonymousAuthenticationIsNotCreate() {
        GeoServerOAuthAuthenticationFilter filter =
                new GeoServerOAuthAuthenticationFilter(null, null, null, null) {
                    @Override
                    protected String getPreAuthenticatedPrincipal(
                            HttpServletRequest request, HttpServletResponse response) {
                        return null;
                    }
                };
        filter.doAuthenticate(new MockHttpServletRequest(), new MockHttpServletResponse());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testAvoidSessionClearingIfNoAccessTokenIsPresent()
            throws IOException, ServletException {
        final boolean[] didLogout = {false};
        MockHttpServletRequest httpServletRequest =
                new MockHttpServletRequest() {
                    @Override
                    public void logout() throws ServletException {
                        super.logout();
                        didLogout[0] = true;
                    }
                };
        httpServletRequest.setSession(new MockHttpSession());
        HttpServletResponse httpServletResponse = new MockHttpServletResponse();
        GeoServerOAuth2SecurityConfiguration config =
                Mockito.mock(GeoServerOAuth2SecurityConfiguration.class);
        GeoServerOAuth2FilterConfig filterConfig = new GeoServerOAuth2FilterConfig();
        filterConfig.setName("openidconnect");
        filterConfig.setClassName(GeoServerOAuthAuthenticationFilter.class.getName());
        filterConfig.setCliendId("efhrufuu3uhu3uhu");
        filterConfig.setClientSecret("48ru48tj58fr8jf");
        filterConfig.setAccessTokenUri("localhost:8080" + "/token");
        filterConfig.setUserAuthorizationUri("localhost:8080" + "/authorize");
        filterConfig.setCheckTokenEndpointUrl("localhost:8080" + "/userinfo");
        filterConfig.setLoginEndpoint("/j_spring_oauth2_openid_connect_login");
        filterConfig.setLogoutEndpoint("/j_spring_oauth2_openid_connect_logout");
        filterConfig.setScopes("openid profile email phone address");
        filterConfig.setEnableRedirectAuthenticationEntryPoint(true);
        OAuth2RestTemplate oAuth2RestTemplate = Mockito.mock(OAuth2RestTemplate.class);
        Mockito.when(oAuth2RestTemplate.getAccessToken()).thenReturn(null);
        Mockito.when(oAuth2RestTemplate.getOAuth2ClientContext())
                .thenReturn(new DefaultOAuth2ClientContext());
        GeoServerOAuthRemoteTokenServices services =
                Mockito.mock(GeoServerOAuthRemoteTokenServices.class);
        GeoServerOAuthAuthenticationFilter filter =
                new GeoServerOAuthAuthenticationFilter(
                        filterConfig, services, config, oAuth2RestTemplate) {
                    @Override
                    protected String getPreAuthenticatedPrincipal(
                            HttpServletRequest request, HttpServletResponse response) {
                        return null;
                    }
                };
        filter.doAuthenticate(httpServletRequest, httpServletResponse);
        MockFilterChain filterChain = new MockFilterChain(new DispatcherServlet(), filter);
        filterChain.doFilter(httpServletRequest, httpServletResponse);
        assertFalse(((MockHttpSession) httpServletRequest.getSession(false)).isInvalid());
        assertFalse(didLogout[0]);
    }
}
