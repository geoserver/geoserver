/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerPropertyConfigurer;
import org.geoserver.geofence.GeoFenceModuleStatus;
import org.geoserver.geofence.cache.CacheConfiguration;
import org.geoserver.geofence.cache.RuleCacheLoaderFactory;
import org.geoserver.geofence.services.RuleReaderServiceFactory;
import org.geoserver.geofence.web.GeofencePage;
import org.geoserver.web.Category;
import org.geoserver.web.MenuPageInfo;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Java-based wiring for beans needing literal constructor arguments, property-file values, or externally-defined
 * GeoServer beans.
 *
 * <p>{@code geofence.properties} is read directly ({@link #loadProperties}), not via a Spring
 * {@code PropertySourcesPlaceholderConfigurer} bean - that resolves too early, before the data directory is set up.
 */
@Configuration
public class GeoFenceSpringConfig implements ApplicationContextAware {

    private static final Logger LOGGER = Logging.getLogger(GeoFenceSpringConfig.class);

    private static final String CONFIG_LOCATION = "file:geofence/geofence.properties";

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    /**
     * Reads {@code geofence/geofence.properties} - same file {@link GeoFenceConfigurationManager} reads/saves through.
     */
    private Properties loadProperties(GeoServerDataDirectory dataDirectory) {
        Properties props = new Properties();
        mergeFrom(props, dataDirectory, CONFIG_LOCATION);
        return props;
    }

    private void mergeFrom(Properties target, GeoServerDataDirectory dataDirectory, String location) {
        GeoServerPropertyConfigurer reader = new GeoServerPropertyConfigurer(dataDirectory);
        reader.setLocation(context.getResource(location));
        try (InputStream in = reader.getConfigFile().in()) {
            target.load(in);
        } catch (IllegalStateException e) {
            // location does not exist, nothing to merge
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read " + location, e);
        }
    }

    @Bean
    public GeoFenceConfiguration geoFenceConfiguration(GeoServerDataDirectory dataDirectory) {
        Properties props = loadProperties(dataDirectory);
        GeoFenceConfiguration cfg = new GeoFenceConfiguration();
        cfg.setInstanceName(props.getProperty("instanceName", "default-gs"));
        cfg.setServicesUrl(props.getProperty("servicesUrl", "http://localhost:9191/geofence/rest"));
        cfg.setAllowRemoteAndInlineLayers(
                Boolean.parseBoolean(props.getProperty("allowRemoteAndInlineLayers", "False")));
        cfg.setGrantWriteToWorkspacesToAuthenticatedUsers(
                Boolean.parseBoolean(props.getProperty("grantWriteToWorkspacesToAuthenticatedUsers", "False")));
        cfg.setUseRolesToFilter(Boolean.parseBoolean(props.getProperty("useRolesToFilter", "False")));
        cfg.setAcceptedRoles(props.getProperty("acceptedRoles", ""));
        cfg.setDefaultUserGroupServiceName(
                props.getProperty("org.geoserver.rest.DefaultUserGroupServiceName", "default"));
        cfg.setRuleReaderBackend(resolveRuleReaderBackend(props.getProperty("ruleReaderBackend", "")));
        cfg.setRuleReaderFrontend(props.getProperty("ruleReaderFrontend", "cachedRuleReader"));
        return cfg;
    }

    /**
     * An explicit {@code ruleReaderBackend} property always wins; otherwise default to the embedded engine if
     * geofence-server is on the classpath, else the REST client.
     */
    private String resolveRuleReaderBackend(String explicit) {
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        return context.containsBeanDefinition(RuleReaderServiceFactory.INTERNAL_RULE_READER_NAME)
                ? RuleReaderServiceFactory.INTERNAL_RULE_READER_NAME
                : RuleReaderServiceFactory.REMOTE_RULE_READER_NAME;
    }

    @Bean
    public CacheConfiguration cacheConfiguration(GeoServerDataDirectory dataDirectory) {
        Properties props = loadProperties(dataDirectory);
        CacheConfiguration cfg = new CacheConfiguration();
        cfg.setSize(Long.parseLong(props.getProperty("cacheSize", "1000")));
        cfg.setRefreshMilliSec(Long.parseLong(props.getProperty("cacheRefresh", "30000")));
        cfg.setExpireMilliSec(Long.parseLong(props.getProperty("cacheExpire", "60000")));
        return cfg;
    }

    @Bean(name = "geofenceConfigurationManager")
    public GeoFenceConfigurationManager geofenceConfigurationManager(
            GeoServerDataDirectory dataDirectory,
            GeoFenceConfiguration geoFenceConfiguration,
            CacheConfiguration cacheConfiguration,
            @Qualifier("ruleReaderBackendFactory") RuleReaderServiceFactory ruleReaderBackendFactory,
            @Qualifier("ruleReaderFrontendFactory") RuleReaderServiceFactory ruleReaderFrontendFactory) {
        GeoFenceConfigurationManager manager = new GeoFenceConfigurationManager();
        manager.setConfigurer(resolveManagerConfigurer(dataDirectory));
        manager.setConfiguration(geoFenceConfiguration);
        manager.setCacheConfiguration(cacheConfiguration);
        manager.setRuleReaderBackendFactory(ruleReaderBackendFactory);
        manager.setRuleReaderFrontendFactory(ruleReaderFrontendFactory);
        return manager;
    }

    /** The properties file {@link GeoFenceConfigurationManager} reads/saves through. */
    private GeoServerPropertyConfigurer resolveManagerConfigurer(GeoServerDataDirectory dataDirectory) {
        GeoServerPropertyConfigurer configurer = new GeoServerPropertyConfigurer(dataDirectory);
        configurer.setLocation(context.getResource(CONFIG_LOCATION));
        return configurer;
    }

    @Bean
    public RuleReaderServiceFactory ruleReaderBackendFactory(GeoServerDataDirectory dataDirectory) {
        String backendName = loadProperties(dataDirectory).getProperty("ruleReaderBackend", "");
        return new RuleReaderServiceFactory(resolveRuleReaderBackend(backendName), false);
    }

    @Bean
    public RuleReaderServiceFactory ruleReaderFrontendFactory(GeoServerDataDirectory dataDirectory) {
        String frontendName = loadProperties(dataDirectory).getProperty("ruleReaderFrontend", "cachedRuleReader");
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
