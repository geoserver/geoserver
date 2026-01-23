/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.security.oauth2.login;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;
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
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;

@TestSetup(run = TestSetupFrequency.REPEAT)
public class GeoServerOAuth2LoginIntegrationTest extends GeoServerSystemTestSupport {

    /** Puts the client (=GeoServer) created random nonce into the id token which is embedded in the access token. */
    private class TokenEndpointBasicTransformer implements ResponseTransformerV2 {
        @Override
        public String getName() {
            return "token-endpoint";
        }

        @Override
        public Response transform(Response response, ServeEvent serveEvent) {
            Request lRequest = serveEvent.getRequest();
            try {
                return transformImpl(lRequest);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in wiremock.", e);
                throw new RuntimeException(e);
            }
        }

        private Response transformImpl(Request pRequest) throws Exception {
            Charset cs = StandardCharsets.UTF_8;
            String idTokenTempl = resourceToString("/OpenIdConnectIntegrationTest/id-token-tmpl.json", cs);
            String accessTokenTempl = resourceToString("/OpenIdConnectIntegrationTest/token-response-tmpl.json", cs);
            if (reqParamNonce == null) {
                throw new IllegalArgumentException("nonce must not be null");
            }
            String idToken = idTokenTempl.replace("${nonce}", reqParamNonce);
            byte[] secretKey = CLIENT_SECRET.getBytes();
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            Payload payload = new Payload(idToken);
            JWSObject jwsObject = new JWSObject(header, payload);
            JWSSigner signer = new MACSigner(secretKey);
            jwsObject.sign(signer);
            String jwt = jwsObject.serialize();
            String accessToken = accessTokenTempl.replace("${id_token}", jwt);
            return Response.response()
                    .body(accessToken)
                    .headers(new HttpHeaders(HttpHeader.httpHeader("Content-Type", "application/json")))
                    .build();
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }
    }

    private static final String CLIENT_ID = "kbyuFDidLLm280LIwVFiazOqjO3ty8KH";
    private static final String CLIENT_SECRET = "60Op4HFM0I8ajz0WdiStAbziZ-VFQttXuxixHHs2R7r7-CW8GR79l-mmLqMhc-Sa";
    private static final String CODE = "R-2CqM7H1agwc7Cx";

    private WireMockServer openIdService;
    private String authService;
    private String baseRedirectUri = "http://localhost:8080/geoserver/";
    private String reqParamNonce;

