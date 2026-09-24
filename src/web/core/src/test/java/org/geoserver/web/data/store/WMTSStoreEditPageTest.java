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
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;

/** Checks the cascaded layers follow the store when its workspace is changed from the editor page. */
public class WMTSStoreEditPageTest extends GeoServerWicketTestSupport {

    private static final String WS_DROPDOWN = "workspacePanel:border:border_body:paramValue";

    private static WireMockServer service;

    private static String capabilities;

    private WMTSStoreInfo store;

    @BeforeClass
    public static void beforeClass() {
        service = new WireMockServer(wireMockConfig().dynamicPort());
        service.start();
        capabilities = "http://localhost:" + service.port()
                + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS";
        service.stubFor(WireMock.get(urlEqualTo("/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                        .withBodyFile("nasa.getcapa.xml")));
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
        store = builder.buildWMTSStore("cascade");
        store.setCapabilitiesURL(capabilities);
        // an enabled store makes the editor run a live capabilities check on save, and a failure there opens a
        // confirmation dialog instead of saving, which has nothing to do with what this test covers
        store.setEnabled(false);
        catalog.add(store);

        builder.setStore(store);
        WMTSLayerInfo resource = builder.buildWMTSLayer("AMSR2_Snow_Water_Equivalent");
        catalog.add(resource);
        LayerInfo layer = builder.buildLayer(resource);
        catalog.add(layer);

        // the page must get the catalog's proxy view, saving a raw store fails in the catalog facade
        tester.startPage(new WMTSStoreEditPage(catalog.getStore(store.getId(), WMTSStoreInfo.class)));
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

        WMTSStoreInfo saved = catalog.getStore(store.getId(), WMTSStoreInfo.class);
        WorkspaceInfo workspace = saved.getWorkspace();
        assertNotEquals(original.getName(), workspace.getName());

        List<WMTSLayerInfo> cascaded = catalog.getResourcesByStore(saved, WMTSLayerInfo.class);
        assertFalse(cascaded.isEmpty());
        for (WMTSLayerInfo resource : cascaded) {
            assertEquals(
                    "Namespace for " + resource.getName() + " was not updated",
                    workspace.getName(),
                    resource.getNamespace().getPrefix());
        }
    }
}
