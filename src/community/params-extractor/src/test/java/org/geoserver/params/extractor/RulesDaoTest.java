/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class RulesDaoTest {

    @Test
    public void testParsingEmptyFile() throws Exception {
        new DoWork("rules1.xml") {
            @Override
            public void doWork(InputStream inputStream) {
                List<Rule> rules = RulesDao.getRules(inputStream);
                assertThat(rules.size(), is(0));
            }
        };
    }

    @Test
    public void testParsingEmptyRules() throws Exception {
        new DoWork("rules2.xml") {
            @Override
            public void doWork(InputStream inputStream) {
                List<Rule> rules = RulesDao.getRules(inputStream);
                assertThat(rules.size(), is(0));
            }
        };
    }

    @Test
    public void testParsingPositionRule() throws Exception {
        new DoWork("rules3.xml") {
            @Override
            public void doWork(InputStream inputStream) {
                List<Rule> rules = RulesDao.getRules(inputStream);
                assertThat(rules.size(), is(1));
                checkRule(rules.get(0), new RuleBuilder().withId("0").withPosition(3)
                        .withParameter("cql_filter").withRemove(1).withTransform("seq='$2'").build());
            }
        };
    }

    @Test
    public void testParsingMatchRule() throws Exception {
        new DoWork("rules4.xml") {
            @Override
            public void doWork(InputStream inputStream) {
                List<Rule> rules = RulesDao.getRules(inputStream);
                assertThat(rules.size(), is(1));
                checkRule(rules.get(0), new RuleBuilder().withId("0").withMatch("^.*?(/([^/]+?))/[^/]+$")
                        .withParameter("cql_filter").withRemove(1).withTransform("seq='$2'").build());
            }
        };
    }

    @Test
    public void testParsingMultipleRules() throws Exception {
        new DoWork("rules5.xml") {
            @Override
            public void doWork(InputStream inputStream) {
                List<Rule> rules = RulesDao.getRules(inputStream);
                assertThat(rules.size(), is(3));
                checkRule(rules.get(0), new RuleBuilder().withId("0").withPosition(3)
                        .withParameter("cql_filter").withRemove(1).withTransform("seq='$2'").build());
                checkRule(rules.get(1), new RuleBuilder().withId("1").withMatch("^.*?(/([^/]+?))/[^/]+$")
                        .withParameter("cql_filter").withRemove(2).withTransform("seq='$2'").build());
                checkRule(rules.get(2), new RuleBuilder().withId("2").withPosition(4)
                        .withParameter("cql_filter").withRemove(null).withTransform("seq='$2'").build());
            }
        };
    }

    private void checkRule(Rule ruleA, Rule ruleB) {
        assertThat(ruleA, notNullValue());
        assertThat(ruleB, notNullValue());
        checkValue(ruleA.getId(), ruleB.getId());
        checkValue(ruleA.getPosition(), ruleB.getPosition());
        checkValue(ruleA.getMatch(), ruleB.getMatch());
        checkValue(ruleA.getParameter(), ruleB.getParameter());
        checkValue(ruleA.getTransform(), ruleB.getTransform());
        checkValue(ruleA.getRemove(), ruleB.getRemove());
        checkValue(ruleA.getCombine(), ruleB.getCombine());
    }

    private <T> void checkValue(T valueA, T valueB) {
        if (valueA == null) {
            assertThat(valueB, nullValue());
        } else {
            assertThat(valueB, notNullValue());
            assertThat(valueA, is(valueB));
        }
    }

    private static abstract class DoWork {

        public DoWork(String resourcePath) {
            URL resource = RulesDaoTest.class.getClassLoader().getResource(resourcePath);
            assertThat(resource, notNullValue());
            File file = new File(resource.getFile());
            assertThat(file.exists(), is(true));
            try (InputStream inputStream = new FileInputStream(file)) {
                if (inputStream.available() == 0) {
                    return;
                }
                doWork(inputStream);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        public abstract void doWork(InputStream inputStream);
    }
}
