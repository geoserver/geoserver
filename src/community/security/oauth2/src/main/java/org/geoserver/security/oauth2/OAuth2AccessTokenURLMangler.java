/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.util.Map;
import org.geoserver.ows.URLMangler;
import org.geoserver.security.GeoServerSecurityManager;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        OAuth2AccessToken token =
                geoServerOauth2RestTemplate.getOAuth2ClientContext().getAccessToken();
        if (authentication != null
                && authentication.isAuthenticated()
                && token != null
                && token.getTokenType().equalsIgnoreCase(OAuth2AccessToken.BEARER_TYPE)) {
            kvp.put("access_token", token.getValue());
        }
    }
}
