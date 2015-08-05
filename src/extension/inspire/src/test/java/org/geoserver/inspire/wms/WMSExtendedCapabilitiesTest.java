/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wms;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_SCHEMA;
import static org.geoserver.inspire.InspireTestSupport.assertInspireCommonScenario1Response;
import static org.geoserver.inspire.InspireTestSupport.assertInspireMetadataUrlResponse;
import static org.geoserver.inspire.InspireTestSupport.assertSchemaLocationContains;
import static org.geoserver.inspire.InspireTestSupport.clearInspireMetadata;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WMSExtendedCapabilitiesTest extends GeoServerSystemTestSupport {

        private static final String WMS_1_1_1_GETCAPREQUEST = "wms?request=GetCapabilities&service=WMS&version=1.1.1";
        private static final String WMS_1_3_0_GETCAPREQUEST = "wms?request=GetCapabilities&service=WMS&version=1.3.0";

    @Test
    public void testNoInspireSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testCreateExtCapsOff() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, false);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);
        
        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testExtCaps130WithFullSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);
        
        NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());
        
        String schemaLocation = dom.getDocumentElement().getAttribute("xsi:schemaLocation");
        assertSchemaLocationContains(schemaLocation, VS_NAMESPACE, VS_SCHEMA);

        final Element extendedCaps = (Element) nodeList.item(0);
        
        assertInspireCommonScenario1Response(extendedCaps, 
                "http://foo.com?bar=baz", "application/vnd.iso.19139+xml", "fre");

    }

    /* There is an INSPIRE DTD for WMS 1.1.1 but not implementing this */
    @Test
    public void testExtCaps111WithFullSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WMS_1_1_1_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    // Test ExtendedCapabilities is not produced if required settings missing
    
    @Test
    public void testNoMetadataUrl() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    // Test ExtendedCapabilities response when optional settings missing
    
    @Test
    public void testNoMediaType() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);

        NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());

        nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MediaType");
        assertEquals("Number of MediaType elements", 0, nodeList.getLength());
    }

    // If settings were created with older version of INSPIRE extension before
    // the on/off check box setting existed we create the extended capabilities
    // if the other required settings exist and don't if they don't
    
    @Test
    public void testCreateExtCapMissingWithRequiredSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);
        
        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);

        NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());
    }
    
    @Test
    public void testCreateExtCapMissingWithoutRequiredSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }
    
    @Test
    public void testChangeMediaType() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);

        Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);
        
        NodeList nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MetadataUrl");
        assertEquals("Number of MediaType elements", 1, nodeList.getLength());
        Element mdUrl = (Element) nodeList.item(0);
        assertInspireMetadataUrlResponse(mdUrl, "http://foo.com?bar=baz", "application/vnd.iso.19139+xml");

        serviceInfo.getMetadata().put(SERVICE_METADATA_TYPE.key, "application/vnd.ogc.csw.GetRecordByIdResponse_xml");
        getGeoServer().save(serviceInfo);

        dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);
        
        nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MetadataUrl");
        assertEquals("Number of MediaType elements", 1, nodeList.getLength());
        mdUrl = (Element) nodeList.item(0);
        assertInspireMetadataUrlResponse(mdUrl, "http://foo.com?bar=baz", "application/vnd.ogc.csw.GetRecordByIdResponse_xml");
    }
}
