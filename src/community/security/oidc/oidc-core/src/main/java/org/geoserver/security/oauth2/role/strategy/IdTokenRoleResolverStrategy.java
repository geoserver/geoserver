/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.role.strategy;

import static java.util.logging.Level.SEVERE;
import static org.geoserver.security.oauth2.token.OAuth2ClaimsHelpers.asStringList;
import static org.geoserver.security.oauth2.token.OAuth2ClaimsHelpers.getClaim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geoserver.security.oauth2.role.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

/** Resolves roles from the {@code tokenRolesClaim} claim of the OIDC ID Token. */
public class IdTokenRoleResolverStrategy implements OpenIdRoleResolverStrategy {

    private static final Logger LOGGER = Logging.getLogger(IdTokenRoleResolverStrategy.class);

    @Override
    public OpenIdRoleSource getRoleSource() {
        return OpenIdRoleSource.IdToken;
    }

    @Override
    public List<String> resolveRoleNames(OAuth2ResolverParam param, GeoServerOAuth2LoginFilterConfig config) {
        OAuth2UserRequest userRequest = param.getUserRequest();
        if (!(userRequest instanceof OidcUserRequest oidcReq)) {
            LOGGER.log(
                    SEVERE,
                    "Role extraction failed. ID token unavailable for clientRegistration %s."
                            .formatted(userRequest.getClientRegistration().getRegistrationId()));
            return Collections.emptyList();
        }
        String claimName = config.getTokenRolesClaim();
        OidcIdToken idToken = oidcReq.getIdToken();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Analyzing ID token for roles. Claim: %s. Claims: %s".formatted(claimName, idToken.getClaims()));
        }
        List<String> roles = asStringList(getClaim(idToken.getClaims(), claimName));
        return roles == null ? new ArrayList<>() : new ArrayList<>(roles);
    }
}
