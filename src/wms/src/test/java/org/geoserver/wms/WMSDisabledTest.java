/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.util.Version;
import org.junit.Test;
import org.w3c.dom.Document;

public class WMSDisabledTest extends WMSTestSupport {

    /** Tests that a disabled version returns an exception */
    @Test
    public void testDisabledVersionReturnsException() throws Exception {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setEnabled(true);
        wms.getDisabledVersions().clear();
        wms.getDisabledVersions().add(new Version("1.3.0"));
        getGeoServer().save(wms);

        // request disabled version -> should fail
        Document doc = getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities");
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());

        wms.getDisabledVersions().clear();
        getGeoServer().save(wms);
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
        Document doc = getAsDOM("wms?bbox="
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

    /** Tests that an enabled version still works when another version is disabled */
    @Test
    public void testEnabledVersionStillWorks() throws Exception {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setEnabled(true);
        wms.getDisabledVersions().clear();
        wms.getDisabledVersions().add(new Version("1.1.1"));
        getGeoServer().save(wms);

        // request enabled version -> should succeed
        Document doc = getAsDOM("wms?service=WMS&version=1.3.0&request=GetCapabilities");
        assertEquals("WMS_Capabilities", doc.getDocumentElement().getNodeName());

        wms.getDisabledVersions().clear();
        getGeoServer().save(wms);
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
        BufferedImage image = getAsImage(
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
