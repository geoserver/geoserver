/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Loads immutable exact-match class allow-lists from GeoServer configuration.
 *
 * <p>This utility merges a built-in default set with optional user-supplied values taken from a system property or an
 * environment variable. System properties take precedence over environment variables. The returned set is immutable, so
 * callers may safely share it across threads within the scope of a single request or operation.
 *
 * <p>This helper intentionally supports exact fully qualified class names only. Callers that need prefix-based matching
 * should implement that policy at a higher level.
 */
public final class ExactClassAllowList {

    /** Utility class, do not instantiate. */
    private ExactClassAllowList() {}

    /**
     * Loads an exact-match allow-list from a system property or environment variable and merges it with the supplied
     * built-in defaults.
     *
     * @param propertyName the system property name
     * @param envName the environment variable name
     * @param defaults the built-in default class names
     * @return the merged exact-match allow-list
     */
    public static Set<String> load(String propertyName, String envName, Set<String> defaults) {
        LinkedHashSet<String> allowed = new LinkedHashSet<>(defaults);

        String raw = System.getProperty(propertyName);
        if (raw == null || raw.isBlank()) raw = System.getenv(envName);
        if (raw == null || raw.isBlank()) return Set.copyOf(allowed);

        Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(allowed::add);
        return Set.copyOf(allowed);
    }
}
