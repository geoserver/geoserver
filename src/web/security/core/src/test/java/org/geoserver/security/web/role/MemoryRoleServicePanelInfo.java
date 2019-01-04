/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.impl.MemoryRoleService;

/**
 * Configuration panel info for {@link MemoryRoleServicePanel}.
 *
 * <p>This service is only used for testing, it is only available when running from the development
 * environment.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MemoryRoleServicePanelInfo
        extends RoleServicePanelInfo<MemoryRoleServiceConfigImpl, MemoryRoleServicePanel> {

    public MemoryRoleServicePanelInfo() {
        setComponentClass(MemoryRoleServicePanel.class);
        setServiceClass(MemoryRoleService.class);
        setServiceConfigClass(MemoryRoleServiceConfigImpl.class);
    }
}
