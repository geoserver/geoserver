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
import org.geoserver.logging.LoggingUtils.GeoToolsLoggingRedirection;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geotools.util.logging.CommonsLoggerFactory;
import org.geotools.util.logging.Log4JLoggerFactory;
import org.geotools.util.logging.Logging;

/**
 * Listens for GeoServer startup and tries to configure logging redirection to LOG4J, then
 * configures LOG4J according to the GeoServer configuration files (provided logging control hasn't
 * been disabled)
 */
public class LoggingStartupContextListener implements ServletContextListener {
    private static Logger LOGGER;

    public void contextDestroyed(ServletContextEvent event) {}

    public void contextInitialized(ServletContextEvent event) {
        // setup GeoTools logging redirection (to log4j by default, but so that it can be
        // overridden)
        final ServletContext context = event.getServletContext();
        GeoToolsLoggingRedirection logging =
                GeoToolsLoggingRedirection.findValue(
                        GeoServerExtensions.getProperty(
                                LoggingUtils.GT2_LOGGING_REDIRECTION, context));
        try {
            if (logging == GeoToolsLoggingRedirection.CommonsLogging) {
                Logging.ALL.setLoggerFactory(CommonsLoggerFactory.getInstance());
            } else if (logging != GeoToolsLoggingRedirection.JavaLogging) {
                Logging.ALL.setLoggerFactory(Log4JLoggerFactory.getInstance());
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not configure log4j logging redirection", e);
        }

        String relinquishLoggingControl =
                GeoServerExtensions.getProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, context);
        if (Boolean.valueOf(relinquishLoggingControl)) {
            getLogger()
                    .info(
                            "RELINQUISH_LOG4J_CONTROL on, won't attempt to reconfigure LOG4J loggers");
        } else {
            try {
                File baseDir =
                        new File(GeoServerResourceLoader.lookupGeoServerDataDirectory(context));
                GeoServerResourceLoader loader = new GeoServerResourceLoader(baseDir);

                LoggingInfo loginfo = getLogging(loader);
                if (loginfo != null) {
                    final String location =
                            LoggingUtils.getLogFileLocation(
                                    loginfo.getLocation(), event.getServletContext());
                    LoggingUtils.initLogging(
                            loader, loginfo.getLevel(), !loginfo.isStdOutLogging(), location);
                } else {
                    // check for old style data directory
                    File f = loader.find("services.xml");
                    if (f != null) {
                        LegacyLoggingImporter loggingImporter = new LegacyLoggingImporter();
                        loggingImporter.imprt(baseDir);
                        final String location =
                                LoggingUtils.getLogFileLocation(loggingImporter.getLogFile(), null);
                        LoggingUtils.initLogging(
                                loader,
                                loggingImporter.getConfigFileName(),
                                loggingImporter.getSuppressStdOutLogging(),
                                location);
                    } else {
                        getLogger()
                                .log(
                                        Level.WARNING,
                                        "Could not find configuration file for logging");
                    }
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Could not configure log4j overrides", e);
            }
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
        if (f != null) {
            XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
            try (BufferedInputStream in = new BufferedInputStream(f.in())) {
                LoggingInfo loginfo = xp.load(in, LoggingInfo.class);
                return loginfo;
            }
        } else {
            return null;
        }
    }

    Logger getLogger() {
        if (LOGGER == null) {
            LOGGER = Logging.getLogger("org.geoserver.logging");
        }
        return LOGGER;
    }
}
