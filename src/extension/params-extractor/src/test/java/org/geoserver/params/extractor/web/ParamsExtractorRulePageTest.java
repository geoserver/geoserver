/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.web;

import java.util.Optional;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class ParamsExtractorRulePageTest extends GeoServerWicketTestSupport {

    private static final String PARAMETER_NAME = "foobar";
    private static final String BASIC_TRANSFORM = "CQL_FILTER=name%3D{PARAMETER}";
    private static final String COMPLEX_TRANSFORM = "CQL_FILTER=value%3D$1";

    @Override
    @Before
    public void login() {
        super.login();
    }

    @Test
    public void testNew() {
        tester.startPage(new ParamsExtractorRulePage(Optional.empty()));
        tester.assertRenderedPage(ParamsExtractorRulePage.class);

        // as opened
        tester.assertComponent("form:tabs:panel", ParamsExtractorRulePage.EchoParameterPanel.class);

        // switch to second tab
        tester.clickLink("form:tabs:tabs-container:tabs:1:link");
        tester.assertComponent("form:tabs:panel", ParamsExtractorRulePage.SimpleRulePanel.class);

        // and third tab
        tester.clickLink("form:tabs:tabs-container:tabs:2:link");
        tester.assertComponent("form:tabs:panel", ParamsExtractorRulePage.ComplexRulePanel.class);
    }

    @Test
    public void testForwardOnly() {
        RuleModel rule = new RuleModel(true);
        rule.setParameter(PARAMETER_NAME);
        tester.startPage(new ParamsExtractorRulePage(Optional.of(rule)));
        tester.assertRenderedPage(ParamsExtractorRulePage.class);

        tester.assertComponent("form:tabs:panel", ParamsExtractorRulePage.EchoParameterPanel.class);
        tester.assertModelValue("form:tabs:panel:parameter", PARAMETER_NAME);
    }

    @Test
    public void testBasicRule() {
        RuleModel rule = new RuleModel(false);
        rule.setParameter(PARAMETER_NAME);
        rule.setPosition(1);
        rule.setTransform(BASIC_TRANSFORM);
        tester.startPage(new ParamsExtractorRulePage(Optional.of(rule)));
        tester.assertRenderedPage(ParamsExtractorRulePage.class);

        tester.assertComponent("form:tabs:panel", ParamsExtractorRulePage.SimpleRulePanel.class);
        tester.assertModelValue("form:tabs:panel:position", 1);
        tester.assertModelValue("form:tabs:panel:parameter", PARAMETER_NAME);
        tester.assertModelValue("form:tabs:panel:transform", BASIC_TRANSFORM);
        tester.assertModelValue("form:tabs:panel:echo", false);
    }

    @Test
    public void testAdvancedRule() {
        RuleModel rule = new RuleModel(false);
        rule.setParameter(PARAMETER_NAME);
        rule.setMatch("[\\d]+");
        rule.setParameter(PARAMETER_NAME);
        rule.setTransform(COMPLEX_TRANSFORM);
        rule.setRepeat(false);
        tester.startPage(new ParamsExtractorRulePage(Optional.of(rule)));
        tester.assertRenderedPage(ParamsExtractorRulePage.class);

        tester.assertComponent("form:tabs:panel", ParamsExtractorRulePage.ComplexRulePanel.class);
        tester.assertModelValue("form:tabs:panel:match", "[\\d]+");
        tester.assertModelValue("form:tabs:panel:parameter", PARAMETER_NAME);
        tester.assertModelValue("form:tabs:panel:transform", COMPLEX_TRANSFORM);
        tester.assertModelValue("form:tabs:panel:repeat", false);
        tester.assertModelValue("form:tabs:panel:echo", false);
    }
}
