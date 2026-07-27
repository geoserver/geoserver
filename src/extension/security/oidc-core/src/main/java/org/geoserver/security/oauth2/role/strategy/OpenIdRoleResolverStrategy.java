/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.role.strategy;

import java.util.List;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.config.OpenIdRoleSource;
import org.geoserver.security.oauth2.role.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;

/**
 * Extension-point strategy that resolves the logged-in user's <em>raw</em> role names from a single
 * {@link OpenIdRoleSource}. The owning {@link GeoServerOAuth2RoleResolver} dispatches by {@link #getRoleSource()} — no
 * central switch — and centrally applies role-converter mapping and the {@code ROLE_AUTHENTICATED} marker after the
 * strategy returns.
 *
 * <h2>Adding a new IdP source</h2>
 *
 * <ol>
 *   <li>Add the enum value to {@link OpenIdRoleSource}.
 *   <li>Implement this interface and return the new enum value from {@link #getRoleSource()}.
 *   <li>Register the implementation as a Spring bean in your module's {@code applicationContext.xml}. Spring-registered
 *       strategies override any built-in default that maps to the same {@link OpenIdRoleSource}.
 * </ol>
 *
 * <p>Implementations are typically stateless aside from {@link java.util.function.Supplier}-style fields used to inject
 * mocks in tests. The resolver creates one strategy instance per filter (the built-in defaults are constructed
 * directly), so per-filter state is fine.
 */
public interface OpenIdRoleResolverStrategy {

    /** The role-source enum value this strategy handles. */
    OpenIdRoleSource getRoleSource();

    /**
     * Resolve the raw external role names for this user/request. The caller applies role-converter mapping and the
     * {@code ROLE_AUTHENTICATED} marker; implementations should NOT do either.
     *
     * @return external role names (never {@code null}); empty list on lookup failure
     */
    List<String> resolveRoleNames(OAuth2ResolverParam param, GeoServerOAuth2LoginFilterConfig config);
}
