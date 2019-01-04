/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.List;
import org.geoserver.params.extractor.web.RuleModel;
import org.geoserver.params.extractor.web.RulesModel;
import org.junit.Test;

public final class RulesModelTest extends TestSupport {

    @Test
    public void testCrudRuleModel() throws Exception {
        // create rules and echo parameters to be used (the rules have all the same)
        Rule ruleA =
                new RuleBuilder()
                        .withId("0")
                        .withActivated(true)
                        .withPosition(3)
                        .withParameter("cql_filter")
                        .withTransform("CFCC='$2'")
                        .build();
        Rule ruleB =
                new RuleBuilder()
                        .withId("0")
                        .withActivated(false)
                        .withPosition(2)
                        .withParameter("cql_filter")
                        .withTransform("CFCC='$2'")
                        .build();
        EchoParameter echoParameterA =
                new EchoParameterBuilder()
                        .withId("0")
                        .withParameter("cql_filter")
                        .withActivated(false)
                        .build();
        EchoParameter echoParameterB =
                new EchoParameterBuilder()
                        .withId("0")
                        .withParameter("cql_filter")
                        .withActivated(false)
                        .build();
        // save rule A
        RuleModel ruleModelA = new RuleModel(ruleA);
        checkRule(ruleA, ruleModelA.toRule());
        RulesModel.saveOrUpdate(ruleModelA);
        List<RuleModel> rulesModels = RulesModel.getRulesModels();
        assertThat(rulesModels.size(), is(1));
        checkRule(ruleA, rulesModels.get(0).toRule());
        List<Rule> rules = RulesDao.getRules();
        assertThat(rules.size(), is(1));
        checkRule(ruleA, rules.get(0));
        // update rule A with rule B, an echo parameter should be produced
        RuleModel ruleModelB = new RuleModel(ruleB);
        ruleModelB.setEcho(true);
        checkRule(ruleB, ruleModelB.toRule());
        checkEchoParameter(echoParameterA, ruleModelB.toEchoParameter());
        RulesModel.saveOrUpdate(ruleModelB);
        rulesModels = RulesModel.getRulesModels();
        assertThat(rulesModels.size(), is(1));
        checkRule(ruleB, rulesModels.get(0).toRule());
        checkEchoParameter(echoParameterA, rulesModels.get(0).toEchoParameter());
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(1));
        checkRule(ruleB, rules.get(0));
        List<EchoParameter> echoParameters = EchoParametersDao.getEchoParameters();
        assertThat(echoParameters.size(), is(1));
        checkEchoParameter(echoParameterA, echoParameters.get(0));
        // updating the rule to make the parameter no echoed, the echo parameter should be removed
        ruleModelB.setEcho(false);
        RulesModel.saveOrUpdate(ruleModelB);
        rulesModels = RulesModel.getRulesModels();
        assertThat(rulesModels.size(), is(1));
        checkRule(ruleB, rulesModels.get(0).toRule());
        assertThat(rulesModels.get(0).getEcho(), is(false));
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(1));
        checkRule(ruleB, rules.get(0));
        echoParameters = EchoParametersDao.getEchoParameters();
        assertThat(echoParameters.size(), is(0));
        // creating echo parameter B, since the ids are the same the rule should contain an echo
        // parameter
        EchoParametersDao.saveOrUpdateEchoParameter(echoParameterB);
        rulesModels = RulesModel.getRulesModels();
        assertThat(rulesModels.size(), is(1));
        checkRule(ruleB, rulesModels.get(0).toRule());
        checkEchoParameter(echoParameterB, rulesModels.get(0).toEchoParameter());
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(1));
        echoParameters = EchoParametersDao.getEchoParameters();
        assertThat(echoParameters.size(), is(1));
        // deleting rule everything should be deleted in cascade
        RulesModel.delete("0");
        rulesModels = RulesModel.getRulesModels();
        assertThat(rulesModels.size(), is(0));
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(0));
        echoParameters = EchoParametersDao.getEchoParameters();
        assertThat(echoParameters.size(), is(0));
    }
}
