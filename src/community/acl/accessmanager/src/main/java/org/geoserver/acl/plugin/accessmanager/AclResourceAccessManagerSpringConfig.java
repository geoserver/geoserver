/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.accessmanager;

import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AclResourceAccessManagerSpringConfig {

    @Bean
    AuthorizationServiceConfig aclConfig(Environment env) {
        AuthorizationServiceConfig config = new AuthorizationServiceConfig();
        String serviceUrl = env.getProperty("geoserver.acl.client.basePath");
        config.setServiceUrl(serviceUrl);
        return config;
    }

    @Bean
    AclResourceAccessManager aclAccessManager(
            AuthorizationService aclService,
            AuthorizationServiceConfig configuration,
            LayerGroupContainmentCache groupsCache,
            AclWPSHelper wpsHelper) {

        return new AclResourceAccessManager(aclService, groupsCache, configuration, wpsHelper);
    }

    @Bean
    WmsRequestAclEnforcerInterceptor aclDispatcherCallback(
            AuthorizationService aclAuthorizationService, @Qualifier("rawCatalog") Catalog rawCatalog) {

        LocalWorkspaceCatalog localWorkspaceCatalog = new LocalWorkspaceCatalog(rawCatalog);
        return new WmsRequestAclEnforcerInterceptor(aclAuthorizationService, localWorkspaceCatalog);
    }

    @Bean
    AclWPSHelper noOpAclWpsHelper(AuthorizationService aclAuthService) {
        return AclWPSHelper.NO_OP;
    }
}
