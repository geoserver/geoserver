/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.login;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_GIT_HUB;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_GOOGLE;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilterBuilder.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;
import static org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent.disableButtonEvent;
import static org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent.enableButtonEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.SecurityManagerListener;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent;
import org.geoserver.web.LoginFormInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Tests for {@link OAuth2LoginButtonManager}'s per-filter-instance dynamic-registration behavior.
 *
 * <p>The manager creates one {@link LoginFormInfo} singleton per active scoped registration ID (one per filter +
 * provider combination), registers it with the application's bean factory so
 * {@link org.geoserver.web.GeoServerBasePage}'s by-type lookup picks it up, and destroys it on the matching disable
 * event. Multiple filters of the same provider type produce multiple independent buttons, each pointing at its own
 * scoped authorization endpoint.
 */
@RunWith(MockitoJUnitRunner.class)
public class OAuth2LoginButtonManagerTest {

    @Mock
    private DefaultListableBeanFactory beanFactory;

    private OAuth2LoginButtonManager sut;

    @Before
    public void setUp() {
        sut = new OAuth2LoginButtonManager();
        sut.setBeanFactory(beanFactory);
    }

    private static String scopedRegId(String filterName, String baseRegId) {
        return GeoServerOAuth2ClientRegistrationId.scopedRegId(filterName, baseRegId);
    }

    private static String expectedLoginPath(String scopedRegId) {
        return "/" + DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + scopedRegId;
    }

    /**
     * Convenience: wire {@code securityManager.getSecurityConfig().getFilterChain().getRequestChains()} to advertise a
     * single request chain whose member filter names are {@code inChainFilterNames}. The sweep's chain-membership check
     * walks this exact graph.
     */
    private static void stubInChainFilterNames(GeoServerSecurityManager securityManager, String... inChainFilterNames) {
        RequestFilterChain chain = org.mockito.Mockito.mock(RequestFilterChain.class);
        when(chain.getFilterNames()).thenReturn(Arrays.asList(inChainFilterNames));
        GeoServerSecurityFilterChain filterChain = org.mockito.Mockito.mock(GeoServerSecurityFilterChain.class);
        Collection<RequestFilterChain> chains = List.of(chain);
        when(filterChain.getRequestChains()).thenReturn((List<RequestFilterChain>) chains);
        SecurityManagerConfig cfg = org.mockito.Mockito.mock(SecurityManagerConfig.class);
        when(cfg.getFilterChain()).thenReturn(filterChain);
        when(securityManager.getSecurityConfig()).thenReturn(cfg);
    }

