/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.springframework.web.servlet.mvc.ServletWrappingController;

/**
 * A wrapper for Spring's {@code ServletWrappingController} that configures MapFish printing with GeoServer's data
 * directory.
 *
 * <p>This controller modifies the servlet init parameter "config" so that it points to
 * {@code $GEOSERVER_DATA_DIR/printing/$CONFIG} or another directory, as configured by environment variables or system
 * properties.
 *
 * <ul>
 *   <li>If <b>GEOSERVER_PRINT_CONFIG_DIR</b> is set as an environment variable, that takes precedence.
 *   <li>If <b>GEOSERVER_PRINT_CONFIG_DIR</b> is set as a system property, that is used next.
 *   <li>Otherwise, {@code "printing"} (relative to the GeoServer data directory) is used.
 * </ul>
 *
 * @author Originally by Alan Gerber, The Open Planning Project (2001) Updated & documented by ...
 */
public class PrintingServletWrappingController extends ServletWrappingController {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(PrintingServletWrappingController.class);

    /** Name of environment and system property for the custom config directory. */
    private static final String PRINT_CONFIG_DIR_PROPERTY = "GEOSERVER_PRINT_CONFIG_DIR";

    /** Fallback directory name under the GeoServer data directory if no custom config is set. */
    private static final String DEFAULT_PRINT_DIR = "printing";

    /**
     * Overrides the init parameter "config" to point to the correct printing config path.
     *
     * @param initParameters the original servlet init parameters
     */
    @Override
    public void setInitParameters(Properties initParameters) {
        String configProp = initParameters.getProperty("config");
        if (configProp == null) {
            // If there's no "config" property at all, just pass it on
            LOGGER.warning("No 'config' init parameter was found. MapFish printing servlet may fail.");
            super.setInitParameters(initParameters);
            return;
        }

        try {
            String configPath = findPrintConfigDirectory(configProp);
            initParameters.setProperty("config", configPath);
        } catch (IOException e) {
            LOGGER.warning("IO error while setting up MapFish printing servlet config: " + e.getMessage());
        } catch (Exception e) {
            // Catch other unexpected issues
            LOGGER.warning("Error while setting up MapFish printing servlet config: " + e.getMessage());
        }
        super.setInitParameters(initParameters);
    }

    /**
     * Determines the final absolute path to the printing configuration.
     *
     * <p>The logic is:
     *
     * <ol>
     *   <li>If environment variable {@link #PRINT_CONFIG_DIR_PROPERTY} is set, use that.
     *   <li>Otherwise, if system property {@link #PRINT_CONFIG_DIR_PROPERTY} is set, use that.
     *   <li>Otherwise, use {@link #DEFAULT_PRINT_DIR}.
     * </ol>
     *
     * Then resolve {@code configProp} relative to that directory if necessary. If the resulting resource doesn't exist,
     * copies the {@code default-config.yaml} from classpath.
     *
     * @param configProp the name of the printing config resource (e.g. "config.yaml")
     * @return the absolute path to the printing configuration file
     * @throws IOException if there is an error accessing or creating the file
     */
    private String findPrintConfigDirectory(String configProp) throws IOException {
        // 1) Check environment variable
        String dir = getPrintConfigEnvVariable();

        // 2) Check system property
        if (dir == null) {
            dir = lookupPrintConfigSystemProperty();
        }

        // 3) Use the default if still null
        if (dir == null) {
            dir = DEFAULT_PRINT_DIR;
        } else {
            // Normalize path separators (e.g., backslash vs slash)
            dir = Paths.convert(dir);
        }

        // Combine base dir + the config filename
        String combinedPath = Paths.path(dir, Paths.convert(configProp));

        // Obtain the resource from the loader.
        //   - If 'dir' is absolute, we still can create a Resource that points to an external location
        //     using a small helper or 'Resources.fromPath(...)' if available in your codebase.
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource configResource;
        Path dirPath = Path.of(dir);

        if (dirPath.isAbsolute()) {
            String absoluteFile =
                    dirPath.resolve(configProp).toAbsolutePath().normalize().toString();
            configResource = Resources.fromPath(absoluteFile, null);
        } else {
            // For relative paths, interpret relative to the GeoServer data directory
            configResource = loader.get(combinedPath);
        }

        // 4) If resource does not exist or is not readable, copy default-config.yaml from classpath
        if (configResource.getType() == Resource.Type.UNDEFINED || !Resources.canRead(configResource)) {
            if (!Resources.canRead(configResource)) {
                LOGGER.warning("Printing configuration resource is undefined or not readable: "
                        + configResource.path()
                        + " . Copying default-config.yaml from classpath.");
            }
            try (InputStream defaultConfigStream = getClass().getResourceAsStream("default-config.yaml")) {
                if (defaultConfigStream == null) {
                    LOGGER.warning("default-config.yaml not found in the classpath!");
                } else {
                    IOUtils.copy(defaultConfigStream, configResource.out());
                    LOGGER.info("default-config.yaml copied to " + configResource.path());
                }
            }
        }

        // 5) Return the absolute path of the underlying file
        return configResource.file().getAbsolutePath();
    }

    protected String lookupPrintConfigSystemProperty() {
        return System.getProperty(PRINT_CONFIG_DIR_PROPERTY);
    }

    protected String getPrintConfigEnvVariable() {
        return System.getenv(PRINT_CONFIG_DIR_PROPERTY);
    }
}
