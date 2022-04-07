/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * Override Log4J XML configuration processing to apply GeoServer settings for suppressing console,
 * suppressing file output, or providing location for file output.
 */
@Plugin(name = "GeoServerXMLConfigurationFactory", category = "ConfigurationFactory")
@Order(10)
public class GeoServerXMLConfigurationFactory extends ConfigurationFactory {
    /** Valid file extensions for XML files. */
    public static final String[] SUFFIXES = new String[] {".xml", "*"};

    public GeoServerXMLConfigurationFactory() {
        // tmp for breakpoints
        LOGGER.info("GeoServerXMLConfigurationFactory created");
    }
    /**
     * Returns the file suffixes for XML files.
     *
     * @return An array of File extensions.
     */
    public String[] getSupportedTypes() {
        return SUFFIXES;
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
        LOGGER.info("GeoServerXMLConfigurationFactory created for ", loggerContext, source);
        return new GeoServerXMLConfiguration(loggerContext, source);
    }
}
