/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import org.geoserver.security.config.SecurityRoleServiceConfig;


public class XMLRoleServiceConfig extends XMLSecurityServiceConfig 
    implements SecurityRoleServiceConfig {

    private static final long serialVersionUID = 1L;

    protected String adminRoleName;

    @Override
    public String getAdminRoleName() {
        return adminRoleName;
    }

    @Override
    public void setAdminRoleName(String name) {
        adminRoleName=name;
    }

}
