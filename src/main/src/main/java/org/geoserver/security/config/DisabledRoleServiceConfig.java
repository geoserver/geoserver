/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import java.io.Serial;
import org.geoserver.security.GeoServerSecurityProvider;

/**
 * Generic placeholder configuration for a role service created by a removed or uninstalled plugin. An extension
 * registers the legacy XStream alias for it through {@link GeoServerSecurityProvider#getLegacyAliases()}; the original
 * alias and source plugin are resolved from that contribution by the persisted {@code className}. The security
 * subsystem recognizes the {@link DisabledSecurityComponentConfig} marker, disables the role service (falling back to
 * the default one) and reports it for manual migration.
 */
public class DisabledRoleServiceConfig extends BaseSecurityNamedServiceConfig
        implements SecurityRoleServiceConfig, DisabledSecurityComponentConfig {

    @Serial
    private static final long serialVersionUID = 1L;

    private String adminRoleName;
    private String groupAdminRoleName;

    @Override
    public String getAdminRoleName() {
        return adminRoleName;
    }

    @Override
    public void setAdminRoleName(String adminRoleName) {
        this.adminRoleName = adminRoleName;
    }

    @Override
    public String getGroupAdminRoleName() {
        return groupAdminRoleName;
    }

    @Override
    public void setGroupAdminRoleName(String groupAdminRoleName) {
        this.groupAdminRoleName = groupAdminRoleName;
    }

    @Override
    public String getOriginalAlias() {
        return GeoServerSecurityProvider.legacyAlias(getClassName());
    }

    @Override
    public String getSourcePlugin() {
        return GeoServerSecurityProvider.legacySourcePlugin(getClassName());
    }
}
