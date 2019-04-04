/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config.impl;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;

public class MemoryRoleServiceConfigImpl extends BaseSecurityNamedServiceConfig
        implements SecurityRoleServiceConfig {
    private static final long serialVersionUID = 1L;
    protected String adminRoleName;
    protected String groupAdminRoleName;

    protected String toBeEncrypted;

    public MemoryRoleServiceConfigImpl() {}

    public MemoryRoleServiceConfigImpl(MemoryRoleServiceConfigImpl other) {
        super(other);
        adminRoleName = other.getAdminRoleName();
        groupAdminRoleName = other.getGroupAdminRoleName();
        toBeEncrypted = other.getToBeEncrypted();
    }

    public String getToBeEncrypted() {
        return toBeEncrypted;
    }

    public void setToBeEncrypted(String toBeEncrypted) {
        this.toBeEncrypted = toBeEncrypted;
    }

    @Override
    public String getAdminRoleName() {
        return adminRoleName;
    }

    @Override
    public void setAdminRoleName(String name) {
        adminRoleName = name;
    }

    public String getGroupAdminRoleName() {
        return groupAdminRoleName;
    }

    public void setGroupAdminRoleName(String groupAdminRoleName) {
        this.groupAdminRoleName = groupAdminRoleName;
    }
}
