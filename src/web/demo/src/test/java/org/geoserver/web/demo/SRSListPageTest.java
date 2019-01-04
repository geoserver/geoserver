/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static junit.framework.TestCase.assertTrue;

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
                "2000");
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
}
