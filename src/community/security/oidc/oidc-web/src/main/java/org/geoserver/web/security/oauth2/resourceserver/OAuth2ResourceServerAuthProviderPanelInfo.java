/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.resourceserver;

import java.io.Serial;
import org.geoserver.security.oauth2.resourceserver.GeoServerOAuth2ResourceServerAuthenticationFilter;
import org.geoserver.security.oauth2.resourceserver.GeoServerOAuth2ResourceServerFilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/** Configuration panel extension for {@link GeoServerOAuthAuthenticationFilter}. */
public class OAuth2ResourceServerAuthProviderPanelInfo
        extends AuthenticationFilterPanelInfo<
                GeoServerOAuth2ResourceServerFilterConfig, OAuth2ResourceServerAuthProviderPanel> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = -3891569684560944819L;

    public OAuth2ResourceServerAuthProviderPanelInfo() {
        setComponentClass(OAuth2ResourceServerAuthProviderPanel.class);
        setServiceClass(GeoServerOAuth2ResourceServerAuthenticationFilter.class);
        setServiceConfigClass(GeoServerOAuth2ResourceServerFilterConfig.class);
    }
}
