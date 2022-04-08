/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.geoserver.platform.resource.Paths;

/**
 * GeoServerXMLConfiguration builds on Log4J XmlConfiguration, adding post-processing to adjust
 * configuration (if needed) prior to use.
 */
public class GeoServerXMLConfiguration extends XmlConfiguration {

    /**
     * Logfile location, updated by {@link LoggingInitializer} in response to configuration changes.
     */
    String loggingLocation = null;

    /**
     * Logfile processing hint, updated by {@link LoggingInitializer} in response to configuration
     * changes.
     */
    boolean suppressFileLogging = false;

    /**
     * Logfile processing hint, updated by {@link LoggingInitializer} in response to configuration
     * changes.
     */
    boolean suppressStdOutLogging = false;

    /** Initial filename observed for geoserverlogfile appender */
    private String initialFilename;

    /** Initial filename observed for geoserverlogfile appender */
    private String initialFilePattern;

    /** Initial logFIleLocation observed for GEOSERVER_LOG_LOCATION property */
    private String initialLogLocation;

    public GeoServerXMLConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
        super(loggerContext, source);
    }

    /** Modify xml document prior to processing. */
    @Override
    public void setup() {
        super.setup();
    }

    @Override
    protected void preConfigure(Node node) {
        if (!node.isRoot()
                && node.getName() != null
                && node.getParent() != null
                && node.getParent().getName() != null) {
            if (node.getName().equals("Property")) {
                if (node.getAttributes().containsKey("name")
                        && node.getAttributes().get("name").equals("GEOSERVER_LOG_LOCATION")) {
                    initialLogLocation = node.getValue();
                    LOGGER.debug(
                            "Preconfiguration property.GEOSERVER_LOG_LOCATION=",
                            initialLogLocation);
                }
            }
            if (node.getName().equals("filename")
                    && node.getParent().getName().equals("RollingFile")
                    && node.getParent().getAttributes().get("name").equals("geoserverlogfile")) {

                initialFilename = node.getValue();
                LOGGER.debug(
                        "Preconfiguration geoserverlogfile.FileAppender.filename=",
                        initialFilename);
            }
            if (node.getName().equals("filePattern")
                    && node.getParent().getName().equals("RollingFile")
                    && node.getParent().getAttributes().get("name").equals("geoserverlogfile")) {

                initialFilePattern = node.getValue();
                LOGGER.debug(
                        "Preconfiguration geoserverlogfile.FileAppender.filePattern=",
                        initialFilePattern);
            }
            if (node.getName().equals("filename")
                    && node.getParent().getName().equals("FileAppender")
                    && node.getParent().getAttributes().get("name").equals("geoserverlogfile")) {
                initialFilename = node.getValue();
                LOGGER.debug(
                        "Preconfiguration geoserverlogfile.FileAppender.filename=",
                        initialFilename);
            }
        }
        super.preConfigure(node);
    }

    /** Post-process configuration. */
    @Override
    protected void doConfigure() {
        super.doConfigure();

        if (loggingLocation != null) {
            LOGGER.debug("External logfileLocation provided:" + loggingLocation);

            String logfile;
            String extension = Paths.extension(loggingLocation);
            if (extension != null) {
                logfile =
                        loggingLocation.substring(
                                0, loggingLocation.length() - extension.length() - 1);
            } else {
                logfile = loggingLocation;
                extension = "log";
            }

            if (getProperties().containsKey("GEOSERVER_LOG_LOCATION")) {
                // Configuration file setup to use property

                // Step 1: update property with correct value
                LOGGER.debug("Update 'GEOSERVER_LOG_LOCATION' to use '", logfile, "'");
                getProperties().put("GEOSERVER_LOG_LOCATION", logfile);

                // Step 2: confirm geoserverlogfile configured to use property
                Appender appender = getAppenders().get("geoserverlogfile");
                if (appender instanceof RollingFileAppender) {
                    RollingFileAppender fileAppender = (RollingFileAppender) appender;
                    String fileName = fileAppender.getFileName();

                    if (fileName != null && fileName.contains("${GEOSERVER_LOG_LOCATION}")) {
                        // nothing to do here property already in use
                        // allows us to respect choice of file extension and file pattern
                        LOGGER.debug(
                                "Confirmed 'geoserverlogfile.fileName' uses ${GEOSERVER_LOG_LOCATION} property");
                    } else {
                        String replacementFileName = "${GEOSERVER_LOG_LOCATION}." + extension;
                        LOGGER.debug(
                                "Setting up replacement 'geoserverlogfile' to use ${GEOSERVER_LOG_LOCATION} property:"
                                        + replacementFileName);
                        RollingFileAppender replacement =
                                newBuilder(fileAppender)
                                        .setConfiguration(this)
                                        .withFileName(replacementFileName)
                                        .withFilePattern(
                                                "${GEOSERVER_LOG_LOCATION}-%i." + extension)
                                        .build();

                        getAppenders().remove("geoserverlogfile");
                        addAppender(replacement);
                    }
                }
                if (appender instanceof FileAppender) {
                    FileAppender fileAppender = (FileAppender) appender;
                    String fileName = fileAppender.getFileName();
                    if (fileName != null
                            && fileName.contains("${GEOSERVER_LOG_LOCATION}")
                            && fileName.contains(".")) {

                        // nothing to do here property already in use and has some kind of log or
                        // txt extension
                        // this allows us to respect configuration choice of file extension
                        LOGGER.debug(
                                "Confirmed 'geoserverlogfile.fileName' uses ${GEOSERVER_LOG_LOCATION} property and an extension:'"
                                        + fileName
                                        + "'");

                    } else {
                        String replacementFileName = "${GEOSERVER_LOG_LOCATION}." + extension;

                        LOGGER.debug(
                                "Setting up replacement 'geoserverlogfile.fileName' to use ${GEOSERVER_LOG_LOCATION} property:"
                                        + replacementFileName);
                        FileAppender replacement =
                                newBuilder(fileAppender)
                                        .setConfiguration(this)
                                        .withFileName(replacementFileName)
                                        .build();

                        getAppenders().remove("geoserverlogfile");
                        addAppender(replacement);
                    }
                }
            } else {
                // Configuration file does not advertise GEOSERVER_LOG_LOCATION use

                // Step 2: confirm geoserverlogfile configured to use location
                Appender appender = getAppenders().get("geoserverlogfile");
                if (appender instanceof RollingFileAppender) {
                    RollingFileAppender fileAppender = (RollingFileAppender) appender;

                    if (!fileAppender.getFileName().equals(logfile)) {
                        RollingFileAppender replacement =
                                newBuilder(fileAppender)
                                        .setConfiguration(this)
                                        .withFileName(logfile + "." + extension)
                                        .withFilePattern(logfile + "-%i." + extension)
                                        .build();

                        getAppenders().remove("geoserverlogfile");
                        addAppender(replacement);
                    }
                } else if (appender instanceof FileAppender) {
                    FileAppender fileAppender = (FileAppender) appender;

                    if (!fileAppender.getFileName().equals(logfile)) {
                        FileAppender replacement =
                                newBuilder(fileAppender)
                                        .setConfiguration(this)
                                        .withFileName(logfile)
                                        .build();

                        getAppenders().remove("geoserverlogfile");
                        addAppender(replacement);
                    }
                }
            }
        }
    }

    /**
     * Builder from existing FileAppender.
     *
     * @param fileAppender
     * @param <B>
     * @return builder
     */
    public static <B extends FileAppender.Builder<B>> B newBuilder(FileAppender fileAppender) {
        return FileAppender.<B>newBuilder()
                .setName(fileAppender.getName())
                .withFileName(fileAppender.getFileName())
                .setLayout(fileAppender.getLayout());
    }

    /**
     * Builder from existing RollingFileAppender.
     *
     * @param rollingFileAppender
     * @param <B>
     * @return builder
     */
    public static <B extends RollingFileAppender.Builder<B>> B newBuilder(
            RollingFileAppender rollingFileAppender) {
        return RollingFileAppender.<B>newBuilder()
                .setName(rollingFileAppender.getName())
                .withFileName(rollingFileAppender.getFileName())
                .withFilePattern(rollingFileAppender.getFilePattern())
                .setLayout(rollingFileAppender.getLayout())
                .withPolicy(rollingFileAppender.getTriggeringPolicy());
    }
}
