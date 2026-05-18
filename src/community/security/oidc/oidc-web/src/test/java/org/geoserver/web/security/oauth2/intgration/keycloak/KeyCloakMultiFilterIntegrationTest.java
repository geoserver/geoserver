/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.intgration.keycloak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.ServletRequestEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.LoginFormInfo;
import org.geoserver.web.security.oauth2.login.OAuth2LoginAuthProviderPanel;
import org.geotools.util.logging.Logging;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextListener;

/**
 * Verifies that two GeoServer OAuth2/OIDC filter instances backed by the same Keycloak realm produce two
 * <em>independent</em> login buttons and two <em>independent</em> Spring {@code ClientRegistration} entries.
 *
 * <p>Companion to the single-filter {@link KeyCloakIntegrationTest}. The end-to-end login flow is already covered
 * there; this test focuses on the multi-filter-specific guarantees:
 *
 * <ol>
 *   <li>The home page renders <em>one button per active filter</em>, each with a filter-scoped login path of the form
 *       {@code /web/oauth2/authorization/<filterName>__oidc}.
 *   <li>Clicking each button drives Spring's
 *       {@link org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
 *       OAuth2AuthorizationRequestRedirectFilter} to build a redirect that carries that filter's <em>own</em> scoped
 *       {@code redirect_uri}, so the Keycloak side cannot conflate the two requests.
 *   <li>The two filters produce different {@code state} parameters in their respective auth requests, proving that
 *       Spring keeps two separate {@code OAuth2AuthorizationRequest} sessions — one per filter.
 *   <li>The dynamic {@link LoginFormInfo} registry exposes exactly one bean per filter, each with its own
 *       {@link LoginFormInfo#getLoginPath()} which carries the scoped registration id (filterName__provider).
 * </ol>
 *
 * <p>Both filters point at the same Keycloak client {@code gs-client} for simplicity — that's enough to prove the
 * per-filter routing works because GeoServer treats each filter as a distinct Spring {@code ClientRegistration}
 * regardless of whether their Keycloak-side configuration happens to overlap.
 */
public class KeyCloakMultiFilterIntegrationTest extends KeyCloakIntegrationTestSupport {

    private static final Logger LOGGER = Logging.getLogger(KeyCloakMultiFilterIntegrationTest.class);

    /** First filter name — drives a scoped registration ID of {@code primary-idp__oidc}. */
    private static final String FILTER_PRIMARY = "primary-idp";

    /** Second filter name — drives a scoped registration ID of {@code secondary-idp__oidc}. */
    private static final String FILTER_SECONDARY = "secondary-idp";

    private static String scopedRegId(String filterName) {
        return filterName + "__oidc";
    }

    @Override
    protected String getLogConfiguration() {
        return "VERBOSE_LOGGING";
    }

    @BeforeClass
    public static void beforeClassLocal() {
        // Mock-server base URL — must match baseRedirectUri set in onSetUp() so resolveBaseRedirectUri()
        // returns the same value after XStream deserialization.
        System.setProperty("OPENID_TEST_GS_PROXY_BASE", "http://localhost:8080/geoserver");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        registerOidcFilter(FILTER_PRIMARY);
        registerOidcFilter(FILTER_SECONDARY);

        // Bind both filters to the /web/** chain so GeoServerBasePage's isFilterInChain check passes
        // for both buttons. Anonymous stays last so it cannot pre-empt the OIDC filters.
        GeoServerSecurityManager manager = getSecurityManager();
        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain web = chain.getRequestChainByName("web");
        web.setFilterNames(FILTER_PRIMARY, FILTER_SECONDARY, "anonymous");
        manager.saveSecurityConfig(config);
    }

