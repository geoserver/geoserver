/*
 *  Copyright (C) 2014 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.geoserver.geofence.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerPropertyConfigurer;
import org.geotools.util.logging.Logging;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public class GeoFencePropertyPlaceholderConfigurer extends GeoServerPropertyConfigurer {

    private static final Logger LOGGER = Logging.getLogger(GeoFencePropertyPlaceholderConfigurer.class);

    private GeoServerDataDirectory data;
    private File configFile;

    public GeoFencePropertyPlaceholderConfigurer(GeoServerDataDirectory data) {
        super(data);
        this.data = data;
    }
        
    public Properties getMergedProperties() throws IOException {
        return mergeProperties();
    }
    
    /**
     * @return the configLocation
     */
    public File getConfigFile() {
        return configFile;
    }
    
    @Override
    public void setLocation(Resource location) {
        super.setLocation(location);

        try {
            File f = location.getFile();
            if (f != null && !f.isAbsolute()) {
                //make relative to data directory
                f = new File(data.root(), f.getPath());
                location = new UrlResource(f.toURI());
                this.configFile = f;
            }
        }
        catch(IOException e) {
            LOGGER.log(Level.WARNING, "Error reading resource " + location, e);
        }
    }

}
