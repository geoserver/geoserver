/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc;

import org.geoserver.security.jdbc.JDBCRoleService;
import org.geoserver.security.jdbc.config.JDBCRoleServiceConfig;
import org.geoserver.security.web.role.RoleServicePanelInfo;

/**
 * Configuration panel extension for {@link JDBCRoleService}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class JDBCRoleServicePanelInfo
        extends RoleServicePanelInfo<JDBCRoleServiceConfig, JDBCRoleServicePanel> {

    public JDBCRoleServicePanelInfo() {
        setComponentClass(JDBCRoleServicePanel.class);
        setServiceClass(JDBCRoleService.class);
        setServiceConfigClass(JDBCRoleServiceConfig.class);
    }
}
