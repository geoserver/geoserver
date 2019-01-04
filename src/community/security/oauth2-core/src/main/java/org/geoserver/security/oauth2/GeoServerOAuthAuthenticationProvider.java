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

    protected RemoteTokenServices tokenServices;

    protected GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration;

    protected OAuth2RestTemplate geoServerOauth2RestTemplate;

    protected ApplicationContext context;

    public GeoServerOAuthAuthenticationProvider(
            GeoServerSecurityManager securityManager,
            String tokenServices,
            String oauth2SecurityConfiguration,
            String geoServerOauth2RestTemplate) {

        assert securityManager != null;

        context = securityManager.getApplicationContext();

        assert context != null;

        this.tokenServices = (RemoteTokenServices) context.getBean(tokenServices);
        this.oauth2SecurityConfiguration =
                (GeoServerOAuth2SecurityConfiguration) context.getBean(oauth2SecurityConfiguration);
        this.geoServerOauth2RestTemplate =
                (OAuth2RestTemplate) context.getBean(geoServerOauth2RestTemplate);

        securityManager.addListener(this);
    }

    @Override
    public abstract void configure(XStreamPersister xp);

    @Override
    public abstract Class<? extends GeoServerSecurityFilter> getFilterClass();

    @Override
    public abstract GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config);

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new OAuth2FilterConfigValidator(securityManager);
    }

    @Override
    public void handlePostChanged(GeoServerSecurityManager securityManager) {
        // By default, nothing to do
    }
}
