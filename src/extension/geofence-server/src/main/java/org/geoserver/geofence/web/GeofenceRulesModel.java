/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.geofence.core.model.LayerDetails;
import org.geoserver.geofence.core.model.Rule;
import org.geoserver.geofence.core.model.RuleLimits;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.locationtech.jts.geom.MultiPolygon;

/**
 * Functions as between webgui and internal geofence db
 *
 * @author Niels Charlier
 */
public class GeofenceRulesModel extends GeoServerDataProvider<ShortRule> {

    private static final long serialVersionUID = 478867886089304835L;

    /** Makes columns that are unsortable and display "*" instead of empty when null */
    public static class RuleBeanProperty<T> extends BeanProperty<T> {
        private static final long serialVersionUID = 483799722644223445L;

        public RuleBeanProperty(String key, String propertyPath) {
            super(key, propertyPath);
        }

        @Override
        public Comparator<T> getComparator() { // unsortable
            return null;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public IModel getModel(IModel<T> itemModel) { // replace null by *
            return new PropertyModel<Object>(itemModel, getPropertyPath()) {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    Object o = super.getObject();
                    return o == null ? "*" : o;
                }
            };
        }
    }

    public static final Property<ShortRule> PRIORITY =
            new BeanProperty<ShortRule>("priority", "priority");

    public static final Property<ShortRule> ROLE =
            new RuleBeanProperty<ShortRule>("roleName", "roleName");

    public static final Property<ShortRule> USER =
            new RuleBeanProperty<ShortRule>("userName", "userName");

    public static final Property<ShortRule> SERVICE =
            new RuleBeanProperty<ShortRule>("service", "service");

    public static final Property<ShortRule> REQUEST =
            new RuleBeanProperty<ShortRule>("request", "request");

    public static final Property<ShortRule> WORKSPACE =
            new RuleBeanProperty<ShortRule>("workspace", "workspace");

    public static final Property<ShortRule> LAYER =
            new RuleBeanProperty<ShortRule>("layer", "layer");

    public static final Property<ShortRule> ACCESS =
            new RuleBeanProperty<ShortRule>("access", "access");

    public static final Property<ShortRule> BUTTONS = new PropertyPlaceholder<ShortRule>("buttons");

    private static RuleAdminService adminService() {
        return (RuleAdminService) GeoServerApplication.get().getBean("ruleAdminService");
    }

    /**
     * We will keep local copy, always sorted on priority to support the up and down arrows easily
     */
    protected List<ShortRule> rules;

    public GeofenceRulesModel() {
        rules = adminService().getAll();
        setSort("priority", SortOrder.ASCENDING);
    }

    @Override
    protected Comparator<ShortRule> getComparator(SortParam<?> sort) {
        return null; // disable on-the-fly sorting
    }

    @Override
    public void setSort(Object property, SortOrder order) {
        super.setSort(property, order);
        Collections.sort(
                rules,
                super.getComparator(new SortParam<>(property, order == SortOrder.ASCENDING)));
    }

    @Override
    public void setSort(SortParam<Object> param) {
        super.setSort(param);
        // enable in-memory sorting
        Collections.sort(rules, super.getComparator(param));
    }

    @Override
    protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<ShortRule>>
            getProperties() {
        return Arrays.asList(
                PRIORITY, ROLE, USER, SERVICE, REQUEST, WORKSPACE, LAYER, ACCESS, BUTTONS);
    }

    @Override
    protected List<ShortRule> getItems() {
        return rules;
    }

    public void save(ShortRule rule) {
        if (rule.getId() == null) {
            shiftIfNecessary(rule.getPriority(), rule);

            // local
            int i = 0;
            while (i < rules.size() && rules.get(i).getPriority() < rule.getPriority()) {
                i++;
            }
            rules.add(i, rule);

            // db
            Rule bigRule = new Rule();
            syncRule(rule, bigRule);
            rule.setId(adminService().insert(bigRule));
        } else {
            // db
            Rule bigRule = adminService().get(rule.getId());
            if (bigRule.getPriority() != rule.getPriority()) {
                shiftIfNecessary(rule.getPriority(), rule);
            }
            syncRule(rule, bigRule);
            adminService().update(bigRule);
        }
    }

    public void remove(Collection<ShortRule> selected) {
        // local
        rules.removeAll(selected);

        // db
        for (ShortRule rule : selected) {
            adminService().delete(rule.getId());
        }
    }

    public boolean canUp(ShortRule rule) {
        return rules.indexOf(rule) > 0;
    }

    public void moveUp(ShortRule rule) {
        int index = rules.indexOf(rule);
        if (index > 0) {
            swap(rule, rules.get(index - 1));
            // local sort
            rules.remove(index);
            rules.add(index - 1, rule);
        }
    }

    public boolean canDown(ShortRule rule) {
        return rules.indexOf(rule) < rules.size() - 1;
    }

    public void moveDown(ShortRule rule) {
        int index = rules.indexOf(rule);
        if (index < rules.size() - 1) {
            swap(rule, rules.get(index + 1));
            // local sort
            rules.remove(index);
            rules.add(index + 1, rule);
        }
    }

    protected void swap(ShortRule rule, ShortRule otherRule) {
        // local
        long p = otherRule.getPriority();
        otherRule.setPriority(rule.getPriority());
        rule.setPriority(p);

        // db
        adminService().swap(rule.getId(), otherRule.getId());
    }

    protected void shiftIfNecessary(long priority, ShortRule keep) {
        // detect if necessary
        boolean necessary = false;
        for (ShortRule rule : rules) {
            if (rule.getPriority() == priority) {
                necessary = true;
                continue;
            }
        }
        if (necessary) {
            // local
            for (ShortRule rule : rules) {
                if (rule.getPriority() >= priority && rule != keep) {
                    rule.setPriority(rule.getPriority() + 1);
                }
            }

            // db
            adminService().shift(priority, 1);
        }
    }

    public ShortRule newRule() {
        ShortRule rule = new ShortRule();
        rule.setAccess(GrantType.ALLOW);
        rule.setPriority(0);
        return rule;
    }

    public void save(Long ruleId, MultiPolygon allowedArea, CatalogMode catalogMode) {
        Rule rule = adminService().get(ruleId);
        RuleLimits ruleLimits = rule.getRuleLimits();
        if (ruleLimits == null) {
            ruleLimits = new RuleLimits();
            ruleLimits.setRule(rule);
        }
        ruleLimits.setAllowedArea(allowedArea);
        ruleLimits.setCatalogMode(catalogMode);
        adminService().setLimits(ruleId, ruleLimits);
    }

    public void save(Long ruleId, LayerDetails layerDetails) {
        adminService().setDetails(ruleId, layerDetails);
    }

    public RuleLimits getRulesLimits(Long ruleId) {
        if (ruleId != null) {
            Rule rule = adminService().get(ruleId);
            if (rule != null) {
                return rule.getRuleLimits();
            }
        }
        return null;
    }

    protected static void syncRule(ShortRule shortRule, Rule rule) {
        rule.setPriority(shortRule.getPriority());
        rule.setUsername(shortRule.getUserName());
        rule.setRolename(shortRule.getRoleName());
        rule.setService(shortRule.getService());
        rule.setRequest(shortRule.getRequest());
        rule.setWorkspace(shortRule.getWorkspace());
        rule.setLayer(shortRule.getLayer());
        rule.setAccess(shortRule.getAccess());
    }

    public LayerDetails getDetails(Long ruleId) {
        if (ruleId != null) {
            Rule rule = adminService().get(ruleId);
            if (rule != null) {
                return rule.getLayerDetails();
            }
        }
        return null;
    }
}
