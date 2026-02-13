/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thetransactioncompany.cors.CORSFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import java.util.Arrays;
import java.util.EnumSet;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.WebApplicationContext;

public class CORSInitializerTest {

    private CORSInitializer initializer;
    private GeoServer geoServer;
    private ServletContext servletContext;
    private FilterRegistration.Dynamic filterRegistration;
    private CORSConfiguration config;

    @BeforeEach
    void setUp() {
        initializer = new CORSInitializer();
        geoServer = mock(GeoServer.class);
        servletContext = mock(ServletContext.class);
        filterRegistration = mock(FilterRegistration.Dynamic.class);

        // Mock the Spring WebApplicationContext to provide the ServletContext
        WebApplicationContext webContext = mock(WebApplicationContext.class);
        when(webContext.getServletContext()).thenReturn(servletContext);
        initializer.setApplicationContext(webContext);

        // Setup GeoServer Settings mocks
        SettingsInfo settings = mock(SettingsInfo.class);
        MetadataMap metadata = new MetadataMap();
        when(geoServer.getSettings()).thenReturn(settings);
        when(settings.getMetadata()).thenReturn(metadata);

        // Prepare the config object
        config = new CORSConfiguration();
        metadata.put(CORSConfiguration.CORS_CONFIGURATION_METADATA_KEY, config);

        // Mock filter registration behavior
        when(servletContext.addFilter(eq("CORS"), any(CORSFilter.class))).thenReturn(filterRegistration);
    }

    @Test
    void testInitializeEnabled() throws Exception {
        // Arrange
        config.setEnabled(true);
        config.setAllowedOriginPatterns("*");
        config.setAllowedMethods(Arrays.asList("GET", "POST"));
        config.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
        config.setSupportsCredentials(true);
        config.setMaxAge(3600);

        // Act
        initializer.initialize(geoServer);

        // Assert
        verify(servletContext).addFilter(eq("CORS"), any(CORSFilter.class));
        verify(filterRegistration).setInitParameter(CORSInitializer.CORS_ALLOW_ORIGIN, "*");
        verify(filterRegistration).setInitParameter(CORSInitializer.CORS_SUPPORTED_METHODS, "GET,POST");
        verify(filterRegistration)
                .setInitParameter(CORSInitializer.CORS_SUPPORTED_HEADERS, "Content-Type,Authorization");
        verify(filterRegistration).setInitParameter(CORSInitializer.CORS_SUPPORTS_CREDENTIALS, "true");
        verify(filterRegistration).setInitParameter(CORSInitializer.CORS_MAX_AGE, "3600");

        verify(filterRegistration).addMappingForUrlPatterns(eq(EnumSet.of(DispatcherType.REQUEST)), eq(true), eq("/*"));
    }

    @Test
    void testInitializeDisabled() throws Exception {
        // Arrange
        config.setEnabled(false);

        // Act
        initializer.initialize(geoServer);

        // Assert
        verify(servletContext, never()).addFilter(anyString(), any(CORSFilter.class));
    }

    @Test
    void testInitializeNullConfig() throws Exception {
        // Arrange: Remove config from metadata
        when(geoServer.getSettings().getMetadata()).thenReturn(new MetadataMap());

        // Act
        initializer.initialize(geoServer);

        // Assert
        verify(servletContext, never()).addFilter(anyString(), any(CORSFilter.class));
    }
}
