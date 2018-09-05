/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import java.util.*;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.geofence.core.model.AdminRule;
import org.geoserver.geofence.core.model.enums.AdminGrantType;
import org.geoserver.geofence.services.AdminRuleAdminService;
import org.geoserver.geofence.services.dto.ShortAdminRule;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class GeofenceAdminRulesModel extends GeoServerDataProvider<ShortAdminRule> {

    private static final long serialVersionUID = 2987962533487848796L;

    public static class RuleBeanProperty<T> extends BeanProperty<T> {
        private static final long serialVersionUID = 3626448043686728925L;

        public RuleBeanProperty(String key, String propertyPath) {
            super(key, propertyPath);
        }

        @Override
        public Comparator<T> getComparator() {
            return null;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public IModel getModel(IModel<T> itemModel) {
            return new PropertyModel<Object>(itemModel, getPropertyPath()) {
                private static final long serialVersionUID = -3213885135907358752L;

                @Override
                public Object getObject() {
                    Object o = super.getObject();
                    return o == null ? "*" : o;
                }
            };
        }
    }

    public static final Property<ShortAdminRule> PRIORITY =
            new BeanProperty<>("priority", "priority");

    public static final Property<ShortAdminRule> ROLE =
            new RuleBeanProperty<>("roleName", "roleName");

    public static final Property<ShortAdminRule> USER =
            new RuleBeanProperty<>("userName", "userName");

    public static final Property<ShortAdminRule> WORKSPACE =
            new RuleBeanProperty<>("workspace", "workspace");

    public static final Property<ShortAdminRule> ACCESS =
            new RuleBeanProperty<>("access", "access");

    public static final Property<ShortAdminRule> BUTTONS = new PropertyPlaceholder<>("buttons");

    private static AdminRuleAdminService adminService() {
        return (AdminRuleAdminService) GeoServerApplication.get().getBean("adminRuleAdminService");
    }

    protected List<ShortAdminRule> rules;

    public GeofenceAdminRulesModel() {
        rules = adminService().getAll();
        setSort("priority", SortOrder.ASCENDING);
    }

    @Override
    protected Comparator<ShortAdminRule> getComparator(SortParam<?> sort) {
        return null;
    }

    @Override
    public void setSort(SortParam<Object> param) {
        super.setSort(param);
        Collections.sort(rules, super.getComparator(param));
    }

    @Override
    protected List<Property<ShortAdminRule>> getProperties() {
        return Arrays.asList(PRIORITY, ROLE, USER, WORKSPACE, ACCESS, BUTTONS);
    }

    @Override
    protected List<ShortAdminRule> getItems() {
        return rules;
    }

    public void save(ShortAdminRule rule) {
        if (rule.getId() == null) {
            shiftIfNecessary(rule.getPriority(), rule);
            int i = 0;
            while (i < rules.size() && rules.get(i).getPriority() < rule.getPriority()) {
                i++;
            }
            rules.add(i, rule);
            AdminRule bigRule = new AdminRule();
            syncRule(rule, bigRule);
            rule.setId(adminService().insert(bigRule));
        } else {
            AdminRule bigRule = adminService().get(rule.getId());
            if (bigRule.getPriority() != rule.getPriority()) {
                shiftIfNecessary(rule.getPriority(), rule);
            }
            syncRule(rule, bigRule);
            adminService().update(bigRule);
        }
    }

    public void remove(Collection<ShortAdminRule> selected) {
        rules.removeAll(selected);
        for (ShortAdminRule rule : selected) {
            adminService().delete(rule.getId());
        }
    }

    public boolean canUp(ShortAdminRule rule) {
        return rules.indexOf(rule) > 0;
    }

    public void moveUp(ShortAdminRule rule) {
        int index = rules.indexOf(rule);
        if (index > 0) {
            swap(rule, rules.get(index - 1));
            rules.remove(index);
            rules.add(index - 1, rule);
        }
    }

    public boolean canDown(ShortAdminRule rule) {
        return rules.indexOf(rule) < rules.size() - 1;
    }

    public void moveDown(ShortAdminRule rule) {
        int index = rules.indexOf(rule);
        if (index < rules.size() - 1) {
            swap(rule, rules.get(index + 1));
            rules.remove(index);
            rules.add(index + 1, rule);
        }
    }

    protected void swap(ShortAdminRule rule, ShortAdminRule otherRule) {
        long p = otherRule.getPriority();
        otherRule.setPriority(rule.getPriority());
        rule.setPriority(p);
        adminService().swap(rule.getId(), otherRule.getId());
    }

    protected void shiftIfNecessary(long priority, ShortAdminRule keep) {
        boolean necessary = false;
        for (ShortAdminRule rule : rules) {
            if (rule.getPriority() == priority) {
                necessary = true;
                continue;
            }
        }
        if (necessary) {
            for (ShortAdminRule rule : rules) {
                if (rule.getPriority() >= priority && rule != keep) {
                    rule.setPriority(rule.getPriority() + 1);
                }
            }
            adminService().shift(priority, 1);
        }
    }

    public ShortAdminRule newRule() {
        ShortAdminRule rule = new ShortAdminRule();
        rule.setAccess(AdminGrantType.USER);
        rule.setPriority(0);
        return rule;
    }

    protected static void syncRule(ShortAdminRule shortRule, AdminRule rule) {
        rule.setPriority(shortRule.getPriority());
        rule.setUsername(shortRule.getUserName());
        rule.setRolename(shortRule.getRoleName());
        rule.setWorkspace(shortRule.getWorkspace());
        rule.setAccess(shortRule.getAccess());
    }
}
