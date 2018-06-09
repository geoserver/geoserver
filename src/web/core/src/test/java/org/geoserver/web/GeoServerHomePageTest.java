/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.junit.Test;

public class GeoServerHomePageTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath*:/org/geoserver/web/GeoServerHomePageTestContext.xml");
    }

    @Test
    public void testProvidedGetCapabilities() {
        tester.startPage(GeoServerHomePage.class);

        tester.assertListView(
                "providedCaps",
                Collections.singletonList(
                        getGeoServerApplication()
                                .getBeanOfType(CapabilitiesHomePageLinkProvider.class)));
    }

    @Test
    public void testProvidedCentralBodyContent() {

        tester.startPage(GeoServerHomePage.class);

        GeoServerApplication geoServerApplication = getGeoServerApplication();
        List<GeoServerHomePageContentProvider> providers;
        providers = geoServerApplication.getBeansOfType(GeoServerHomePageContentProvider.class);
        assertTrue(providers.size() > 0);
        tester.assertListView("contributedContent", providers);
    }

    @Test
    public void testEmailIfNull() {
        GeoServerApplication geoServerApplication = getGeoServerApplication();
        String contactEmail =
                geoServerApplication
                        .getGeoServer()
                        .getGlobal()
                        .getSettings()
                        .getContact()
                        .getContactEmail();
        assertEquals(
                "andrea@geoserver.org",
                contactEmail == null ? "andrea@geoserver.org" : contactEmail);
    }

    public static class MockHomePageContentProvider implements GeoServerHomePageContentProvider {
        public Component getPageBodyComponent(final String id) {
            return new Label(id, "MockHomePageContentProvider");
        }
    }
}
