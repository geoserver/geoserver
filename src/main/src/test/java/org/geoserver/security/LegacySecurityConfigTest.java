/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import org.geoserver.config.util.XStreamUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.SecurityConfigDiagnostics.DisabledComponent;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.filter.DisabledSecurityFilter;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Verifies the <em>core</em> generic tolerance: a persisted security filter whose configuration cannot be loaded (for
 * example because it was created by a community plugin that is not installed) must not prevent GeoServer from starting.
 * It is disabled, recorded in {@link GeoServerSecurityManager#getConfigDiagnostics()} and removed from the request
 * filter chains. This holds with no OIDC/community plugin on the classpath; precise per-plugin recognition (alias +
 * originating plugin) is tested separately in the OIDC module.
 */
public class LegacySecurityConfigTest extends GeoServerSecurityTestSupport {

    /** Copies a fixture config.xml into the data directory's security tree. */
    private void install(String fixture, String securityPath) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("legacy/" + fixture)) {
            assertNotNull("Missing test fixture: legacy/" + fixture, in);
            Resource target = getResourceLoader().get(securityPath);
            try (OutputStream out = target.out()) {
                in.transferTo(out);
            }
        }
    }

    private static DisabledComponent disabledByName(GeoServerSecurityManager secMgr, String name) {
        return secMgr.getConfigDiagnostics().getDisabledComponents().stream()
                .filter(c -> name.equals(c.name()))
                .findFirst()
                .orElse(null);
    }

    @Test
    public void testUnloadableFilterIsDisabledNotFatal() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        // a filter from a plugin this GeoServer does not have: its config.xml root is an alias core cannot resolve
        install("foreign-filter.xml", "security/filter/acme/config.xml");

        // reload() runs the same code path as startup; it must NOT throw
        secMgr.reload();

        DisabledComponent acme = disabledByName(secMgr, "acme");
        assertNotNull("the unloadable filter should be reported as disabled", acme);
        assertEquals(SecurityConfigDiagnostics.ComponentType.AUTHENTICATION_FILTER, acme.type());
        // the generic net recovers the root element (alias) but does not know the originating plugin
        assertEquals("acmeCustomAuthentication", acme.alias());
        assertNull(acme.sourcePlugin());
    }

    @Test
    public void testManualMigrationClearsWarnings() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        install("foreign-filter.xml", "security/filter/acme/config.xml");
        secMgr.reload();
        assertFalse(secMgr.getConfigDiagnostics().isEmpty());

        // admin removes the obsolete configuration
        getResourceLoader().get("security/filter/acme").delete();
        secMgr.reload();

        assertTrue(
                "diagnostics should be empty after the obsolete filter is removed",
                secMgr.getConfigDiagnostics().isEmpty());
    }

    @Test
    public void testFailClosedWhenChainLosesAuthenticatorAndHasNoInterceptor() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();

        // a variable chain whose only authentication filter is the disabled one and which has no interceptor
        ServiceLoginFilterChain custom = new ServiceLoginFilterChain("/custom/**");
        custom.setName("customNoInterceptor");
        custom.setFilterNames("legacyFilter");
        custom.setInterceptorName(null);
        config.getFilterChain().getRequestChains().add(custom);

        secMgr.getConfigDiagnostics().clear();
        secMgr.stripDisabledFiltersFromChains(config, new TreeSet<>(List.of("legacyFilter")));

        RequestFilterChain after = config.getFilterChain().getRequestChainByName("customNoInterceptor");
        assertTrue(
                "a fail-closed deny filter must be injected",
                after.getFilterNames().contains(GeoServerSecurityFilterChain.DISABLED_FILTER));
        assertTrue(secMgr.getConfigDiagnostics().getAffectedChains().stream()
                .anyMatch(
                        a -> "customNoInterceptor".equals(a.chainName()) && a.lostAuthenticator() && a.accessDenied()));
    }

    @Test
    public void testNoDenyFilterWhenChainKeepsInterceptor() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();

        // the web chain has a security interceptor: even when it loses its only authentication filter, the interceptor
        // still enforces access control, so no deny filter is injected (it would otherwise break the chain)
        RequestFilterChain web =
                config.getFilterChain().getRequestChainByName(GeoServerSecurityFilterChain.WEB_CHAIN_NAME);
        web.setFilterNames("legacyFilter");

        secMgr.getConfigDiagnostics().clear();
        secMgr.stripDisabledFiltersFromChains(config, new TreeSet<>(List.of("legacyFilter")));

        RequestFilterChain webAfter =
                config.getFilterChain().getRequestChainByName(GeoServerSecurityFilterChain.WEB_CHAIN_NAME);
        assertFalse(webAfter.getFilterNames().contains(GeoServerSecurityFilterChain.DISABLED_FILTER));
        assertTrue(secMgr.getConfigDiagnostics().getAffectedChains().stream()
                .anyMatch(a -> GeoServerSecurityFilterChain.WEB_CHAIN_NAME.equals(a.chainName())
                        && a.lostAuthenticator()
                        && !a.accessDenied()));
    }

    /** Route dispatched requests through the real security filter chain so the fail-closed deny filter is exercised. */
    @Override
    protected List<Filter> getFilters() {
        return Arrays.asList(applicationContext.getBean(GeoServerSecurityFilterChainProxy.class));
    }

    /**
     * End-to-end fail-closed behaviour: a previously protected chain whose only authentication filter was created by an
     * uninstalled plugin loses that filter on startup and, having no security interceptor to otherwise enforce access,
     * has the {@link DisabledSecurityFilter} injected. An actual HTTP request matching that chain must then be denied
     * with {@code 403 Forbidden} carrying {@link DisabledSecurityFilter#DENY_MESSAGE}, rather than silently passing
     * through an open chain.
     */
    @Test
    public void testRequestToChainThatLostAuthenticatorIsDeniedWith403() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        // the foreign filter created by a plugin this GeoServer no longer has
        install("foreign-filter.xml", "security/filter/acme/config.xml");

        // a previously protected chain whose ONLY authentication filter is that foreign one, and which has no security
        // interceptor of its own (a constant chain): exactly the situation in which the strip must fail closed
        SecurityManagerConfig config = secMgr.loadSecurityConfig();
        ConstantFilterChain protectedChain = new ConstantFilterChain("/acmeprotected/**");
        protectedChain.setName("acmeProtected");
        protectedChain.getFilterNames().add("acme");
        // ahead of the catch-all chains so this specific pattern is the one that matches the request
        config.getFilterChain().getRequestChains().add(0, protectedChain);

        // persist the config directly, the way the old plugin would have written it: saveSecurityConfig() would reject
        // a chain referencing an unresolvable filter, but an old data directory on disk has no such guard
        XStreamUtils.xStreamPersist(
                secMgr.security().get(GeoServerSecurityManager.CONFIG_FILENAME), config, secMgr.globalPersister());

        // reload() runs the startup code path: it disables the foreign filter, strips it from the chain and, because
        // the
        // chain is now authenticator-less and interceptor-less, injects the fail-closed deny filter
        secMgr.reload();

        // sanity: the diagnostics record the chain as access-denied
        assertTrue(
                "the protected chain should be recorded as fail-closed (access denied)",
                secMgr.getConfigDiagnostics().getAffectedChains().stream()
                        .anyMatch(a ->
                                "acmeProtected".equals(a.chainName()) && a.lostAuthenticator() && a.accessDenied()));

        // drive a real request at the protected path through the security filter chain
        MockHttpServletRequest request = createRequest("/acmeprotected/whatever");
        request.setMethod("GET");
        MockHttpServletResponse response = dispatch(request);

        assertEquals(
                "a request to the chain that lost its authenticator must be denied",
                HttpServletResponse.SC_FORBIDDEN,
                response.getStatus());
        assertNotNull("a deny message should be present", response.getErrorMessage());
        assertTrue(
                "the 403 should carry the disabled-filter deny message, was: " + response.getErrorMessage(),
                response.getErrorMessage().contains(DisabledSecurityFilter.DENY_MESSAGE));
    }
}
