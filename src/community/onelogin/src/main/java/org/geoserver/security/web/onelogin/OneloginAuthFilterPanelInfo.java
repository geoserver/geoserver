/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.onelogin;

import org.geoserver.security.onelogin.OneloginAuthenticationFilter;
import org.geoserver.security.onelogin.OneloginAuthenticationFilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/**
 * Configuration panel extension for {@link OneloginAuthFilterPanel}.
 *
 * @author Xandros
 */
public class OneloginAuthFilterPanelInfo
        extends AuthenticationFilterPanelInfo<
                OneloginAuthenticationFilterConfig, OneloginAuthFilterPanel> {

    private static final long serialVersionUID = 2786521869232176111L;

    public OneloginAuthFilterPanelInfo() {
        setComponentClass(OneloginAuthFilterPanel.class);
        setServiceClass(OneloginAuthenticationFilter.class);
        setServiceConfigClass(OneloginAuthenticationFilterConfig.class);
    }
}
