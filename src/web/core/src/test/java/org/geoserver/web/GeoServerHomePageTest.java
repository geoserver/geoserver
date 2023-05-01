/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class GeoServerHomePageTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath*:/org/geoserver/web/GeoServerHomePageTestContext.xml");
    }

    @Before
    public void resetMail() {
        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        SettingsInfo settings = global.getSettings();
        settings.getContact().setContactEmail("andrea@geoserver.org");
        gs.save(global);
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
        List<GeoServerHomePageContentProvider> providers =
                geoServerApplication.getBeansOfType(GeoServerHomePageContentProvider.class);
        assertFalse(providers.isEmpty());
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

    @Test
    public void testEmailEscape() {
        // try adding a HTML bit
        GeoServerApplication geoServerApplication = getGeoServerApplication();
        GeoServer gs = geoServerApplication.getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        SettingsInfo settings = global.getSettings();
        settings.getContact().setContactEmail("<b>hello</b>test@mail.com");
        gs.save(global);

        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();
        assertThat(
                html,
                CoreMatchers.containsString(
                        " <a href=\"mailto:&lt;b&gt;hello&lt;/b&gt;test@mail.com\">administrator</a>"));
    }

    public static class MockHomePageContentProvider implements GeoServerHomePageContentProvider {
        @Override
        public Component getPageBodyComponent(final String id) {
            return new Label(id, "MockHomePageContentProvider");
        }
    }
}
