/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.datadir.config;

import java.util.Optional;
import org.geoserver.catalog.datadir.DataDirectoryGeoServerLoader;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * {@link Configuration @Configuration} class that contributes the {@link
 * DataDirectoryGeoServerLoader} to the {@link ApplicationContext} unless it's disabled.
 *
 * <p>This data directory loader is enabled by default, and hence becomes the new loader
 *
 * <p>To disable, set the System property, environment variable, or spring environment property
 * {@code datadir.loader.enabled} (or {@code DATADIR_LOADER_ENABLED}) to {@code false} (or anything
 * but {@code true} really).
 *
 * @see DataDirLoaderEnabledCondition
 * @since 2.25
 */
@Configuration(proxyBeanMethods = false)
@Conditional(value = DataDirectoryLoaderConfiguration.DataDirLoaderEnabledCondition.class)
public class DataDirectoryLoaderConfiguration {

    @Bean
    public DataDirectoryGeoServerLoader dataDirectoryGeoServerLoader(
            GeoServerResourceLoader resourceLoader,
            GeoServerSecurityManager securityManager,
            XStreamPersisterFactory xpf) {
        DataDirectoryGeoServerLoader loader =
                new DataDirectoryGeoServerLoader(resourceLoader, securityManager);
        loader.setXStreamPeristerFactory(xpf);
        return loader;
    }

    /**
     * Spring-context {@link Condition conditional} to determine whether the {@link
     * DataDirectoryGeoServerLoader} shall be used, works like a spring-boot's
     * {@code @ConditionalOnProperty(value="datadir.loader.enabled", havingValue="true",
     * matchIfMissing=true)}
     */
    public static class DataDirLoaderEnabledCondition implements Condition {

        public static final String SYSPROP_KEY = "datadir.loader.enabled";
        public static final String ENVVAR_KEY = "DATADIR_LOADER_ENABLED";

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value =
                    getProperty(context, SYSPROP_KEY)
                            .or(() -> getProperty(context, ENVVAR_KEY))
                            .orElse("true");
            return Boolean.parseBoolean(value);
        }

        private Optional<String> getProperty(ConditionContext context, String prop) {
            String value = GeoServerExtensions.getProperty(prop);
            if (!StringUtils.hasText(value)) {
                // GeoServerExtensions.getProperty() doesn't check the Environment property
                // doing it here, with lower priority than env variables and system properties,
                // so it also works with GeoServer Cloud's externalized configuration
                value = context.getEnvironment().getProperty(prop);
            }
            return Optional.ofNullable(value);
        }
    }
}
