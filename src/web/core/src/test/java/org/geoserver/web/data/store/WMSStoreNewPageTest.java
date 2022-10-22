/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
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
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;

public class WMSStoreNewPageTest extends GeoServerWicketTestSupport {

    /** print page structure? */
    private static final boolean debugMode = false;

    private static WireMockServer wmsService;

    private static String capabilities;

    @BeforeClass
    public static void beforeClass() throws Exception {
        WireMockConfiguration config = wireMockConfig().dynamicPort();
        if (debugMode) config.notifier(new ConsoleNotifier(debugMode));
        wmsService = new WireMockServer(config);
        wmsService.start();
        capabilities =
                "http://localhost:"
                        + wmsService.port()
                        + "/geoserver/wms?REQUEST=GetCapabilities&VERSION=1.3.0&SERVICE=WMS";
        wmsService.stubFor(
                WireMock.get(
                                urlEqualTo(
                                        "/geoserver/wms?REQUEST=GetCapabilities&VERSION=1.3.0&SERVICE=WMS"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                                        .withBodyFile("caps130.xml")));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        wmsService.shutdown();
    }

    @Before
    public void init() {}

    private WMSStoreNewPage startPage() {

        login();
        final WMSStoreNewPage page = new WMSStoreNewPage();
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

        WMSStoreNewPage page = startPage();
        // print(page, true, true);

        assertNull(page.getDefaultModelObject());

        tester.assertModelValue("form:enabledPanel:paramValue", Boolean.TRUE);
        tester.assertModelValue(
                "form:workspacePanel:border:border_body:paramValue",
                getCatalog().getDefaultWorkspace());
    }

    @Test
    public void testSaveNewStore() {

        WMSStoreNewPage page = startPage();
        // print(page, true, true);

        assertNull(page.getDefaultModelObject());

        final Catalog catalog = getCatalog();
        WMSStoreInfo info = catalog.getFactory().createWebMapServer();
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
        assertTrue(msg.toString().startsWith("Connection test failed:"));

        catalog.save(info);

        assertNotNull(info.getId());

        WMSStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

        assertNotNull(expandedStore.getId());
        assertNotNull(expandedStore.getCatalog());

        catalog.validate(expandedStore, false).throwIfInvalid();
    }

    @Test
    public void testSaveNewStoreEntityExpansion() throws Exception {

        WMSStoreNewPage page = startPage();

        assertNull(page.getDefaultModelObject());

        final Catalog catalog = getCatalog();
        WMSStoreInfo info = getCatalog().getFactory().createWebMapServer();
        URL url = getClass().getResource("1.3.0Capabilities-xxe.xml");
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

        WMSStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

        assertNotNull(expandedStore.getId());
        assertNotNull(expandedStore.getCatalog());

        catalog.validate(expandedStore, false).throwIfInvalid();
    }

    @Test
    public void testDisableOnConnFailureCheckbox() {

        startPage();

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
                catalog.getStoreByName("fooAutoDisable", WMSStoreInfo.class)
                        .isDisableOnConnFailure());
    }
}
