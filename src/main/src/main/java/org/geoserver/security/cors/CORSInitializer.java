/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cors;

import static jakarta.servlet.DispatcherType.REQUEST;

import com.thetransactioncompany.cors.CORSFilter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import java.util.EnumSet;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.SettingsInfo;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.web.context.WebApplicationContext;

/** Use stored configuration to implement CORS headers */
public class CORSInitializer implements GeoServerInitializer, ApplicationContextAware {
    public static final String CORS_ALLOW_ORIGIN = "cors.allowOrigin";
    public static final String CORS_SUPPORTED_METHODS = "cors.supportedMethods";
    public static final String CORS_SUPPORTED_HEADERS = "cors.supportedHeaders";
    public static final String CORS_SUPPORTS_CREDENTIALS = "cors.supportsCredentials";
    public static final String CORS_MAX_AGE = "cors.maxAge";
    public static final String CORS = "CORS";
    public static final String URL_PATTERNS = "/*";
    ServletContext servletContext;

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        SettingsInfo info = geoServer.getSettings();
        MetadataMap metadata = info.getMetadata();
        CORSConfiguration corsSettings =
                metadata.get(CORSConfiguration.CORS_CONFIGURATION_METADATA_KEY, CORSConfiguration.class);
        if (corsSettings != null && corsSettings.isEnabled()) {
            // Register the CORSFilter dynamically
            FilterRegistration.Dynamic corsFilterRegistration = servletContext.addFilter(CORS, new CORSFilter());

            // Set initialization parameters for the CORSFilter
            // Example: Allow all origins and methods
            if (corsSettings.getAllowedOriginPatterns() != null
                    && !corsSettings.getAllowedOriginPatterns().isBlank()) {
                corsFilterRegistration.setInitParameter(CORS_ALLOW_ORIGIN, corsSettings.getAllowedOriginPatterns());
            }
            if (corsSettings.getAllowedMethods() != null
                    && !corsSettings.getAllowedMethods().isEmpty()) {
                corsFilterRegistration.setInitParameter(
                        CORS_SUPPORTED_METHODS, String.join(",", corsSettings.getAllowedMethods()));
            }
            if (corsSettings.getAllowedHeaders() != null
                    && !corsSettings.getAllowedHeaders().isEmpty()) {
                corsFilterRegistration.setInitParameter(
                        CORS_SUPPORTED_HEADERS, String.join(",", corsSettings.getAllowedHeaders()));
            }
            if (corsSettings.getSupportsCredentials() != null) {
                corsFilterRegistration.setInitParameter(
                        CORS_SUPPORTS_CREDENTIALS,
                        corsSettings.getSupportsCredentials().toString());
            }
            if (corsSettings.getMaxAge() != null) {
                corsFilterRegistration.setInitParameter(
                        CORS_MAX_AGE, corsSettings.getMaxAge().toString());
            }

            // Map the filter to all URL patterns
            corsFilterRegistration.addMappingForUrlPatterns(EnumSet.of(REQUEST), true, URL_PATTERNS);
        }
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof WebApplicationContext context) {
            servletContext = context.getServletContext();
        }
    }
}
