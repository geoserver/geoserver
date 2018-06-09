/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.cas;

import org.geoserver.security.cas.CasAuthenticationFilterConfig;
import org.geoserver.security.cas.GeoServerCasAuthenticationFilter;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/**
 * Configuration panel extension for {@link GeoServerCasAuthenticationFilter}.
 *
 * @author mcr
 */
public class CasAuthFilterPanelInfo
        extends AuthenticationFilterPanelInfo<CasAuthenticationFilterConfig, CasAuthFilterPanel> {

    private static final long serialVersionUID = 1L;

    public CasAuthFilterPanelInfo() {
        setComponentClass(CasAuthFilterPanel.class);
        setServiceClass(GeoServerCasAuthenticationFilter.class);
        setServiceConfigClass(CasAuthenticationFilterConfig.class);
    }
}
