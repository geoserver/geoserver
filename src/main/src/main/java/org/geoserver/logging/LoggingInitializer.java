/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import java.util.List;
import java.util.logging.Logger;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Initializes GeoServer logging functionality based on configuration settings.
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
                String logLocation = LoggingUtils.getLogFileLocation(newLogging.getLocation());
                LoggingUtils.initLogging(
                        resourceLoader,
                        newLogging.getLevel(),
                        !newLogging.isStdOutLogging(),
                        logLocation);
                newLogging.setLocation(logLocation);
                listener.setCurrentLogging(newLogging);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** A {@link ConfigurationListenerAdapter} taking care of the Logging changes */
    public static class LoggingListener extends ConfigurationListenerAdapter {
        private final GeoServerResourceLoader resourceLoader;
        private final boolean relinquishLoggingControl;
        private LoggingInfo currentLogging = new LoggingInfoImpl();

        public LoggingListener(
                GeoServerResourceLoader resourceLoader, boolean relinquishLoggingControl) {
            super();
            this.resourceLoader = resourceLoader;
            this.relinquishLoggingControl = relinquishLoggingControl;
        }

        public LoggingInfo getCurrentLogging() {
            return currentLogging;
        }

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
            // TODO: get rid of this hack checking singleton
            if (!relinquishLoggingControl) {
                boolean reload = false;
                String loggingLevel = logging.getLevel();
                String loggingLocation = logging.getLocation();
                Boolean stdOutLogging = logging.isStdOutLogging();

                if (propertyNames.contains("level")) {
                    loggingLevel = (String) newValues.get(propertyNames.indexOf("level"));
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
                loggingLocation = LoggingUtils.getLogFileLocation(loggingLocation);

                if (reload) {
                    try {
                        LoggingUtils.initLogging(
                                resourceLoader, loggingLevel, !stdOutLogging, loggingLocation);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /** logging instance */
    static Logger LOGGER = Logging.getLogger("org.geoserver.logging");

    GeoServerResourceLoader resourceLoader;

    Boolean relinquishLoggingControl;

    LoggingListener listener;

    GeoServer geoServer;

    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        listener = new LoggingListener(resourceLoader, relinquishLoggingControl);
        geoServer.addListener(listener);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String strValue =
                GeoServerExtensions.getProperty(
                        LoggingUtils.RELINQUISH_LOG4J_CONTROL, applicationContext);
        relinquishLoggingControl = Boolean.valueOf(strValue);
    }
}
