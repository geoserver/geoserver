/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.opengis.ows10.GetCapabilitiesType;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.eclipse.emf.common.util.EList;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.kvp.GetCapabilitiesKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.csw.CSWConfiguration;
import org.geotools.filter.v1_1.OGC;
import org.geotools.xlink.XLINK;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

public class GetCapabilitiesTest extends CSWSimpleTestSupport {

    static XpathEngine xpath = XMLUnit.newXpathEngine();

    static {
        Map<String, String> prefixMap = new HashMap<String, String>();
        prefixMap.put("ows", OWS.NAMESPACE);
        prefixMap.put("ogc", OGC.NAMESPACE);
        prefixMap.put("gml", "http://www.opengis.net/gml");
        prefixMap.put("gmd", "http://www.isotc211.org/2005/gmd");
        prefixMap.put("xlink", XLINK.NAMESPACE);
        NamespaceContext nameSpaceContext = new SimpleNamespaceContext(prefixMap);
        xpath.setNamespaceContext(nameSpaceContext);
    }

    @Test
    public void testKVPReader() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "CSW");
        raw.put("request", "GetCapabilities");
        raw.put("acceptVersions", "2.0.2,2.0.0,0.7.2");
        raw.put("sections", "OperationsMetadata,foo");
        raw.put("acceptFormats", "application/xml,text/plain");

        GetCapabilitiesKvpRequestReader reader = new GetCapabilitiesKvpRequestReader();
        Object request = reader.createRequest();
        GetCapabilitiesType caps = (GetCapabilitiesType) reader.read(request, parseKvp(raw), raw);
        assertReturnedCapabilitiesComplete(caps);
    }

    private void assertReturnedCapabilitiesComplete(GetCapabilitiesType caps) {
        assertNotNull(caps);

        EList versions = caps.getAcceptVersions().getVersion();
        assertEquals(3, versions.size());
        assertEquals("2.0.2", versions.get(0));
        assertEquals("2.0.0", versions.get(1));
        assertEquals("0.7.2", versions.get(2));

        EList sections = caps.getSections().getSection();
        assertEquals(2, sections.size());
        assertEquals("OperationsMetadata", sections.get(0));
        assertEquals("foo", sections.get(1));

        EList outputFormats = caps.getAcceptFormats().getOutputFormat();
        assertEquals(2, outputFormats.size());
        assertEquals("application/xml", outputFormats.get(0));
        assertEquals("text/plain", outputFormats.get(1));
    }

    @Test
    public void testXMLReader() throws Exception {
        CSWXmlReader reader =
                new CSWXmlReader(
                        "GetCapabilities",
                        "2.0.2",
                        new CSWConfiguration(),
                        EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
        GetCapabilitiesType caps =
                (GetCapabilitiesType)
                        reader.read(null, getResourceAsReader("GetCapabilities.xml"), (Map) null);
        assertReturnedCapabilitiesComplete(caps);
    }

    @Test
    public void testXMLReaderInvalid() throws Exception {
        // create a schema invalid request
        String capRequest = getResourceAsString("GetCapabilities.xml");
        capRequest = capRequest.replace("ows:Sections", "ows:foo");
        try {
            CSWXmlReader reader =
                    new CSWXmlReader(
                            "GetCapabilities",
                            "2.0.2",
                            new CSWConfiguration(),
                            EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
            reader.read(null, new StringReader(capRequest), (Map) null);
            fail("the parsing should have failed, the document is invalid");
        } catch (ServiceException e) {
            // it is a validation exception right?
            assertTrue(e.getCause() instanceof SAXParseException);
            SAXParseException cause = (SAXParseException) e.getCause();
            System.out.println(cause.getMessage());
            // JDK8 and JDK11 return slightly different message
            assertTrue(cause.getMessage().matches(".*ows.?:foo.*"));
        }
    }

    @Test
    public void testGetBasic() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetCapabilities");
        // print(dom);
        checkValidationErrors(dom);

        // basic check on local name
        Element e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());

        // basic check on xpath node
        assertXpathEvaluatesTo("1", "count(/csw:Capabilities)", dom);

        assertTrue(
                xpath.getMatchingNodes("//ows:OperationsMetadata/ows:Operation", dom).getLength()
                        > 0);
        assertEquals("5", xpath.evaluate("count(//ows:Operation)", dom));

        // basic check on GetCapabilities operation constraint
        assertEquals(
                "XML",
                xpath.evaluate(
                        "//ows:OperationsMetadata/ows:Operation[@name=\"GetCapabilities\"]/ows:Constraint/ows:Value",
                        dom));

        // check we have csw:AnyText among the queriables
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:Operation[@name='GetRecords']/ows:Constraint[@name='SupportedDublinCoreQueryables' and ows:Value = 'csw:AnyText'])",
                dom);

        // check we have dc:subject among the domain property names
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:Operation[@name='GetDomain']/ows:Parameter[@name='PropertyName' and ows:Value = 'dc:title'])",
                dom);
    }

    @Test
    public void testPostBasic() throws Exception {
        Document dom = postAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetCapabilities");
        // print(dom);
        checkValidationErrors(dom);

        // basic check on local name
        Element e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());

        // basic check on xpath node
        assertXpathEvaluatesTo("1", "count(/csw:Capabilities)", dom);

        assertTrue(
                xpath.getMatchingNodes("//ows:OperationsMetadata/ows:Operation", dom).getLength()
                        > 0);
        assertEquals("5", xpath.evaluate("count(//ows:Operation)", dom));

        // basic check on GetCapabilities operation constraint
        assertEquals(
                "XML",
                xpath.evaluate(
                        "//ows:OperationsMetadata/ows:Operation[@name=\"GetCapabilities\"]/ows:Constraint/ows:Value",
                        dom));
    }

    @Test
    public void testVirtualService() throws Exception {
        List<CSWInfo> infos = GeoServerExtensions.extensions(CSWInfo.class);

        Catalog catalog = getCatalog();
        GeoServer geoServer = getGeoServer();

        CSWInfo localCSW = new CSWInfoImpl();
        localCSW.setName("localCSW");
        localCSW.setWorkspace(catalog.getWorkspaceByName("gs"));
        localCSW.setAbstract("Local CSW");

        CSWInfo globalCSW = geoServer.getService(CSWInfo.class);
        globalCSW.setAbstract("Global CSW");

        geoServer.add(localCSW);
        geoServer.save(globalCSW);

        // Test global abstract
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetCapabilities");
        checkValidationErrors(dom);
        assertEquals("Global CSW", xpath.evaluate("//ows:ServiceIdentification/ows:Abstract", dom));

        // Test local abstract
        dom = getAsDOM("gs/" + BASEPATH + "?service=csw&version=2.0.2&request=GetCapabilities");
        checkValidationErrors(dom);
        assertEquals("Local CSW", xpath.evaluate("//ows:ServiceIdentification/ows:Abstract", dom));
    }

    @Test
    public void testSections() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?service=csw&version=2.0.2&request=GetCapabilities&sections=ServiceIdentification,ServiceProvider");
        // print(dom);
        checkValidationErrors(dom);

        // basic check on local name
        Element e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());

        // basic check on xpath node
        assertXpathEvaluatesTo("1", "count(/csw:Capabilities)", dom);
        assertEquals("1", xpath.evaluate("count(//ows:ServiceIdentification)", dom));
        assertEquals("1", xpath.evaluate("count(//ows:ServiceProvider)", dom));
        assertEquals("0", xpath.evaluate("count(//ows:OperationsMetadata)", dom));
        // this one is mandatory, cannot be skipped
        assertEquals("1", xpath.evaluate("count(//ogc:Filter_Capabilities)", dom));

        assertTrue(
                xpath.getMatchingNodes("//ows:OperationsMetadata/ows:Operation", dom).getLength()
                        == 0);
        assertEquals("0", xpath.evaluate("count(//ows:Operation)", dom));
    }

    @Test
    public void testCiteCompliance() throws Exception {
        CSWInfo csw = getGeoServer().getService(CSWInfo.class);
        try {
            csw.setCiteCompliant(true);
            getGeoServer().save(csw);

            Document dom = getAsDOM(BASEPATH + "?request=GetCapabilities");
            checkOws10Exception(dom, ServiceException.MISSING_PARAMETER_VALUE, "service");
        } finally {
            csw.setCiteCompliant(false);
            getGeoServer().save(csw);
        }
    }
}
