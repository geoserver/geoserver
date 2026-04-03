/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import org.geoserver.catalog.Info;

/**
 * Logging configuration settings.
 *
 * <p>The logging configuration settings are primirly used for troubleshooting with the ability to change
 * {@link #getLevel()} at runtime.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface LoggingInfo extends Info {

    /**
     * The GeoServer logging configuration to use from {@code log} folder.
     *
     * <p>The name of the level is specific to GeoServer, and independent of the actual logging framework. GeoServer
     * includes the following levels internally:
     *
     * <ul>
     *   <li>{@code DEFAULT_LOGGING}
     *   <li>{@code GEOSERVER_DEVELOPER_LOGGING}
     *   <li>{@code GEOTOOLS_DEVELOPER_LOGGING}
     *   <li>{@code PRODUCTION_LOGGING}
     *   <li>{@code QUIET_LOGGING}
     *   <li>{@code TEST_LOGGING}
     *   <li>{@code VERBOSE_LOGGING}
     * </ul>
     *
     * Additional configuration can be defined in the data directory {@code log} folder. The file naming convention is
     * based on the logging framework configured by GT2_LOGGING_REDIRECTION.
     */
    String getLevel();

    /**
     * Sets the logging configuration to one of the files available in {@code log} folder.
     *
     * @param loggingConfiguration Logging configuration from {@code log} folder
     */
    void setLevel(String loggingConfiguration);

    /**
     * Flag indicating if GeoServer is allowed to log to STDOUT, {@code false} to disable Console appenders.
     *
     * <p>A value of {@code true} does not ensure logging to STDOUT, as this requires the selected logging profile to
     * configure a Console appender. When this value is set to {@code false} logging profile will be post-processed to
     * remove any console appenders.
     *
     * @return true to indicate console appenders allowed, false to disable all console appenders
     */
    boolean isStdOutLogging();

    /**
     * Flag indicating if GeoServer is allowed to log to stdout, use {@code false} to disable console appenders to
     * STDOUT.
     *
     * @param allowStdOutLogging Allow console appenders to write to STDOUT, if set to {@code false} they will be
     *     disabled.
     */
    void setStdOutLogging(boolean allowStdOutLogging);
}
