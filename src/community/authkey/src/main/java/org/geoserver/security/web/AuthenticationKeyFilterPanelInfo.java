/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import org.geoserver.security.AuthenticationKeyFilterConfig;
import org.geoserver.security.GeoServerAuthenticationKeyFilter;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/**
 * Configuration panel extension for {@link GeoServerAuthenticationKeyFilter}.
 * 
 * @author mcr
 */
public class AuthenticationKeyFilterPanelInfo 
    extends AuthenticationFilterPanelInfo<AuthenticationKeyFilterConfig, AuthenticationKeyFilterPanel> {

    private static final long serialVersionUID = 1L;

    public AuthenticationKeyFilterPanelInfo() {
        setComponentClass(AuthenticationKeyFilterPanel.class);
        setServiceClass(GeoServerAuthenticationKeyFilter.class);
        setServiceConfigClass(AuthenticationKeyFilterConfig.class);
    }
}
