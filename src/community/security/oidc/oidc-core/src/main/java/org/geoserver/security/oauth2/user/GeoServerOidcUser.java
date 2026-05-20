/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.user;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** GeoServer user principal that also exposes Spring OIDC user details. */
public class GeoServerOidcUser extends GeoServerOAuth2User implements OidcUser {

    private static final long serialVersionUID = 1L;

    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    /**
     * Creates a GeoServer-backed OIDC principal.
     *
     * @param username resolved principal name
     * @param authorities resolved GeoServer authorities
     * @param properties resolved GeoServer user properties
     * @param claims merged OIDC claims exposed to Spring callers
     * @param nameAttributeKey provider attribute used to derive the Spring principal name
     * @param idToken original ID token returned by the provider
     * @param userInfo optional user-info response returned by the provider
     */
    public GeoServerOidcUser(
            String username,
            Collection<GeoServerRole> authorities,
            Properties properties,
            Map<String, Object> claims,
            String nameAttributeKey,
            OidcIdToken idToken,
            OidcUserInfo userInfo) {
        super(username, authorities, properties, claims, nameAttributeKey);
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    /** Returns the merged OIDC claims map used by this principal. */
    @Override
    public Map<String, Object> getClaims() {
        return getAttributes();
    }

    /** Returns the original OIDC ID token. */
    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    /** Returns the original OIDC user-info response, when available. */
    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }
}
