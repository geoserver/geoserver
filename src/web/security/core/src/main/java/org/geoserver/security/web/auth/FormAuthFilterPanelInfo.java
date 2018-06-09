/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.UsernamePasswordAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerUserNamePasswordAuthenticationFilter;

/**
 * Configuration panel extension for {@link GeoServerUserNamePasswordAuthenticationFilter}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class FormAuthFilterPanelInfo
        extends AuthenticationFilterPanelInfo<
                UsernamePasswordAuthenticationFilterConfig, FormAuthFilterPanel> {

    public FormAuthFilterPanelInfo() {
        setServiceClass(GeoServerUserNamePasswordAuthenticationFilter.class);
        setServiceConfigClass(UsernamePasswordAuthenticationFilterConfig.class);
        setComponentClass(FormAuthFilterPanel.class);
    }
}
