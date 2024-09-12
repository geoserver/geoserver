/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.oauth2.bearer.TokenValidator;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/** AuthenticationProvider for OpenId Connect. */
public class OpenIdConnectAuthenticationProvider extends GeoServerOAuthAuthenticationProvider
        implements ApplicationListener {

    TokenValidator bearerTokenValidator;

    public OpenIdConnectAuthenticationProvider(
            GeoServerSecurityManager securityManager,
            String tokenServices,
            String oauth2SecurityConfiguration,
            String geoServerOauth2RestTemplate,
            String bearTokenValidatorBeanName) {
        super(
                securityManager,
                tokenServices,
                oauth2SecurityConfiguration,
                geoServerOauth2RestTemplate);
        if ((bearTokenValidatorBeanName != null) && (!bearTokenValidatorBeanName.isEmpty())) {
            bearerTokenValidator = (TokenValidator) context.getBean(bearTokenValidatorBeanName);
        }
    }

    @Override
    public void configure(XStreamPersister xp) {
        xp.getXStream().alias("openIdConnectAuthentication", OpenIdConnectFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return OpenIdConnectAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new OpenIdConnectAuthenticationFilter(
                config,
                tokenServices,
                oauth2SecurityConfiguration,
                geoServerOauth2RestTemplate,
                bearerTokenValidator);
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new OpenIdConnectFilterConfigValidator(securityManager);
    }

    /**
     * Provide a helpful OIDC_LOGGING configuration for this extension on context load event.
     *
     * @param event application event, responds ContextLoadEvent
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextLoadedEvent) {
            // provide a helpful logging config for this extension
            GeoServer geoserver = GeoServerExtensions.bean(GeoServer.class, this.context);
            GeoServerResourceLoader loader = geoserver.getCatalog().getResourceLoader();
            LoggingUtils.checkBuiltInLoggingConfiguration(loader, "OIDC_LOGGING");
        }
    }
}
