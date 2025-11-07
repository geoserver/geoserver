/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.config.wps;

import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.plugin.wps.AclWPSHelperImpl;
import org.geoserver.acl.plugin.wps.WPSChainStatusHolder;
import org.geoserver.acl.plugin.wps.WPSProcessListener;
import org.geoserver.wps.resource.WPSResourceManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
// spring with no spring boot equivalent to @ConditionalOnClass(WPSResourceManager.class)
@Conditional(value = WPSResourceManagerClassCondition.class)
public class AclWpsIntegrationConfiguration {

    @Primary
    @Bean
    AclWPSHelperImpl aclWpsHelper(
            WPSResourceManager wpsManager, AuthorizationService aclAuthService, WPSChainStatusHolder statusHolder) {
        return new AclWPSHelperImpl(wpsManager, aclAuthService, statusHolder);
    }

    @Bean
    WPSProcessListener aclWpsProcessListener(WPSChainStatusHolder statusHolder) {
        return new WPSProcessListener(statusHolder);
    }

    @Bean
    WPSChainStatusHolder aclWpsChainStatusHolder() {
        return new WPSChainStatusHolder();
    }
}
