package org.geoserver.geofence;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import org.geoserver.catalog.*;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.rest.catalog.WMTSLayerTest;
import org.geoserver.security.WMTSAccessLimits;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.w3c.dom.Document;

public class AccessManagerWMTSLayerTest extends GeofenceBaseTest {

    private static final String LAYER_NAME = "AMSR2_Snow_Water_Equivalent";

    @Rule public TestHttpClientRule clientMocker = new TestHttpClientRule();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // GeoFenceBaseTest enables the secure catalog, so we need to login first
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        // add a wmts store
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMTSStoreInfo wmts = cb.buildWMTSStore("demo");
        wmts.setCapabilitiesURL(
                clientMocker.getServer()
                        + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS");
        catalog.add(wmts);

        // and a wmts layer as well (cannot use the builder, would turn this test into an online one
        addWmtsLayer();

        // logout before running any tests
        logout();
    }

    public void addWmtsLayer() throws Exception {
        String capabilities =
                clientMocker.getServer()
                        + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS";
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
                new MockHttpResponse(
                        WMTSLayerTest.class.getResource("nasa.getcapa.xml"), "text/xml"));
        clientMocker.bind(client, capabilities);
    }

    @After
    public void removeLayer() throws Exception {
        if (IS_GEOFENCE_AVAILABLE) {
            LayerInfo l = catalog.getLayerByName(new NameImpl("sf", LAYER_NAME));
            if (l != null) {
                catalog.remove(l);
            }
        }
    }

    @Test
    public void testWmsLimited() {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("wmsuser", "wmsuser");

        // check layer in the sf workspace with a wfs request
        Request request = new Request();
        request.setService("WFS");
        request.setRequest("GetFeature");
        Dispatcher.REQUEST.set(request);

        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        LayerInfo wmtsLayer = catalog.getLayerByName("sf:" + LAYER_NAME);
        assertNotNull(wmtsLayer);
        logout();

        WMTSAccessLimits limits = (WMTSAccessLimits) accessManager.getAccessLimits(user, wmtsLayer);
        // TODO: Why is this EXCLUDE, how is it set
        assertEquals(Filter.INCLUDE, limits.getReadFilter());

        // now fake a getmap request (using a service and request with a different case than the
        // geofenceService)
        request = new Request();
        request.setService("wms");
        Dispatcher.REQUEST.set(request);
        limits = (WMTSAccessLimits) accessManager.getAccessLimits(user, wmtsLayer);
        assertEquals(Filter.INCLUDE, limits.getReadFilter());

        // Test resource as well
        limits = (WMTSAccessLimits) accessManager.getAccessLimits(user, wmtsLayer.getResource());
        assertEquals(Filter.INCLUDE, limits.getReadFilter());
    }

    @Test
    public void testGetWmtsLayer() {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

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
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }
        login("sf", "sf", "ROLE_SF_ADMIN");
        MockHttpServletResponse response = getAsServletResponse("wms?request=GetCapabilities");
        assertEquals(200, response.getStatus());
        try (InputStream inputStream = new ByteArrayInputStream(response.getContentAsByteArray())) {
            Document dom = dom(inputStream);
            assertEquals("WMS_Capabilities", dom.getDocumentElement().getNodeName());
            assertXpathEvaluatesTo("4", "count(//Layer[starts-with(Name, 'sf:')])", dom);
            assertXpathEvaluatesTo("1", "count(//Name[text()='sf:" + LAYER_NAME + "'])", dom);
        }
    }
}
