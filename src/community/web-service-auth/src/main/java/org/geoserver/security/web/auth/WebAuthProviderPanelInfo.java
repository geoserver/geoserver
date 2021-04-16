/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.auth.web.WebAuthenticationConfig;
import org.geoserver.security.auth.web.WebServiceAuthenticationProvider;

/** @author Imran Rajjad - Geo Solutions */
public class WebAuthProviderPanelInfo
        extends AuthenticationProviderPanelInfo<WebAuthenticationConfig, WebAuthProviderPanel> {

    /** serialVersionUID */
    private static final long serialVersionUID = -2639046148651128781L;

    public WebAuthProviderPanelInfo() {
        setComponentClass(WebAuthProviderPanel.class);
        setServiceClass(WebServiceAuthenticationProvider.class);
        setServiceConfigClass(WebAuthenticationConfig.class);
        setPriority(10);
    }
}
