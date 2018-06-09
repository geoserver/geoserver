/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.RequestHeaderAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerRequestHeaderAuthenticationFilter;

/**
 * Configuration panel extension for {@link GeoServerRequestHeaderAuthenticationFilter}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class HeaderAuthFilterPanelInfo
        extends AuthenticationFilterPanelInfo<
                RequestHeaderAuthenticationFilterConfig, HeaderAuthFilterPanel> {

    public HeaderAuthFilterPanelInfo() {
        setComponentClass(HeaderAuthFilterPanel.class);
        setServiceClass(GeoServerRequestHeaderAuthenticationFilter.class);
        setServiceConfigClass(RequestHeaderAuthenticationFilterConfig.class);
    }
}
