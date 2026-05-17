/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common.roleresolver;

import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.geoserver.security.oauth2.common.OAuth2ClaimsHelpers.asStringList;
import static org.geoserver.security.oauth2.common.OAuth2ClaimsHelpers.getClaim;

import com.nimbusds.jose.JWSObject;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.oauth2.common.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

/**
 * Resolves roles from either the OAuth2 access-token scopes (when {@code tokenRolesClaim=scope}) or a configured claim
 * read from the token's {@code additionalParameters} or its decoded JWT payload.
 */
public class AccessTokenRoleResolverStrategy implements OpenIdRoleResolverStrategy {

    private static final Logger LOGGER = Logging.getLogger(AccessTokenRoleResolverStrategy.class);

    @Override
    public OpenIdRoleSource getRoleSource() {
        return OpenIdRoleSource.AccessToken;
    }

    @Override
    public List<String> resolveRoleNames(OAuth2ResolverParam param, GeoServerOAuth2LoginFilterConfig config) {
        OAuth2UserRequest userRequest = param.getUserRequest();
        OAuth2AccessToken accessToken = userRequest.getAccessToken();
        String claimName = config.getTokenRolesClaim();
        Set<String> scopes = accessToken.getScopes();
        if (LOGGER.isLoggable(Level.FINE)) {
            String scopeTxt = scopes == null ? null : scopes.stream().collect(joining(","));
            LOGGER.fine("Analyzing access token for roles. Claim: %s. Scopes: %s, additionals: %s"
                    .formatted(claimName, scopeTxt, userRequest.getAdditionalParameters()));
        }
        List<String> roles;
        if ("scope".equals(claimName)) {
            roles = new ArrayList<>(accessToken.getScopes());
        } else {
            Object claimValue = userRequest.getAdditionalParameters().get(claimName);
            roles = new ArrayList<>(toStringList(claimValue, param, config));
            if (roles.isEmpty()) {
                // access token may be a JWT — try decoding and reading the claim from its payload
                try {
                    JWSObject jws = JWSObject.parse(accessToken.getTokenValue());
                    if (jws != null) {
                        Map<String, Object> claims = jws.getPayload().toJSONObject();
                        List<String> fromJwt = asStringList(getClaim(claims, claimName));
                        if (fromJwt != null) roles = new ArrayList<>(fromJwt);
                    }
                } catch (ParseException e) {
                    LOGGER.log(
                            SEVERE, "Could not parse Access Token as JWT — IdP likely returns an opaque access token.");
                }
            }
        }
        return roles;
    }

    /** Coerce a claim value into a list of role-name strings, logging diagnostics for unsupported shapes. */
    private static Collection<String> toStringList(
            Object value, OAuth2ResolverParam param, GeoServerOAuth2LoginFilterConfig config) {
        if (value == null) {
            LOGGER.log(
                    SEVERE,
                    "Role extraction failed. User '%s', roleSource=%s: Claim '%s' is missing."
                            .formatted(param.getPrincipal(), param.getRoleSource(), config.getTokenRolesClaim()));
            return Collections.emptyList();
        }
        if (value instanceof String s) return Collections.singleton(s);
        if (value instanceof String[] arr) return Arrays.asList(arr);
        if (value instanceof List<?> list) {
            List<String> strings = list.stream()
                    .filter(o -> o instanceof String)
                    .map(o -> (String) o)
                    .collect(toList());
            if (strings.size() == list.size()) return strings;
        }
        LOGGER.log(
                SEVERE,
                "Role extraction failed. User '%s', roleSource=%s: Type %s is not supported. Value: %s"
                        .formatted(
                                param.getPrincipal(),
                                param.getRoleSource(),
                                value.getClass().getName(),
                                value));
        return Collections.emptyList();
    }
}
