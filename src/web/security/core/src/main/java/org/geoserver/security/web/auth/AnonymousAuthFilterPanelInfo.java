/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.AnonymousAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter;

/**
 * Configuration panel extension for {@link GeoServerAnonymousAuthenticationFilter}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AnonymousAuthFilterPanelInfo
        extends AuthenticationFilterPanelInfo<
                AnonymousAuthenticationFilterConfig, AnonymousAuthFilterPanel> {

    public AnonymousAuthFilterPanelInfo() {
        setServiceClass(GeoServerAnonymousAuthenticationFilter.class);
        setServiceConfigClass(AnonymousAuthenticationFilterConfig.class);
        setComponentClass(AnonymousAuthFilterPanel.class);
    }
}
