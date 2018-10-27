/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.crs.netcdf;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.coverage.io.netcdf.crs.NetCDFCRSAuthorityFactory;
import org.geotools.util.URLs;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.crs.CRSAuthorityFactory;

/**
 * Same as the {@link NetCDFCRSAuthorityFactory}, but this one reads a different file and can be
 * populate from an external properties file provided within the
 * GEOSERVER_DATA_DIR/user_projections/netcdf.projections.properties file.
 */
public class NetCDFCRSOverridingAuthorityFactory extends NetCDFCRSAuthorityFactory
        implements CRSAuthorityFactory {

    public NetCDFCRSOverridingAuthorityFactory() {
        this(null);
    }

    public NetCDFCRSOverridingAuthorityFactory(Hints userHints) {
        super(userHints, MAXIMUM_PRIORITY - 2);
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
                Resource custom_proj = loader.get("user_projections/netcdf.projections.properties");
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
        cust_proj_file = "override_epsg.netcdf.properties";

        return NetCDFCRSOverridingAuthorityFactory.class.getResource(cust_proj_file);
    }
}
