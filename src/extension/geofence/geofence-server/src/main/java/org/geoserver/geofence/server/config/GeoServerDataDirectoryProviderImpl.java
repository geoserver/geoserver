/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.config;

import java.io.File;
import java.util.Optional;
import org.geofence.core.db.config.GeoFenceConfigDirectoryProvider;
import org.geoserver.config.GeoServerDataDirectory;
import org.springframework.stereotype.Component;

@Component
public class GeoServerDataDirectoryProviderImpl implements GeoFenceConfigDirectoryProvider {

    private final GeoServerDataDirectory dataDirectory;

    public GeoServerDataDirectoryProviderImpl(GeoServerDataDirectory dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    @Override
    public Optional<String> getConfigDirectory() {
        return Optional.of(new File(dataDirectory.root(), "geofence").getAbsolutePath());
    }
}
