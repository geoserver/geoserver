/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import java.util.Map;

/**
 * Smoke tester for the ACL plugin. The ACL client normally probes the remote ACL API during startup and fails fast if
 * it can't reach it; setting {@code geoserver.acl.client.startupCheck=false} lets the Spring beans load and the WMS
 * capabilities endpoint respond without a running ACL server. basePath/username/password still have to be set because
 * they're read eagerly, even though they're never used once the probe is disabled.
 */
public class GeoServerAclTester extends DefaultPluginTester {

    @Override
    protected Map<String, String> systemProperties() {
        return Map.of(
                "geoserver.acl.client.startupCheck", "false",
                "geoserver.acl.client.basePath", "http://localhost:0/acl/api",
                "geoserver.acl.client.username", "geoserver",
                "geoserver.acl.client.password", "pwd");
    }

    /**
     * Authenticate as the default admin so GetCapabilities bypasses the ACL access manager — an anonymous call would
     * trigger a remote authorization lookup and fail on the unreachable ACL API.
     */
    @Override
    protected String basicAuthCredentials() {
        return "admin:geoserver";
    }
}
