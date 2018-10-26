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
import org.geotools.util.factory.Hints;

/**
 * Same as the {@link GeoserverCustomWKTFactory}, but this one reads a different file and actually
 * overrides official EPSG code interpretations
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
            if (loader != null) { // Not available for SystemTestData
                Resource custom_proj = loader.get("user_projections/epsg_overrides.properties");
                if (custom_proj.getType() == Type.RESOURCE) {
                    cust_proj_file = custom_proj.file().getAbsolutePath();
                }
            }
        }
        // Attempt to load user-defined projections
        if (cust_proj_file != null) {
            File proj_file = new File(cust_proj_file);

            if (proj_file.exists()) {
                URL url = URLs.fileToUrl(proj_file);
                if (url != null) {
                    return url;
                } else {
                    LOGGER.log(
                            Level.SEVERE, "Had troubles converting " + cust_proj_file + " to URL");
                }
            }
        }

        // Use the built-in property definitions
        cust_proj_file = "override_epsg.properties";

        return GeoserverOverridingWKTFactory.class.getResource(cust_proj_file);
    }
}
