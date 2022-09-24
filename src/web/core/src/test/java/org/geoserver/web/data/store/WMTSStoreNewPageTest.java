/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.util.logging.Logging;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;

public class WMTSStoreNewPageTest extends GeoServerWicketTestSupport {

    /** print page structure? */
    private static final boolean debugMode = false;

    private static WireMockServer wmtsService;

    private static String capabilities;

    @BeforeClass
    public static void beforeClass() throws Exception {
        WireMockConfiguration config = wireMockConfig().dynamicPort();
        if (debugMode) config.notifier(new ConsoleNotifier(debugMode));
        wmtsService = new WireMockServer(config);
        wmtsService.start();
        capabilities =
                "http://localhost:"
                        + wmtsService.port()
                        + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS";
        wmtsService.stubFor(
                WireMock.get(
                                urlEqualTo(
                                        "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                                        .withBodyFile("nasa.getcapa.xml")));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        wmtsService.shutdown();
    }

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

        List<FeedbackMessage> feedback = tester.getFeedbackMessages(IFeedbackMessageFilter.ALL);
        assertEquals(1, feedback.size());
        Serializable msg = feedback.get(0).getMessage();
        assertTrue(msg.toString().startsWith("WMTS Connection test failed:"));

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
        WMTSStoreInfo info = getCatalog().getFactory().createWebMapTileServer();
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

        WMTSStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

        assertNotNull(expandedStore.getId());
        assertNotNull(expandedStore.getCatalog());

        catalog.validate(expandedStore, false).throwIfInvalid();
    }

    @Test
    public void testDisableOnConnFailureCheckbox() throws Exception {

        startPage();

        tester.assertNoErrorMessage();

        FormTester form = tester.newFormTester("form");
        form.select("workspacePanel:border:border_body:paramValue", 4);
        Component wsDropDown =
                tester.getComponentFromLastRenderedPage(
                        "form:workspacePanel:border:border_body:paramValue");
        tester.executeAjaxEvent(wsDropDown, "change");
        form.setValue("namePanel:border:border_body:paramValue", "fooAutoDisable");
        form.setValue("capabilitiesURL:border:border_body:paramValue", capabilities);
        Component component =
                tester.getComponentFromLastRenderedPage(
                        "form:disableOnConnFailurePanel:paramValue");
        CheckBox checkBox = (CheckBox) component;
        assertFalse(Boolean.valueOf(checkBox.getInput()).booleanValue());
        form.setValue("disableOnConnFailurePanel:paramValue", true);

        form.submit("save");
        tester.assertNoErrorMessage();
        final Catalog catalog = getCatalog();
        assertTrue(
                catalog.getStoreByName("fooAutoDisable", WMTSStoreInfo.class)
                        .isDisableOnConnFailure());
    }
}
