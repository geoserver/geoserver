/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.web;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.params.extractor.EchoParameter;
import org.geoserver.params.extractor.EchoParametersDao;
import org.geoserver.params.extractor.RulesDao;
import org.geoserver.params.extractor.Utils;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class RulesModel extends GeoServerDataProvider<RuleModel> {

    public static final Property<RuleModel> EDIT_BUTTON = new PropertyPlaceholder<>("Edit");
    public static final Property<RuleModel> ACTIVATE_BUTTON = new PropertyPlaceholder<>("Active");

    private static final List<Property<RuleModel>> PROPERTIES =
            Arrays.asList(
                    new BeanProperty<>("Position", "position"),
                    new BeanProperty<>("Match", "match"),
                    new BeanProperty<>("Activation", "activation"),
                    new BeanProperty<>("Parameter", "parameter"),
                    new BeanProperty<>("Transform", "transform"),
                    new BeanProperty<>("Remove", "remove"),
                    new BeanProperty<>("Combine", "combine"),
                    new BeanProperty<>("Repeat", "repeat"),
                    new BeanProperty<>("Echo", "echo"),
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
        List<RuleModel> ruleModels =
                RulesDao.getRules().stream().map(RuleModel::new).collect(Collectors.toList());
        EchoParametersDao.getEchoParameters()
                .forEach(forwardParameter -> mergedForwardParameter(ruleModels, forwardParameter));
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

    private static void mergedForwardParameter(
            List<RuleModel> ruleModels, EchoParameter echoParameter) {
        for (RuleModel ruleModel : ruleModels) {
            if (ruleModel.getId().equals(echoParameter.getId())) {
                Utils.checkCondition(
                        echoParameter.getParameter().equals(ruleModel.getParameter()),
                        "Rule and echo parameter with id '%s' should have the same parameter.",
                        ruleModel.getId());
                Utils.checkCondition(
                        echoParameter.getActivated() == ruleModel.getActivated(),
                        "Rule and echo parameter with id '%s' should both be deactivated or activated.",
                        ruleModel.getId());
                ruleModel.setEcho(true);
                return;
            }
        }
        ruleModels.add(new RuleModel(echoParameter));
    }
}
