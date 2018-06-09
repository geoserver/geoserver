/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wmts;

import org.geoserver.config.ServiceInfo;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.inspire.ViewServicesTestSupport;

public class WMTSExtendedCapabilitiesTest extends ViewServicesTestSupport {

    private static final String WMTS_1_0_0_GETCAPREQUEST =
            "gwc/service/wmts?REQUEST=GetCapabilities";

    @Override
    protected String getGetCapabilitiesRequestPath() {
        return WMTS_1_0_0_GETCAPREQUEST;
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
        return getGeoServer().getService(WMTSInfo.class);
    }

    @Override
    protected String getInspireNameSpace() {
        return WMTSExtendedCapabilitiesProvider.VS_VS_OWS_NAMESPACE;
    }

    @Override
    protected String getInspireSchema() {
        return WMTSExtendedCapabilitiesProvider.VS_VS_OWS_SCHEMA;
    }
}
