/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import java.util.Properties;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.geofence.GeoFenceModuleStatus;
import org.geoserver.geofence.cache.CacheConfiguration;
import org.geoserver.geofence.cache.RuleCacheLoaderFactory;
import org.geoserver.geofence.services.RuleReaderServiceFactory;
import org.geoserver.geofence.web.GeofencePage;
import org.geoserver.web.Category;
import org.geoserver.web.MenuPageInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Java-based wiring for the beans that need literal constructor arguments, property-placeholder resolution, or
 * externally-defined GeoServer beans, and therefore aren't plain {@code @Component}-scanned classes.
 *
 * <p>Implements {@link ApplicationContextAware} (rather than taking {@code ApplicationContext} as a {@code @Bean}
 * method parameter) so resolving the context doesn't go through Spring's type-based dependency scan, which walks every
 * bean definition in the (very large) aggregated GeoServer context.
 */
@Configuration
public class GeoFenceSpringConfig implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    @Bean(name = "geofence-configurer")
    public GeoFencePropertyPlaceholderConfigurer geofenceConfigurer(GeoServerDataDirectory dataDirectory) {
        GeoFencePropertyPlaceholderConfigurer configurer = new GeoFencePropertyPlaceholderConfigurer(dataDirectory);
        configurer.setOrder(5);
        configurer.setIgnoreResourceNotFound(true);
        configurer.setIgnoreUnresolvablePlaceholders(true);

        // This location is relative to the datadir
        configurer.setLocation(context.getResource("file:geofence/geofence.properties"));

        // default properties
        Properties props = new Properties();
        // other default values are set directly into the related config beans,
        // anyway this value is used at least twice, so it's better to define it here
        props.setProperty("servicesUrl", "http://localhost:8081/geofence/remoting/RuleReader");
        // The frontend will be injected in the access manager.
        // You may replace the cachedRuleReader value with restRuleReaderService in order to disable the caching
        props.setProperty("ruleReaderFrontend", "cachedRuleReader");
        // The backend will be injected in the cached reader.
        // We need this entry to allow geofence-server to replace the backend with the local bean
        props.setProperty("ruleReaderBackend", "restRuleReaderService");
        configurer.setProperties(props);

        return configurer;
    }

    @Bean
    public GeoFenceConfiguration geoFenceConfiguration(
            @Value("${instanceName:default-gs}") String instanceName,
            @Value("${servicesUrl}") String servicesUrl,
            @Value("${allowRemoteAndInlineLayers:False}") boolean allowRemoteAndInlineLayers,
            @Value("${grantWriteToWorkspacesToAuthenticatedUsers:False}")
                    boolean grantWriteToWorkspacesToAuthenticatedUsers,
            @Value("${useRolesToFilter:False}") boolean useRolesToFilter,
            @Value("${acceptedRoles:}") String acceptedRoles,
            @Value("${gwc.context.suffix:gwc}") String gwcContextSuffix,
            @Value("${org.geoserver.rest.DefaultUserGroupServiceName:default}") String defaultUserGroupServiceName,
            @Value("${ruleReaderBackend}") String ruleReaderBackend,
            @Value("${ruleReaderFrontend}") String ruleReaderFrontend) {
        GeoFenceConfiguration cfg = new GeoFenceConfiguration();
        cfg.setInstanceName(instanceName);
        cfg.setServicesUrl(servicesUrl);
        cfg.setAllowRemoteAndInlineLayers(allowRemoteAndInlineLayers);
        cfg.setGrantWriteToWorkspacesToAuthenticatedUsers(grantWriteToWorkspacesToAuthenticatedUsers);
        cfg.setUseRolesToFilter(useRolesToFilter);
        cfg.setAcceptedRoles(acceptedRoles);
        cfg.setGwcContextSuffix(gwcContextSuffix);
        cfg.setDefaultUserGroupServiceName(defaultUserGroupServiceName);
        cfg.setRuleReaderBackend(ruleReaderBackend);
        cfg.setRuleReaderFrontend(ruleReaderFrontend);
        return cfg;
    }

    @Bean
    public CacheConfiguration cacheConfiguration(
            @Value("${cacheSize:1000}") long size,
            @Value("${cacheRefresh:30000}") long refreshMilliSec,
            @Value("${cacheExpire:60000}") long expireMilliSec) {
        CacheConfiguration cfg = new CacheConfiguration();
        cfg.setSize(size);
        cfg.setRefreshMilliSec(refreshMilliSec);
        cfg.setExpireMilliSec(expireMilliSec);
        return cfg;
    }

    @Bean(name = "geofenceConfigurationManager")
    public GeoFenceConfigurationManager geofenceConfigurationManager(
            @Qualifier("geofence-configurer") GeoFencePropertyPlaceholderConfigurer configurer,
            GeoFenceConfiguration geoFenceConfiguration,
            CacheConfiguration cacheConfiguration,
            @Qualifier("ruleReaderBackendFactory") RuleReaderServiceFactory ruleReaderBackendFactory,
            @Qualifier("ruleReaderFrontendFactory") RuleReaderServiceFactory ruleReaderFrontendFactory) {
        GeoFenceConfigurationManager manager = new GeoFenceConfigurationManager();
        manager.setConfigurer(configurer);
        manager.setConfiguration(geoFenceConfiguration);
        manager.setCacheConfiguration(cacheConfiguration);
        manager.setRuleReaderBackendFactory(ruleReaderBackendFactory);
        manager.setRuleReaderFrontendFactory(ruleReaderFrontendFactory);
        return manager;
    }

    @Bean
    public RuleReaderServiceFactory ruleReaderBackendFactory(@Value("${ruleReaderBackend}") String backendName) {
        return new RuleReaderServiceFactory(backendName, false);
    }

    @Bean
    public RuleReaderServiceFactory ruleReaderFrontendFactory(@Value("${ruleReaderFrontend}") String frontendName) {
        return new RuleReaderServiceFactory(frontendName, true);
    }

    @Bean
    public RuleCacheLoaderFactory ruleCacheLoaderFactory(
            @Qualifier("ruleReaderBackendFactory") RuleReaderServiceFactory backendFactory) {
        return new RuleCacheLoaderFactory(backendFactory);
    }

    @Bean
    public GeoFenceModuleStatus geoFenceModuleStatus() {
        return new GeoFenceModuleStatus("gs-geofence", "GeoFence");
    }

    @Bean
    public MenuPageInfo<GeofencePage> geofencePage(@Qualifier("securityCategory") Category securityCategory) {
        MenuPageInfo<GeofencePage> page = new MenuPageInfo<>();
        page.setId("geofencePage");
        page.setTitleKey("GeofencePage.page.title");
        page.setDescriptionKey("GeofencePage.page.description");
        page.setComponentClass(GeofencePage.class);
        page.setCategory(securityCategory);
        page.setOrder(1000);
        page.setIcon("img/icons/geofence.png");
        return page;
    }
}
