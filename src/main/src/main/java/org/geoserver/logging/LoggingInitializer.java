/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import java.util.List;
import javax.servlet.ServletContext;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;

/**
 * Initializes GeoServer logging functionality based on configuration settings.
 *
 * <p>This initializer is responsible for configuring {@link LoggingUtils}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class LoggingInitializer
        implements GeoServerInitializer, ApplicationContextAware, GeoServerLifecycleHandler {

    @Override
    public void onReset() {}

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {}

    @Override
    public void onReload() {

        LoggingInfo previousLogging = listener.getCurrentLogging();
        LoggingInfo newLogging = geoServer.getLogging();

        if (previousLogging != null && !previousLogging.equals(newLogging)) {
            // No need to re-init logging when nothing changed
            try {
                String logLocation =
                        LoggingUtils.getLogFileLocation(newLogging.getLocation(), servletContext);

                LoggingUtils.initLogging(
                        resourceLoader,
                        newLogging.getLevel(),
                        !newLogging.isStdOutLogging(),
                        false,
                        logLocation);

                newLogging.setLocation(logLocation);
                listener.setCurrentLogging(newLogging);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** A {@link ConfigurationListenerAdapter} taking care of the Logging changes. */
    public static class LoggingListener extends ConfigurationListenerAdapter {
        private final GeoServerResourceLoader resourceLoader;
        private LoggingInfo currentLogging = new LoggingInfoImpl();
        private ServletContext servletContext;

        public LoggingListener(
                GeoServerResourceLoader resourceLoader, ServletContext servletContext) {
            super();
            this.resourceLoader = resourceLoader;
            this.servletContext = servletContext;
        }

        /**
         * The current logging configuration, used as a reference to detect change.
         *
         * @return current logging configuration, used as reference to detect change
         */
        public LoggingInfo getCurrentLogging() {
            return currentLogging;
        }

        /**
         * Record of the current logging configuration, used as reference to detect change.
         *
         * @param currentLogging current logging configuration, used as a reference to detect change
         */
        public void setCurrentLogging(LoggingInfo currentLogging) {
            this.currentLogging = currentLogging;
        }

        @Override
        public void handleLoggingChange(
                LoggingInfo logging,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {

            this.currentLogging = logging;

            boolean reload = false;

            String loggingProfile = logging.getLevel();
            String loggingLocation = logging.getLocation();
            Boolean stdOutLogging = logging.isStdOutLogging();

            if (propertyNames.contains("level")) {
                loggingProfile = (String) newValues.get(propertyNames.indexOf("level"));
                reload = true;
            }
            if (propertyNames.contains("location")) {
                loggingLocation = (String) newValues.get(propertyNames.indexOf("location"));
                reload = true;
            }
            if (propertyNames.contains("stdOutLogging")) {
                stdOutLogging = (Boolean) newValues.get(propertyNames.indexOf("stdOutLogging"));
                reload = true;
            }

            // maintain the system variable overlay
            loggingLocation = LoggingUtils.getLogFileLocation(loggingLocation, servletContext);

            if (reload) {
                try {
                    LoggingUtils.initLogging(
                            resourceLoader, loggingProfile, !stdOutLogging, false, loggingLocation);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    ServletContext servletContext;

    GeoServerResourceLoader resourceLoader;

    LoggingListener listener;

    GeoServer geoServer;

    /**
     * Acquire resourceLoader to look up logging configuration file.
     *
     * @param resourceLoader
     */
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Acquire geoServer settings to look up logging settings.
     *
     * @param geoServer
     */
    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof WebApplicationContext) {
            servletContext = ((WebApplicationContext) applicationContext).getServletContext();
        }
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        if (!LoggingUtils.relinquishLog4jControl) {
            listener = new LoggingListener(resourceLoader, servletContext);
            geoServer.addListener(listener);
        }
    }
}
