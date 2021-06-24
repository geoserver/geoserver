/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

public class InspireDirectoryManager {

    private GeoServerDataDirectory dataDirectory;

    private static Logger LOGGER = Logging.getLogger(InspireDirectoryManager.class);

    public static final String LANGUAGES_MAPPINGS = "available_languages.properties";

    public static final String INSPIRE_DIR = "inspire";

    public InspireDirectoryManager(GeoServerDataDirectory dataDirectory) {
        this.dataDirectory = dataDirectory;
        try {
            this.dataDirectory.findOrCreateDir(INSPIRE_DIR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Properties getLanguagesMappings() throws IOException {
        File file = null;
        try {
            file = this.dataDirectory.findFile(INSPIRE_DIR, LANGUAGES_MAPPINGS);
        } catch (IOException e) {
            LOGGER.fine("Error occurred while retrieving languages mappings from inspire dir");
        }

        try (InputStream is =
                file != null && file.exists()
                        ? new FileInputStream(file)
                        : getClass().getResource("available_languages.properties").openStream()) {
            Properties list = new Properties();
            list.load(is);
            return list;
        }
    }

    public static InspireDirectoryManager get() {
        return GeoServerExtensions.bean(InspireDirectoryManager.class);
    }
}
