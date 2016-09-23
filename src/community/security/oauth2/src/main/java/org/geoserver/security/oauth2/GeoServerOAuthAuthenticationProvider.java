/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.util.logging.Logger;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecurityManagerListener;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

/**
 * Security provider for OAuth2
 * 
 * @author Alessio Fabiani, GeoSolutions
 */
public abstract class GeoServerOAuthAuthenticationProvider extends AbstractFilterProvider
        implements SecurityManagerListener {

    static Logger LOGGER = Logging.getLogger("org.geoserver.security.outh2");

    RemoteTokenServices tokenServices;

    @Autowired
    GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration;

    @Autowired
    OAuth2RestTemplate geoServerOauth2RestTemplate;

    private ApplicationContext context;

    public GeoServerOAuthAuthenticationProvider(GeoServerSecurityManager securityManager) {
        context = securityManager.getApplicationContext();
        tokenServices = context.getBean(GeoServerOAuthRemoteTokenServices.class);
        securityManager.addListener(this);
    }

    @Override
    public abstract void configure(XStreamPersister xp);

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerOAuthAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new GeoServerOAuthAuthenticationFilter(config, tokenServices,
                oauth2SecurityConfiguration, geoServerOauth2RestTemplate);
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new OAuth2FilterConfigValidator(securityManager);
    }

}
