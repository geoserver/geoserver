/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.referencing.factory.epsg.FactoryUsingWKT;
import org.geotools.util.URLs;

/** Authority allowing users to define their own CRS in a separate file */
public class GeoserverCustomWKTFactory extends FactoryUsingWKT {
    public static final String SYSTEM_DEFAULT_USER_PROJ_FILE = "user.projections.file";

    /**
     * Returns the URL to the property file that contains CRS definitions. The default
     * implementation returns the URL to the {@value #FILENAME} file.
     *
     * @return The URL, or {@code null} if none.
     */
    protected URL getDefinitionsURL() {
        String cust_proj_file = System.getProperty(SYSTEM_DEFAULT_USER_PROJ_FILE);
        if (cust_proj_file == null) {
            GeoServerResourceLoader loader =
                    GeoServerExtensions.bean(GeoServerResourceLoader.class);
            if (loader
                    != null) { // not available during construction SystemTestData - call CRS reset
                // to fix
                Resource custom_proj = loader.get("user_projections/epsg.properties");
                if (custom_proj.getType() == Type.RESOURCE) {
                    cust_proj_file = custom_proj.file().getAbsolutePath();
                }
            }
        }
        if (cust_proj_file != null) {
            // Attempt to load user-defined projections
            File proj_file = new File(cust_proj_file);
            if (proj_file.exists()) {
                URL url = URLs.fileToUrl(proj_file);
                if (url != null) {
                    return url;
                } else {
                    LOGGER.log(Level.SEVERE, "Had troubles converting file name to URL");
                }
            }
        }
        // Use the built-in property defintions
        cust_proj_file = "user_epsg.properties";
        return GeoserverCustomWKTFactory.class.getResource(cust_proj_file);
    }
}
