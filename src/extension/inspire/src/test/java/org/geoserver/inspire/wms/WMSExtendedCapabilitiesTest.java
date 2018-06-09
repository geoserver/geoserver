/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wms;

import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireSchema.VS_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_SCHEMA;
import static org.geoserver.inspire.InspireTestSupport.clearInspireMetadata;
import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.inspire.ViewServicesTestSupport;
import org.geoserver.wms.WMSInfo;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class WMSExtendedCapabilitiesTest extends ViewServicesTestSupport {

    private static final String WMS_1_1_1_GETCAPREQUEST =
            "wms?request=GetCapabilities&service=WMS&version=1.1.1";
    private static final String WMS_1_3_0_GETCAPREQUEST =
            "wms?request=GetCapabilities&service=WMS&version=1.3.0";

    @Override
    protected String getGetCapabilitiesRequestPath() {
        return WMS_1_3_0_GETCAPREQUEST;
    }

    @Override
    protected String getMetadataUrl() {
        return "http://foo.com?bar=baz";
    }

    @Override
    protected String getMetadataType() {
        return "application/vnd.iso.19139+xml";
    }

    @Override
    protected String getLanguage() {
        return "fre";
    }

    @Override
    protected String getAlternateMetadataType() {
        return "application/vnd.ogc.csw.GetRecordByIdResponse_xml";
    }

    @Override
    protected ServiceInfo getServiceInfo() {
        return getGeoServer().getService(WMSInfo.class);
    }

    @Override
    protected String getInspireNameSpace() {
        return VS_NAMESPACE;
    }

    @Override
    protected String getInspireSchema() {
        return VS_SCHEMA;
    }

    // There is an INSPIRE DTD for WMS 1.1.1 but not implementing this
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
}
