/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * Resolves the authentication provider allow-list used by the REST security controller.
 *
 * <p>The allow-list is built from the shipped GeoServer authentication provider and provider configuration classes plus
 * any additional exact fully qualified class names supplied through a system property or environment variable. System
 * properties take precedence over environment variables. The class is stateless and thread-safe because it derives a
 * fresh immutable set for each load operation.
 */
public final class AllowedAuthenticationProviderClasses {
    private static final Logger LOGGER = Logging.getLogger(AllowedAuthenticationProviderClasses.class);

    public static final String ALLOWED_LIST_PROP = "geoserver.security.allowedAuthenticationProviderClasses";
    public static final String ALLOWED_LIST_ENV = "GEOSERVER_SECURITY_ALLOWED_AUTHENTICATION_PROVIDER_CLASSES";

    private static final Set<String> DEFAULTS = Set.of(
            "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
            "org.geoserver.security.jdbc.JDBCConnectAuthProvider",
            "org.geoserver.security.ldap.LDAPAuthenticationProvider",
            "org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProvider",
            "org.geoserver.security.auth.web.WebServiceAuthenticationProvider",
            "org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig",
            "org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig",
            "org.geoserver.security.ldap.LDAPSecurityServiceConfig",
            "org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProviderConfig",
            "org.geoserver.security.auth.web.WebAuthenticationConfig");

    /** Utility class, do not instantiate. */
    private AllowedAuthenticationProviderClasses() {}

    /**
     * Loads the authentication provider allow-list from the configured property or environment variable.
     *
     * @return the merged exact-match allow-list for provider and provider config classes
     */
    public static Set<String> load() {
        Set<String> allowed = ExactClassAllowList.load(ALLOWED_LIST_PROP, ALLOWED_LIST_ENV, DEFAULTS);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Loaded authentication provider allow-list with {0} entries from {1}", new Object[] {
                Integer.valueOf(allowed.size()), source()
            });
        }
        return allowed;
    }

    /**
     * Checks whether the supplied provider or provider config class is allowed.
     *
     * @param className the class name to validate
     * @return {@code true} if the class is allowed; {@code false} otherwise
     */
    public static boolean isAllowed(String className) {
        return className != null && !className.isBlank() && load().contains(className);
    }

    /**
     * Determines which configuration source is currently active for the authentication provider allow-list.
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
