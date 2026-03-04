/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.pngwind.config.BandMatchingConfig.BandTypeMatcher;
import org.geoserver.pngwind.config.PngWindConfig.DirectionConvention;
import org.geoserver.pngwind.config.PngWindConfig.DirectionUnit;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

/**
 * Loads the PNG-WIND configuration from a properties file (pngwind.properties), first looking in the GeoServer data
 * directory, and then falling back to the classpath.
 */
public final class PngWindConfigLoader {

    private static final Logger LOGGER = Logging.getLogger(PngWindConfigLoader.class);

    public static final String RESOURCE = "pngwind.properties";

    private static final double HARD_DEFAULT_MIN = -25.0;
    private static final double HARD_DEFAULT_MAX = 25.0;

    private PngWindConfigLoader() {}

    /**
     * Loads the PNG-WIND configuration from the GeoServer data directory or classpath.
     *
     * @param loader the GeoServer resource loader to use for loading from the data directory
     * @return the loaded PNG-WIND configuration
     */
    public static PngWindConfig load(GeoServerResourceLoader loader) {
        Properties props = loadProperties(loader);
        BandMatchingConfig matching = new BandMatchingConfig(
                parseMatcher(props, "band.speed"),
                parseMatcher(props, "band.dir"),
                parseMatcher(props, "band.u"),
                parseMatcher(props, "band.v"));

        double defaultMin = readDouble(props, "default.min", HARD_DEFAULT_MIN);
        double defaultMax = readDouble(props, "default.max", HARD_DEFAULT_MAX);

        DirectionConvention convention =
                readEnum(props, "direction.convention", DirectionConvention.class, DirectionConvention.FROM);

        DirectionUnit unit = readEnum(props, "direction.unit", DirectionUnit.class, DirectionUnit.DEG);

        return new PngWindConfig(matching, defaultMin, defaultMax, convention, unit);
    }

    private static Properties loadProperties(GeoServerResourceLoader loader) {
        Properties props = new Properties();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Loading configuration");
        }
        // First try to load from the data directory
        Resource properties = loader.get(RESOURCE);

        if (!Resources.exists(properties)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Properties file not found");
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Copying the default properties file inside the data directory");
            }
            try (InputStream is = PngWindConfigLoader.class.getClassLoader().getResourceAsStream(RESOURCE)) {
                // Copy the default property file into the data directory
                if (is != null) {
                    IOUtils.copy(is, properties.out());
                    properties = loader.get(RESOURCE);
                }
            } catch (IOException e) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Properties file found");
            }
        }

        try (InputStream in = properties.in()) {
            props.load(in);
            LOGGER.info("Loaded PNG-WIND config from GeoServer data directory: " + RESOURCE);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load PNG-WIND config from GeoServer data dir resource: " + RESOURCE, e);
        }
    }

    private static BandTypeMatcher parseMatcher(Properties props, String prefix) {
        return new BandTypeMatcher(
                parseCsv(props.getProperty(prefix + ".exact")), parseCsv(props.getProperty(prefix + ".contains")));
    }

    private static Set<String> parseCsv(String value) {
        Set<String> result = new LinkedHashSet<>();
        if (value == null || value.trim().isEmpty()) {
            return result;
        }

        for (String token : value.split(",")) {
            String s = token.trim().toLowerCase(Locale.ROOT);
            if (!s.isEmpty()) {
                result.add(s);
            }
        }
        return result;
    }

    private static double readDouble(Properties props, String key, double fallback) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static <E extends Enum<E>> E readEnum(Properties props, String key, Class<E> enumClass, E fallback) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
