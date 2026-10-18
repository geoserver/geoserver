/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

/** How urgent a notification is, most urgent first; the order of the constants is the display priority. */
public enum UpdatesCheckerSeverity {
    CRITICAL,
    HIGH,
    WARN,
    NOTICE,
    INFO;

    /**
     * Maps the {@code severity} of a security advisory to a notification severity: critical, high, medium, low. An
     * unrecognised value is treated as HIGH - an advisory must never be quietly downgraded because the feed said
     * something unexpected.
     */
    public static UpdatesCheckerSeverity fromAdvisory(String advisorySeverity) {
        if (advisorySeverity != null) {
            switch (advisorySeverity.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "critical":
                    return CRITICAL;
                case "high":
                    return HIGH;
                case "medium":
                case "moderate":
                    return WARN;
                case "low":
                    return INFO;
                default:
                    break;
            }
        }
        return HIGH;
    }

    /** @return the text shown at the start of a banner, so severity never depends on colour alone. */
    public String label(java.util.Locale locale) {
        return UpdatesCheckerMessages.get(locale, "severity." + name());
    }
}
