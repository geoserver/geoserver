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
import org.geotools.util.logging.Logging;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

/**
 * Converts a validated {@link Jwt} (resource server / bearer token) into a GeoServer-friendly authentication token.
 *
 * <p>This converter is intended for "hybrid mode" use, where the existing OAuth2/OIDC Login filter also accepts
 * machine-to-machine requests carrying {@code Authorization: Bearer <JWT>}.
 */
public class GeoServerOAuth2JwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken>, GeoServerOAuth2ClientRegistrationId {

    private static final Logger LOGGER = Logging.getLogger(GeoServerOAuth2JwtAuthenticationConverter.class);

    private final GeoServerSecurityManager securityManager;
    private final GeoServerOAuth2LoginFilterConfig config;
    private final RoleConverter roleConverter;

    public GeoServerOAuth2JwtAuthenticationConverter(
            GeoServerSecurityManager securityManager, GeoServerOAuth2LoginFilterConfig config) {
        this.securityManager = Objects.requireNonNull(securityManager, "securityManager");
        this.config = Objects.requireNonNull(config, "config");

        JwtConfiguration jwtConfiguration = new JwtConfiguration();
        jwtConfiguration.setRoleConverterString(config.getRoleConverterString());
        jwtConfiguration.setOnlyExternalListedRoles(config.isOnlyExternalListedRoles());
        this.roleConverter = new RoleConverter(jwtConfiguration);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        String principal = resolvePrincipal(jwt);
        if (!StringUtils.hasText(principal)) {
            principal = Optional.ofNullable(jwt.getSubject()).orElse("unknown");
        }

        // Avoid unintentional collision with built-in administrator users.
        if (ADMIN_USERNAME.equalsIgnoreCase(principal) || ROOT_USERNAME.equalsIgnoreCase(principal)) {
            LOGGER.log(
                    Level.WARNING, "Potentially harmful JWT principal '{0}' detected. Granting no roles.", principal);
            return new JwtAuthenticationToken(jwt, List.of(), principal);
        }

        Collection<GeoServerRole> roles = resolveRoles(jwt);
        roles = applyRoleServices(principal, roles);

        return new JwtAuthenticationToken(jwt, roles, principal);
    }

    private String resolvePrincipal(Jwt jwt) {
        Map<String, Object> claims = jwt.getClaims();

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

        // Common fallbacks for access tokens across providers.
        for (String claim : List.of("preferred_username", "email", "sub", "client_id", "azp")) {
            String v = claimAsString(claims, claim);
            if (StringUtils.hasText(v)) {
                return v;
            }
        }

        return jwt.getSubject();
    }

    private String claimAsString(Map<String, Object> claims, String claimName) {
        if (!StringUtils.hasText(claimName)) {
            return null;
        }
        Object o = JwtHeaderUserNameExtractor.getClaim(claims, claimName);
        if (o == null) {
            return null;
        }
        if (o instanceof String s) {
            return s;
        }
        return o.toString();
    }

    private Collection<GeoServerRole> resolveRoles(Jwt jwt) {
        String claimName = config.getTokenRolesClaim();
        List<String> roleStrings = new ArrayList<>();

        if (!StringUtils.hasText(claimName)) {
            // No role claim configured; still mark as authenticated.
            roleStrings.add("ROLE_AUTHENTICATED");
            return roleStrings.stream().map(GeoServerRole::new).collect(toList());
        }

        Map<String, Object> claims = jwt.getClaims();

        if ("scope".equalsIgnoreCase(claimName) || "scp".equalsIgnoreCase(claimName)) {
            // OAuth2 scopes are frequently represented as a space-delimited string.
            String scope = jwt.getClaimAsString(claimName);
            if (StringUtils.hasText(scope)) {
                for (String s : scope.split("\\s+")) {
                    if (StringUtils.hasText(s)) {
                        roleStrings.add(s);
                    }
                }
            } else {
                // Fallback to the alternate claim if configured claim not present as String
                String altName = "scope".equalsIgnoreCase(claimName) ? "scp" : "scope";
                Object rawAlt = claims.get(altName);
                if (rawAlt instanceof String altStr && StringUtils.hasText(altStr)) {
                    for (String s : altStr.split("\\s+")) {
                        if (StringUtils.hasText(s)) roleStrings.add(s);
                    }
                } else {
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
