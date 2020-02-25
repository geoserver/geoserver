/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;

/**
 * Basic property file based {@link RemoteProcessFactoryConfigurationGenerator} implementation with
 * ability to reload config when the file changes. If property file is not present, a new one will
 * be created.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class RemoteProcessFactoryConfigurationWatcher extends TimerTask
        implements RemoteProcessFactoryConfigurationGenerator {

    public static final String REMOTE_PROCESS_DIR = "remote-process";

    public static final String PROPERTYFILENAME = "remoteProcess.properties";

    public static final String DEFAULT_PROPERTY_PATH =
            REMOTE_PROCESS_DIR + File.separator + PROPERTYFILENAME;

    /** The LOGGER */
    public static final Logger LOGGER =
            Logging.getLogger(RemoteProcessFactoryConfigurationWatcher.class);

    /** {@link PropertyFileWatcher} used for loading the property file. */
    private PropertyFileWatcher watcher;

    /** time in seconds between successive task executions */
    private long period = 60 * 2;

    /** delay in seconds before task is to be executed */
    private long delay = 60 * 2;

    /**
     * The new {@link RemoteProcessFactoryConfiguration} object containing the properties load from
     * the properties file.
     */
    private RemoteProcessFactoryConfiguration configuration;

    /** {@link Timer} object used for periodically watching the properties file */
    private Timer timer;

    /** Default watches remoteProcess.properties */
    public RemoteProcessFactoryConfigurationWatcher() {
        // Get the Resource loader
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        // Check if the property file is present
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Checking properties file");
        }
        File properties = null;
        try {
            properties = loader.find(PROPERTYFILENAME);
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
        // Properties file not found. A new one is copied into the GeoServer data directory
        if (properties == null || !properties.exists()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Properties file not found");
            }
            try {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                            Level.FINE,
                            "Copying the default properties file inside the data directory");
                }
                // Copy the default property file into the data directory
                URL url =
                        RemoteProcessFactoryConfigurationWatcher.class.getResource(
                                DEFAULT_PROPERTY_PATH);
                if (url != null) {
                    properties = loader.createFile(PROPERTYFILENAME);
                    loader.copyFromClassPath(
                            DEFAULT_PROPERTY_PATH,
                            properties,
                            RemoteProcessFactoryConfigurationWatcher.class);
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
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Loading configuration");
        }
        // Get the Property file as a Resource
        Resource remoteProcessFactoryProperties = loader.get(PROPERTYFILENAME);
        init(new PropertyFileWatcher(remoteProcessFactoryProperties));
    }

    /**
     * Initialization method for loading the {@link RemoteProcessFactoryConfiguration}.
     *
     * @param propertyFileWatcher Watcher of the property file
     */
    private void init(PropertyFileWatcher propertyFileWatcher) {
        Utilities.ensureNonNull("propertyFileWatcher", propertyFileWatcher);
        // Loading configuration from the file
        this.watcher = propertyFileWatcher;
        RemoteProcessFactoryConfiguration newConfiguration = loadConfiguration();
        if (newConfiguration != null) {
            configuration = newConfiguration;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("New configuration loaded:\n" + configuration);
            }
        }

        // start background checks
        timer = new Timer(true);
        timer.scheduleAtFixedRate(this, delay * 1000, period * 1000);
    }

    /**
     * Loads the configuration from disk.
     *
     * @return an instance of {@link RemoteProcessFactoryConfiguration}.
     */
    private RemoteProcessFactoryConfiguration loadConfiguration() {
        // load remote process factory Properties
        final File file = watcher.getFile();
        RemoteProcessFactoryConfiguration newConfiguration = null;
        try {
            if (file.exists() && file.canRead()) {
                // load contents
                Properties properties = watcher.getProperties();

                // parse contents
                newConfiguration = parseConfigurationValues(properties);
            } else {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(
                            "Unable to read confguration file for remote process factory: "
                                    + file.getAbsolutePath()
                                    + " continuing with default configuration-->\n"
                                    + configuration);
                }
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, e.getLocalizedMessage(), e);
            }
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(
                        "Unable to read confguration file for remote process factory: "
                                + file.getAbsolutePath()
                                + " continuing with default configuration-->\n"
                                + configuration);
            }
        }
        // return
        return newConfiguration;
    }

    /**
     * Parses the properties file for the remote process factory configuration. When it runs into
     * problems it uses default values
     *
     * @param remoteProcessFactoryProperties the {@link Properties} file to parse. Cannot be null.
     * @return an instance of {@link RemoteProcessFactoryConfiguration}.
     */
    private RemoteProcessFactoryConfiguration parseConfigurationValues(
            Properties remoteProcessFactoryProperties) {
        Utilities.ensureNonNull("remoteProcessFactoryProperties", remoteProcessFactoryProperties);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Parsing the properties file");
        }
        // Initialize the configuration fields with default values
        long remoteProcessStubCycleSleepTime = RemoteProcessFactoryConfiguration.DEFAULT_SLEEP_TIME;
        Map<String, String> configKvPs = new HashMap<String, String>();

        // Extract the keyset from the property files
        Set<Object> properties = remoteProcessFactoryProperties.keySet();

        // Iterates on the various keys in order to search for the various properies
        for (Object property : properties) {
            String prop = (String) property;
            // remote process sleep time
            if (prop.equalsIgnoreCase(RemoteProcessFactoryConfiguration.DEFAULT_SLEEP_TIME_NAME)) {
                // get value
                String value =
                        (String)
                                remoteProcessFactoryProperties.get(
                                        RemoteProcessFactoryConfiguration.DEFAULT_SLEEP_TIME_NAME);

                // check and assign
                try {
                    final long parseLong = Long.parseLong(value);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("maxFeatures parsed to " + parseLong);
                    }
                    if (parseLong > 0) {
                        remoteProcessStubCycleSleepTime = parseLong;
                    }

                } catch (NumberFormatException e) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.log(Level.INFO, e.getLocalizedMessage(), e);
                    }
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "remoteProcessStubCycleSleepTime assigned to "
                                    + remoteProcessStubCycleSleepTime);
                }
            } else {
                configKvPs.put(prop, remoteProcessFactoryProperties.getProperty(prop));
            }
        }

        // create the configuration object
        return new RemoteProcessFactoryConfiguration(remoteProcessStubCycleSleepTime, configKvPs);
    }

    @Override
    public void run() {
        if (watcher.isStale()) {
            // reload
            RemoteProcessFactoryConfiguration newConfiguration = loadConfiguration();
            if (newConfiguration != null) {
                synchronized (newConfiguration) {
                    configuration = newConfiguration;
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("New configuration loaded:\n" + configuration);
                    }
                }
            }
        }
    }

    /** Stop the configuration watcher. */
    public void stop() {
        try {
            timer.cancel();
        } catch (Throwable t) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, t.getLocalizedMessage(), t);
            }
        }
    }

    @Override
    public RemoteProcessFactoryConfiguration getConfiguration() {
        return configuration;
    }
}
