/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginCustomizers.ClientRegistrationCustomizer;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginCustomizers.FilterBuilderCustomizer;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginCustomizers.HttpSecurityCustomizer;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * Creates a {@link GeoServerOAuth2LoginAuthenticationFilter} which supports OAuth2 and OpenID Connect login by
 * delegating to Spring's respective filters.
 *
 * <p>This provider uses the Spring public API ({@link HttpSecurity}) to setup the required Spring filters. Advantage:
 * It's a hopefully future proof way of settings up the filters and their related objects. Disadvantage: Spring API is
 * not designed to support changing of configuration in running applications. Effect: When saving changes via the admin
 * UI, some then obsolete instances remain in the Spring factories as "disposableBeans". However, the memory footprint
 * of these classes is small and no negative impact on the application has been identified, as the objects are no longer
 * used. The advantages of this approach seem to outweigh the disadvantages.
 *
 * @author awaterme
 * @see https://github.com/spring-projects/spring-security/issues/7449 (currently in status "open") regarding more
 *     flexible configuration
 */
public class GeoServerOAuth2LoginAuthenticationProvider extends AbstractFilterProvider
        implements ApplicationListener<ApplicationEvent>, GeoServerOAuth2ClientRegistrationId {

    private static final Logger LOGGER = Logging.getLogger(GeoServerOAuth2LoginAuthenticationProvider.class);

    private GeoServerSecurityManager securityManager;
    private ApplicationContext context;
    private String builderBeanName = "geoServerOAuth2LoginAuthenticationFilterBuilder";

    public GeoServerOAuth2LoginAuthenticationProvider(GeoServerSecurityManager pSecurityManager) {
        this.securityManager = pSecurityManager;
        context = pSecurityManager.getApplicationContext();
    }

    @Override
    public void configure(XStreamPersister xp) {
        xp.getXStream().alias("oauth2LoginAuthentication", GeoServerOAuth2LoginFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerOAuth2LoginAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        GeoServerOAuth2LoginFilterConfig lConfig = (GeoServerOAuth2LoginFilterConfig) config;
        LOGGER.fine("Using '" + builderBeanName + "' for filter creation");

        HttpSecurityCustomizer lHttpCustomizer;
        ClientRegistrationCustomizer lClientCustomizer;
        FilterBuilderCustomizer lBuilderCustomizer;
        GeoServerOAuth2LoginAuthenticationFilterBuilder lBuilder;

        lHttpCustomizer = getOptionalBean(HttpSecurityCustomizer.class);
        lClientCustomizer = getOptionalBean(ClientRegistrationCustomizer.class);
        lBuilderCustomizer = getOptionalBean(FilterBuilderCustomizer.class);
        lBuilder = context.getBean(builderBeanName, GeoServerOAuth2LoginAuthenticationFilterBuilder.class);

        lBuilder.setConfiguration(lConfig);
        lBuilder.setSecurityManager(securityManager);
        lBuilder.setEventPublisher(context);
        lBuilder.setHttpSecurityCustomizer(lHttpCustomizer);
        lBuilder.setClientRegistrationCustomizer(lClientCustomizer);
        if (lBuilderCustomizer != null) {
            lBuilderCustomizer.accept(lBuilder);
        }

        return lBuilder.build();
    }

    private <T> T getOptionalBean(Class<T> pClass) {
        try {
            return context.getBean(pClass);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(GeoServerSecurityManager securityManager) {
        return new GeoServerOAuth2LoginFilterConfigValidator(securityManager);
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

    /** @param pBuildBeanName the buildBeanName to set */
    public void setBuilderBeanName(String pBuildBeanName) {
        builderBeanName = pBuildBeanName;
    }
}
