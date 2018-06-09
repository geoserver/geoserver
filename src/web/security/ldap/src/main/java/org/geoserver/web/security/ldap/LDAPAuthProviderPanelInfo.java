/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import org.geoserver.security.ldap.LDAPAuthenticationProvider;
import org.geoserver.security.ldap.LDAPSecurityServiceConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanelInfo;

/**
 * Configuration panel extension for {@link LDAPAuthenticationProvider}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LDAPAuthProviderPanelInfo
        extends AuthenticationProviderPanelInfo<LDAPSecurityServiceConfig, LDAPAuthProviderPanel> {

    private static final long serialVersionUID = 4754541712479614409L;

    public LDAPAuthProviderPanelInfo() {
        setComponentClass(LDAPAuthProviderPanel.class);
        setServiceClass(LDAPAuthenticationProvider.class);
        setServiceConfigClass(LDAPSecurityServiceConfig.class);
    }
}
