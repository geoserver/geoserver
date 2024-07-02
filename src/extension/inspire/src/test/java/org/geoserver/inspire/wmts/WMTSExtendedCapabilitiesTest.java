/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wmts;

import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.OTHER_LANGUAGES;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireTestSupport.clearInspireMetadata;
import static org.geoserver.inspire.wmts.WMTSExtendedCapabilitiesProvider.VS_VS_OWS_NAMESPACE;
import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.inspire.ServicesTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WMTSExtendedCapabilitiesTest extends ServicesTestSupport {

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
        return VS_VS_OWS_NAMESPACE;
    }

    @Override
    protected String getInspireSchema() {
        return WMTSExtendedCapabilitiesProvider.VS_VS_OWS_SCHEMA;
    }

    @Test
    public void testSupportedLanguages() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMTSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(OTHER_LANGUAGES.key, "ita,eng");
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(WMTS_1_0_0_GETCAPREQUEST);
        NodeList nodeList = dom.getElementsByTagNameNS(VS_VS_OWS_NAMESPACE, "ExtendedCapabilities");
        final Element extendedCaps = (Element) nodeList.item(0);

        final Element suppLangs =
                (Element)
                        extendedCaps
                                .getElementsByTagNameNS(COMMON_NAMESPACE, "SupportedLanguages")
                                .item(0);

        nodeList = suppLangs.getElementsByTagNameNS(COMMON_NAMESPACE, "DefaultLanguage");
        assertEquals("Number of DefaultLanguage elements", 1, nodeList.getLength());
        nodeList = suppLangs.getElementsByTagNameNS(COMMON_NAMESPACE, "SupportedLanguage");
        assertEquals("Number of Supported Languages", 2, nodeList.getLength());
    }

    @Test
    public void testUnSupportedLanguages() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMTSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(OTHER_LANGUAGES.key, "ita,eng");
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(WMTS_1_0_0_GETCAPREQUEST + "&LANGUAGE=unsupported");
        NodeList nodeList = dom.getElementsByTagNameNS(VS_VS_OWS_NAMESPACE, "ExtendedCapabilities");
        final Element extendedCaps = (Element) nodeList.item(0);

        final Element suppLangs =
                (Element)
                        extendedCaps
                                .getElementsByTagNameNS(COMMON_NAMESPACE, "SupportedLanguages")
                                .item(0);

        nodeList = suppLangs.getElementsByTagNameNS(COMMON_NAMESPACE, "DefaultLanguage");
        assertEquals("Number of DefaultLanguage elements", 1, nodeList.getLength());
        nodeList = suppLangs.getElementsByTagNameNS(COMMON_NAMESPACE, "SupportedLanguage");
        assertEquals("Number of Supported Languages", 2, nodeList.getLength());

        final String responseLanguage =
                dom.getElementsByTagNameNS(COMMON_NAMESPACE, "ResponseLanguage")
                        .item(0)
                        .getFirstChild()
                        .getNextSibling()
                        .getFirstChild()
                        .getNodeValue();
        assertEquals("Unsupported LANGUAGE returns the Default one", "fre", responseLanguage);
    }
}
