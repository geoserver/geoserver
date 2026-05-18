/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common.roleresolver;

import static java.util.stream.Collectors.toList;
import static org.geoserver.security.oauth2.common.OAuth2ClaimsHelpers.asStringList;
import static org.geoserver.security.oauth2.common.OAuth2ClaimsHelpers.getClaim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.oauth2.common.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Resolves roles by calling the OIDC {@code /userinfo} endpoint and reading either the configured claim or the
 * {@code authorities} returned by Spring's {@link OAuth2UserService}.
 */
public class UserInfoRoleResolverStrategy implements OpenIdRoleResolverStrategy {

    private static final Logger LOGGER = Logging.getLogger(UserInfoRoleResolverStrategy.class);

    /** Supplier of {@link OAuth2UserService} — visible to tests via {@link #setUserServiceSupplier(Supplier)}. */
    private Supplier<OAuth2UserService<OAuth2UserRequest, OAuth2User>> userServiceSupplier =
            DefaultOAuth2UserService::new;

    @Override
    public OpenIdRoleSource getRoleSource() {
        return OpenIdRoleSource.UserInfo;
    }

    @Override
    public List<String> resolveRoleNames(OAuth2ResolverParam param, GeoServerOAuth2LoginFilterConfig config) {
        OAuth2UserRequest userRequest = param.getUserRequest();
        OAuth2UserService<OAuth2UserRequest, OAuth2User> service = userServiceSupplier.get();
        OAuth2User user = service.loadUser(userRequest);

        String claimName = config.getTokenRolesClaim();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Analyzing userInfo for roles. Claim: %s. User: %s".formatted(claimName, user));
        }
        if ("authorities".equals(claimName)) {
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
            return authorities.stream().map(GrantedAuthority::getAuthority).collect(toList());
        }
        List<String> roles = asStringList(getClaim(user.getAttributes(), claimName));
        return roles == null ? new ArrayList<>() : new ArrayList<>(roles);
    }

    public void setUserServiceSupplier(Supplier<OAuth2UserService<OAuth2UserRequest, OAuth2User>> supplier) {
        if (supplier == null) throw new IllegalArgumentException("Supplier for OAuth2UserService must not be null.");
        this.userServiceSupplier = supplier;
    }
}
