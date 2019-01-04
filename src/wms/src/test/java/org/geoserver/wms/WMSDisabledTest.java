/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.w3c.dom.Document;

public class WMSDisabledTest extends WMSTestSupport {

    @Test
    public void testDisabledServiceResponse() throws Exception {
        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setEnabled(false);
        getGeoServer().save(wms);

        Document doc = getAsDOM("wms?service=WMS&version=1.1.1&request=getCapabilities");
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testEnabledServiceResponse() throws Exception {
        enableWMS();

        Document doc = getAsDOM("wms?service=WMS&version=1.1.1&request=getCapabilities");
        assertEquals("WMT_MS_Capabilities", doc.getDocumentElement().getNodeName());
    }

    /** Tests WMS service disabled on layer-resource */
    @Test
    public void testLayerDisabledServiceResponse() throws Exception {
        enableWMS();
        String layerName = "cite:RoadSegments";
        FeatureTypeInfo ftinfo = getCatalog().getFeatureTypeByName(layerName);
        ftinfo.setServiceConfiguration(true);
        ftinfo.setDisabledServices(new ArrayList<>(Arrays.asList("WMS")));
        getCatalog().save(ftinfo);
        Document doc =
                getAsDOM(
                        "wms?bbox="
                                + "-180,-90,180,90"
                                + "&layers="
                                + layerName
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());
    }

    /** Tests WMS service enabled on layer-resource */
    @Test
    public void testLayerEnabledServiceResponse() throws Exception {
        enableWMS();
        String layerName = "cite:RoadSegments";
        FeatureTypeInfo ri = getCatalog().getFeatureTypeByName(layerName);
        ri.setServiceConfiguration(false);
        ri.setDisabledServices(new ArrayList<>());
        getCatalog().save(ri);
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + "-180,-90,180,90"
                                + "&layers="
                                + layerName
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326",
                        "image/png");
        assertNotNull(image);
    }

    private void enableWMS() {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setEnabled(true);
        getGeoServer().save(wms);
    }
}
