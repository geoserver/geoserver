/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import org.apache.wicket.markup.html.WebPage;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.Service;
import org.geotools.util.Version;
import org.junit.Test;

public class CapabilitiesHomePagePanelTest extends GeoServerWicketTestSupport {

    public static class TestPage extends WebPage {

        private static final long serialVersionUID = -4374237095130771859L;
        /*
         * Empy WebPage to aid in testing CapabilitiesHomePagePanel as a component of this page (the
         * accompanying CapabilitiesHomePagePanelTest$TestPage.html. Needed since
         * WicketTester.assertListView does not work for a detached component, so this void page
         * acts as container
         */
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCapabilitiesLinks() {

        org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo ci1 =
                new org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo(
                        "FakeService1", new Version("1.0.0"), "../caps1_v1");
        org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo ci2 =
                new org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo(
                        "FakeService1", new Version("1.1.0"), "../caps1_v2");
        org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo ci3 =
                new org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo(
                        "FakeService2", new Version("1.1.0"), "../caps2");

        CapabilitiesHomePagePanel panel = new CapabilitiesHomePagePanel("capsList", Arrays.asList(ci1, ci2, ci3));

        TestPage page = new TestPage();
        page.add(panel);

        tester.startPage(page);

        // super.print(page, false, true);

        tester.assertLabel("capsList:services:0:link:service", "FakeService1");
        tester.assertLabel("capsList:services:1:link:service", "FakeService1");
        tester.assertLabel("capsList:services:2:link:service", "FakeService2");

        tester.assertLabel("capsList:services:0:link:version", "1.1.0");
        tester.assertLabel("capsList:services:1:link:version", "1.0.0");
        tester.assertLabel("capsList:services:2:link:version", "1.1.0");
    }

    @Test
    public void testCapabilitiesLinksSkipped() {
        Service fakeService1 =
                new Service("Fake", new Object(), new Version("1.0.0"), Arrays.asList("GetCapabilities"));
        Service fakeService2 =
                new Service("Fake2", new Object(), new Version("1.0.0"), Arrays.asList("GetCapabilities"));

        try {
            GeoServerExtensionsHelper.singleton("fake1", fakeService1);
            GeoServerExtensionsHelper.singleton("fake2", fakeService2);

            // Ensure that FakeServiceDescriptionProvider is being picked up as a service to skip
            ServiceInfoCapabilitiesProvider provider = new ServiceInfoCapabilitiesProvider();
            Set<String> skip = provider.skipServiceDescriptionProviders();
            assertTrue(skip.contains("fake"));

            // Ask provider what services are available, and ensure fakseService1 is not listed
            // as it is covered by FakeServiceDescriptionProvider above
            CapabilitiesHomePagePanel panel = (CapabilitiesHomePagePanel) provider.getCapabilitiesComponent("capsList");

            TestPage page = new TestPage();
            page.add(panel);

            tester.startPage(page);

            tester.assertLabel("capsList:services:0:link:service", "Fake2");
            tester.assertLabel("capsList:services:0:link:version", "1.0.0");
        } finally {
            GeoServerExtensionsHelper.clear();
        }
    }
}
