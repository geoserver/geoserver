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
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.api.filter.expression.Expression;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * AutopopulateTemplate class is used to load the properties from the file and store them in a map for further use.
 *
 * <p>It also provides the methods to get the properties and set the properties.
 *
 * @author Alessio Fabiani, GeoSolutions SRL, alessio.fabiani@geosolutionsgroup.com
 */
public class AutopopulateTemplate {
    /** logger */
    private static final Logger LOGGER = Logging.getLogger(AutopopulateTransactionCallback.class);

    /** The file watcher */
    private final PropertyFileWatcher watcher;

    /**
     * Constructs the template loader.
     *
     * @param templateResource The file path to load the properties from
     */
    public AutopopulateTemplate(Resource templateResource) {
        this.watcher = new PropertyFileWatcher(templateResource);
    }

    /**
     * Load the properties from the file and store them in the map.
     *
     * @return propertiesMap
     */
    private Map<String, String> loadProperties() {
        HashMap<String, String> propertiesMap = new HashMap<>();
        try {
            Properties properties = this.watcher.getProperties();
            if (properties == null) {
                LOGGER.warning("Unable to load the properties file: " + this.watcher.getFile());
            } else {
                for (String key : properties.stringPropertyNames()) {
                    String expression = properties.getProperty(key);
                    propertiesMap.put(key, expression);

                    // First check on the Syntax of the expression
                    try {
                        propertiesMap.put(key, getValue(expression));
                    } catch (CQLException e) {
                        LOGGER.warning("Unable to parse the following Expression" + e.getSyntaxError());
                    }
                }
            }
        } catch (IOException e) {
            // Handle file loading error here
            LOGGER.severe("Unable to load the properties file: " + e.getMessage());
            throw new RuntimeException("Unable to load the properties file: " + e.getMessage());
        }
        return propertiesMap;
    }

    private static String getValue(String expression) throws CQLException {
        Expression ecql = ECQL.toExpression(expression);
        String value = ecql.evaluate(null, String.class);
        if (value == null && expression.contains("GSUSER")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                value = auth.getName();
            }
        }
        return value;
    }

    /**
     * Get all the properties from the map.
     *
     * @return The properties map
     */
    public Map<String, String> getAllProperties() {
        return loadProperties();
    }

    /**
     * Check if the template file has been modified on the filesystem.
     *
     * @return true if the template file has been modified and must be reloaded, false otherwise
     */
    public boolean needsReload() {
        return watcher.isStale();
    }

    @Override
    public String toString() {
        return "AutopopulateTemplate{" + "file=" + watcher.getFile().getAbsolutePath() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AutopopulateTemplate)) return false;
        AutopopulateTemplate that = (AutopopulateTemplate) o;
        return Objects.equals(
                watcher.getFile().getAbsolutePath(), that.watcher.getFile().getAbsolutePath());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(watcher.getFile().getAbsolutePath());
    }
}
