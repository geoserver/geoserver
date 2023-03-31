/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.ServletRequestEvent;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.VariableFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;

public class OpenIdConnectIntegrationTest extends GeoServerSystemTestSupport {

    private static final String CLIENT_ID = "kbyuFDidLLm280LIwVFiazOqjO3ty8KH";
    private static final String CLIENT_SECRET =
            "60Op4HFM0I8ajz0WdiStAbziZ-VFQttXuxixHHs2R7r7-CW8GR79l-mmLqMhc-Sa";
    private static final String CODE = "R-2CqM7H1agwc7Cx";
    private static WireMockServer openIdService;
    private static String authService;

    @BeforeClass
    public static void beforeClass() throws Exception {
        openIdService =
                new WireMockServer(
                        wireMockConfig()
                                .dynamicPort()
                                // uncomment the following to get wiremock logging
                                .notifier(new ConsoleNotifier(true)));
        openIdService.start();

        openIdService.stubFor(
                WireMock.get(urlEqualTo(".well-known/jwks.json"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("jkws.json")));

        openIdService.stubFor(
                WireMock.post(urlPathEqualTo("/token"))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("client_id=" + CLIENT_ID))
                        //                        .withQueryParam("client_secret",
                        // equalTo(CLIENT_SECRET))
                        .withRequestBody(containing("code=" + CODE))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("token_response.json")));
        openIdService.stubFor(
                WireMock.post(urlPathEqualTo("/token"))
                        .withRequestBody(containing("grant_type=authorization_code"))
                        .withRequestBody(containing("client_id=" + CLIENT_ID))
                        .withRequestBody(containing("client_secret=" + CLIENT_SECRET))
                        .withRequestBody(containing("code=" + CODE))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("token_response_with_secret.json")));
        openIdService.stubFor(
                WireMock.get(WireMock.urlMatching(".*/userinfo")) // disallow query parameters
                        /*.withHeader(
                        "Authorization", equalTo("Bearer CPURR33RUz-gGhjwODTd9zXo5JkQx4wS"))*/
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("userinfo.json")));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        openIdService.shutdown();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // prepare mock server base path
        authService = "http://localhost:" + openIdService.port();

        // setup openid
        GeoServerSecurityManager manager = getSecurityManager();
        OpenIdConnectFilterConfig filterConfig = new OpenIdConnectFilterConfig();
        filterConfig.setName("openidconnect");
        filterConfig.setClassName(OpenIdConnectAuthenticationFilter.class.getName());
        filterConfig.setCliendId(CLIENT_ID);
        filterConfig.setClientSecret(CLIENT_SECRET);
        filterConfig.setAccessTokenUri(authService + "/token");
        filterConfig.setUserAuthorizationUri(authService + "/authorize");
        filterConfig.setCheckTokenEndpointUrl(authService + "/userinfo");
        filterConfig.setLoginEndpoint("/j_spring_oauth2_openid_connect_login");
        filterConfig.setLogoutEndpoint("/j_spring_oauth2_openid_connect_logout");
        filterConfig.setLogoutUri(authService + "/endSession");
        filterConfig.setScopes("openid profile email phone address");
        filterConfig.setEnableRedirectAuthenticationEntryPoint(true);
        filterConfig.setPrincipalKey("email");
        filterConfig.setRoleSource(OpenIdConnectFilterConfig.OpenIdRoleSource.IdToken);
        filterConfig.setTokenRolesClaim("roles");
        // for ease of testing, do not use HTTPS
        filterConfig.setForceUserAuthorizationUriHttps(false);
        filterConfig.setForceAccessTokenUriHttps(false);
        manager.saveFilter(filterConfig);

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");
        www.setFilterNames("openidconnect", "anonymous");
        manager.saveSecurityConfig(config);
    }

    /**
     * Enable the Spring Security authentication filters, we want the test to be complete and
     * realistic
     */
    @Override
    protected List<Filter> getFilters() {

        SecurityManagerConfig mconfig = getSecurityManager().getSecurityConfig();
        GeoServerSecurityFilterChain filterChain = mconfig.getFilterChain();
        VariableFilterChain chain = (VariableFilterChain) filterChain.getRequestChainByName("web");
        List<Filter> result = new ArrayList<>();
        for (String filterName : chain.getCompiledFilterNames()) {
            try {
                result.add(getSecurityManager().loadFilter(filterName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Test
    public void testRoleExtraction() throws Exception {
        // mimick user pressing on login button
        MockHttpServletRequest webRequest = createRequest("web/");
        MockHttpServletResponse webResponse = executeOnSecurityFilters(webRequest);

        // should have got a redirect to the authentication page of the auth server
        assertEquals(302, webResponse.getStatus());
        String location = webResponse.getHeader("Location");
        assertNotNull(location);
        assertThat(location, CoreMatchers.startsWith(authService));
        Map<String, Object> kvp = KvpUtils.parseQueryString(location);
        assertThat(kvp, Matchers.hasEntry("client_id", CLIENT_ID));
        assertThat(kvp, Matchers.hasEntry("redirect_uri", "http://localhost/geoserver"));
        assertThat(kvp, Matchers.hasEntry("scope", "openid profile email phone address"));
        assertThat(kvp, Matchers.hasEntry("response_type", "code"));

        // make believe we authenticated and got the redirect back, with the code
        MockHttpServletRequest codeRequest = createRequest("web/?code=" + CODE);
        MockHttpServletResponse codeResponse = executeOnSecurityFilters(codeRequest);

        // should have authenticated and given roles, and they have been saved in the session
        SecurityContext context =
                new HttpSessionSecurityContextRepository()
                        .loadContext(new HttpRequestResponseHolder(codeRequest, codeResponse));
        Authentication auth = context.getAuthentication();
        assertNotNull(auth);
        assertEquals("andrea.aime@gmail.com", auth.getPrincipal());

        assertThat(
                auth.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.toList()),
                CoreMatchers.hasItems("R1", "R2", "ROLE_AUTHENTICATED"));
    }

    @Test
    public void testClientConfidential() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        OpenIdConnectFilterConfig config =
                (OpenIdConnectFilterConfig) manager.loadFilterConfig("openidconnect");
        config.setSendClientSecret(true);
        manager.saveFilter(config);

        // make believe we authenticated and got the redirect back, with the code
        MockHttpServletRequest codeRequest = createRequest("web/?code=" + CODE);
        MockHttpServletResponse codeResponse = executeOnSecurityFilters(codeRequest);

        // should have authenticated and given roles, and they have been saved in the session
        SecurityContext context =
                new HttpSessionSecurityContextRepository()
                        .loadContext(new HttpRequestResponseHolder(codeRequest, codeResponse));
        Authentication auth = context.getAuthentication();
        OAuth2ClientContext oauth2Context =
                GeoServerExtensions.bean(ValidatingOAuth2RestTemplate.class)
                        .getOAuth2ClientContext();
        assertEquals("CPURR33RUz-secret", oauth2Context.getAccessToken().getValue());
        assertNotNull(auth);
        assertEquals("andrea.aime@gmail.com", auth.getPrincipal());
    }

    @Test
    public void testIdTokenHintInEndSessionURI() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        OpenIdConnectFilterConfig config =
                (OpenIdConnectFilterConfig) manager.loadFilterConfig("openidconnect");
        config.setSendClientSecret(true);
        config.setPostLogoutRedirectUri(null);
        manager.saveFilter(config);

        // make believe we authenticated and got the redirect back, with the code
        MockHttpServletRequest codeRequest = createRequest("web/?code=" + CODE);
        MockHttpServletResponse codeResponse = executeOnSecurityFilters(codeRequest);

        // should have authenticated and given roles, and they have been saved in the session
        SecurityContext context =
                new HttpSessionSecurityContextRepository()
                        .loadContext(new HttpRequestResponseHolder(codeRequest, codeResponse));
        Authentication auth = context.getAuthentication();
        OAuth2ClientContext oauth2Context =
                GeoServerExtensions.bean(ValidatingOAuth2RestTemplate.class)
                        .getOAuth2ClientContext();
        assertEquals("CPURR33RUz-secret", oauth2Context.getAccessToken().getValue());
        assertNotNull(auth);
        assertEquals("andrea.aime@gmail.com", auth.getPrincipal());
        assertNotNull(oauth2Context.getAccessToken().getAdditionalInformation());
        assertNotNull(oauth2Context.getAccessToken().getAdditionalInformation().get("id_token"));

        final String idToken =
                (String) oauth2Context.getAccessToken().getAdditionalInformation().get("id_token");

        assertEquals(
                config.buildEndSessionUrl(idToken).toString(),
                config.getLogoutUri() + "?id_token_hint=" + idToken);
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

    @After
    public void clear() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }
}
