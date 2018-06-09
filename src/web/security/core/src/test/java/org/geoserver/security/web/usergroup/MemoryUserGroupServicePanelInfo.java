/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.impl.MemoryUserGroupService;

/**
 * Configuration panel for {@link MemoryUserGroupService}.
 *
 * <p>This service is only used for testing, it is only available when running from the development
 * environment.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MemoryUserGroupServicePanelInfo
        extends UserGroupServicePanelInfo<
                MemoryUserGroupServiceConfigImpl, MemoryUserGroupServicePanel> {

    public MemoryUserGroupServicePanelInfo() {
        setComponentClass(MemoryUserGroupServicePanel.class);
        setServiceClass(MemoryUserGroupService.class);
        setServiceConfigClass(MemoryUserGroupServiceConfigImpl.class);
    }
}
