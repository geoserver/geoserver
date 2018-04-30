/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.PropertyFileWatcher;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic property file based {@link DownloadServiceConfigurationGenerator} implementation with ability to reload config when the file changes. If
 * property file is not present, a new one will be created.
 * 
 * @author Simone Giannecchini, GeoSolutions
 */
public class DownloadServiceConfigurationWatcher extends TimerTask implements
        DownloadServiceConfigurationGenerator {

    public static final String DOWNLOAD_PROCESS_DIR = "download-process";

    public static final String PROPERTYFILENAME = "download.properties";

    public final static String DEFAULT_PROPERTY_PATH = Paths.path(DOWNLOAD_PROCESS_DIR,         
            PROPERTYFILENAME);

    public final static Logger LOGGER = Logging
            .getLogger(DownloadServiceConfigurationWatcher.class);

    /**
     * {@link PropertyFileWatcher} used for loading the property file.
     */
    private PropertyFileWatcher watcher;

    /**
     * time in seconds between successive task executions
     */
    private long period = 60 * 2;

    /**
     * delay in seconds before task is to be executed
     */
    private long delay = 60 * 2;

    /**
     * The new {@link DownloadServiceConfiguration} object containing the properties load from the properties file.
     */
    private DownloadServiceConfiguration configuration = new DownloadServiceConfiguration();

    /**
     * {@link Timer} object used for periodically watching the properties file
     */
    private Timer timer;

    /** Default watches controlflow.properties */
    public DownloadServiceConfigurationWatcher() {
        // Get the Resource loader
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        // Check if the property file is present
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Checking properties file");
        }
        Resource properties = loader.get(PROPERTYFILENAME);
        // Properties file not found. A new one is copied into the GeoServer data directory
        if (properties == null || !Resources.exists(properties)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Properties file not found");
            }
            try {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE,
                            "Copying the default properties file inside the data directory");
                }
                // Copy the default property file into the data directory
                InputStream is = DownloadServiceConfigurationWatcher.class
                        .getResourceAsStream(DEFAULT_PROPERTY_PATH);
                if (is != null) {
                    IOUtils.copy(is, properties.out());
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
        Resource downloadProperties = loader.get(PROPERTYFILENAME);
        init(new PropertyFileWatcher(downloadProperties));
    }

    /**
     * Initialization method for loading the {@link DownloadServiceConfiguration}.
     * 
     * @param propertyFileWatcher Watcher of the property file
     */
    private void init(PropertyFileWatcher propertyFileWatcher) {
        Utilities.ensureNonNull("propertyFileWatcher", propertyFileWatcher);
        // Loading configuration from the file
        this.watcher = propertyFileWatcher;
        DownloadServiceConfiguration newConfiguration = loadConfiguration();
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
     * @return an instance of {@link DownloadServiceConfiguration}.
     */
    private DownloadServiceConfiguration loadConfiguration() {
        // load download Process Properties
        final Resource file = watcher.getResource();
        DownloadServiceConfiguration newConfiguration = null;
        try {
            if (Resources.exists(file) && Resources.canRead(file)) {
                // load contents
                Properties properties = watcher.getProperties();

                // parse contents
                newConfiguration = parseConfigurationValues(properties);
            } else {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info("Unable to read confguration file for download service: "
                            + file.path()
                            + " continuing with default configuration-->\n" + configuration);
                }
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, e.getLocalizedMessage(), e);
            }
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Unable to read confguration file for download service: "
                        + file.path() + " continuing with default configuration-->\n"
                        + configuration);
            }
        }
        // return
        return newConfiguration;

    }

    /**
     * Parses the properties file for the download process configuration. When it runs into problems it uses default values
     * 
     * @param downloadProcessProperties the {@link Properties} file to parse. Cannot be null.
     * @return an instance of {@link DownloadServiceConfiguration}.
     */
    private DownloadServiceConfiguration parseConfigurationValues(
            Properties downloadProcessProperties) {
        Utilities.ensureNonNull("downloadProcessProperties", downloadProcessProperties);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Parsing the properties file");
        }
        // Initialize the configuration fields with default values
        long maxFeatures = getLongValue(downloadProcessProperties, DownloadServiceConfiguration.MAX_FEATURES_NAME, DownloadServiceConfiguration.DEFAULT_MAX_FEATURES);
        long rasterSizeLimits = getLongValue(downloadProcessProperties, DownloadServiceConfiguration.RASTER_SIZE_LIMITS_NAME, DownloadServiceConfiguration.DEFAULT_RASTER_SIZE_LIMITS);
        long writeLimits = getLongValue(downloadProcessProperties, DownloadServiceConfiguration.WRITE_LIMITS_NAME, DownloadServiceConfiguration.DEFAULT_RASTER_SIZE_LIMITS);
        long hardOutputLimit = getLongValue(downloadProcessProperties, "hardOutputLimit", DownloadServiceConfiguration.DEFAULT_WRITE_LIMITS);
        int compressionLevel =  getIntValue(downloadProcessProperties, "compressionLevel", DownloadServiceConfiguration.DEFAULT_COMPRESSION_LEVEL);
        int maxFrames =  getIntValue(downloadProcessProperties, DownloadServiceConfiguration.MAX_ANIMATION_FRAMES_NAME, DownloadServiceConfiguration.DEFAULT_MAX_ANIMATION_FRAMES);
        
        // create the configuration object
        return new DownloadServiceConfiguration(maxFeatures, rasterSizeLimits, writeLimits,
                hardOutputLimit, compressionLevel, maxFrames);
    }

    private long getLongValue(Properties properties, String key, long defaultValue) {
        // get value
        String value = (String) properties.get(key);
        if (value != null) {
            // check and assign
            try {
                final long parseLong = Long.parseLong(value);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(key + " parsed to " + parseLong);
                }
                if (parseLong > 0) {
                    return parseLong;
                }
            } catch (NumberFormatException e) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, e.getLocalizedMessage(), e);
                }
            }
        }
        return defaultValue;
    }

    private int getIntValue(Properties properties, String key, int defaultValue) {
        // get value
        String value = (String) properties.get(key);
        if (value != null) {
            // check and assign
            try {
                final int parseInt = Integer.parseInt(value);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(key + " parsed to " + parseInt);
                }
                if (parseInt > 0) {
                    return parseInt;
                }
            } catch (NumberFormatException e) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, e.getLocalizedMessage(), e);
                }
            }
        }
        return defaultValue;
    }

    @Override
    public void run() {
        if (watcher.isStale()) {
            // reload
            DownloadServiceConfiguration newConfiguration = loadConfiguration();
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

    /**
     * Returns the {@link DownloadServiceConfiguration} instance.
     */
    public DownloadServiceConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Stop the configuration watcher.
     */
    public void stop() {
        try {
            timer.cancel();
        } catch (Throwable t) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, t.getLocalizedMessage(), t);
            }
        }
    }
}