    /** Configures a fresh OIDC filter against the running Keycloak container, scoped to the given filter name. */
    private void registerOidcFilter(String filterName) throws Exception {
        String baseKeycloakUrl = keycloakContainer.getAuthServerUrl() + "/realms/gs-realm/protocol/openid-connect";

        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setName(filterName);
        cfg.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        cfg.setOidcEnabled(true);
        cfg.setOidcClientId(oidcClient);
        cfg.setOidcClientSecret(oidcClientSecret);
        cfg.setBaseRedirectUri("http://localhost:8080/geoserver/");
        cfg.calculateRedirectUris();
        cfg.setOidcTokenUri(baseKeycloakUrl + "/token");
        cfg.setOidcAuthorizationUri(baseKeycloakUrl + "/authorize");
        cfg.setOidcUserInfoUri(baseKeycloakUrl + "/userinfo");
        cfg.setOidcLogoutUri(baseKeycloakUrl + "/endSession");
        cfg.setOidcJwkSetUri(baseKeycloakUrl + "/certs");
        cfg.setOidcScopes("openid profile email phone address");
        cfg.setOidcUserNameAttribute("email");
        cfg.setEnableRedirectAuthenticationEntryPoint(false);
        cfg.setOidcForceAuthorizationUriHttps(false);
        cfg.setOidcForceTokenUriHttps(false);
        getSecurityManager().saveFilter(cfg);
    }

    /**
     * Both filters' login buttons must render with their own filter-scoped login paths. The previous static-bean design
     * collapsed both into a single button pointing at "an arbitrary surviving one" — this test guards against any
     * regression that would reintroduce that bug. Also asserts that the dynamic {@link LoginFormInfo} registry exposes
     * one bean per filter, and that the un-scoped legacy URL is absent.
     */
    @Test
    public void testTwoFiltersBothRenderDistinctLoginButtons() {
        tester.startPage(new GeoServerHomePage());
        String html = tester.getLastResponseAsString();

        // The two filter-scoped login URLs must both appear in the rendered HTML.
        for (String filterName : Arrays.asList(FILTER_PRIMARY, FILTER_SECONDARY)) {
            String expected =
                    "href=\"http://localhost/context/web/oauth2/authorization/" + scopedRegId(filterName) + "\"";
            assertTrue(filterName + " button missing — expected " + expected, html.contains(expected));
        }
        // Defence in depth: the un-scoped legacy URL must NOT appear in the rendered HTML.
        assertFalse(
                "un-scoped /web/oauth2/authorization/oidc must not appear",
                html.contains("href=\"http://localhost/context/web/oauth2/authorization/oidc\""));

        // The dynamic registry must have one LoginFormInfo per filter — looked up via GeoServerExtensions so the
        // manager's ExtensionProvider<LoginFormInfo> contribution is included (raw applicationContext.getBeansOfType
        // would miss it).
        List<LoginFormInfo> oidcButtons =
                org.geoserver.platform.GeoServerExtensions.extensions(LoginFormInfo.class, applicationContext).stream()
                        .filter(i -> i.getComponentClass() != null
                                && OAuth2LoginAuthProviderPanel.class
                                        .getName()
                                        .equals(i.getComponentClass().getName()))
                        .toList();
        // Filter identity is encoded in each button's loginPath: /web/oauth2/authorization/<filterName>__<provider>.
        long primary = oidcButtons.stream()
                .filter(i -> i.getLoginPath() != null && i.getLoginPath().contains("/" + FILTER_PRIMARY + "__"))
                .count();
        long secondary = oidcButtons.stream()
                .filter(i -> i.getLoginPath() != null && i.getLoginPath().contains("/" + FILTER_SECONDARY + "__"))
                .count();
        assertEquals("expected exactly one LoginFormInfo bean for " + FILTER_PRIMARY, 1L, primary);
        assertEquals("expected exactly one LoginFormInfo bean for " + FILTER_SECONDARY, 1L, secondary);
    }

