/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext.web;

import java.util.Optional;
import org.geoserver.proxybase.ext.config.ProxyBaseExtensionRule;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

/** Test for the {@link ProxyBaseExtensionRulePage}. */
public class ProxyBaseExtensionRulePageTest extends GeoServerWicketTestSupport {

    @Override
    @Before
    public void login() {
        super.login();
    }

    @Test
    public void testNew() {
        tester.startPage(new ProxyBaseExtensionRulePage(Optional.empty()));
        tester.assertRenderedPage(ProxyBaseExtensionRulePage.class);

        // as opened
        tester.assertComponent("form:tabs:panel", ProxyBaseExtensionRulePage.SimpleRulePanel.class);
    }

    @Test
    public void testBasicRule() {
        ProxyBaseExtensionRule rule = new ProxyBaseExtensionRule();
        rule.setMatcher(".*");
        rule.setPosition(1);
        rule.setTransformer("https://example.com/{PARAMETER}");
        tester.startPage(new ProxyBaseExtensionRulePage(Optional.of(rule)));
        tester.assertRenderedPage(ProxyBaseExtensionRulePage.class);

        tester.assertComponent("form:tabs:panel", ProxyBaseExtensionRulePage.SimpleRulePanel.class);
        tester.assertModelValue("form:tabs:panel:position", 1);
        tester.assertModelValue("form:tabs:panel:matcher", ".*");
        tester.assertModelValue("form:tabs:panel:transformer", "https://example.com/{PARAMETER}");
    }
}
