/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerSecurityManager;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *
 */
public class GoogleOAuth2AuthenticationProvider extends GeoServerOAuthAuthenticationProvider {

    // Default values
    protected String accessTokenUri = "https://accounts.google.com/o/oauth2/token";

    protected String userAuthorizationUri = "https://accounts.google.com/o/oauth2/auth";

    protected String redirectUri = "http://localhost:8080/geoserver";

    protected String checkTokenEndpointUrl = "https://www.googleapis.com/oauth2/v1/tokeninfo";

    protected String logoutUri = "https://accounts.google.com/logout";

    public GoogleOAuth2AuthenticationProvider(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public void handlePostChanged(GeoServerSecurityManager securityManager) {
        // Nothing to do
    }

    @Override
    public void configure(XStreamPersister xp) {
        xp.getXStream().alias("googleOauth2Authentication", GoogleOAuth2FilterConfig.class);
    }

}
