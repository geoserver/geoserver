/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.CredentialsFromRequestHeaderFilterConfig;
import org.geoserver.security.filter.GeoServerCredentialsFromRequestHeaderFilter;

/**
 * Configuration panel extension for {@link
 * GeoServerCredentialsFromRequestHeaderAuthenticationFilter}.
 *
 * @author Lorenzo Natali, GeoSolutions
 * @author Mauro Bartolomeoli, GeoSolutions
 */
public class CredentialsFromRequestHeaderFilterPanelInfo
        extends AuthenticationFilterPanelInfo<
                CredentialsFromRequestHeaderFilterConfig, CredentialsFromRequestHeaderFilterPanel> {

    private static final long serialVersionUID = 1L;

    public CredentialsFromRequestHeaderFilterPanelInfo() {
        setComponentClass(CredentialsFromRequestHeaderFilterPanel.class);
        setServiceClass(GeoServerCredentialsFromRequestHeaderFilter.class);
        setServiceConfigClass(CredentialsFromRequestHeaderFilterConfig.class);
    }
}
