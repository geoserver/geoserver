/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.ows.URLMangler;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * A URL Mangler checking for a "BEARER" type OAuth2 Access Token into the OAuth2 Security Context
 * and injecting it on the OWS URLs.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class OAuth2AccessTokenURLMangler implements URLMangler {

    public static final String ALLOW_OAUTH2_URL_MANGLER = "ALLOW_OAUTH2_URL_MANGLER";

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(OAuth2AccessTokenURLMangler.class);

    GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration;

    OAuth2RestTemplate geoServerOauth2RestTemplate;

    private ApplicationContext context;

    public OAuth2AccessTokenURLMangler(
            GeoServerSecurityManager securityManager,
            String oauth2SecurityConfiguration,
            String geoServerOauth2RestTemplate) {

        assert securityManager != null;

        context = securityManager.getApplicationContext();

        assert context != null;

        this.oauth2SecurityConfiguration =
                (GeoServerOAuth2SecurityConfiguration) context.getBean(oauth2SecurityConfiguration);
        this.geoServerOauth2RestTemplate =
                (OAuth2RestTemplate) context.getBean(geoServerOauth2RestTemplate);
    }

    public OAuth2AccessTokenURLMangler(
            GeoServerSecurityManager securityManager,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestTemplate geoServerOauth2RestTemplate) {

        assert securityManager != null;

        context = securityManager.getApplicationContext();

        assert context != null;

        this.oauth2SecurityConfiguration = oauth2SecurityConfiguration;
        this.geoServerOauth2RestTemplate = geoServerOauth2RestTemplate;
    }

    /** @return the context */
    public ApplicationContext getContext() {
        return context;
    }

    /** @param context the context to set */
    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        Boolean OAUTH2_URL_MANGLER_ENABLED =
                Boolean.valueOf(System.getProperty(ALLOW_OAUTH2_URL_MANGLER, "false"));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            OAuth2AccessToken token =
                    geoServerOauth2RestTemplate.getOAuth2ClientContext().getAccessToken();
            if (OAUTH2_URL_MANGLER_ENABLED
                    && authentication != null
                    && authentication.isAuthenticated()
                    && token != null
                    && token.getTokenType().equalsIgnoreCase(OAuth2AccessToken.BEARER_TYPE)) {
                kvp.put("access_token", token.getValue());
            }
        } catch (Exception e) {
            // We are outside the session scope
            LOGGER.warning(e.getMessage());
        }
    }
}
