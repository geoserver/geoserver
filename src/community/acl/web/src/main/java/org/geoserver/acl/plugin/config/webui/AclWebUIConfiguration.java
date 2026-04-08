/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.config.webui;

import org.geoserver.acl.plugin.web.accessrules.AccessRulesACLPage;
import org.geoserver.acl.plugin.web.adminrules.AdminRulesACLPage;
import org.geoserver.acl.plugin.web.config.ACLServiceConfigPage;
import org.geoserver.acl.plugin.web.css.CSSConfiguration;
import org.geoserver.web.Category;
import org.geoserver.web.MenuPageInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CSSConfiguration.class)
public class AclWebUIConfiguration {

    @Bean
    MenuPageInfo<ACLServiceConfigPage> aclServiceConfigPageMenuInfo(@Qualifier("securityCategory") Category category) {
        MenuPageInfo<ACLServiceConfigPage> info = new MenuPageInfo<>();
        info.setId("aclServiceConfigPageMenuInfo");
        info.setTitleKey("ACLServiceConfigPage.page.title");
        info.setDescriptionKey("ACLServiceConfigPage.page.description");
        info.setComponentClass(ACLServiceConfigPage.class);
        info.setCategory(category);
        info.setOrder(1000);
        // relative to component class
        info.setIcon("../img/acl.svg");
        return info;
    }

    @Bean
    MenuPageInfo<AccessRulesACLPage> accessRulesACLPageMenuInfo(@Qualifier("securityCategory") Category category) {
        MenuPageInfo<AccessRulesACLPage> info = new MenuPageInfo<>();
        info.setId("accessRulesACLPageMenuInfo");
        info.setTitleKey("AccessRulesACLPage.page.title");
        info.setDescriptionKey("AccessRulesACLPage.page.description");
        info.setComponentClass(AccessRulesACLPage.class);
        info.setCategory(category);
        info.setOrder(1001);
        // relative to component class
        info.setIcon("../img/acl.svg");
        return info;
    }

    @Bean
    MenuPageInfo<AdminRulesACLPage> adminRulesAclPageMenuInfo(@Qualifier("securityCategory") Category category) {
        MenuPageInfo<AdminRulesACLPage> info = new MenuPageInfo<>();
        info.setId("adminRulesAclPageMenuInfo");
        info.setTitleKey("AdminRulesACLPage.title");
        info.setDescriptionKey("AdminRulesACLPage.description");
        info.setComponentClass(AdminRulesACLPage.class);
        info.setCategory(category);
        info.setOrder(1002);
        // relative to component class
        info.setIcon("../img/acl.svg");
        return info;
    }
}
