/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.saml;

import org.geoserver.security.saml.SAMLAuthenticationFilter;
import org.geoserver.security.saml.SAMLAuthenticationFilterConfig;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/**
 * Configuration panel extension for {@link SAMLAuthFilterPanel}.
 *
 * @author Xandros
 */
public class SAMLAuthFilterPanelInfo
        extends AuthenticationFilterPanelInfo<SAMLAuthenticationFilterConfig, SAMLAuthFilterPanel> {

    private static final long serialVersionUID = 2786521869232176111L;

    public SAMLAuthFilterPanelInfo() {
        setComponentClass(SAMLAuthFilterPanel.class);
        setServiceClass(SAMLAuthenticationFilter.class);
        setServiceConfigClass(SAMLAuthenticationFilterConfig.class);
    }
}
