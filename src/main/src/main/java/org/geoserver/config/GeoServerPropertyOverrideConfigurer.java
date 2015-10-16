/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * 
 * Allows the use of ${GEOSERVER_DATA_DIR} inside properties to refer to the data directory, irrespective of whether that system property has been set
 * or not.
 * 
 * @author niels
 *
 */
public class GeoServerPropertyOverrideConfigurer extends PropertyOverrideConfigurer {

    private static final String DATA_DIR = "${GEOSERVER_DATA_DIR}";

    private static final String DATA_DIR_PATTERN = Pattern.quote(DATA_DIR);

    static final Logger LOGGER = Logging.getLogger(GeoServerPropertyOverrideConfigurer.class);

    protected GeoServerDataDirectory data;

    private String dataDirUnixPath;

    public GeoServerPropertyOverrideConfigurer(GeoServerDataDirectory data) throws IOException {
        this.data = data;
        this.dataDirUnixPath = data.root().getCanonicalFile().getAbsolutePath().replace("\\", "/");
    }

    @Override
    public void setLocations(Resource[] locations) {
        for (int i = 0; i < locations.length; i++) {
            locations[i] = resolveDataDirectory(locations[i]);
        }
    }
    
    @Override
    public void setLocation(Resource location) {
        Resource resolved = resolveDataDirectory(location);
        super.setLocation(resolved);
    }

    private Resource resolveDataDirectory(Resource location) {
        try {
            if(OwsUtils.has(location, "path")) {
                String path = (String) OwsUtils.get(location, "path"); 
                if (path != null && path.contains(DATA_DIR)) {
                    String resolvedPath = path.replaceAll(DATA_DIR_PATTERN,
                            dataDirUnixPath);
                    location = new FileSystemResource(new File(resolvedPath));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error reading resource " + location, e);
        }

        return location;
    }

    @Override
    protected String convertPropertyValue(String property) {
        return property.replaceAll(DATA_DIR_PATTERN, dataDirUnixPath);
    }

}
