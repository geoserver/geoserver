/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import static org.apache.logging.log4j.Level.DEBUG;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.geotools.util.logging.Logging;

/**
 * Used to log confidential information, if enabled.
 *
 * <p>Also adjusts log levels of relevant Spring classes to enable HTTP related logging. Unfortunately it is required to
 * use Log4J2 API directly to adjust the log levels at runtime to gain access to the required logger instances.
 *
 * @author awaterme
 */
public class ConfidentialLogger {

    private static final List<String> SPRING_LOGGER_NAMES =
            List.of("org.springframework.web.HttpLogging", "org.springframework.security.web.DefaultRedirectStrategy");
    private static final Map<String, org.apache.logging.log4j.Level> SPRING_ORG_LEVELS =
            determineOrgLevels(SPRING_LOGGER_NAMES);

    private static Logger LOGGER = Logging.getLogger(ConfidentialLogger.class);
    private static boolean enabled = false;

    public static void log(Level level, String msg, Object[] params) {
        if (!enabled) {
            return;
        }
        LOGGER.log(level, msg, params);
    }

    public static boolean isLoggable(Level level) {
        return enabled && LOGGER.isLoggable(level);
    }

    /** @param pEnabled the enabled to set */
    public static void setEnabled(boolean pEnabled) {
        setSpringLoggersEnabled(pEnabled);
        enabled = pEnabled;
    }

    private static void setSpringLoggersEnabled(boolean pEnabled) {
        List<org.apache.logging.log4j.core.Logger> lLoggers = SPRING_LOGGER_NAMES.stream()
                .map(n -> LogManager.getLogger(n))
                .filter(l -> l instanceof org.apache.logging.log4j.core.Logger)
                .map(l -> (org.apache.logging.log4j.core.Logger) l)
                .collect(Collectors.toList());

        lLoggers.forEach(l -> {
            org.apache.logging.log4j.Level lLevel = pEnabled ? DEBUG : SPRING_ORG_LEVELS.get(l.getName());
            l.setLevel(lLevel);
        });
    }

    public static Level getLevel() {
        return LOGGER.getLevel();
    }

    /** @return the enabled */
    public static boolean isEnabled() {
        return enabled;
    }

    public static void setLevel(Level pLevel) {
        LOGGER.setLevel(pLevel);
    }

    private static Map<String, org.apache.logging.log4j.Level> determineOrgLevels(List<String> pLoggerNames) {
        Map<String, org.apache.logging.log4j.Level> lMap = new HashMap<>();
        pLoggerNames.forEach(n -> {
            org.apache.logging.log4j.Logger lLogger = LogManager.getLogger(n);
            lMap.put(n, lLogger.getLevel());
        });
        return lMap;
    }
}
