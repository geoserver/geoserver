/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.Locale;

/** How urgent a notification is. The order of the constants is the display order. */
public enum Severity {
    CRITICAL,
    HIGH,
    WARN,
    NOTICE,
    INFO;

    /**
     * Maps an advisory's {@code critical}, {@code high}, {@code medium} (or {@code moderate}) or {@code low}. Anything
     * else is HIGH, so an unexpected value never downgrades an advisory.
     */
    public static Severity fromAdvisory(String advisorySeverity) {
        switch (normalise(advisorySeverity)) {
            case "critical":
                return CRITICAL;
            case "medium":
            case "moderate":
                return WARN;
            case "low":
                return INFO;
            default:
                return HIGH;
        }
    }

    /** @return whether the text is one of the advisory severities {@link #fromAdvisory} knows */
    static boolean isAdvisorySeverity(String advisorySeverity) {
        switch (normalise(advisorySeverity)) {
            case "critical":
            case "high":
            case "medium":
            case "moderate":
            case "low":
                return true;
            default:
                return false;
        }
    }

    private static String normalise(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    /** @return the word shown before a notification, so severity never depends on colour alone */
    public String label(Locale locale) {
        return Messages.get(locale, "severity." + name());
    }
}
