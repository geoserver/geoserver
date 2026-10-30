/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.File;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;

/** Builds the module's services for a test, with the running version fixed. */
public final class TestServices {

    private TestServices() {}

    /** @return a config persister over the data directory in {@code dir}. */
    public static ConfigPersister persister(File dir) {
        return new ConfigPersister(new GeoServerDataDirectory(new GeoServerResourceLoader(dir)));
    }

    /** @return a service that reports {@code version} as the running version, or none when it is {@code null}. */
    public static UpdatesCheckerService service(ConfigPersister persister, String version) {
        return new UpdatesCheckerService(persister, new FeedClient()) {
            @Override
            protected String readManifestVersion() {
                return version;
            }
        };
    }
}
