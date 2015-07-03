/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.regex.Pattern;

import org.geoserver.config.GeoServerDataDirectory;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;

/**
 * 
 * Allows the use of ${GEOSERVER_DATA_DIR} inside properties to refer to the data directory, 
 * irrespective of whether that system property has been set or not.
 * 
 * @author niels
 *
 */
public class GeoServerPropertyOverrideConfigurer extends PropertyOverrideConfigurer {
    
    protected GeoServerDataDirectory data;
    
    public GeoServerPropertyOverrideConfigurer(GeoServerDataDirectory data) {
        this.data = data;
    }
    
    @Override
    protected String convertPropertyValue(String property) {
        return property.replaceAll(Pattern.quote("${GEOSERVER_DATA_DIR}"), data.root().toString());
    }

}
