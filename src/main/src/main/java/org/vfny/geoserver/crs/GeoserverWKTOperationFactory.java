/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import java.io.File;
import java.net.URL;
import org.geotools.data.DataUtilities;
import org.geotools.factory.Hints;
import org.geotools.referencing.factory.epsg.CoordinateOperationFactoryUsingWKT;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Authority allowing users to define their own CoordinateOperations in a separate file.
 * Will override EPSG definitions.
 * 
 * @author Oscar Fonts
 */
public class GeoserverWKTOperationFactory extends CoordinateOperationFactoryUsingWKT
        implements CoordinateOperationAuthorityFactory {

    public GeoserverWKTOperationFactory() {
        super(null, MAXIMUM_PRIORITY);
    }
    
    public GeoserverWKTOperationFactory(Hints userHints) {
        super(userHints, MAXIMUM_PRIORITY);
    }

    /**
     * Returns the URL to the property file that contains Operation definitions from
     * $GEOSERVER_DATA_DIR/user_projections/{@value #FILENAME}
     * @return The URL, or {@code null} if none.
     */
    protected URL getDefinitionsURL() {
        File file = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),
                "user_projections/" + FILENAME);

        if (file.exists()) {
            return DataUtilities.fileToURL(file);
        } else {
            LOGGER.info(file.getAbsolutePath() + " was not found, using the default set of " +
            		"coordinate operation overrides (normally empty)");
            // use the built-in file
            return GeoserverOverridingWKTFactory.class.getResource(FILENAME); 
        }
    }
}
