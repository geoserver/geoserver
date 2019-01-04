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
public class LoggingInitializer implements GeoServerInitializer, ApplicationContextAware {

    /** logging instance */
    static Logger LOGGER = Logging.getLogger("org.geoserver.logging");

    GeoServerResourceLoader resourceLoader;

    Boolean relinquishLoggingControl;

    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void initialize(GeoServer geoServer) throws Exception {
        geoServer.addListener(
                new ConfigurationListenerAdapter() {

                    @Override
                    public void handleLoggingChange(
                            LoggingInfo logging,
                            List<String> propertyNames,
                            List<Object> oldValues,
                            List<Object> newValues) {

                        // TODO: get rid of this hack checking singleton
                        if (!relinquishLoggingControl) {
                            boolean reload = false;
                            String loggingLevel = logging.getLevel();
                            String loggingLocation = logging.getLocation();
                            Boolean stdOutLogging = logging.isStdOutLogging();

                            if (propertyNames.contains("level")) {
                                loggingLevel =
                                        (String) newValues.get(propertyNames.indexOf("level"));
                                reload = true;
                            }
                            if (propertyNames.contains("location")) {
                                loggingLocation =
                                        (String) newValues.get(propertyNames.indexOf("location"));
                                reload = true;
                            }
                            if (propertyNames.contains("stdOutLogging")) {
                                stdOutLogging =
                                        (Boolean)
                                                newValues.get(
                                                        propertyNames.indexOf("stdOutLogging"));
                                reload = true;
                            }
                            // maintain the system variable overlay
                            loggingLocation = LoggingUtils.getLogFileLocation(loggingLocation);

                            if (reload) {
                                try {
                                    LoggingUtils.initLogging(
                                            resourceLoader,
                                            loggingLevel,
                                            !stdOutLogging,
                                            loggingLocation);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                });
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String strValue =
                GeoServerExtensions.getProperty(
                        LoggingUtils.RELINQUISH_LOG4J_CONTROL, applicationContext);
        relinquishLoggingControl = Boolean.valueOf(strValue);
    }
}
