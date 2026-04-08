/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.resourceserver;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import jakarta.servlet.Filter;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geoserver.security.filter.GeoServerRoleResolvers;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverContext;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfigValidator;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Provider for {@link GeoServerOAuth2ResourceServerAuthenticationFilter}.
 *
 * <p>Used for the "Resource Server" use case. Implementation is unfinished, because a different GS extension supports
 * this case already. Filter is not offered in UI. This code is never executed.
 *
 * @author awaterme
 */
public class GeoServerOAuth2ResourceServerAuthenticationProvider extends AbstractFilterProvider
        implements ApplicationListener<ApplicationEvent> {

    /** Filter types required for GeoServer */
    private static final List<Class<?>> REQ_FILTER_TYPES = asList(BearerTokenAuthenticationFilter.class);

    private class FilterBuilder {

        private final GeoServerOAuth2ResourceServerFilterConfig config;
        private final HttpSecurity http;

        /**
         * @param pConfig
         * @param pHttpSecurity
         */
        public FilterBuilder(GeoServerOAuth2ResourceServerFilterConfig pConfig, HttpSecurity pHttpSecurity) {
            super();
            this.config = pConfig;
            this.http = pHttpSecurity;
        }

        private List<Filter> createFilters() {
            try {
                return createFiltersImpl();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create OpenID filter.", e);
            }
        }

        private List<Filter> createFiltersImpl() throws Exception {
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter;
            if (redirectAuto) {
                http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
            }

            JwtDecoder lDecoder = JwtDecoders.fromIssuerLocation(config.getIssuerUri());
            ResolverContext lRoleResolverCtx = createRoleResolverContext();
            jwtAuthenticationConverter = new GeoServerJwtAuthenticationConverter(lRoleResolverCtx);

            // Use lambda customizer (non-deprecated) instead of http.oauth2ResourceServer()
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {
                jwt.decoder(lDecoder);
                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter);
            }));

            SecurityFilterChain lChain = http.build();
            List<Filter> lFilters = lChain.getFilters();
            lFilters = lFilters.stream()
                    .filter(f -> REQ_FILTER_TYPES.contains(f.getClass()))
                    .collect(toList());
            return lFilters;
        }

        private ResolverContext createRoleResolverContext() {
            GeoServerRoleConverter lConverter = null;
            if (PreAuthenticatedUserNameRoleSource.Header.equals(config.getRoleSource())) {
                String converterName = config.getRoleConverterName();
                lConverter = GeoServerRoleResolvers.loadConverter(converterName);
            }
            return new GeoServerRoleResolvers.DefaultResolverContext(
                    securityManager,
                    config.getRoleServiceName(),
                    config.getUserGroupServiceName(),
                    config.getRolesHeaderAttribute(),
                    lConverter,
                    config.getRoleSource());
        }
    }

    private final GeoServerSecurityManager securityManager;
    private final ApplicationContext context;
    private boolean redirectAuto = false;

    public GeoServerOAuth2ResourceServerAuthenticationProvider(GeoServerSecurityManager pSecurityManager) {
        assert pSecurityManager != null;
        this.securityManager = pSecurityManager;
        this.context = pSecurityManager.getApplicationContext();
        assert this.context != null;
    }

    @Override
    public void configure(XStreamPersister xp) {
        xp.getXStream().alias("oauth2ResourceServerAuthentication", GeoServerOAuth2ResourceServerFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerOAuth2ResourceServerAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        GeoServerOAuth2ResourceServerFilterConfig lConfig = (GeoServerOAuth2ResourceServerFilterConfig) config;
        HttpSecurity httpSecurity = context.getBean(HttpSecurity.class);

        FilterBuilder lBuilder = new FilterBuilder(lConfig, httpSecurity);
        List<Filter> lFilters = lBuilder.createFilters();

        GeoServerCompositeFilter filter = new GeoServerOAuth2ResourceServerAuthenticationFilter();
        filter.setNestedFilters(lFilters);
        return filter;
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
}
