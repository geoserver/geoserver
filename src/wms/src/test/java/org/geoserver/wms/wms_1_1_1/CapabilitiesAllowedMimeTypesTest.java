/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.featureinfo.TextFeatureInfoOutputFormat;
import org.geoserver.wms.map.RenderedImageMapOutputFormat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class CapabilitiesAllowedMimeTypesTest extends WMSTestSupport {

    GetMapOutputFormat getMapFormat;
    GetFeatureInfoOutputFormat getInfoFormat;

    void addMimeTypes() {
        getMapFormat = new RenderedImageMapOutputFormat(getWMS());
        getInfoFormat = new TextFeatureInfoOutputFormat(getWMS());
        WMSInfo wms = getWMS().getServiceInfo();
        wms.getGetMapMimeTypes().add(getMapFormat.getMimeType());
        wms.getGetFeatureInfoMimeTypes().add(getInfoFormat.getContentType());
        wms.setGetMapMimeTypeCheckingEnabled(true);
        wms.setGetFeatureInfoMimeTypeCheckingEnabled(true);
        getGeoServer().save(wms);
    }

    @After
    public void removeMimeTypes() {
        WMSInfo wms = getWMS().getServiceInfo();
        wms.getGetMapMimeTypes().clear();
        wms.getGetFeatureInfoMimeTypes().clear();
        wms.setGetMapMimeTypeCheckingEnabled(false);
        wms.setGetFeatureInfoMimeTypeCheckingEnabled(false);
        getGeoServer().save(wms);
    }

    @Test
    public void testAllowedMimeTypes() throws Exception {

        // check with no restrictions
        Document doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.1.1",
                        true);
        XpathEngine xpath = XMLUnit.newXpathEngine();

        NodeList formatNodes =
                xpath.getMatchingNodes(
                        "/WMT_MS_Capabilities/Capability/Request/GetMap/Format", doc);
        Assert.assertTrue(formatNodes.getLength() > 1);

        formatNodes =
                xpath.getMatchingNodes(
                        "/WMT_MS_Capabilities/Capability/Request/GetFeatureInfo/Format", doc);
        Assert.assertTrue(formatNodes.getLength() > 1);

        // add mime type restrictions
        addMimeTypes();

        doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version==1.1.1",
                        true);
        formatNodes =
                xpath.getMatchingNodes(
                        "/WMT_MS_Capabilities/Capability/Request/GetMap/Format", doc);
        Assert.assertEquals(1, formatNodes.getLength());
        Assert.assertEquals(getMapFormat.getMimeType(), formatNodes.item(0).getTextContent());

        formatNodes =
                xpath.getMatchingNodes(
                        "/WMT_MS_Capabilities/Capability/Request/GetFeatureInfo/Format", doc);
        Assert.assertEquals(1, formatNodes.getLength());
        Assert.assertEquals(getInfoFormat.getContentType(), formatNodes.item(0).getTextContent());

        // remove restrictions
        removeMimeTypes();

        doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version==1.1.1",
                        true);
        formatNodes =
                xpath.getMatchingNodes(
                        "/WMT_MS_Capabilities/Capability/Request/GetMap/Format", doc);
        Assert.assertTrue(formatNodes.getLength() > 1);

        formatNodes =
                xpath.getMatchingNodes(
                        "/WMT_MS_Capabilities/Capability/Request/GetFeatureInfo/Format", doc);
        Assert.assertTrue(formatNodes.getLength() > 1);
    }
}
