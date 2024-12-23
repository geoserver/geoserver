/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.regex.Pattern;
import org.geoserver.platform.GeoServerExtensions;

/** Some constants and utility methods for the Content Security Policy implementation. */
public final class CSPUtils {

    /**
     * The default value of the Content-Security-Policy header when there is a misconfiguration or some other error that
     * prevents determining the proper header value
     */
    public static final String DEFAULT_FALLBACK =
            "base-uri 'none'; form-action 'none'; default-src 'none'; frame-ancestors 'none';";

    /**
     * The system property for the value of the Content-Security-Policy header when there is a misconfiguration or some
     * other error that prevents determining the proper header value
     */
    public static final String GEOSERVER_CSP_FALLBACK = "geoserver.csp.fallbackDirectives";

    /**
     * The system property that allows adding remote hosts to fetch directives in the Content-Security-Policy header for
     * static web files without having to modify the configuration (e.g., if OpenLayers is loaded from a CDN rather than
     * GeoServer)
     */
    public static final String GEOSERVER_CSP_REMOTE_RESOURCES = "geoserver.csp.remoteResources";

    /**
     * The system property to set that allows controlling what to set the frame-ancestors directive to in the
     * Content-Security-Policy header without having to modify the configuration
     */
    public static final String GEOSERVER_CSP_FRAME_ANCESTORS = "geoserver.csp.frameAncestors";

    /** The regular expression for GeoServer/GeoTools/GeoWebCache property keys */
    public static final Pattern PROPERTY_KEY_REGEX =
            Pattern.compile("(?i)^[a-z0-9_\\.]*(geoserver|geotools|geowebcache)[a-z0-9_\\.]*$");

    /**
     * The regular expression for valid property values to allow injecting into directives of the
     * Content-Security-Policy header
     */
    public static final Pattern PROPERTY_VALUE_REGEX = Pattern.compile("(?i)^[a-z0-9'\\*][a-z0-9_\\-':/\\.\\* ]{4,}$");

    /** The regular expression to match one or more whitespace characters */
    private static final Pattern WHITESPACE_REGEX = Pattern.compile("\\s+");

    private CSPUtils() {}

    /**
     * Removes unnecessary whitespace characters from the CSP directives and ensures that the string ends with a
     * semicolon.
     *
     * @param directives the original directives
     * @return the cleaned up directives
     */
    public static String cleanDirectives(String directives) {
        directives = WHITESPACE_REGEX.matcher(directives).replaceAll(" ").replace(" ;", ";");
        return directives.endsWith(";") ? directives : directives + ";";
    }

    /**
     * Looks up the value of the specified key or uses the default value if it doesn't exist.
     *
     * @param key the property key
     * @param defaultValue the default property value
     * @return the property value if it exists, otherwise the default value
     */
    public static String getStringProperty(String key, String defaultValue) {
        return Optional.ofNullable(GeoServerExtensions.getProperty(key))
                .map(String::trim)
                .map(Strings::emptyToNull)
                .orElse(defaultValue);
    }

    /**
     * Trims leading and trailing whitespace characters from a non-empty string or converts a null string to an empty
     * string.
     *
     * @param value the original value
     * @return the trimmed value or an empty string
     */
    public static String trimWhitespace(String value) {
        return value != null ? value.trim() : "";
    }
}
