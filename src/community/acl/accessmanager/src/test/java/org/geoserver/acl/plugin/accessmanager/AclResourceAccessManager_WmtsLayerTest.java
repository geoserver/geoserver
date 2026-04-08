package org.geoserver.acl.plugin.accessmanager;

/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.TestHttpClientRule;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.rest.catalog.WMTSLayerTest;
import org.geoserver.security.WMTSAccessLimits;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.w3c.dom.Document;

public class AclResourceAccessManager_WmtsLayerTest extends AclGeoServerSystemTestSupport {

    private static final String LAYER_NAME = "AMSR2_Snow_Water_Equivalent";

    @Rule
    public TestHttpClientRule clientMocker = new TestHttpClientRule();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // BaseTest enables the secure catalog, so we need to login first
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        // add a wmts store
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMTSStoreInfo wmts = cb.buildWMTSStore("demo");
        wmts.setCapabilitiesURL(
                clientMocker.getServer() + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS");
        catalog.add(wmts);

        // and a wmts layer as well (cannot use the builder, would turn this test into an online one
        addWmtsLayer();

        // logout before running any tests
        logout();
    }

    public void addWmtsLayer() throws Exception {
        String capabilities =
                clientMocker.getServer() + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS";
        WMTSLayerInfo wml = catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class);
        if (wml == null) {
            wml = catalog.getFactory().createWMTSLayer();
            wml.setName(LAYER_NAME);
            wml.setNativeName("topp:" + LAYER_NAME);
            wml.setStore(catalog.getStoreByName("demo", WMTSStoreInfo.class));
            wml.setCatalog(catalog);
            wml.setNamespace(catalog.getNamespaceByPrefix("sf"));
            wml.setSRS("EPSG:4326");
            CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
            wml.setNativeCRS(wgs84);
            wml.setLatLonBoundingBox(new ReferencedEnvelope(-110, 0, -60, 50, wgs84));
            wml.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

            catalog.add(wml);

            LayerInfo layer = catalog.getFactory().createLayer();
            layer.setResource(wml);
            layer.setName(LAYER_NAME);
            layer.setEnabled(true);
            catalog.add(layer);
        }

        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL(capabilities),
                new MockHttpResponse(WMTSLayerTest.class.getResource("nasa.getcapa.xml"), "text/xml"));
        clientMocker.bind(client, capabilities);
    }

    @Test
    public void testWmsLimited() {
        UsernamePasswordAuthenticationToken user = new UsernamePasswordAuthenticationToken("wmsuser", "wmsuser");

        // check layer in the sf workspace with a wfs request
        Request request = new Request();
        request.setService("WFS");
        request.setRequest("GetFeature");
        Dispatcher.REQUEST.set(request);

        login("wmsuser", "wmsuser", "ROLE_ADMINISTRATOR");
        LayerInfo wmtsLayer = catalog.getLayerByName("sf:" + LAYER_NAME);
        assertNotNull(wmtsLayer);
        logout();

        WMTSAccessLimits limits = (WMTSAccessLimits) accessManager.getAccessLimits(user, wmtsLayer);
        assertEquals(Filter.EXCLUDE, limits.getReadFilter());

        request = new Request();
        request.setService("wms"); // case shouldn't matter
        Dispatcher.REQUEST.set(request);
        limits = (WMTSAccessLimits) accessManager.getAccessLimits(user, wmtsLayer);
        assertEquals(Filter.INCLUDE, limits.getReadFilter());

        // Test resource as well
        limits = (WMTSAccessLimits) accessManager.getAccessLimits(user, wmtsLayer.getResource());
        assertEquals(Filter.INCLUDE, limits.getReadFilter());
    }

    @Test
    public void testWmsUnlimited() {
        Authentication admin = getUser("admin", "geoserver", "ROLE_ADMINISTRATOR");

        // check layer in the sf workspace with a wfs request
        Request request = new Request();
        request.setService("WFS");
        request.setRequest("GetFeature");
        Dispatcher.REQUEST.set(request);

        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        LayerInfo wmtsLayer = catalog.getLayerByName("sf:" + LAYER_NAME);
        assertNotNull(wmtsLayer);
        logout();

        WMTSAccessLimits limits = (WMTSAccessLimits) accessManager.getAccessLimits(admin, wmtsLayer);
        assertEquals(Filter.INCLUDE, limits.getReadFilter());

        // now fake a getmap request (using a service and request with a different case)
        request = new Request();
        request.setService("wms");
        Dispatcher.REQUEST.set(request);
        limits = (WMTSAccessLimits) accessManager.getAccessLimits(admin, wmtsLayer);
        assertEquals(Filter.INCLUDE, limits.getReadFilter());

        // Test resource as well
        limits = (WMTSAccessLimits) accessManager.getAccessLimits(admin, wmtsLayer.getResource());
        assertEquals(Filter.INCLUDE, limits.getReadFilter());
    }

    @Test
    public void testGetWmtsLayer() {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        WorkspaceInfo ws = catalog.getWorkspaceByName("sf");
        assertNotNull(ws);

        WMTSStoreInfo store = catalog.getStoreByName("sf", "demo", WMTSStoreInfo.class);
        assertNotNull(store);

        LayerInfo layer = catalog.getLayerByName("sf:" + LAYER_NAME);
        assertNotNull(layer);
        assertNotNull(layer.getResource());
        assertTrue(layer.getResource() instanceof WMTSLayerInfo);

        logout();
    }

    @Test
    public void testWmsGetCapabilites() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        Document dom = getAsDOM("wms?request=GetCapabilities");
        //        print(dom);
        assertEquals("WMS_Capabilities", dom.getDocumentElement().getNodeName());

        assertXpathEvaluatesTo("30", "count(//*[local-name()='Layer'])", dom);
        assertXpathEvaluatesTo(
                "4", "count(//*[local-name()='Layer']/*[local-name()='Name' and starts-with(text(), 'sf:')])", dom);
        assertXpathEvaluatesTo(
                "1", "count(//*[local-name()='Layer']/*[local-name()='Name' and text()='sf:" + LAYER_NAME + "'])", dom);
    }
}
