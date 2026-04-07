/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Resolves the allow-list of authentication filter chain implementation class names.
 *
 * <p>The allow-list is used to guard reflective instantiation of {@code RequestFilterChain} implementations and prevent
 * construction of unexpected or unsupported classes.
 *
 * <p>Allowed classes can be configured via a system property or environment variable:
 *
 * <ul>
 *   <li>{@code geoserver.security.allowedAuthFilterChainClasses}
 *   <li>{@code GEOSERVER_SECURITY_ALLOWED_AUTH_FILTERCHAIN_CLASSES}
 * </ul>
 *
 * <p>The value is a comma-separated list of fully qualified class names and/or package prefixes ending in {@code .*}.
 *
 * <p>If no explicit configuration is provided, a default allow-list of built-in GeoServer authentication filter chain
 * implementations is used. The class is stateless and thread-safe because each load operation returns an immutable
 * snapshot of the current configuration.
 */
public final class AllowedAuthFilterChainClasses {
    private static final Logger LOGGER = Logging.getLogger(AllowedAuthFilterChainClasses.class);

    public static final String ALLOWED_LIST_PROP = "geoserver.security.allowedAuthFilterChainClasses";
    public static final String ALLOWED_LIST_ENV = "GEOSERVER_SECURITY_ALLOWED_AUTH_FILTERCHAIN_CLASSES";

    private final Set<String> exactAllowed;
    private final List<String> packagePrefixes;

    /**
     * Creates a new immutable allow-list snapshot.
     *
     * @param exactAllowed exact class names currently allowed
     * @param packagePrefixes package prefixes currently allowed
     */
    private AllowedAuthFilterChainClasses(Set<String> exactAllowed, List<String> packagePrefixes) {
        this.exactAllowed = Set.copyOf(exactAllowed);
        this.packagePrefixes = List.copyOf(packagePrefixes);
    }

    /**
     * Loads the filter-chain allow-list from the configured property or environment variable.
     *
     * <p>The result always includes the built-in GeoServer filter-chain implementations.
     *
     * @return the merged allow-list for authentication filter-chain classes
     */
    public static AllowedAuthFilterChainClasses load() {
        Set<String> exact = new HashSet<>(defaultBuiltIns());
        List<String> prefixes = new ArrayList<>();

        String raw = System.getProperty(ALLOWED_LIST_PROP);
        if (raw == null || raw.isBlank()) {
            raw = System.getenv(ALLOWED_LIST_ENV);
        }
        if (raw != null && !raw.isBlank()) {
            List<String> tokens = Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty())
                    .toList();
            for (String token : tokens) {
                if (token.endsWith(".*")) {
                    prefixes.add(token.substring(0, token.length() - 2));
                } else {
                    exact.add(token);
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "Loaded authentication filter-chain allow-list with {0} exact entries, {1} prefix entries from {2}",
                    new Object[] {Integer.valueOf(exact.size()), Integer.valueOf(prefixes.size()), source()});
        }
        return new AllowedAuthFilterChainClasses(exact, prefixes);
    }

    /**
     * Returns the built-in authentication filter-chain classes that are always allowed.
     *
     * @return the immutable built-in baseline for filter-chain class validation
     */
    private static Set<String> defaultBuiltIns() {
        // Built-in classes used by default GeoServer filter chains
        return Set.of(
                "org.geoserver.security.HtmlLoginFilterChain",
                "org.geoserver.security.ConstantFilterChain",
                "org.geoserver.security.LogoutFilterChain",
                "org.geoserver.security.ServiceLoginFilterChain",
                "org.geoserver.security.VariableFilterChain");
    }

    /**
     * Checks whether the supplied class name is allowed by exact match or configured package prefix.
     *
     * @param className the class name to validate
     * @return {@code true} if the class is allowed; {@code false} otherwise
     */
    public boolean isAllowed(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        if (exactAllowed.contains(className)) {
            return true;
        }
        for (String prefix : packagePrefixes) {
            if (className.startsWith(prefix + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines which configuration source is currently active for the filter-chain allow-list.
     *
     * @return {@code system property}, {@code environment variable}, or {@code defaults}
     */
    private static String source() {
        String propertyValue = System.getProperty(ALLOWED_LIST_PROP);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return "system property";
        }
        String envValue = System.getenv(ALLOWED_LIST_ENV);
        if (envValue != null && !envValue.isBlank()) {
            return "environment variable";
        }
        return "defaults";
    }
}
