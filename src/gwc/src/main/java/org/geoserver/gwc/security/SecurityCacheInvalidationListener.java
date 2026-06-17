/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

/**
 * Callback notified when security configuration changes require tile cache invalidation.
 *
 * <p>Implemented by {@link SecurityCacheInvalidator}. Sources call this directly - no Spring event bus is involved.
 */
public interface SecurityCacheInvalidationListener {

    /** Called when security configuration has changed and some cached tiles may be stale. */
    void onSecurityConfigChange(SecurityConfigurationChangeEvent event);
}
