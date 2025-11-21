/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.test.config;

import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.config.domain.AdminRuleAdminServiceConfiguration;
import org.geoserver.acl.config.domain.AuthorizationServiceConfiguration;
import org.geoserver.acl.config.domain.RuleAdminServiceConfiguration;
import org.geoserver.acl.domain.adminrules.AdminRuleAdminService;
import org.geoserver.acl.domain.adminrules.AdminRuleRepository;
import org.geoserver.acl.domain.adminrules.MemoryAdminRuleRepository;
import org.geoserver.acl.domain.rules.MemoryRuleRepository;
import org.geoserver.acl.domain.rules.RuleAdminService;
import org.geoserver.acl.domain.rules.RuleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Loaded from {@literal src/test/resources/applicationContext-test.xml}, provides a memory implementation of
 * {@link RuleRepository} and {@link AdminRuleRepository}, and imports the configurations to load
 * {@link RuleAdminService}, {@link AdminRuleAdminService}, and {@link AuthorizationService}.
 *
 * <p>As a consequence, {@link AuthorizationService} runs in-process and uses rule and adminrule services backed by heap
 * storage.
 */
@Configuration
@Import({
    RuleAdminServiceConfiguration.class,
    AdminRuleAdminServiceConfiguration.class,
    AuthorizationServiceConfiguration.class
})
public class AuthorizationServiceTestConfig {

    @Bean
    RuleRepository memoryRuleRepository() {
        return new MemoryRuleRepository();
    }

    @Bean
    AdminRuleRepository memoryAdminRuleRepository() {
        return new MemoryAdminRuleRepository();
    }
}
