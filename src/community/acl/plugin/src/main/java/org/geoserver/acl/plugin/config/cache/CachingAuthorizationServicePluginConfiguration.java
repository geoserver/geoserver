/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.config.cache;

import jakarta.annotation.PostConstruct;
import java.util.logging.Logger;
import org.geoserver.acl.authorization.cache.CachingAuthorizationService;
import org.geoserver.acl.authorization.cache.CachingAuthorizationServiceConfiguration;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geotools.util.logging.Logging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Plugin-specific extension for {@link CachingAuthorizationServiceConfiguration} to support GeoServer without spring
 * boot enabling and disabling through its {@literal @ConditionalOnAclEnabled} annotation.
 *
 * @see CachingAuthorizationServiceConfiguration
 */
@Configuration
@Import(CachingAuthorizationServiceConfiguration.class)
public class CachingAuthorizationServicePluginConfiguration {
    private static final Logger log = Logging.getLogger(CachingAuthorizationServicePluginConfiguration.class);

    @PostConstruct
    void logUsing() {
        log.info("Caching ACL AuthorizationService enabled");
    }

    @Bean
    CachingAclAuthorizationCleanupService cachingAclAuthorizationCleanupService(
            CachingAuthorizationService cachingService) {
        return new CachingAclAuthorizationCleanupService(cachingService);
    }

    static class CachingAclAuthorizationCleanupService implements GeoServerLifecycleHandler {
        private CachingAuthorizationService cachingService;

        public CachingAclAuthorizationCleanupService(CachingAuthorizationService cachingService) {
            this.cachingService = cachingService;
        }

        @Override
        public void onReset() {
            cachingService.evictAll();
        }

        @Override
        public void onDispose() {
            // no=op
        }

        @Override
        public void beforeReload() {
            // no=op
        }

        @Override
        public void onReload() {
            // no=op
        }
    }
}
