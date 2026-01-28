/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.toList;
import static org.geoserver.security.impl.GeoServerUser.ADMIN_USERNAME;
import static org.geoserver.security.impl.GeoServerUser.ROOT_USERNAME;
import static org.geoserver.security.jwtheaders.roles.JwtHeadersRolesExtractor.asStringList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.geoserver.security.jwtheaders.roles.RoleConverter;
import org.geoserver.security.jwtheaders.username.JwtHeaderUserNameExtractor;
import org.geoserver.security.oauth2.common.TokenIntrospector;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.StringUtils;

/**
 * Resource-server opaque token introspector that bridges RFC7662 introspection responses into GeoServer roles.
 *
 * <p>This component is used by Spring Security's opaque-token resource server support. It performs token introspection
 * using {@link TokenIntrospector} and then maps claims/scopes to {@link GeoServerRole}s.
 */
public final class GeoServerOAuth2OpaqueTokenIntrospector
        implements OpaqueTokenIntrospector, GeoServerOAuth2ClientRegistrationId {

    private static final Logger LOGGER = Logging.getLogger(GeoServerOAuth2OpaqueTokenIntrospector.class);

    private final TokenIntrospector delegate;
    private final GeoServerSecurityManager securityManager;
    private final GeoServerOAuth2LoginFilterConfig config;
    private final RoleConverter roleConverter;

    public GeoServerOAuth2OpaqueTokenIntrospector(
            TokenIntrospector delegate,
            GeoServerSecurityManager securityManager,
            GeoServerOAuth2LoginFilterConfig config) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.securityManager = Objects.requireNonNull(securityManager, "securityManager");
        this.config = Objects.requireNonNull(config, "config");

        JwtConfiguration jwtConfiguration = new JwtConfiguration();
        jwtConfiguration.setRoleConverterString(config.getRoleConverterString());
        jwtConfiguration.setOnlyExternalListedRoles(config.isOnlyExternalListedRoles());
        this.roleConverter = new RoleConverter(jwtConfiguration);
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BadOpaqueTokenException("Missing bearer token");
        }

        Map<String, Object> claims = delegate.introspectToken(token);
        if (claims == null || claims.isEmpty()) {
            throw new BadOpaqueTokenException("Empty introspection response");
        }

        if (!Boolean.TRUE.equals(claims.get("active"))) {
            throw new BadOpaqueTokenException("Token is not active");
        }

        String principal = resolvePrincipal(claims);
        if (!StringUtils.hasText(principal)) {
            principal = Optional.ofNullable(claimAsString(claims, "sub")).orElse("unknown");
        }

        // Avoid collisions with built-in administrator users.
        if (ADMIN_USERNAME.equalsIgnoreCase(principal) || ROOT_USERNAME.equalsIgnoreCase(principal)) {
            LOGGER.log(
                    Level.WARNING,
                    "Potentially harmful opaque-token principal '{0}' detected. Granting no roles.",
                    principal);
            return new DefaultOAuth2AuthenticatedPrincipal(principal, claims, List.of());
        }

        Collection<GeoServerRole> roles = resolveRoles(claims);
        roles = applyRoleServices(principal, roles);

        // IMPORTANT: DefaultOAuth2AuthenticatedPrincipal expects Collection<GrantedAuthority>
        // Collection<GeoServerRole> is not assignable due to generics invariance.
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(roles); // GeoServerRole implements GrantedAuthority

        return new DefaultOAuth2AuthenticatedPrincipal(principal, claims, authorities);
    }

    private String resolvePrincipal(Map<String, Object> claims) {
        // If exactly one provider is enabled, prefer that provider's configured username attribute.
        if (config.getActiveProviderCount() == 1) {
            String preferred = null;
            if (config.isOidcEnabled()) {
                preferred = config.getOidcUserNameAttribute();
            } else if (config.isMsEnabled()) {
                preferred = config.getMsUserNameAttribute();
            } else if (config.isGoogleEnabled()) {
                preferred = config.getGoogleUserNameAttribute();
            } else if (config.isGitHubEnabled()) {
                preferred = config.getGitHubUserNameAttribute();
            }
            String v = claimAsString(claims, preferred);
            if (StringUtils.hasText(v)) {
                return v;
            }
        }

        // Common fallbacks across providers.
        for (String claim : List.of("preferred_username", "email", "sub", "username", "client_id", "azp")) {
            String v = claimAsString(claims, claim);
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return claimAsString(claims, "sub");
    }

    private String claimAsString(Map<String, Object> claims, String claimName) {
        if (!StringUtils.hasText(claimName)) {
            return null;
        }
        Object o = JwtHeaderUserNameExtractor.getClaim(claims, claimName);
        if (o == null) {
            return null;
        }
        return (o instanceof String s) ? s : o.toString();
    }

    private Collection<GeoServerRole> resolveRoles(Map<String, Object> claims) {
        String claimName = config.getTokenRolesClaim();
        List<String> roleStrings = new ArrayList<>();

        if (!StringUtils.hasText(claimName)) {
            roleStrings.add("ROLE_AUTHENTICATED");
            return roleStrings.stream().map(GeoServerRole::new).collect(toList());
        }

        if ("scope".equalsIgnoreCase(claimName) || "scp".equalsIgnoreCase(claimName)) {
            Object rawScope = claims.get(claimName);

            // RFC 7662 typically uses space-delimited string for scope
            if (rawScope instanceof String scope && StringUtils.hasText(scope)) {
                for (String s : scope.split("\\s+")) {
                    if (StringUtils.hasText(s)) roleStrings.add(s);
                }
            } else {
                // Try the alternate claim
                String altName = "scope".equalsIgnoreCase(claimName) ? "scp" : "scope";
                Object rawAlt = claims.get(altName);
                if (rawAlt instanceof String altStr && StringUtils.hasText(altStr)) {
                    for (String s : altStr.split("\\s+")) {
                        if (StringUtils.hasText(s)) roleStrings.add(s);
                    }
                } else {
                    // Fall back to generic extraction helpers
                    Object raw = JwtHeaderUserNameExtractor.getClaim(claims, claimName);
                    roleStrings.addAll(asStringList(raw));
                    roleStrings.addAll(asStringList(rawAlt));
                }
            }
        } else {
            Object raw = JwtHeaderUserNameExtractor.getClaim(claims, claimName);
            roleStrings.addAll(asStringList(raw));
        }

        roleStrings = roleStrings.stream().filter(StringUtils::hasText).collect(toList());
        roleStrings = roleConverter.convert(roleStrings);

        if (!roleStrings.contains("ROLE_AUTHENTICATED")) {
            roleStrings.add("ROLE_AUTHENTICATED");
        }

        return roleStrings.stream().map(GeoServerRole::new).collect(toList());
    }

    private Collection<GeoServerRole> applyRoleServices(String principal, Collection<GeoServerRole> roles) {
        Collection<GeoServerRole> result = roles == null ? new ArrayList<>() : new ArrayList<>(roles);

        RoleCalculator calc = new RoleCalculator(securityManager.getActiveRoleService());
        try {
            calc.addInheritedRoles(result);
        } catch (IOException e) {
            LOGGER.log(SEVERE, "Role calculation failed on inherited roles for user '%s'.".formatted(principal), e);
        }
        calc.addMappedSystemRoles(result);

        if (!result.contains(GeoServerRole.AUTHENTICATED_ROLE)) {
            result.add(GeoServerRole.AUTHENTICATED_ROLE);
        }
        return result;
    }
}
