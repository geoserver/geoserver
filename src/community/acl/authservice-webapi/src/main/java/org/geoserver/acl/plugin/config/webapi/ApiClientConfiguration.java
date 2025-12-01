/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.config.webapi;

import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.config.domain.DomainServicesConfiguration;
import org.geoserver.acl.config.webapi.client.ApiClientAdapterConfiguration;
import org.geoserver.acl.config.webapi.client.ApiClientApplicationServicesConfiguration;
import org.geoserver.acl.config.webapi.client.ApiClientDomainPortsConfiguration;
import org.geoserver.acl.config.webapi.client.ApiClientProperties;
import org.geoserver.acl.domain.adminrules.AdminRuleAdminService;
import org.geoserver.acl.domain.adminrules.AdminRuleRepository;
import org.geoserver.acl.domain.rules.RuleAdminService;
import org.geoserver.acl.domain.rules.RuleRepository;
import org.geoserver.acl.webapi.client.AclClientAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * Configuration to contribute a GeoSever ACL {@link AuthorizationService} that works by delegating to a remote ACL
 * service through the OpenAPI HTTP interface.
 *
 * <p>Contributes:
 *
 * <ul>
 *   <li>{@link ApiClientProperties} required by {@link ApiClientAdapterConfiguration}
 *   <li>{@link AclClientAdapter} from {@link ApiClientAdapterConfiguration}
 *   <li>{@link RuleRepository} and {@link AdminRuleRepository} from {@link ApiClientDomainPortsConfiguration}
 *   <li>{@link RuleAdminService} and {@link AdminRuleAdminService} from {@link DomainServicesConfiguration}
 *   <li>{@link AuthorizationService} from {@link ApiClientApplicationServicesConfiguration}
 * </ul>
 *
 * <p>The net effect of this {@code @Configuration} class is the {@link ApplicationContext} is set up with GeoServer ACL
 * with authorization and domain services backed by the REST API client.
 *
 * @see ApiClientAdapterConfiguration
 * @see ApiClientDomainPortsConfiguration
 * @see ApiClientApplicationServicesConfiguration
 */
@Configuration
@Import({
    // AclClientAdapter from org.geoserver.acl.integration.openapi:gs-acl-api-client
    ApiClientAdapterConfiguration.class,
    // domain ports from org.geoserver.acl.integration.openapi:gs-acl-api-client
    ApiClientDomainPortsConfiguration.class,
    // AuthorizationService from
    // org.geoserver.acl.integration.openapi:gs-acl-api-client
    ApiClientApplicationServicesConfiguration.class,
    // domain services from
    // org.geoserver.acl.integration:gs-acl-domain-spring-integration
    DomainServicesConfiguration.class
})
public class ApiClientConfiguration {

    @Bean
    ApiClientProperties aclApiClientProperties(Environment env) {
        String basePath = env.getProperty("geoserver.acl.client.basePath");
        String username = env.getProperty("geoserver.acl.client.username");
        String password = env.getProperty("geoserver.acl.client.password");
        boolean caching = env.getProperty("geoserver.acl.client.caching", Boolean.class, true);
        boolean startupCheck = env.getProperty("geoserver.acl.client.startupCheck", Boolean.class, true);
        Integer initTimeout = env.getProperty("geoserver.acl.client.initTimeout", Integer.class);

        ApiClientProperties configProps = new ApiClientProperties();
        configProps.setBasePath(basePath);
        configProps.setUsername(username);
        configProps.setPassword(password);
        configProps.setCaching(caching);
        configProps.setStartupCheck(startupCheck);
        if (null != initTimeout) configProps.setInitTimeout(initTimeout);

        return configProps;
    }
}
