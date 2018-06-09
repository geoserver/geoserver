/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.referencing.CRS;
import org.geotools.util.NumberRange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GetCapabilitiesTransformerTest {

    private static final class EmptyExtendedCapabilitiesProvider
            implements ExtendedCapabilitiesProvider {
        public String[] getSchemaLocations(String schemaBaseURL) {
            throw new UnsupportedOperationException();
        }

        public void registerNamespaces(NamespaceSupport namespaces) {
            throw new UnsupportedOperationException();
        }

        public List<String> getVendorSpecificCapabilitiesRoots(GetCapabilitiesRequest request) {
            return null;
        }

        /**
         * @see
         *     org.geoserver.wms.ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls()
         */
        public List<String> getVendorSpecificCapabilitiesChildDecls(
                GetCapabilitiesRequest request) {
            return null;
        }

        public void encode(Translator tx, WMSInfo wms, GetCapabilitiesRequest request)
                throws IOException {}

        @Override
        public void customizeRootCrsList(Set<String> srs) {}

        @Override
        public NumberRange<Double> overrideScaleDenominators(
                PublishedInfo layer, NumberRange<Double> scaleDenominators) {
            return null;
        }
    }

    private static final class TestExtendedCapabilitiesProvider
            implements ExtendedCapabilitiesProvider {
        public String[] getSchemaLocations(String schemaBaseURL) {
            throw new UnsupportedOperationException();
        }

        public void registerNamespaces(NamespaceSupport namespaces) {
            throw new UnsupportedOperationException();
        }

        public List<String> getVendorSpecificCapabilitiesRoots(GetCapabilitiesRequest request) {
            return Collections.singletonList("TestElement?");
        }

        /**
         * @see
         *     org.geoserver.wms.ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls()
         */
        public List<String> getVendorSpecificCapabilitiesChildDecls(
                GetCapabilitiesRequest request) {
            return Collections.singletonList("<!ELEMENT TestSubElement (#PCDATA) >");
        }

        public void encode(Translator tx, WMSInfo wms, GetCapabilitiesRequest request)
                throws IOException {
            tx.start("TestElement");
            tx.start("TestSubElement");
            tx.end("TestSubElement");
            tx.end("TestElement");
        }

        @Override
        public void customizeRootCrsList(Set<String> srs) {
            srs.clear();
            srs.add("EPSG:4326");
        }

        @Override
        public NumberRange<Double> overrideScaleDenominators(
                PublishedInfo layer, NumberRange<Double> scaleDenominators) {
            return new NumberRange<Double>(Double.class, 0d, 1000d);
        }
    }

    private XpathEngine XPATH;

    /**
     * default base url to feed a GetCapabilitiesTransformer with for it to append the DTD location
     */
    private static final String baseUrl = "http://localhost/geoserver";

    /** test map formats to feed a GetCapabilitiesTransformer with */
    private static final Set<String> mapFormats = Collections.singleton("image/png");

    /** test legend formats to feed a GetCapabilitiesTransformer with */
    private static final Set<String> legendFormats = Collections.singleton("image/png");

    /**
     * a mocked up {@link GeoServer} config, almost empty after setUp(), except for the {@link
     * WMSInfo}, {@link GeoServerInfo} and empty {@link Catalog}, Specific tests should add content
     * as needed
     */
    private GeoServerImpl geosConfig;

    /**
     * a mocked up {@link GeoServerInfo} for {@link #geosConfig}. Specific tests should set its
     * properties as needed
     */
    private GeoServerInfoImpl geosInfo;

    /**
     * a mocked up {@link WMSInfo} for {@link #geosConfig}, empty except for the WMSInfo after
     * setUp(), Specific tests should set its properties as needed
     */
    private WMSInfoImpl wmsInfo;

    /**
     * a mocked up {@link Catalog} for {@link #geosConfig}, empty after setUp(), Specific tests
     * should add content as needed
     */
    private CatalogImpl catalog;

    private GetCapabilitiesRequest req;

    private WMS wmsConfig;

    /**
     * Sets up the configuration objects with default values. Since they're live, specific tests can
     * modify their state before running the assertions
     */
    @Before
    public void setUp() throws Exception {
        geosConfig = new GeoServerImpl();

        geosInfo = new GeoServerInfoImpl(geosConfig);
        geosInfo.setContact(new ContactInfoImpl());
        geosConfig.setGlobal(geosInfo);

        wmsInfo = new WMSInfoImpl();
        geosConfig.add(wmsInfo);

        catalog = new CatalogImpl();
        geosConfig.setCatalog(catalog);

        wmsConfig = new WMS(geosConfig);

        req = new GetCapabilitiesRequest();
        req.setBaseUrl(baseUrl);

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        XPATH = XMLUnit.newXpathEngine();
    }

    @Test
    public void testHeader() throws Exception {
        GetCapabilitiesTransformer tr;
        tr = new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
        StringWriter writer = new StringWriter();
        tr.transform(req, writer);
        String content = writer.getBuffer().toString();

        Assert.assertTrue(content.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        String dtdDef =
                "<!DOCTYPE WMT_MS_Capabilities SYSTEM \""
                        + baseUrl
                        + "/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd\">";
        Assert.assertTrue(content.contains(dtdDef));
    }

    @Test
    public void testRootElement() throws Exception {
        GetCapabilitiesTransformer tr;
        tr = new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);

        Document dom = WMSTestSupport.transform(req, tr);
        Element root = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", root.getNodeName());
        assertEquals("1.1.1", root.getAttribute("version"));
        assertEquals("0", root.getAttribute("updateSequence"));

        geosInfo.setUpdateSequence(10);
        tr = new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
        dom = WMSTestSupport.transform(req, tr);
        root = dom.getDocumentElement();
        assertEquals("10", root.getAttribute("updateSequence"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testServiceSection() throws Exception {
        wmsInfo.setTitle("title");
        wmsInfo.setAbstract("abstract");
        wmsInfo.getKeywords().add(new Keyword("k1"));
        wmsInfo.getKeywords().add(new Keyword("k2"));
        // @REVISIT: this is not being respected, but the onlineresource is being set based on the
        // proxyBaseUrl... not sure if that's correct
        wmsInfo.setOnlineResource("http://onlineresource/fake");

        ContactInfo contactInfo = new ContactInfoImpl();
        geosInfo.setContact(contactInfo);
        contactInfo.setContactPerson("contactPerson");
        contactInfo.setContactOrganization("contactOrganization");
        contactInfo.setContactPosition("contactPosition");
        contactInfo.setAddress("address");
        contactInfo.setAddressType("addressType");
        contactInfo.setAddressCity("city");
        contactInfo.setAddressState("state");
        contactInfo.setAddressPostalCode("postCode");
        contactInfo.setAddressCountry("country");
        contactInfo.setContactVoice("voice");
        contactInfo.setContactEmail("email");
        contactInfo.setContactFacsimile("fax");

        wmsInfo.setFees("fees");
        wmsInfo.setAccessConstraints("accessConstraints");

        GetCapabilitiesTransformer tr;
        tr = new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        String service = "/WMT_MS_Capabilities/Service";
        assertXpathEvaluatesTo("OGC:WMS", service + "/Name", dom);

        assertXpathEvaluatesTo("title", service + "/Title", dom);
        assertXpathEvaluatesTo("abstract", service + "/Abstract", dom);
        assertXpathEvaluatesTo("k1", service + "/KeywordList/Keyword[1]", dom);
        assertXpathEvaluatesTo("k2", service + "/KeywordList/Keyword[2]", dom);

        assertXpathEvaluatesTo(
                wmsInfo.getOnlineResource(), service + "/OnlineResource/@xlink:href", dom);

        assertXpathEvaluatesTo(
                "contactPerson",
                service + "/ContactInformation/ContactPersonPrimary/ContactPerson",
                dom);
        assertXpathEvaluatesTo(
                "contactOrganization",
                service + "/ContactInformation/ContactPersonPrimary/ContactOrganization",
                dom);
        assertXpathEvaluatesTo(
                "contactPosition", service + "/ContactInformation/ContactPosition", dom);
        assertXpathEvaluatesTo(
                "address", service + "/ContactInformation/ContactAddress/Address", dom);
        assertXpathEvaluatesTo(
                "addressType", service + "/ContactInformation/ContactAddress/AddressType", dom);
        assertXpathEvaluatesTo("city", service + "/ContactInformation/ContactAddress/City", dom);
        assertXpathEvaluatesTo(
                "state", service + "/ContactInformation/ContactAddress/StateOrProvince", dom);
        assertXpathEvaluatesTo(
                "postCode", service + "/ContactInformation/ContactAddress/PostCode", dom);
        assertXpathEvaluatesTo(
                "country", service + "/ContactInformation/ContactAddress/Country", dom);
        assertXpathEvaluatesTo("voice", service + "/ContactInformation/ContactVoiceTelephone", dom);
        assertXpathEvaluatesTo(
                "fax", service + "/ContactInformation/ContactFacsimileTelephone", dom);
        assertXpathEvaluatesTo(
                "email", service + "/ContactInformation/ContactElectronicMailAddress", dom);

        assertXpathEvaluatesTo("fees", service + "/Fees", dom);
        assertXpathEvaluatesTo("accessConstraints", service + "/AccessConstraints", dom);
    }

    @Test
    public void testCRSList() throws Exception {
        GetCapabilitiesTransformer tr;
        tr = new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);
        final Set<String> supportedCodes = CRS.getSupportedCodes("EPSG");
        supportedCodes.addAll(CRS.getSupportedCodes("AUTO"));
        NodeList allCrsCodes =
                XPATH.getMatchingNodes("/WMT_MS_Capabilities/Capability/Layer/SRS", dom);
        assertEquals(supportedCodes.size() - 1 /* WGS84(DD) */, allCrsCodes.getLength());
    }

    @Test
    public void testLimitedCRSList() throws Exception {
        wmsInfo.getSRS().add("EPSG:3246");
        wmsInfo.getSRS().add("EPSG:23030");

        GetCapabilitiesTransformer tr;
        tr = new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);
        NodeList limitedCrsCodes =
                XPATH.getMatchingNodes("/WMT_MS_Capabilities/Capability/Layer/SRS", dom);
        assertEquals(2, limitedCrsCodes.getLength());
    }

    @Test
    public void testVendorSpecificCapabilities() throws Exception {
        ExtendedCapabilitiesProvider vendorCapsProvider = new TestExtendedCapabilitiesProvider();

        GetCapabilitiesTransformer tr;
        tr =
                new GetCapabilitiesTransformer(
                        wmsConfig,
                        baseUrl,
                        mapFormats,
                        legendFormats,
                        Collections.singletonList(vendorCapsProvider));
        tr.setIndentation(2);
        checkVendorSpecificCapsProviders(tr);
    }

    @Test
    public void testVendorSpecificCapabilitiesWithEmptyProvider() throws Exception {
        ExtendedCapabilitiesProvider emptyCapsProvider = new EmptyExtendedCapabilitiesProvider();
        ExtendedCapabilitiesProvider vendorCapsProvider = new TestExtendedCapabilitiesProvider();

        GetCapabilitiesTransformer tr;
        tr =
                new GetCapabilitiesTransformer(
                        wmsConfig,
                        baseUrl,
                        mapFormats,
                        legendFormats,
                        Arrays.asList(emptyCapsProvider, vendorCapsProvider));
        tr.setIndentation(2);
        checkVendorSpecificCapsProviders(tr);
    }

    private void checkVendorSpecificCapsProviders(GetCapabilitiesTransformer tr)
            throws Exception, XpathException {
        Document dom = WMSTestSupport.transform(req, tr);
        assertXpathEvaluatesTo("1", "count(/WMT_MS_Capabilities/Capability/Layer/SRS)", dom);
        assertXpathEvaluatesTo(
                "1", "count(/WMT_MS_Capabilities/Capability/Layer[SRS='EPSG:4326'])", dom);

        NodeList list =
                XPATH.getMatchingNodes(
                        "/WMT_MS_Capabilities/Capability/VendorSpecificCapabilities/TestElement",
                        dom);
        assertEquals(1, list.getLength());

        list =
                XPATH.getMatchingNodes(
                        "/WMT_MS_Capabilities/Capability/VendorSpecificCapabilities/TestElement/TestSubElement",
                        dom);
        assertEquals(1, list.getLength());
    }
}
