/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.config.spring;

import org.geoserver.acl.plugin.config.accessmanager.AclWebApiAccessManagerConfiguration;
import org.geoserver.acl.plugin.config.cache.CachingAuthorizationServicePluginConfiguration;
import org.geoserver.acl.plugin.config.webui.AclWebUIConfiguration;
import org.geoserver.acl.plugin.config.wps.AclWpsIntegrationConfiguration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Single Spring configuration entry point for GeoServer.
 *
 * <p>This configuration is loaded by component scan defined in {@literal applicationContext.xml}.
 *
 * <p>GeoServer Cloud will use Spring Boot's AutoConfiguration mechanism instead, hence
 * {@link AclWebApiAccessManagerConfiguration}, {@link AclWebUIConfiguration}, {@link AclWpsIntegrationConfiguration},
 * and {@link CachingAuthorizationServicePluginConfiguration} are each in their own packages.
 */
@Configuration
@Conditional(AclEnabledCondition.class)
@Import({
    AclWebApiAccessManagerConfiguration.class,
    AclWebUIConfiguration.class,
    AclWpsIntegrationConfiguration.class,
    CachingAuthorizationServicePluginConfiguration.class
})
public class AclPluginSpringConfiguration {}
