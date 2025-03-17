/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir.config;

import static org.geoserver.config.DataDirectoryGeoServerLoader.GEOSERVER_DATA_DIR_LOADER_ENABLED;

import org.geoserver.config.DataDirectoryGeoServerLoader;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Spring configuration class that registers the {@link DataDirectoryGeoServerLoader} as a Spring bean.
 *
 * <p>This configuration is conditionally enabled based on the {@code GEOSERVER_DATA_DIR_LOADER_ENABLED} environment
 * variable or system property. If this property is not set, or set to "true", the optimized loader will be enabled.
 *
 * <p>The condition is implemented by the inner class {@link DataDirLoaderEnabledCondition}, which checks the property
 * value in both the system properties and the Spring environment.
 *
 * <p>This class also registers a {@link ModuleStatusImpl} bean to report the module's status in the GeoServer web admin
 * interface.
 */
@Configuration
@Conditional(value = DataDirectoryGeoServerLoaderConfiguration.DataDirLoaderEnabledCondition.class)
public class DataDirectoryGeoServerLoaderConfiguration {

    @Bean
    public DataDirectoryGeoServerLoader dataDirectoryGeoServerLoader(
            GeoServerDataDirectory dataDirectory, GeoServerSecurityManager securityManager) {
        return new DataDirectoryGeoServerLoader(dataDirectory, securityManager);
    }

    @Bean
    public ModuleStatusImpl moduleStatus() {
        ModuleStatusImpl module = new ModuleStatusImpl("gs-datadir-catalog-loader", "DataDirectory loader");
        module.setAvailable(true);
        module.setEnabled(true);
        return module;
    }

    /**
     * Spring condition that determines whether the optimized data directory loader should be enabled.
     *
     * <p>The condition checks the {@code GEOSERVER_DATA_DIR_LOADER_ENABLED} property in the following order:
     *
     * <ol>
     *   <li>Via {@link GeoServerExtensions#getProperty(String)} (system properties with fallback)
     *   <li>From the Spring {@link org.springframework.core.env.Environment}
     *   <li>Defaults to "true" if not specified in either location
     * </ol>
     *
     * <p>This condition allows users to disable the optimized loader by setting
     * {@code GEOSERVER_DATA_DIR_LOADER_ENABLED=false} in the environment or as a system property, in which case the
     * default loader will be used.
     */
    public static class DataDirLoaderEnabledCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value = GeoServerExtensions.getProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED);
            if (!StringUtils.hasText(value)) {
                value = context.getEnvironment().getProperty(GEOSERVER_DATA_DIR_LOADER_ENABLED);
            }
            if (!StringUtils.hasText(value)) {
                value = "true";
            }
            return Boolean.parseBoolean(value);
        }
    }
}
