/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;
import org.geoserver.catalog.CascadedLayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSCascadeTestSupport;
import org.geoserver.wms.WMSInfo;
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

        CascadedLayerInfo info = (CascadedLayerInfo) getCatalog().getLayerByName("roads_wms");
        info.setPrefferedFormat("image/jpeg");

        String getMapRequest =
                "wms?service=WMS&version=1.1.0"
                        + "&request=GetMap"
                        + "&layers="
                        + info.getName()
                        + "&bbox=589434.85646865,4914006.33783702,609527.21021496,4928063.39801461"
                        + "&width=768&height=537&srs=EPSG:26713&Format=image/png";

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
                        + "&width=768&height=537&srs=EPSG:26713&Format=image/gif";

        // the request should generate exepected remote WMS URL
        // e.g default forced remote style
        // correct image format because gif is not part of cap doc
        // the mock client is not expecting a remote request in image/gif
        response = getAsImage(getMapUnsupportedRequest, "image/gif");
        assertNotNull(response);
    }
}
