/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.security.SecurityConfigDiagnostics.ComponentType;

/**
 * Declares that a security component (an authentication filter or a role service) created by a now removed or
 * uninstalled plugin was persisted under the given XStream {@code alias} with the given {@code className}. The security
 * subsystem uses this to deserialize an old data directory into a disabled placeholder and report it for migration,
 * instead of failing to start.
 *
 * <p>Extensions contribute these through {@link GeoServerSecurityProvider#getLegacyAliases()} — pure data, no XStream
 * code or placeholder classes required.
 *
 * @param alias the XStream alias / root element the component was stored under
 * @param className the persisted filter/role-service implementation class name; the reliable identifier after
 *     deserialization (the root alias is consumed, the {@code className} element is not)
 * @param type the kind of component; only {@link ComponentType#AUTHENTICATION_FILTER} and
 *     {@link ComponentType#ROLE_SERVICE} are currently supported
 * @param sourcePlugin a human readable description of the plugin that originally provided the component
 */
public record LegacySecurityAlias(String alias, String className, ComponentType type, String sourcePlugin) {}
