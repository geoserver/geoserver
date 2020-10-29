/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.auth.web.SimpleWebAuthenticationConfig;
import org.geoserver.security.auth.web.SimpleWebServiceAuthenticationProvider;

/** @author Imran Rajjad - Geo Solutions */
public class SimpleWebAuthProviderPanelInfo
        extends AuthenticationProviderPanelInfo<
                SimpleWebAuthenticationConfig, SimpleWebAuthProviderPanel> {

    /** serialVersionUID */
    private static final long serialVersionUID = -2639046148651128781L;

    public SimpleWebAuthProviderPanelInfo() {
        setComponentClass(SimpleWebAuthProviderPanel.class);
        setServiceClass(SimpleWebServiceAuthenticationProvider.class);
        setServiceConfigClass(SimpleWebAuthenticationConfig.class);
        setPriority(10);
    }
}
