/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.wicket.markup.html.WebPage;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
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

    /** Helper class that exposes a ServiceInfo with configurable disabled versions. */
    private static class FakeServiceBackingObject {
        private final ServiceInfo serviceInfo;

        FakeServiceBackingObject(boolean enabled, List<Version> disabledVersions) {
            ServiceInfoImpl info = new ServiceInfoImpl();
            info.setEnabled(enabled);
            info.setDisabledVersions(disabledVersions);
            this.serviceInfo = info;
        }

        public ServiceInfo getServiceInfo() {
            return serviceInfo;
        }
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

    @Test
    public void testDisabledVersionsNotShown() {
        // 1.0.0 is disabled, 1.1.0 is enabled
        List<Version> disabled = Collections.singletonList(new Version("1.0.0"));
        Object backing = new FakeServiceBackingObject(true, disabled);

        Service svc1 = new Service("TestSvc", backing, new Version("1.0.0"), Arrays.asList("GetCapabilities"));
        Service svc2 = new Service("TestSvc", backing, new Version("1.1.0"), Arrays.asList("GetCapabilities"));

        try {
            GeoServerExtensionsHelper.singleton("testSvc1", svc1);
            GeoServerExtensionsHelper.singleton("testSvc2", svc2);

            ServiceInfoCapabilitiesProvider provider = new ServiceInfoCapabilitiesProvider();
            CapabilitiesHomePagePanel panel = (CapabilitiesHomePagePanel) provider.getCapabilitiesComponent("capsList");

            TestPage page = new TestPage();
            page.add(panel);
            tester.startPage(page);

            // only version 1.1.0 should appear
            tester.assertLabel("capsList:services:0:link:service", "TestSvc");
            tester.assertLabel("capsList:services:0:link:version", "1.1.0");
        } finally {
            GeoServerExtensionsHelper.clear();
        }
    }

    @Test
    public void testAllVersionsShownWhenNoneDisabled() {
        Object backing = new FakeServiceBackingObject(true, new ArrayList<>());
        Service svc1 = new Service("TestSvc", backing, new Version("1.0.0"), Arrays.asList("GetCapabilities"));
        Service svc2 = new Service("TestSvc", backing, new Version("1.1.0"), Arrays.asList("GetCapabilities"));

        try {
            GeoServerExtensionsHelper.singleton("testSvc1", svc1);
            GeoServerExtensionsHelper.singleton("testSvc2", svc2);

            ServiceInfoCapabilitiesProvider provider = new ServiceInfoCapabilitiesProvider();
            CapabilitiesHomePagePanel panel = (CapabilitiesHomePagePanel) provider.getCapabilitiesComponent("capsList");

            TestPage page = new TestPage();
            page.add(panel);
            tester.startPage(page);

            // both versions should appear
            tester.assertLabel("capsList:services:0:link:version", "1.1.0");
            tester.assertLabel("capsList:services:1:link:version", "1.0.0");
        } finally {
            GeoServerExtensionsHelper.clear();
        }
    }

    @Test
    public void testDisabledServiceNotShown() {
        Object backing = new FakeServiceBackingObject(false, new ArrayList<>());
        Service svc1 = new Service("TestSvc", backing, new Version("1.0.0"), Arrays.asList("GetCapabilities"));

        try {
            GeoServerExtensionsHelper.singleton("testSvc1", svc1);

            ServiceInfoCapabilitiesProvider provider = new ServiceInfoCapabilitiesProvider();
            CapabilitiesHomePagePanel panel = (CapabilitiesHomePagePanel) provider.getCapabilitiesComponent("capsList");

            if (panel != null) {
                TestPage page = new TestPage();
                page.add(panel);
                tester.startPage(page);

                int serviceCount = getServiceCount(panel);
                for (int i = 0; i < serviceCount; i++) {
                    String serviceName = tester.getComponentFromLastRenderedPage(
                                    "capsList:services:" + i + ":link:service")
                            .getDefaultModelObjectAsString();
                    assertNotEquals("Disabled service TestSvc should not appear in panel", "TestSvc", serviceName);
                }
            }
        } finally {
            GeoServerExtensionsHelper.clear();
        }
    }

    private int getServiceCount(CapabilitiesHomePagePanel panel) {
        // Access the panel's model to count services
        @SuppressWarnings("unchecked")
        List<?> services = (List<?>) panel.getDefaultModelObject();
        return services != null ? services.size() : 0;
    }
}
