/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import java.io.File;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.geoserver.platform.resource.Paths;

/**
 * GeoServerXMLConfiguration builds on Log4J XmlConfiguration, adding post-processing to adjust
 * configuration (if needed) prior to use.
 */
public class GeoServerXMLConfiguration extends XmlConfiguration {

    /** Node name for console output */
    protected static final String CONSOLE_NODE = "Console";
    /** Node attribute for name */
    protected static final String NAME_ATTRIBUTE = "name";
    /** Console node attribute name to supress for {@link #suppressStdOutLogging} */
    protected static final String STDOUT = "stdout";

    /** File output node attribute name to supress for {@link #suppressFileLogging} */
    protected static final String GEOSERVERLOGFILE = "geoserverlogfile";

    /** Node name for appender references */
    protected static final String APPENDER_REF_NODE = "AppenderRef";
    /** Node attribute for ref */
    protected static final String REF_NODE = "ref";
    /** Node name for rolling file output */
    protected static final String ROLLING_FILE_NODE = "RollingFile";
    /** Node name for file appender output */
    protected static final String FILE_APPENDER_NODE = "FileAppender";
    /** Node name for appenders */
    protected static final String APPENDERS_NODE = "Appenders";
    /** Node name for appender */
    protected static final String APPENDER_NODE = "Appender";
    /** Node name for loggers */
    protected static final String LOGGERS_NODE = "Loggers";
    /** Node name for appender */
    protected static final String LOGGER_NODE = "Logger";

    public static final String ROOT_NODE = "Root";
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

    /** Initial logFileLocation observed for GEOSERVER_LOG_LOCATION property */
    // private String initialLogLocation;

