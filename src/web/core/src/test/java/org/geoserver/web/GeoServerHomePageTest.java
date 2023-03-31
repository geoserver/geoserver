/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.geoserver.data.test.MockData.CITE_PREFIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geotools.util.GrowableInternationalString;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class GeoServerHomePageTest extends GeoServerWicketTestSupport {

    @Before
    public void setupMode() {
        // avoid flip-flops due to timeouts in the testing environment
        HomePageSelection.MODE = HomePageSelection.SelectionMode.DROPDOWN;
    }

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
    public void testHelloWorld() {
        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        SettingsInfo settings = global.getSettings();
        ContactInfo contact = settings.getContact();

        GrowableInternationalString helloWorld = new GrowableInternationalString("Hello World");
        helloWorld.add(Locale.ENGLISH, "Hello World");
        helloWorld.add(Locale.ITALIAN, "Ciao mondo");
        helloWorld.add(Locale.FRENCH, "Bonjour le monde");

        contact.setWelcome("Hello world");
        contact.setInternationalWelcome(helloWorld);
        gs.save(global);

        tester.getSession().setLocale(Locale.ITALIAN);
        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();
        assertThat(html, CoreMatchers.containsString("Ciao mondo"));

        tester.getSession().setLocale(Locale.FRENCH);
        tester.startPage(GeoServerHomePage.class);
        html = tester.getLastResponseAsString();
        assertThat(html, CoreMatchers.containsString("Bonjour le monde"));
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

    @Test
    @SuppressWarnings("unchecked")
    public void testDropDownSelection() throws Exception {
        tester.startPage(GeoServerHomePage.class);
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
        Page page1 = tester.getLastRenderedPage();

        // check workspaces use a drop-down with suitable content
        Select2DropDownChoice<WorkspaceInfo> workspaceSelector =
                (Select2DropDownChoice<WorkspaceInfo>)
                        tester.getComponentFromLastRenderedPage("form:workspace:select");
        List<WorkspaceInfo> workspaces = getCatalog().getWorkspaces();
        List<? extends WorkspaceInfo> workspaceChoices = workspaceSelector.getChoices();
        assertEquals(workspaces.size(), workspaceChoices.size());

        // and same goes for the layers case
        Select2DropDownChoice<PublishedInfo> publishedSelector =
                (Select2DropDownChoice<PublishedInfo>)
                        tester.getComponentFromLastRenderedPage("form:layer:select");
        List<PublishedInfo> publisheds = new ArrayList<>(getCatalog().getLayers());
        publisheds.addAll(getCatalog().getLayerGroups());
        List<? extends PublishedInfo> publishedChoices = publishedSelector.getChoices();
        assertEquals(publisheds.size(), publishedChoices.size());

        // select a workspace
        FormTester form = tester.newFormTester("form");
        form.setValue("workspace:select", CITE_PREFIX);
        tester.executeAjaxEvent("form:workspace:select", "change");

        // it switched to a new page with a single workspace
        tester.assertRenderedPage(GeoServerHomePage.class);
        GeoServerHomePage page2 = (GeoServerHomePage) tester.getLastRenderedPage();
        assertNotSame(page1, page2);
        assertEquals(page2.getWorkspaceInfo(), getCatalog().getWorkspaceByName(CITE_PREFIX));

        // switch layer as well
        form = tester.newFormTester("form");
        form.setValue("layer:select", BASIC_POLYGONS.getLocalPart());
        tester.executeAjaxEvent("form:layer:select", "change");

        // it switched to a new page with a single layer
        tester.assertRenderedPage(GeoServerHomePage.class);
        GeoServerHomePage page3 = (GeoServerHomePage) tester.getLastRenderedPage();
        assertNotSame(page1, page3);
        assertNotSame(page2, page3);
        assertEquals(page3.getWorkspaceInfo(), getCatalog().getWorkspaceByName(CITE_PREFIX));
        assertEquals(
                page3.getPublishedInfo(), getCatalog().getLayerByName(getLayerId(BASIC_POLYGONS)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDropDownLayerSelection() throws Exception {
        tester.startPage(GeoServerHomePage.class);
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
        Page page1 = tester.getLastRenderedPage();

        // select a layer directly
        FormTester form = tester.newFormTester("form");
        form.setValue("layer:select", getLayerId(BASIC_POLYGONS));
        tester.executeAjaxEvent("form:layer:select", "change");

        // it switched to a new page with a single layer
        tester.assertRenderedPage(GeoServerHomePage.class);
        GeoServerHomePage page2 = (GeoServerHomePage) tester.getLastRenderedPage();
        assertNotSame(page1, page2);
        assertEquals(page2.getWorkspaceInfo(), getCatalog().getWorkspaceByName(CITE_PREFIX));
        assertEquals(
                page2.getPublishedInfo(), getCatalog().getLayerByName(getLayerId(BASIC_POLYGONS)));

        // now un-select the layer
        form = tester.newFormTester("form");
        form.setValue("layer:select", null);
        tester.executeAjaxEvent("form:layer:select", "change");

        // it switched to a new page with a single layer
        tester.assertRenderedPage(GeoServerHomePage.class);
        GeoServerHomePage page3 = (GeoServerHomePage) tester.getLastRenderedPage();
        assertNotSame(page1, page3);
        assertNotSame(page2, page3);
        assertEquals(page2.getWorkspaceInfo(), getCatalog().getWorkspaceByName(CITE_PREFIX));
        assertNull(page2.getPublishedInfo());
    }

    @Test
    public void testTextSelection() throws Exception {
        HomePageSelection.MODE = HomePageSelection.SelectionMode.TEXT;
        tester.startPage(GeoServerHomePage.class);
        tester.assertNoErrorMessage();
        tester.assertRenderedPage(GeoServerHomePage.class);
        GeoServerHomePage page1 = (GeoServerHomePage) tester.getLastRenderedPage();

        // check workspaces and layers use a simple text field
        tester.assertComponent("form:workspace:text", TextField.class);
        tester.assertComponent("form:layer:text", TextField.class);

        // select a workspace
        FormTester form = tester.newFormTester("form");
        form.setValue("workspace:text", CITE_PREFIX);
        tester.executeAjaxEvent("form:workspace:text", "change");

        // it switched to a new page with a single workspace
        tester.assertRenderedPage(GeoServerHomePage.class);
        GeoServerHomePage page2 = (GeoServerHomePage) tester.getLastRenderedPage();
        assertNotSame(page1, page2);
        assertEquals(page2.getWorkspaceInfo(), getCatalog().getWorkspaceByName(CITE_PREFIX));

        // switch layer as well
        form = tester.newFormTester("form");
        form.setValue("layer:text", BASIC_POLYGONS.getLocalPart());
        tester.executeAjaxEvent("form:layer:text", "change");

        // it switched to a new page with a single layer
        tester.assertRenderedPage(GeoServerHomePage.class);
        GeoServerHomePage page3 = (GeoServerHomePage) tester.getLastRenderedPage();
        assertNotSame(page1, page3);
        assertNotSame(page2, page3);
        assertEquals(page3.getWorkspaceInfo(), getCatalog().getWorkspaceByName(CITE_PREFIX));
        assertEquals(
                page3.getPublishedInfo(), getCatalog().getLayerByName(getLayerId(BASIC_POLYGONS)));
    }

    @Test
    public void testAutoSelection() throws Exception {
        // go automatic
        HomePageSelection.MODE = HomePageSelection.SelectionMode.AUTOMATIC;
        HomePageSelection.HOME_PAGE_TIMEOUT = 1000 * 60 * 60 * 24; // basically no time limit

        // force it to go text by means of the items
        HomePageSelection.HOME_PAGE_MAX_ITEMS = 1;
        tester.startPage(GeoServerHomePage.class);
        tester.assertComponent("form:workspace:text", TextField.class);
        tester.assertComponent("form:layer:text", TextField.class);

        // now give it just enough to fill the workspaces but not the layers
        HomePageSelection.HOME_PAGE_MAX_ITEMS = getCatalog().getWorkspaces().size();
        tester.startPage(GeoServerHomePage.class);
        tester.assertComponent("form:workspace:select", Select2DropDownChoice.class);
        tester.assertComponent("form:layer:text", TextField.class);

        // and now so much it can do dropdowns for both
        HomePageSelection.HOME_PAGE_MAX_ITEMS = Integer.MAX_VALUE;
        tester.startPage(GeoServerHomePage.class);
        tester.assertComponent("form:workspace:select", Select2DropDownChoice.class);
        tester.assertComponent("form:layer:select", Select2DropDownChoice.class);
    }

    public static class MockHomePageContentProvider implements GeoServerHomePageContentProvider {
        @Override
        public Component getPageBodyComponent(final String id) {
            return new Label(id, "MockHomePageContentProvider");
        }
    }
}
