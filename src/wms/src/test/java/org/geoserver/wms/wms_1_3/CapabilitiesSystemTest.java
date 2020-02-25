/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.xsd.XML;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

/**
 * WMS 1.3 GetCapabilities system tests, following clauses in section 7.2 of <a
 * href="http://portal.opengeospatial.org/files/?artifact_id=14416">OGC document 06-042, OpenGIS Web
 * Map Service (WMS) Implementation Specification</a> as requirements specification.
 *
 * <p>These are system tests in the sense that they verify specific system requirements as stated in
 * the various specification clauses. Yet they base on the same integration testing framework
 * established for other GeoServer tests in order to be able of mocking up a given configuration and
 * hence not needing a fully functional instance running inside a servlet container in order to do
 * some minimal system testing.
 *
 * <p>For tests that do not check adherence to specific spec clauses but that verify integration
 * aspects under different configuration situations use {@link CapabilitiesIntegrationTest} instead.
 *
 * @author Gabriel Roldan
 */
public class CapabilitiesSystemTest extends WMSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");

        NamespaceContext ctx = new SimpleNamespaceContext(namespaces);
        XMLUnit.setXpathNamespaceContext(ctx);

        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);

        LayerInfo layer = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        MetadataLinkInfo mdlink = getCatalog().getFactory().createMetadataLink();
        mdlink.setMetadataType("FGDC");
        mdlink.setContent("http://geoserver.org");
        mdlink.setType("text/xml");
        ResourceInfo resource = layer.getResource();
        resource.getMetadataLinks().add(mdlink);
        getCatalog().save(resource);

        Catalog catalog = getCatalog();
        DataStoreInfo info = catalog.getDataStoreByName(MockData.SF_PREFIX);
        info.setEnabled(false);
        catalog.save(info);
    }

    /** As for section 7.2.4.1, ensures the GeCapabilities document validates against its schema */
    @Test
    public void testValidateCapabilitiesXML() throws Exception {
        final Document dom = getAsDOM("ows?service=WMS&version=1.3.0&request=GetCapabilities");

        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        URL schemaLocation = getClass().getResource("/schemas/wms/1.3.0/capabilities_1_3_0.xsd");

        factory.setResourceResolver(
                new LSResourceResolver() {

                    public LSInput resolveResource(
                            String type,
                            String namespaceURI,
                            String publicId,
                            String systemId,
                            String baseURI) {

                        if (namespaceURI.equals("http://www.w3.org/1999/xlink")) {
                            try {
                                LSInput input =
                                        ((DOMImplementationLS) dom.getImplementation())
                                                .createLSInput();
                                URL xlink = getClass().getResource("/schemas/xlink/1999/xlink.xsd");
                                systemId = xlink.toURI().toASCIIString();
                                input.setPublicId(publicId);
                                input.setSystemId(systemId);
                                return input;
                            } catch (Exception e) {
                                return null;
                            }
                        } else if (XML.NAMESPACE.equals(namespaceURI)) {
                            try {
                                LSInput input =
                                        ((DOMImplementationLS) dom.getImplementation())
                                                .createLSInput();
                                URL xml = XML.class.getResource("xml.xsd");
                                systemId = xml.toURI().toASCIIString();
                                input.setPublicId(publicId);
                                input.setSystemId(systemId);
                                return input;
                            } catch (Exception e) {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }
                });
        Schema schema = factory.newSchema(schemaLocation);

        Validator validator = schema.newValidator();
        Source source = new DOMSource(dom);
        try {
            validator.validate(source);
            assertTrue(true);
        } catch (SAXException ex) {
            LOGGER.log(Level.WARNING, "WMS 1.3.0 capabilities validation error", ex);
            print(dom);
            fail("WMS 1.3.0 capabilities validation error: " + ex.getMessage());
        }
    }

    /**
     * Tests that GeoServer performs the server side of version number negotiation as defined in
     * section 6.2.4
     *
     * <p>This tests assumes the only versions supported are 1.1.1 and 1.3.0, and shall be modified
     * accordingly whenever support for another version is added, or support for one of the existing
     * versions is removed
     */
    @Test
    public void testRequestVersionNumberNegotiation() throws Exception {
        Document dom;
        /*
         * In response to a GetCapabilities request (for which the VERSION parameter is optional)
         * that does not specify a version number, the server shall respond with the highest version
         * it supports.
         */
        dom = getAsDOM("ows?service=WMS&request=GetCapabilities");
        assertXpathEvaluatesTo("1.3.0", "/wms:WMS_Capabilities/@version", dom);

        /*
         * A specific version is requested, the server responds with that specific version iif
         * supported
         */
        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.1.1");
        assertXpathEvaluatesTo("1.1.1", "/WMT_MS_Capabilities/@version", dom);

        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.3.0");
        assertXpathEvaluatesTo("1.3.0", "/wms:WMS_Capabilities/@version", dom);

        /*
         * If a version unknown to the server and higher than the lowest supported version is
         * requested, the server shall send the highest version it supports that is less than the
         * requested version.
         */
        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.1.2");
        assertXpathEvaluatesTo("1.1.1", "/WMT_MS_Capabilities/@version", dom);

        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.4.0");
        assertXpathEvaluatesTo("1.3.0", "/wms:WMS_Capabilities/@version", dom);

        /*
         * If a version lower than any of those known to the server is requested, then the server
         * shall send the lowest version it supports.
         */
        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.0.0");
        assertXpathEvaluatesTo("1.1.1", "/WMT_MS_Capabilities/@version", dom);
    }

    /**
     * Section 7.2.3.1, FORMAT is an optional parameter, all services shall support {@code text/xml}
     * , if a non supported format is requested, the default {@code text/xml} representation shall
     * be returned. The response Content-Type header shall also be {@code text/xml}
     */
    @Test
    public void testRequestOptionalFormatParameter() throws Exception {
        MockHttpServletResponse response;
        String path = "ows?service=WMS&request=GetCapabilities&version=1.3.0";
        response = getAsServletResponse(path);
        assertEquals("WMS_Capabilities", getAsDOM(path).getDocumentElement().getNodeName());
        assertEquals("text/xml", response.getContentType());

        path = "ows?service=WMS&request=GetCapabilities&version=1.3.0&format=text/xml";
        response = getAsServletResponse(path);
        assertEquals("WMS_Capabilities", getAsDOM(path).getDocumentElement().getNodeName());
        assertEquals("text/xml", response.getContentType());

        path =
                "ows?service=WMS&request=GetCapabilities&version=1.3.0&format=application/unsupported";
        response = getAsServletResponse(path);
        assertEquals("WMS_Capabilities", getAsDOM(path).getDocumentElement().getNodeName());
        assertEquals("text/xml", response.getContentType());
    }

    /** Section 7.2.3.3, SERVICE is a mandatory parameter with fixed value {@code WMS} */
    @Test
    public void testRequestMandatoryServiceParameter() throws Exception {
        Document dom = getAsDOM("ows?request=GetCapabilities&version=1.3.0");
        // print(dom);
        assertXpathEvaluatesTo(
                "InvalidParameterValue", "/ows:ExceptionReport/ows:Exception/@exceptionCode", dom);
    }

    /**
     * Section 7.2.3.4, REQUEST is a mandatory parameter with fixed value {@code GetCapabilities}
     * (case insensitive for GeoServer, spec doesn't tell)
     */
    @Test
    public void testRequestMandatoryRequestParameter() throws Exception {
        Document dom = getAsDOM("ows?request=GetCapabilities&version=1.3.0");
        // print(dom);
        assertXpathEvaluatesTo(
                "InvalidParameterValue", "/ows:ExceptionReport/ows:Exception/@exceptionCode", dom);
    }

    /**
     * Section 7.2.3.5, table 4: response handling if client specifies UPDATESEQUENCE
     *
     * <p>This test assumes GeoServer delivers numeric (integer) {@code updateSequence} values
     */
    @Test
    public void testRequestUpdateSequence() throws Exception {
        Document dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.3.0");
        final XpathEngine xpath = XMLUnit.newXpathEngine();
        final String locationPath = "/wms:WMS_Capabilities/@updateSequence";
        final String updateSeq = xpath.evaluate(locationPath, dom);

        final int currUpdateSeq = Integer.parseInt(updateSeq);

        /*
         * Client: none, Server: any, response: current
         */
        dom = getAsDOM("ows?service=WMS&request=GetCapabilities&version=1.3.0");
        assertXpathEvaluatesTo(updateSeq, locationPath, dom);

        /*
         * Client: any, Server: none, response: current. Can't implement this as a test cause
         * GeoServer will always have an internal update sequence
         */
        // can't test

        /*
         * Client: equal, Server: equal, response: exception code=CurrentUpdateSequence
         */
        dom =
                getAsDOM(
                        "ows?service=WMS&request=GetCapabilities&version=1.3.0&updateSequence="
                                + updateSeq);
        // print(dom);
        assertXpathEvaluatesTo("1.3.0", "/ogc:ServiceExceptionReport/@version", dom);
        assertXpathEvaluatesTo(
                "CurrentUpdateSequence",
                "/ogc:ServiceExceptionReport/ogc:ServiceException/@code",
                dom);

        /*
         * Client: lower, Server: higher, response: current
         */
        dom =
                getAsDOM(
                        "ows?service=WMS&request=GetCapabilities&version=1.3.0&updateSequence="
                                + (currUpdateSeq - 1));
        assertXpathEvaluatesTo(updateSeq, locationPath, dom);

        /*
         * Client: higher, Server: lower, response: exception code=InvalidUpdateSequence
         */
        dom =
                getAsDOM(
                        "ows?service=WMS&request=GetCapabilities&version=1.3.0&updateSequence="
                                + (currUpdateSeq + 1));
        assertXpathEvaluatesTo(
                "InvalidUpdateSequence",
                "/ogc:ServiceExceptionReport/ogc:ServiceException/@code",
                dom);
    }
}
