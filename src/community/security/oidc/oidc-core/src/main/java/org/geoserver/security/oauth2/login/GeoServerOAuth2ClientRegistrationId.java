/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

/**
 * Defines IDs for the supported Spring OAuth2 ClientRegistrations.
 *
 * <p>Registration IDs are scoped by filter name to allow multiple filter instances to coexist. Each instance produces
 * unique registration IDs of the form {@code filterName__baseRegId} (e.g. {@code my-filter__oidc}). Spring Security
 * uses the registration ID in callback paths ({@code /web/login/oauth2/code/{registrationId}}) and in the
 * {@code ClientRegistrationRepository}, so uniqueness across filter instances is essential.
 *
 * @author awaterme
 */
public interface GeoServerOAuth2ClientRegistrationId {

    String REG_ID_GIT_HUB = "gitHub";
    String REG_ID_GOOGLE = "google";
    String REG_ID_OIDC = "oidc";
    String REG_ID_MICROSOFT = "microsoft";

    /** Separator between filter name and base registration ID. */
    String REG_ID_SCOPE_SEPARATOR = "__";

    /**
     * Creates a registration ID scoped to a specific filter instance.
     *
     * @param filterName the name of the filter instance (e.g. {@code "my-oidc-filter"})
     * @param baseRegId the base registration ID (e.g. {@link #REG_ID_OIDC})
     * @return the scoped registration ID (e.g. {@code "my-oidc-filter__oidc"})
     */
    static String scopedRegId(String filterName, String baseRegId) {
        if (filterName == null || filterName.isEmpty()) {
            return baseRegId;
        }
        return filterName + REG_ID_SCOPE_SEPARATOR + baseRegId;
    }

    /**
     * Checks whether a (possibly scoped) registration ID corresponds to a given base registration ID.
     *
     * @param registrationId the actual registration ID, possibly scoped (e.g. {@code "my-filter__oidc"} or
     *     {@code "oidc"})
     * @param baseRegId the base registration ID to match against (e.g. {@link #REG_ID_OIDC})
     * @return {@code true} if the registration ID matches the base ID (either directly or as a scoped variant)
     */
    static boolean isRegIdOfType(String registrationId, String baseRegId) {
        if (registrationId == null || baseRegId == null) {
            return false;
        }
        return baseRegId.equals(registrationId) || registrationId.endsWith(REG_ID_SCOPE_SEPARATOR + baseRegId);
    }
}
