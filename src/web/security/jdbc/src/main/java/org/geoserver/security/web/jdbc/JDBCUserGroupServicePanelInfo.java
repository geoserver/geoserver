/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc;

import org.geoserver.security.jdbc.JDBCUserGroupService;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.geoserver.security.web.usergroup.UserGroupServicePanelInfo;

/**
 * Configuration panel extension for {@link JDBCUserGroupService}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class JDBCUserGroupServicePanelInfo
        extends UserGroupServicePanelInfo<JDBCUserGroupServiceConfig, JDBCUserGroupServicePanel> {

    public JDBCUserGroupServicePanelInfo() {
        setComponentClass(JDBCUserGroupServicePanel.class);
        setServiceClass(JDBCUserGroupService.class);
        setServiceConfigClass(JDBCUserGroupServiceConfig.class);
    }
}
