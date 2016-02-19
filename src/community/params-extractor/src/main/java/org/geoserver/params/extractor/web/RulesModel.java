/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.web;

import org.geoserver.params.extractor.RulesDao;
import org.geoserver.web.wicket.GeoServerDataProvider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RulesModel extends GeoServerDataProvider<RuleModel> {

    public static final Property<RuleModel> BUTTONS = new PropertyPlaceholder<>("Edit");

    private final static List<Property<RuleModel>> PROPERTIES = Arrays.asList(
            new BeanProperty<>("Position", "position"),
            new BeanProperty<>("Match", "match"),
            new BeanProperty<>("Activation", "activation"),
            new BeanProperty<>("Parameter", "parameter"),
            new BeanProperty<>("Transform", "transform"),
            new BeanProperty<>("Remove", "remove"),
            new BeanProperty<>("Combine", "combine"),
            BUTTONS);

    @Override
    protected List<Property<RuleModel>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected List<RuleModel> getItems() {
        return RulesDao.getRules().stream().map(RuleModel::new).collect(Collectors.toList());
    }

    static void saveOrUpdate(RuleModel ruleModel) {
        RulesDao.saveOrUpdateRule(ruleModel.toRule());
    }

    static void deleteRules(String... rulesIds) {
        RulesDao.deleteRules(rulesIds);
    }
}
