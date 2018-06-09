/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import org.geoserver.security.config.J2eeRoleServiceConfig;
import org.geoserver.security.impl.GeoServerJ2eeRoleService;

/**
 * Configuration panel extension for {@link GeoServerJ2eeRoleService}.
 *
 * @author christian
 */
public class J2eeRoleServicePanelInfo
        extends RoleServicePanelInfo<J2eeRoleServiceConfig, J2eeRoleServicePanel> {

    public J2eeRoleServicePanelInfo() {
        setComponentClass(J2eeRoleServicePanel.class);
        setServiceClass(GeoServerJ2eeRoleService.class);
        setServiceConfigClass(J2eeRoleServiceConfig.class);
        setPriority(0);
    }
}
