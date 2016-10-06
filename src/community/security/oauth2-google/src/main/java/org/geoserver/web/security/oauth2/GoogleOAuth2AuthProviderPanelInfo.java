/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2;

import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;
import org.geoserver.security.oauth2.GoogleOAuth2FilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/**
 * Configuration panel extension for {@link GeoServerOAuthAuthenticationFilter}.
 * 
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GoogleOAuth2AuthProviderPanelInfo
        extends AuthenticationFilterPanelInfo<GoogleOAuth2FilterConfig, GoogleOAuth2AuthProviderPanel> {

    private static final long serialVersionUID = 9128733240285123850L;

    public GoogleOAuth2AuthProviderPanelInfo() {
        setComponentClass(GoogleOAuth2AuthProviderPanel.class);
        setServiceClass(GeoServerOAuthAuthenticationFilter.class);
        setServiceConfigClass(GoogleOAuth2FilterConfig.class);
    }
}
