/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.datadir.config;

import org.geoserver.catalog.datadir.DataDirectoryGeoServerLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

@Configuration
@Conditional(value = DataDirectoryLoaderConfiguration.DataDirLoaderEnabledCondition.class)
public class DataDirectoryLoaderConfiguration {

    @Bean
    public DataDirectoryGeoServerLoader dataDirectoryGeoServerLoader(
            GeoServerResourceLoader resourceLoader, GeoServerSecurityManager securityManager) {
        return new DataDirectoryGeoServerLoader(resourceLoader, securityManager);
    }

    @Bean
    public ModuleStatusImpl moduleStatus() {
        ModuleStatusImpl module =
                new ModuleStatusImpl("gs-datadir-catalog-loader", "DataDirectory loader");
        module.setAvailable(true);
        module.setEnabled(true);
        return module;
    }

    public static class DataDirLoaderEnabledCondition implements Condition {

        public static final String KEY = "datadir.loader.enabled";

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value = GeoServerExtensions.getProperty(KEY);
            if (!StringUtils.hasText(value)) {
                value = context.getEnvironment().getProperty(KEY);
            }
            if (!StringUtils.hasText(value)) {
                value = "true";
            }
            return Boolean.parseBoolean(value);
        }
    }
}
