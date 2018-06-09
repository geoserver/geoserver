/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.DLS_NAMESPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.MetadataMap;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class InspireTestSupport {

    public static void clearInspireMetadata(MetadataMap metadata) {
        for (InspireMetadata item : InspireMetadata.values()) {
            metadata.remove(item.key);
        }
    }

    public static void assertSchemaLocationContains(
            String schemaLocation, String namespace, String url) {
        assertTrue(schemaLocation.contains(namespace));

        String[] schemaLocationParts = schemaLocation.split("\\s+");
        for (int i = 0; i < schemaLocationParts.length; i++) {
            if (schemaLocationParts[i].equals(namespace)) {
                assertTrue(schemaLocationParts[i + 1].equals(url));
            }
        }
    }

    public static void assertInspireCommonScenario1Response(
            final Element extendedCapabilities,
            final String metadataUrl,
            final String mediaType,
            final String language) {

        NodeList nodeList =
                extendedCapabilities.getElementsByTagNameNS(COMMON_NAMESPACE, "MetadataUrl");
        assertEquals("Number of MetadataUrl elements", 1, nodeList.getLength());
        final Element mdUrl = (Element) nodeList.item(0);

        assertInspireMetadataUrlResponse(mdUrl, metadataUrl, mediaType);

        nodeList =
                extendedCapabilities.getElementsByTagNameNS(COMMON_NAMESPACE, "SupportedLanguages");
        assertEquals("Number of SupportedLanguages elements", 1, nodeList.getLength());
        final Element suppLangs = (Element) nodeList.item(0);

        nodeList = suppLangs.getElementsByTagNameNS(COMMON_NAMESPACE, "DefaultLanguage");
        assertEquals("Number of DefaultLanguage elements", 1, nodeList.getLength());
        final Element defLang = (Element) nodeList.item(0);

        nodeList = defLang.getElementsByTagNameNS(COMMON_NAMESPACE, "Language");
        assertEquals("Number of DefaultLanguage/Language elements", 1, nodeList.getLength());
        final Element defLangVal = (Element) nodeList.item(0);
        assertEquals(
                "DefaultLanguage/Language", language, defLangVal.getFirstChild().getNodeValue());

        nodeList =
                extendedCapabilities.getElementsByTagNameNS(COMMON_NAMESPACE, "ResponseLanguage");
        assertEquals("Number of ResponseLanguage elements", 1, nodeList.getLength());
        final Element respLang = (Element) nodeList.item(0);

        nodeList = respLang.getElementsByTagNameNS(COMMON_NAMESPACE, "Language");
        assertEquals("Number of ResponseLanguage/Language elements", 1, nodeList.getLength());
        final Element respLangVal = (Element) nodeList.item(0);
        assertEquals(
                "ResponseLanguage/Language", language, respLangVal.getFirstChild().getNodeValue());
    }

    public static void assertInspireMetadataUrlResponse(
            final Element mdUrl, final String metadataUrl, final String mediaType) {

        NodeList nodeList = mdUrl.getElementsByTagNameNS(COMMON_NAMESPACE, "URL");
        assertEquals("Number of URL elements", 1, nodeList.getLength());
        final Element url = (Element) nodeList.item(0);
        assertEquals("MetadataUrl/URL", metadataUrl, url.getFirstChild().getNodeValue());

        nodeList = mdUrl.getElementsByTagNameNS(COMMON_NAMESPACE, "MediaType");
        if (mediaType == null) {
            assertEquals("Number of MediaType elements", 0, nodeList.getLength());
        } else {
            assertEquals("Number of MediaType elements", 1, nodeList.getLength());
            assertEquals(
                    "MediaType",
                    mediaType,
                    ((Element) nodeList.item(0)).getFirstChild().getNodeValue());
        }
    }

    public static void assertInspireDownloadSpatialDataSetIdentifierResponse(
            final Element extendedCapabilities, final UniqueResourceIdentifiers ids) {

        final NodeList spatialDataSetIdentifiers =
                extendedCapabilities.getElementsByTagNameNS(
                        DLS_NAMESPACE, "SpatialDataSetIdentifier");
        assertEquals(
                "Number of SpatialDataSetIdentifer elements",
                ids.size(),
                spatialDataSetIdentifiers.getLength());

        final Map<String, UniqueResourceIdentifier> idMap =
                new HashMap<String, UniqueResourceIdentifier>();

        for (UniqueResourceIdentifier id : ids) {
            idMap.put(id.getCode(), id);
        }

        for (int i = 0; i < spatialDataSetIdentifiers.getLength(); i++) {
            Element sdi = (Element) spatialDataSetIdentifiers.item(i);
            NodeList nodeList = sdi.getElementsByTagNameNS(COMMON_NAMESPACE, "Code");
            assertEquals("Number of Code elements", 1, nodeList.getLength());
            String code = ((Element) nodeList.item(0)).getFirstChild().getNodeValue();
            assertTrue("Should be an identifier with code " + code, idMap.containsKey(code));
            nodeList = sdi.getElementsByTagNameNS(COMMON_NAMESPACE, "Namespace");
            String expectedNamespace = idMap.get(code).getNamespace();
            if (expectedNamespace == null) {
                assertEquals(
                        "Number of Namespace elements for identifier with code " + code,
                        0,
                        nodeList.getLength());
            } else {
                assertEquals(
                        "Number of Namespace elements for identifier with code " + code,
                        1,
                        nodeList.getLength());
                String actualNamespace =
                        ((Element) nodeList.item(0)).getFirstChild().getNodeValue();
                assertEquals(
                        "Namespace for identifier with code " + code,
                        expectedNamespace,
                        actualNamespace);
            }
            String expectedMetadataUrl = idMap.get(code).getMetadataURL();
            String actualMetadataUrl = sdi.getAttribute("metadataURL");
            if (expectedMetadataUrl == null) {
                expectedMetadataUrl = "";
            }
            assertEquals(
                    "metadataURL attribute for identifer with code" + code,
                    expectedMetadataUrl,
                    actualMetadataUrl);
        }
    }
}
