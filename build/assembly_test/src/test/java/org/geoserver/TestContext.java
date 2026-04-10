/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import java.nio.file.Path;

/** Runtime context shared with plugin-specific smoke testers. */
public record TestContext(
        String pluginName,
        Path workDir,
        Process process,
        int httpPort,
        int stopPort,
        int startupTimeoutSeconds,
        int startupPollIntervalMs) {

    public String baseUrl() {
        return "http://localhost:" + httpPort;
    }
}
