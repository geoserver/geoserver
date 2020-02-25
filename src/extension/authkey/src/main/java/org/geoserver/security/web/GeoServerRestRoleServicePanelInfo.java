/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import org.geoserver.security.GeoServerRestRoleService;
import org.geoserver.security.GeoServerRestRoleServiceConfig;
import org.geoserver.security.web.role.RoleServicePanelInfo;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoServerRestRoleServicePanelInfo
        extends RoleServicePanelInfo<
                GeoServerRestRoleServiceConfig, GeoServerRestRoleServicePanel> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1804560200750315834L;

    public GeoServerRestRoleServicePanelInfo() {
        setComponentClass(GeoServerRestRoleServicePanel.class);
        setServiceClass(GeoServerRestRoleService.class);
        setServiceConfigClass(GeoServerRestRoleServiceConfig.class);
    }
}
