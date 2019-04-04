/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class WMTSStoreNewPageTest extends GeoServerWicketTestSupport {

    /** print page structure? */
    private static final boolean debugMode = true;

    @Before
    public void init() {
        Logging.getLogger("org.geoserver").setLevel(Level.FINE);
        Logging.getLogger("org.vfny.geoserver").setLevel(Level.FINE);
        Logging.getLogger("org.geotools").setLevel(Level.FINE);
    }

    private WMTSStoreNewPage startPage() {

        login();
        final WMTSStoreNewPage page = new WMTSStoreNewPage();
        tester.startPage(page);

        if (debugMode) {
            print(page, true, true);
        }

        return page;
    }

    /** A kind of smoke test that only asserts the page is rendered when first loaded */
    @Test
    public void testPageRendersOnLoad() {

        startPage();

        tester.assertComponent("form:workspacePanel", WorkspacePanel.class);
    }

    @Test
    public void testInitialModelState() {

        WMTSStoreNewPage page = startPage();
        // print(page, true, true);

        assertNull(page.getDefaultModelObject());

        tester.assertModelValue("form:enabledPanel:paramValue", Boolean.TRUE);
        tester.assertModelValue(
                "form:workspacePanel:border:border_body:paramValue",
                getCatalog().getDefaultWorkspace());
    }

    @Test
    public void testSaveNewStore() {

        WMTSStoreNewPage page = startPage();
        // print(page, true, true);

        assertNull(page.getDefaultModelObject());

        final Catalog catalog = getCatalog();
        WMTSStoreInfo info = catalog.getFactory().createWebMapTileServer();
        info.setName("foo");

        tester.assertNoErrorMessage();

        FormTester form = tester.newFormTester("form");
        form.select("workspacePanel:border:border_body:paramValue", 4);
        Component wsDropDown =
                tester.getComponentFromLastRenderedPage(
                        "form:workspacePanel:border:border_body:paramValue");
        tester.executeAjaxEvent(wsDropDown, "change");
        form.setValue("namePanel:border:border_body:paramValue", "foo");
        form.setValue("capabilitiesURL:border:border_body:paramValue", "http://foo");

        tester.clickLink("form:save", true);
        tester.assertErrorMessages("WMTS Connection test failed: foo");
        catalog.save(info);

        assertNotNull(info.getId());

        WMTSStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

        assertNotNull(expandedStore.getId());
        assertNotNull(expandedStore.getCatalog());

        catalog.validate(expandedStore, false).throwIfInvalid();
    }

    @Test
    @Ignore
    public void testSaveNewStoreEntityExpansion() throws Exception {

        WMTSStoreNewPage page = startPage();

        assertNull(page.getDefaultModelObject());

        final Catalog catalog = getCatalog();
        WMTSStoreInfo info = (WMTSStoreInfo) getCatalog().getFactory().createWebMapTileServer();
        URL url = getClass().getResource("WMTSGetCapabilities.xml");
        assertNotNull(url);
        info.setName("bar");

        tester.assertNoErrorMessage();

        FormTester form = tester.newFormTester("form");
        form.select("workspacePanel:border:border_body:paramValue", 4);
        Component wsDropDown =
                tester.getComponentFromLastRenderedPage(
                        "form:workspacePanel:border:border_body:paramValue");
        tester.executeAjaxEvent(wsDropDown, "change");
        form.setValue("namePanel:border:border_body:paramValue", "bar");
        form.setValue("capabilitiesURL:border:border_body:paramValue", url.toExternalForm());

        tester.clickLink("form:save", true);
        tester.assertErrorMessages("Connection test failed: Error while parsing XML.");

        // make sure clearing the catalog does not clear the EntityResolver
        getGeoServer().reload();
        tester.clickLink("form:save", true);
        tester.assertErrorMessages("Connection test failed: Error while parsing XML.");

        catalog.save(info);

        assertNotNull(info.getId());

        WMTSStoreInfo expandedStore = (WMTSStoreInfo) catalog.getResourcePool().clone(info, true);

        assertNotNull(expandedStore.getId());
        assertNotNull(expandedStore.getCatalog());

        catalog.validate(expandedStore, false).throwIfInvalid();
    }
}
