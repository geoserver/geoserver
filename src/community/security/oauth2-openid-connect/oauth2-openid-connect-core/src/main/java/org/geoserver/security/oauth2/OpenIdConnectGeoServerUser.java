/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.io.Serial;
import java.util.Collection;
import java.util.HashSet;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.core.GrantedAuthority;

/**
 * OIDC-specific authenticated principal type.
 *
 * <p>This extends {@link GeoServerUser} so OAuth2/OIDC authentication can carry user-profile properties in the
 * principal object while remaining compatible with existing GeoServer security APIs expecting a {@code GeoServerUser}.
 */
class OpenIdConnectGeoServerUser extends GeoServerUser {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an OIDC principal with the authenticated username and resolved authorities.
     *
     * @param username authenticated principal name
     * @param authorities authorities resolved during authentication
     */
    OpenIdConnectGeoServerUser(String username, Collection<? extends GrantedAuthority> authorities) {
        super(username);
        if (authorities != null) {
            setAuthorities(new HashSet<>(authorities));
        }
    }
}
