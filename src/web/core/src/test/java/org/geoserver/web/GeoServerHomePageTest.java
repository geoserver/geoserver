package org.geoserver.web;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;

public class GeoServerHomePageTest extends GeoServerWicketTestSupport {

    @Override
    protected String[] getSpringContextLocations() {
        String[] defaultLocations = super.getSpringContextLocations();
        String[] extraLocations = new String[defaultLocations.length + 1];
        System.arraycopy(defaultLocations, 0, extraLocations, 0, defaultLocations.length);
        extraLocations[extraLocations.length - 1] = "classpath*:/org/geoserver/web/GeoServerHomePageTestContext.xml";
        return extraLocations;
    }

    public void testProvidedGetCapabilities() {
        tester.startPage(GeoServerHomePage.class);

        tester.assertListView(
                "providedCaps",
                Collections.singletonList(getGeoServerApplication().getBeanOfType(
                        CapabilitiesHomePageLinkProvider.class)));
    }

    public void testProvidedCentralBodyContent() {

        tester.startPage(GeoServerHomePage.class);

        GeoServerApplication geoServerApplication = getGeoServerApplication();
        List<GeoServerHomePageContentProvider> providers;
        providers = geoServerApplication.getBeansOfType(GeoServerHomePageContentProvider.class);
        assertTrue(providers.size() > 0);
        tester.assertListView("contributedContent", providers);
    }

    public static class MockHomePageContentProvider implements GeoServerHomePageContentProvider {
        public Component getPageBodyComponent(final String id) {
            return new Label(id, "MockHomePageContentProvider");
        }

    }
}
