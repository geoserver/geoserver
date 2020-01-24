/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import javax.imageio.ImageIO;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpStatus;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSCascadeTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.legendgraphic.JSONLegendGraphicBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.test.ImageAssert;
import org.geotools.ows.wms.WebMapServer;
import org.geotools.ows.wms.request.GetLegendGraphicRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

@RunWith(Parameterized.class)
public class WMSCascadeTest extends WMSCascadeTestSupport {

    private final boolean aphEnabled;

    @Parameters(name = "{index} APH enabled: {0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[] {true}, new Object[] {false});
    }

    public WMSCascadeTest(boolean aphEnabled) {
        this.aphEnabled = aphEnabled;
    }

    @Before
    public void setupAdvancedProjectionHandling() {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, aphEnabled);
        gs.save(wms);
    }

    @Test
    public void testCascadeGetMapOnto130() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox=-180,-90,180,90"
                                + "&styles=&layers="
                                + WORLD4326_130
                                + "&Format=image/png&request=GetMap"
                                + "&width=180&height=90&srs=EPSG:4326");
        // we'll get a service exception if the requests are not the ones expected
        checkImage(response, "image/png", 180, 90);
    }

    @Test
    public void testCascadeGetMapOnto110() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox=-180,-90,180,90"
                                + "&styles=&layers="
                                + WORLD4326_110
                                + "&Format=image/png&request=GetMap"
                                + "&width=180&height=90&srs=EPSG:4326");
        // we'll get a service exception if the requests are not the ones expected
        checkImage(response, "image/png", 180, 90);
    }

    @Test
    public void testCascadeCapabilitiesClientNoGetFeatureInfo() throws Exception {
        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.1.0&service=wms");
        // print(dom);

        xpath.evaluate("//Layer[name='" + WORLD4326_110_NFI + "']", dom);
    }

    @Test
    public void testCascadedSettings() throws Exception {

        LayerInfo info = getCatalog().getLayerByName("roads_wms");
        WMSLayerInfo wmsLayer = (WMSLayerInfo) info.getResource();
        wmsLayer.setPreferredFormat("image/jpeg");
        String getMapRequest =
                "wms?service=WMS&version=1.1.0"
                        + "&request=GetMap"
                        + "&layers="
                        + info.getName()
                        + "&bbox=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&width=768&height=537&srs=EPSG:26713&Format=image/png&styles=line1";

        // the request should generate exepected remote WMS URL
        // e.g default remote style, correct image format
        BufferedImage response = getAsImage(getMapRequest, "image/png");
        assertNotNull(response);

        // below request should force geoserver to request in default format
        String getMapUnsupportedRequest =
                "wms?service=WMS&version=1.1.0"
                        + "&request=GetMap"
                        + "&layers="
                        + info.getName()
                        + "&bbox=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&width=768&height=537&srs=EPSG:26713&Format=image/gif&styles=line1";

        // the request should generate exepected remote WMS URL
        // e.g default forced remote style
        // correct image format because gif is not part of cap doc
        // the mock client is not expecting a remote request in image/gif
        response = getAsImage(getMapUnsupportedRequest, "image/gif");
        assertNotNull(response);
    }

    @Test
    public void testCascadeGetLegendRequest() throws Exception {

        URL exptectedResponse = this.getClass().getResource("../cascadedLegend.png");
        URL rasterLegendresource = this.getClass().getResource("../rasterLegend.png");
        BufferedImage expected = ImageIO.read(exptectedResponse);

        WMSLayerInfo layerInfo =
                (WMSLayerInfo) getCatalog().getLayerByName(WORLD4326_110).getResource();
        WebMapServer webMapServer = layerInfo.getStore().getWebMapServer(null);
        // setting URL of local file for mock response
        webMapServer
                .getCapabilities()
                .getRequest()
                .getGetLegendGraphic()
                .setGet(rasterLegendresource);
        GetLegendGraphicRequest getLegend = webMapServer.createGetLegendGraphicRequest();

        BufferedImage image =
                getAsImage(
                        "wms?service=WMS&version=1.0.0&request=GetLegendGraphic"
                                + "&layer="
                                + WORLD4326_110
                                + "&format=image/png&width=20&height=20&transparent=true",
                        "image/png");

        assertNotNull(image);
        ImageAssert.assertEquals(expected, image, 0);
        // resizing a remotely acquired image corrupts the image
        // since the resizing occurs on image and graphics
        // cascaded get legend request should not be response image
        // because size params are already passed over to remote server
        assertNotEquals(20, image.getHeight());
        assertNotEquals(20, image.getWidth());
    }

    @Test
    public void testCascadeGetLegendRequestJSON() throws Exception {
        WMSLayerInfo layerInfo =
                (WMSLayerInfo) getCatalog().getLayerByName("roads_wms").getResource();
        WebMapServer webMapServer = layerInfo.getStore().getWebMapServer(null);
        // reset getlegend URL for this test
        webMapServer.getCapabilities().getRequest().getGetLegendGraphic().setGet(null);

        GetLegendGraphicRequest getLegend = webMapServer.createGetLegendGraphicRequest();
        assertNotNull(getLegend);
        // For Mock URL check WMSCascadeTestSupport.setupWMS110Layer()
        JSON dom =
                getAsJSON(
                        "wms?service=WMS&version=1.0.0&request=GetLegendGraphic"
                                + "&layer=roads_wms"
                                + "&format=application/json",
                        HttpStatus.SC_OK);

        JSONObject responseJson = JSONObject.fromObject(dom.toString());
        assertTrue(responseJson.has(JSONLegendGraphicBuilder.LEGEND));

        JSONArray legendArray = responseJson.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertFalse(legendArray.isEmpty());

        JSONArray rulesJSONArray =
                legendArray.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertFalse(rulesJSONArray.isEmpty());
    }

    @Test
    public void testCascadeLayerGroup() throws Exception {

        String getMapRequest =
                "wms?service=WMS&version=1.1.0"
                        + "&request=GetMap"
                        + "&layers=roads_group"
                        + "&bbox=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&width=768&height=537&srs=EPSG:26713&Format=image/png";

        // the request should generate exepected remote WMS URL
        // e.g default remote styles should include the forced remote style of one layer
        // and empty for second layer
        // For Mock URL check WMSCascadeTestSupport.setupWMS110Layer()
        BufferedImage response = getAsImage(getMapRequest, "image/png");
        assertNotNull(response);
    }

    @Test
    public void testLegacyCascadeLayerGroup() throws Exception {

        String getMapRequest =
                "wms?bbox=-180,-90,180,90"
                        + "&styles=&layers=legacy_group_lyr"
                        + "&Format=image/png&request=GetMap"
                        + "&width=180&height=90&srs=EPSG:4326";

        // the request should generate exepected remote WMS URL
        // e.g default remote styles should empty in remote request
        // For Mock URL check WMSCascadeTestSupport.setupWMS110Layer()
        BufferedImage response = getAsImage(getMapRequest, "image/png");
        assertNotNull(response);
    }

    @Test
    public void testCascadedBounds() throws Exception {
        LayerGroupInfo info = getCatalog().getLayerGroupByName("cascaded_group");
        LayerInfo groupLayer2 = getCatalog().getLayerByName("group_lyr_2");
        assertNotNull(info);
        assertNotNull(groupLayer2);
        ReferencedEnvelope request1 =
                new ReferencedEnvelope(groupLayer2.getResource().getNativeBoundingBox());
        // minx,miny,maxx,maxy
        String lyrBBox =
                request1.getMinX()
                        + ","
                        + request1.getMinY()
                        + ","
                        + request1.getMaxX()
                        + ","
                        + request1.getMaxY();

        String getMapRequest =
                "wms?service=WMS&version=1.1.0"
                        + "&request=GetMap"
                        + "&layers="
                        + info.getName()
                        + "&bbox="
                        + lyrBBox
                        + "&width=768&height=537&srs=EPSG:4326&Format=image/png";

        // should result in a request with both group layers present
        // should invoke expected Mock URL in which both layers are present
        // since the BBOX covers both

        BufferedImage response = getAsImage(getMapRequest, "image/png");
        assertNotNull(response);

        // next part of test
        // make getMap request outside group_lyr_2 but inside group_lyr_1
        // should result in a URL with single layer only

        request1 = new ReferencedEnvelope(groupLayer2.getResource().getNativeBoundingBox());

        // minx,miny,maxx,maxy
        String lyrBBoxOutSideNativeBounds = "-10.0,0,-5.0,5";
        getMapRequest =
                "wms?service=WMS&version=1.1.0"
                        + "&request=GetMap"
                        + "&layers="
                        + info.getName()
                        + "&bbox="
                        + lyrBBoxOutSideNativeBounds
                        + "&width=768&height=537&srs=EPSG:4326&Format=image/png";

        // should result in a request with single group layers present
        // should invoke expected Mock URL in which only 1 layer is present
        // since the BBOX only covers one layer
        response = getAsImage(getMapRequest, "image/png");
        assertNotNull(response);
    }

    @Test
    public void testScaleDenominator() throws Exception {
        LayerGroupInfo info = getCatalog().getLayerGroupByName("cascaded_group");
        LayerInfo groupLayer2 = getCatalog().getLayerByName("group_lyr_2");
        assertNotNull(info);
        assertNotNull(groupLayer2);
        ReferencedEnvelope request1 =
                new ReferencedEnvelope(groupLayer2.getResource().getNativeBoundingBox());
        // minx,miny,maxx,maxy
        String lyrBBox =
                request1.getMinX()
                        + ","
                        + request1.getMinY()
                        + ","
                        + request1.getMaxX()
                        + ","
                        + request1.getMaxY();

        // configure max scale on one of the layers
        // set max scale as small as possible to have it filtered
        WMSLayerInfo groupLayer1WMSResource =
                (WMSLayerInfo) getCatalog().getLayerByName("group_lyr_1").getResource();
        groupLayer1WMSResource.setMinScale(1d);
        groupLayer1WMSResource.setMaxScale(1000d);
        getCatalog().save(groupLayer1WMSResource);

        String getMapRequest =
                "wms?service=WMS&version=1.1.0"
                        + "&request=GetMap"
                        + "&layers="
                        + info.getName()
                        + "&bbox="
                        + lyrBBox
                        + "&width=768&height=537&srs=EPSG:4326&Format=image/png";

        // should result in a request with both group layers present
        // should invoke expected Mock URL in which both layers are present
        // since the BBOX covers both
        // but group_lyr_1 min/max scale is outside the bound of map scale
        try {
            BufferedImage response = getAsImage(getMapRequest, "image/png");
            assertNotNull(response);

        } finally {
            // reset
            groupLayer1WMSResource.setMinScale(null);
            groupLayer1WMSResource.setMaxScale(null);
            getCatalog().save(groupLayer1WMSResource);
        }
    }

    @Test
    public void testVendorOptionClip() throws Exception {
        URL exptectedResponse = this.getClass().getResource("../wms_clip_cascaded.png");
        BufferedImage expectedImage = ImageIO.read(exptectedResponse);
        String rasterMask =
                "POLYGON((-14.50804652396198 55.579454354599356,34.53492222603802 55.579454354599356,34.53492222603802 32.400173313532584,-14.50804652396198 32.400173313532584,-14.50804652396198 55.579454354599356))";
        BufferedImage response =
                getAsImage(
                        "wms?bbox=-180,-90,180,90"
                                + "&styles=&layers="
                                + WORLD4326_110
                                + "&Format=image/png&request=GetMap"
                                + "&width=180&height=90&srs=EPSG:4326"
                                + "&clip="
                                + rasterMask,
                        "image/png");
        ImageAssert.assertEquals(expectedImage, response, 100);
        String rasterMask900913 =
                "srid=900913;POLYGON ((-1615028.3514525702 7475148.401208023, 3844409.956787858 7475148.401208023, 3844409.956787858 3815954.983140064, -1615028.3514525702 3815954.983140064, -1615028.3514525702 7475148.401208023))";
        response =
                getAsImage(
                        "wms?bbox=-180,-90,180,90"
                                + "&styles=&layers="
                                + WORLD4326_110
                                + "&Format=image/png&request=GetMap"
                                + "&width=180&height=90&srs=EPSG:4326"
                                + "&clip="
                                + rasterMask900913,
                        "image/png");
        ImageAssert.assertEquals(expectedImage, response, 100);
    }

    @Test
    public void testGetFeatureInfoClipParam() throws Exception {
        String wkt =
                "POLYGON((-103.81422590870386 44.406335162406855,-103.81645750660425 44.39480642272217,-103.78839087147242 44.39210787899582,-103.78718924183374 44.40443430323224,-103.80598616261011 44.4091556783195,-103.81422590870386 44.406335162406855))";
        String url =
                "wms?SERVICE=WMS&VERSION=1.1.0&REQUEST=GetFeatureInfo&FORMAT=image/png&TRANSPARENT=true"
                        + "&QUERY_LAYERS="
                        + WORLD4326_110
                        + "&STYLES&LAYERS="
                        + WORLD4326_110
                        + "&INFO_FORMAT=application/json"
                        + "&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG:4326&WIDTH=101&HEIGHT=101&BBOX=-103.829117187,44.3898919295,-103.804563429,44.4069939679"
                        + "&CLIP="
                        + wkt;
        String json = getAsString(url);
        assertNotNull(json);
        // assert no features were returned
        JSONObject responseJson = JSONObject.fromObject(json);
        assertTrue(responseJson.getJSONArray("features").isEmpty());
    }
}
