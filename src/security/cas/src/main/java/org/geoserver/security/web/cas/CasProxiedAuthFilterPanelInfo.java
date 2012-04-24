/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.cas;

import org.geoserver.security.cas.CasProxiedAuthenticationFilterConfig;
import org.geoserver.security.cas.GeoServerCasProxiedAuthenticationFilter;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/**
 * Configuration panel extension for {@link GeoServerCasProxiedAuthenticationFilter}.
 * 
 * @author mcr
 */
public class CasProxiedAuthFilterPanelInfo 
    extends AuthenticationFilterPanelInfo<CasProxiedAuthenticationFilterConfig, CasProxiedAuthFilterPanel> {

    private static final long serialVersionUID = 1L;

    public CasProxiedAuthFilterPanelInfo() {
        setComponentClass(CasProxiedAuthFilterPanel.class);
        setServiceClass(GeoServerCasProxiedAuthenticationFilter.class);
        setServiceConfigClass(CasProxiedAuthenticationFilterConfig.class);
    }
}
