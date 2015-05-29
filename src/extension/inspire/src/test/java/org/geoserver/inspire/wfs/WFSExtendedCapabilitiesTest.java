/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.test.GeoServerSystemTestSupport;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.DLS_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.DLS_SCHEMA;
import org.geoserver.inspire.InspireMetadata;
import org.geoserver.wfs.WFSInfo;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.util.HashMap;

public class WFSExtendedCapabilitiesTest extends GeoServerSystemTestSupport {
    
    private static final String WFS_1_0_0_GETCAPREQUEST = "wfs?request=GetCapabilities&service=WFS&version=1.0.0";
    private static final String WFS_1_1_0_GETCAPREQUEST = "wfs?request=GetCapabilities&service=WFS&version=1.1.0";
    private static final String WFS_2_0_0_GETCAPREQUEST = "wfs?request=GetCapabilities&service=WFS&acceptVersions=2.0.0";

    @Before
    public void clearMetadata() {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().clear();
        getGeoServer().save(wfs);
    }

    @Test
    public void testNoInspireElementWhenNoMetadata() throws Exception {
        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals(0, nodeList.getLength());
    }

    @Test
    public void testNoInspireElementWhenNoMetadataUrl() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wfs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one");
        getGeoServer().save(wfs);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals(0, nodeList.getLength());
    }

    @Test
    public void testNoInspireElementWhenNoSpatialDataset() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        getGeoServer().save(wfs);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals(0, nodeList.getLength());
    }

    @Test
    public void testNoInspireElementWhenNoSpatialDatasetCode() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wfs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, ",http://www.geoserver.org/inspire/one");
        getGeoServer().save(wfs);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals(0, nodeList.getLength());
    }

    @Test
    public void testNoInspireElement100() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wfs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one");
        getGeoServer().save(wfs);

        final Document dom = getAsDOM(WFS_1_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertTrue(nodeList.getLength() == 0);
    }

    @Test
    public void testNoMediaTypeElement() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wfs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one,http://metadata.geoserver.org/id?one");
        getGeoServer().save(wfs);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MediaType");
        assertEquals(0, nodeList.getLength());
    }

    @Test
    public void testExtendedCaps110() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        wfs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one,http://metadata.geoserver.org/id?one");
        getGeoServer().save(wfs);

        final Document dom = getAsDOM(WFS_1_1_0_GETCAPREQUEST);

        XpathEngine xpath = getXpathEngine();

        assertEquals("Existence of ExtendedCapabilities element", "1",
                xpath.evaluate("count(//inspire_dls:ExtendedCapabilities)", dom));

        String schemaLocation = dom.getDocumentElement().getAttribute("xsi:schemaLocation"); 
        assertTrue(schemaLocation.contains(DLS_NAMESPACE));
        
        String[] schemaLocationParts = schemaLocation.split("\\s+");
        for (int i = 0; i < schemaLocationParts .length; i++) {
            if (schemaLocationParts[i].equals(DLS_NAMESPACE)) {
                assertTrue(schemaLocationParts[i+1].equals(DLS_SCHEMA));
            }
        }

        assertEquals("Expected MetadataURL URL",
                "http://foo.com?bar=baz",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:MetadataUrl/inspire_common:URL", dom));

        assertEquals("Expected MetadataURL MediaType",
                "application/vnd.iso.19139+xml",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:MetadataUrl/inspire_common:MediaType", dom));

        assertEquals("Expected default language",
                "fre",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:SupportedLanguages/inspire_common:DefaultLanguage/inspire_common:Language", dom));

        // Can decide to repeat default language in list of supported languages
        // but it isn't required by INSPIRE so won't test for it
        assertEquals("Expected response language",
                "fre",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:ResponseLanguage/inspire_common:Language", dom));

        assertEquals("Expected response spatial dataset identifier code",
                "one",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_dls:SpatialDataSetIdentifier/inspire_common:Code", dom));

        assertEquals("Expected spatial dataset identifier namespace",
                "http://www.geoserver.org/inspire/one",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_dls:SpatialDataSetIdentifier/inspire_common:Namespace", dom));

        assertEquals("Expected spatial dataset identifier metadata URL attribute",
                "http://metadata.geoserver.org/id?one",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_dls:SpatialDataSetIdentifier/@metadataURL", dom));
    }
    
    @Test
    public void testExtendedCaps200() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        wfs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one,http://metadata.geoserver.org/id?one");
        getGeoServer().save(wfs);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        XpathEngine xpath = getXpathEngine();

        assertEquals("Existence of ExtendedCapabilities element", "1",
                xpath.evaluate("count(//inspire_dls:ExtendedCapabilities)", dom));

        String schemaLocation = dom.getDocumentElement().getAttribute("xsi:schemaLocation"); 
        assertTrue(schemaLocation.contains(DLS_NAMESPACE));
        
        String[] schemaLocationParts = schemaLocation.split("\\s+");
        for (int i = 0; i < schemaLocationParts .length; i++) {
            if (schemaLocationParts[i].equals(DLS_NAMESPACE)) {
                assertTrue(schemaLocationParts[i+1].equals(DLS_SCHEMA));
            }
        }

        assertEquals("Expected MetadataURL URL",
                "http://foo.com?bar=baz",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:MetadataUrl/inspire_common:URL", dom));

        assertEquals("Expected MetadataURL MediaType",
                "application/vnd.iso.19139+xml",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:MetadataUrl/inspire_common:MediaType", dom));

        assertEquals("Expected default language",
                "fre",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:SupportedLanguages/inspire_common:DefaultLanguage/inspire_common:Language", dom));

        // Can decide to repeat default language in list of supported languages
        // but it isn't required by INSPIRE so won't test for it
        assertEquals("Expected response language",
                "fre",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:ResponseLanguage/inspire_common:Language", dom));

        assertEquals("Expected response spatial dataset identifier code",
                "one",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_dls:SpatialDataSetIdentifier/inspire_common:Code", dom));

        assertEquals("Expected spatial dataset identifier namespace",
                "http://www.geoserver.org/inspire/one",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_dls:SpatialDataSetIdentifier/inspire_common:Namespace", dom));

        assertEquals("Expected spatial dataset identifier metadata URL attribute",
                "http://metadata.geoserver.org/id?one",
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_dls:SpatialDataSetIdentifier/@metadataURL", dom));
    }

    @Test
    public void testChangeMediaType() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_TYPE.key, "application/vnd.ogc.csw.GetRecordByIdResponse_xml");
        wfs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one");
        getGeoServer().save(wfs);

        Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        assertMetadataUrlAndMediaType(dom, "http://foo.com?bar=baz", "application/vnd.ogc.csw.GetRecordByIdResponse_xml");

        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_TYPE.key, "application/xml");
        getGeoServer().save(wfs);

        dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        assertMetadataUrlAndMediaType(dom, "http://foo.com?bar=baz", "application/xml");
    }

    @Test
    public void testAddSpatialDatasetIdentifier() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getMetadata().put(InspireMetadata.LANGUAGE.key, "fre");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        wfs.getMetadata().put(InspireMetadata.SERVICE_METADATA_TYPE.key, "application/vnd.ogc.csw.GetRecordByIdResponse_xml");
        wfs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/inspire/one,http://metadata.geoserver.org/id?one");
        getGeoServer().save(wfs);

        Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        XpathEngine xpath = getXpathEngine();

        NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "SpatialDataSetIdentifier");
        assertEquals(1, nodeList.getLength());

        wfs.getMetadata().put(InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/inspire/one,"
                + ";two,,http://metadata.geoserver.org/id?two");
        getGeoServer().save(wfs);

        dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "SpatialDataSetIdentifier");
        assertEquals(2, nodeList.getLength());

        assertEquals("Expected first spatial dataset identifier namespace",
                "http://www.geoserver.org/inspire/one",
                xpath.evaluate("//inspire_dls:SpatialDataSetIdentifier[inspire_common:Code = 'one']/inspire_common:Namespace", dom));

        assertEquals("Expected first spatial dataset identifier no metadataURL",
                "0",
                xpath.evaluate("count(//inspire_dls:SpatialDataSetIdentifier[inspire_common:Code = 'one']/@metadataURL)", dom));

        assertEquals("Expected second spatial dataset identifier metadataURL",
                "http://metadata.geoserver.org/id?two",
                xpath.evaluate("//inspire_dls:SpatialDataSetIdentifier[inspire_common:Code = 'two']/@metadataURL", dom));

        assertEquals("Expected second spatial dataset identifier no namespace",
                "0",
                xpath.evaluate("count(//inspire_dls:SpatialDataSetIdentifier[inspire_common:Code = 'two']/inspire_common:Namespace)", dom));
    }

    private void assertMetadataUrlAndMediaType(Document dom, String metadataUrl, String metadataMediaType) throws XpathException {
        XpathEngine xpath = getXpathEngine();

        assertEquals("Existence of ExtendedCapabilities element", "1",
                xpath.evaluate("count(//inspire_dls:ExtendedCapabilities)", dom));

        assertEquals("Expected MetadataURL URL",
                metadataUrl,
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:MetadataUrl/inspire_common:URL", dom));

        assertEquals("Expected MetadataURL MediaType",
                metadataMediaType,
                xpath.evaluate("//inspire_dls:ExtendedCapabilities/inspire_common:MetadataUrl/inspire_common:MediaType", dom));

    }

    private XpathEngine getXpathEngine() {
        HashMap namespaces = new HashMap();
        namespaces.put("inspire_common", COMMON_NAMESPACE);
        namespaces.put("inspire_dls", DLS_NAMESPACE);
        NamespaceContext nsCtx = new SimpleNamespaceContext(namespaces);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        xpath.setNamespaceContext(nsCtx);
        return xpath;
    }
}
