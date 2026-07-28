/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.config;

import java.util.Properties;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.geofence.config.GeoFencePropertyPlaceholderConfigurer;
import org.geoserver.geofence.server.rest.GeofenceSecurityInterceptor;
import org.geoserver.geofence.server.web.GeofenceServerAdminPage;
import org.geoserver.geofence.server.web.GeofenceServerPage;
import org.geoserver.web.Category;
import org.geoserver.web.MenuPageInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Java-based wiring for the {@code geofence-server} module's beans that need literal constructor arguments,
 * property-placeholder resolution, or MVC interceptor registration, replacing the module's former
 * {@code applicationContext.xml} bean definitions.
 */
@Configuration
public class GeoFenceServerSpringConfig implements ApplicationContextAware, WebMvcConfigurer {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    @Bean(name = "geofence-server-configurer")
    public GeoFencePropertyPlaceholderConfigurer geofenceServerConfigurer(GeoServerDataDirectory dataDirectory) {
        GeoFencePropertyPlaceholderConfigurer configurer = new GeoFencePropertyPlaceholderConfigurer(dataDirectory);
        configurer.setOrder(99);
        configurer.setIgnoreResourceNotFound(true);
        configurer.setIgnoreUnresolvablePlaceholders(true);

        // This location is relative to the datadir
        configurer.setLocation(context.getResource("file:geofence/geofence-server.properties"));

        Properties props = new Properties();
        // ruleReaderBackend: no default here, see GeoFenceSpringConfig.resolveRuleReaderBackend()
        // The frontend will be injected in the access manager.
        // You may replace this value with ruleReaderServiceImpl in order to disable the caching
        props.setProperty("ruleReaderFrontend", "cachedRuleReader");
        configurer.setProperties(props);

        return configurer;
    }

    @Bean
    public MenuPageInfo<GeofenceServerPage> geofenceServerPage(
            @Qualifier("securityCategory") Category securityCategory) {
        MenuPageInfo<GeofenceServerPage> page = new MenuPageInfo<>();
        page.setId("geofenceServerPage");
        page.setTitleKey("GeofenceServerPage.page.title");
        page.setDescriptionKey("GeofenceServerPage.page.description");
        page.setComponentClass(GeofenceServerPage.class);
        page.setCategory(securityCategory);
        page.setOrder(1001);
        page.setIcon("img/icons/geofence.png");
        return page;
    }

    @Bean
    public MenuPageInfo<GeofenceServerAdminPage> geofenceServerAdminPage(
            @Qualifier("securityCategory") Category securityCategory) {
        MenuPageInfo<GeofenceServerAdminPage> page = new MenuPageInfo<>();
        page.setId("geofenceServerAdminPage");
        page.setTitleKey("GeofenceServerAdminPage.page.title");
        page.setDescriptionKey("GeofenceServerAdminPage.page.description");
        page.setComponentClass(GeofenceServerAdminPage.class);
        page.setCategory(securityCategory);
        page.setOrder(1002);
        page.setIcon("img/icons/geofence.png");
        return page;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GeofenceSecurityInterceptor());
    }
}
