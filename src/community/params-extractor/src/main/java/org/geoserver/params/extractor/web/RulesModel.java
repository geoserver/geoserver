/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.web;

import org.geoserver.params.extractor.Rule;
import org.geoserver.params.extractor.RulesDao;
import org.geoserver.web.wicket.GeoServerDataProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RulesModel extends GeoServerDataProvider<RuleModel> {

    public static final Property<RuleModel> BUTTONS = new PropertyPlaceholder<>("Edit");

    private final static List<Property<RuleModel>> PROPERTIES = Arrays.asList(
            new BeanProperty<RuleModel>("Position", "position"),
            new BeanProperty<RuleModel>("Match", "match"),
            new BeanProperty<RuleModel>("Activation", "activation"),
            new BeanProperty<RuleModel>("Parameter", "parameter"),
            new BeanProperty<RuleModel>("Transform", "transform"),
            new BeanProperty<RuleModel>("Remove", "remove"),
            new BeanProperty<RuleModel>("Combine", "combine"),
            BUTTONS);

    @Override
    protected List<Property<RuleModel>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected List<RuleModel> getItems() {
        List<RuleModel> ruleModels = new ArrayList<>();
        for (Rule rule : RulesDao.getRules()) {
            ruleModels.add(new RuleModel(rule));
        }
        return ruleModels;
    }

    static void saveOrUpdate(RuleModel ruleModel) {
        RulesDao.saveOrUpdateRule(ruleModel.toRule());
    }

    static void deleteRules(String... rulesIds) {
        RulesDao.deleteRules(rulesIds);
    }
}