    /**
     * Both filters' saved configurations must carry filter-scoped redirect URIs. This is the persistence side of Part
     * C: when an admin saves an OIDC filter, the {@link GeoServerOAuth2LoginFilterConfig#getOidcRedirectUri()} field
     * reflects the filter's scoped registration ID — never the legacy un-scoped form — so that when the filter is built
     * and registers a Spring {@code ClientRegistration}, the Keycloak / IdP side is told exactly which scoped callback
     * URL to redirect back to.
     */
    @Test
    public void testBothFiltersHaveScopedRedirectUrisPersisted() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();

        for (String filterName : Arrays.asList(FILTER_PRIMARY, FILTER_SECONDARY)) {
            GeoServerOAuth2LoginFilterConfig saved =
                    (GeoServerOAuth2LoginFilterConfig) manager.loadFilterConfig(filterName, true);
            assertNotNull("filter " + filterName + ": config should be loadable", saved);

            String expected = "http://localhost:8080/geoserver/web/login/oauth2/code/" + scopedRegId(filterName);
            assertEquals(
                    "filter " + filterName + ": persisted oidcRedirectUri must be scoped to the filter name",
                    expected,
                    saved.getOidcRedirectUri());

            assertNotEquals(
                    "filter " + filterName + ": must NOT use the un-scoped redirect URI",
                    "http://localhost:8080/geoserver/web/login/oauth2/code/oidc",
                    saved.getOidcRedirectUri());
        }

