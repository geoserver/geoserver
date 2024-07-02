/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire.wfs;

import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.OTHER_LANGUAGES;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireMetadata.SPATIAL_DATASET_IDENTIFIER_TYPE;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.DLS_NAMESPACE;
import static org.geoserver.inspire.InspireSchema.DLS_SCHEMA;
import static org.geoserver.inspire.InspireTestSupport.assertInspireCommonScenario1Response;
import static org.geoserver.inspire.InspireTestSupport.assertInspireDownloadSpatialDataSetIdentifierResponse;
import static org.geoserver.inspire.InspireTestSupport.assertInspireMetadataUrlResponse;
import static org.geoserver.inspire.InspireTestSupport.assertSchemaLocationContains;
import static org.geoserver.inspire.InspireTestSupport.clearInspireMetadata;
import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.inspire.UniqueResourceIdentifier;
import org.geoserver.inspire.UniqueResourceIdentifiers;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wfs.WFSInfo;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WFSExtendedCapabilitiesTest extends GeoServerSystemTestSupport {

    private static final String WFS_1_0_0_GETCAPREQUEST =
            "wfs?request=GetCapabilities&service=WFS&version=1.0.0";
    private static final String WFS_1_1_0_GETCAPREQUEST =
            "wfs?request=GetCapabilities&service=WFS&version=1.1.0";
    private static final String WFS_2_0_0_GETCAPREQUEST =
            "wfs?request=GetCapabilities&service=WFS&acceptVersions=2.0.0";

    @Test
    public void testNoInspireSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testCreateExtCapsOff() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, false);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/one");
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testExtendedCaps110WithFullSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_1_1_0_GETCAPREQUEST);

        NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());

        String schemaLocation = dom.getDocumentElement().getAttribute("xsi:schemaLocation");
        assertSchemaLocationContains(schemaLocation, DLS_NAMESPACE, DLS_SCHEMA);

        final Element extendedCaps = (Element) nodeList.item(0);

        assertInspireCommonScenario1Response(
                extendedCaps, "http://foo.com?bar=baz", "application/vnd.iso.19139+xml", "fre");

        final UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(new UniqueResourceIdentifier("one", "http://www.geoserver.org/one"));
        ids.add(
                new UniqueResourceIdentifier(
                        "two",
                        "http://www.geoserver.org/two",
                        "http://metadata.geoserver.org/id?two"));

        assertInspireDownloadSpatialDataSetIdentifierResponse(extendedCaps, ids);
    }

    @Test
    public void testExtendedCaps200WithFullSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());

        String schemaLocation = dom.getDocumentElement().getAttribute("xsi:schemaLocation");
        assertSchemaLocationContains(schemaLocation, DLS_NAMESPACE, DLS_SCHEMA);

        final Element extendedCaps = (Element) nodeList.item(0);

        assertInspireCommonScenario1Response(
                extendedCaps, "http://foo.com?bar=baz", "application/vnd.iso.19139+xml", "fre");

        final UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(new UniqueResourceIdentifier("one", "http://www.geoserver.org/one"));
        ids.add(
                new UniqueResourceIdentifier(
                        "two",
                        "http://www.geoserver.org/two",
                        "http://metadata.geoserver.org/id?two"));

        assertInspireDownloadSpatialDataSetIdentifierResponse(extendedCaps, ids);
    }

    @Test
    public void testReloadSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);
        getGeoServer().reload();
        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals(
                "Number of INSPIRE ExtendedCapabilities elements after settings reload",
                1,
                nodeList.getLength());
    }

    // No INSPIRE ExtendedCapabilities should be returned in a WFS 1.0.0 response
    @Test
    public void testExtCaps100WithFullSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_1_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals(0, nodeList.getLength());
    }

    // Test ExtendedCapabilities is not produced if required settings missing

    @Test
    public void testNoMetadataUrl() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testNoSpatialDataset() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testNoSpatialDatasetCode() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                ",http://www.geoserver.org/one;,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    // Test ExtendedCapabilities response when optional settings missing

    @Test
    public void testNoMediaType() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());

        nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MediaType");
        assertEquals("Number of MediaType elements", 0, nodeList.getLength());
    }

    // If settings were created with older version of INSPIRE extension before
    // the on/off check box setting existed we create the extended capabilities
    // if the other required settings exist and don't if they don't

    @Test
    public void testCreateExtCapMissingWithRequiredSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/one");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());
    }

    @Test
    public void testCreateExtCapMissingWithoutRequiredSettings() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        final NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testChangeMediaType() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/one");
        getGeoServer().save(serviceInfo);

        Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        NodeList nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MetadataUrl");
        assertEquals("Number of MediaType elements", 1, nodeList.getLength());
        Element mdUrl = (Element) nodeList.item(0);
        assertInspireMetadataUrlResponse(
                mdUrl, "http://foo.com?bar=baz", "application/vnd.iso.19139+xml");

        serviceInfo
                .getMetadata()
                .put(
                        SERVICE_METADATA_TYPE.key,
                        "application/vnd.ogc.csw.GetRecordByIdResponse_xml");
        getGeoServer().save(serviceInfo);

        dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MetadataUrl");
        assertEquals("Number of MediaType elements", 1, nodeList.getLength());
        mdUrl = (Element) nodeList.item(0);
        assertInspireMetadataUrlResponse(
                mdUrl,
                "http://foo.com?bar=baz",
                "application/vnd.ogc.csw.GetRecordByIdResponse_xml");
    }

    @Test
    public void testAddSpatialDatasetIdentifier() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(SPATIAL_DATASET_IDENTIFIER_TYPE.key, "one,http://www.geoserver.org/one");
        getGeoServer().save(serviceInfo);

        Document dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "SpatialDataSetIdentifier");
        assertEquals(1, nodeList.getLength());

        serviceInfo
                .getMetadata()
                .put(
                        SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                        metadata.get(SPATIAL_DATASET_IDENTIFIER_TYPE.key)
                                + ";two,,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);

        dom = getAsDOM(WFS_2_0_0_GETCAPREQUEST);

        nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "SpatialDataSetIdentifier");
        assertEquals(2, nodeList.getLength());

        final UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(new UniqueResourceIdentifier("one", "http://www.geoserver.org/one"));
        ids.add(new UniqueResourceIdentifier("two", null, "http://metadata.geoserver.org/id?two"));

        nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        final Element extendedCaps = (Element) nodeList.item(0);
        assertInspireDownloadSpatialDataSetIdentifierResponse(extendedCaps, ids);
    }

    @Test
    public void testSupportedLanguages() throws Exception {
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(OTHER_LANGUAGES.key, "ita,eng");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_1_1_0_GETCAPREQUEST);

        NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());

        String schemaLocation = dom.getDocumentElement().getAttribute("xsi:schemaLocation");
        assertSchemaLocationContains(schemaLocation, DLS_NAMESPACE, DLS_SCHEMA);

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
        final ServiceInfo serviceInfo = getGeoServer().getService(WFSInfo.class);
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, "http://foo.com?bar=baz");
        metadata.put(SERVICE_METADATA_TYPE.key, "application/vnd.iso.19139+xml");
        metadata.put(LANGUAGE.key, "fre");
        metadata.put(OTHER_LANGUAGES.key, "ita,eng");
        metadata.put(
                SPATIAL_DATASET_IDENTIFIER_TYPE.key,
                "one,http://www.geoserver.org/one;two,http://www.geoserver.org/two,http://metadata.geoserver.org/id?two");
        getGeoServer().save(serviceInfo);

        final Document dom = getAsDOM(WFS_1_1_0_GETCAPREQUEST + "&LANGUAGE=unsupported");

        NodeList nodeList = dom.getElementsByTagNameNS(DLS_NAMESPACE, "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());

        String schemaLocation = dom.getDocumentElement().getAttribute("xsi:schemaLocation");
        assertSchemaLocationContains(schemaLocation, DLS_NAMESPACE, DLS_SCHEMA);

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
                        .getFirstChild()
                        .getNodeValue();
        assertEquals("Unsupported LANGUAGE returns the Default one", "fre", responseLanguage);
    }
}
