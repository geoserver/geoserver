/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.autopopulate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.api.filter.expression.Expression;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;

/**
 * AutopopulateTemplate class is used to load the properties from the file and store them in a map
 * for further use.
 *
 * <p>It also provides the methods to get the properties and set the properties.
 *
 * @author Alessio Fabiani, GeoSolutions SRL, alessio.fabiani@geosolutionsgroup.com
 */
public class AutopopulateTemplate {
    /** logger */
    private static final Logger LOGGER = Logging.getLogger(AutopopulateTransactionCallback.class);
    /** The properties map */
    private final Map<String, String> propertiesMap;

    /** The file watcher */
    private final PropertyFileWatcher watcher;

    /**
     * Constructs the template loader.
     *
     * @param filePath The file path to load the properties from
     */
    public AutopopulateTemplate(String filePath) {
        this.propertiesMap = new HashMap<>();
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        this.watcher = new PropertyFileWatcher(loader.get(filePath));
        loadProperties(filePath);
    }

    /**
     * Load the properties from the file and store them in the map.
     *
     * @param filePath The file path to load the properties from
     */
    private void loadProperties(String filePath) {
        try {
            Properties properties = this.watcher.getProperties();
            if (properties == null) {
                LOGGER.warning("Unable to load the properties file: " + filePath);
                return;
            } else {
                for (String key : properties.stringPropertyNames()) {
                    String expression = properties.getProperty(key);
                    propertiesMap.put(key, expression);

                    // First check on the Syntax of the expression
                    try {
                        Expression ecql = ECQL.toExpression(expression);
                        if (ecql != null) {
                            propertiesMap.put(key, ecql.evaluate(null, String.class));
                        }
                    } catch (CQLException e) {
                        LOGGER.warning(
                                "Unable to parse the following Expression" + e.getSyntaxError());
                    }
                }
            }
        } catch (IOException e) {
            // Handle file loading error here
            LOGGER.severe("Unable to load the properties file: " + e.getMessage());
            throw new RuntimeException("Unable to load the properties file: " + e.getMessage());
        }
    }

    /**
     * Get the property from the map.
     *
     * @param key The key to get the property
     * @return The property value
     */
    public String getProperty(String key) {
        return propertiesMap.get(key);
    }

    /**
     * Get all the properties from the map.
     *
     * @return The properties map
     */
    public Map<String, String> getAllProperties() {
        return propertiesMap;
    }

    /**
     * Check if the template file has been modified on the filesystem.
     *
     * @return true if the template file has been modified and must be reloaded, false otherwise
     */
    public boolean needsReload() {
        if (watcher != null) return watcher.isStale();
        return true;
    }

    @Override
    public String toString() {
        return "AutopopulateTemplate{" + "propertiesMap=" + propertiesMap + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AutopopulateTemplate)) return false;
        AutopopulateTemplate that = (AutopopulateTemplate) o;
        return Objects.equals(propertiesMap, that.propertiesMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertiesMap);
    }
}
