/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.RememberMeAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerRememberMeAuthenticationFilter;

/**
 * Configuration panel extension for {@link GeoServerRememberMeAuthenticationFilter}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class RememberMeAuthFilterPanelInfo extends
        AuthenticationFilterPanelInfo<RememberMeAuthenticationFilterConfig, RememberMeAuthFilterPanel> {

    public RememberMeAuthFilterPanelInfo() {
        setServiceClass(GeoServerRememberMeAuthenticationFilter.class);
        setServiceConfigClass(RememberMeAuthenticationFilterConfig.class);
        setComponentClass(RememberMeAuthFilterPanel.class);
    }
}
