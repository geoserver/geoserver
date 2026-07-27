/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

/**
 * Marker for placeholder security configurations that stand in for a component which can no longer be loaded (for
 * example a filter or role service created by a community plugin that is not installed in the running GeoServer).
 *
 * <p>Configurations implementing this interface deserialize cleanly from an old data directory but represent a
 * <em>disabled</em> component: the security subsystem records them and excludes them from the active configuration
 * instead of failing to start.
 */
public interface DisabledSecurityComponentConfig {

    /** The XStream alias / root XML element the original configuration was stored under. */
    String getOriginalAlias();

    /** A human readable hint about the plugin that originally provided the component. */
    String getSourcePlugin();
}
