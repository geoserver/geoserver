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
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class RulesDaoTest {

    @Test
    public void testParsingEmptyFile() throws Exception {
        doWork("rules1.xml", (InputStream inputStream) -> {
            List<Rule> rules = RulesDao.getRules(inputStream);
            assertThat(rules.size(), is(0));
        });
    }

    @Test
    public void testParsingEmptyRules() throws Exception {
        doWork("rules2.xml", (InputStream inputStream) -> {
            List<Rule> rules = RulesDao.getRules(inputStream);
            assertThat(rules.size(), is(0));
        });
    }

    @Test
    public void testParsingPositionRule() throws Exception {
        doWork("rules3.xml", (InputStream inputStream) -> {
            List<Rule> rules = RulesDao.getRules(inputStream);
            assertThat(rules.size(), is(1));
            checkRule(rules.get(0), new RuleBuilder().withId("0").withPosition(3)
                    .withParameter("cql_filter").withRemove(1).withTransform("seq='$2'").build());
        });
    }

    @Test
    public void testParsingMatchRule() throws Exception {
        doWork("rules4.xml", (InputStream inputStream) -> {
            List<Rule> rules = RulesDao.getRules(inputStream);
            assertThat(rules.size(), is(1));
            checkRule(rules.get(0), new RuleBuilder().withId("0").withMatch("^.*?(/([^/]+?))/[^/]+$")
                    .withParameter("cql_filter").withRemove(1).withTransform("seq='$2'").build());
        });
    }

    @Test
    public void testParsingMultipleRules() throws Exception {
        doWork("rules5.xml", (InputStream inputStream) -> {
            List<Rule> rules = RulesDao.getRules(inputStream);
            assertThat(rules.size(), is(3));
            checkRule(rules.get(0), new RuleBuilder().withId("0").withPosition(3)
                    .withParameter("cql_filter").withRemove(1).withTransform("seq='$2'").build());
            checkRule(rules.get(1), new RuleBuilder().withId("1").withMatch("^.*?(/([^/]+?))/[^/]+$")
                    .withParameter("cql_filter").withRemove(2).withTransform("seq='$2'").build());
            checkRule(rules.get(2), new RuleBuilder().withId("2").withPosition(4)
                    .withParameter("cql_filter").withRemove(null).withTransform("seq='$2'").build());
        });
    }

    private void checkRule(Rule ruleA, Rule ruleB) {
        assertThat(ruleA, notNullValue());
        assertThat(ruleB, notNullValue());
        checkValue(ruleA, ruleB, Rule::getId);
        checkValue(ruleA, ruleB, Rule::getPosition);
        checkValue(ruleA, ruleB, Rule::getMatch);
        checkValue(ruleA, ruleB, Rule::getParameter);
        checkValue(ruleA, ruleB, Rule::getTransform);
        checkValue(ruleA, ruleB, Rule::getRemove);
        checkValue(ruleA, ruleB, Rule::getCombine);
    }

    private <T, R> void checkValue(T ruleA, T ruleB, Function<T, R> getter) {
        R valueA = getter.apply(ruleA);
        R valueB = getter.apply(ruleB);
        if (valueA == null) {
            assertThat(valueB, nullValue());
        } else {
            assertThat(valueB, notNullValue());
            assertThat(valueA, is(valueB));
        }
    }

    private static void doWork(String resourcePath, Consumer<InputStream> consumer) throws Exception {
        URL resource = RulesDaoTest.class.getClassLoader().getResource(resourcePath);
        assertThat(resource, notNullValue());
        File file = new File(resource.getFile());
        assertThat(file.exists(), is(true));
        try (InputStream inputStream = new FileInputStream(file)) {
            if (inputStream.available() == 0) {
                return;
            }
            consumer.accept(inputStream);
        }
    }
}
