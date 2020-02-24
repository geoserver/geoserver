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
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.data.test.CiteTestData;
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
        roadsWmsLayer.setPreferredFormat("image/jpeg");
        roadsWmsLayer.setForcedRemoteStyle("line1");

        getCatalog().add(roadsWmsLayer);
        LayerInfo wmsRaodsLayer = cb.buildLayer(roadsWmsLayer);
        getCatalog().add(wmsRaodsLayer);

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

        // mock JSON Legend calls
        String jsonResponse =
                "{\"Legend\": [{\n"
                        + "  \"layerName\": \"roads22\",\n"
                        + "  \"title\": \"roads\",\n"
                        + "  \"rules\": [  {\n"
                        + "    \"name\": \"Rule 1\",\n"
                        + "    \"title\": \"Green Line\",\n"
                        + "    \"abstract\": \"A green line with a 2 pixel width\",\n"
                        + "    \"symbolizers\": [{\"Line\":     {\n"
                        + "      \"stroke\": \"#0000FF\",\n"
                        + "      \"stroke-width\": 1,\n"
                        + "      \"stroke-opacity\": \"1\",\n"
                        + "      \"stroke-linecap\": \"butt\",\n"
                        + "      \"stroke-linejoin\": \"miter\"\n"
                        + "    }}]\n"
                        + "  }]\n"
                        + "}]}";

        // this url is coming from caps111.xml file
        String mockCascadedJSONUrl =
                wms13BaseURL
                        + "?REQUEST=GetLegendGraphic&LAYER=roads_wms_130&FORMAT=application/json"
                        + "&VERSION=1.3.0&SERVICE=WMS";
        wms13Client.expectGet(
                new URL(mockCascadedJSONUrl),
                new MockHttpResponse(jsonResponse, "application/json"));

        // styleless roads layer
        WMSLayerInfo styleLessroadsWmsLayer = cb.buildWMSLayer("roads_styleless_130");
        styleLessroadsWmsLayer.setName("roads_styleless_130");
        styleLessroadsWmsLayer.reset();
        getCatalog().add(styleLessroadsWmsLayer);
        LayerInfo styleLessroadsWmsLayerLayer = cb.buildLayer(styleLessroadsWmsLayer);

        getCatalog().add(styleLessroadsWmsLayerLayer);

        // make a layer group of roads_wms and styleless_roads
        LayerGroupInfo roadsGroup = getCatalog().getFactory().createLayerGroup();

        roadsGroup.setName("roads_group_130");
        roadsGroup.getLayers().add(wmsRaodsLayer);
        roadsGroup.getLayers().add(styleLessroadsWmsLayerLayer);

        try {
            cb.calculateLayerGroupBounds(roadsGroup);
        } catch (Exception e) {
            throw new IOException(e);
        }

        getCatalog().add(roadsGroup);

        // setup mock URL
        String mockLayergroupRequest =
                wms13BaseURL
                        + "?SERVICE=WMS&LAYERS=roads_wms_130,roads_styleless_130"
                        + "&CRS=EPSG:26713&FORMAT=image%2Fpng&HEIGHT=537&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF"
                        + "&REQUEST=GetMap&BBOX=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461&WIDTH=768"
                        + "&STYLES=line1,&VERSION=1.3.0";
        wms13Client.expectGet(
                new URL(mockLayergroupRequest), new MockHttpResponse(pngRoadsImage, "image/png"));
        // SET UP layer group of two cascaded wms layers
        WMSLayerInfo group_lyr_1 = cb.buildWMSLayer("group_lyr_130");
        group_lyr_1.setName("group_lyr_130");
        group_lyr_1.reset();
        group_lyr_1.setMetadataBBoxRespected(true);

        getCatalog().add(group_lyr_1);
        LayerInfo layer1 = cb.buildLayer(group_lyr_1);
        getCatalog().add(layer1);

        WMSLayerInfo group_lyr_2 = cb.buildWMSLayer("group_lyr_230");
        group_lyr_2.setName("group_lyr_230");
        group_lyr_2.reset();
        group_lyr_2.setMetadataBBoxRespected(true);
        getCatalog().add(group_lyr_2);
        LayerInfo layer2 = cb.buildLayer(group_lyr_2);
        getCatalog().add(layer2);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();

        group.setName("cascaded_group_130");
        group.getLayers().add(layer1);
        group.getLayers().add(layer2);
        try {
            cb.calculateLayerGroupBounds(group);
        } catch (Exception e) {
            throw new IOException(e);
        }
        getCatalog().add(group);

        // we dont care about image response, the URL should have correct number of layers
        String mockBothLayerUrl =
                wms13BaseURL
                        + "?SERVICE=WMS&LAYERS=group_lyr_130,group_lyr_230&CRS=EPSG:4326&FORMAT=image%2Fpng&HEIGHT=537&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF&REQUEST=GetMap&BBOX=0.0,0.0,20.0,20.0&WIDTH=768&STYLES=,&VERSION=1.3.0";
        wms13Client.expectGet(
                new URL(mockBothLayerUrl), new MockHttpResponse(pngRoadsImage, "image/png"));

        String mockSingleLayerUrl =
                wms13BaseURL
                        + "?SERVICE=WMS&LAYERS=group_lyr_130&CRS=EPSG:4326&FORMAT=image%2Fpng&HEIGHT=537&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF&REQUEST=GetMap&BBOX=-10.0,0.0,-5.0,5.0&WIDTH=768&STYLES=&VERSION=1.3.0";
        wms13Client.expectGet(
                new URL(mockSingleLayerUrl), new MockHttpResponse(pngRoadsImage, "image/png"));

        String mockURLWithSingleLayerInsideBounds =
                wms13BaseURL
                        + "?SERVICE=WMS&LAYERS=group_lyr_230&CRS=EPSG:4326&FORMAT=image%2Fpng&HEIGHT=537&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF&REQUEST=GetMap&BBOX=0.0,0.0,20.0,20.0&WIDTH=768&STYLES=&VERSION=1.3.0";
        wms13Client.expectGet(
                new URL(mockURLWithSingleLayerInsideBounds),
                new MockHttpResponse(pngRoadsImage, "image/png"));

        WMSLayerInfo legacy_group_lyr = cb.buildWMSLayer("legacy_group_lyr_130");
        legacy_group_lyr.setName("legacy_group_lyr_130");
        getCatalog().add(legacy_group_lyr);

        LayerInfoImpl legacy_lyr = (LayerInfoImpl) cb.buildLayer(legacy_group_lyr);
        StyleInfo defaultRaster = getCatalog().getStyleByName(CiteTestData.DEFAULT_RASTER_STYLE);

        legacy_lyr.setDefaultStyle(defaultRaster);
        getCatalog().add(legacy_lyr);
        LayerGroupInfo legacyCascadedGroup = getCatalog().getFactory().createLayerGroup();

        legacyCascadedGroup.setName("cascaded_legacy_group_130");
        legacyCascadedGroup.getLayers().add(legacy_lyr);

        try {
            cb.calculateLayerGroupBounds(legacyCascadedGroup);
        } catch (Exception e) {
            throw new IOException(e);
        }
        getCatalog().add(legacyCascadedGroup);

        String mockURLWithLegacyLayers =
                wms13BaseURL
                        + "?SERVICE=WMS&LAYERS=legacy_group_lyr_130&CRS=EPSG:4326&FORMAT=image/png&HEIGHT=90&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF&REQUEST=GetMap&BBOX=-90.0,-180.0,90.0,180.0&WIDTH=180&STYLES=&VERSION=1.3.0";
        // we dont care about response, URL content is important
        wms13Client.expectGet(
                new URL(mockURLWithLegacyLayers), new MockHttpResponse(pngRoadsImage, "image/png"));
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
        // setting up mock get feature info response
        URL featureInfo = WMSTestSupport.class.getResource("wms-features.xml");
        wms11Client.expectGet(
                new URL(
                        wms11BaseURL
                                + "?Y=50&X=50"
                                + "&SERVICE=WMS"
                                + "&INFO_FORMAT=application/vnd.ogc.gml"
                                + "&LAYERS=world4326"
                                + "&FEATURE_COUNT=50"
                                + "&FORMAT=image%2Fpng"
                                + "&HEIGHT=101&TRANSPARENT=TRUE"
                                + "&REQUEST=GetFeatureInfo"
                                + "&WIDTH=101"
                                + "&BBOX=-103.829117187,44.3898919295,-103.804563429,44.4069939679"
                                + "&STYLES=&SRS=EPSG:4326&QUERY_LAYERS=world4326&VERSION=1.1.1"),
                new MockHttpResponse(featureInfo, "application/vnd.ogc.gml"));

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
        roadsWmsLayer.setForcedRemoteStyle("line1");
        getCatalog().add(roadsWmsLayer);
        LayerInfo wmsRoadsLayer = cb.buildLayer(roadsWmsLayer);

        getCatalog().add(wmsRoadsLayer);

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

        // mock JSON Legend calls
        String jsonResponse =
                "{\"Legend\": [{\n"
                        + "  \"layerName\": \"roads22\",\n"
                        + "  \"title\": \"roads\",\n"
                        + "  \"rules\": [  {\n"
                        + "    \"name\": \"Rule 1\",\n"
                        + "    \"title\": \"Green Line\",\n"
                        + "    \"abstract\": \"A green line with a 2 pixel width\",\n"
                        + "    \"symbolizers\": [{\"Line\":     {\n"
                        + "      \"stroke\": \"#0000FF\",\n"
                        + "      \"stroke-width\": 1,\n"
                        + "      \"stroke-opacity\": \"1\",\n"
                        + "      \"stroke-linecap\": \"butt\",\n"
                        + "      \"stroke-linejoin\": \"miter\"\n"
                        + "    }}]\n"
                        + "  }]\n"
                        + "}]}";

        // this url is coming from caps111.xml file
        String mockCascadedJSONUrl =
                wms11BaseURL
                        + "?REQUEST=GetLegendGraphic&LAYER=roads_wms&FORMAT=application/json&VERSION=1.0.0&SERVICE=WMS";
        wms11Client.expectGet(
                new URL(mockCascadedJSONUrl),
                new MockHttpResponse(jsonResponse, "application/json"));
        // styleless roads layer
        WMSLayerInfo styleLessroadsWmsLayer = cb.buildWMSLayer("styleless_roads");
        styleLessroadsWmsLayer.setName("styleless_roads");
        styleLessroadsWmsLayer.reset();
        getCatalog().add(styleLessroadsWmsLayer);
        LayerInfo styleLessroadsWmsLayerLayer = cb.buildLayer(styleLessroadsWmsLayer);

        getCatalog().add(styleLessroadsWmsLayerLayer);

        // make a layer group of roads_wms and styleless_roads
        LayerGroupInfo roadsGroup = getCatalog().getFactory().createLayerGroup();

        roadsGroup.setName("roads_group");
        roadsGroup.getLayers().add(wmsRoadsLayer);
        roadsGroup.getLayers().add(styleLessroadsWmsLayerLayer);

        try {
            cb.calculateLayerGroupBounds(roadsGroup);
        } catch (Exception e) {
            throw new IOException(e);
        }

        getCatalog().add(roadsGroup);

        String mockLayerGroupRequest =
                wms11BaseURL
                        + "?SERVICE=WMS&LAYERS=roads_wms,styleless_roads"
                        + "&FORMAT=image%2Fpng&HEIGHT=537&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF&REQUEST=GetMap"
                        + "&BBOX=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&WIDTH=768&STYLES=line1,&SRS=EPSG:26713&VERSION=1.1.1";
        wms11Client.expectGet(
                new URL(mockLayerGroupRequest), new MockHttpResponse(pngRoadsImage, "image/png"));

        // SET UP layer group of two cascaded wms layers
        WMSLayerInfo group_lyr_1 = cb.buildWMSLayer("group_lyr_1");
        group_lyr_1.setName("group_lyr_1");
        group_lyr_1.reset();
        group_lyr_1.setMetadataBBoxRespected(true);
        getCatalog().add(group_lyr_1);
        LayerInfo layer1 = cb.buildLayer(group_lyr_1);
        getCatalog().add(layer1);

        WMSLayerInfo group_lyr_2 = cb.buildWMSLayer("group_lyr_2");
        group_lyr_2.setName("group_lyr_2");
        group_lyr_2.reset();
        group_lyr_2.setMetadataBBoxRespected(true);
        getCatalog().add(group_lyr_2);
        LayerInfo layer2 = cb.buildLayer(group_lyr_2);
        getCatalog().add(layer2);

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();

        // group.setId("casc_group");
        group.setName("cascaded_group");
        group.getLayers().add(layer1);
        group.getLayers().add(layer2);
        try {
            cb.calculateLayerGroupBounds(group);
        } catch (Exception e) {
            throw new IOException(e);
        }
        getCatalog().add(group);

        // setting up mock requests
        String mockURLWithBothLayers =
                wms11BaseURL
                        + "?SERVICE=WMS&LAYERS=group_lyr_1,group_lyr_2&FORMAT=image%2Fpng&HEIGHT=537&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF&REQUEST=GetMap&BBOX=0.0,0.0,20.0,20.0&WIDTH=768&STYLES=,&SRS=EPSG:4326&VERSION=1.1.1";
        // we dont care about response, URL content is important
        wms11Client.expectGet(
                new URL(mockURLWithBothLayers), new MockHttpResponse(pngRoadsImage, "image/png"));

        String mockURLWithSingleLayer =
                wms11BaseURL
                        + "?SERVICE=WMS&LAYERS=group_lyr_1&FORMAT=image%2Fpng&HEIGHT=537&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF&REQUEST=GetMap&BBOX=-10.0,0.0,-5.0,5.0&WIDTH=768&STYLES=&SRS=EPSG:4326&VERSION=1.1.1";
        // we dont care about response, URL content is important
        wms11Client.expectGet(
                new URL(mockURLWithSingleLayer), new MockHttpResponse(pngRoadsImage, "image/png"));
        String mockURLWithSingleLayerInsideBounds =
                wms11BaseURL
                        + "?SERVICE=WMS&LAYERS=group_lyr_2&FORMAT=image%2Fpng&HEIGHT=537&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF&REQUEST=GetMap&BBOX=0.0,0.0,20.0,20.0&WIDTH=768&STYLES=&SRS=EPSG:4326&VERSION=1.1.1";
        wms11Client.expectGet(
                new URL(mockURLWithSingleLayerInsideBounds),
                new MockHttpResponse(pngRoadsImage, "image/png"));

        WMSLayerInfo legacy_group_lyr = cb.buildWMSLayer("legacy_group_lyr");
        legacy_group_lyr.setName("legacy_group_lyr");
        getCatalog().add(legacy_group_lyr);

        LayerInfoImpl legacy_lyr = (LayerInfoImpl) cb.buildLayer(legacy_group_lyr);
        StyleInfo defaultRaster = getCatalog().getStyleByName(CiteTestData.DEFAULT_RASTER_STYLE);

        legacy_lyr.setDefaultStyle(defaultRaster);
        getCatalog().add(legacy_lyr);
        LayerGroupInfo legacyCascadedGroup = getCatalog().getFactory().createLayerGroup();

        legacyCascadedGroup.setName("cascaded_legacy_group");
        legacyCascadedGroup.getLayers().add(legacy_lyr);

        try {
            cb.calculateLayerGroupBounds(legacyCascadedGroup);
        } catch (Exception e) {
            throw new IOException(e);
        }
        getCatalog().add(legacyCascadedGroup);

        String mockURLWithLegacyLayers =
                wms11BaseURL
                        + "?SERVICE=WMS&LAYERS=legacy_group_lyr&FORMAT=image%2Fpng&HEIGHT=90&TRANSPARENT=FALSE&BGCOLOR=0xFFFFFF&REQUEST=GetMap&BBOX=-180.0,-90.0,180.0,90.0&WIDTH=180&STYLES=&SRS=EPSG:4326&VERSION=1.1.1";
        // we dont care about response, URL content is important
        wms11Client.expectGet(
                new URL(mockURLWithLegacyLayers), new MockHttpResponse(pngRoadsImage, "image/png"));
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
