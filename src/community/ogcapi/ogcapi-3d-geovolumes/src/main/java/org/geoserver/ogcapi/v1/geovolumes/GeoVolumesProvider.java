/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class parses the <code>collections.json</code> file and provides its contents as a set of
 * GeoVolumesCollectionDocument objects.
 */
@Component
public class GeoVolumesProvider {

    static final Logger LOGGER = Logging.getLogger(GeoVolumesProvider.class);

    /**
     * Path to a folder that contains <code>collections.json</code> file and the associated
     * resources linked by relative paths.
     */
    public static final String GEOVOLUMES_LOCATION = "GEOVOLUMES_LOCATION";

    /** Default directory where the GeoVolumes collections are stored, inside the data directory. */
    public static final String GEOVOLUMES_DEFAULT_DIR = "geovolumes";

    private final Resource geovolumesDirectory;
    private final FileWatcher<GeoVolumes> collectionsWatcher;
    private GeoVolumes collections;
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public GeoVolumesProvider(GeoServerDataDirectory dd) {
        // look up the folder where the collections.json file is located
        this(getGeoVolumesResource(dd));
    }

    public GeoVolumesProvider(Resource geovolumeDirectory) {
        this.geovolumesDirectory = geovolumeDirectory;
        // create if missing
        if (geovolumeDirectory.getType() == Resource.Type.UNDEFINED) {
            geovolumeDirectory.dir();
        }

        this.collectionsWatcher =
                new FileWatcher<>(geovolumeDirectory.get("collections.json")) {
                    @Override
                    protected GeoVolumes parseFileContents(InputStream in) throws IOException {
                        return mapper.readValue(in, GeoVolumes.class);
                    }
                };
    }

    /**
     * Returns a GeoVolumes object ready for use, updated to the latest contents found in the <code>
     * collections.json</code> file.
     */
    public GeoVolumes getGeoVolumes() throws IOException {
        if (collectionsWatcher.isModified()) {
            collections = collectionsWatcher.read();
        } else if (collections == null) {
            collections = new GeoVolumes(Collections.emptyList());
        }
        return collections;
    }

    private static Resource getGeoVolumesResource(GeoServerDataDirectory dd) {
        String location = GeoServerExtensions.getProperty(GEOVOLUMES_LOCATION);
        Resource geovolumes;
        if (location != null) {
            geovolumes = Files.asResource(new File(location));
        } else {
            geovolumes = dd.get(GEOVOLUMES_DEFAULT_DIR);
        }
        return geovolumes;
    }

    public Resource getResource(String path) {
        return geovolumesDirectory.get(path);
    }
}
