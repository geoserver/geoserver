/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;

/**
 * Small test helper for creating and (optionally) persisting authentication provider configs during controller tests.
 *
 * <p><strong>Enable/disable semantics:</strong> in GeoServer, a provider is considered <em>enabled</em> when its name
 * is present in {@code <authProviderNames>} within the global {@link SecurityManagerConfig}. This helper, when saving a
 * provider, will:
 *
 * <ol>
 *   <li>Persist the provider configuration to disk via
 *       {@link GeoServerSecurityManager#saveAuthenticationProvider(org.geoserver.security.config.SecurityAuthProviderConfig)}
 *   <li>Insert its name into {@code <authProviderNames>} (at the end, or at a requested position)
 *   <li>Persist the global config and call {@link GeoServerSecurityManager#reload()}
 * </ol>
 *
 * <p>On failure this class throws {@link AssertionError} with the original cause attached, which is ideal for use in
 * tests (you get a clean stack trace without sprinkling try/catch in your test methods).
 */
public class AuthenticationProviderHelper {

    /** Default user/group service name used by the stock username/password provider. */
    public static final String DEFAULT_USER_GROUP_SERVICE = "default";

    private final GeoServerSecurityManager securityManager;

    public AuthenticationProviderHelper(GeoServerSecurityManager securityManager) {
        this.securityManager = Objects.requireNonNull(securityManager, "securityManager must not be null");
    }

    /**
     * Builds a {@link UsernamePasswordAuthenticationProviderConfig} with the given name and the default user group
     * service. If {@code save} is true, the provider is persisted and appended at the end of the enabled order.
     *
     * @param name provider name (non-empty)
     * @param save whether to persist and enable the provider
     */
    public UsernamePasswordAuthenticationProviderConfig createUsernamePasswordAuthenticationProviderConfig(
            final String name, final boolean save) {

        return createUsernamePasswordAuthenticationProviderConfig(name, save, null);
    }

    /**
     * Builds a {@link UsernamePasswordAuthenticationProviderConfig} with the given name and default user group service.
     * If {@code save} is true, the provider is persisted and inserted into {@code <authProviderNames>} at
     * {@code position} (clamped to a valid range).
     *
     * @param name provider name (non-empty)
     * @param save whether to persist and enable the provider
     * @param position target position in the enabled order; when {@code null} the provider is appended
     */
    public UsernamePasswordAuthenticationProviderConfig createUsernamePasswordAuthenticationProviderConfig(
            final String name, final boolean save, final Integer position) {

        final String validatedName = requireNonBlank(name, "provider name");

        // 1) Build the config object (unsaved)
        final UsernamePasswordAuthenticationProviderConfig cfg = new UsernamePasswordAuthenticationProviderConfig();
        cfg.setName(validatedName);
        cfg.setUserGroupServiceName(DEFAULT_USER_GROUP_SERVICE);
        cfg.setClassName(UsernamePasswordAuthenticationProvider.class.getName());

        if (!save) {
            return cfg;
        }

        // 2) Persist config and enable it in the global order
        try {
            // Save provider config to disk
            securityManager.saveAuthenticationProvider(cfg);

            // Load global config, adjust order, then save it back
            final SecurityManagerConfig smc = securityManager.loadSecurityConfig();
            final List<String> order = new ArrayList<>(smc.getAuthProviderNames());

            // Ensure no duplicates, then insert at requested (clamped) position
            order.remove(cfg.getName());
            final int pos = (position == null) ? order.size() : clamp(position, 0, order.size());
            order.add(pos, cfg.getName());

            smc.getAuthProviderNames().clear();
            smc.getAuthProviderNames().addAll(order);

            securityManager.saveSecurityConfig(smc);
            securityManager.reload();

            return cfg;
        } catch (Exception e) {
            // Keep tests clean: surface as an assertion failure, preserving the cause/stack
            throw new AssertionError("Unexpected failure while creating/saving provider '" + name + "'", e);
        }
    }

    // ---------------------------------------------------------------------
    // internal utilities

    private static int clamp(int value, int minInclusive, int maxInclusiveExclusive) {
        if (value < minInclusive) return minInclusive;
        if (value > maxInclusiveExclusive) return maxInclusiveExclusive;
        return value;
    }

    private static String requireNonBlank(String s, String what) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(what + " must not be null or blank");
        }
        return s;
    }
}
