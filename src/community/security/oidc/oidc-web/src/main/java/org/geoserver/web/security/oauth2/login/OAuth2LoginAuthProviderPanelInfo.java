/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.login;

import java.io.Serial;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/** Configuration panel extension for {@link GeoServerOAuthAuthenticationFilter}. */
public class OAuth2LoginAuthProviderPanelInfo
        extends AuthenticationFilterPanelInfo<GeoServerOAuth2LoginFilterConfig, OAuth2LoginAuthProviderPanel> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = -3891569684560944819L;

    public OAuth2LoginAuthProviderPanelInfo() {
        setComponentClass(OAuth2LoginAuthProviderPanel.class);
        setServiceClass(GeoServerOAuth2LoginAuthenticationFilter.class);
        setServiceConfigClass(GeoServerOAuth2LoginFilterConfig.class);
    }
}