    public void setupWireMock() throws Exception {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://localhost/geoserver");

        openIdService =
                new WireMockServer(wireMockConfig().dynamicPort().extensions(new TokenEndpointBasicTransformer()));
        // uncomment the following to get wiremock logging
        // .notifier(new ConsoleNotifier(true)));
        openIdService.start();

        openIdService.stubFor(WireMock.get(urlEqualTo(".well-known/jwks.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("jkws.json")));

        openIdService.stubFor(WireMock.get(WireMock.urlMatching(".*/userinfo")) // disallow query
                // parameters
                /*
                 * .withHeader( "Authorization", equalTo("Bearer CPURR33RUz-gGhjwODTd9zXo5JkQx4wS"))
                 */
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBodyFile("userinfo.json")));
    }

    @Override
    protected void onTearDown(SystemTestData pTestData) throws Exception {
        if (openIdService != null) {
            openIdService.shutdown();
        }
        super.onTearDown(pTestData);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        setupWireMock();
        super.onSetUp(testData);

        // prepare mock server base path
        authService = "http://localhost:" + openIdService.port();

        // setup openid
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2LoginFilterConfig filterConfig = new GeoServerOAuth2LoginFilterConfig();
        filterConfig.setName("openidconnect");
        filterConfig.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        filterConfig.setOidcEnabled(true);
        filterConfig.setOidcClientId(CLIENT_ID);
        filterConfig.setOidcClientSecret(CLIENT_SECRET);
        filterConfig.setBaseRedirectUri(baseRedirectUri);
        filterConfig.calculateRedirectUris();
        filterConfig.setOidcTokenUri(authService + "/token");
        filterConfig.setOidcAuthorizationUri(authService + "/authorize");
        filterConfig.setOidcUserInfoUri(authService + "/userinfo");
        filterConfig.setOidcLogoutUri(authService + "/endSession");
        filterConfig.setOidcJwkSetUri(authService + "/.well-known/jwks.json");
        filterConfig.setOidcEnforceTokenValidation(false);
        filterConfig.setOidcScopes("openid profile email phone address");
        filterConfig.setEnableRedirectAuthenticationEntryPoint(true);
        filterConfig.setOidcUserNameAttribute("email");
        filterConfig.setRoleSource(GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource.IdToken);
        filterConfig.setTokenRolesClaim("roles");
        // for ease of testing, do not use HTTPS
        filterConfig.setOidcForceAuthorizationUriHttps(false);
        filterConfig.setOidcForceTokenUriHttps(false);
        filterConfig.setOidcJwsAlgorithmName(JwsAlgorithms.HS256);
        manager.saveFilter(filterConfig);

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");
        www.setFilterNames("openidconnect", "anonymous");

        manager.saveSecurityConfig(config);
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
    public void testBearerJwtAuthenticatesNoRedirect() throws Exception {
        RSAKey rsa = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        String jwksPath = "/jwks-rs.json";
        repointJwkSetUri(jwksPath);
        stubJwks(jwksPath, rsa.toPublicJWK());

        String token = signJwt(rsa, "m2m@example.com", List.of("R1", "R2"), List.of());

        MockHttpServletRequest req = createRequest("web/");
        req.addHeader("Authorization", "Bearer " + token);

        AtomicReference<Authentication> authRef = new AtomicReference<>();
        MockHttpServletResponse resp = executeOnSecurityFiltersCapturingAuth(req, authRef);

        assertEquals(200, resp.getStatus());
        assertNull(resp.getHeader("Location"));

        Authentication auth = authRef.get();
        assertNotNull(auth);
        assertThat(auth, CoreMatchers.instanceOf(JwtAuthenticationToken.class));

        assertThat(
                auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList()),
                CoreMatchers.hasItems("R1", "R2", "ROLE_AUTHENTICATED"));
    }

    @Test
    public void testBearerJwtIsStatelessNotSavedInSession() throws Exception {
        RSAKey rsa = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        String jwksPath = "/jwks-rs.json";
        repointJwkSetUri(jwksPath);
        stubJwks(jwksPath, rsa.toPublicJWK());

        String token = signJwt(rsa, "m2m@example.com", List.of("R1"), List.of());

        // Force session creation before request to prove we are not persisting bearer auth into it.
        MockHttpServletRequest req1 = createRequest("web/");
        HttpSession session = req1.getSession(true);
        req1.addHeader("Authorization", "Bearer " + token);

        AtomicReference<Authentication> authRef = new AtomicReference<>();
        MockHttpServletResponse resp1 = executeOnSecurityFiltersCapturingAuth(req1, authRef);
        assertEquals(200, resp1.getStatus());
        assertNull(resp1.getHeader("Location"));

        // Ensure nothing was stored in the session
        assertNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));

        // Second request: same session, no Authorization header -> must trigger login redirect (not authenticated)
        MockHttpServletRequest req2 = createRequest("web/");
        req2.setSession(session);
        MockHttpServletResponse resp2 = executeOnSecurityFilters(req2);

        assertEquals(302, resp2.getStatus());
        assertEquals(baseRedirectUri + "web/oauth2/authorizationoidc", resp2.getHeader("Location"));
    }

    @Test
    public void testBearerJwtInvalidSignatureNoRedirect() throws Exception {
        RSAKey jwksKey = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        String jwksPath = "/jwks-rs.json";
        repointJwkSetUri(jwksPath);
        stubJwks(jwksPath, jwksKey.toPublicJWK());

        // Sign with a different key, so signature verification must fail.
        RSAKey signerKey = new RSAKeyGenerator(2048).keyID("kid-2").generate();
        String token = signJwt(signerKey, "m2m@example.com", List.of("R1"), List.of());

        MockHttpServletRequest req = createRequest("web/");
        req.addHeader("Authorization", "Bearer " + token);

        AtomicReference<Authentication> authRef = new AtomicReference<>();
        MockHttpServletResponse resp = executeOnSecurityFiltersCapturingAuth(req, authRef);

        assertNull(resp.getHeader("Location"));
        assertThat(resp.getStatus(), Matchers.anyOf(Matchers.is(401), Matchers.is(403)));
        assertNull(authRef.get());
    }

