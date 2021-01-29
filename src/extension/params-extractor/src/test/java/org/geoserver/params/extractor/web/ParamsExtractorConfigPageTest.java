/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor.web;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.params.extractor.EchoParametersDao;
import org.geoserver.params.extractor.RulesDao;
import org.geoserver.util.IOUtils;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class ParamsExtractorConfigPageTest extends GeoServerWicketTestSupport {

    @Before
    public void prepareConfiguration() throws IOException {
        GeoServerDataDirectory dd = getDataDirectory();

        // setup sample echoes
        try (OutputStream os = dd.get(EchoParametersDao.getEchoParametersPath()).out();
                InputStream is = getClass().getResourceAsStream("/data/echoParameters3.xml")) {
            IOUtils.copy(is, os);
        }

        // setup sample rules
        try (OutputStream os = dd.get(RulesDao.getRulesPath()).out();
                InputStream is = getClass().getResourceAsStream("/data/rules4.xml")) {
            IOUtils.copy(is, os);
        }
    }

    @Test
    public void testPage() {
        login();
        tester.startPage(ParamsExtractorConfigPage.class);
        tester.assertRenderedPage(ParamsExtractorConfigPage.class);

        // two rules loaded
        DataView table =
                (DataView)
                        tester.getComponentFromLastRenderedPage("rulesPanel:listContainer:items");
        assertEquals(2, table.getItemCount());
        // match of the first rule
        tester.assertModelValue(
                "rulesPanel:listContainer:items:1:itemProperties:1:component",
                "^.*?(/([^/]+?))/[^/]+$");
        // parameter of the second rule
        tester.assertModelValue(
                "rulesPanel:listContainer:items:2:itemProperties:3:component", "CQL_FILTER");
    }

    @Test
    public void testEditComplexRule() {
        login();
        tester.startPage(ParamsExtractorConfigPage.class);
        tester.assertRenderedPage(ParamsExtractorConfigPage.class);

        // click the edit link of the first rule
        tester.clickLink("rulesPanel:listContainer:items:1:itemProperties:10:component:edit:link");
        tester.assertRenderedPage(ParamsExtractorRulePage.class);
        tester.assertComponent("form:tabs:panel", ParamsExtractorRulePage.ComplexRulePanel.class);
        tester.assertModelValue("form:tabs:panel:match", "^.*?(/([^/]+?))/[^/]+$");
    }

    @Test
    public void testEditEchoRule() {
        login();
        tester.startPage(ParamsExtractorConfigPage.class);
        tester.assertRenderedPage(ParamsExtractorConfigPage.class);

        // click the edit link of the second rule
        tester.clickLink("rulesPanel:listContainer:items:2:itemProperties:10:component:edit:link");
        tester.assertRenderedPage(ParamsExtractorRulePage.class);
        tester.assertComponent("form:tabs:panel", ParamsExtractorRulePage.EchoParameterPanel.class);
        tester.assertModelValue("form:tabs:panel:parameter", "CQL_FILTER");
    }
}
