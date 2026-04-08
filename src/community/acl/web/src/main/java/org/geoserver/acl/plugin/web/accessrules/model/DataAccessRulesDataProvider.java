/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.GeofenceRulesModel)
 */
package org.geoserver.acl.plugin.web.accessrules.model;

import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.acl.domain.rules.RuleAdminService;
import org.geoserver.acl.domain.rules.RuleIdentifierConflictException;
import org.geoserver.acl.plugin.web.components.RulesDataProvider;
import org.geoserver.acl.plugin.web.components.RulesTablePanel;
import org.geoserver.acl.plugin.web.support.ApplicationContextSupport;
import org.geoserver.acl.plugin.web.support.RuleBeanProperty;
import org.springframework.dao.DuplicateKeyException;

/**
 * @author Niels Charlier - Originally as part of GeoFence's GeoServer extension
 * @author Gabriel Roldan - Camptocamp
 */
@SuppressWarnings("serial")
public class DataAccessRulesDataProvider extends RulesDataProvider<MutableRule> {

    public static final Property<MutableRule> PRIORITY = new BeanProperty<>("priority");
    public static final Property<MutableRule> ROLE = RuleBeanProperty.of("roleName");
    public static final Property<MutableRule> USER = RuleBeanProperty.of("userName");
    public static final Property<MutableRule> SERVICE = RuleBeanProperty.of("service");
    public static final Property<MutableRule> REQUEST = RuleBeanProperty.of("request");
    public static final Property<MutableRule> SUBFIELD = RuleBeanProperty.of("subfield");
    public static final Property<MutableRule> WORKSPACE = RuleBeanProperty.of("workspace");
    public static final Property<MutableRule> LAYER = RuleBeanProperty.of("layer", "layer");
    public static final Property<MutableRule> ACCESS = RuleBeanProperty.of("access");
    public static final Property<MutableRule> ADDRESS_RANGE = RuleBeanProperty.of("addressRange");
    public static final Property<MutableRule> BUTTONS = RulesTablePanel.buttons();

    public DataAccessRulesDataProvider() {
        super(MutableRule.class);
    }

    @Override
    public List<Property<MutableRule>> getProperties() {
        return List.of(
                PRIORITY, ROLE, USER, SERVICE, REQUEST, SUBFIELD, WORKSPACE, LAYER, ACCESS, ADDRESS_RANGE, BUTTONS);
    }

    private static RuleAdminService adminService() {
        return ApplicationContextSupport.getBeanOfType(RuleAdminService.class);
    }

    @Override
    protected void delete(MutableRule rule) {
        adminService().delete(rule.getId());
    }

    @Override
    protected List<MutableRule> doReload() {
        return adminService().getAll().map(MutableRule::new).collect(Collectors.toList());
    }

    @Override
    protected void swap(MutableRule rule, MutableRule otherRule) {
        adminService().swapPriority(rule.getId(), otherRule.getId());
        long p = otherRule.getPriority();
        otherRule.setPriority(rule.getPriority());
        rule.setPriority(p);
    }

    @Override
    protected MutableRule update(MutableRule rule) throws DuplicateKeyException {
        Rule bigRule = adminService().get(rule.getId()).orElseThrow();
        bigRule = rule.toRule(bigRule);
        try {
            bigRule = adminService().update(bigRule);
        } catch (RuleIdentifierConflictException e) {
            throw new DuplicateKeyException(e.getMessage(), e);
        }
        return new MutableRule(bigRule);
    }

    @Override
    protected String getId(MutableRule r) {
        return r.getId();
    }

    @Override
    protected long getPriority(MutableRule r) {
        return r.getPriority();
    }

    @Override
    protected void setPriority(MutableRule r, long p) {
        r.setPriority(p);
    }
}
