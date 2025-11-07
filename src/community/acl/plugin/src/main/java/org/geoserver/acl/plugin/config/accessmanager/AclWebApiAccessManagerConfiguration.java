/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.config.accessmanager;

import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.domain.adminrules.AdminRuleAdminService;
import org.geoserver.acl.domain.rules.RuleAdminService;
import org.geoserver.acl.plugin.accessmanager.AclResourceAccessManager;
import org.geoserver.acl.plugin.accessmanager.AclResourceAccessManagerSpringConfig;
import org.geoserver.acl.plugin.config.domain.client.ApiClientAclDomainServicesConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link Configuration @Configuration} for the GeoServer Access Control List {@link AclResourceAccessManager} operating
 * against the REST API.
 *
 * <p>
 *
 * <ul>
 *   <li>{@link ApiClientAclDomainServicesConfiguration} contributes the {@link AuthorizationService},
 *       {@link RuleAdminService}, and {@link AdminRuleAdminService}, that work by delegating to a remote ACL service
 *       through the OpenAPI HTTP interface.
 *   <li>{@link AclResourceAccessManagerSpringConfig} sets up the {@link AclResourceAccessManager} with the
 *       implementations provided by {@code ApiClientAclDomainServicesConfiguration}
 * </ul>
 *
 * @see ApiClientAclDomainServicesConfiguration
 * @see AccessManagerSpringConfig
 */
@Configuration
@Import({ApiClientAclDomainServicesConfiguration.class, AclResourceAccessManagerSpringConfig.class})
public class AclWebApiAccessManagerConfiguration {}
