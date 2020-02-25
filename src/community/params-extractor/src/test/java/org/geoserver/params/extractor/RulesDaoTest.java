/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.List;
import org.junit.Test;

public final class RulesDaoTest extends TestSupport {

    @Test
    public void testParsingEmptyFile() throws Exception {
        doWork(
                "data/rules1.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(inputStream);
                    assertThat(rules.size(), is(0));
                });
    }

    @Test
    public void testParsingEmptyRules() throws Exception {
        doWork(
                "data/rules2.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(inputStream);
                    assertThat(rules.size(), is(0));
                });
    }

    @Test
    public void testParsingPositionRule() throws Exception {
        doWork(
                "data/rules3.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(inputStream);
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
                });
    }

    @Test
    public void testParsingMatchRule() throws Exception {
        doWork(
                "data/rules4.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(inputStream);
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
                });
    }

    @Test
    public void testParsingMultipleRules() throws Exception {
        doWork(
                "data/rules5.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(inputStream);
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
                });
    }

    @Test
    public void testParsingCombineRepeatRule() throws Exception {
        doWork(
                "data/rules6.xml",
                (InputStream inputStream) -> {
                    List<Rule> rules = RulesDao.getRules(inputStream);
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
                });
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
        List<Rule> rules = RulesDao.getRules();
        assertThat(rules.size(), is(0));
        // we save rules A and B
        RulesDao.saveOrUpdateRule(ruleA);
        RulesDao.saveOrUpdateRule(ruleB);
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(2));
        checkRule(ruleA, findRule("0", rules));
        checkRule(ruleB, findRule("1", rules));
        // we update rule B using rule C
        RulesDao.saveOrUpdateRule(ruleC);
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(2));
        checkRule(ruleA, findRule("0", rules));
        checkRule(ruleC, findRule("1", rules));
        // we delete rule A
        RulesDao.deleteRules("0");
        rules = RulesDao.getRules();
        assertThat(rules.size(), is(1));
        checkRule(ruleC, findRule("1", rules));
    }
}
