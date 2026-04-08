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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.StringUtils;

/**
 * Resource-server opaque token introspector that bridges RFC7662 introspection responses into GeoServer roles.
 *
 * <p>IMPORTANT: Spring expects certain standard claims (e.g. exp/iat/nbf) to be {@link Instant}, not numbers. Normalize
 * them before returning the {@link OAuth2AuthenticatedPrincipal}.
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

        Map<String, Object> rawClaims = delegate.introspectToken(token);
        if (rawClaims == null || rawClaims.isEmpty()) {
            throw new BadOpaqueTokenException("Empty introspection response");
        }

        Map<String, Object> claims = normalizeClaimsForSpring(rawClaims);

        Object active = claims.get(OAuth2TokenIntrospectionClaimNames.ACTIVE);
        if (!(active instanceof Boolean) || !Boolean.TRUE.equals(active)) {
            throw new BadOpaqueTokenException("Token is not active");
        }

        String principal = resolvePrincipal(claims);
        if (!StringUtils.hasText(principal)) {
            principal = Optional.ofNullable(claimAsString(claims, OAuth2TokenIntrospectionClaimNames.SUB))
                    .orElse("unknown");
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

        // DefaultOAuth2AuthenticatedPrincipal expects Collection<GrantedAuthority>
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.addAll(roles); // GeoServerRole implements GrantedAuthority

        return new DefaultOAuth2AuthenticatedPrincipal(principal, claims, authorities);
    }

    /**
     * Spring's OpaqueTokenAuthenticationProvider expects exp/iat/nbf to be Instant (not Integer/Long). Spring's own
     * introspectors do this conversion internally. :contentReference[oaicite:1]{index=1}
     */
    private Map<String, Object> normalizeClaimsForSpring(Map<String, Object> in) {
        Map<String, Object> out = new HashMap<>(in);

        convertToInstantIfPresent(out, OAuth2TokenIntrospectionClaimNames.EXP);
        convertToInstantIfPresent(out, OAuth2TokenIntrospectionClaimNames.IAT);
        convertToInstantIfPresent(out, OAuth2TokenIntrospectionClaimNames.NBF);

        // Optional normalization: "scope" in RFC7662 is commonly a space-delimited string.
        // Converting to a List<String> improves downstream compatibility.
        Object scope = out.get(OAuth2TokenIntrospectionClaimNames.SCOPE);
        if (scope instanceof String s && StringUtils.hasText(s)) {
            List<String> scopes = new ArrayList<>();
            for (String part : s.split("\\s+")) {
                if (StringUtils.hasText(part)) scopes.add(part);
            }
            out.put(OAuth2TokenIntrospectionClaimNames.SCOPE, scopes);
        }

        return out;
    }

    private void convertToInstantIfPresent(Map<String, Object> claims, String claimName) {
        Object v = claims.get(claimName);
        if (v == null || v instanceof Instant) {
            return;
        }

        Long epoch = null;
        if (v instanceof Number n) {
            epoch = n.longValue();
        } else if (v instanceof String s && StringUtils.hasText(s)) {
            try {
                epoch = Long.parseLong(s);
            } catch (NumberFormatException ignore) {
                return;
            }
        } else {
            return;
        }

        // Heuristic: treat very large values as millis, otherwise seconds.
        Instant inst = (epoch != null)
                ? (epoch >= 1_000_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch))
                : null;

        if (inst != null) {
            claims.put(claimName, inst);
        }
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

            // RFC 7662 often uses space-delimited string for scope
            if (rawScope instanceof String scope && StringUtils.hasText(scope)) {
                for (String s : scope.split("\\s+")) {
                    if (StringUtils.hasText(s)) roleStrings.add(s);
                }
            } else {
                // Try the alternate claim
                String altName = "scope".equalsIgnoreCase(claimName) ? "scp" : "scope";
                Object rawAlt = claims.get(altName);

                // If alt is string, split it; otherwise fall back to generic extraction helpers
                if (rawAlt instanceof String altStr && StringUtils.hasText(altStr)) {
                    for (String s : altStr.split("\\s+")) {
                        if (StringUtils.hasText(s)) roleStrings.add(s);
                    }
                } else {
                    Object raw = JwtHeaderUserNameExtractor.getClaim(claims, claimName);
                    safeAddAll(roleStrings, asStringList(raw));
                    safeAddAll(roleStrings, asStringList(rawAlt));
                }
            }
        } else {
            Object raw = JwtHeaderUserNameExtractor.getClaim(claims, claimName);
            safeAddAll(roleStrings, asStringList(raw));
        }

        roleStrings = roleStrings.stream().filter(StringUtils::hasText).collect(toList());

        List<String> converted = roleConverter.convert(roleStrings);
        roleStrings = (converted != null) ? converted : Collections.emptyList();

        if (!roleStrings.contains("ROLE_AUTHENTICATED")) {
            roleStrings = new ArrayList<>(roleStrings);
            roleStrings.add("ROLE_AUTHENTICATED");
        }

        return roleStrings.stream().map(GeoServerRole::new).collect(toList());
    }

    private void safeAddAll(List<String> target, List<String> values) {
        if (values != null && !values.isEmpty()) {
            target.addAll(values);
        }
    }

    private Collection<GeoServerRole> applyRoleServices(String principal, Collection<GeoServerRole> roles) {
        Collection<GeoServerRole> result = (roles == null) ? new ArrayList<>() : new ArrayList<>(roles);

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
