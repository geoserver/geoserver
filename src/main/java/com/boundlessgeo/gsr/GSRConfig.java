/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr;

import org.geotools.util.logging.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static configuration values used in the api output
 */
public class GSRConfig {

    private static final Logger LOGGER = Logging.getLogger( "com.boundlessgeo.gsr" );
    private static final Properties properties;

    public static final String PRODUCT_NAME;
    public static final String SPEC_VERSION;
    public static final double CURRENT_VERSION;

    static {
        properties = new Properties();
        try (InputStream in = GSRConfig.class.getResourceAsStream("config.properties")) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String productName = properties.getProperty("PRODUCT_NAME");
        String specVersion = properties.getProperty("SPEC_VERSION");
        Double currentVersion = null;
        try {
            currentVersion = Double.parseDouble(properties.getProperty("CURRENT_VERSION"));
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "CURRENT_VERSION could not be parsed to double: " + properties.getProperty("CURRENT_VERSION"), e);
        }

        PRODUCT_NAME = productName == null ? "Boundless Suite" : productName;
        SPEC_VERSION = specVersion == null ? "1.0" : specVersion;
        CURRENT_VERSION = currentVersion == null ? 10.1 : currentVersion;
    }
}
