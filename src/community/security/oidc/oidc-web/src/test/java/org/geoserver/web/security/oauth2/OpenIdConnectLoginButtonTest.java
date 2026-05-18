package org.geoserver.web.security.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.ServletRequestEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.LoginFormInfo;
import org.geoserver.web.security.oauth2.login.OAuth2LoginAuthProviderPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextListener;

@TestSetup(run = TestSetupFrequency.REPEAT)
public class OpenIdConnectLoginButtonTest extends GeoServerWicketTestSupport {

    private static final String MARKUP_IMG =
            "<img src=\"./wicket/resource/org.geoserver.web.security.oauth2.login.OAuth2LoginAuthProviderPanel/openid";
    private static final String MARKUP_FORM =
            "href=\"http://localhost/context/web/oauth2/authorization/openidconnect__oidc\"";

    @Override
    protected String getLogConfiguration() {
        return "DEFAULT_LOGGING";
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    private void activateOidcFilterWithEnabledState(boolean pEnabled)
            throws IOException, SecurityConfigException, Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2LoginFilterConfig filterConfig = new GeoServerOAuth2LoginFilterConfig();
        filterConfig.setName("openidconnect");
        filterConfig.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        filterConfig.setOidcEnabled(pEnabled);
        filterConfig.setOidcClientId("foo");
        filterConfig.setOidcClientSecret("bar");
        filterConfig.setOidcTokenUri("https://www.connectid/fake/test");
        filterConfig.setOidcAuthorizationUri("https://www.connectid/fake/test");
        filterConfig.setOidcUserInfoUri("https://www.connectid/fake/test");
        filterConfig.setOidcJwkSetUri("https://www.connectid/fake/test");
        manager.saveFilter(filterConfig);

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");
        www.setFilterNames("openidconnect", "anonymous");
        manager.saveSecurityConfig(config);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data to setup, this is a smoke test
    }

