/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geotools.util.logging.Logging;

/**
 * Listens for GeoServer startup to configure logging redirection to LOG4J, then redirect LOG4J
 * output according to the GeoServer configuration files (provided logging control hasn't been
 * disabled).
 *
 * <p>Results are recorded by:
 *
 * <ul>
 *   <li>{@link LoggingUtils#relinquishLog4jControl}
 * </ul>
 */
public class LoggingStartupContextListener implements ServletContextListener {

    /** Logger configured after logging policy established. */
    static Logger LOGGER;

    @Override
    public void contextDestroyed(ServletContextEvent event) {}

    @Override
    public void contextInitialized(ServletContextEvent event) {

        // setup GeoTools logging redirection
        // (to log4j by default, although this can be overridden)
        final ServletContext context = event.getServletContext();

        String relinquishLoggingControl =
                GeoServerExtensions.getProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, context);

        LoggingUtils.relinquishLog4jControl = Boolean.valueOf(relinquishLoggingControl);

        if (LoggingUtils.relinquishLog4jControl) {
            getLogger().info("RELINQUISH_LOG4J_CONTROL on, won't attempt to reconfigure LOG4J");
            return;
        }

        String updateBuiltInLoggingProfiles =
                GeoServerExtensions.getProperty(
                        LoggingUtils.UPDATE_BUILT_IN_LOGGING_PROFILES, context);

        LoggingUtils.updateBuiltInLoggingProfiles = Boolean.valueOf(updateBuiltInLoggingProfiles);

        try {
            File baseDir = new File(GeoServerResourceLoader.lookupGeoServerDataDirectory(context));

            GeoServerResourceLoader loader = new GeoServerResourceLoader(baseDir);
            LoggingInfo loginfo = getLogging(loader);

            if (loginfo != null) {
                final String location =
                        LoggingUtils.getLogFileLocation(loginfo.getLocation(), context);

                LoggingUtils.initLogging(
                        loader, loginfo.getLevel(), !loginfo.isStdOutLogging(), false, location);
            } else {
                // check for old style data directory
                File f = loader.find("services.xml");
                if (f != null) {
                    LegacyLoggingImporter loggingImporter = new LegacyLoggingImporter();
                    loggingImporter.imprt(baseDir);

                    final String location =
                            LoggingUtils.getLogFileLocation(loggingImporter.getLogFile(), context);

                    LoggingUtils.initLogging(
                            loader,
                            loggingImporter.getConfigFileName(),
                            loggingImporter.getSuppressStdOutLogging(),
                            false,
                            location);
                } else {
                    getLogger()
                            .log(
                                    Level.WARNING,
                                    "Could not find GeoServer logging.xml (or old services.xml) settings for logging");
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not reconfigure LOG4J loggers", e);
        }
    }

    /**
     * Get the LoggingInfo used at startup before the regular configuration system is available.
     *
     * <p>You probably want {@link org.geoserver.config.GeoServer#getLogging} instead
     *
     * @return LoggingInfo loaded directly from logging.xml. Returns null if logging.xml does not
     *     exist
     */
    public static @Nullable LoggingInfo getLogging(ResourceStore store) throws IOException {
        // Exposing this is a hack to provide JDBCConfig with the information it needs to compute
        // the "change" between logging.xml and the versions stored in JDBC. KS
        // TODO find a better solution than re-initializing on JDBCCOnfig startup.
        Resource f = store.get("logging.xml");
        if (f != null && f.getType() == Resource.Type.RESOURCE) {
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            try (BufferedInputStream in = new BufferedInputStream(f.in())) {
                LoggingInfo loginfo = xp.load(in, LoggingInfo.class);
                return loginfo;
            }
        } else {
            return null;
        }
    }

    /**
     * Lazy creation of {@code org.geoserver.logging} Logger.
     *
     * <p>Configure {@link Logging#ALL} prior to use.
     *
     * @return logger for {@code org.geoserver.logging}
     */
    static Logger getLogger() {
        if (LOGGER == null) {
            LOGGER = Logging.getLogger("org.geoserver.logging");
        }
        return LOGGER;
    }
}
