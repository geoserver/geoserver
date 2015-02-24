/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import org.geotools.referencing.factory.epsg.FactoryUsingWKT;
import org.vfny.geoserver.global.GeoserverDataDirectory;


/**
 * Authority allowing users to define their own CRS in a separate file
 */
public class GeoserverCustomWKTFactory extends FactoryUsingWKT {
    public static final String SYSTEM_DEFAULT_USER_PROJ_FILE = "user.projections.file";

    /**
     * Returns the URL to the property file that contains CRS definitions. The
     * default implementation returns the URL to the {@value #FILENAME} file.
     *
     * @return The URL, or {@code null} if none.
     */
    protected URL getDefinitionsURL() {
        String cust_proj_file = System.getProperty(SYSTEM_DEFAULT_USER_PROJ_FILE);

        if (cust_proj_file == null) {
            cust_proj_file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),
                    "user_projections/epsg.properties").getAbsolutePath();
        }

        // Attempt to load user-defined projections
        File proj_file = new File(cust_proj_file);

        if (proj_file.exists()) {
            try {
                return proj_file.toURL();
            } catch (MalformedURLException e) {
                LOGGER.log(Level.SEVERE, "Had troubles converting file name to URL", e);
            }
        }

        // Use the built-in property defintions
        cust_proj_file = "user_epsg.properties";

        return GeoserverCustomWKTFactory.class.getResource(cust_proj_file);
    }
}
