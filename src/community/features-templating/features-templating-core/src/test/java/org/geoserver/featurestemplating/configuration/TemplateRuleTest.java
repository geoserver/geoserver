package org.geoserver.featurestemplating.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.KvpMap;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TemplateRuleTest {

    @BeforeClass
    public static void setDispatcherRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setScheme("http");
        req.setServerPort(8080);
        req.setContextPath("/geoserver");
        req.setPathInfo("/wfs");
        req.addHeader("testHeader", "testHeaderValue");
        req.addParameter("testParameter", "testParameterValue");
        Request request = new Request();
        request.setOutputFormat("application/json");
        request.setHttpRequest(req);
        request.setRawKvp(new KvpMap<>());
        request.getRawKvp().put("TESTPARAMETER", "testParameterValue");
        Dispatcher.REQUEST.set(request);
    }

    @Test
    public void testTemplateRule() {
        TemplateRule rule = new TemplateRule();
        rule.setTemplateIdentifier("123456789");
        rule.setOutputFormat(SupportedFormat.GEOJSON);
        rule.setTemplateName("templateName");
        rule.setRuleId("1");
        assertTrue(rule.applyRule(Dispatcher.REQUEST.get()));

        rule.setOutputFormat(SupportedFormat.GML);

        assertFalse(rule.applyRule(Dispatcher.REQUEST.get()));
    }

    @Test
    public void testTemplateRules() {
        TemplateRule rule = new TemplateRule();
        rule.setTemplateIdentifier("123456789");
        rule.setOutputFormat(SupportedFormat.GEOJSON);
        rule.setTemplateName("templateName");
        rule.setRuleId("1");
        rule.setPriority(1);
        TemplateRule rule2 = new TemplateRule();
        rule2.setTemplateIdentifier("23456789");
        rule2.setOutputFormat(SupportedFormat.GEOJSON);
        rule2.setTemplateName("templateName2");
        rule2.setRuleId("2");
        rule2.setCqlFilter("requestParam('testParameter')='testParameterValue'");
        rule2.setPriority(0);
        TemplateRule rule3 = new TemplateRule();
        rule3.setTemplateIdentifier("3456789");
        rule3.setOutputFormat(SupportedFormat.GEOJSON);
        rule3.setTemplateName("templateName3");
        rule3.setRuleId("3");
        rule3.setCqlFilter("requestParam('anotherTestParameter')=true");
        rule3.setPriority(2);
        // test selection and sorting
        List<TemplateRule> rules =
                Arrays.asList(rule, rule2, rule3).stream()
                        .filter(r -> r.applyRule(Dispatcher.REQUEST.get()))
                        .sorted(new TemplateRule.TemplateRuleComparator())
                        .collect(Collectors.toList());
        assertEquals(2, rules.size());
        assertEquals(rule2, rules.get(0));
        assertEquals(rule, rules.get(1));
    }
}
