/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.J2eeAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerJ2eeAuthenticationFilter;

/**
 * Configuration panel extension for {@link GeoServerJ2eeAuthenticationFilter}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class J2eeAuthFilterPanelInfo 
    extends AuthenticationFilterPanelInfo<J2eeAuthenticationFilterConfig, J2eeAuthFilterPanel> {

    public J2eeAuthFilterPanelInfo() {
        setComponentClass(J2eeAuthFilterPanel.class);
        setServiceClass(GeoServerJ2eeAuthenticationFilter.class);
        setServiceConfigClass(J2eeAuthenticationFilterConfig.class);
        setPriority(0);
    }
}
