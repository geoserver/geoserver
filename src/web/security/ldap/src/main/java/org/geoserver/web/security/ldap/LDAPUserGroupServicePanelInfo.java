/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import org.geoserver.security.ldap.LDAPUserGroupService;
import org.geoserver.security.ldap.LDAPUserGroupServiceConfig;
import org.geoserver.security.web.usergroup.UserGroupServicePanelInfo;

/** @author Niels Charlier */
public class LDAPUserGroupServicePanelInfo
        extends UserGroupServicePanelInfo<LDAPUserGroupServiceConfig, LDAPUserGroupServicePanel> {

    private static final long serialVersionUID = 3768741389681107925L;

    public LDAPUserGroupServicePanelInfo() {
        setComponentClass(LDAPUserGroupServicePanel.class);
        setServiceClass(LDAPUserGroupService.class);
        setServiceConfigClass(LDAPUserGroupServiceConfig.class);
    }
}
