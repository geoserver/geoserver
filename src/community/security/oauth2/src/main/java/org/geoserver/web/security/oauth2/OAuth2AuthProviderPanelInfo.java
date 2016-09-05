/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2;

import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;
import org.geoserver.security.oauth2.OAuth2FilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/**
 * Configuration panel extension for {@link GeoServerOAuthAuthenticationFilter}.
 * 
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class OAuth2AuthProviderPanelInfo
        extends AuthenticationFilterPanelInfo<OAuth2FilterConfig, OAuth2AuthProviderPanel> {

    private static final long serialVersionUID = 9128733240285123850L;

    public OAuth2AuthProviderPanelInfo() {
        setComponentClass(OAuth2AuthProviderPanel.class);
        setServiceClass(GeoServerOAuthAuthenticationFilter.class);
        setServiceConfigClass(OAuth2FilterConfig.class);
    }
}