    @BeforeClass
    public static void setup() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://localhost/geoserver");
    }

    @Test
    public void testLoginButtonPresentWithOidcEnabled() throws SecurityConfigException, IOException, Exception {
        boolean lOidcEnabled = true;
        activateOidcFilterWithEnabledState(lOidcEnabled);

        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();
        LOGGER.log(Level.INFO, "Last HTML page output:\n" + html);

        // the login form is there and has the link
        assertTrue(html.contains(MARKUP_FORM));
        assertTrue(html.contains(MARKUP_IMG));
    }

    @Test
    public void testLoginButtonOmittedWithOidcDisabled() throws SecurityConfigException, IOException, Exception {
        boolean lOidcEnabled = false;
        activateOidcFilterWithEnabledState(lOidcEnabled);

        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();
        LOGGER.log(Level.INFO, "Last HTML page output:\n" + html);

        // the login form is there and has the link
        assertFalse(html.contains(MARKUP_FORM));
        assertFalse(html.contains(MARKUP_IMG));
    }

    // ── Multi-filter integration tests ────────────────────────────────────────
    //
    // These tests guard against the regression where multiple OIDC filters collapsed
    // into a single login button pointing at "an arbitrary surviving one". With the
    // dynamic per-filter registration in OAuth2LoginButtonManager, each filter must
    // produce its own LoginFormInfo bean, the home page must render one button per
    // filter, and the rendered loginPaths must be scoped to each filter's name so
    // Spring's OAuth2 filter chain routes the click to the correct ClientRegistration.

    /** Builds a minimal OIDC filter config bound to a given name; all *Uris point at a stub host. */
    private GeoServerOAuth2LoginFilterConfig newOidcFilterConfig(String name) {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setName(name);
        cfg.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        cfg.setOidcEnabled(true);
        cfg.setOidcClientId("client-" + name);
        cfg.setOidcClientSecret("secret-" + name);
        cfg.setOidcTokenUri("https://idp.example.com/" + name + "/token");
        cfg.setOidcAuthorizationUri("https://idp.example.com/" + name + "/auth");
        cfg.setOidcUserInfoUri("https://idp.example.com/" + name + "/userinfo");
        cfg.setOidcJwkSetUri("https://idp.example.com/" + name + "/jwks");
        return cfg;
    }

    /** Saves each filter via the security manager and adds them all to the {@code /web/**} chain. */
    private void saveOidcFiltersAndAddToWebChain(List<String> filterNames) throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        for (String filterName : filterNames) {
            manager.saveFilter(newOidcFilterConfig(filterName));
        }
        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain web = chain.getRequestChainByName("web");
        // anonymous stays last so it does not pre-empt the OIDC filters
        String[] names = new String[filterNames.size() + 1];
        for (int i = 0; i < filterNames.size(); i++) names[i] = filterNames.get(i);
        names[filterNames.size()] = "anonymous";
        web.setFilterNames(names);
        manager.saveSecurityConfig(config);
    }

    /**
     * Builds the expected anchor markup for a given OIDC filter's scoped login path. The path follows
     * {@code /web/oauth2/authorization/<filterName>__oidc} so that Spring Security's OAuth2 filter chain can route the
     * click to that filter's {@code ClientRegistration} (which uses the same scoped registration ID).
     */
    private static String expectedScopedLoginAnchor(String filterName) {
        return "href=\"http://localhost/context/web/oauth2/authorization/" + filterName + "__oidc\"";
    }

    /**
     * Two OIDC filters, both in the {@code /web/**} chain, must produce two independent login buttons — one per filter
     * — each pointing at its own scoped authorization endpoint. The previous static-bean design collapsed both into one
     * button pointing at "an arbitrary surviving one"; the dynamic registry must keep them independent.
     */
    @Test
    public void testTwoOidcFiltersRenderTwoDistinctLoginButtons() throws Exception {
        saveOidcFiltersAndAddToWebChain(Arrays.asList("keycloak-prod", "auth0-staging"));

        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();
        LOGGER.log(Level.INFO, "Last HTML page output:\n" + html);

        // Both scoped login URLs must appear in the rendered HTML.
        assertTrue(
                "keycloak-prod button missing — expected " + expectedScopedLoginAnchor("keycloak-prod"),
                html.contains(expectedScopedLoginAnchor("keycloak-prod")));
        assertTrue(
                "auth0-staging button missing — expected " + expectedScopedLoginAnchor("auth0-staging"),
                html.contains(expectedScopedLoginAnchor("auth0-staging")));

        // The dynamic-registration manager must contribute one LoginFormInfo per filter, both visible via the
        // GeoServerExtensions lookup that GeoServerBasePage uses. We go through GeoServerExtensions (not the raw
        // applicationContext.getBeansOfType) because the manager surfaces its buttons via
        // ExtensionProvider<LoginFormInfo> — bypassing GeoServerExtensions' static extensionsCache that would
        // otherwise hide dynamically-registered singletons. See OAuth2LoginButtonManager's javadoc on the cache trap.
        List<LoginFormInfo> infos =
                org.geoserver.platform.GeoServerExtensions.extensions(LoginFormInfo.class, applicationContext).stream()
                        .filter(i -> i.getComponentClass() != null
                                && OAuth2LoginAuthProviderPanel.class
                                        .getName()
                                        .equals(i.getComponentClass().getName()))
                        .toList();
        assertNotNull(infos);
        long keycloakBeans = countButtonsForFilter(infos, "keycloak-prod");
        long auth0Beans = countButtonsForFilter(infos, "auth0-staging");
        assertEquals("expected exactly one LoginFormInfo for keycloak-prod", 1L, keycloakBeans);
        assertEquals("expected exactly one LoginFormInfo for auth0-staging", 1L, auth0Beans);
    }

    /**
     * Identifies which OIDC filter a {@link LoginFormInfo} corresponds to by inspecting its scoped registration ID
     * embedded in the loginPath ({@code /web/oauth2/authorization/<filterName>__<provider>}). Used by the multi-filter
     * tests to count buttons per filter without depending on a dedicated {@code filterName} field on the bean.
     */
    private static long countButtonsForFilter(List<LoginFormInfo> infos, String filterName) {
        String marker = "/" + filterName + "__";
        return infos.stream()
                .filter(i -> i.getLoginPath() != null && i.getLoginPath().contains(marker))
                .count();
    }

    /**
     * Three OIDC filters round out the multi-filter coverage. Beyond proving that the dynamic registry scales beyond
     * two, this case verifies that the rendered HTML keeps every login path distinct (no shared / overlapping login
     * paths between filters).
     */
    @Test
    public void testThreeOidcFiltersRenderThreeDistinctLoginButtons() throws Exception {
        List<String> filterNames = Arrays.asList("keycloak-prod", "auth0-staging", "entra-tenant");
        saveOidcFiltersAndAddToWebChain(filterNames);

        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();

        for (String name : filterNames) {
            assertTrue(
                    name + " button missing — expected " + expectedScopedLoginAnchor(name),
                    html.contains(expectedScopedLoginAnchor(name)));
        }
        // Defence in depth — the un-scoped path that the legacy static-bean design produced must NOT appear.
        // If this assertion ever fires after a future refactor, somebody has reintroduced the collapse-to-one-button
        // bug.
        assertFalse(
                "un-scoped /web/oauth2/authorization/oidc must not appear in rendered HTML",
                html.contains("href=\"http://localhost/context/web/oauth2/authorization/oidc\""));
    }

    /**
     * Failure-path UX regression coverage: when OAuth2 / OIDC authentication fails (bad client secret, invalid token,
     * state mismatch, IdP refusing the code exchange, ...) the user must land somewhere they can recover from —
     * specifically the GeoServer home page where the standard login form is available.
     *
     * <p>Before the fix, Spring's default failure handler redirected to {@code /login?error}, which has no handler in
     * the GeoServer webapp (no Wicket page mount, no servlet, no static resource). Tomcat's default servlet then served
     * the path back with no Content-Type and browsers downloaded an empty file named {@code login} — confusing UX
     * reported in the wild by Dorset County Council during a Keycloak integration with stale client credentials.
     *
     * <p>This test exercises the failure path without needing a real IdP: it hits the OAuth2 callback URL with a fake
     * {@code code} + {@code state} that won't match any stored {@code OAuth2AuthorizationRequest}, which makes Spring's
     * filter invoke the failure handler immediately at state validation — the exact same code path a real
     * token-exchange failure follows. Runs without Docker; complements the Keycloak-container regression test in
     * {@code KeyCloakMultiFilterIntegrationTest.testOAuth2FailureRedirectsToWebHomeNotLoginError}.
     */
    @Test
    public void testOAuth2FailureRedirectsToWebHomeNotLoginError() throws Exception {
        // Reuse the existing single-filter setup helper — same OIDC filter the existing render tests use.
        activateOidcFilterWithEnabledState(true);

        MockHttpServletRequest req = createRequest("web/login/oauth2/code/openidconnect__oidc");
        req.setParameter("code", "fake-authorization-code");
        req.setParameter("state", "fake-state-no-match");

        MockHttpServletResponse resp = executeOnSecurityFilters(req);

        // Spring's failure handler must redirect, not produce an opaque body that browsers turn into a download.
        assertEquals("expected 302 redirect from the failure handler", 302, resp.getStatus());
        String location = resp.getHeader("Location");
        assertNotNull("failure-handler redirect must include a Location header", location);

        // The fix: must NOT target Spring's default /login?error.
        assertFalse(
                "failure redirect must not target /login?error (no handler in GeoServer webapp, browsers download an empty file): "
                        + location,
                location.endsWith("/login?error") || location.endsWith("/login") || location.contains("/login?"));

        // Positive assertion: must land somewhere under /web/ where the Wicket admin can render the login form.
        assertTrue(
                "failure redirect must land somewhere under /web/ — got " + location,
                location.endsWith("/web/")
                        || location.endsWith("/web")
                        || location.contains("/web/")
                        || location.contains("/web?"));
    }

    /**
     * Mirrors {@code KeyCloakIntegrationTest.executeOnSecurityFilters}; copied locally to keep this test Docker-free.
     * Drives the request through {@link GeoServerSecurityFilterChainProxy} so Spring's OAuth2 filters activate exactly
     * as they would in production.
     */
    private static MockHttpServletResponse executeOnSecurityFilters(MockHttpServletRequest request)
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

    /**
     * A filter that is configured (saved + {@code oidcEnabled=true}) but NOT bound to the {@code /web/**} chain must
     * not render a button — clicking such a button would 404 on the OAuth2 authorization request endpoint because no
     * filter is mounted to handle it. This is guaranteed by
     * {@link org.geoserver.web.security.oauth2.login.OAuth2LoginButtonManager#sweepOAuth2Filters()}: it only registers
     * a {@link LoginFormInfo} singleton for OIDC filters that appear in at least one request filter chain, and tears
     * down singletons for filters that have left every chain on the next configuration change.
     */
    @Test
    public void testFilterNotInChainDoesNotRenderButton() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        // Filter "in-chain" is configured AND added to /web/**; "off-chain" is configured but never bound.
        manager.saveFilter(newOidcFilterConfig("in-chain"));
        manager.saveFilter(newOidcFilterConfig("off-chain"));

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain web = chain.getRequestChainByName("web");
        web.setFilterNames("in-chain", "anonymous");
        manager.saveSecurityConfig(config);

        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();

        assertTrue("in-chain button must render", html.contains(expectedScopedLoginAnchor("in-chain")));
        assertFalse(
                "off-chain button must NOT render — filter is configured but not bound to /web/**",
                html.contains(expectedScopedLoginAnchor("off-chain")));
    }

    /**
     * Layout regression: every rendered OIDC button must be wrapped in a {@code <div class="gs-login-external-links">},
     * because the user-dropdown CSS in {@code theme/css/geoserver.css} targets that exact class to lay multiple buttons
     * out horizontally (inline-flex) and centers the group via {@code .gs-user-dropdown-info ul { text-align: center
     * }}.
     *
     * <p>Without those wrappers — e.g. if a refactor of {@link org.geoserver.web.GeoServerBasePage}'s template ever
     * moved the {@code class="gs-login-external-links"} attribute off the {@code wicket:id="loginExternalLinks"} div —
     * multiple buttons would stack vertically (block-level anchors) and the centered, side-by-side layout would
     * silently break. This is the kind of regression that escapes pure unit tests because the markup is valid; this
     * test guards against it by asserting both the wrapper class and one wrapper per filter.
     */
    @Test
    public void testMultipleButtonsAreWrappedInGsLoginExternalLinksDivs() throws Exception {
        List<String> filterNames = Arrays.asList("keycloak-prod", "auth0-staging");
        saveOidcFiltersAndAddToWebChain(filterNames);

        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();

        // One wrapper per button — Wicket repeats the template element via the loginExternalLinks listview, so we
        // get N siblings rather than one wrapper around N anchors. Either layout works as long as the CSS rules
        // match what the template actually emits.
        int wrapperCount = countOccurrences(html, "class=\"gs-login-external-links\"");
        assertEquals("expected one .gs-login-external-links wrapper per OIDC filter", filterNames.size(), wrapperCount);

        // Each wrapper must directly enclose the scoped anchor — verify the (wrapper, anchor) pairing rather than
        // just total counts so a hypothetical regression that emits the wrapper around the wrong element fails here.
        for (String name : filterNames) {
            String anchor = expectedScopedLoginAnchor(name);
            int anchorIdx = html.indexOf(anchor);
            assertTrue(name + " anchor missing", anchorIdx > 0);
            int wrapperIdx = html.lastIndexOf("class=\"gs-login-external-links\"", anchorIdx);
            assertTrue(
                    name + " anchor is not preceded by a .gs-login-external-links wrapper",
                    wrapperIdx > 0 && wrapperIdx < anchorIdx);
        }
    }

    /**
     * Auto-enablement: when an admin saves a brand-new OIDC filter through the security manager <em>after</em> the
     * application has finished bootstrapping, the {@code OAuth2LoginButtonManager} must register a matching login
     * button into its internal registry — proof that no JVM restart is required to make the button available.
     *
     * <p>This guards the {@link org.geoserver.security.SecurityManagerListener} hook on the
     * {@link org.geoserver.web.security.oauth2.login.OAuth2LoginButtonManager}. Before that hook,
     * {@link GeoServerSecurityManager#saveFilter} on a fresh filter (no id yet) did <em>not</em> fire any change
     * notification, so the filter never went through the builder's {@code build()} path and never published the
     * {@code OAuth2LoginButtonEnablementEvent} the button manager listens for — buttons only appeared after a JVM
     * restart. With the listener wired in, the sweep loads every OAuth2 filter via the provider's {@code createFilter}
     * path, the builder fires the enable events, and the manager records the matching {@link LoginFormInfo} in its
     * registry.
     *
     * <p>This test asserts on the manager's registry rather than rendered HTML because the Wicket test harness wires
     * the security manager and the button manager into adjacent application contexts; the existing
     * {@code testTwoOidcFiltersRenderTwoDistinctLoginButtons} covers HTML rendering for the same scenario when both
     * filters are saved in a single batch. The unit tests in {@code OAuth2LoginButtonManagerTest.handlePostChanged_*}
     * cover the listener wiring in isolation. What this test adds is the seam between them: after a fired notification,
     * the registry reflects every saved OIDC filter — including filters saved post-bootstrap.
     */
    @Test
    public void testSavingNewOidcFilterAutoRegistersButtonInRegistry() throws Exception {
        // 1. Baseline: save + bind filter1 (keycloak-prod). The build path runs as part of the chain rebuild and
        //    publishes the enable event; the manager picks it up and registers button1 as a LoginFormInfo bean.
        saveOidcFiltersAndAddToWebChain(Arrays.asList("keycloak-prod"));

        long baselineKeycloakBeans = countButtonsForFilter(oauth2LoginFormInfos(), "keycloak-prod");
        assertEquals(
                "baseline registry must contain exactly one LoginFormInfo for keycloak-prod after the first save",
                1L,
                baselineKeycloakBeans);

        // 2. Save filter2 (auth0-staging) AFTER bootstrap and add it to the chain. Without the listener hook, this
        //    filter would never be loaded through createFilter again, never publish its enable event, and never
        //    appear in the registry until a container restart.
        GeoServerSecurityManager manager = getSecurityManager();
        manager.saveFilter(newOidcFilterConfig("auth0-staging"));
        SecurityManagerConfig cfg = manager.getSecurityConfig();
        RequestFilterChain web = cfg.getFilterChain().getRequestChainByName("web");
        web.setFilterNames("keycloak-prod", "auth0-staging", "anonymous");
        manager.saveSecurityConfig(cfg);

        // 3. Drive handlePostChanged on the live manager — same call SecurityManager fires via fireChanged().
        //    The sweep must rerun the build path for every saved OAuth2 filter and re-register the buttons.
        org.geoserver.web.security.oauth2.login.OAuth2LoginButtonManager buttonMgr =
                applicationContext.getBean(org.geoserver.web.security.oauth2.login.OAuth2LoginButtonManager.class);
        buttonMgr.handlePostChanged(manager);

        // 4. Both filters must be visible via the standard bean-by-type lookup GeoServerBasePage uses — proof that
        //    the listener-driven sweep registered the newly-saved filter without a container restart. This is the
        //    failure mode the user reported in the wild ("I have created 2 oidc filters but I still see only one
        //    button") and the contract the fix locks in.
        List<LoginFormInfo> infos = oauth2LoginFormInfos();
        long keycloakBeans = countButtonsForFilter(infos, "keycloak-prod");
        long auth0Beans = countButtonsForFilter(infos, "auth0-staging");
        assertEquals("keycloak-prod LoginFormInfo must remain after the post-save sweep", 1L, keycloakBeans);
        assertEquals(
                "auth0-staging LoginFormInfo must appear after the post-save sweep — no restart required",
                1L,
                auth0Beans);

        // 5. Each registered button's login path must encode the scoped registration id; the suffix
        //    /<filterName>__<provider> is what makes Spring's OAuth2 filter chain route the click to the right
        //    ClientRegistration. Use endsWith so the test does not couple itself to the exact base URI literal.
        LoginFormInfo keycloakInfo = infos.stream()
                .filter(i -> i.getLoginPath() != null && i.getLoginPath().contains("/keycloak-prod__"))
                .findFirst()
                .orElseThrow();
        LoginFormInfo auth0Info = infos.stream()
                .filter(i -> i.getLoginPath() != null && i.getLoginPath().contains("/auth0-staging__"))
                .findFirst()
                .orElseThrow();
        assertTrue(
                "keycloak login path must include the scoped registration id — got " + keycloakInfo.getLoginPath(),
                keycloakInfo.getLoginPath().endsWith("/oauth2/authorization/keycloak-prod__oidc"));
        assertTrue(
                "auth0 login path must include the scoped registration id — got " + auth0Info.getLoginPath(),
                auth0Info.getLoginPath().endsWith("/oauth2/authorization/auth0-staging__oidc"));
    }

    /**
     * Mirrors what {@code GeoServerBasePage} does when deciding which buttons to render. Goes through
     * {@link org.geoserver.platform.GeoServerExtensions} so the manager's {@code ExtensionProvider<LoginFormInfo>}
     * contribution is included — the raw {@code applicationContext.getBeansOfType} would miss it.
     */
    private List<LoginFormInfo> oauth2LoginFormInfos() {
        return org.geoserver.platform.GeoServerExtensions.extensions(LoginFormInfo.class, applicationContext).stream()
                .filter(i -> i.getComponentClass() != null
                        && OAuth2LoginAuthProviderPanel.class
                                .getName()
                                .equals(i.getComponentClass().getName()))
                .toList();
    }

    /**
     * Helper: count non-overlapping occurrences of {@code needle} in {@code haystack}. Used by markup-shape assertions
     * that need exact wrapper counts.
     */
    private static int countOccurrences(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isEmpty()) {
            return 0;
        }
        int count = 0;
        int from = 0;
        while (true) {
            int idx = haystack.indexOf(needle, from);
            if (idx < 0) {
                return count;
            }
            count++;
            from = idx + needle.length();
        }
    }
}
