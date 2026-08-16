/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

/**
 * Smoke tester for the standalone {@code geofence} plugin (remote-client mode, as opposed to {@code geofence-server}'s
 * embedded engine). Its {@code RestRuleReaderService} is still a stub that always returns {@code null} - an anonymous
 * GetCapabilities call would trigger a rule lookup through it and fail with a cache-loader exception. Authenticating as
 * admin bypasses the lookup entirely: {@code GeofenceAccessManager} has an explicit superuser shortcut that never
 * consults the rule reader.
 */
public class GeofenceTester extends DefaultPluginTester {

    @Override
    protected String basicAuthCredentials() {
        return "admin:geoserver";
    }
}