    @Test
    public void testBearerJwtAudienceValidationRejectsWrongAudience() throws Exception {
        // Enable audience validation in the same OIDC filter config
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2LoginFilterConfig cfg =
                (GeoServerOAuth2LoginFilterConfig) manager.loadFilterConfig("openidconnect", true);
        cfg.setValidateTokenAudience(true);
        cfg.setValidateTokenAudienceClaimName("aud");
        cfg.setValidateTokenAudienceClaimValue("geoserver");
        manager.saveFilter(cfg);

        RSAKey rsa = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        String jwksPath = "/jwks-rs.json";
        repointJwkSetUri(jwksPath);
        stubJwks(jwksPath, rsa.toPublicJWK());

        // aud does NOT contain "geoserver"
        String token = signJwt(rsa, "m2m@example.com", List.of("R1"), List.of("other-aud"));

        MockHttpServletRequest req = createRequest("web/");
        req.addHeader("Authorization", "Bearer " + token);

        AtomicReference<Authentication> authRef = new AtomicReference<>();
        MockHttpServletResponse resp = executeOnSecurityFiltersCapturingAuth(req, authRef);

        assertNull(resp.getHeader("Location"));
        assertThat(resp.getStatus(), Matchers.anyOf(Matchers.is(401), Matchers.is(403)));
        assertNull(authRef.get());
    }

    @Test
    public void testBearerJwtWhenResourceServerModeDisabledRedirectsToProvider() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2LoginFilterConfig cfg =
                (GeoServerOAuth2LoginFilterConfig) manager.loadFilterConfig("openidconnect", true);

        boolean prev = cfg.isEnableResourceServerMode();
        boolean prevSkip = cfg.getEnableRedirectAuthenticationEntryPoint();
        cfg.setEnableResourceServerMode(false);
        cfg.setEnableRedirectAuthenticationEntryPoint(true);
        manager.saveFilter(cfg);

