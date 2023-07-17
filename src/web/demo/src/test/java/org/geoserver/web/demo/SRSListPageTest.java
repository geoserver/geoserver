/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.HeaderContribution;
import org.junit.Rule;
import org.junit.Test;

public class SRSListPageTest extends GeoServerWicketTestSupport {

    @Rule
    public GeoServerExtensionsHelper.ExtensionsHelperRule extensions =
            new GeoServerExtensionsHelper.ExtensionsHelperRule();

    @Test
    public void testBasicPage() throws Exception {
        tester.startPage(SRSListPage.class);
        tester.assertLabel(
                "srsListPanel:table:listContainer:items:1:itemProperties:0:component:link:label",
                "CRS:27");
        tester.clickLink(
                "srsListPanel:table:listContainer:items:1:itemProperties:0:component:link");
        tester.assertRenderedPage(SRSDescriptionPage.class);
    }

    @Test
    public void testHeaderContribution() throws Exception {
        HeaderContribution testHeaderContribution = new HeaderContribution();
        testHeaderContribution.setCSSFilename("testHeaderContribution.css");
        testHeaderContribution.setScope(getClass());
        extensions.singleton(
                "testHeaderContribution", testHeaderContribution, HeaderContribution.class);

        tester.startPage(SRSListPage.class);
        tester.clickLink(
                "srsListPanel:table:listContainer:items:1:itemProperties:0:component:link");
        tester.assertRenderedPage(SRSDescriptionPage.class);
        assertTrue(tester.getLastResponse().getDocument().contains("testHeaderContribution.css"));
    }

    @Test
    public void testIAUCodes() throws Exception {
        tester.startPage(SRSListPage.class);

        FormTester ft = tester.newFormTester("srsListPanel:table:filterForm");
        ft.setValue("filter", "IAU:30115");
        ft.submit("submit");

        // find and click the link with the 30115 code
        AtomicBoolean found = new AtomicBoolean(false);
        tester.getLastRenderedPage()
                .visitChildren(
                        AjaxLink.class,
                        (link, visit) -> {
                            if ("IAU:30115".equals(link.getDefaultModelObjectAsString())) {
                                visit.stop();
                                found.set(true);
                            }
                        });

        // the component has been found
        assertTrue(found.get());
    }
}
