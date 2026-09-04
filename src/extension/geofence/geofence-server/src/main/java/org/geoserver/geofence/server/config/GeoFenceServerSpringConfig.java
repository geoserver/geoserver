/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.config;

import org.geoserver.geofence.server.rest.GeofenceSecurityInterceptor;
import org.geoserver.geofence.server.web.GeofenceServerAdminPage;
import org.geoserver.geofence.server.web.GeofenceServerPage;
import org.geoserver.web.Category;
import org.geoserver.web.MenuPageInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Java-based wiring for the {@code geofence-server} module's beans that need literal constructor arguments or MVC
 * interceptor registration.
 *
 * <p>The embedded engine and {@code geofence.properties} are both handled by the base {@code gs-geofence} module's
 * {@code GeoFenceSpringConfig}, not here.
 */
@Configuration
public class GeoFenceServerSpringConfig implements WebMvcConfigurer {

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
