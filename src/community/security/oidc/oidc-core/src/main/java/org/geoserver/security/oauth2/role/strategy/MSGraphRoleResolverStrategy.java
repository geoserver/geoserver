/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.role.strategy;

import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.joining;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.isRegIdOfType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geoserver.security.oauth2.role.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geoserver.security.oauth2.role.provider.msgraph.MSGraphRolesResolver;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;

/**
 * Resolves roles by calling Microsoft Graph endpoints using the user's delegated access token. Only meaningful when the
 * originating client registration is the Microsoft / Entra ID provider.
 */
public class MSGraphRoleResolverStrategy implements OpenIdRoleResolverStrategy {

    private static final Logger LOGGER = Logging.getLogger(MSGraphRoleResolverStrategy.class);

    /** Supplier of {@link MSGraphRolesResolver} — visible to tests via {@link #setResolverSupplier(Supplier)}. */
    private Supplier<MSGraphRolesResolver> resolverSupplier = MSGraphRolesResolver::new;

    @Override
    public OpenIdRoleSource getRoleSource() {
        return OpenIdRoleSource.MSGraphAPI;
    }

    @Override
    public List<String> resolveRoleNames(OAuth2ResolverParam param, GeoServerOAuth2LoginFilterConfig config) {
        OAuth2UserRequest userRequest = param.getUserRequest();
        ClientRegistration clientReg = userRequest.getClientRegistration();
        String user = param.getPrincipal();
        if (!isRegIdOfType(clientReg.getRegistrationId(), REG_ID_MICROSOFT)) {
            // UI prevents this, but enforce here too: don't leak the access token to MS Graph if it didn't come from MS
            LOGGER.log(
                    SEVERE,
                    ("Resolving roles failed. RoleSource Microsoft Graph API supported with provider %s only. "
                                    + "Currently processing login with %s instead.")
                            .formatted(REG_ID_MICROSOFT, clientReg.getRegistrationId()));
            return Collections.emptyList();
        }
        List<String> roles = new ArrayList<>();
        try {
            String accessToken = userRequest.getAccessToken().getTokenValue();
            roles = resolverSupplier
                    .get()
                    .resolveRoles(
                            accessToken,
                            config.isMsGraphMemberOf(),
                            config.isMsGraphAppRoleAssignments(),
                            config.getMsGraphAppRoleAssignmentsObjectId());
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Role assignments for '%s' from MS Graph: %s"
                        .formatted(user, roles.stream().collect(joining(","))));
            }
        } catch (Exception e) {
            LOGGER.log(SEVERE, "Resolving roles from Microsoft Graph API failed for user '%s'.".formatted(user), e);
        }
        return roles;
    }

    public void setResolverSupplier(Supplier<MSGraphRolesResolver> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier for MSGraphRolesResolver must not be null.");
        }
        this.resolverSupplier = supplier;
    }
}
