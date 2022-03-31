/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.core.Constants;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

/**
 * Utility class uses to process GeoServer configuration workflow through external environment
 * variables.
 *
 * <p>This class must be used everytime we need to resolve a configuration placeholder at runtime.
 *
 * <p>An instance of this class needs to be registered in spring context as follows.
 *
 * <pre>
 * <code>
 *         &lt;bean id="geoserverEnvironment" class="org.geoserver.GeoServerEnvironment" depends-on="extensions"/&gt;
 * </code>
 * </pre>
 *
 * It must be a singleton, and must not be loaded lazily. Furthermore, this bean must be loaded
 * before any beans that use it.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class GeoServerEnvironment {

    /** logger */
    protected static final Logger LOGGER = Logging.getLogger("org.geoserver.platform");

    private static final Constants constants = new Constants(PlaceholderConfigurerSupport.class);

    /**
     * Variable set via System Environment in order to instruct GeoServer to make use or not of the
     * config placeholders translation.
     *
     * <p>Default to FALSE
     */
    private static volatile boolean allowEnvParametrization =
            Boolean.valueOf(System.getProperty("ALLOW_ENV_PARAMETRIZATION", "false"));

    /**
     * Returns the variable set via System Environment in order to instruct GeoServer to make use or
     * not of the config placeholders translation.
     */
    public static boolean allowEnvParametrization() {
        return allowEnvParametrization;
    }

    /**
     * Reloads the variable set via System Environment in order to instruct GeoServer to make use or
     * not of the config placeholders translation. Use this synchronized method only for testing
     * purposes.
     */
    public static synchronized void reloadAllowEnvParametrization() {
        allowEnvParametrization =
                Boolean.valueOf(System.getProperty("ALLOW_ENV_PARAMETRIZATION", "false"));
    }

    private static final String PROPERTYFILENAME = "geoserver-environment.properties";

    static final String ENV_PROPERTIES_NAME = "ENV_PROPERTIES";
    private final String propertiesPath = GeoServerExtensions.getProperty(ENV_PROPERTIES_NAME);

    private static final String nullValue = "null";

    private final PropertyPlaceholderHelper helper =
            new PropertyPlaceholderHelper(
                    constants.asString("DEFAULT_PLACEHOLDER_PREFIX"),
                    constants.asString("DEFAULT_PLACEHOLDER_SUFFIX"),
                    constants.asString("DEFAULT_VALUE_SEPARATOR"),
                    true);

    private final PlaceholderResolver resolver = name -> resolvePlaceholder(name);

    private FileWatcher<Properties> configFile;

    private Properties props;

    /**
     * Internal "props" getter method.
     *
     * @return the props
     */
    public Properties getProps() {
        return props;
    }

    public GeoServerEnvironment() {
        try {
            this.configFile = getEnvironmentProperties(propertiesPath);
            props = configFile.read();
        } catch (Exception e) {
            String file = propertiesPath != null ? propertiesPath : PROPERTYFILENAME;
            LOGGER.log(Level.WARNING, "Could not find any '" + file + "' property file.", e);
            props = new Properties();
        }
    }

    private FileWatcher<Properties> getEnvironmentProperties(String propertiesFilePath) {
        if (propertiesFilePath != null && !"".equals(propertiesFilePath)) {
            File propertyFile = new File(propertiesFilePath);
            if (propertyFile.exists()) {
                return loadGeoServerEnvProps(propertyFile);
            } else {
                String message =
                        "File "
                                + propertiesPath
                                + " not found. Trying to load the environment properties from GeoServer datadir.";
                LOGGER.warning(message);
            }
        }
        return loadGeoServerEnvProps(PROPERTYFILENAME);
    }

    private FileWatcher<Properties> loadGeoServerEnvProps(File propertyFile) {
        return loadGeoServerEnvProps(Files.asResource(propertyFile));
    }

    private FileWatcher<Properties> loadGeoServerEnvProps(String propertyFile) {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Resource resource = loader.get(propertyFile);
        return loadGeoServerEnvProps(resource);
    }

    private FileWatcher<Properties> loadGeoServerEnvProps(Resource propertyFile) {
        return new FileWatcher<Properties>(propertyFile) {

            @Override
            protected Properties parseFileContents(InputStream in) throws IOException {
                Properties p = new Properties();
                p.load(in);
                return p;
            }
        };
    }

    protected String resolvePlaceholder(String placeholder) {
        String propVal = resolveSystemProperty(placeholder);

        if (configFile != null && configFile.isModified()) {
            try {
                props = configFile.read();
            } catch (IOException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Could not find any '" + PROPERTYFILENAME + "' property file.",
                        e);
                props = new Properties();
            }
        }

        if (props != null && propVal == null) {
            propVal = props.getProperty(placeholder);
        }

        return propVal;
    }

    protected String resolveSystemProperty(String key) {
        try {
            String value = System.getProperty(key);
            if (value == null) {
                value = System.getenv(key);
            }
            return value;
        } catch (Throwable ex) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Could not access system property '" + key + "': " + ex);
            }
            return null;
        }
    }

    protected String resolveStringValue(String strVal) throws BeansException {
        String resolved = this.helper.replacePlaceholders(strVal, this.resolver);

        return (resolved.equals(nullValue) ? null : resolved);
    }

    /**
     * Translates placeholders in the form of Spring Property placemark ${...} into their real
     * values.
     *
     * <p>The method first looks for System variables which take precedence on local ones, then into
     * internal props loaded from configuration file 'geoserver-environment.properties'.
     */
    public Object resolveValue(Object value) {
        if (value != null) {
            if (value instanceof String) {
                return resolveStringValue((String) value);
            }
        }

        return value;
    }

    /**
     * Returns 'false' whenever the configuration file 'geoserver-environment.properties' has
     * changed.
     */
    public boolean isStale() {
        return this.configFile != null && this.configFile.isModified();
    }
}
