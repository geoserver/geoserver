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

/** This class provides methods to retrieve the content of the inspire directory. */
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

    /**
     * Get available languages from the available_languages.properties file in the inspire directory
     * if present otherwise from the file in the classpath.
     *
     * @return
     * @throws IOException
     */
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

    /**
     * Get the singleton bean of this class.
     *
     * @return the singletion bean of this class.
     */
    public static InspireDirectoryManager get() {
        return GeoServerExtensions.bean(InspireDirectoryManager.class);
    }
}
