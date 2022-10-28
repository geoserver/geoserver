/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import static org.geoserver.params.extractor.RulesDao.getRules;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.junit.Test;

public final class RulesDaoTest extends TestSupport {

    static List<Rule> getRules() {
        return RulesDao.getRules();
    }

    static List<Rule> getRules(String path) {
        return RulesDao.getRules(getResource(path));
    }

    @Test
    public void testParsingEmptyFile() {
        List<Rule> rules = getRules("data/rules1.xml");
        assertThat(rules.size(), is(0));
    }

    @Test
    public void testParsingEmptyRules() {
        List<Rule> rules = getRules("data/rules2.xml");
        assertThat(rules.size(), is(0));
    }

    @Test
    public void testParsingPositionRule() {
        List<Rule> rules = getRules("data/rules3.xml");
        assertThat(rules.size(), is(1));
        checkRule(
                rules.get(0),
                new RuleBuilder()
                        .withId("0")
                        .withPosition(3)
                        .withParameter("cql_filter")
                        .withRemove(1)
                        .withTransform("seq='$2'")
                        .build());
    }

    @Test
    public void testParsingMatchRule() {
        List<Rule> rules = getRules("data/rules4.xml");
        assertThat(rules.size(), is(1));
        checkRule(
                rules.get(0),
                new RuleBuilder()
                        .withId("0")
                        .withMatch("^.*?(/([^/]+?))/[^/]+$")
                        .withParameter("cql_filter")
                        .withRemove(1)
                        .withTransform("seq='$2'")
                        .build());
    }

    @Test
    public void testParsingMultipleRules() {
        List<Rule> rules = getRules("data/rules5.xml");
        assertThat(rules.size(), is(3));
        checkRule(
                findRule("0", rules),
                new RuleBuilder()
                        .withId("0")
                        .withPosition(3)
                        .withParameter("cql_filter")
                        .withRemove(1)
                        .withTransform("seq='$2'")
                        .build());
        checkRule(
                findRule("1", rules),
                new RuleBuilder()
                        .withId("1")
                        .withMatch("^.*?(/([^/]+?))/[^/]+$")
                        .withParameter("cql_filter")
                        .withRemove(2)
                        .withTransform("seq='$2'")
                        .build());
        checkRule(
                findRule("2", rules),
                new RuleBuilder()
                        .withId("2")
                        .withPosition(4)
                        .withParameter("cql_filter")
                        .withRemove(null)
                        .withTransform("seq='$2'")
                        .build());
    }

    @Test
    public void testParsingCombineRepeatRule() {
        List<Rule> rules = getRules("data/rules6.xml");
        assertThat(rules.size(), is(1));
        checkRule(
                rules.get(0),
                new RuleBuilder()
                        .withId("0")
                        .withMatch("^.*?(/([^/]+?))/[^/]+$")
                        .withParameter("cql_filter")
                        .withRemove(1)
                        .withTransform("seq='$2'")
                        .withCombine("$1;$2")
                        .withRepeat(true)
                        .build());
    }

    @Test
    public void testRuleCrud() {
        // create the rules to be used, rule C is an update of rule B (the id is the same)
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
                        .withId("1")
                        .withActivated(true)
                        .withMatch("^(?:/[^/]*){3}(/([^/]+)).*$")
                        .withParameter("cql_filter")
                        .withActivation("^.*$")
                        .withTransform("CFCC='$2'")
                        .withRemove(1)
                        .withCombine("$1 AND $2")
                        .build();
        Rule ruleC =
                new RuleBuilder()
                        .withId("1")
                        .withActivated(false)
                        .withMatch("^(?:/[^/]*){4}(/([^/]+)).*$")
                        .withParameter("cql_filter")
                        .withActivation("^.*$")
                        .withTransform("CFCC='$2'")
                        .withRemove(1)
                        .withCombine("$1 OR $2")
                        .build();
        // get the existing rules, this should return an empty list
        List<Rule> rules = getRules();
        assertThat(rules.size(), is(0));
        // we save rules A and B
        RulesDao.saveOrUpdateRule(ruleA);
        RulesDao.saveOrUpdateRule(ruleB);
        rules = getRules();
        assertThat(rules.size(), is(2));
        checkRule(ruleA, findRule("0", rules));
        checkRule(ruleB, findRule("1", rules));
        // we update rule B using rule C
        RulesDao.saveOrUpdateRule(ruleC);
        rules = getRules();
        assertThat(rules.size(), is(2));
        checkRule(ruleA, findRule("0", rules));
        checkRule(ruleC, findRule("1", rules));
        // we delete rule A
        RulesDao.deleteRules("0");
        rules = getRules();
        assertThat(rules.size(), is(1));
        checkRule(ruleC, findRule("1", rules));
    }
}
