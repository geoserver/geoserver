/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.core.io.Resource;

/**
 * Allows the use of ${GEOSERVER_DATA_DIR} inside properties to refer to the data directory,
 * irrespective of whether that system property has been set or not.
 *
 * <p>Also makes locations relative to the GeoServer Data Directory.
 *
 * @author niels
 */
public class GeoServerPropertyOverrideConfigurer extends PropertyOverrideConfigurer {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.config");

    protected GeoServerDataDirectory data;

    public GeoServerPropertyOverrideConfigurer(GeoServerDataDirectory data) {
        this.data = data;
    }

    @Override
    public void setLocation(Resource location) {
        try {
            location = SpringResourceAdaptor.relative(location, data.getResourceStore());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading resource " + location, e);
        }

        super.setLocation(location);
    }

    @Override
    public void setLocations(Resource[] locations) {
        Resource[] newLocations = new Resource[locations.length];
        for (int i = 0; i < locations.length; i++) {
            try {
                newLocations[i] =
                        SpringResourceAdaptor.relative(locations[i], data.getResourceStore());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error reading resource " + locations[i], e);
                newLocations[i] = locations[i];
            }
        }
        super.setLocations(newLocations);
    }

    @Override
    protected String convertPropertyValue(String property) {
        return property.replace("${GEOSERVER_DATA_DIR}", data.root().getPath());
    }
}
