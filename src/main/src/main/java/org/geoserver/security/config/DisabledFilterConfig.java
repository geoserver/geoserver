/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import java.io.Serial;
import org.geoserver.security.GeoServerSecurityProvider;

/**
 * Generic placeholder configuration for an authentication filter created by a removed or uninstalled plugin. An
 * extension registers the legacy XStream alias for it through {@link GeoServerSecurityProvider#getLegacyAliases()}; the
 * original alias and source plugin are resolved from that contribution by the persisted {@code className}. The security
 * subsystem recognizes the {@link DisabledSecurityComponentConfig} marker, disables the filter and reports it for
 * manual migration instead of failing to start.
 */
public class DisabledFilterConfig extends SecurityFilterConfig
        implements SecurityAuthFilterConfig, DisabledSecurityComponentConfig {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getOriginalAlias() {
        return GeoServerSecurityProvider.legacyAlias(getClassName());
    }

    @Override
    public String getSourcePlugin() {
        return GeoServerSecurityProvider.legacySourcePlugin(getClassName());
    }
}
