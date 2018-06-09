/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLRoleServiceConfig;

/**
 * Configuration panel extension for {@link XMLRoleService}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class XMLRoleServicePanelInfo
        extends RoleServicePanelInfo<XMLRoleServiceConfig, XMLRoleServicePanel> {

    public XMLRoleServicePanelInfo() {
        setComponentClass(XMLRoleServicePanel.class);
        setServiceClass(XMLRoleService.class);
        setServiceConfigClass(XMLRoleServiceConfig.class);
        setPriority(0);
    }
}
