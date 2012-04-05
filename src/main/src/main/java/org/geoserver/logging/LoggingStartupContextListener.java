/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.geoserver.config.LoggingInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.logging.LoggingUtils.GeoToolsLoggingRedirection;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.CommonsLoggerFactory;
import org.geotools.util.logging.Log4JLoggerFactory;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Listens for GeoServer startup and tries to configure logging redirection to
 * LOG4J, then configures LOG4J according to the GeoServer configuration files
 * (provided logging control hasn't been disabled)
 * 
 */
public class LoggingStartupContextListener implements ServletContextListener {
    private static Logger LOGGER;

    public void contextDestroyed(ServletContextEvent event) {
    }

    public void contextInitialized(ServletContextEvent event) {
        // setup GeoTools logging redirection (to log4j by default, but so that it can be overridden)
        final ServletContext context = event.getServletContext();
        GeoToolsLoggingRedirection logging = GeoToolsLoggingRedirection.findValue(
                GeoServerExtensions.getProperty(LoggingUtils.GT2_LOGGING_REDIRECTION, 
                context));
        try {
            if(logging == GeoToolsLoggingRedirection.JavaLogging) {
                // no redirection needed 
            } else if(logging == GeoToolsLoggingRedirection.CommonsLogging) {
                Logging.ALL.setLoggerFactory(CommonsLoggerFactory.getInstance());
            } else {
                Logging.ALL.setLoggerFactory(Log4JLoggerFactory.getInstance());
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not configure log4j logging redirection", e);
        }
        
        String relinquishLoggingControl = GeoServerExtensions.getProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, 
                context);
        if(Boolean.valueOf(relinquishLoggingControl)) {
            getLogger().info("RELINQUISH_LOG4J_CONTROL on, won't attempt to reconfigure LOG4J loggers");
        } else {
            try {
                File baseDir = new File(GeoserverDataDirectory.findGeoServerDataDir(context));
                GeoServerResourceLoader loader = new GeoServerResourceLoader(baseDir);
                
                File f= loader.find( "logging.xml" );
                if ( f != null ) {
                    XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
                    BufferedInputStream in = new BufferedInputStream( new FileInputStream( f ) );
                    try {
                        LoggingInfo loginfo = xp.load(in,LoggingInfo.class);
                        final String location = LoggingUtils.getLogFileLocation(loginfo.getLocation(), event.getServletContext());
                        LoggingUtils.initLogging(loader, loginfo.getLevel(), !loginfo.isStdOutLogging(),
                            location);
                    }
                    finally {
                        in.close();
                    }
                }
                else {
                    //check for old style data directory
                    f = loader.find( "services.xml" );
                    if ( f != null ) {
                        LegacyLoggingImporter loggingImporter = new LegacyLoggingImporter();
                        loggingImporter.imprt(baseDir);
                        final String location = LoggingUtils.getLogFileLocation(loggingImporter.getLogFile(), null);
                        LoggingUtils.initLogging(loader, loggingImporter.getConfigFileName(), loggingImporter
                                .getSuppressStdOutLogging(), location);
                    }
                    else {
                        getLogger().log(Level.WARNING, "Could not find configuration file for logging");
                    }
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Could not configure log4j overrides", e);
            }
        }
    }

    Logger getLogger() {
        if(LOGGER == null) {
            LOGGER = Logging.getLogger("org.geoserver.logging");
        }
        return LOGGER;
    }
}
