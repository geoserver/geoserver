/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoNodeOAuthAuthenticationFilter extends GeoServerOAuthAuthenticationFilter {

    public GeoNodeOAuthAuthenticationFilter(
            SecurityNamedServiceConfig config,
            RemoteTokenServices tokenServices,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestOperations oauth2RestTemplate) {
        super(config, tokenServices, oauth2SecurityConfiguration, oauth2RestTemplate);
    }
}
