/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
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
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.VariableFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.util.Base64;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
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

    private static final String CLIENT_ID = "geoserver";
    private static final String CLIENT_SECRET = "abcdefg";
    private static final String CODE = "R-2CqM7H1agwc7Cx";
    private static WireMockServer openIdService;
    private static String authService;

    private static final String TEST_OPAQUE_TOKEN =
            "eyJhbGciOiJSU0EtT0FFUCIsImVuYyI6IkEyNTZDQkMtSFM1MTIiLCJraWQiOiI2MEJEQkVDM0I1RDA4RTAyMjYyMjc2OTk3QzU4RDBCMzE5QUE1QzQwIiwidHlwIjoiYXQrand0IiwiY3R5IjoiSldUIn0.M3XYgq7_AoCgSBLslRmc26bD3CXabctyk9nJDWYLLZFk3Qsc2rlerStlLDNnnCeW9b8NQeEh4PibX0M2gmhHLvVyk0GXicxPG8HzEe1A-TYiFH9Y0eQViQZFwEUKDH8cDZbR8-xc9FVdntgrrkuJZI3nNbHM_eavZppPKILCacW49w8R6iH20ANUoAQi3azyYPpO_mV89hhJ5aZYW5ZON8jnFD0bKSCJgxcMJgSyU14Nlk9oQ9wxPfxrSnuaJanbqSdpYDii2LkTm_O5b8v72jaLwr-3ZDnA6LA8_4KXcXJvJaAdzMDWA_eCbzDFb1T6BbUmOEIukX25ssAgTDMczw.TL9b1TcXwhRRinASSr7tvg.MdNG-2HgHsjHykL_6mepDkJlCUXPnEQvpG82eApGfSSx3EwMfzLfahPAcigwSoVDW5oN-tZwmTHg8PIQ7mj3kaJiZ5XVgrF6LYUZPcXKi_d6WZCO2hL2tHkZKqAndIjGhI3km1nw6i1nF86kn7Pam6uXd-5P9NOqk53VLt711-76GHfGXOZfOfRvNK9IgFwyf8jWL4reIj6f57X5TKWr732DydUUnt9U-V1klUHaKgHNMXkit2zs8pmfWzqA6PjkPejX2xUKqYpC4KHhf4mCkShMyJg2OcA_Mocz8ZGoNDdt1SmWtMZKemEAkoauQ8qmWc1tmXl44raiIkKLP2OJY_nNZVr3TM8zl0dkGZmDfgAmT-coJOVsnNAiWJrhh4rDf7ybWdQfr58FtKtdZQWQklXwk3-fprBSJWzdpaxWkVgXtAfco4Z11KN3vs8csCksUVUXQTTH5UI7Lqri_7lI-ykIeDQ2qMglIem-l7mQ7ycgNLQjxHnJN8_u6Z6IFAbQk807X-4JccS21IeM_l7WvjTNbWQL40HOpcs_EAMw61vT-Jj2CTJ9d-ACqtpno4Uzk_YDX2NV81wZ--3043bjkaikVlsdlzr423JYWplQ7q8_ArN154UmoiTOao0Jy2dzgYLxC4SIIO7MuKYg88VuAWE6Wt19dAW8m7ekgMW3jFpq4E7fqBoeiEA5Vkiv9BnN6S33p964wt9X9fqUF0LehqQ-QYtSA3MUQpxz0kj9pjIzuUKCIch2CbR12RPrsyXKCawTmRS1J_BWRKtC9sHcmdhCTmZUULggYo_T9GJjBxbUC-IddElF1tlgs9iwfQCWvE600lnk4mCsOh6AE2kz95JvoJSpQwHVhtgxyMdb55tQvrCNKsLrWcF2NohLCdoErce7jfkjFaVZUTgHQYOVwu-icwGqrtnFClmPbIGo7x95vprKtZfBIlwWYBVSSMtjw4kwkJerW1jMchjlqfJR6_1Lr2yyS9DBVbmE6kLw_xltHFU6NYGCnKQjWVDzhxlN-nEanIQvPcIaTqYrXxY74fNFU0DpRmScnX920C-_3BPG8sVsgfAC1pRw-3bec4YCrGFhCJoMmc0dnJV8wi9uws5trPKffDEOD4iMXiXsy3gcCS5z-JJlddy19NLB6qPWI8kSJkfKBdlrwGx7R-_rcS6dxImI-ov3JSpvy100C3X6IF-F5VRQrL7wH1LTF2r05OxxYOv4uYtvmpQVXQc3UePA5gjknUEmtEobDVeiP4NjOBiIsWK4AMqIfy7Mn55_cVlSOXWZe2qJO70pGI3fjV7wP0ubJzHbPAj50EedghtSUtkwDdIyfgvLssvoHlVTn4HLPLaw6IHEmlS51HIyVPGdckqTzEyijV3hyNEl2cLDPYy1uuvTyUzjezh6rLxUTnqE7zOLhVsDh8zxLgFVrpXXzlrMUwt0ZM3Vshsv2ENC_SwbC9j18FaL0nHkzdv_YtC_ME0Rk_3edxZNgxhkynJjzoIyov4TvUhaI4okidrztwbGZOsK008-Qx-Trgs_mkkp6XmgT7SlP33cvhQ6DXwgBzb0BzwWCdq7JpDbANSORARoW2rNt75dEylVy53H0_urrRlaY4-zMdsPuHEcKzzzfKR5pj_0VCSL9C8ws1RfcGX1Kzt621JUH07rHL0uBlc4xkE54MS1AuAK2wENVm2PbUdykqdpx17ug_eS6v-ndQ4UUjFhYU0yEl1zZKiT.wskwKJEtkwHmmvc74Ay8uk0dKwJVnNW6uW5anSlpZsQ";

    @BeforeClass
    public static void beforeClass() throws Exception {
        openIdService = new WireMockServer(wireMockConfig()
                .dynamicPort()
                // uncomment the following to get wiremock logging
                .notifier(new ConsoleNotifier(true)));
        openIdService.start();

        openIdService.stubFor(WireMock.get(urlEqualTo("/.well-known/jwks.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("jwks.json")));

        openIdService.stubFor(WireMock.post(urlPathEqualTo("/token"))
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("client_id=" + CLIENT_ID))
                //                        .withQueryParam("client_secret",
                // equalTo(CLIENT_SECRET))
                .withRequestBody(containing("code=" + CODE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("token_response.json")));
        openIdService.stubFor(WireMock.post(urlPathEqualTo("/token"))
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("client_id=" + CLIENT_ID))
                .withRequestBody(containing("client_secret=" + CLIENT_SECRET))
                .withRequestBody(containing("code=" + CODE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("token_response_with_secret.json")));

        openIdService.stubFor(WireMock.get(WireMock.urlMatching(".*/userinfo")) // disallow query parameters
                .withHeader("Authorization", equalTo("Bearer " + TEST_OPAQUE_TOKEN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("userinfo-opaque.json")));

        openIdService.stubFor(WireMock.get(WireMock.urlMatching(".*/userinfo")) // disallow query parameters
                .withHeader(
                        "Authorization",
                        WireMock.or(
                                equalTo("Bearer CPURR33RUz-gGhjwODTd9zXo5JkQx4wS"),
                                equalTo("Bearer CPURR33RUz-secret")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("userinfo.json")));

        openIdService.stubFor(WireMock.post(urlPathEqualTo("/introspect"))
                .withHeader(
                        "Authorization",
                        equalTo("Basic " + Base64.encodeBytes((CLIENT_ID + ":" + CLIENT_SECRET).getBytes())))
                .withRequestBody(containing("token=" + TEST_OPAQUE_TOKEN))
                .withRequestBody(containing("token_type_hint=access_token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"active\": true}")));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        openIdService.stop();
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
        filterConfig.setIntrospectionEndpointUrl(authService + "/introspect");
        filterConfig.setLoginEndpoint("/j_spring_oauth2_openid_connect_login");
        filterConfig.setLogoutEndpoint("/j_spring_oauth2_openid_connect_logout");
        filterConfig.setLogoutUri(authService + "/endSession");
        filterConfig.setJwkURI(authService + "/.well-known/jwks.json");
        filterConfig.setEnforceTokenValidation(false);
        filterConfig.setScopes("openid profile email phone address");
        filterConfig.setEnableRedirectAuthenticationEntryPoint(true);
        filterConfig.setRedirectUri("http://localhost/geoserver");
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
        RequestFilterChain defaultChain = chain.getRequestChainByName("default");
        defaultChain.setFilterNames("openidconnect", "anonymous");
        manager.saveSecurityConfig(config);
    }

    @Before
    public void resetFilterConfig() throws IOException, SecurityConfigException {
        GeoServerSecurityManager manager = getSecurityManager();
        OpenIdConnectFilterConfig filterConfig =
                (OpenIdConnectFilterConfig) manager.loadFilterConfig("openidconnect", true);
        filterConfig.setRoleSource(OpenIdConnectFilterConfig.OpenIdRoleSource.IdToken);
        manager.saveFilter(filterConfig);
    }

    /** Enable the Spring Security authentication filters, we want the test to be complete and realistic */
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
        SecurityContext context = new HttpSessionSecurityContextRepository()
                .loadContext(new HttpRequestResponseHolder(codeRequest, codeResponse));
        Authentication auth = context.getAuthentication();
        assertNotNull(auth);
        assertEquals("andrea.aime@gmail.com", auth.getPrincipal());

        assertThat(
                auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList()),
                CoreMatchers.hasItems("R1", "R2", "ROLE_AUTHENTICATED"));
    }

    @Test
    public void testClientConfidential() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        OpenIdConnectFilterConfig config = (OpenIdConnectFilterConfig) manager.loadFilterConfig("openidconnect", true);
        config.setSendClientSecret(true);
        manager.saveFilter(config);

        // make believe we authenticated and got the redirect back, with the code
        MockHttpServletRequest codeRequest = createRequest("web/?code=" + CODE);
        MockHttpServletResponse codeResponse = executeOnSecurityFilters(codeRequest);

        // should have authenticated and given roles, and they have been saved in the session
        SecurityContext context = new HttpSessionSecurityContextRepository()
                .loadContext(new HttpRequestResponseHolder(codeRequest, codeResponse));
        Authentication auth = context.getAuthentication();
        OAuth2ClientContext oauth2Context =
                GeoServerExtensions.bean(ValidatingOAuth2RestTemplate.class).getOAuth2ClientContext();
        assertEquals("CPURR33RUz-secret", oauth2Context.getAccessToken().getValue());
        assertNotNull(auth);
        assertEquals("andrea.aime@gmail.com", auth.getPrincipal());
    }

    @Test
    public void testIdTokenHintInEndSessionURI() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        OpenIdConnectFilterConfig config = (OpenIdConnectFilterConfig) manager.loadFilterConfig("openidconnect", true);
        config.setSendClientSecret(true);
        config.setPostLogoutRedirectUri(null);
        manager.saveFilter(config);

        // make believe we authenticated and got the redirect back, with the code
        MockHttpServletRequest codeRequest = createRequest("web/?code=" + CODE);
        MockHttpServletResponse codeResponse = executeOnSecurityFilters(codeRequest);

        // should have authenticated and given roles, and they have been saved in the session
        SecurityContext context = new HttpSessionSecurityContextRepository()
                .loadContext(new HttpRequestResponseHolder(codeRequest, codeResponse));
        Authentication auth = context.getAuthentication();
        OAuth2ClientContext oauth2Context =
                GeoServerExtensions.bean(ValidatingOAuth2RestTemplate.class).getOAuth2ClientContext();
        assertEquals("CPURR33RUz-secret", oauth2Context.getAccessToken().getValue());
        assertNotNull(auth);
        assertEquals("andrea.aime@gmail.com", auth.getPrincipal());
        assertNotNull(oauth2Context.getAccessToken().getAdditionalInformation());
        assertNotNull(oauth2Context.getAccessToken().getAdditionalInformation().get("id_token"));

        final String idToken = (String)
                oauth2Context.getAccessToken().getAdditionalInformation().get("id_token");

        assertEquals(
                config.buildEndSessionUrl(idToken).toString(), config.getLogoutUri() + "?id_token_hint=" + idToken);
    }

    @Test
    public void testOpaqueBearerToken() throws Exception {
        // make it pull the roles from the userinfo
        GeoServerSecurityManager manager = getSecurityManager();
        OpenIdConnectFilterConfig filterConfig =
                (OpenIdConnectFilterConfig) manager.loadFilterConfig("openidconnect", true);
        filterConfig.setRoleSource(OpenIdConnectFilterConfig.OpenIdRoleSource.UserInfo);
        manager.saveFilter(filterConfig);

        // set up a GetFeature request with a bearer token in the headers
        MockHttpServletRequest request = createRequest(
                "wfs?service=WFS&version=2.0.0&request=GetFeature&typeName=" + getLayerId(MockData.BASIC_POLYGONS));
        request.addHeader("Authorization", "Bearer " + TEST_OPAQUE_TOKEN);
        MockHttpServletResponse response = executeOnSecurityFilters(request);

        System.out.println(response.getStatus());
        System.out.println(response.getContentAsString());

        SecurityContext context = new HttpSessionSecurityContextRepository()
                .loadContext(new HttpRequestResponseHolder(request, response));
        Authentication auth = context.getAuthentication();
        assertNotNull(auth);
        assertEquals("claudius.ptolemy@gmail.com", auth.getPrincipal());
        assertThat(
                auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList()),
                CoreMatchers.hasItems("geography", "astronomy", "ROLE_AUTHENTICATED"));
    }

    private MockHttpServletResponse executeOnSecurityFilters(MockHttpServletRequest request)
            throws IOException, javax.servlet.ServletException {
        // for session local support in Spring
        new RequestContextListener().requestInitialized(new ServletRequestEvent(request.getServletContext(), request));

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
