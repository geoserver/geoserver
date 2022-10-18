/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.geoserver.config.LoggingInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.vfny.geoserver.global.ConfigurationException;

/**
 * Used to manage GeoServer logging facilities.
 *
 * <p>GeoTools logging is configured following {@link GeoToolsLoggingRedirection} policy, mapping
 * {@link #GT2_LOGGING_REDIRECTION} policy to appropriate LoggingFactory responsible for routing
 * java util logging api.
 *
 * <p>Use {@code -DGT2_LOGGING_REDIRECTION=Log4j} {@code -DRELINQUISH_LOG4J_CONTROL=true} {@code
 * -Dlog4j.configuration=log4j.properties} to redirect GeoServer to use LOG4J API with
 * log4j.properties configuration.
 *
 * <p>Prior to GeoSerer 2.21 the LOG4J library was included as the application default, to maintain
 * an older data directory use {@code -DGT2_LOGGING_REDIRECTION=Log4j2
 * -DRELINQUISH_LOG4J_CONTROL=true -Dlog4j.configuration=CUSTOM_LOGGING.properties}. This forces
 * GeoServer to read your {@code CUSTOM_LOGGING.properties} Log4J 1.2 configuration (via Log4J
 * bridge).
 *
 * <p>To troubleshoot use {@code org.apache.logging.log4j.simplelog.StatusLogger.level=DEBUG}, or
 * adjust {@code log4j2.xml} file {@code <Configuration status="trace">}.
 *
 * @see org.geoserver.config.LoggingInfo
 */
public class LoggingUtils {

    /**
     * Property used to disable {@link LoggingInfo#getLevel()}, when managing logging configuration
     * manually.
     *
     * <p>Successful use of this setting requires selection {@link #GT2_LOGGING_REDIRECTION} policy,
     * and logging configuration.
     */
    public static final String RELINQUISH_LOG4J_CONTROL = "RELINQUISH_LOG4J_CONTROL";

    /** Logging redirection value established by {@link LoggingStartupContextListener}. */
    static boolean relinquishLog4jControl = false;

    /**
     * Property used to enable update of built-in logging profiles on startup.
     *
     * <p>Use {@code -DUPDATE_BUILT_IN_LOGGING_PROFILES=true} to allow GeoServer to update built-in
     * logging profiles during startup.
     */
    public static final String UPDATE_BUILT_IN_LOGGING_PROFILES =
            "UPDATE_BUILT_IN_LOGGING_PROFILES";

    /** Logging profile update policy established by {@link LoggingStartupContextListener}. */
    static boolean updateBuiltInLoggingProfiles = false;

    /**
     * Property used override Log4J default, and force use of another logging framework.
     *
     * <p>Successful use of this setting requires use of {@link #RELINQUISH_LOG4J_CONTROL}, and a
     * custom WAR with the logging framework jars used. Use {@code
     * -DGT2_LOGGING_REDIRECTION=Logback} {@code -DRELINQUISH_LOG4J_CONTROL=true} {@code
     * -Dlog4j.configuration=logback.xml} to redirect GeoServer to use SLF4J API with logback.xml
     * configuration.
     */
    public static final String GT2_LOGGING_REDIRECTION = "GT2_LOGGING_REDIRECTION";

    /** Flag used to override {@link LoggingInfo#getLocation()} */
    public static final String GEOSERVER_LOG_LOCATION = "GEOSERVER_LOG_LOCATION";

    /** Policy settings for {@link LoggingUtils#GEOSERVER_LOG_LOCATION} configuration. */
    public static enum GeoToolsLoggingRedirection {
        Logback,
        JavaLogging,
        CommonsLogging,
        Log4J,
        Log4J2;

        /**
         * Returns the enum value corresponding to the name (using case insensitive comparison) or
         * Log4J2 if no match is found.
         */
        public static GeoToolsLoggingRedirection findValue(String name) {
            for (GeoToolsLoggingRedirection value : values()) {
                if (value.name().equalsIgnoreCase(name)) return value;
            }
            return Log4J2;
        }
    }

    /** Built-in logging configurations. */
    public static final String[] STANDARD_LOGGING_CONFIGURATIONS = {
        "DEFAULT_LOGGING",
        "GEOSERVER_DEVELOPER_LOGGING",
        "GEOTOOLS_DEVELOPER_LOGGING",
        "PRODUCTION_LOGGING",
        "QUIET_LOGGING",
        "TEST_LOGGING",
        "VERBOSE_LOGGING",
    };

