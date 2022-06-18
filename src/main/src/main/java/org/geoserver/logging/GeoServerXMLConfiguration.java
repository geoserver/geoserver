/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import java.io.File;
import java.util.Map;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.geoserver.platform.resource.Paths;
import org.geotools.util.logging.Log4J2Logger;

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

    /** File output node attribute name to suppress for {@link #suppressFileLogging} */
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
        if (isGeoServerLogFile(node, FILE_APPENDER_NODE)) {
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

    /**
     * Check node name (xml element and attribute names are not case sensitive, and have not been
     * corrected yet).
     *
     * @param node config node
     * @param type node type
     * @return node name matches type
     */
    protected boolean isType(Node node, String type) {
        return node != null && node.getName() != null && node.getName().equalsIgnoreCase(type);
    }

    /**
     * Get value for named attribute (xml element and attribute names are not case sensitive, and
     * have not been corrected yet).
     *
     * @param node Config node
     * @param name Attribute name
     * @return attribute value, or {@code null} if not available
     */
    static final String attributeGet(Node node, String name) {
        if (node != null && name != null) {
            for (Map.Entry<String, String> attribute : node.getAttributes().entrySet()) {
                if (attribute.getKey().equalsIgnoreCase(name)) {
                    return attribute.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Put value for named attribute (xml element and attribute names are not case sensitive, and
     * have not been corrected yet).
     *
     * @param node Config node
     * @param name Attribute name
     * @param value Attribute value
     * @return old attribute value
     */
    static final String attributePut(Node node, String name, String value) {
        if (node != null && name != null) {
            for (Map.Entry<String, String> attribute : node.getAttributes().entrySet()) {
                if (attribute.getKey().equalsIgnoreCase(name)) {
                    return attribute.setValue(value);
                }
            }
            return node.getAttributes().put(name, value);
        }
        return null;
    }

    static boolean isAppenderRef(Node node, String refName) {
        return refName != null
                && node != null
                && node.getName() != null
                && node.getName().equalsIgnoreCase(APPENDER_REF_NODE)
                && refName.equalsIgnoreCase(attributeGet(node, REF_NODE));
    }

    static boolean isConsoleStout(Node node) {
        return node != null
                && node.getName() != null
                && node.getName().equalsIgnoreCase(CONSOLE_NODE)
                && STDOUT.equalsIgnoreCase(attributeGet(node, NAME_ATTRIBUTE));
    }

    static boolean isGeoServerLogFile(Node node, String type) {
        return type != null
                && node != null
                && node.getName() != null
                && node.getName().equalsIgnoreCase(type)
                && GEOSERVERLOGFILE.equalsIgnoreCase(attributeGet(node, NAME_ATTRIBUTE));
    }

    protected void fixFileAppender(Node node) {
        String fileName = fileName();

        String fileNameTemplate = attributeGet(node, "fileName");
        LOGGER.debug("Preconfiguration geoserverlogfile.FileAppender.filename=", fileNameTemplate);

        String path = applyPathTemplate(fileName, fileNameTemplate);
        attributePut(node, "filename", path);
        LOGGER.debug("                 geoserverlogfile.FileAppender.filename=", path);
    }

    protected void fixRollingFileAppender(Node node) {
        String fileName = fileName();
        String fileNameTemplate = attributeGet(node, "fileName");
        if (fileNameTemplate != null) {
            LOGGER.debug(
                    "Preconfiguration geoserverlogfile.RollingFile.filename=", fileNameTemplate);

            String path = applyPathTemplate(fileName, fileNameTemplate);
            attributePut(node, "fileName", path);
            LOGGER.debug("                 geoserverlogfile.RollingFile.filename=", path);
        }
        String filePatternTemplate = attributeGet(node, "filePattern");
        if (filePatternTemplate != null) {
            LOGGER.debug(
                    "Preconfiguration geoserverlogfile.RollingFile.filePattern=",
                    filePatternTemplate);

            String pattern = applyPathTemplate(fileName, filePatternTemplate);
            attributePut(node, "filePattern", pattern);
            LOGGER.debug("                 geoserverlogfile.RollingFile.filePattern=", pattern);
        }
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

    /**
     * Use the provided template to configure fileName extensions and log4j decorations.
     *
     * @param path fileName providing path and base name and optional extension
     * @param template template providing log4j decorations and default extension
     * @return fileName configured with extension and log4j decorations from template
     */
    static String applyPathTemplate(String path, String template) {
        if (path.contains("$") || path.contains("%")) {
            // clean input, avoid any log4j property substitution
            return template;
        }

        // default to provided extension (or .log)
        String ext = Paths.extension(path);
        if (ext == null) {
            ext = ".log";
        }

        if (template.contains(".")) {
            // recognize formats similar to "logs/geoserver.log"
            ext = template.substring(template.lastIndexOf("."));
        }

        if (template.contains("-%")) {
            // recognize formats similar to "logs/geoserver-%i.log"
            ext = template.substring(template.indexOf("-%"));
        } else if (template.contains("%")) {
            // recognize formats similar to "logs/%hostName.log"
            ext = template.substring(template.indexOf("%"));
        }

        if (template.contains("-$") && !ext.contains("-$")) {
            // recognize formats similar to "logs/geoserver-$hostName-%i.log"
            ext = template.substring(template.indexOf("-$"));
        } else if (template.contains("$") && !ext.contains("$")) {
            // recognize formats similar to "logs/$hostName-%i.log"
            ext = template.substring(template.indexOf("$"));
        }

        if (Paths.extension(path) != null && ext.contains(".log")) {
            // handles logs/geoserver-$i.log.gz
            ext = ext.replace(".log", "." + Paths.extension(path));
        }

        if (path.lastIndexOf(".") == -1) {
            return path + ext;
        } else {
            return path.substring(0, path.lastIndexOf(".")) + ext;
        }
    }

    /** Post-process configuration check. */
    @Override
    protected void doConfigure() {
        LOGGER.debug("Custom CONFIG level=" + Log4J2Logger.CONFIG.intLevel());
        LOGGER.debug("Custom FINEST level=" + Log4J2Logger.FINEST.intLevel());

        super.doConfigure();

        // The following checks if configuration was successfully applied
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
            if (getAppenders().containsKey("geoserverlogfile")) {
                LOGGER.warn("Appender 'geoserverlogfile' expected to be suppressed");
            }
            for (LoggerConfig loggerConfig : getLoggers().values()) {
                if (loggerConfig.getAppenders().containsKey("geoserverlogfile")) {
                    LOGGER.warn(
                            "Logger '"
                                    + loggerConfig.getName()
                                    + "' includes suppressed 'geoserverlogfile':"
                                    + loggerConfig.getAppenders().get("geoserverlogfile"));
                }
            }
        }

        if (suppressStdOutLogging) {
            if (getAppenders().containsKey("stdout")) {
                LOGGER.warn("Appender 'stdout' expected to be suppressed");
            }
            for (LoggerConfig loggerConfig : getLoggers().values()) {
                if (loggerConfig.getAppenders().containsKey("stdout")) {
                    LOGGER.warn(
                            "Logger '"
                                    + loggerConfig.getName()
                                    + "' includes suppressed 'stdout':"
                                    + loggerConfig.getAppenders().get("stdout"));
                }
            }
        }
    }
}