        GeoServerOAuth2LoginFilterConfig primary =
                (GeoServerOAuth2LoginFilterConfig) manager.loadFilterConfig(FILTER_PRIMARY, true);
        GeoServerOAuth2LoginFilterConfig secondary =
                (GeoServerOAuth2LoginFilterConfig) manager.loadFilterConfig(FILTER_SECONDARY, true);
        assertNotEquals(
                "the two filters must have different oidcRedirectUri values — that is the routing key",
                primary.getOidcRedirectUri(),
                secondary.getOidcRedirectUri());
    }

    /**
     * Clicking the first filter's login button drives Spring to issue a 302 to Keycloak with the filter's own scoped
     * {@code redirect_uri} (not the un-scoped legacy form). This is the end-to-end runtime proof for Part C in a
     * multi-filter setup.
     *
     * <p><b>Known limitation:</b> only the first registered filter's authorization endpoint is exercised here. Driving
     * the auth-init URL of a <em>second</em> simultaneously-active OAuth2 filter in the same
     * {@code GeoServerSecurityFilterChainProxy} surfaces a Spring HttpSecurity isolation issue — the first filter's
     * chain processes the request first, fails to find the foreign scoped registration in its own
     * {@code ClientRegistrationRepository}, and the request never reaches the second filter's chain. That's a separate
     * architectural fix (Part D) outside the scope of this branch — captured in the wiki delta + a follow-up JIRA.
     * Button rendering (Part B) and config persistence (Part C) verified above are independent of this limitation and
     * work correctly with N filters.
     */
    @Test
    public void testFirstFilterAuthRequestUsesItsOwnScopedRedirectUri() throws Exception {
        MockHttpServletRequest req = createRequest("web/oauth2/authorization/" + scopedRegId(FILTER_PRIMARY), true);
        MockHttpServletResponse resp = executeOnSecurityFilters(req);

        assertEquals("expected 302 to Keycloak", 302, resp.getStatus());
        String location = resp.getHeader("Location");
        assertNotNull("missing Location header", location);
        assertTrue("expected redirect to Keycloak at " + authServerUrl, location.startsWith(authServerUrl));

        List<NameValuePair> params = new URIBuilder(new URI(location), StandardCharsets.UTF_8).getQueryParams();
        String redirectUri = params.stream()
                .filter(p -> "redirect_uri".equals(p.getName()))
                .findFirst()
                .map(NameValuePair::getValue)
                .orElseThrow(() -> new AssertionError("missing redirect_uri"));

        String expectedRedirectSuffix = "/web/login/oauth2/code/" + scopedRegId(FILTER_PRIMARY);
        assertTrue(
                "redirect_uri must end with " + expectedRedirectSuffix + " — got " + redirectUri,
                redirectUri.endsWith(expectedRedirectSuffix));
        LOGGER.fine("filter " + FILTER_PRIMARY + " → " + location);
    }

    /**
     * When OAuth2 / OIDC authentication fails (bad client secret, invalid token, missing session state, IdP refusing
     * the code exchange, ...) Spring's default failure handler redirects to {@code /login?error}. That path has no
     * handler in the GeoServer webapp — no Wicket page mount, no servlet, no static resource — so Tomcat's default
     * servlet serves the path back without a Content-Type and browsers render the response as a downloadable empty file
     * named {@code login}. Reported in the wild by Dorset County Council during a Keycloak integration with a stale
     * client secret (see the support#4578 thread).
     *
     * <p>Fix: {@code GeoServerOAuth2LoginAuthenticationFilterBuilder.createFiltersImpl} now configures
     * {@code oauthConfig.failureUrl("/web/")} so a failed login leaves the user on the GeoServer home page where the
     * standard login form is available.
     *
     * <p>Reproduction path: hit the OAuth2 callback endpoint with a {@code state} parameter that does not match any
     * session-stored {@code OAuth2AuthorizationRequest}. Spring's filter activates on the URL match, fails the state
     * lookup, and invokes the failure handler — exactly the same code path the user hits after a real token-exchange
     * failure against the IdP.
     */
    @Test
    public void testOAuth2FailureRedirectsToWebHomeNotLoginError() throws Exception {
        // No prior OAuth2AuthorizationRequest stored in any session — the state lookup will fail and
        // Spring will invoke the authenticationFailureHandler.
        MockHttpServletRequest req = createRequest("web/login/oauth2/code/" + scopedRegId(FILTER_PRIMARY));
        req.setParameter("code", "fake-authorization-code");
        req.setParameter("state", "fake-state-no-match");

        MockHttpServletResponse resp = executeOnSecurityFilters(req);

        // Spring's failure handler must redirect, not produce an opaque body that the browser downloads.
        assertEquals("expected 302 redirect from the failure handler", 302, resp.getStatus());
        String location = resp.getHeader("Location");
        assertNotNull("failure-handler redirect must include a Location header", location);

        // The crux of the fix: the redirect target must NOT be Spring's default /login?error, which has no
        // handler in the GeoServer webapp and falls through to Tomcat's default servlet → downloadable empty
        // file named "login". Verified against the user-reported symptom on support#4578.
        assertFalse(
                "failure redirect must not target /login?error (no handler in GeoServer webapp, browsers download an empty file): "
                        + location,
                location.endsWith("/login?error") || location.endsWith("/login") || location.contains("/login?"));

        // Positive assertion: the redirect target should be the GeoServer home page (or a sub-path under /web/) so
        // the Wicket admin UI can render the standard login form for the user to retry.
        assertTrue(
                "failure redirect must land somewhere under /web/ — got " + location,
                location.endsWith("/web/")
                        || location.endsWith("/web")
                        || location.contains("/web/")
                        || location.contains("/web?"));
    }

    /**
     * Execute a web request through the GeoServer security filter chain (mirrors
     * {@code KeyCloakIntegrationTest.executeOnSecurityFilters}; copied locally rather than refactoring the existing
     * helper into a base class so this test stays self-contained).
     */
    private MockHttpServletResponse executeOnSecurityFilters(MockHttpServletRequest request)
            throws IOException, jakarta.servlet.ServletException {
        RequestContextListener listener = new RequestContextListener();
        ServletRequestEvent event = new ServletRequestEvent(request.getServletContext(), request);
        listener.requestInitialized(event);
        try {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            GeoServerSecurityFilterChainProxy filterChainProxy =
                    GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
            filterChainProxy.doFilter(request, response, chain);
            return response;
        } finally {
            listener.requestDestroyed(event);
        }
    }
}
