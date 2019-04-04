/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;

import java.util.Map;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

public class CapabilitiesBBOXForEachCRSTest extends WMSTestSupport {

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
    }

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
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.3.0",
                        true);

        String layer = MockData.PRIMITIVEGEOFEATURE.getLocalPart();
        assertXpathExists(
                "//wms:Layer[wms:Name='" + layer + "']/wms:BoundingBox[@CRS = 'EPSG:4326']", doc);
        assertXpathNotExists(
                "//wms:Layer[wms:Name='" + layer + "']/wms:BoundingBox[@CRS = 'EPSG:3005']", doc);
        assertXpathNotExists(
                "//wms:Layer[wms:Name='" + layer + "']/wms:BoundingBox[@CRS = 'EPSG:3857']", doc);

        addSRSAndSetFlag();
        doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.3.0",
                        true);

        assertXpathExists(
                "//wms:Layer[wms:Name='" + layer + "']/wms:BoundingBox[@CRS = 'EPSG:4326']", doc);
        assertXpathExists(
                "//wms:Layer[wms:Name='" + layer + "']/wms:BoundingBox[@CRS = 'EPSG:3005']", doc);
        assertXpathExists(
                "//wms:Layer[wms:Name='" + layer + "']/wms:BoundingBox[@CRS = 'EPSG:3857']", doc);
    }

    @Test
    public void testRootLayer() throws Exception {
        Document doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.3.0",
                        true);

        assertXpathNotExists(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:BoundingBox[@CRS = 'EPSG:4326']",
                doc);
        assertXpathNotExists(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:BoundingBox[@CRS = 'EPSG:3005']",
                doc);
        assertXpathNotExists(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:BoundingBox[@CRS = 'EPSG:3857']",
                doc);

        addSRSAndSetFlag();
        doc =
                getAsDOM(
                        "sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.3.0",
                        true);

        assertXpathExists(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:BoundingBox[@CRS = 'EPSG:4326']",
                doc);
        assertXpathExists(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:BoundingBox[@CRS = 'EPSG:3005']",
                doc);
        assertXpathExists(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:BoundingBox[@CRS = 'EPSG:3857']",
                doc);
    }
}
