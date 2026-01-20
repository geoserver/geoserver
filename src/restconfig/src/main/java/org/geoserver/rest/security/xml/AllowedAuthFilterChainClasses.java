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

/**
 * Maintains an allow-list of authentication filter chain implementation class names.
 *
 * <p>The allow-list is used to guard reflective instantiation of {@code RequestFilterChain} implementations and prevent
 * construction of unexpected or unsupported classes
 *
 * <p>Allowed classes can be configured via a system property or environment variable:
 *
 * <ul>
 *   <li>{@code geoserver.security.allowedAuthFilterChainClasses}
 *   <li>{@code GEOSERVER_SECURITY_ALLOWED_AUTH_FILTERCHAIN_CLASSES}
 * </ul>
 *
 * The value is a comma-separated list of fully qualified class names and/or package prefixes ending in {@code .*}.
 *
 * <p>If no explicit configuration is provided, a default allow-list of built-in GeoServer authentication filter chain
 * implementations is used.
 */
public class AllowedAuthFilterChainClasses {

    public static final String ALLOWED_LIST_PROP = "geoserver.security.allowedAuthFilterChainClasses";
    public static final String ALLOWED_LIST_ENV = "GEOSERVER_SECURITY_ALLOWED_AUTH_FILTERCHAIN_CLASSES";

    private final Set<String> exact;
    private final List<String> prefixes;

    private AllowedAuthFilterChainClasses(Set<String> exact, List<String> prefixes) {
        this.exact = Set.copyOf(exact);
        this.prefixes = List.copyOf(prefixes);
    }

    static AllowedAuthFilterChainClasses load() {
        // Always include built-in GeoServer implementations
        Set<String> exact = new HashSet<>(defaultBuiltIns());
        List<String> prefixes = new ArrayList<>();

        String raw = System.getProperty(ALLOWED_LIST_PROP);
        if (raw == null || raw.isBlank()) raw = System.getenv(ALLOWED_LIST_ENV);

        if (raw == null || raw.isBlank()) {
            return new AllowedAuthFilterChainClasses(exact, prefixes);
        }

        // Merge configured allow-list on top of defaults
        List<String> tokens = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        for (String t : tokens) {
            if (t.endsWith(".*")) {
                prefixes.add(t.substring(0, t.length() - 2));
            } else {
                exact.add(t);
            }
        }

        return new AllowedAuthFilterChainClasses(exact, prefixes);
    }

    private static Set<String> defaultBuiltIns() {
        // Built-in classes used by default GeoServer filter chains
        return Set.of(
                "org.geoserver.security.HtmlLoginFilterChain",
                "org.geoserver.security.ConstantFilterChain",
                "org.geoserver.security.LogoutFilterChain",
                "org.geoserver.security.ServiceLoginFilterChain",
                "org.geoserver.security.VariableFilterChain");
    }

    boolean isAllowed(String className) {
        if (className == null || className.isBlank()) return false;
        if (exact.contains(className)) return true;
        for (String p : prefixes) {
            if (className.startsWith(p + ".")) return true;
        }
        return false;
    }
}