        try {
            // Prepare JWKS + JWT (even though RS mode is disabled; token must be ignored)
            RSAKey rsa = new RSAKeyGenerator(2048).keyID("kid-1").generate();
            String jwksPath = "/jwks-disabled.json";
            repointJwkSetUri(jwksPath);
            stubJwks(jwksPath, rsa.toPublicJWK());
            String token = signJwt(rsa, "m2m@example.com", List.of("R1"), List.of());

            MockHttpServletRequest req = createRequest("web/");
            req.addHeader("Authorization", "Bearer " + token);

            AtomicReference<Authentication> authRef = new AtomicReference<>();
            MockHttpServletResponse resp = executeOnSecurityFiltersCapturingAuth(req, authRef);

            // With RS mode disabled, Bearer requests fall back to the login entrypoint behavior
            assertEquals(302, resp.getStatus());
            assertEquals(baseRedirectUri + "web/oauth2/authorizationoidc", resp.getHeader("Location"));
            assertNull(authRef.get());
        } finally {
            GeoServerOAuth2LoginFilterConfig restore =
                    (GeoServerOAuth2LoginFilterConfig) manager.loadFilterConfig("openidconnect", true);
            restore.setEnableResourceServerMode(prev);
            restore.setEnableRedirectAuthenticationEntryPoint(prevSkip);
            manager.saveFilter(restore);
        }
    }

    @Test
    public void testRoleExtraction() throws Exception {
        // request token with basic auth

        openIdService.stubFor(WireMock.post(urlPathEqualTo("/token"))
                .withBasicAuth(CLIENT_ID, CLIENT_SECRET)
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("code=" + CODE))
                .withRequestBody(containing(
                        "redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fgeoserver%2Fweb%2Flogin%2Foauth2%2Fcode%2Foidc"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withTransformers("token-endpoint")));

        verifyLoginLogout();
    }

    @Test
    public void testClientConfidental() throws Exception {

        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2LoginFilterConfig config =
                (GeoServerOAuth2LoginFilterConfig) manager.loadFilterConfig("openidconnect", true);
        config.setOidcAuthenticationMethodPostSecret(true);
        manager.saveFilter(config);

        // request token with secret in post body
        openIdService.stubFor(WireMock.post(urlPathEqualTo("/token"))
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("client_id=" + CLIENT_ID))
                .withRequestBody(containing("client_secret=" + CLIENT_SECRET))
                .withRequestBody(containing("code=" + CODE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withTransformers("token-endpoint")));

        verifyLoginLogout();
    }

    private void verifyLoginLogout() throws IOException, ServletException {
        // given: request a protected URL
        MockHttpServletRequest webRequest = createRequest("web/");

        // when: execute request
        MockHttpServletResponse webResponse = executeOnSecurityFilters(webRequest);
        HttpSession lSession = webRequest.getSession();

        // then: "skip login dialog"/AEP is enabled -> spring's initiate login endpoint
        assertEquals(302, webResponse.getStatus());
        String location = webResponse.getHeader("Location");
        String authStartPath = "web/oauth2/authorizationoidc";
        assertEquals(baseRedirectUri + authStartPath, location);

        // when: request to spring's initiate login endpoint is issued on same session
        webRequest = createRequest("/" + authStartPath);
        webRequest.setSession(lSession);
        webResponse = executeOnSecurityFilters(webRequest);

        // then: response is forward to auth server login
        location = webResponse.getHeader("Location");
        assertNotNull(location);
        assertThat(location, CoreMatchers.startsWith(baseRedirectUri));

        Map<String, Object> kvp = KvpUtils.parseQueryString(location);
        if (!kvp.isEmpty()) {
            assertThat(kvp, Matchers.hasEntry("client_id", CLIENT_ID));
            assertThat(
                    kvp,
                    Matchers.hasEntry("redirect_uri", "http://localhost:8080/geoserver/web/login/oauth2/code/oidc"));
            assertThat(kvp, Matchers.hasEntry("scope", "openid profile email phone address"));
            assertThat(kvp, Matchers.hasEntry("response_type", "code"));

            Object state = kvp.get("state");
            assertNotNull(state);
            Object lNonce = kvp.get("nonce");
            assertNotNull(lNonce);
            reqParamNonce = lNonce.toString();

            // make believe we authenticated and got the redirect back, with the code
            MockHttpServletRequest codeRequest =
                    createRequest("web/login/oauth2/code/oidc?code=" + CODE + "&state=" + state);
            codeRequest.setSession(lSession);
            executeOnSecurityFilters(codeRequest);

            // should have authenticated and given roles, and they have been saved in the session
            SecurityContext context = new HttpSessionSecurityContextRepository()
                    .loadDeferredContext(codeRequest)
                    .get();
            Authentication auth = context.getAuthentication();
            assertNotNull(auth);
            assertEquals(DefaultOidcUser.class, auth.getPrincipal().getClass());
            DefaultOidcUser lUser = (DefaultOidcUser) auth.getPrincipal();
            assertEquals("andrea.aime@gmail.com", lUser.getName());
            assertEquals(lNonce, lUser.getNonce());

            assertThat(
                    auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList()),
                    CoreMatchers.hasItems("R1", "R2", "ROLE_AUTHENTICATED"));

            // given: id token
            String lIdTokenValue = lUser.getIdToken().getTokenValue();
            assertNotNull(lIdTokenValue);

            // when: logout
            webRequest = createRequest("/logout");
            webRequest.setSession(lSession);
            webResponse = executeOnSecurityFilters(webRequest);

            // then: id token value must be in id_token_hint
            location = webResponse.getHeader("Location");
            assertNotNull(location);
            kvp = KvpUtils.parseQueryString(location);
            assertThat(kvp, Matchers.hasEntry("id_token_hint", lIdTokenValue));
        }
    }

    private void repointJwkSetUri(String jwksPath) throws IOException, SecurityConfigException {
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2LoginFilterConfig cfg =
                (GeoServerOAuth2LoginFilterConfig) manager.loadFilterConfig("openidconnect", true);
        cfg.setOidcJwkSetUri(authService + jwksPath);
        manager.saveFilter(cfg);
    }

    private void stubJwks(String jwksPath, RSAKey publicKey) {
        Map<String, Object> json = new JWKSet(publicKey).toJSONObject(true);
        openIdService.stubFor(WireMock.get(urlPathEqualTo(jwksPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(json.toString())));
    }

    private String signJwt(RSAKey signingKey, String email, List<String> roles, List<String> audiences)
            throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder b = new JWTClaimsSet.Builder()
                .subject("m2m-client")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(120)))
                .claim("email", email)
                .claim("roles", roles);

        if (audiences != null && !audiences.isEmpty()) {
            for (String aud : audiences) {
                b.audience(aud);
            }
        } else {
            b.audience("geoserver");
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(signingKey.getKeyID())
                        .build(),
                b.build());
        jwt.sign(new RSASSASigner(signingKey.toPrivateKey()));
        return jwt.serialize();
    }

    private MockHttpServletResponse executeOnSecurityFiltersCapturingAuth(
            MockHttpServletRequest request, AtomicReference<Authentication> authRef)
            throws IOException, ServletException {

        new RequestContextListener().requestInitialized(new ServletRequestEvent(request.getServletContext(), request));

        HttpServlet terminal = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) {
                authRef.set(SecurityContextHolder.getContext().getAuthentication());
            }
        };

        MockFilterChain chain = new MockFilterChain(terminal);
        MockHttpServletResponse response = new MockHttpServletResponse();
        GeoServerSecurityFilterChainProxy filterChainProxy =
                GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
        filterChainProxy.doFilter(request, response, chain);

        return response;
    }

    private MockHttpServletResponse executeOnSecurityFilters(MockHttpServletRequest request)
            throws IOException, jakarta.servlet.ServletException {
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
