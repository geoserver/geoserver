/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.GeofenceAdminRulesModel)
 */
package org.geoserver.acl.plugin.web.adminrules.model;

import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.acl.domain.adminrules.AdminRule;
import org.geoserver.acl.domain.adminrules.AdminRuleAdminService;
import org.geoserver.acl.domain.adminrules.AdminRuleIdentifierConflictException;
import org.geoserver.acl.plugin.web.components.RulesDataProvider;
import org.geoserver.acl.plugin.web.components.RulesTablePanel;
import org.geoserver.acl.plugin.web.support.ApplicationContextSupport;
import org.geoserver.acl.plugin.web.support.RuleBeanProperty;
import org.springframework.dao.DuplicateKeyException;

@SuppressWarnings("serial")
public class AdminRulesTableDataProvider extends RulesDataProvider<MutableAdminRule> {

    public static final Property<MutableAdminRule> PRIORITY = RuleBeanProperty.of("priority");
    public static final Property<MutableAdminRule> ROLE = RuleBeanProperty.of("roleName");
    public static final Property<MutableAdminRule> USER = RuleBeanProperty.of("userName");
    public static final Property<MutableAdminRule> WORKSPACE = RuleBeanProperty.of("workspace");
    public static final Property<MutableAdminRule> ACCESS = RuleBeanProperty.of("access");
    public static final Property<MutableAdminRule> BUTTONS = RulesTablePanel.buttons();

    public AdminRulesTableDataProvider() {
        super(MutableAdminRule.class);
    }

    private static AdminRuleAdminService adminService() {
        return ApplicationContextSupport.getBeanOfType(AdminRuleAdminService.class);
    }

    @Override
    public List<Property<MutableAdminRule>> getProperties() {
        return List.of(PRIORITY, ROLE, USER, WORKSPACE, ACCESS, BUTTONS);
    }

    @Override
    protected List<MutableAdminRule> doReload() {
        return adminService().getAll().map(MutableAdminRule::new).collect(Collectors.toList());
    }

    protected @Override void delete(MutableAdminRule rule) {
        adminService().delete(rule.getId());
    }

    @Override
    protected void swap(MutableAdminRule rule, MutableAdminRule otherRule) {
        adminService().swap(rule.getId(), otherRule.getId());
        long p = otherRule.getPriority();
        otherRule.setPriority(rule.getPriority());
        rule.setPriority(p);
    }

    protected @Override String getId(MutableAdminRule r) {
        return r.getId();
    }

    protected @Override long getPriority(MutableAdminRule r) {
        return r.getPriority();
    }

    protected @Override void setPriority(MutableAdminRule r, long p) {
        r.setPriority(p);
    }

    @Override
    protected MutableAdminRule update(MutableAdminRule rule) {
        AdminRule bigRule = adminService().get(rule.getId()).orElseThrow();
        bigRule = rule.toRule(bigRule);
        try {
            bigRule = adminService().update(bigRule);
        } catch (AdminRuleIdentifierConflictException e) {
            throw new DuplicateKeyException(e.getMessage(), e);
        }
        return new MutableAdminRule(bigRule);
    }
}