    @Test
    public void singleFilter_enableRegisters_disableRemoves() {
        String lFilterName = "my-filter";
        String lScopedOidc = scopedRegId(lFilterName, REG_ID_OIDC);

        // when: enable fires for the filter
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScopedOidc));

        // then: the manager's registry holds a LoginFormInfo with the expected shape. The manager now exposes its
        // contents via ExtensionProvider<LoginFormInfo>, so GeoServerExtensions.extensions(LoginFormInfo.class, ctx)
        // picks them up on every call — no bean-factory singleton registration, no extension-cache invalidation
        // headaches.
        LoginFormInfo registered = sut.getRegisteredButtons().get(lScopedOidc);
        assertNotNull(registered);
        assertEquals(expectedLoginPath(lScopedOidc), registered.getLoginPath());
        assertTrue(
                "login path must end with the scoped registration id",
                registered.getLoginPath().endsWith("/" + lScopedOidc));
        assertTrue(
                "scoped registration id must start with the filter name", lScopedOidc.startsWith(lFilterName + "__"));
        assertEquals("openid.png", registered.getIcon());
        assertEquals("GET", registered.getMethod());
        assertTrue(registered.isEnabled());
        assertTrue(registered.isJustUseExternalLink());
        assertEquals("openidconnect.login.button.title", registered.getTitleKey());
        assertEquals("OAuth2LoginAuthProviderPanel.oidcDescription", registered.getDescriptionKey());

        // and: ExtensionProvider exposes it under LoginFormInfo
        assertEquals(LoginFormInfo.class, sut.getExtensionPoint());
        assertEquals(1, sut.getExtensions(LoginFormInfo.class).size());

        // when: disable fires for the same filter
        sut.enablementChanged(disableButtonEvent(this, REG_ID_OIDC, lScopedOidc));

        // then: the registry is empty and the ExtensionProvider returns nothing
        assertNull(sut.getRegisteredButtons().get(lScopedOidc));
        assertTrue(sut.getExtensions(LoginFormInfo.class).isEmpty());
    }

    @Test
    public void twoFiltersSameProvider_eachGetsOwnIndependentButton() {
        String lFilter1 = "keycloak-prod";
        String lFilter2 = "auth0-staging";
        String lScoped1 = scopedRegId(lFilter1, REG_ID_OIDC);
        String lScoped2 = scopedRegId(lFilter2, REG_ID_OIDC);

        // when: both filters enable OIDC
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScoped1));
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScoped2));

        // then: two distinct entries are tracked, each with its own scoped login path
        assertEquals(
                expectedLoginPath(lScoped1),
                sut.getRegisteredButtons().get(lScoped1).getLoginPath());
        assertEquals(
                expectedLoginPath(lScoped2),
                sut.getRegisteredButtons().get(lScoped2).getLoginPath());
        // Filter name embedded in the loginPath via the scoped registration id (`<filterName>__<provider>`).
        assertTrue(
                sut.getRegisteredButtons().get(lScoped1).getLoginPath().endsWith("/" + lFilter1 + "__" + REG_ID_OIDC));
        assertTrue(
                sut.getRegisteredButtons().get(lScoped2).getLoginPath().endsWith("/" + lFilter2 + "__" + REG_ID_OIDC));
        // ExtensionProvider returns both as a snapshot — the path GeoServerBasePage uses.
        assertEquals(2, sut.getExtensions(LoginFormInfo.class).size());

        // when: only the first filter disables
        sut.enablementChanged(disableButtonEvent(this, REG_ID_OIDC, lScoped1));

        // then: only the first entry is removed; the second one survives untouched
        assertNull(sut.getRegisteredButtons().get(lScoped1));
        assertNotNull(sut.getRegisteredButtons().get(lScoped2));
        assertEquals(1, sut.getExtensions(LoginFormInfo.class).size());
    }

    @Test
    public void threeOidcFilters_allProduceDistinctButtonsWithUniqueLoginPaths() {
        // Beyond proving "two filters → two buttons", this case guards against any future refactor that
        // accidentally caps the registry, collapses entries sharing a base registration ID, or trims paths
        // back to the un-scoped form.
        String[] filterNames = {"keycloak-prod", "auth0-staging", "entra-tenant"};

        for (String filterName : filterNames) {
            sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, scopedRegId(filterName, REG_ID_OIDC)));
        }

        // Three independent entries tracked, each with its own scoped login path.
        for (String filterName : filterNames) {
            String scopedId = scopedRegId(filterName, REG_ID_OIDC);
            LoginFormInfo info = sut.getRegisteredButtons().get(scopedId);
            assertNotNull("expected a LoginFormInfo for " + scopedId, info);
            assertEquals(expectedLoginPath(scopedId), info.getLoginPath());
            assertTrue(
                    "loginPath must end with /<filterName>__<provider>",
                    info.getLoginPath().endsWith("/" + filterName + "__" + REG_ID_OIDC));
            assertEquals("openid.png", info.getIcon());
        }
        assertEquals(filterNames.length, sut.getExtensions(LoginFormInfo.class).size());

        // Login paths are pairwise distinct — no two filters share a login URL.
        java.util.Set<String> loginPaths = new java.util.HashSet<>();
        for (String filterName : filterNames) {
            loginPaths.add(sut.getRegisteredButtons()
                    .get(scopedRegId(filterName, REG_ID_OIDC))
                    .getLoginPath());
        }
        assertEquals("each filter must have a unique login path", filterNames.length, loginPaths.size());

        // Bean ids are pairwise distinct too.
        java.util.Set<String> beanIds = new java.util.HashSet<>();
        for (String filterName : filterNames) {
            beanIds.add(sut.getRegisteredButtons()
                    .get(scopedRegId(filterName, REG_ID_OIDC))
                    .getId());
        }
        assertEquals("each filter must have a unique bean id", filterNames.length, beanIds.size());

        // Disabling the middle filter must leave the other two intact.
        String middle = scopedRegId("auth0-staging", REG_ID_OIDC);
        sut.enablementChanged(disableButtonEvent(this, REG_ID_OIDC, middle));
        assertNull(sut.getRegisteredButtons().get(middle));
        assertNotNull(sut.getRegisteredButtons().get(scopedRegId("keycloak-prod", REG_ID_OIDC)));
        assertNotNull(sut.getRegisteredButtons().get(scopedRegId("entra-tenant", REG_ID_OIDC)));
    }

    @Test
    public void differentProvidersOnDifferentFilters_eachGetsOwnIconAndPath() {
        String lScopedGoogle = scopedRegId("google-corp", REG_ID_GOOGLE);
        String lScopedGitHub = scopedRegId("github-org", REG_ID_GIT_HUB);
        String lScopedMs = scopedRegId("entra-tenant", REG_ID_MICROSOFT);
        String lScopedOidc = scopedRegId("keycloak", REG_ID_OIDC);

        sut.enablementChanged(enableButtonEvent(this, REG_ID_GOOGLE, lScopedGoogle));
        sut.enablementChanged(enableButtonEvent(this, REG_ID_GIT_HUB, lScopedGitHub));
        sut.enablementChanged(enableButtonEvent(this, REG_ID_MICROSOFT, lScopedMs));
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScopedOidc));

        assertEquals("google.png", sut.getRegisteredButtons().get(lScopedGoogle).getIcon());
        assertEquals("github.png", sut.getRegisteredButtons().get(lScopedGitHub).getIcon());
        assertEquals("microsoft.png", sut.getRegisteredButtons().get(lScopedMs).getIcon());
        assertEquals("openid.png", sut.getRegisteredButtons().get(lScopedOidc).getIcon());

        // Google / GitHub / Microsoft are icon-only (titleKey empty); OIDC carries an i18n label.
        assertEquals("", sut.getRegisteredButtons().get(lScopedGoogle).getTitleKey());
        assertEquals("", sut.getRegisteredButtons().get(lScopedGitHub).getTitleKey());
        assertEquals("", sut.getRegisteredButtons().get(lScopedMs).getTitleKey());
        assertEquals(
                "openidconnect.login.button.title",
                sut.getRegisteredButtons().get(lScopedOidc).getTitleKey());
    }

    @Test
    public void repeatedEnable_isIdempotent_keepsExistingEntry() {
        String lScopedOidc = scopedRegId("my-filter", REG_ID_OIDC);

        // when: enable fires twice in a row (e.g. two consecutive filter rebuilds)
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScopedOidc));
        LoginFormInfo first = sut.getRegisteredButtons().get(lScopedOidc);
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScopedOidc));
        LoginFormInfo second = sut.getRegisteredButtons().get(lScopedOidc);

        // then: the entry is identical — bean identity stays stable for any consumer holding a reference, and
        // the ExtensionProvider snapshot returns one and only one button for this scoped id.
        assertSame(first, second);
        assertEquals(1, sut.getExtensions(LoginFormInfo.class).size());
    }

    @Test
    public void disableForNeverEnabledScopedId_isNoOp() {
        String lScopedOidc = scopedRegId("ghost-filter", REG_ID_OIDC);

        // when: a disable event arrives for a scoped ID we never saw enabled
        sut.enablementChanged(disableButtonEvent(this, REG_ID_OIDC, lScopedOidc));

        // then: registry stays empty (and unregisterButton short-circuits when there is nothing to remove)
        assertNull(sut.getRegisteredButtons().get(lScopedOidc));
        assertTrue(sut.getExtensions(LoginFormInfo.class).isEmpty());
    }

    @Test
    public void filterNameWithoutScopedSeparator_treatedAsLiteralFilterName() {
        // Defensive: scoped IDs are always of the form <filterName>__<baseRegId> in practice, but if
        // the convention is ever broken the manager should still register a button rather than crash.
        String rawId = "no-separator-here";
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, rawId));

        LoginFormInfo info = sut.getRegisteredButtons().get(rawId);
        assertNotNull(info);
        // The loginPath always echoes the raw scoped-id (no parsing surprises if the separator is absent).
        assertTrue(
                "loginPath must end with the raw scoped id when the separator is absent",
                info.getLoginPath().endsWith("/" + rawId));
        // filterNameFromScopedRegId returns the whole id when no separator is found — defensive parse path.
        assertEquals(rawId, OAuth2LoginButtonManager.filterNameFromScopedRegId(rawId));
    }

    @Test
    public void missingBeanFactory_eventsAreSwallowed() {
        OAuth2LoginButtonManager bare = new OAuth2LoginButtonManager();
        // intentionally no setBeanFactory call

        // when: an event arrives
        bare.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, scopedRegId("any", REG_ID_OIDC)));

        // then: nothing crashes and the registry stays empty
        assertTrue(bare.getRegisteredButtons().isEmpty());
    }

    @Test
    public void nullScopedRegistrationId_isIgnored() {
        OAuth2LoginButtonEnablementEvent nullScoped =
                new OAuth2LoginButtonEnablementEvent(this, true, REG_ID_OIDC, null);
        sut.enablementChanged(nullScoped);
        assertTrue(
                "null scopedRegId must not produce a registered button",
                sut.getRegisteredButtons().isEmpty());
    }

    @Test
    public void filterClass_isAuthenticationFilter() {
        String lScopedOidc = scopedRegId("my-filter", REG_ID_OIDC);
        sut.enablementChanged(enableButtonEvent(this, REG_ID_OIDC, lScopedOidc));

        LoginFormInfo info = sut.getRegisteredButtons().get(lScopedOidc);
        assertNotNull(info);
        assertEquals(
                "org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter",
                info.getFilterClass().getName());
    }

    // ── SecurityManagerListener: auto-enablement on save ─────────────────────
    //
    // These tests cover the "save filter → button appears without container restart" path.
    // After context refresh the manager must self-register as a SecurityManagerListener and
    // sweep existing filters; subsequent saves arrive via handlePostChanged which must trigger
    // the same sweep, forcing each OAuth2 filter through the createFilter path that publishes
    // the OAuth2LoginButtonEnablementEvent.

    @Test
    public void contextRefresh_registersListenerAndSweepsExistingFilters() throws Exception {
        GeoServerSecurityManager securityManager = org.mockito.Mockito.mock(GeoServerSecurityManager.class);
        TreeSet<String> filterNames = new TreeSet<>();
        filterNames.add("keycloak-prod");
        filterNames.add("auth0-staging");
        when(securityManager.listFilters(GeoServerOAuth2LoginAuthenticationFilter.class))
                .thenReturn(filterNames);
        when(beanFactory.getBean(GeoServerSecurityManager.class)).thenReturn(securityManager);
        // Both filters are bound to the web chain — eligible to render a button.
        stubInChainFilterNames(securityManager, "keycloak-prod", "auth0-staging");

        ContextRefreshedEvent event = new ContextRefreshedEvent(org.mockito.Mockito.mock(ApplicationContext.class));
        sut.onApplicationEvent(event);

        // The manager must register itself as a SecurityManagerListener so that subsequent
        // saves (via GeoServerSecurityManager#saveSecurityConfig) call back into handlePostChanged.
        verify(securityManager).addListener(same(sut));
        // Initial sweep: each in-chain OAuth2 filter must be loaded so the builder publishes the
        // enablement event the button manager listens for.
        verify(securityManager).loadFilter("keycloak-prod");
        verify(securityManager).loadFilter("auth0-staging");
    }

    @Test
    public void contextRefresh_skipsOffChainFiltersDuringSweep() throws Exception {
        GeoServerSecurityManager securityManager = org.mockito.Mockito.mock(GeoServerSecurityManager.class);
        TreeSet<String> filterNames = new TreeSet<>();
        filterNames.add("keycloak-prod");
        filterNames.add("orphan-filter"); // configured but not bound to any chain
        when(securityManager.listFilters(GeoServerOAuth2LoginAuthenticationFilter.class))
                .thenReturn(filterNames);
        when(beanFactory.getBean(GeoServerSecurityManager.class)).thenReturn(securityManager);
        // Only keycloak-prod is in the chain; orphan-filter is not — its button must NOT register.
        stubInChainFilterNames(securityManager, "keycloak-prod");

        sut.onApplicationEvent(new ContextRefreshedEvent(org.mockito.Mockito.mock(ApplicationContext.class)));

        verify(securityManager).loadFilter("keycloak-prod");
        verify(securityManager, never()).loadFilter("orphan-filter");
    }

    @Test
    public void contextRefresh_isIdempotent_listenerRegisteredOnlyOnce() throws Exception {
        GeoServerSecurityManager securityManager = org.mockito.Mockito.mock(GeoServerSecurityManager.class);
        when(securityManager.listFilters(GeoServerOAuth2LoginAuthenticationFilter.class))
                .thenReturn(new TreeSet<>());
        when(beanFactory.getBean(GeoServerSecurityManager.class)).thenReturn(securityManager);
        stubInChainFilterNames(securityManager); // empty chain

        ContextRefreshedEvent event = new ContextRefreshedEvent(org.mockito.Mockito.mock(ApplicationContext.class));

        sut.onApplicationEvent(event);
        sut.onApplicationEvent(event);
        sut.onApplicationEvent(event);

        // A re-refresh of the context (which can happen in test harnesses or after parent-context
        // shuffling) must not re-register the listener — otherwise GeoServerSecurityManager would
        // fire handlePostChanged N times per save.
        verify(securityManager, times(1)).addListener(any(SecurityManagerListener.class));
    }

    @Test
    public void handlePostChanged_sweepsAllOAuth2FiltersThroughLoadFilter() throws Exception {
        // Pre-condition: simulate that the listener is already wired up via onApplicationEvent.
        GeoServerSecurityManager securityManager = org.mockito.Mockito.mock(GeoServerSecurityManager.class);
        when(securityManager.listFilters(GeoServerOAuth2LoginAuthenticationFilter.class))
                .thenReturn(new TreeSet<>()); // initial sweep finds nothing
        when(beanFactory.getBean(GeoServerSecurityManager.class)).thenReturn(securityManager);
        stubInChainFilterNames(securityManager); // empty chain at boot

        sut.onApplicationEvent(new ContextRefreshedEvent(org.mockito.Mockito.mock(ApplicationContext.class)));

        // Now simulate an admin saving two new filters AND adding them to the chain — handlePostChanged
        // is the call that runs once GeoServerSecurityManager#saveSecurityConfig calls fireChanged().
        TreeSet<String> newlySaved = new TreeSet<>();
        newlySaved.add("keycloak-prod");
        newlySaved.add("auth0-staging");
        when(securityManager.listFilters(GeoServerOAuth2LoginAuthenticationFilter.class))
                .thenReturn(newlySaved);
        stubInChainFilterNames(securityManager, "keycloak-prod", "auth0-staging");

        sut.handlePostChanged(securityManager);

        // Each freshly-saved filter must be loaded through the provider's createFilter path,
        // which is where the enablement event is published.
        verify(securityManager).loadFilter("keycloak-prod");
        verify(securityManager).loadFilter("auth0-staging");
    }

    @Test
    public void handlePostChanged_loadFilterFailureForOneNameDoesNotSkipTheRest() throws Exception {
        GeoServerSecurityManager securityManager = org.mockito.Mockito.mock(GeoServerSecurityManager.class);
        TreeSet<String> filters = new TreeSet<>();
        filters.add("broken-filter");
        filters.add("healthy-filter");
        when(securityManager.listFilters(GeoServerOAuth2LoginAuthenticationFilter.class))
                .thenReturn(filters);
        when(beanFactory.getBean(GeoServerSecurityManager.class)).thenReturn(securityManager);
        // Both filters bound to the chain so the sweep tries to load each.
        stubInChainFilterNames(securityManager, "broken-filter", "healthy-filter");
        // First call throws — second must still happen.
        when(securityManager.loadFilter("broken-filter")).thenThrow(new RuntimeException("simulated bad config"));

        sut.onApplicationEvent(new ContextRefreshedEvent(org.mockito.Mockito.mock(ApplicationContext.class)));

        // Defence-in-depth: a single bad filter (e.g. half-saved config) must not block the
        // remaining OIDC filters from getting their buttons registered.
        verify(securityManager).loadFilter("broken-filter");
        verify(securityManager).loadFilter("healthy-filter");
    }

    @Test
    public void handlePostChanged_beforeContextRefresh_isNoOp() {
        // If a save event somehow reaches us before the context-refresh hook ran (the security
        // manager reference is then null), the manager must not crash and must not attempt to
        // reach into a null security manager.
        GeoServerSecurityManager securityManager = org.mockito.Mockito.mock(GeoServerSecurityManager.class);

        sut.handlePostChanged(securityManager); // not called via the wiring; should be silent

        // No interaction at all on the passed manager — we sweep against our cached field, not the arg.
        org.mockito.Mockito.verifyNoInteractions(securityManager);
    }
}