    public GeoServerXMLConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
        super(loggerContext, source);
    }

    @Override
    protected void preConfigure(Node node) {
        if (suppressFileLogging) {
            stripGeoServerLogFile(node);
        }
        if (suppressStdOutLogging) {
            stripConsoleStout(node);
        }
        if (isGeoServerLogFile(node, ROLLING_FILE_NODE)) {
            fixRollingFileAppender(node);
        }
        if (isGeoServerLogFile(node.getParent(), FILE_APPENDER_NODE)) {
            fixFileAppender(node);
        }
        super.preConfigure(node);
    }

    protected void stripGeoServerLogFile(Node node) {
        if (isType(node, APPENDERS_NODE)) {
            node.getChildren()
                    .removeIf(
                            appender ->
                                    isGeoServerLogFile(appender, ROLLING_FILE_NODE)
                                            || isGeoServerLogFile(appender, FILE_APPENDER_NODE));
        }
        if (isType(node, LOGGERS_NODE)) {
            for (final Node logger : node.getChildren()) {
                if (isType(logger, LOGGER_NODE) || isType(logger, ROOT_NODE)) {
                    logger.getChildren().removeIf(ref -> isAppenderRef(ref, GEOSERVERLOGFILE));
                }
            }
            node.getChildren().removeIf(logger -> logger.getChildren().isEmpty());
        }
    }

    protected void stripConsoleStout(Node node) {
        if (isType(node, APPENDERS_NODE)) {
            node.getChildren().removeIf(appender -> isConsoleStout(appender));
        }
        if (isType(node, LOGGERS_NODE)) {
            for (final Node logger : node.getChildren()) {
                if (isType(logger, LOGGER_NODE) || isType(logger, ROOT_NODE)) {
                    logger.getChildren().removeIf(ref -> isAppenderRef(ref, STDOUT));
                }
            }
            node.getChildren().removeIf(logger -> logger.getChildren().isEmpty());
        }
    }

    protected boolean isType(Node node, String type) {
        return node != null && node.getName() != null && node.getName().equals(type);
    }

    protected boolean isAppenderRef(Node node, String refName) {
        return node != null
                && node.getName() != null
                && node.getName().equals(APPENDER_REF_NODE)
                && node.getAttributes().containsKey(REF_NODE)
                && node.getAttributes().get(REF_NODE).equals(refName);
    }

    protected boolean isConsoleStout(Node node) {
        return node != null
                && node.getName() != null
                && node.getName().equals(CONSOLE_NODE)
                && node.getAttributes().containsKey(NAME_ATTRIBUTE)
                && node.getAttributes().get(NAME_ATTRIBUTE).equals(STDOUT);
    }

    protected boolean isGeoServerLogFile(Node node, String type) {
        return node != null
                && node.getName() != null
                && node.getName().equals(type)
                && node.getAttributes().containsKey(NAME_ATTRIBUTE)
                && node.getAttributes().get(NAME_ATTRIBUTE).equals(GEOSERVERLOGFILE);
    }

    protected void fixFileAppender(Node node) {
        String fileName = fileName();
        initialFilename = node.getAttributes().get("filename");
        LOGGER.debug("Preconfiguration geoserverlogfile.FileAppender.filename=", initialFilename);
        node.getAttributes().put("filename", fileName);
        LOGGER.debug("                 geoserverlogfile.FileAppender.filename=", fileName);
    }

    protected void fixRollingFileAppender(Node node) {
        String fileName = fileName();
        String extension = Paths.extension(fileName);
        if (extension == null) {
            extension = "log";
        }
        String filePattern = Paths.sidecar(fileName, null) + "-%i." + extension;

        initialFilename = node.getAttributes().get("filename");
        LOGGER.debug("Preconfiguration geoserverlogfile.RollingFile.filename=", initialFilename);
        node.getAttributes().put("filename", fileName);
        LOGGER.debug("                 geoserverlogfile.RollingFile.filename=", fileName);

        initialFilePattern = node.getAttributes().get("filePattern");
        LOGGER.debug(
                "Preconfiguration geoserverlogfile.RollingFile.filePattern=", initialFilePattern);
        node.getAttributes().put("filePattern", filePattern);
        LOGGER.debug("                 geoserverlogfile.RollingFile.filePattern=", filePattern);
    }

    /**
     * Clean up loggingLocation (providing default logs/geoserver.log location if required).
     *
     * @return fileName to use for logging
     */
    protected String fileName() {
        String fileName = loggingLocation != null ? loggingLocation : "logs/geoserver.log";
        if (fileName.startsWith("logs/")) {
            File check = new File("pom.xml");
            if (check.exists()) {
                // developer testing using jetty
                return "target/" + fileName;
            }
        }
        return fileName;
    }

    /** Post-process configuration. */
    @Override
    protected void doConfigure() {
        super.doConfigure();

        if (fileName() != null) {
            LOGGER.debug("External logfileLocation provided:" + fileName());

            // Confirm geoserverlogfile configured to use loggingLocation
            Appender appender = getAppenders().get("geoserverlogfile");
            if (appender instanceof RollingFileAppender) {
                RollingFileAppender fileAppender = (RollingFileAppender) appender;
                String fileName = fileAppender.getFileName();
                String filePattern = fileAppender.getFilePattern();

                LOGGER.debug("Postconfigure geoserverlogfile.filename=" + fileName);
                LOGGER.debug("Postconfigure geoserverlogfile.filePattern=" + filePattern);
            } else if (appender instanceof FileAppender) {
                FileAppender fileAppender = (FileAppender) appender;
                String fileName = fileAppender.getFileName();
                LOGGER.debug("Postconfigure geoserverlogfile.filename=" + fileName);
            }
        }

        if (suppressFileLogging) {
            getAppenders().remove("geoserverlogfile");
            for (LoggerConfig loggerConfig : getLoggers().values()) {
                if (loggerConfig.getAppenders().containsKey("geoserverlogfile")) {
                    LOGGER.debug(
                            "Logger '"
                                    + loggerConfig.getName()
                                    + "' includes supressed 'geoserverlogfile':"
                                    + loggerConfig.getAppenders().get("geoserverlogfile"));
                }
            }
        }

        if (suppressStdOutLogging) {
            getAppenders().remove("stdout");
            for (LoggerConfig loggerConfig : getLoggers().values()) {
                if (loggerConfig.getAppenders().containsKey("stdout")) {
                    LOGGER.debug(
                            "Logger '"
                                    + loggerConfig.getName()
                                    + "' includes supressed 'stdout':"
                                    + loggerConfig.getAppenders().get("stdout"));
                }
            }
        }
    }
}
