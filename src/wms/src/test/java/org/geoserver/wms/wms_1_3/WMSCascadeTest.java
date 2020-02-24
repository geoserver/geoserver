/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.apache.http.HttpStatus;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSCascadeTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
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
        // make sure GetFeatureInfo is not deactivated (this will only update the global service)
        wms.setFeaturesReprojectionDisabled(false);
        gs.save(wms);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // on WMS 1.3 the requested area is enlarged to account for reprojection
        // this is not really needed, it's something we should optimize out.
        // See GEOS-5837 and remove these when it is fixed
        URL pngImage = WMSTestSupport.class.getResource("world.png");
        wms13Client.expectGet(
                new URL(
                        wms13BaseURL
                                + "?service=WMS&version=1.3.0&request=GetMap&layers=world4326"
                                + "&styles&bbox=-110.0,-200.0,110.0,200.0&crs=EPSG:4326&bgcolor=0xFFFFFF&transparent=FALSE&format=image/png&width=190&height=100"),
                new MockHttpResponse(pngImage, "image/png"));
        wms11Client.expectGet(
                new URL(
                        wms11BaseURL
                                + "?service=WMS&version=1.1.1&request=GetMap&layers=world4326"
                                + "&styles&bbox=-200.0,-110.0,200.0,110.0&srs=EPSG:4326&bgcolor=0xFFFFFF&transparent=FALSE&format=image/png&width=190&height=100"),
                new MockHttpResponse(pngImage, "image/png"));

        // setup mocked get feature info (the return features use EPSG:3857)
        URL featureInfo = WMSTestSupport.class.getResource("wms-features.xml");
        wms13Client.expectGet(
                new URL(
                        wms13BaseURL
                                + "?SERVICE=WMS&INFO_FORMAT=application/vnd.ogc.gml&LAYERS=world4326"
                                + "&CRS=EPSG:4326&FEATURE_COUNT=50&FORMAT=image%2Fpng&HEIGHT=101&TRANSPARENT=TRUE&J=-609621&REQUEST=GetFeatureInfo"
                                + "&I=-875268&WIDTH=101&BBOX=-103.829117187,44.3898919295,-103.804563429,44.4069939679&STYLES=&QUERY_LAYERS=world4326&VERSION=1.3.0"),
                new MockHttpResponse(featureInfo, "application/vnd.ogc.gml"));
    }

    @Test
    public void testCascadeGetMapOnto13() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox=-90,-180,90,180"
                                + "&styles=&layers="
                                + WORLD4326_130
                                + "&Format=image/png&request=GetMap&version=1.3.0&service=wms"
                                + "&width=180&height=90&crs=EPSG:4326");
        // we'll get a service exception if the requests are not the ones expected
        checkImage(response, "image/png", 180, 90);
    }

    @Test
    public void testCascadeGetMapOnto11() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?bbox=-90,-180,90,180"
                                + "&styles=&layers="
                                + WORLD4326_110
                                + "&Format=image/png&request=GetMap&version=1.3.0&service=wms"
                                + "&width=180&height=90&crs=EPSG:4326");
        // we'll get a service exception if the requests are not the ones expected
        checkImage(response, "image/png", 180, 90);
    }

    @Test
    public void testCascadeCapabilitiesClientNoGetFeatureInfo() throws Exception {
        Document dom = getAsDOM("wms?request=GetCapabilities&version=1.3.0&service=wms");
        // print(dom);

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("link", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        NamespaceContext newNsCtxt = new SimpleNamespaceContext(namespaces);

        xpath.setNamespaceContext(newNsCtxt);

        xpath.evaluate("//wms:Layer[name='" + WORLD4326_110_NFI + "']", dom);
    }

    @Test
    public void testGetFeatureInfoReprojection() throws Exception {
        // do the get feature request using EPSG:4326
        String url =
                "wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetFeatureInfo&FORMAT=image/png&TRANSPARENT=true"
                        + "&QUERY_LAYERS="
                        + WORLD4326_130
                        + "&STYLES&LAYERS="
                        + WORLD4326_130
                        + "&INFO_FORMAT=text/xml; subtype=gml/3.1.1"
                        + "&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG:4326&WIDTH=101&HEIGHT=101&BBOX=-103.829117187,44.3898919295,-103.804563429,44.4069939679";
        Document result = getAsDOM(url);
        // setup XPATH engine namespaces
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("gs", "http://geoserver.org");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        xpath.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        // check the response content, the features should have been reproject from EPSG:3857 to
        // EPSG:4326
        String srs =
                xpath.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "gs:world4326_130[@gml:id='bugsites.55']/gs:the_geom/gml:Point/@srsName",
                        result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("4326"), is(true));
        String rawCoordinates =
                xpath.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "gs:world4326_130[@gml:id='bugsites.55']/gs:the_geom/gml:Point/gml:pos/text()",
                        result);
        assertThat(rawCoordinates, notNullValue());
        String[] coordinates = rawCoordinates.split(" ");
        assertThat(coordinates.length, is(2));
        checkNumberSimilar(coordinates[0], 44.39832008, 0.0001);
        checkNumberSimilar(coordinates[1], -103.81711048, 0.0001);
        // deactivate features reprojection
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(true);
        getGeoServer().save(wms);
        // execute the get feature info request
        result = getAsDOM(url);
        srs =
                xpath.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "gs:world4326_130[@gml:id='bugsites.55']/gs:the_geom/gml:Point/@srsName",
                        result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("3857"), is(true));
        rawCoordinates =
                xpath.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "gs:world4326_130[@gml:id='bugsites.55']/gs:the_geom/gml:Point/gml:pos/text()",
                        result);
        assertThat(rawCoordinates, notNullValue());
        coordinates = rawCoordinates.split(" ");
        assertThat(coordinates.length, is(2));
        checkNumberSimilar(coordinates[0], -11556867.874, 0.0001);
        checkNumberSimilar(coordinates[1], 5527291.47718493, 0.0001);
    }

    @Test
    public void testCascadedSettings() throws Exception {

        LayerInfo info = getCatalog().getLayerByName("roads_wms_130");

        info.toString();

        String getMapRequest =
                "wms?bbox=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&styles=line1&layers="
                        + info.getName()
                        + "&Format=image/png"
                        + "&request=GetMap&version=1.3.0&service=wms"
                        + "&width=180&height=90&crs=EPSG:26713";

        // the request should generate exepected remote WMS URL
        // e.g default remote style, correct image format
        BufferedImage response = getAsImage(getMapRequest, "image/png");
        assertNotNull(response);

        // below request should force geoserver to request in default format
        String getMapUnsupportedRequest =
                "wms?bbox=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&styles=line1&layers="
                        + info.getName()
                        + "&Format=image/gif"
                        + "&request=GetMap&version=1.3.0&service=wms"
                        + "&width=180&height=90&crs=EPSG:26713";

        // the request should generate exepected remote WMS URL
        // e.g default forced remote style and jpeg format in remote style
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
                        "wms?service=WMS&version=1.3.0&request=GetLegendGraphic"
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

        JSON dom =
                getAsJSON(
                        "wms?service=WMS&version=1.3.0&request=GetLegendGraphic"
                                + "&layer=roads_wms_130"
                                + "&format=application/json",
                        HttpStatus.SC_OK);

        JSONObject responseJson = JSONObject.fromObject(dom.toString());
        assertFalse(responseJson.isEmpty());
    }

    @Test
    public void testCascadeLayerGroup() throws Exception {

        String getMapRequest =
                "wms?service=WMS&version=1.3.0"
                        + "&request=GetMap"
                        + "&layers=roads_group_130"
                        + "&bbox=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&width=768&height=537&srs=EPSG:26713&Format=image/png";

        // the request should generate exepected remote WMS URL
        // e.g default remote styles should include the forced remote style of one layer
        // and empty for second layer
        // For Mock URL check WMSCascadeTestSupport.setupWMS130Layer()
        BufferedImage response = getAsImage(getMapRequest, "image/png");
        assertNotNull(response);
    }

    @Test
    public void testLegacyCascadeLayerGroup() throws Exception {

        String getMapRequest =
                "wms?bbox=-90,-180,90,180"
                        + "&styles=&layers=cascaded_legacy_group_130"
                        + "&Format=image/png&request=GetMap&version=1.3.0&service=wms"
                        + "&width=180&height=90&crs=EPSG:4326";

        // the request should generate exepected remote WMS URL
        // e.g default remote styles should be empty in remote request
        // For Mock URL check WMSCascadeTestSupport.setupWMS130Layer()
        BufferedImage response = getAsImage(getMapRequest, "image/png");
        assertNotNull(response);
    }

    @Test
    public void testCascadedBounds() throws Exception {
        LayerGroupInfo info = getCatalog().getLayerGroupByName("cascaded_group_130");
        LayerInfo groupLayer2 = getCatalog().getLayerByName("group_lyr_230");
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
                "wms?service=WMS&version=1.3.0"
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

        // minx,miny,maxx,maxy
        String lyrBBoxOutSideNativeBounds = "-10.0,0,-5.0,5";
        getMapRequest =
                "wms?service=WMS&version=1.3.0"
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
        LayerGroupInfo info = getCatalog().getLayerGroupByName("cascaded_group_130");
        LayerInfo groupLayer2 = getCatalog().getLayerByName("group_lyr_230");
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
                (WMSLayerInfo) getCatalog().getLayerByName("group_lyr_130").getResource();
        groupLayer1WMSResource.setMinScale(1d);
        groupLayer1WMSResource.setMaxScale(1000d);
        getCatalog().save(groupLayer1WMSResource);

        String getMapRequest =
                "wms?service=WMS&version=1.3.0"
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
                        "wms?bbox=-90,-180,90,180"
                                + "&styles=&layers="
                                + WORLD4326_110
                                + "&Format=image/png&request=GetMap&version=1.3.0&service=wms"
                                + "&width=180&height=90&crs=EPSG:4326"
                                + "&clip="
                                + rasterMask,
                        "image/png");
        ImageAssert.assertEquals(expectedImage, response, 100);
        String rasterMask900913 =
                "srid=900913;POLYGON ((-1615028.3514525702 7475148.401208023, 3844409.956787858 7475148.401208023, 3844409.956787858 3815954.983140064, -1615028.3514525702 3815954.983140064, -1615028.3514525702 7475148.401208023))";
        response =
                getAsImage(
                        "wms?bbox=-90,-180,90,180"
                                + "&styles=&layers="
                                + WORLD4326_110
                                + "&Format=image/png&request=GetMap&version=1.3.0&service=wms"
                                + "&width=180&height=90&crs=EPSG:4326"
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
