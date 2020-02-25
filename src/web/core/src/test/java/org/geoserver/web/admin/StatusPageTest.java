/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.*;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

public class StatusPageTest extends GeoServerWicketTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // print(tester.getLastRenderedPage(), true, true);
    }

    @Before
    public void setupTests() {
        login();
        tester.startPage(StatusPage.class);
    }

    @Test
    public void testValues() {
        tester.assertRenderedPage(StatusPage.class);
        tester.assertLabel("tabs:panel:locks", "0");
        tester.assertLabel("tabs:panel:jai.memory.used", "0 KB");
    }

    @Test
    public void testFreeLocks() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:panel:free.locks", false);
        tester.assertRenderedPage(StatusPage.class);
    }

    @Test
    public void testFreeMemory() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:panel:free.memory", false);
        tester.assertRenderedPage(StatusPage.class);
    }

    @Test
    public void testFreeMemoryJAI() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:panel:free.memory.jai", false);
        tester.assertRenderedPage(StatusPage.class);
    }

    @Test
    public void testClearCache() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:panel:clear.resourceCache", true);
        tester.assertRenderedPage(StatusPage.class);
    }

    @Test
    public void testReloadConfig() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:panel:reload.catalogConfig", true);
        tester.assertRenderedPage(StatusPage.class);
    }

    @Test
    public void testReload() throws Exception {
        // the status page was rendered as expected
        tester.assertRenderedPage(StatusPage.class);

        // now force a config reload
        getGeoServer().reload();

        // force the page reload
        login();
        tester.startPage(StatusPage.class);

        // check we did not NPE
        tester.assertRenderedPage(StatusPage.class);
    }

    @Test
    public void testModuleStatusPanel() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:1:link", true);
        tester.assertContains("gs-main");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testModuleStatusPanelOrder() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:1:link", true);
        tester.assertContains("gs-main");
        Component component =
                tester.getComponentFromLastRenderedPage("tabs:panel:listViewContainer:modules");
        assertThat(component, instanceOf(ListView.class));
        List<String> modules =
                ((ListView<ModuleStatus>) component)
                        .getList()
                        .stream()
                        .map(ModuleStatus::getModule)
                        .collect(Collectors.toList());

        // verify that the expected modules are present
        assertThat(modules, hasItem("gs-main"));
        assertThat(modules, hasItem("gs-web-core"));
        assertThat(modules, hasItem("jvm"));

        // verify that the system modules are filtered
        assertThat(modules, not(hasItem(startsWith("system-"))));

        // verify that the modules are sorted
        List<String> sorted = modules.stream().sorted().collect(Collectors.toList());
        assertEquals(sorted, modules);
    }

    @Test
    public void testModuleStatusPanelVersion() {
        // Skip this test if we are excecuting from an IDE; the version is extracted from the
        // compiled jar
        Assume.assumeFalse(
                ModuleStatusImpl.class
                        .getResource("ModuleStatusImpl.class")
                        .getProtocol()
                        .equals("file"));

        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:1:link", true);
        tester.assertContains("gs-main");
        Component component =
                tester.getComponentFromLastRenderedPage(
                        "tabs:panel:listViewContainer:modules:0:version");
        assertTrue(component instanceof Label);
        assertNotNull(component.getDefaultModelObjectAsString());
        assertNotEquals("", component.getDefaultModelObjectAsString().trim());
    }

    @Test
    public void testModuleStatusPopup() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:1:link", true);
        tester.clickLink("tabs:panel:listViewContainer:modules:0:msg", true);
        tester.assertRenderedPage(StatusPage.class);
        tester.assertContains("GeoServer Main");
        tester.assertContains("gs-main");
        tester.assertContains("Message:");
    }

    @Test
    public void testExtraTabExists() {
        // render the page, GeoServer status tab is show
        tester.assertRenderedPage(StatusPage.class);
        // click on the extra tab link
        tester.clickLink("tabs:tabs-container:tabs:3:link", true);
        // render extra tab content
        tester.assertRenderedPage(StatusPage.class);
        // check that extra tab content was rendered
        tester.assertContains("extra tab content");
        // check that the tab has the correct title
        Component component =
                tester.getComponentFromLastRenderedPage("tabs:tabs-container:tabs:3:link:title");
        assertThat(component, instanceOf(Label.class));
        Label label = (Label) component;
        assertThat(label.getDefaultModel(), notNullValue());
        assertThat(label.getDefaultModel().getObject(), is("extra"));
    }

    /** Extra tab definition that will be added to GeoServer status page. */
    public static final class ExtraTabDefinition implements StatusPage.TabDefinition {

        @Override
        public String getTitleKey() {
            return "StatusPageTest.extra";
        }

        @Override
        public Panel createPanel(String panelId, Page containerPage) {
            return new ExtraTabPanel(panelId);
        }
    }

    @Test
    public void redirectUnauthorizedToLogin() throws Exception {
        logout();
        MockHttpServletResponse response =
                getAsServletResponse(
                        "web/wicket/bookmarkable/org.geoserver.web.admin"
                                + ".StatusPage?29-1.ILinkListener-tabs-tabs~container-tabs-1-link");
        assertEquals(HttpStatus.FOUND.value(), response.getStatus());
        assertEquals("./org.geoserver.web.GeoServerLoginPage", response.getHeader("Location"));
    }

    @Test
    public void testTabSwitch() {
        // render the page, GeoServer status tab is show
        tester.assertRenderedPage(StatusPage.class);
        // click on the system status tab
        tester.clickLink("tabs:tabs-container:tabs:2:link", true);
        // render the system monitoring information
        tester.assertRenderedPage(StatusPage.class);
        // check that we have system monitoring tab expected content
        tester.assertContains("CPUs");
        // now click on modules tab
        tester.clickLink("tabs:tabs-container:tabs:1:link", true);
        // no rendering error should happen
        tester.assertRenderedPage(StatusPage.class);
        // check that we have a valid content
        tester.assertContains("gs-main");
    }
}
