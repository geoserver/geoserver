/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import java.io.IOException;
import java.util.Properties;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerPropertyConfigurer;

public class GeoFencePropertyPlaceholderConfigurer extends GeoServerPropertyConfigurer {

    public GeoFencePropertyPlaceholderConfigurer(GeoServerDataDirectory data) {
        super(data);
    }

    public Properties getMergedProperties() throws IOException {
        return mergeProperties();
    }
}
