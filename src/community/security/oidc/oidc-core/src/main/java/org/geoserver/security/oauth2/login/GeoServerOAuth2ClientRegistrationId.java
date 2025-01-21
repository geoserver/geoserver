/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

/**
 * Defines IDs for the supported Spring OAuth2 ClientRegistrations.
 *
 * @author awaterme
 */
public interface GeoServerOAuth2ClientRegistrationId {

    String REG_ID_GIT_HUB = "gitHub";
    String REG_ID_GOOGLE = "google";
    String REG_ID_OIDC = "oidc";
    String REG_ID_MICROSOFT = "microsoft";
}
