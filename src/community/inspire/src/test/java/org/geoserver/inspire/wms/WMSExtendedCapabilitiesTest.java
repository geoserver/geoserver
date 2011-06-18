package org.geoserver.inspire.wms;

import static org.geoserver.inspire.wms.WMSExtendedCapabilitiesProvider.NAMESPACE;

import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.wms.WMSInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WMSExtendedCapabilitiesTest extends GeoServerTestSupport {

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
}
