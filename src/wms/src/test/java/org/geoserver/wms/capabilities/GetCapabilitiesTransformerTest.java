/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.NumberRange;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GetCapabilitiesTransformerTest extends WMSTestSupport {

    private static final class EmptyExtendedCapabilitiesProvider
            implements ExtendedCapabilitiesProvider {
        @Override
        public String[] getSchemaLocations(String schemaBaseURL) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerNamespaces(NamespaceSupport namespaces) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getVendorSpecificCapabilitiesRoots(GetCapabilitiesRequest request) {
            return null;
        }

        /**
         * @see
         *     org.geoserver.wms.ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls()
         */
        @Override
        public List<String> getVendorSpecificCapabilitiesChildDecls(
                GetCapabilitiesRequest request) {
            return null;
        }

        @Override
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
        @Override
        public String[] getSchemaLocations(String schemaBaseURL) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerNamespaces(NamespaceSupport namespaces) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> getVendorSpecificCapabilitiesRoots(GetCapabilitiesRequest request) {
            return Collections.singletonList("TestElement?");
        }

        /**
         * @see
         *     org.geoserver.wms.ExtendedCapabilitiesProvider#getVendorSpecificCapabilitiesChildDecls()
         */
        @Override
        public List<String> getVendorSpecificCapabilitiesChildDecls(
                GetCapabilitiesRequest request) {
            return Collections.singletonList("<!ELEMENT TestSubElement (#PCDATA) >");
        }

        @Override
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
            return new NumberRange<>(Double.class, 0d, 1000d);
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

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("wms", "http://www.opengis.net/wms");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        XPATH = XMLUnit.newXpathEngine();
    }

    @Test
    public void testHeader() throws Exception {
        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
        StringWriter writer = new StringWriter();
        tr.transform(req, writer);
        String content = writer.getBuffer().toString();

        assertTrue(content.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        String dtdDef =
                "<!DOCTYPE WMT_MS_Capabilities SYSTEM \""
                        + baseUrl
                        + "/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd\">";
        assertTrue(content.contains(dtdDef));
    }

    @Test
    public void testRootElement() throws Exception {
        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);

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

        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
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
        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
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

        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);
        NodeList limitedCrsCodes =
                XPATH.getMatchingNodes("/WMT_MS_Capabilities/Capability/Layer/SRS", dom);
        assertEquals(2, limitedCrsCodes.getLength());
    }

    @Test
    public void testVendorSpecificCapabilities() throws Exception {
        ExtendedCapabilitiesProvider vendorCapsProvider = new TestExtendedCapabilitiesProvider();

        GetCapabilitiesTransformer tr =
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

        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(
                        wmsConfig,
                        baseUrl,
                        mapFormats,
                        legendFormats,
                        Arrays.asList(emptyCapsProvider, vendorCapsProvider));
        tr.setIndentation(2);
        checkVendorSpecificCapsProviders(tr);
    }

    private void checkVendorSpecificCapsProviders(GetCapabilitiesTransformer tr) throws Exception {
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

    /**
     * Gets a capabilities document as an XML string Also, adds: <!DOCTYPE WMT_MS_Capabilities
     * SYSTEM "WMS_MS_Capabilities.dtd"> at the top of the document.
     */
    String getCapabilitiesXML() throws Exception {
        // WMS wms = new WMS(getGeoServer());
        WMS wms = (WMS) applicationContext.getBean("wms");

        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wms, baseUrl, mapFormats, legendFormats, null);

        Document dom = WMSTestSupport.transform(req, tr);
        TransformerFactory ttf = TransformerFactory.newInstance();
        Transformer trans = ttf.newTransformer();
        StringWriter sw = new StringWriter();
        trans.transform(new DOMSource(dom), new StreamResult(sw));

        String xml = sw.toString();
        xml =
                xml.replace(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<!DOCTYPE WMT_MS_Capabilities SYSTEM \"WMS_MS_Capabilities.dtd\">");

        return xml;
    }

    @Test
    public void testValidatesAgainstDTD() throws Exception {
        // get a capabilities document
        String getCapXML = getCapabilitiesXML();

        // get the wms 1.1.1 DTD
        URL dtdURL =
                GetCapabilitiesTransformer.class.getResource(
                        "/schemas/wms/1.1.1/WMS_MS_Capabilities.dtd");
        String dtd = Resources.toString(dtdURL, StandardCharsets.UTF_8);

        try (InputStream dtdInputStream = new ByteArrayInputStream(dtd.getBytes())) {

            // parse and validate the capabilities document against the DTD
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();

            // Normally, the DTD would downloaded from the internet.  We don't want to do that, so
            // we tell the parse to use our DTD instead of downloading it.
            builder.setEntityResolver(
                    new EntityResolver() {
                        @Override
                        public InputSource resolveEntity(String publicId, String systemId)
                                throws SAXException, IOException {
                            if (systemId.endsWith("WMS_MS_Capabilities.dtd")) {
                                return new InputSource(dtdInputStream);
                            }
                            return null;
                        }
                    });

            // make sure sax throws an error when it finds an error
            builder.setErrorHandler(
                    new ErrorHandler() {
                        @Override
                        public void warning(SAXParseException exception) throws SAXException {}

                        @Override
                        public void error(SAXParseException exception) throws SAXException {
                            throw new SAXException("SAX ERROR OCCURRED!", exception);
                        }

                        @Override
                        public void fatalError(SAXParseException exception) throws SAXException {
                            throw new SAXException("SAX ERROR OCCURRED!", exception);
                        }
                    });

            // this will parse and validate - if there are parse issues the ErrorHandler will throw.
            builder.parse(new ByteArrayInputStream(getCapXML.getBytes()));
        }
    }

    @Test
    public void testLayerStyleSections() throws Exception {
        // Given
        String LAYER_GROUP_NAME = "testLayerGroup";
        createLayerGroup(LAYER_GROUP_NAME);

        // When
        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
        Document trDom = WMSTestSupport.transform(req, tr);
        Element trRoot = trDom.getDocumentElement();

        Capabilities_1_3_0_Transformer tr130 =
                new Capabilities_1_3_0_Transformer(
                        wmsConfig,
                        baseUrl,
                        wmsConfig.getAllowedMapFormats(),
                        wmsConfig.getAvailableExtendedCapabilitiesProviders());
        Document tr130Dom = WMSTestSupport.transform(req, tr130);
        Element tr130Root = tr130Dom.getDocumentElement();

        // Then
        assertEquals("WMT_MS_Capabilities", trRoot.getNodeName());
        assertEquals(1, trDom.getElementsByTagName("Style").getLength());

        assertEquals("WMS_Capabilities", tr130Root.getNodeName());
        assertEquals(1, tr130Dom.getElementsByTagName("Style").getLength());
    }

    @Test
    public void testDisableDefaultLayerGroupStyle1_1() throws Exception {
        // test that when defaultGroupStyleEnabled
        // is set to false the default layerGroup style doesn't appears in
        // getCapabilities resp if mode is not single nor opaque.
        String layerGroupName = "aLayerGroup";
        createLayerGroup(layerGroupName);
        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
        Document dom = WMSTestSupport.transform(req, tr);

        // default the style should be present
        String lgStyleName =
                XPATH.evaluate("/WMT_MS_Capabilities/Capability/Layer/Layer/Style/Title", dom);
        assertEquals("aLayerGroup style", lgStyleName);
        WMS wms =
                new WMS(geosConfig) {
                    @Override
                    public boolean isDefaultGroupStyleEnabled() {
                        return false;
                    }
                };
        GetCapabilitiesTransformer tr2 =
                new GetCapabilitiesTransformer(wms, baseUrl, mapFormats, legendFormats, null);
        Document dom2 = WMSTestSupport.transform(req, tr2);
        lgStyleName =
                XPATH.evaluate("/WMT_MS_Capabilities/Capability/Layer/Layer/Style/Title", dom2);
        // the style won't appear
        assertEquals(lgStyleName, "");
    }

    @Test
    public void testDisableDefaultLayerGroupStyle1_3() throws Exception {
        // test that when defaultGroupStyleEnabled
        // is set to false the default layerGroup style doesn't appears in
        // getCapabilities resp if mode is not single nor opaque.
        String layerGroupName = "aLayerGroup";
        createLayerGroup(layerGroupName);

        Capabilities_1_3_0_Transformer tr =
                new Capabilities_1_3_0_Transformer(
                        wmsConfig,
                        baseUrl,
                        wmsConfig.getAllowedMapFormats(),
                        wmsConfig.getAvailableExtendedCapabilitiesProviders());
        Document dom = WMSTestSupport.transform(req, tr);

        // default case, the style should be present
        NodeList nodeList = dom.getElementsByTagName("Style");
        assertEquals(1, nodeList.getLength());
        Element styleEl = (Element) nodeList.item(0);
        String title = styleEl.getElementsByTagName("Title").item(0).getTextContent();
        assertEquals("aLayerGroup style", title);
        WMS wms =
                new WMS(geosConfig) {
                    @Override
                    public boolean isDefaultGroupStyleEnabled() {
                        return false;
                    }
                };
        Capabilities_1_3_0_Transformer tr2 =
                new Capabilities_1_3_0_Transformer(
                        wms,
                        baseUrl,
                        wms.getAllowedMapFormats(),
                        wms.getAvailableExtendedCapabilitiesProviders());
        Document dom2 = WMSTestSupport.transform(req, tr2);
        nodeList = dom2.getElementsByTagName("Style");
        // the style won't appear
        assertEquals(0, nodeList.getLength());
    }

    @Test
    public void testDefaultLayerGroupStyle1_1ModeSingle() throws Exception {
        // test that when defaultGroupStyleEnabled
        // is set to false the default layerGroup style appears in
        // getCapabilities resp if mode is single.
        String layerGroupName = "aLayerGroup";
        createLayerGroup(layerGroupName, LayerGroupInfo.Mode.SINGLE);
        WMS wms =
                new WMS(geosConfig) {
                    @Override
                    public boolean isDefaultGroupStyleEnabled() {
                        return false;
                    }
                };
        GetCapabilitiesTransformer tr2 =
                new GetCapabilitiesTransformer(wms, baseUrl, mapFormats, legendFormats, null);
        Document dom2 = WMSTestSupport.transform(req, tr2);
        // the style should appear
        String lgStyleTitle =
                XPATH.evaluate("/WMT_MS_Capabilities/Capability/Layer/Layer/Style/Title", dom2);
        assertEquals(lgStyleTitle, "aLayerGroup style");

        String lgStyleName =
                XPATH.evaluate("/WMT_MS_Capabilities/Capability/Layer/Layer/Style/Name", dom2);
        assertEquals(lgStyleName, "default-style-aLayerGroup");
    }

    @Test
    public void testDefaultLayerGroupStyle1_3ModeOpaque() throws Exception {
        // test that when defaultGroupStyleEnabled
        // is set to false the default layerGroup style appears in
        // getCapabilities resp if mode is opaque.
        String layerGroupName = "aLayerGroup";
        createLayerGroup(layerGroupName, LayerGroupInfo.Mode.OPAQUE_CONTAINER);
        WMS wms =
                new WMS(geosConfig) {
                    @Override
                    public boolean isDefaultGroupStyleEnabled() {
                        return false;
                    }
                };
        Capabilities_1_3_0_Transformer tr =
                new Capabilities_1_3_0_Transformer(
                        wms,
                        baseUrl,
                        wmsConfig.getAllowedMapFormats(),
                        wmsConfig.getAvailableExtendedCapabilitiesProviders());
        Document dom = WMSTestSupport.transform(req, tr);

        // the style should appear
        NodeList nodeList = dom.getElementsByTagName("Style");
        assertEquals(1, nodeList.getLength());
        Element styleEl = (Element) nodeList.item(0);
        String title = styleEl.getElementsByTagName("Title").item(0).getTextContent();
        assertEquals("aLayerGroup style", title);
        String name = styleEl.getElementsByTagName("Name").item(0).getTextContent();
        assertEquals("default-style-aLayerGroup", name);
    }

    @Test
    public void testDefaultLayerAbstract() throws Exception {
        WMS wms = new WMS(getGeoServer());

        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wms, baseUrl, mapFormats, legendFormats, null);
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        // verify that abstract in document matches layer with abstract set
        String layerName = CiteTestData.PRIMITIVEGEOFEATURE.getLocalPart();
        LayerInfo l = getCatalog().getLayerByName(layerName);
        assertFalse(Strings.isNullOrEmpty(l.getAbstract()));

        Element abstractEl = findAbstractForLayerWithName(layerName, dom);
        assertNotNull(abstractEl);
        assertEquals(l.getAbstract(), abstractEl.getFirstChild().getTextContent());

        // verify that abstract in document is empty when layer has no abstract
        layerName = CiteTestData.WORLD.getLocalPart();
        l = getCatalog().getLayerByName(layerName);
        assertTrue(Strings.isNullOrEmpty(l.getAbstract()));

        abstractEl = findAbstractForLayerWithName(layerName, dom);
        assertNotNull(abstractEl);
        assertNull(abstractEl.getFirstChild());
    }

    @Test
    public void testDefaultLayerGroupAbstract() throws Exception {
        String layerGroupName = "myLayerGroup";
        createLayerGroup(layerGroupName);

        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wmsConfig, baseUrl, mapFormats, legendFormats, null);
        tr.setIndentation(2);
        Document dom = WMSTestSupport.transform(req, tr);

        Element abstractEl = findAbstractForLayerWithName(layerGroupName, dom);
        assertNotNull(abstractEl);
        assertNull(abstractEl.getFirstChild());
    }

    /** Helper to fetch the abstract for a given layer in the capabilities doc. */
    private Element findAbstractForLayerWithName(String name, Document dom) {
        NodeList nodeList = dom.getElementsByTagName("Layer");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element && node.getLocalName().equals("Layer")) {
                Element el = (Element) node;
                Node title = getFirstElementByTagName(el, "Title");
                if (title != null
                        && title.getFirstChild() != null
                        && name.equals(title.getFirstChild().getTextContent())) {
                    return getFirstElementByTagName(el, "Abstract");
                }
            }
        }
        return null;
    }

    private void createLayerGroup(String layerGroupName) throws FactoryException {
        createLayerGroup(layerGroupName, LayerGroupInfo.Mode.NAMED);
    }

    private void createLayerGroup(String layerGroupName, LayerGroupInfo.Mode mode)
            throws FactoryException {
        StyleInfo styleInfo = this.catalog.getFactory().createStyle();
        styleInfo.setName("testStyle");
        styleInfo.setFilename("testStyle.sld");
        this.catalog.add(styleInfo);

        NamespaceInfo namespaceInfo = this.catalog.getFactory().createNamespace();
        namespaceInfo.setURI("http://test");
        namespaceInfo.setPrefix("test");
        this.catalog.add(namespaceInfo);

        WorkspaceInfo workspaceInfo = this.catalog.getFactory().createWorkspace();
        workspaceInfo.setName("testDatastore");
        this.catalog.add(workspaceInfo);

        WMSStoreInfo wmsStoreInfo = this.catalog.getFactory().createWebMapServer();
        wmsStoreInfo.setName("testDatastore");
        wmsStoreInfo.setWorkspace(workspaceInfo);
        this.catalog.add(wmsStoreInfo);

        WMSLayerInfo wmsLayerInfo = this.catalog.getFactory().createWMSLayer();
        wmsLayerInfo.setName("testDatastore:testLayer");
        wmsLayerInfo.setStore(wmsStoreInfo);
        wmsLayerInfo.setNamespace(namespaceInfo);
        this.catalog.add(wmsLayerInfo);

        LayerInfo layerInfo = this.catalog.getFactory().createLayer();
        layerInfo.setDefaultStyle(styleInfo);
        layerInfo.setResource(wmsLayerInfo);
        this.catalog.add(layerInfo);

        CoordinateReferenceSystem nativeCrs = CRS.decode("EPSG:4326", true);
        ReferencedEnvelope nativeBounds = new ReferencedEnvelope(-180, 180, -90, 90, nativeCrs);

        LayerGroupInfo layerGroupInfo = this.catalog.getFactory().createLayerGroup();
        layerGroupInfo.setName(layerGroupName);
        layerGroupInfo.setBounds(nativeBounds);
        layerGroupInfo.setMode(mode);
        layerGroupInfo.getLayers().add(layerInfo);
        this.catalog.add(layerGroupInfo);
    }
}
