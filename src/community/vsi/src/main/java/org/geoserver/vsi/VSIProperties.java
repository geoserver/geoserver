/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.vsi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.gdal.gdal.gdal;

/**
 * GDAL config loader
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public final class VSIProperties {

    // Name of property that defines where the vsi.properties file is
    public static final String LOCATION_PROPERTY = "vsi.properties.location";

    private static final Logger LOGGER = Logger.getLogger(VSIProperties.class.getName());

    /**
     * Read the vsi.properties file for credentials.
     *
     * @return Validated Properties object of key-value pairs
     * @throws IOException
     */
    public static Properties getProperties() throws IOException {
        final String propLocation = System.getProperty(LOCATION_PROPERTY);

        if (propLocation == null) {
            throw new IOException("The system property 'vsi.properties.location' is not set.");
        }

        final Properties prop = new Properties();

        try (FileInputStream inStream = new FileInputStream(propLocation)) {
            prop.load(inStream);
        } catch (FileNotFoundException ex) {
            throw new FileNotFoundException(
                    "The system property 'vsi.properties.location' is set but "
                            + propLocation
                            + " does not exist.");
        } catch (IOException ex) {
            throw new IOException(
                    "The system property 'vsi.properties.location' is set but "
                            + propLocation
                            + " could not be read.");
        }

        return prop;
    }

    /** Set a configuration option for GDAL and log it */
    private static void setConfigOption(String option, String value) {
        gdal.SetConfigOption(option, value);
        LOGGER.debug((value.equals("") ? "Cleared" : "Set") + " GDAL config option: " + option);
    }

    /**
     * Set the configuration options in GDAL, using credentials found in vsi.properties
     *
     * @throws IOException
     */
    public static void sync() throws IOException {
        final Properties properties = getProperties();

        for (String option : properties.stringPropertyNames()) {
            setConfigOption(option, properties.getProperty(option));
        }
    }
}
