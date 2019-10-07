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
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSCascadeTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.legendgraphic.JSONLegendGraphicBuilder;
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
        wmsLayer.setPrefferedFormat("image/jpeg");

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
        assertTrue(true);

        WMSLayerInfo layerInfo =
                (WMSLayerInfo) getCatalog().getLayerByName(WORLD4326_110).getResource();
        WebMapServer webMapServer = layerInfo.getStore().getWebMapServer(null);
        // setting up OperationType
        // webMapServer.getCapabilities().getRequest().setGetLegendGraphic(new OperationType());
        // assert that webserver can make getLegend requests
        GetLegendGraphicRequest getLegend = webMapServer.createGetLegendGraphicRequest();
        assertNotNull(getLegend);

        JSON dom =
                getAsJSON(
                        "wms?service=WMS&version=1.0.0&request=GetLegendGraphic"
                                + "&layer="
                                + WORLD4326_110
                                + "&format=application/json",
                        HttpStatus.SC_OK);

        print(dom);
        JSONObject responseJson = JSONObject.fromObject(dom.toString());
        assertTrue(responseJson.has(JSONLegendGraphicBuilder.LEGEND));

        JSONArray legendArray = responseJson.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
        assertFalse(legendArray.isEmpty());

        JSONArray rulesJSONArray =
                legendArray.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
        assertFalse(rulesJSONArray.isEmpty());
    }
}
