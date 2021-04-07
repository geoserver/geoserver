/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import java.io.IOException;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.auth.web.WebAuthenticationConfig;
import org.geoserver.security.auth.web.WebServiceAuthenticationProvider;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextListener;

public class WebAuthProviderTest extends AbstractAuthenticationProviderTest {
    public static final String testFilterName = "basicAuthTestFilter";

    private static WireMockServer externalHTTPService;
    private static final String VALID_RESPONSE = "\"roles\":\"WEB_SERVICE_ROLE\"";
    private static final String NO_AUTH_RESPONSE = "\"authenticated\":\"false\"";

    private static final String AUTH_REQUEST_PATH = "/auth";
    private static final String TEST_USERNAME_ENC = encode(testUserName);
    private static final String TEST_PWD_ENC = encode(testPassword);
    private static final String TEST_HEADER_ENC = encode(testUserName + ":" + testPassword);

    private static final String TEST_NO_AUTH_USERNAME_ENC = encode("unauthUser");
    private static final String TEST_NO_AUTH_PWD_ENC = encode("unauthPwd");
    private static final String TEST_NO_AUTH_HEADER_ENC = encode("unauthUser" + ":" + "unauthPwd");

    private static String authService;

    @BeforeClass
    public static void beforeClass() throws Exception {
        externalHTTPService =
                new WireMockServer(
                        wireMockConfig()
                                .dynamicPort()
                                // uncomment the following to get wiremock logging
                                .notifier(new ConsoleNotifier(true)));
        externalHTTPService.start();
        authService = "http://localhost:" + externalHTTPService.port();
        externalHTTPService.stubFor(
                WireMock.get(urlPathEqualTo(AUTH_REQUEST_PATH))
                        .withHeader(
                                "X-HTTP-AUTHORIZATION",
                                equalTo(encode(testUserName + ":" + testPassword)))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)
                                        .withBody(VALID_RESPONSE)));
        externalHTTPService.stubFor(
                WireMock.get(urlPathEqualTo(AUTH_REQUEST_PATH))
                        .withQueryParam("user", equalTo(TEST_USERNAME_ENC))
                        .withQueryParam("password", equalTo(TEST_PWD_ENC))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)
                                        .withBody(VALID_RESPONSE)));
        externalHTTPService.stubFor(
                WireMock.get(urlPathEqualTo(AUTH_REQUEST_PATH))
                        .withQueryParam("user", equalTo(TEST_NO_AUTH_USERNAME_ENC))
                        .withQueryParam("password", equalTo(TEST_NO_AUTH_PWD_ENC))
                        .willReturn(
                                aResponse()
                                        .withStatus(401)
                                        .withHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)
                                        .withBody(NO_AUTH_RESPONSE)));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // configure the basic filter username password filter
        BasicAuthenticationFilterConfig config = new BasicAuthenticationFilterConfig();
        config.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        config.setUseRememberMe(false);
        config.setName(testFilterName);
        getSecurityManager().saveFilter(config);
    }

    @Test
    public void testWebAuthWithRoleInResponse() throws Exception {

        WebAuthenticationConfig config = new WebAuthenticationConfig();
        config.setClassName(WebServiceAuthenticationProvider.class.getName());
        config.setName("webAuthProvider");
        config.setConnectionURL(
                authService + AUTH_REQUEST_PATH + "?user={user}&password={password}");
        config.setRoleRegex("^.*?\"roles\"\\s*:\\s*\"([^\"]+)\".*$");
        config.setAllowHTTPConnection(true);

        config.setAuthorizationOption(WebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_WEB);

        getSecurityManager().saveAuthenticationProvider(config);

        prepareFilterChain(pattern, testFilterName);
        prepareAuthProviders("webAuthProvider");

        MockHttpServletRequest request = createMockRequest();

        MockHttpServletResponse response = executeOnSecurityFilters(request);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        Authentication auth = getAuth(testFilterName, testUserName, null, null);

        // role from default service
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        // role from Auth provider
        assertTrue(auth.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE));

        // role from Web Response
        assertTrue(auth.getAuthorities().contains(new GeoServerRole("ROLE_WEB_SERVICE_ROLE")));
    }

    @Test
    public void testWebAuthWithCredentialsInHeader() throws Exception {
        String providerName = "webAuthProviderUseHeader";
        WebAuthenticationConfig config = new WebAuthenticationConfig();
        config.setClassName(WebServiceAuthenticationProvider.class.getName());
        config.setName(providerName);
        config.setConnectionURL(authService + AUTH_REQUEST_PATH);
        config.setRoleRegex("^.*?\"roles\"\\s*:\\s*\"([^\"]+)\".*$");
        config.setAuthorizationOption(WebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_WEB);
        config.setUseHeader(true);
        config.setAllowHTTPConnection(true);

        getSecurityManager().saveAuthenticationProvider(config);

        prepareFilterChain(pattern, testFilterName);
        prepareAuthProviders(providerName);

        MockHttpServletRequest request = createMockRequest();

        MockHttpServletResponse response = executeOnSecurityFilters(request);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        Authentication auth = getAuth(testFilterName, testUserName, null, null);

        // role from default service
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        // role from Auth provider
        assertTrue(auth.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE));

        // role from Web Response
        assertTrue(auth.getAuthorities().contains(new GeoServerRole("ROLE_WEB_SERVICE_ROLE")));
    }

    @Test
    public void testWebAuthWithRoleService() throws Exception {

        String providerName = "webAuthProviderWithRoleService";
        WebAuthenticationConfig config = new WebAuthenticationConfig();
        config.setClassName(WebServiceAuthenticationProvider.class.getName());
        config.setName(providerName);
        config.setConnectionURL(
                authService + AUTH_REQUEST_PATH + "?user={user}&password={password}");
        config.setAuthorizationOption(WebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_SERVICE);
        config.setRoleServiceName(getSecurityManager().getActiveRoleService().getName());
        config.setAllowHTTPConnection(true);
        getSecurityManager().saveAuthenticationProvider(config);

        prepareFilterChain(pattern, testFilterName);
        prepareAuthProviders(providerName);

        MockHttpServletRequest request = createMockRequest();

        MockHttpServletResponse response = executeOnSecurityFilters(request);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        Authentication auth = getAuth(testFilterName, testUserName, null, null);

        // role from selected service
        assertTrue(auth.getAuthorities().contains(new GeoServerRole(derivedRole)));
        // role from Auth provider
        assertTrue(auth.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE));
    }

    @Test
    public void testWebAuthFails() throws Exception {
        String providerName = "NoAuthProvider";
        WebAuthenticationConfig config = new WebAuthenticationConfig();
        config.setClassName(WebServiceAuthenticationProvider.class.getName());
        config.setName(providerName);
        config.setConnectionURL(
                authService + AUTH_REQUEST_PATH + "?user={user}&password={password}");
        config.setAuthorizationOption(WebAuthenticationConfig.AUTHORIZATION_RADIO_OPTION_SERVICE);
        config.setRoleServiceName(getSecurityManager().getActiveRoleService().getName());
        config.setAllowHTTPConnection(true);

        getSecurityManager().saveAuthenticationProvider(config);

        prepareFilterChain(pattern, testFilterName);
        prepareAuthProviders(providerName);

        MockHttpServletRequest request = createMockRequest(TEST_NO_AUTH_HEADER_ENC);

        MockHttpServletResponse response = executeOnSecurityFilters(request);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    Authentication getAuth(String filterName, String user, Integer idleTime, Integer liveTime) {

        Map<String, byte[]> map = getCache().cache.get(filterName);
        if (map == null) return null;
        Authentication result = null;
        String cacheKey = null;
        for (Entry<String, byte[]> entry : map.entrySet()) {
            Authentication auth = getCache().deserializeAuthentication(entry.getValue());
            Object o = auth.getPrincipal();

            if (o instanceof UserDetails) {
                if (user.equals(((UserDetails) o).getUsername())) {
                    result = auth;
                    cacheKey = entry.getKey();
                    break;
                }
            }
            if (o instanceof Principal) {
                if (user.equals(((Principal) o).getName())) {
                    result = auth;
                    cacheKey = entry.getKey();
                    break;
                }
            }
            if (o instanceof String) {
                if (user.equals(((String) o))) {
                    result = auth;
                    cacheKey = entry.getKey();
                    break;
                }
            }
        }

        if (result != null) {
            Integer[] seconds = getCache().getExpireTimes(filterName, cacheKey);
            if (idleTime == null)
                assertEquals(TestingAuthenticationCache.DEFAULT_IDLE_SECS, seconds[0]);
            else assertEquals(idleTime, seconds[0]);

            if (liveTime == null)
                assertEquals(TestingAuthenticationCache.DEFAULT_LIVE_SECS, seconds[1]);
            else assertEquals(liveTime, seconds[1]);
        }

        return result;
    }

    private static String encode(String credentials) throws RuntimeException {
        return new String(Base64.getEncoder().encode(credentials.getBytes()));
    }

    private MockHttpServletResponse executeOnSecurityFilters(MockHttpServletRequest request)
            throws IOException, javax.servlet.ServletException {
        // for session local support in Spring
        new RequestContextListener()
                .requestInitialized(new ServletRequestEvent(request.getServletContext(), request));

        // run on the
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        GeoServerSecurityFilterChainProxy filterChainProxy =
                GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
        filterChainProxy.doFilter(request, response, chain);

        return response;
    }

    private MockHttpServletRequest createMockRequest() {
        return createMockRequest(TEST_HEADER_ENC);
    }

    private MockHttpServletRequest createMockRequest(String authHeader) {
        MockHttpServletRequest request = createRequest("/foo/bar");
        request.setMethod(RequestMethod.GET.toString());
        request.addHeader("Authorization", "Basic " + authHeader);
        return request;
    }
}
