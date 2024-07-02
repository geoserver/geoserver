/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.OTHER_LANGUAGES;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.VS_SCHEMA;
import static org.geoserver.inspire.InspireTestSupport.assertSchemaLocationContains;
import static org.geoserver.inspire.InspireTestSupport.clearInspireMetadata;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.inspire.ServicesTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.GrowableInternationalString;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WMSExtendedCapabilitiesTest extends ServicesTestSupport {

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

    @Test
    public void testINSPIRESchemaLocations() throws Exception {
        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);
        final NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
        String schemaLocation = dom.getDocumentElement().getAttribute("xsi:schemaLocation");
        assertSchemaLocationContains(schemaLocation, VS_NAMESPACE, VS_SCHEMA);
    }

    @Test
    public void testSupportedLanguages() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(OTHER_LANGUAGES.key, "ita,eng");
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);
        NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
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
    public void testResponseLanguageMatchesRequested() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        try {
            ServiceInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
            GrowableInternationalString title = new GrowableInternationalString();
            title.add(Locale.ITALIAN, "italian title");
            wmsInfo.setInternationalTitle(title);
            getGeoServer().save(wmsInfo);
            final MetadataMap metadata = serviceInfo.getMetadata();
            clearInspireMetadata(metadata);
            metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
            metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
            metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
            metadata.put(LANGUAGE.key, "fre");
            metadata.put(OTHER_LANGUAGES.key, "ita,eng");
            getGeoServer().save(serviceInfo);
            final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST + "&LANGUAGE=ita");
            NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
            final Element extendedCaps = (Element) nodeList.item(0);

            final Element suppLangs =
                    (Element)
                            extendedCaps
                                    .getElementsByTagNameNS(COMMON_NAMESPACE, "ResponseLanguage")
                                    .item(0);
            String language = null;
            for (int i = 0; i < suppLangs.getChildNodes().getLength(); i++) {
                Node el = suppLangs.getChildNodes().item(i);
                if (isLangNode(el)) language = el.getTextContent();
            }
            assertEquals("ita", language);
        } finally {
            getGeoServer().save(serviceInfo);
        }
    }

    @Test
    public void testResponseLanguageIsDefault() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        try {
            ServiceInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
            GrowableInternationalString title = new GrowableInternationalString();
            title.add(Locale.GERMAN, "de title");
            wmsInfo.setInternationalTitle(title);
            getGeoServer().save(wmsInfo);
            final MetadataMap metadata = serviceInfo.getMetadata();
            clearInspireMetadata(metadata);
            metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
            metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
            metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
            metadata.put(LANGUAGE.key, "fre");
            metadata.put(OTHER_LANGUAGES.key, "ita,eng");
            getGeoServer().save(serviceInfo);
            final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST + "&LANGUAGE=ger");
            NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
            final Element extendedCaps = (Element) nodeList.item(0);

            final Element suppLangs =
                    (Element)
                            extendedCaps
                                    .getElementsByTagNameNS(COMMON_NAMESPACE, "ResponseLanguage")
                                    .item(0);
            String language = null;
            for (int i = 0; i < suppLangs.getChildNodes().getLength(); i++) {
                Node el = suppLangs.getChildNodes().item(i);
                if (isLangNode(el)) language = el.getTextContent();
            }
            assertEquals("fre", language);
        } finally {
            getGeoServer().save(serviceInfo);
        }
    }

    @Test
    public void testResponseLanguageIsDefault2() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(OTHER_LANGUAGES.key, "ita,eng");
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST);
        NodeList nodeList = dom.getElementsByTagNameNS(VS_NAMESPACE, "ExtendedCapabilities");
        final Element extendedCaps = (Element) nodeList.item(0);

        final Element suppLangs =
                (Element)
                        extendedCaps
                                .getElementsByTagNameNS(COMMON_NAMESPACE, "ResponseLanguage")
                                .item(0);
        String language = null;
        for (int i = 0; i < suppLangs.getChildNodes().getLength(); i++) {
            Node el = suppLangs.getChildNodes().item(i);
            if (isLangNode(el)) language = el.getTextContent();
        }
        assertEquals("fre", language);
        final Element supportedLanguage =
                (Element)
                        extendedCaps
                                .getElementsByTagNameNS(COMMON_NAMESPACE, "SupportedLanguage")
                                .item(0);

        String nodeName = "";
        for (int i = 0; i < supportedLanguage.getChildNodes().getLength(); i++) {
            Node el = supportedLanguage.getChildNodes().item(i);
            if (isLangNode(el)) {
                language = el.getTextContent();
                nodeName = el.getNodeName();
            }
        }

        assertEquals("ita", language);
        assertEquals("inspire_common:Language", nodeName);
    }

    @Test
    public void testSupportedLanguageIsNull() throws Exception {
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
        final Element extendedCaps = (Element) nodeList.item(0);

        final Element supportedLanguage =
                (Element)
                        extendedCaps
                                .getElementsByTagNameNS(COMMON_NAMESPACE, "SupportedLanguage")
                                .item(0);

        assertNull(supportedLanguage);
    }

    @Test
    public void testUnSupportedLanguages() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ITALIAN, "italian title");
        title.add(Locale.FRENCH, "french title");
        serviceInfo.setDefaultLocale(Locale.FRENCH);
        serviceInfo.setInternationalTitle(title);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(OTHER_LANGUAGES.key, "ita,eng");
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(WMS_1_3_0_GETCAPREQUEST + "&LANGUAGE=unsupported");

        final String responseLanguage =
                dom.getElementsByTagNameNS(COMMON_NAMESPACE, "ResponseLanguage")
                        .item(0)
                        .getFirstChild()
                        .getNextSibling()
                        .getFirstChild()
                        .getNodeValue();
        assertEquals("Unsupported LANGUAGE returns the Default one", "fre", responseLanguage);

        // title checks for configured i18n title with unsupported language and with default

        // Define the XPath expression
        String xPathExpression =
                "//*[local-name()='WMS_Capabilities']/*[local-name()='Service']/*[local-name()='Title']";

        // Assert the value of the Title element
        assertXpathEvaluatesTo("french title", xPathExpression, dom);
    }

    private boolean isLangNode(Node el) {
        return el != null && el.getLocalName() != null && el.getLocalName().equals("Language");
    }
}
