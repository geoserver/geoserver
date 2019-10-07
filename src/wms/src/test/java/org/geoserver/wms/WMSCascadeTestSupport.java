/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.junit.Before;

/**
 * Base class for WMS cascading tests
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class WMSCascadeTestSupport extends WMSTestSupport {

    protected static final String WORLD4326_130 = "world4326_130";
    protected static final String WORLD4326_110 = "world4326_110";
    protected static final String WORLD4326_110_NFI = "world4326_110_NFI";
    protected MockHttpClient wms13Client;
    protected URL wms13BaseURL;
    protected MockHttpClient wms11Client;
    protected URL wms11BaseURL;
    protected MockHttpClient wms11ClientNfi;
    protected URL wms11BaseNfiURL;
    protected XpathEngine xpath;

    @Before
    public void setupXpathEngine() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // we only setup the cascaded WMS layer, so no call to super
        testData.setUpSecurity();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // we only setup the cascaded WMS layer, so no call to super
        setupWMS130Layer();
        setupWMS110Layer();
        setupWMS110NfiLayer();
    }

    private void setupWMS130Layer() throws MalformedURLException, IOException {
        // prepare the WMS 1.3 mock client
        wms13Client = new MockHttpClient();
        wms13BaseURL = new URL(TestHttpClientProvider.MOCKSERVER + "/wms13");
        URL capsDocument = WMSTestSupport.class.getResource("caps130.xml");
        wms13Client.expectGet(
                new URL(wms13BaseURL + "?service=WMS&request=GetCapabilities&version=1.3.0"),
                new MockHttpResponse(capsDocument, "text/xml"));
        URL pngImage = WMSTestSupport.class.getResource("world.png");
        // we expect a getmap request with flipped coordinates
        wms13Client.expectGet(
                new URL(
                        wms13BaseURL
                                + "?service=WMS&version=1.3.0&request=GetMap&layers=world4326"
                                + "&styles&bbox=-90.0,-180.0,90.0,180.0&crs=EPSG:4326&bgcolor=0xFFFFFF&transparent=FALSE&format=image/png&width=180&height=90"),
                new MockHttpResponse(pngImage, "image/png"));

        String caps = wms13BaseURL + "?service=WMS&request=GetCapabilities&version=1.3.0";
        TestHttpClientProvider.bind(wms13Client, caps);

        // setup the WMS layer
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        WMSStoreInfo store = cb.buildWMSStore("mock-wms-store-130");
        getCatalog().add(store);
        cb.setStore(store);
        store.setCapabilitiesURL(caps);
        WMSLayerInfo wmsLayer = cb.buildWMSLayer("world4326");
        wmsLayer.setName(WORLD4326_130);
        getCatalog().add(wmsLayer);
        LayerInfo gsLayer = cb.buildLayer(wmsLayer);
        getCatalog().add(gsLayer);

        // roads layer
        WMSLayerInfo roadsWmsLayer = cb.buildWMSLayer("roads_wms_130");
        roadsWmsLayer.setName("roads_wms_130");
        roadsWmsLayer.reset();
        roadsWmsLayer.setPrefferedFormat("image/jpeg");
        getCatalog().add(roadsWmsLayer);
        LayerInfo wmsRaodsLayer = cb.buildLayer(roadsWmsLayer);
        getCatalog().add(wmsRaodsLayer);

        // http://mock.test.geoserver.org/wms13
        String mockPNGUrl =
                wms13BaseURL
                        + "?&SERVICE=WMS&LAYERS=roads_wms_130&CRS=EPSG:26713"
                        + "&FORMAT=image%2Fpng&HEIGHT=90&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF"
                        + "&REQUEST=GetMap&BBOX=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&WIDTH=180&STYLES=line1&VERSION=1.3.0";
        String mockJpegUrl =
                wms13BaseURL
                        + "?&SERVICE=WMS&LAYERS=roads_wms_130&CRS=EPSG:26713"
                        + "&FORMAT=image%2Fjpeg&HEIGHT=90&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF"
                        + "&REQUEST=GetMap&BBOX=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&WIDTH=180&STYLES=line1&VERSION=1.3.0";

        URL pngRoadsImage = WMSTestSupport.class.getResource("roads_wms.png");
        URL gifRoadsImage = WMSTestSupport.class.getResource("roads_wms.gif");

        wms13Client.expectGet(
                new URL(mockPNGUrl), new MockHttpResponse(pngRoadsImage, "image/png"));
        wms13Client.expectGet(
                new URL(mockJpegUrl), new MockHttpResponse(gifRoadsImage, "image/gif"));
    }

    private void setupWMS110Layer() throws MalformedURLException, IOException {
        // prepare the WMS 1.1 mock client
        wms11Client = new MockHttpClient();
        wms11BaseURL = new URL(TestHttpClientProvider.MOCKSERVER + "/wms11");
        URL capsDocument = WMSTestSupport.class.getResource("caps111.xml");
        wms11Client.expectGet(
                new URL(wms11BaseURL + "?service=WMS&request=GetCapabilities&version=1.1.1"),
                new MockHttpResponse(capsDocument, "text/xml"));
        URL pngImage = WMSTestSupport.class.getResource("world.png");
        // we expect a getmap request with flipped coordinates
        wms11Client.expectGet(
                new URL(
                        wms11BaseURL
                                + "?service=WMS&version=1.1.1&request=GetMap&layers=world4326"
                                + "&styles&bbox=-180.0,-90.0,180.0,90.0&srs=EPSG:4326&bgcolor=0xFFFFFF&transparent=FALSE&format=image/png&width=180&height=90"),
                new MockHttpResponse(pngImage, "image/png"));

        String caps = wms11BaseURL + "?service=WMS&request=GetCapabilities&version=1.1.1";
        TestHttpClientProvider.bind(wms11Client, caps);

        // setup the WMS layer
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        WMSStoreInfo store = cb.buildWMSStore("mock-wms-store-110");
        getCatalog().add(store);
        cb.setStore(store);
        store.setCapabilitiesURL(caps);
        WMSLayerInfo wmsLayer = cb.buildWMSLayer("world4326");
        wmsLayer.setName(WORLD4326_110);
        getCatalog().add(wmsLayer);
        LayerInfo gsLayer = cb.buildLayer(wmsLayer);
        getCatalog().add(gsLayer);

        // roads layer
        WMSLayerInfo roadsWmsLayer = cb.buildWMSLayer("roads_wms");
        roadsWmsLayer.setName("roads_wms");
        roadsWmsLayer.reset();
        roadsWmsLayer.setPrefferedFormat("image/jpeg");
        getCatalog().add(roadsWmsLayer);
        LayerInfo wmsRaodsLayer = cb.buildLayer(roadsWmsLayer);

        getCatalog().add(wmsRaodsLayer);

        // setting up mock response

        String mockPNGUrl =
                wms11BaseURL
                        + "?SERVICE=WMS&LAYERS=roads_wms&FORMAT=image%2Fpng"
                        + "&HEIGHT=537&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF"
                        + "&REQUEST=GetMap&BBOX=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&WIDTH=768&STYLES=line1&SRS=EPSG:26713&VERSION=1.1.1";
        String mockJpegUrl =
                wms11BaseURL
                        + "?SERVICE=WMS&LAYERS=roads_wms&FORMAT=image%2Fjpeg"
                        + "&HEIGHT=537&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF"
                        + "&REQUEST=GetMap&BBOX=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&WIDTH=768&STYLES=line1&SRS=EPSG:26713&VERSION=1.1.1";

        URL pngRoadsImage = WMSTestSupport.class.getResource("roads_wms.png");
        URL gifRoadsImage = WMSTestSupport.class.getResource("roads_wms.gif");

        wms11Client.expectGet(
                new URL(mockPNGUrl), new MockHttpResponse(pngRoadsImage, "image/png"));
        wms11Client.expectGet(
                new URL(mockJpegUrl), new MockHttpResponse(gifRoadsImage, "image/gif"));
    }

    private void setupWMS110NfiLayer() throws MalformedURLException, IOException {
        // prepare the WMS 1.1 mock client
        wms11ClientNfi = new MockHttpClient();
        wms11BaseNfiURL = new URL(TestHttpClientProvider.MOCKSERVER + "/wms11_nfi");
        URL capsDocument = WMSTestSupport.class.getResource("caps111_no_feature_info.xml");
        wms11ClientNfi.expectGet(
                new URL(wms11BaseNfiURL + "?service=WMS&request=GetCapabilities&version=1.1.1"),
                new MockHttpResponse(capsDocument, "text/xml"));

        String caps = wms11BaseNfiURL + "?service=WMS&request=GetCapabilities&version=1.1.1";
        TestHttpClientProvider.bind(wms11ClientNfi, caps);

        // setup the WMS layer
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        WMSStoreInfo store = cb.buildWMSStore("mock-wms-store-110-nfi");
        getCatalog().add(store);
        cb.setStore(store);
        store.setCapabilitiesURL(caps);
        WMSLayerInfo wmsLayer = cb.buildWMSLayer("world4326");
        wmsLayer.setName(WORLD4326_110_NFI);
        getCatalog().add(wmsLayer);
        LayerInfo gsLayer = cb.buildLayer(wmsLayer);
        getCatalog().add(gsLayer);
    }
}
