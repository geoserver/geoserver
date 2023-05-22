/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext.web;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.proxybase.ext.config.ProxyBaseExtRuleDAO;
import org.geoserver.util.IOUtils;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

/** Test for the {@link ProxyBaseExtensionConfigPage}. */
public class ProxyBaseExtensionConfigPageTest extends GeoServerWicketTestSupport {

    @Before
    public void prepareConfiguration() throws IOException {
        GeoServerDataDirectory dd = getDataDirectory();
        new File(
                        testData.getDataDirectoryRoot(),
                        ProxyBaseExtRuleDAO.PROXY_BASE_EXT_RULES_DIRECTORY)
                .mkdir();
        try (OutputStream os = dd.get(ProxyBaseExtRuleDAO.PROXY_BASE_EXT_RULES_PATH).out();
                InputStream is = getClass().getResourceAsStream("/proxy-base-ext.xml")) {
            IOUtils.copy(is, os);
        }
    }

    @Test
    public void testPage() {
        login();
        tester.startPage(ProxyBaseExtensionConfigPage.class);
        tester.assertRenderedPage(ProxyBaseExtensionConfigPage.class);
        // three rules loaded
        DataView table =
                (DataView)
                        tester.getComponentFromLastRenderedPage("rulesPanel:listContainer:items");
        assertEquals(3, table.getItemCount());
        // match of the first rule
        tester.assertModelValue(
                "rulesPanel:listContainer:items:1:itemProperties:1:component", "schemas/(.*)");
        // parameter of the second rule
        tester.assertModelValue(
                "rulesPanel:listContainer:items:2:itemProperties:2:component",
                "https://basic.example.com");
    }

    @Test
    public void testEditRule() {
        login();
        tester.startPage(ProxyBaseExtensionConfigPage.class);
        tester.assertRenderedPage(ProxyBaseExtensionConfigPage.class);

        print(tester.getLastRenderedPage(), true, true);

        // click the edit link of the first rule
        tester.clickLink("rulesPanel:listContainer:items:1:itemProperties:4:component:link");
        tester.assertRenderedPage(ProxyBaseExtensionRulePage.class);
        tester.assertComponent("form:tabs:panel", ProxyBaseExtensionRulePage.SimpleRulePanel.class);
        tester.assertModelValue("form:tabs:panel:matcher", "schemas/(.*)");
    }
}
