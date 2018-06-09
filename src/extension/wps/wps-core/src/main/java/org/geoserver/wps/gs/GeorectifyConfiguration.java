/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import com.google.common.collect.Maps;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 */
public class GeorectifyConfiguration implements ApplicationListener {

    // TODO: we should allow change the configuration through the GUI
    // this will also allow to check for updates on config parameters on file changes
    static class GRKeys {
        static final String GDAL_CACHEMAX = "GDAL_CACHEMAX";
        static final String GDAL_DATA = "GDAL_DATA";
        static final String GDAL_WARP_PARAMS = "GDAL_WARP_PARAMS";
        static final String GDAL_TRANSLATE_PARAMS = "GDAL_TRANSLATE_PARAMS";
        static final String GDAL_LOGGING_DIR = "GDAL_LOGGING_DIR";
        static final String TEMP_DIR = "TEMP_DIR";
        static final String EXECUTION_TIMEOUT = "EXECUTION_TIMEOUT";
    }

    static class GRDefaults {
        static final String GDAL_TRANSLATE_COMMAND = "gdal_translate";
        static final String GDAL_WARP_COMMAND = "gdalwarp";
        static final String GDAL_WARPING_PARAMETERS = "-co TILED=yes -wm 64 -multi -dstalpha";
        static final String GDAL_TRANSLATE_PARAMETERS = ""; // -expand rgb";
        static final String TEMP_DIR = SYSTEM_TEMP_DIR;
        static final String LOGGING_DIR = SYSTEM_TEMP_DIR;
        static final Long EXECUTION_TIMEOUT = 180000l;
    }

    private static final Logger LOGGER = Logging.getLogger(GeorectifyConfiguration.class);

    private static final String GDAL_CONFIG_FILE = "gdalops.properties";

    Resource configFile;

    Timer timer;

    public GeorectifyConfiguration() {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        configFile = loader.get(GDAL_CONFIG_FILE);
        timer = new Timer(true);
        timer.schedule(new ConfigurationPoller(), 1000);
    }

    private void loadConfiguration() {
        try {
            if (configFile.getType() == Type.RESOURCE) {
                loadConfig();
            } else {
                tempFolder = initFolder(GRDefaults.TEMP_DIR);
                loggingFolder = initFolder(GRDefaults.LOGGING_DIR);
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(
                        Level.SEVERE,
                        "Unable to configure some of the GeorectifyCoverage process properties.",
                        e);
            }
        }
    }

    private class ConfigurationPoller extends TimerTask {
        Long lastModified = null;

        public ConfigurationPoller() {
            run();
        }

        public void run() {
            long newLastModified = configFile.lastmodified();
            if (lastModified == null || newLastModified != lastModified) {
                lastModified = newLastModified;
                loadConfiguration();
            }
        }
    }

    private static final String SYSTEM_TEMP_DIR = System.getProperty("java.io.tmpdir");

    /** Temporary folder where the gdal ops will create files */
    private File tempFolder;

    /** Temporary folder where to put logging files produced by task executions */
    private File loggingFolder;

    /** Wait this time when executing an ant task to do gdal processing before give up */
    private long executionTimeout = GRDefaults.EXECUTION_TIMEOUT;

    private Map<String, String> envVariables;

    /** Set on this String any parameter used by gdalwarp */
    private String gdalWarpingParameters = GRDefaults.GDAL_WARPING_PARAMETERS;

    /** Set on this String any parameter used by gdal_translate used to set gcps */
    private String gdalTranslateParameters = GRDefaults.GDAL_TRANSLATE_PARAMETERS;

    /**
     * The name of the warping command. It could be either a simple command name like "gdalwarp"
     * (provided it can be found in the PATH) or the full command path.
     */
    private String warpingCommand = GRDefaults.GDAL_WARP_COMMAND;

    /**
     * The name of the gdal translate command. It could be either a simple command name like
     * "gdal_translate" (provided it can be found in the PATH) or the full command path.
     */
    private String translateCommand = GRDefaults.GDAL_TRANSLATE_COMMAND;

