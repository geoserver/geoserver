/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;

/** Checks the cascaded layers follow the store when its workspace is changed from the editor page. */
public class WMSStoreEditPageTest extends GeoServerWicketTestSupport {

    private static final String WS_DROPDOWN = "workspacePanel:border:border_body:paramValue";

    private static WireMockServer service;

    private static String capabilities;

    private WMSStoreInfo store;

    @BeforeClass
    public static void beforeClass() {
        service = new WireMockServer(wireMockConfig().dynamicPort());
        service.start();
        capabilities = "http://localhost:" + service.port()
                + "/geoserver/wms?REQUEST=GetCapabilities&VERSION=1.3.0&SERVICE=WMS";
        service.stubFor(WireMock.get(urlEqualTo("/geoserver/wms?REQUEST=GetCapabilities&VERSION=1.3.0&SERVICE=WMS"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                        .withBodyFile("caps130.xml")));
    }

    @AfterClass
    public static void afterClass() {
        service.stop();
    }

    @Before
    public void init() throws Exception {
        login();
        Catalog catalog = getCatalog();

        // built like the new store page does it, the editor validates the connection defaults it sets
        CatalogBuilder builder = new CatalogBuilder(catalog);
        store = builder.buildWMSStore("cascade");
        store.setCapabilitiesURL(capabilities);
        // an enabled store makes the editor run a live capabilities check on save, and a failure there opens a
        // confirmation dialog instead of saving, which has nothing to do with what this test covers
        store.setEnabled(false);
        catalog.add(store);

        builder.setStore(store);
        WMSLayerInfo resource = builder.buildWMSLayer("world4326");
        catalog.add(resource);
        LayerInfo layer = builder.buildLayer(resource);
        catalog.add(layer);

        // the page must get the catalog's proxy view, saving a raw store fails in the catalog facade
        tester.startPage(new WMSStoreEditPage(catalog.getStore(store.getId(), WMSStoreInfo.class)));
    }

    @Test
    public void testWorkspaceSyncsUpWithNamespace() {
        Catalog catalog = getCatalog();
        WorkspaceInfo original = store.getWorkspace();

        FormTester form = tester.newFormTester("form");
        form.select(WS_DROPDOWN, 2);
        tester.clickLink("form:save", true);

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(StorePage.class);

        WMSStoreInfo saved = catalog.getStore(store.getId(), WMSStoreInfo.class);
        WorkspaceInfo workspace = saved.getWorkspace();
        assertNotEquals(original.getName(), workspace.getName());

        List<WMSLayerInfo> cascaded = catalog.getResourcesByStore(saved, WMSLayerInfo.class);
        assertFalse(cascaded.isEmpty());
        for (WMSLayerInfo resource : cascaded) {
            assertEquals(
                    "Namespace for " + resource.getName() + " was not updated",
                    workspace.getName(),
                    resource.getNamespace().getPrefix());
        }
    }
}
