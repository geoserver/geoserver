/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.csp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.csp.CSPConfiguration;
import org.geoserver.security.csp.CSPDefaultConfiguration;
import org.geoserver.security.csp.CSPHeaderDAO;
import org.geoserver.security.csp.CSPPolicy;
import org.geoserver.security.csp.CSPRule;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.Icon;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CSPConfigurationPageTest extends GeoServerWicketTestSupport {

    private static final CSPConfiguration DEFAULT_CONFIG = CSPDefaultConfiguration.newInstance();

    private CSPConfiguration expectedConfig = null;

    @BeforeClass
    public static void setLanguage() {
        // for error message tests
        Locale.setDefault(Locale.ENGLISH);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) {
        // no test data needed, faster execution
    }

    @Before
    public void startConfigurationPage() throws Exception {
        getDao().setConfig(defaultConfig());
        login();
        tester.startPage(CSPConfigurationPage.class);
        this.expectedConfig = defaultConfig();
        assertConfigPage(this.expectedConfig, 0);
    }

    @Test
    public void testAddPolicyMissingName() throws Exception {
        tester.clickLink("form:policies:add");
        assertPolicyPage(new CSPPolicy(), 0);
        tester.newFormTester("form").submit("save");
        tester.assertErrorMessages("Field 'Name' is required.");
    }

    @Test
    public void testAddPolicyDuplicateName() throws Exception {
        String name = this.expectedConfig.getPolicies().get(0).getName();
        tester.clickLink("form:policies:add");
        assertPolicyPage(new CSPPolicy(), 0);
        tester.newFormTester("form").setValue("name", name).submit("save");
        tester.assertErrorMessages("Another policy with the same name already exists: '" + name + "'");
    }

    @Test
    public void testAddRuleMissingName() throws Exception {
        tester.clickLink("form:policies:add");
        assertPolicyPage(new CSPPolicy(), 0);
        tester.clickLink("form:rules:add");
        assertRulePage(new CSPRule());
        tester.newFormTester("form").submit("save");
        tester.assertErrorMessages("Field 'Name' is required.");
    }

    @Test
    public void testAddRuleDuplicateName() throws Exception {
        CSPPolicy expectedPolicy = this.expectedConfig.getPolicies().get(0);
        String name = expectedPolicy.getRules().get(0).getName();
        tester.clickLink("form:policies:table:listContainer:items:1:itemProperties:3:component:link");
        assertPolicyPage(expectedPolicy, 0);
        tester.clickLink("form:rules:add");
        assertRulePage(new CSPRule());
        tester.newFormTester("form").setValue("name", name).submit("save");
        tester.assertErrorMessages("Another rule with the same name already exists: '" + name + "'");
    }

    @Test
    public void testEditConfigFieldsAndSave() throws Exception {
        // verify that config changes are saved
        this.expectedConfig.setEnabled(false);
        this.expectedConfig.setInjectProxyBase(true);
        this.expectedConfig.setRemoteResources("http://geoserver.org");
        this.expectedConfig.setFrameAncestors("'self' http://geoserver.org");
        tester.newFormTester("form")
                .setValue("enabled", this.expectedConfig.isEnabled())
                .setValue("injectProxyBase", this.expectedConfig.isInjectProxyBase())
                .setValue("remoteResources", this.expectedConfig.getRemoteResources())
                .setValue("frameAncestors", this.expectedConfig.getFrameAncestors())
                .submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testEditConfigFieldsAndCancel() throws Exception {
        // verify that config changes are discarded
        tester.newFormTester("form")
                .setValue("enabled", false)
                .setValue("injectProxyBase", true)
                .setValue("remoteResources", "http://geoserver.org")
                .setValue("frameAncestors", "'self' http://geoserver.org")
                .submit("cancel");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testEditPolicyFieldsAndSave() throws Exception {
        // verify that policy changes are saved
        CSPPolicy expectedPolicy = this.expectedConfig.getPolicies().get(0);
        tester.clickLink("form:policies:table:listContainer:items:1:itemProperties:3:component:link");
        assertPolicyPage(expectedPolicy, 0);
        expectedPolicy.setDescription("foo");
        expectedPolicy.setEnabled(false);
        tester.newFormTester("form")
                .setValue("description", expectedPolicy.getDescription())
                .setValue("enabled", expectedPolicy.isEnabled())
                .submit("save");
        assertConfigPage(this.expectedConfig, this.expectedConfig.getPolicies().size());
        tester.newFormTester("form").submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testEditPolicyFieldsAndCancel() throws Exception {
        // verify that policy changes are discarded
        CSPPolicy expectedPolicy = expectedConfig.getPolicies().get(0);
        tester.clickLink("form:policies:table:listContainer:items:1:itemProperties:3:component:link");
        assertPolicyPage(expectedPolicy, 0);
        tester.newFormTester("form")
                .setValue("description", "foo")
                .setValue("enabled", false)
                .submit("cancel");
        assertConfigPage(this.expectedConfig, this.expectedConfig.getPolicies().size());
        tester.newFormTester("form").submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testEditRuleFieldsAndSave() throws Exception {
        // verify that rule changes are saved
        CSPPolicy expectedPolicy = this.expectedConfig.getPolicies().get(0);
        CSPRule expectedRule = expectedPolicy.getRules().get(0);
        tester.clickLink("form:policies:table:listContainer:items:1:itemProperties:3:component:link");
        assertPolicyPage(expectedPolicy, 0);
        tester.clickLink("form:rules:table:listContainer:items:1:itemProperties:3:component:link");
        assertRulePage(expectedRule);
        expectedRule.setDescription("foo");
        expectedRule.setEnabled(false);
        expectedRule.setFilter("PATH(^.*$)");
        expectedRule.setDirectives("NONE");
        tester.newFormTester("form")
                .setValue("description", expectedRule.getDescription())
                .setValue("enabled", expectedRule.isEnabled())
                .setValue("filter", expectedRule.getFilter())
                .setValue("directives", expectedRule.getDirectives())
                .submit("save");
        assertPolicyPage(expectedPolicy, expectedPolicy.getRules().size());
        tester.newFormTester("form").submit("save");
        assertConfigPage(this.expectedConfig, this.expectedConfig.getPolicies().size());
        tester.newFormTester("form").submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testEditRuleFieldsAndCancel() throws Exception {
        // verify that rule changes are discarded
        CSPPolicy expectedPolicy = this.expectedConfig.getPolicies().get(0);
        CSPRule expectedRule = expectedPolicy.getRules().get(0);
        tester.clickLink("form:policies:table:listContainer:items:1:itemProperties:3:component:link");
        assertPolicyPage(expectedPolicy, 0);
        tester.clickLink("form:rules:table:listContainer:items:1:itemProperties:3:component:link");
        assertRulePage(expectedRule);
        tester.newFormTester("form")
                .setValue("description", "foo")
                .setValue("enabled", false)
                .setValue("filter", "")
                .setValue("directives", "NONE")
                .submit("cancel");
        assertPolicyPage(expectedPolicy, expectedPolicy.getRules().size());
        tester.newFormTester("form").submit("save");
        assertConfigPage(this.expectedConfig, this.expectedConfig.getPolicies().size());
        tester.newFormTester("form").submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testMovePolicyDown() throws Exception {
        // move the first policy down
        tester.clickLink("form:policies:table:listContainer:items:1:itemProperties:1:component:down:link");
        this.expectedConfig.getPolicies().add(this.expectedConfig.getPolicies().remove(0));
        assertConfigPage(this.expectedConfig, this.expectedConfig.getPolicies().size());
        tester.newFormTester("form").submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testMovePolicyUp() throws Exception {
        // move the second policy up
        tester.clickLink("form:policies:table:listContainer:items:2:itemProperties:1:component:up:link");
        this.expectedConfig.getPolicies().add(this.expectedConfig.getPolicies().remove(0));
        assertConfigPage(this.expectedConfig, this.expectedConfig.getPolicies().size());
        tester.newFormTester("form").submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testRemovePolicy() throws Exception {
        // remove the first policy
        tester.clickLink("form:policies:table:listContainer:items:1:itemProperties:5:component:link");
        this.expectedConfig.getPolicies().remove(0);
        assertConfigPage(this.expectedConfig, this.expectedConfig.getPolicies().size() + 1);
        tester.newFormTester("form").submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testMoveRuleDown() throws Exception {
        // move the first rule down
        CSPPolicy expectedPolicy = this.expectedConfig.getPolicies().get(0);
        tester.clickLink("form:policies:table:listContainer:items:1:itemProperties:3:component:link");
        assertPolicyPage(expectedPolicy, 0);
        tester.clickLink("form:rules:table:listContainer:items:1:itemProperties:1:component:down:link");
        expectedPolicy.getRules().add(0, expectedPolicy.getRules().remove(1));
        assertPolicyPage(expectedPolicy, expectedPolicy.getRules().size());
        tester.newFormTester("form").submit("save");
        assertConfigPage(this.expectedConfig, this.expectedConfig.getPolicies().size());
        tester.newFormTester("form").submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testMoveRuleUp() throws Exception {
        // move the second rule up
        CSPPolicy expectedPolicy = this.expectedConfig.getPolicies().get(0);
        tester.clickLink("form:policies:table:listContainer:items:1:itemProperties:3:component:link");
        assertPolicyPage(expectedPolicy, 0);
        tester.clickLink("form:rules:table:listContainer:items:2:itemProperties:1:component:up:link");
        expectedPolicy.getRules().add(0, expectedPolicy.getRules().remove(1));
        assertPolicyPage(expectedPolicy, expectedPolicy.getRules().size());
        tester.newFormTester("form").submit("save");
        assertConfigPage(this.expectedConfig, this.expectedConfig.getPolicies().size());
        tester.newFormTester("form").submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testRemoveRule() throws Exception {
        // remove the first rule
        CSPPolicy expectedPolicy = this.expectedConfig.getPolicies().get(0);
        tester.clickLink("form:policies:table:listContainer:items:1:itemProperties:3:component:link");
        assertPolicyPage(expectedPolicy, 0);
        tester.clickLink("form:rules:table:listContainer:items:1:itemProperties:7:component:link");
        expectedPolicy.getRules().remove(0);
        assertPolicyPage(expectedPolicy, expectedPolicy.getRules().size() + 1);
        tester.newFormTester("form").submit("save");
        assertConfigPage(this.expectedConfig, this.expectedConfig.getPolicies().size());
        tester.newFormTester("form").submit("save");
        tester.assertNoErrorMessage();
        assertConfig(this.expectedConfig);
    }

    @Test
    public void testTestEmptyURL() throws Exception {
        tester.newFormTester("form").submit("testLink");
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:testResult", "Enter URL");
    }

    @Test
    public void testTestBlankURL() throws Exception {
        tester.newFormTester("form").setValue("testUrl", "     ").submit("testLink");
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:testResult", "Enter URL");
    }

    @Test
    public void testTestInvalidURL() throws Exception {
        tester.newFormTester("form").setValue("testUrl", "~!@#$").submit("testLink");
        List<Serializable> messages = tester.getMessages(FeedbackMessage.ERROR);
        assertEquals(1, messages.size());
        assertThat(messages.get(0), instanceOf(MalformedURLException.class));
        tester.assertModelValue("form:testResult", "ERROR");
    }

    @Test
    public void testTestValidURL() throws Exception {
        tester.newFormTester("form")
                .setValue("testUrl", "http://localhost/geoserver/wms&request=GetCapabilities")
                .submit("testLink");
        tester.assertNoErrorMessage();
        tester.assertModelValue(
                "form:testResult",
                "base-uri 'self'; form-action 'self'; default-src 'none'; child-src 'self'; "
                        + "connect-src 'self'; font-src 'self'; img-src 'self' data:; "
                        + "style-src 'self' 'unsafe-inline'; script-src 'self';, "
                        + "frame-ancestors 'self';");
    }

    @Test
    public void testTestDisabled() throws Exception {
        tester.newFormTester("form")
                .setValue("enabled", false)
                .setValue("testUrl", "http://localhost/geoserver/wms&request=GetCapabilities")
                .submit("testLink");
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:testResult", "NONE");
    }

    private static void assertConfigPage(CSPConfiguration config, int offset) {
        assertConfigPage(
                config.isEnabled(),
                config.isReportOnly(),
                config.isAllowOverride(),
                config.isInjectProxyBase(),
                config.getRemoteResources(),
                config.getFrameAncestors(),
                config.getPolicies(),
                offset);
    }

    private static void assertConfigPage(
            boolean enabled,
            boolean reportOnly,
            boolean allowOverride,
            boolean injectProxyBase,
            String remoteResources,
            String frameAncestors,
            List<CSPPolicy> policies,
            int offset) {
        tester.assertRenderedPage(CSPConfigurationPage.class);
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:enabled", enabled);
        tester.assertModelValue("form:reportOnly", reportOnly);
        tester.assertModelValue("form:allowOverride", allowOverride);
        tester.assertModelValue("form:injectProxyBase", injectProxyBase);
        tester.assertModelValue("form:remoteResources", remoteResources);
        tester.assertModelValue("form:frameAncestors", frameAncestors);
        tester.assertComponent("form:policies", CSPPolicyPanel.class);
        tester.assertComponent("form:policies:add", AjaxLink.class);
        Component component = tester.getComponentFromLastRenderedPage("form:policies:table:listContainer:items");
        assertThat(component, instanceOf(MarkupContainer.class));
        assertEquals(policies.size(), ((MarkupContainer) component).size());
        for (int i = 1; i <= policies.size(); i++) {
            CSPPolicy policy = policies.get(i - 1);
            // new list items are created every time the table is rendered
            String path = "form:policies:table:listContainer:items:" + (i + offset) + ":itemProperties:";
            tester.assertLabel(path + "0:component", Integer.toString(i));
            if (policy.isEnabled()) {
                component = tester.getComponentFromLastRenderedPage(path + "2:component");
                assertThat(component, instanceOf(Icon.class));
            } else {
                tester.assertLabel(path + "2:component", "");
            }
            tester.assertLabel(path + "3:component:link:label", policy.getName());
            tester.assertModelValue(path + "4:component", policy.getDescription());
        }
        tester.assertModelValue("form:testUrl", "");
        tester.assertComponent("form:testLink", AjaxSubmitLink.class);
        tester.assertModelValue("form:testResult", "");
        tester.assertComponent("form:save", SubmitLink.class);
        tester.assertComponent("form:apply", Button.class);
        tester.assertComponent("form:cancel", Button.class);
    }

    private static void assertPolicyPage(CSPPolicy policy, int offset) {
        assertPolicyPage(policy.getName(), policy.getDescription(), policy.isEnabled(), policy.getRules(), offset);
    }

    private static void assertPolicyPage(
            String name, String description, boolean enabled, List<CSPRule> rules, int offset) {
        tester.assertRenderedPage(CSPPolicyPage.class);
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:name", name);
        if (name == null) {
            tester.assertEnabled("form:name");
        } else {
            tester.assertDisabled("form:name");
        }
        tester.assertModelValue("form:description", description);
        tester.assertModelValue("form:enabled", enabled);
        tester.assertComponent("form:rules", CSPRulePanel.class);
        tester.assertComponent("form:rules:add", AjaxLink.class);
        Component component = tester.getComponentFromLastRenderedPage("form:rules:table:listContainer:items");
        assertThat(component, instanceOf(MarkupContainer.class));
        assertEquals(rules.size(), ((MarkupContainer) component).size());
        for (int i = 1; i <= rules.size(); i++) {
            CSPRule rule = rules.get(i - 1);
            // new list items are created every time the table is rendered
            String path = "form:rules:table:listContainer:items:" + (i + offset) + ":itemProperties:";
            tester.assertLabel(path + "0:component", Integer.toString(i));
            if (rule.isEnabled()) {
                component = tester.getComponentFromLastRenderedPage(path + "2:component");
                assertThat(component, instanceOf(Icon.class));
            } else {
                tester.assertLabel(path + "2:component", "");
            }
            tester.assertLabel(path + "3:component:link:label", rule.getName());
            component = tester.getComponentFromLastRenderedPage(path + "4:component");
            assertThat(component, instanceOf(Icon.class));
            assertEquals(
                    rule.getDescription(),
                    tester.getTagById(component.getMarkupId()).getChild("img").getAttribute("title"));
            tester.assertModelValue(path + "5:component", rule.getFilter());
            tester.assertModelValue(path + "6:component", rule.getDirectives());
        }
        tester.assertComponent("form:save", SubmitLink.class);
        tester.assertComponent("form:cancel", Button.class);
    }

    private static void assertRulePage(CSPRule rule) {
        assertRulePage(rule.getName(), rule.getDescription(), rule.isEnabled(), rule.getFilter(), rule.getDirectives());
    }

    private static void assertRulePage(
            String name, String description, boolean enabled, String filter, String directives) {
        tester.assertRenderedPage(CSPRulePage.class);
        tester.assertNoErrorMessage();
        tester.assertModelValue("form:name", name);
        if (name == null) {
            tester.assertEnabled("form:name");
        } else {
            tester.assertDisabled("form:name");
        }
        tester.assertModelValue("form:description", description);
        tester.assertModelValue("form:enabled", enabled);
        tester.assertModelValue("form:filter", filter);
        tester.assertModelValue("form:directives", directives);
        tester.assertComponent("form:save", SubmitLink.class);
        tester.assertComponent("form:cancel", Button.class);
    }

    private static void assertConfig(CSPConfiguration expectedConfig) throws Exception {
        assertConfig(
                expectedConfig.isEnabled(),
                expectedConfig.isInjectProxyBase(),
                expectedConfig.getRemoteResources(),
                expectedConfig.getFrameAncestors(),
                expectedConfig.getPolicies());
    }

    private static void assertConfig(
            boolean enabled,
            boolean injectProxyBase,
            String remoteResources,
            String frameAncestors,
            List<CSPPolicy> policies)
            throws Exception {
        CSPConfiguration actualConfig = getConfig();
        assertEquals(enabled, actualConfig.isEnabled());
        assertEquals(injectProxyBase, actualConfig.isInjectProxyBase());
        assertEquals(remoteResources, actualConfig.getRemoteResources());
        assertEquals(frameAncestors, actualConfig.getFrameAncestors());
        assertEquals(policies.size(), actualConfig.getPolicies().size());
        for (int i = 0; i < policies.size(); i++) {
            assertPolicy(policies.get(i), actualConfig.getPolicies().get(i));
        }
    }

    private static void assertPolicy(CSPPolicy expectedPolicy, CSPPolicy actualPolicy) {
        assertPolicy(
                actualPolicy,
                expectedPolicy.getName(),
                expectedPolicy.getDescription(),
                expectedPolicy.isEnabled(),
                expectedPolicy.getRules());
    }

    private static void assertPolicy(
            CSPPolicy actualPolicy, String name, String description, boolean enabled, List<CSPRule> rules) {
        assertEquals(name, actualPolicy.getName());
        assertEquals(description, actualPolicy.getDescription());
        assertEquals(enabled, actualPolicy.isEnabled());
        assertEquals(rules.size(), actualPolicy.getRules().size());
        for (int i = 0; i < rules.size(); i++) {
            assertRule(rules.get(i), actualPolicy.getRules().get(i));
        }
    }

    private static void assertRule(CSPRule expectedRule, CSPRule actualRule) {
        assertRule(
                actualRule,
                expectedRule.getName(),
                expectedRule.getDescription(),
                expectedRule.isEnabled(),
                expectedRule.getFilter(),
                expectedRule.getDirectives());
    }

    private static void assertRule(
            CSPRule actualRule, String name, String description, boolean enabled, String filter, String directives) {
        assertEquals(name, actualRule.getName());
        assertEquals(description, actualRule.getDescription());
        assertEquals(enabled, actualRule.isEnabled());
        assertEquals(filter, actualRule.getFilter());
        assertEquals(directives, actualRule.getDirectives());
    }

    private static CSPConfiguration defaultConfig() {
        // get a new copy of the default configuration
        return new CSPConfiguration(DEFAULT_CONFIG);
    }

    private static CSPConfiguration getConfig() throws Exception {
        // force reading the configuration file
        CSPHeaderDAO dao = getDao();
        dao.reset();
        return dao.getConfig();
    }

    private static CSPHeaderDAO getDao() {
        return GeoServerExtensions.bean(CSPHeaderDAO.class);
    }
}
