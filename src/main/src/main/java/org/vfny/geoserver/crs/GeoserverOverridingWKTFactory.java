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

import org.geotools.factory.Hints;
import org.geotools.referencing.factory.epsg.FactoryUsingWKT;
import org.vfny.geoserver.global.GeoserverDataDirectory;


/**
 * Same as the {@link GeoserverCustomWKTFactory}, but this one reads a different file and
 * actually overrides official EPSG code interpretations
 */
public class GeoserverOverridingWKTFactory extends FactoryUsingWKT {
    public static final String SYSTEM_DEFAULT_USER_PROJ_FILE = "user.projections.override.file";

    public GeoserverOverridingWKTFactory() {
        super(null, MAXIMUM_PRIORITY);
    }
    
    public GeoserverOverridingWKTFactory(Hints userHints) {
        super(userHints, MAXIMUM_PRIORITY);
    }

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
                    "user_projections/epsg_overrides.properties").getAbsolutePath();
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

        // Use the built-in property definitions
        cust_proj_file = "override_epsg.properties";

        return GeoserverOverridingWKTFactory.class.getResource(cust_proj_file);
    }
    
    
}
