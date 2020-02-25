/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

public class CapabilitiesBBOXForEachCRSTest extends WMSTestSupport {

    void addSRSAndSetFlag() {
        WMSInfo wms = getWMS().getServiceInfo();
        wms.getSRS().add("4326");
        wms.getSRS().add("3005");
        wms.getSRS().add("3857");
        wms.setBBOXForEachCRS(true);
        getGeoServer().save(wms);
    }

    @After
    public void removeSRS() {
        WMSInfo wms = getWMS().getServiceInfo();
        wms.getSRS().remove("4326");
        wms.getSRS().remove("3005");
        wms.getSRS().remove("3857");
        wms.setBBOXForEachCRS(false);
        getGeoServer().save(wms);
    }

    @Test
    public void testBBOXForEachCRS() throws Exception {
        Document doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.1.0",
                        true);

        String layer = MockData.PRIMITIVEGEOFEATURE.getLocalPart();
        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:4326']", doc);
        assertXpathNotExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3005']", doc);
        assertXpathNotExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3857']", doc);

        addSRSAndSetFlag();

        doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.1.0",
                        true);

        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:4326']", doc);
        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3005']", doc);
        assertXpathExists("//Layer[Name='" + layer + "']/BoundingBox[@SRS = 'EPSG:3857']", doc);
    }

    @Test
    public void testRootLayer() throws Exception {
        Document doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.1.0",
                        true);

        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/BoundingBox[@SRS = 'EPSG:4326']", doc);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/BoundingBox[@SRS = 'EPSG:3005']", doc);
        assertXpathNotExists(
                "/WMT_MS_Capabilities/Capability/Layer/BoundingBox[@SRS = 'EPSG:3857']", doc);

        addSRSAndSetFlag();
        doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.1.0",
                        true);

        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/BoundingBox[@SRS = 'EPSG:4326']", doc);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/BoundingBox[@SRS = 'EPSG:3005']", doc);
        assertXpathExists(
                "/WMT_MS_Capabilities/Capability/Layer/BoundingBox[@SRS = 'EPSG:3857']", doc);
    }
}
