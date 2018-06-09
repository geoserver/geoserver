/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.security.xml.XMLUserGroupServiceConfig;

/**
 * Configuration panel extension for {@link XMLUserGroupService}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class XMLUserGroupServicePanelInfo
        extends UserGroupServicePanelInfo<XMLUserGroupServiceConfig, XMLUserGroupServicePanel> {

    public XMLUserGroupServicePanelInfo() {
        setComponentClass(XMLUserGroupServicePanel.class);
        setServiceClass(XMLUserGroupService.class);
        setServiceConfigClass(XMLUserGroupServiceConfig.class);
        setPriority(0);
    }
}
