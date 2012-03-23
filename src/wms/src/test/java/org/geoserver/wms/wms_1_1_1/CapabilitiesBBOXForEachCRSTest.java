package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;

import java.util.Map;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.w3c.dom.Document;

public class CapabilitiesBBOXForEachCRSTest extends WMSTestSupport {

//    @Override
//    protected void registerNamespaces(Map<String, String> namespaces) {
//        namespaces.put("wms", "http://www.opengis.net/wms");
//        namespaces.put("ows", "http://www.opengis.net/ows");
//    }
    
    void addSRSAndSetFlag() {
        WMSInfo wms = getWMS().getServiceInfo();
        wms.getSRS().add("4326");
        wms.getSRS().add("3005");
        wms.setBBOXForEachCRS(true);
        getGeoServer().save(wms);
    }

    public void testBBOXForEachCRS() throws Exception {
        Document doc = getAsDOM("sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.1.0", true);

        String layer = MockData.PRIMITIVEGEOFEATURE.getLocalPart();
        assertXpathExists("//Layer[Name='"+layer+"']/BoundingBox[@SRS = 'EPSG:4326']", doc);
        assertXpathNotExists("//Layer[Name='"+layer+"']/BoundingBox[@SRS = 'EPSG:3005']", doc);
        
        addSRSAndSetFlag();

        doc = getAsDOM("sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.1.0", true);

        assertXpathExists("//Layer[Name='"+layer+"']/BoundingBox[@SRS = 'EPSG:4326']", doc);
        assertXpathExists("//Layer[Name='"+layer+"']/BoundingBox[@SRS = 'EPSG:3005']", doc);
    }

    public void testRootLayer() throws Exception {
        Document doc = getAsDOM("sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.1.0", true);

        assertXpathNotExists("/WMT_MS_Capabilities/Capability/Layer/BoundingBox[@SRS = 'EPSG:4326']", doc);
        assertXpathNotExists("/WMT_MS_Capabilities/Capability/Layer/BoundingBox[@SRS = 'EPSG:3005']", doc);

        addSRSAndSetFlag();
        doc = getAsDOM("sf/PrimitiveGeoFeature/wms?service=WMS&request=getCapabilities&version=1.1.0", true);

        assertXpathExists("/WMT_MS_Capabilities/Capability/Layer/BoundingBox[@SRS = 'EPSG:4326']", doc);
        assertXpathExists("/WMT_MS_Capabilities/Capability/Layer/BoundingBox[@SRS = 'EPSG:3005']", doc);
    }
}
