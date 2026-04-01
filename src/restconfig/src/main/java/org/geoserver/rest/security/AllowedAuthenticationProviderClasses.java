/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Resolves the authentication provider allow-list used by the REST security controller.
 *
 * <p>The allow-list is built from the shipped GeoServer authentication provider and provider configuration classes plus
 * any additional exact class names or package prefixes (ending in {@code .*}) supplied through a system property or
 * environment variable. System properties take precedence over environment variables. The class is stateless and
 * thread-safe because each call to {@link #load()} creates a new immutable snapshot.
 */
public final class AllowedAuthenticationProviderClasses {
    private static final Logger LOGGER = Logging.getLogger(AllowedAuthenticationProviderClasses.class);

    public static final String ALLOWED_LIST_PROP = "geoserver.security.allowedAuthenticationProviderClasses";
    public static final String ALLOWED_LIST_ENV = "GEOSERVER_SECURITY_ALLOWED_AUTHENTICATION_PROVIDER_CLASSES";

    private static final Set<String> DEFAULT_EXACT = Set.of(
            "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
            "org.geoserver.security.jdbc.JDBCConnectAuthProvider",
            "org.geoserver.security.ldap.LDAPAuthenticationProvider",
            "org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProvider",
            "org.geoserver.security.auth.web.WebServiceAuthenticationProvider",
            "org.geoserver.security.WebServiceBodyResponseSecurityProvider",
            "org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig",
            "org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig",
            "org.geoserver.security.ldap.LDAPSecurityServiceConfig",
            "org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProviderConfig",
            "org.geoserver.security.auth.web.WebAuthenticationConfig",
            "org.geoserver.security.WebServiceBodyResponseSecurityProviderConfig");

    private final Set<String> exactAllowed;
    private final List<String> packagePrefixes;
    private final String source;

    /**
     * Creates a new immutable allow-list snapshot.
     *
     * @param exactAllowed exact class names currently allowed
     * @param packagePrefixes package prefixes currently allowed
     * @param source configuration source used to build this snapshot
     */
    private AllowedAuthenticationProviderClasses(
            Set<String> exactAllowed, List<String> packagePrefixes, String source) {
        this.exactAllowed = Set.copyOf(exactAllowed);
        this.packagePrefixes = List.copyOf(packagePrefixes);
        this.source = source;
    }

    /**
     * Loads the authentication provider allow-list from the configured property or environment variable.
     *
     * @return the immutable allow-list snapshot for provider and provider config classes
     */
    public static AllowedAuthenticationProviderClasses load() {
        Set<String> exact = new LinkedHashSet<>(DEFAULT_EXACT);
        List<String> prefixes = new ArrayList<>();

        String raw = System.getProperty(ALLOWED_LIST_PROP);
        String source = "defaults";
        if (raw != null && !raw.isBlank()) {
            source = "system property";
        } else {
            raw = System.getenv(ALLOWED_LIST_ENV);
            if (raw != null && !raw.isBlank()) {
                source = "environment variable";
            }
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

        AllowedAuthenticationProviderClasses snapshot =
                new AllowedAuthenticationProviderClasses(exact, prefixes, source);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "Loaded authentication provider allow-list with {0} exact entries, {1} prefix entries from {2}",
                    new Object[] {
                        Integer.valueOf(snapshot.getExactAllowed().size()),
                        Integer.valueOf(snapshot.getPackagePrefixes().size()),
                        snapshot.getSource()
                    });
        }
        return snapshot;
    }

    /**
     * Checks whether the supplied provider or provider config class is allowed.
     *
     * <p>This convenience method loads a fresh snapshot for each invocation. Callers performing multiple checks in one
     * operation should prefer {@link #load()} once and then invoke {@link #allows(String)} on that snapshot.
     *
     * @param className the class name to validate
     * @return {@code true} if the class is allowed; {@code false} otherwise
     */
    public static boolean isAllowed(String className) {
        return load().allows(className);
    }

    /**
     * Checks whether the supplied class name is allowed by exact match or configured package prefix.
     *
     * @param className the class name to validate
     * @return {@code true} if the class is allowed; {@code false} otherwise
     */
    public boolean allows(String className) {
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
     * Returns exact class names in the current snapshot.
     *
     * @return immutable set of exact class names
     */
    public Set<String> getExactAllowed() {
        return exactAllowed;
    }

    /**
     * Returns configured package prefixes in the current snapshot.
     *
     * @return immutable list of package prefixes without trailing {@code .*}
     */
    public List<String> getPackagePrefixes() {
        return packagePrefixes;
    }

    /**
     * Returns the source that produced this snapshot.
     *
     * @return {@code system property}, {@code environment variable}, or {@code defaults}
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns all entries in display form, including prefix entries with trailing {@code .*}.
     *
     * @return immutable set of exact and prefix display entries
     */
    public Set<String> getDisplayEntries() {
        LinkedHashSet<String> entries = new LinkedHashSet<>(exactAllowed);
        for (String prefix : packagePrefixes) {
            entries.add(prefix + ".*");
        }
        return Set.copyOf(entries);
    }

    /**
     * Returns the built-in exact default entries.
     *
     * @return immutable set of built-in exact class names
     */
    public static Set<String> defaultExactClasses() {
        return DEFAULT_EXACT;
    }
}
