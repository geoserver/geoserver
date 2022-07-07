/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.web.security.oauth2;

import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;
import org.geoserver.security.oauth2.OpenIdConnectAuthenticationFilter;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/** Configuration panel extension for {@link GeoServerOAuthAuthenticationFilter}. */
public class OpenIdConnectAuthProviderPanelInfo
        extends AuthenticationFilterPanelInfo<
                OpenIdConnectFilterConfig, OpenIdConnectAuthProviderPanel> {

    /** serialVersionUID */
    private static final long serialVersionUID = -3891569684560944819L;

    public OpenIdConnectAuthProviderPanelInfo() {
        setComponentClass(OpenIdConnectAuthProviderPanel.class);
        setServiceClass(OpenIdConnectAuthenticationFilter.class);
        setServiceConfigClass(OpenIdConnectFilterConfig.class);
    }
}