    /** Default logging if configResource unavailable */
    public static void configureGeoServerLogging(String level) {}

    /**
     * Reconfigures GeoServer logging using the provided loggingConfigStream, which is interpreted
     * as a property file or xml.
     *
     * <p>A number of overrides are provided to postprocess indicated configuration.
     *
     * <p>The actual implementation was moved into a separate class to prevent unnecessarily loading
     * any log4j-core classes from LoggingUtils when RELINQUISH_LOG4J_CONTROL is true which can
     * potentially interfere with the application server's logging.
     *
     * @param loader GeoServerResource loader used to access directory for logFileName
     * @param configResource Logging configuration (to be read as property file or xml)
     * @param suppressStdOutLogging Flag requesting that standard output logging be suppressed
     * @param suppressFileLogging Flag indicating that file output should be suppressed
     * @param logFileName Logfile name override (replacing geoserver.log default)
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ConfigurationException
     */
    public static void configureGeoServerLogging(
            GeoServerResourceLoader loader,
            Resource configResource,
            boolean suppressStdOutLogging,
            boolean suppressFileLogging,
            String logFileName)
            throws FileNotFoundException, IOException, ConfigurationException {
        if (!relinquishLog4jControl) {
            LoggingUtilsDelegate.configureGeoServerLogging(
                    loader,
                    configResource,
                    suppressStdOutLogging,
                    suppressFileLogging,
                    logFileName);
        }
    }

    /**
     * Initial setup of the logging system and handling of built-in configuration.
     *
     * <p>A number of overrides are provided to postprocess indicated configuration.
     *
     * <p>The actual implementation was moved into a separate class to prevent unnecessarily loading
     * any log4j-core classes from LoggingUtils when RELINQUISH_LOG4J_CONTROL is true which can
     * potentially interfere with the application server's logging.
     *
     * @param resourceLoader GeoServerResource loader used to access directory for logFileName
     * @param configFileName Logging configuration filename (default is "DEFAULT_LOGGING");
     * @param suppressStdOutLogging Flag requesting that standard output logging be * suppressed
     *     param boolean suppressFileLogging
     * @param logFileName Logfile name override (replacing geoserver.log default)
     * @throws Exception
     */
    public static void initLogging(
            GeoServerResourceLoader resourceLoader,
            String configFileName,
            boolean suppressStdOutLogging,
            boolean suppressFileLogging,
            String logFileName)
            throws Exception {
        if (!relinquishLog4jControl) {
            LoggingUtilsDelegate.initLogging(
                    resourceLoader,
                    configFileName,
                    suppressStdOutLogging,
                    suppressFileLogging,
                    logFileName);
        }
    }

    /**
     * Used by modules to register additional built-in logging profiles during startup, confirming
     * logConfigFile is available (and updating if needed).
     *
     * <p>This method will check resource loader logConfigFile profile against the internal
     * templates and only update the xml file if needed. If the file is updated the previous
     * definition is available as a {@code xml.bak} file allowing.
     *
     * @param resourceLoader GeoServer resource access
     * @param logConfigFile Logging profile matching a built-in template on the classpath
     */
    public static void checkBuiltInLoggingConfiguration(
            GeoServerResourceLoader resourceLoader, String logConfigFile) {
        if (!relinquishLog4jControl) {
            LoggingUtilsDelegate.checkBuiltInLoggingConfiguration(resourceLoader, logConfigFile);
        }
    }
    /**
     * Finds the log location in the "context" (system variable, env variable, servlet context) or
     * uses the provided base location otherwise
     */
    public static String getLogFileLocation(String baseLocation) {
        return getLogFileLocation(baseLocation, null);
    }

    /**
     * Finds the log location in the "context" (system variable, env variable, servlet context) or
     * uses the provided base location otherwise.
     *
     * <p>This method accepts a servlet context directly for cases where the logging location must
     * be known but the spring application context may not be initialized yet.
     */
    public static String getLogFileLocation(String baseLocation, ServletContext context) {
        // accept a servlet context directly in the case of startup where the application context
        // is not yet available, in other cases (like a logging change) we can fall back on the
        // app context and derive the servlet context from that
        String location =
                context != null
                        ? GeoServerExtensions.getProperty(
                                LoggingUtils.GEOSERVER_LOG_LOCATION, context)
                        : GeoServerExtensions.getProperty(LoggingUtils.GEOSERVER_LOG_LOCATION);
        if (location == null) {
            return baseLocation;
        } else {
            return location;
        }
    }
}
