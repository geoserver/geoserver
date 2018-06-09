/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc;

import org.geoserver.security.jdbc.JDBCConnectAuthProvider;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.geoserver.security.web.auth.AuthenticationProviderPanelInfo;

/**
 * Configuration panel extension for {@link JDBCConnectAuthProvider}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class JDBCAuthProviderPanelInfo
        extends AuthenticationProviderPanelInfo<
                JDBCConnectAuthProviderConfig, JDBCAuthProviderPanel> {

    public JDBCAuthProviderPanelInfo() {
        setComponentClass(JDBCAuthProviderPanel.class);
        setServiceConfigClass(JDBCConnectAuthProviderConfig.class);
        setServiceClass(JDBCConnectAuthProvider.class);
    }
}
