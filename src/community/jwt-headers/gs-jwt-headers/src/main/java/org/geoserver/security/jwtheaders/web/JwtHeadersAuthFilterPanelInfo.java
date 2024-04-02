/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.web;

import org.geoserver.security.filter.GeoServerRequestHeaderAuthenticationFilter;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilter;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/** Configuration panel extension for {@link GeoServerRequestHeaderAuthenticationFilter}. */
public class JwtHeadersAuthFilterPanelInfo
        extends AuthenticationFilterPanelInfo<
                GeoServerJwtHeadersFilterConfig, JwtHeadersAuthFilterPanel> {

    public JwtHeadersAuthFilterPanelInfo() {
        setComponentClass(JwtHeadersAuthFilterPanel.class);
        setServiceClass(GeoServerJwtHeadersFilter.class);
        setServiceConfigClass(GeoServerJwtHeadersFilterConfig.class);
    }
}
