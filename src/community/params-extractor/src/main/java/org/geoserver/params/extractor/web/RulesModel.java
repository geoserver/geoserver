/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.web;

import org.geoserver.params.extractor.*;
import org.geoserver.web.wicket.GeoServerDataProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RulesModel extends GeoServerDataProvider<RuleModel> {

    public static final Property<RuleModel> EDIT_BUTTON = new PropertyPlaceholder<>("Edit");
    public static final Property<RuleModel> ACTIVATE_BUTTON = new PropertyPlaceholder<>("Active");

    private final static List<Property<RuleModel>> PROPERTIES = Arrays.asList(
            new BeanProperty<RuleModel>("Position", "position"),
            new BeanProperty<RuleModel>("Match", "match"),
            new BeanProperty<RuleModel>("Activation", "activation"),
            new BeanProperty<RuleModel>("Parameter", "parameter"),
            new BeanProperty<RuleModel>("Transform", "transform"),
            new BeanProperty<RuleModel>("Remove", "remove"),
            new BeanProperty<RuleModel>("Combine", "combine"),
            new BeanProperty<RuleModel>("Echo", "echo"),
            ACTIVATE_BUTTON,
            EDIT_BUTTON);

    @Override
    protected List<Property<RuleModel>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected List<RuleModel> getItems() {
        return getRulesModels();
    }

    public static List<RuleModel> getRulesModels() {
        List<RuleModel> ruleModels = new ArrayList<>();
        for (Rule rule : RulesDao.getRules()) {
            ruleModels.add(new RuleModel(rule));
        }
        for (EchoParameter echoParameter : EchoParametersDao.getEchoParameters()) {
            mergedForwardParameter(ruleModels, echoParameter);
        }
        return ruleModels;
    }

    public static void saveOrUpdate(RuleModel ruleModel) {
        if (!ruleModel.isEchoOnly()) {
            RulesDao.saveOrUpdateRule(ruleModel.toRule());
            if (!ruleModel.getEcho()) {
                EchoParametersDao.deleteEchoParameters(ruleModel.getId());
            }
        }
        if (ruleModel.getEcho() || ruleModel.isEchoOnly()) {
            EchoParametersDao.saveOrUpdateEchoParameter(ruleModel.toEchoParameter());
        }
    }

    public static void delete(String... rulesIds) {
        RulesDao.deleteRules(rulesIds);
        EchoParametersDao.deleteEchoParameters(rulesIds);
    }

    private static void mergedForwardParameter(List<RuleModel> ruleModels, EchoParameter echoParameter) {
        for (RuleModel ruleModel : ruleModels) {
            if (ruleModel.getId().equals(echoParameter.getId())) {
                Utils.checkCondition(echoParameter.getParameter().equals(ruleModel.getParameter()),
                        "Rule and echo parameter with id '%s' should have the same parameter.", ruleModel.getId());
                Utils.checkCondition(echoParameter.getActivated() == ruleModel.getActivated(),
                        "Rule and echo parameter with id '%s' should both be deactivated or activated.", ruleModel.getId());
                ruleModel.setEcho(true);
                return;
            }
        }
        ruleModels.add(new RuleModel(echoParameter));
    }
}
