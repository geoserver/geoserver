/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

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

    @Test
    public void testModuleStatusPanelVersion() {
        //Skip this test if we are excecuting from an IDE; the version is extracted from the compiled jar
        Assume.assumeFalse(ModuleStatusImpl.class.getResource("ModuleStatusImpl.class").getProtocol().equals("file"));

        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:1:link", true);
        tester.assertContains("gs-main");
        Component component = tester.getComponentFromLastRenderedPage("tabs:panel:listViewContainer:modules:1:version");
        assertTrue(component instanceof Label);
        assertNotNull(component.getDefaultModelObjectAsString());
        assertNotEquals("", component.getDefaultModelObjectAsString().trim());
    }

    @Test
    public void testModuleStatusPopup() {
        tester.assertRenderedPage(StatusPage.class);
        tester.clickLink("tabs:tabs-container:tabs:1:link", true);
        tester.clickLink("tabs:panel:listViewContainer:modules:1:msg", true);
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
        tester.clickLink("tabs:tabs-container:tabs:2:link", true);
        // render extra tab content
        tester.assertRenderedPage(StatusPage.class);
        // check that extra tab content was rendered
        tester.assertContains("extra tab content");
        // check that the tab has the correct title
        Component component = tester.getComponentFromLastRenderedPage(
                "tabs:tabs-container:tabs:2:link:title");
        assertThat(component, instanceOf(Label.class));
        Label label = (Label) component;
        assertThat(label.getDefaultModel(), notNullValue());
        assertThat(label.getDefaultModel().getObject(), is("extra"));
    }

    /**
     * Extra tab definition that will be added to GeoServer status page.
     */
    public static final class ExtraTabDefinition implements StatusPage.TabDefinition  {

        @Override
        public String getTitleKey() {
            return "StatusPageTest.extra";
        }

        @Override
        public Panel createPanel(String panelId, Page containerPage) {
            return new ExtraTabPanel(panelId);
        }
    }
}
