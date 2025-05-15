/* (c) 2014 - 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckEndpoint {

    private final GeoServer geoServer;

    public HealthCheckEndpoint(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @GetMapping("/health")
    public boolean isCatalogLoading() {
        return !geoServer.isCatalogLoading();
    }
}
