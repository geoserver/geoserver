/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wms;

import static org.geoserver.inspire.wms.WMSExtendedCapabilitiesProvider.NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.inspire.InspireMetadata;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WMSExtendedCapabilitiesTest extends GeoServerSystemTestSupport {

    @Test
    public void testExtendedCaps() throws Exception {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        wms.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wms.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        getGeoServer().save(wms);

        Document dom = getAsDOM("wms?request=getcapabilities");
        assertEquals(NAMESPACE, dom.getDocumentElement().getAttribute("xmlns:inspire_vs"));

        final Element extendedCaps = getFirstElementByTagName(dom,
                "inspire_vs:ExtendedCapabilities");
        assertNotNull(extendedCaps);

        final Element mdUrl = getFirstElementByTagName(extendedCaps, "inspire_common:MetadataUrl");
        assertNotNull(mdUrl);

        final Element url = getFirstElementByTagName(mdUrl, "inspire_common:URL");
        assertNotNull(url);
        assertEquals("http://foo.com?bar=baz", url.getFirstChild().getNodeValue());

        final Element suppLangs = getFirstElementByTagName(extendedCaps,
                "inspire_common:SupportedLanguages");
        assertNotNull(suppLangs);
        final Element defLang = getFirstElementByTagName(suppLangs,
                "inspire_common:DefaultLanguage");
        assertNotNull(defLang);
        final Element defLangVal = getFirstElementByTagName(defLang, "inspire_common:Language");
        assertEquals("fre", defLangVal.getFirstChild().getNodeValue());

        final Element respLang = getFirstElementByTagName(extendedCaps,
                "inspire_common:ResponseLanguage");
        assertNotNull(respLang);
        final Element respLangVal = getFirstElementByTagName(defLang, "inspire_common:Language");
        assertEquals("fre", respLangVal.getFirstChild().getNodeValue());
    }

    @Test
    public void testChangeMediaType() throws Exception {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        wms.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wms.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        getGeoServer().save(wms);

        Document dom = getAsDOM("wms?request=getcapabilities");
        assertEquals(NAMESPACE, dom.getDocumentElement().getAttribute("xmlns:inspire_vs"));
        assertMetadataUrlAndMediaType(dom, "http://foo.com?bar=baz", "application/vnd.iso.19139+xml");

        wms.getMetadata().put(InspireMetadata.SERVICE_METADATA_TYPE.key, "application/xml");
        getGeoServer().save(wms);

        dom = getAsDOM("wms?request=getcapabilities");
        assertEquals(NAMESPACE, dom.getDocumentElement().getAttribute("xmlns:inspire_vs"));
        assertMetadataUrlAndMediaType(dom, "http://foo.com?bar=baz", "application/xml");
    }

    void assertMetadataUrlAndMediaType(Document dom, String metadataUrl, String metadataMediaType) {
        final Element extendedCaps = getFirstElementByTagName(dom,
                "inspire_vs:ExtendedCapabilities");
        assertNotNull(extendedCaps);

        final Element mdUrl = getFirstElementByTagName(extendedCaps, "inspire_common:MetadataUrl");
        assertNotNull(mdUrl);

        final Element url = getFirstElementByTagName(mdUrl, "inspire_common:URL");
        assertNotNull(url);
        assertEquals(metadataUrl, url.getFirstChild().getNodeValue());

        final Element mediaType = getFirstElementByTagName(mdUrl, "inspire_common:MediaType");
        assertNotNull(mediaType);
        assertEquals(metadataMediaType, mediaType.getFirstChild().getNodeValue());

    }
}
