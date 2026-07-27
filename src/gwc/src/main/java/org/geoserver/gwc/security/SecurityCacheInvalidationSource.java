/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

/**
 * Extension point for components that can detect security configuration changes and trigger tile cache invalidation.
 *
 * <p>Any Spring bean can implement this interface. {@link SecurityCacheInvalidator} collects all such beans at startup
 * and registers itself as a listener. The source owns its change-detection mechanism entirely - Hibernate events,
 * polling, REST callbacks, or any other approach.
 *
 * <p>It is intentionally separate from {@link org.geoserver.security.ResourceAccessManager}: a RAM that cannot be
 * modified can have a companion bean act as its invalidation source.
 */
public interface SecurityCacheInvalidationSource {

    /** Registers a listener to be notified when security configuration changes. */
    void register(SecurityCacheInvalidationListener listener);
}