    /**
     * Load the configured parameters through the properties file. TODO: Move to XML instead of
     * properties file
     *
     * @throws IOException
     */
    private void loadConfig() throws IOException {
        final boolean hasPropertiesFile =
                configFile != null && configFile.getType() == Type.RESOURCE;

        if (hasPropertiesFile) {
            Properties props = new Properties();
            InputStream fis = null;
            try {
                fis = configFile.in();
                props.load(fis);
                Iterator<Object> keys = props.keySet().iterator();
                envVariables = Maps.newHashMap();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (key.equalsIgnoreCase(GRKeys.GDAL_CACHEMAX)) {
                        // Setting GDAL_CACHE_MAX Environment variable if available
                        String cacheMax = null;
                        try {
                            cacheMax = (String) props.get(GRKeys.GDAL_CACHEMAX);
                            if (cacheMax != null) {
                                int gdalCacheMaxMemory =
                                        Integer.parseInt(cacheMax); // Only for validation
                                envVariables.put(GRKeys.GDAL_CACHEMAX, cacheMax);
                            }
                        } catch (NumberFormatException nfe) {
                            if (LOGGER.isLoggable(Level.WARNING)) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Unable to parse the specified property as a number: "
                                                + cacheMax,
                                        nfe);
                            }
                        }
                    } else if (key.equalsIgnoreCase(GRKeys.GDAL_DATA)
                            || key.equalsIgnoreCase(GRKeys.GDAL_LOGGING_DIR)
                            || key.equalsIgnoreCase(GRKeys.TEMP_DIR)) {
                        // Parsing specified folder path
                        String path = (String) props.get(key);
                        if (path != null) {
                            final File directory = new File(path);
                            if (directory.exists()
                                    && directory.isDirectory()
                                    && ((key.equalsIgnoreCase(GRKeys.GDAL_DATA)
                                                    && directory.canRead())
                                            || directory.canWrite())) {
                                envVariables.put(key, path);
                            } else {
                                if (LOGGER.isLoggable(Level.WARNING)) {
                                    LOGGER.log(
                                            Level.WARNING,
                                            "The specified folder for "
                                                    + key
                                                    + " variable isn't valid, "
                                                    + "or it doesn't exist or it isn't a readable directory or it is a "
                                                    + "destination folder which can't be written: "
                                                    + path);
                                }
                            }
                        }
                    } else if (key.equalsIgnoreCase(GRKeys.EXECUTION_TIMEOUT)) {
                        // Parsing execution timeout
                        String timeout = null;
                        try {
                            timeout = (String) props.get(GRKeys.EXECUTION_TIMEOUT);
                            if (timeout != null) {
                                executionTimeout = Long.parseLong(timeout); // Only for validation
                            }
                        } catch (NumberFormatException nfe) {
                            if (LOGGER.isLoggable(Level.WARNING)) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Unable to parse the specified property as a number: "
                                                + timeout,
                                        nfe);
                            }
                        }
                    } else if (key.equalsIgnoreCase(GRKeys.GDAL_WARP_PARAMS)
                            || key.equalsIgnoreCase(GRKeys.GDAL_TRANSLATE_PARAMS)) {
                        // Parsing gdal operations custom option parameters
                        String param = (String) props.get(key);
                        if (param != null) {
                            if (key.equalsIgnoreCase(GRKeys.GDAL_WARP_PARAMS)) {
                                gdalWarpingParameters = param.trim();
                            } else {
                                gdalTranslateParameters = param.trim();
                            }
                        }
                    } else if (key.endsWith("PATH")) {
                        // Dealing with properties like LD_LIBRARY_PATH, PATH, ...
                        String param = (String) props.get(key);
                        if (param != null) {
                            envVariables.put(key, param);
                        }
                    }
                }

            } catch (FileNotFoundException e) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                            Level.WARNING,
                            "Unable to parse the config file: " + configFile.path(),
                            e);
                }

            } catch (IOException e) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                            Level.WARNING,
                            "Unable to parse the config file: " + configFile.path(),
                            e);
                }
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Throwable t) {
                        // Does nothing
                    }
                }
            }
        }
    }

    /** Make sure the specified folder path exist or try to create it */
    private File initFolder(final String folderPath) throws IOException {
        File tempFolder = new File(folderPath);
        if (!tempFolder.exists()) {
            boolean createdFolder = false;
            try {
                createdFolder = tempFolder.mkdir();
            } catch (SecurityException se) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning(
                            "Unable to create the specified folder: "
                                    + folderPath
                                    + "\nProceeding with using the System temp folder: "
                                    + SYSTEM_TEMP_DIR);
                }
            }
            if (!createdFolder) {
                tempFolder = new File(SYSTEM_TEMP_DIR);
            }
        }
        if (!tempFolder.exists() || !tempFolder.canWrite()) {
            throw new IOException(
                    "Unable to write on the specified folder: " + tempFolder.getAbsolutePath());
        }
        return tempFolder;
    }

    public File getTempFolder() {
        return tempFolder;
    }

    public void setTempFolder(File tempFolder) {
        this.tempFolder = tempFolder;
    }

    public File getLoggingFolder() {
        return loggingFolder;
    }

    public void setLoggingFolder(File loggingFolder) {
        this.loggingFolder = loggingFolder;
    }

    public long getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(long executionTimeout) {
        this.executionTimeout = executionTimeout;
    }

    public Map<String, String> getEnvVariables() {
        return envVariables;
    }

    public void setEnvVariables(Map<String, String> envVariables) {
        this.envVariables = envVariables;
    }

    public String getWarpingCommand() {
        return warpingCommand;
    }

    public void setWarpingCommand(String warpingCommand) {
        this.warpingCommand = warpingCommand;
    }

    public String getTranslateCommand() {
        return translateCommand;
    }

    public void setTranslateCommand(String translateCommand) {
        this.translateCommand = translateCommand;
    }

    public String getGdalWarpingParameters() {
        return gdalWarpingParameters;
    }

    public String getGdalTranslateParameters() {
        return gdalTranslateParameters;
    }

    /** Kill all threads on web app context shutdown to avoid permgen leaks */
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextClosedEvent) {
            timer.cancel();
        }
    }
}
