/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.referencing.operation.PropertyCoordinateOperationFactory;
import org.geotools.util.URLs;
import org.geotools.util.factory.Hints;
import org.jspecify.annotations.NonNull;

/**
 * Authority allowing users to define their own CoordinateOperations in a separate file. Will override EPSG definitions.
 *
 * @author Oscar Fonts
 */
public class GeoserverWKTOperationFactory extends PropertyCoordinateOperationFactory {

    private static final String FILENAME = "epsg_operations.properties";

    private volatile Properties processedDefinitions;

    public GeoserverWKTOperationFactory() {
        super(null, MAXIMUM_PRIORITY);
    }

    public GeoserverWKTOperationFactory(Hints userHints) {
        super(userHints, MAXIMUM_PRIORITY);
    }

    @Override
    protected Properties getDefinitions() {
        if (processedDefinitions == null) {
            Properties definitions = super.getDefinitions();
            if (definitions == null) return null;

            // post-process the definitions for backwards compatibility, if the key contains just numbers assume it's
            // an EPSG code
            Properties processed = new Properties();
            for (String key : definitions.stringPropertyNames()) {
                String[] split = key.split("\\s*,\\s*");
                if (split.length == 2) {
                    String source = split[0];
                    String target = split[1];
                    source = qualifyCode(source);
                    target = qualifyCode(target);
                    String newKey = source + "," + target;
                    processed.put(newKey, definitions.getProperty(key));
                } else {
                    processed.put(key, definitions.getProperty(key));
                }
            }
            this.processedDefinitions = processed;
        }
        return processedDefinitions;
    }

    private static @NonNull String qualifyCode(String code) {
        if (!code.contains(":")) {
            code = "EPSG:" + code;
        }
        return code;
    }

    /**
     * Returns the URL to the property file that contains Operation definitions from
     * $GEOSERVER_DATA_DIR/user_projections/{@value #FILENAME}
     *
     * @return The URL, or {@code null} if none.
     */
    @Override
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
                LOGGER.info(definition.path()
                        + " was not found, using the default set of "
                        + "coordinate operation overrides (normally empty)");
            }
        }
        return GeoserverOverridingWKTFactory.class.getResource(FILENAME);
    }
}
