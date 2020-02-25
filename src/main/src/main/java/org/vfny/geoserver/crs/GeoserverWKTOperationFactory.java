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
import org.geotools.referencing.factory.epsg.CoordinateOperationFactoryUsingWKT;
import org.geotools.util.URLs;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.operation.CoordinateOperationAuthorityFactory;

/**
 * Authority allowing users to define their own CoordinateOperations in a separate file. Will
 * override EPSG definitions.
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
     *
     * @return The URL, or {@code null} if none.
     */
    protected URL getDefinitionsURL() {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        if (loader != null) { // not available for SystemTestData
            Resource definition = loader.get("user_projections/" + FILENAME);
            if (definition.getType() == Type.RESOURCE) {
                File file = definition.file();
                URL url = URLs.fileToUrl(file);
                if (url != null) {
                    return url;
                } else {
                    LOGGER.log(Level.SEVERE, "Had troubles converting file name to URL");
                }
            } else {
                LOGGER.info(
                        definition.path()
                                + " was not found, using the default set of "
                                + "coordinate operation overrides (normally empty)");
            }
        }
        return GeoserverOverridingWKTFactory.class.getResource(FILENAME);
    }
}
