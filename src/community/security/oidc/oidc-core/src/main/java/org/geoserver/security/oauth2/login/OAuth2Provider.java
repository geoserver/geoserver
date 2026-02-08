/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import java.util.Arrays;
import java.util.List;

/**
 * Supported OAuth2/OpenID Connect providers.
 *
 * <p>Each constant carries the <em>property prefix</em> used by {@link GeoServerOAuth2LoginFilterConfig} (e.g.
 * {@code "oidc"} → {@code oidcClientId}, {@code oidcClientSecret}, …) and the human-readable label shown in the admin
 * UI dropdown.
 *
 * <p>Declaration order determines dropdown order (OIDC first, as it is the default).
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public enum OAuth2Provider {
    OIDC("oidc", "OpenID Connect Provider"),
    GOOGLE("google", "Google"),
    GITHUB("gitHub", "GitHub"),
    MICROSOFT("ms", "Microsoft Azure");

    /** Bean-property prefix inside {@link GeoServerOAuth2LoginFilterConfig}. */
    private final String propertyPrefix;

    /** Human-readable label for the admin UI. */
    private final String displayLabel;

    OAuth2Provider(String propertyPrefix, String displayLabel) {
        this.propertyPrefix = propertyPrefix;
        this.displayLabel = displayLabel;
    }

    /** @return the bean-property prefix (e.g. {@code "oidc"}, {@code "ms"}) */
    public String getPropertyPrefix() {
        return propertyPrefix;
    }

    /** @return the human-readable label shown in the UI dropdown */
    public String getDisplayLabel() {
        return displayLabel;
    }

    /**
     * Looks up an {@link OAuth2Provider} by its property prefix.
     *
     * @param prefix the prefix string (e.g. {@code "gitHub"})
     * @return the matching provider
     * @throws IllegalArgumentException if no provider matches the given prefix
     */
    public static OAuth2Provider fromPropertyPrefix(String prefix) {
        for (OAuth2Provider p : values()) {
            if (p.propertyPrefix.equals(prefix)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown OAuth2 provider prefix: " + prefix);
    }

    /** @return the provider prefixes in declaration order (suitable for the dropdown model) */
    public static List<String> prefixes() {
        return Arrays.stream(values()).map(OAuth2Provider::getPropertyPrefix).toList();
    }

    /** Whether this provider supports a user-configurable {@code scopes} field. */
    public boolean supportsScopes() {
        return this == OIDC || this == MICROSOFT;
    }

    /** Whether this provider is the custom OIDC provider (with discovery, endpoints, etc.). */
    public boolean isOidc() {
        return this == OIDC;
    }
}
