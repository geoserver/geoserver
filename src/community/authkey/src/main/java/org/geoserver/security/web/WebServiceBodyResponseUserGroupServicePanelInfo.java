/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import org.geoserver.security.WebServiceBodyResponseUserGroupService;
import org.geoserver.security.WebServiceBodyResponseUserGroupServiceConfig;
import org.geoserver.security.web.usergroup.UserGroupServicePanelInfo;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class WebServiceBodyResponseUserGroupServicePanelInfo
        extends UserGroupServicePanelInfo<
                WebServiceBodyResponseUserGroupServiceConfig,
                WebServiceBodyResponseUserGroupServicePanel> {

    /** serialVersionUID */
    private static final long serialVersionUID = 5523562464549930885L;

    public WebServiceBodyResponseUserGroupServicePanelInfo() {
        setComponentClass(WebServiceBodyResponseUserGroupServicePanel.class);
        setServiceClass(WebServiceBodyResponseUserGroupService.class);
        setServiceConfigClass(WebServiceBodyResponseUserGroupServiceConfig.class);
    }
}
