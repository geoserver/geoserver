/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

import static org.geoserver.inspire.InspireMetadata.CREATE_EXTENDED_CAPABILITIES;
import static org.geoserver.inspire.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.InspireMetadata.SERVICE_METADATA_URL;
import static org.geoserver.inspire.InspireSchema.COMMON_NAMESPACE;
import static org.geoserver.inspire.InspireTestSupport.assertInspireCommonScenario1Response;
import static org.geoserver.inspire.InspireTestSupport.assertInspireMetadataUrlResponse;
import static org.geoserver.inspire.InspireTestSupport.assertSchemaLocationContains;
import static org.geoserver.inspire.InspireTestSupport.clearInspireMetadata;
import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public abstract class ViewServicesTestSupport extends GeoServerSystemTestSupport {

    protected abstract String getGetCapabilitiesRequestPath();

    protected abstract String getMetadataUrl();

    protected abstract String getMetadataType();

    protected abstract String getLanguage();

    protected abstract String getAlternateMetadataType();

    protected abstract ServiceInfo getServiceInfo();

    protected abstract String getInspireNameSpace();

    protected abstract String getInspireSchema();

    @Test
    public void testNoInspireSettings() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        final NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testCreateExtCapsOff() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, false);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        final NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testExtCapsWithFullSettings() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());
        String schemaLocation = dom.getDocumentElement().getAttribute("xsi:schemaLocation");
        assertSchemaLocationContains(schemaLocation, getInspireNameSpace(), getInspireSchema());
        final Element extendedCaps = (Element) nodeList.item(0);
        assertInspireCommonScenario1Response(
                extendedCaps, getMetadataUrl(), getMetadataType(), getLanguage());
    }

    @Test
    public void testReloadSettings() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        getGeoServer().reload();
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals(
                "Number of INSPIRE ExtendedCapabilities elements after settings reload",
                1,
                nodeList.getLength());
    }

    // Test ExtendedCapabilities is not produced if required settings missing
    @Test
    public void testNoMetadataUrl() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        final NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    // Test ExtendedCapabilities response when optional settings missing
    @Test
    public void testNoMediaType() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());
        nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MediaType");
        assertEquals("Number of MediaType elements", 0, nodeList.getLength());
    }

    // If settings were created with older version of INSPIRE extension before
    // the on/off check box setting existed we create the extended capabilities
    // if the other required settings exist and don't if they don't
    @Test
    public void testCreateExtCapMissingWithRequiredSettings() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 1, nodeList.getLength());
    }

    @Test
    public void testCreateExtCapMissingWithoutRequiredSettings() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        final Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        final NodeList nodeList =
                dom.getElementsByTagNameNS(getInspireNameSpace(), "ExtendedCapabilities");
        assertEquals("Number of INSPIRE ExtendedCapabilities elements", 0, nodeList.getLength());
    }

    @Test
    public void testChangeMediaType() throws Exception {
        final ServiceInfo serviceInfo = getServiceInfo();
        final MetadataMap metadata = serviceInfo.getMetadata();
        clearInspireMetadata(metadata);
        metadata.put(CREATE_EXTENDED_CAPABILITIES.key, true);
        metadata.put(SERVICE_METADATA_URL.key, getMetadataUrl());
        metadata.put(SERVICE_METADATA_TYPE.key, getMetadataType());
        metadata.put(LANGUAGE.key, getLanguage());
        getGeoServer().save(serviceInfo);
        Document dom = getAsDOM(getGetCapabilitiesRequestPath());
        NodeList nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MetadataUrl");
        assertEquals("Number of MediaType elements", 1, nodeList.getLength());
        Element mdUrl = (Element) nodeList.item(0);
        assertInspireMetadataUrlResponse(mdUrl, getMetadataUrl(), getMetadataType());
        serviceInfo.getMetadata().put(SERVICE_METADATA_TYPE.key, getAlternateMetadataType());
        getGeoServer().save(serviceInfo);
        dom = getAsDOM(getGetCapabilitiesRequestPath());
        nodeList = dom.getElementsByTagNameNS(COMMON_NAMESPACE, "MetadataUrl");
        assertEquals("Number of MediaType elements", 1, nodeList.getLength());
        mdUrl = (Element) nodeList.item(0);
        assertInspireMetadataUrlResponse(mdUrl, getMetadataUrl(), getAlternateMetadataType());
    }
}
