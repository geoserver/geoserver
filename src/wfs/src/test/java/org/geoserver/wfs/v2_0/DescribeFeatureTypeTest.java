package org.geoserver.wfs.v2_0;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geotools.gml3.v3_2.GML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DescribeFeatureTypeTest extends WFS20TestSupport {

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        super.setUpNamespaces(namespaces);
        namespaces.put("soap", "http://www.w3.org/2003/05/soap-envelope");
    }
    
    @Override
    protected void setUpInternal()throws Exception{
      getTestData().disableDataStore(MockData.CITE_PREFIX);  
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.disableDataStore(MockData.CITE_PREFIX);
    }
    
    public void testGet() throws Exception {
        String typeName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        Document doc = getAsDOM(
            "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName=" + typeName);
        assertSchema(doc, MockData.PRIMITIVEGEOFEATURE);
    }

    public void testPost() throws Exception {
        String typeName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        String xml = "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
          + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " 
          + "xmlns:sf='" + MockData.PRIMITIVEGEOFEATURE.getNamespaceURI() + "'>" 
          + " <wfs:TypeName>" + typeName + "</wfs:TypeName>"
          + "</wfs:DescribeFeatureType>";
        
        Document doc = postAsDOM("wfs", xml);
        assertSchema(doc, MockData.PRIMITIVEGEOFEATURE);
    }
    
    void assertSchema(Document doc, QName... types) throws Exception {
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathExists("//xsd:import[@namespace='" + GML.NAMESPACE + "']", doc);
        
        for (QName type : types) {
            String eName = type.getLocalPart();
            String tName = eName + "Type";
            
            XMLAssert.assertXpathEvaluatesTo("1", "count(//xsd:complexType[@name='" + tName + "'])", doc);
            XMLAssert.assertXpathEvaluatesTo("1", "count(//xsd:element[@name='" + eName + "'])", doc);
        }
    }
    
    public void testDateMappings() throws Exception {

        String typeName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        String xml = "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
          + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " 
          + "xmlns:sf='" + MockData.PRIMITIVEGEOFEATURE.getNamespaceURI() + "'>" 
          + " <wfs:TypeName>" + typeName + "</wfs:TypeName>"
          + "</wfs:DescribeFeatureType>";
        
        Document doc = postAsDOM("wfs", xml);
        assertSchema(doc, MockData.PRIMITIVEGEOFEATURE);
        
        NodeList elements = doc.getElementsByTagName("xsd:element");
        boolean date = false;
        boolean dateTime = false;

        for (int i = 0; i < elements.getLength(); i++) {
            Element e = (Element) elements.item(i);
            if ("dateProperty".equals(e.getAttribute("name"))) {
                date = "xsd:date".equals(e.getAttribute("type"));
            }
            if ("dateTimeProperty".equals(e.getAttribute("name"))) {
                dateTime = "xsd:dateTime".equals(e.getAttribute("type"));
            }
        }

        assertTrue(date);
        assertTrue(dateTime);
    }

    public void testNoNamespaceDeclaration() throws Exception {
        String typeName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        String xml = "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
          + "xmlns:wfs='http://www.opengis.net/wfs/2.0'>" 
          + " <wfs:TypeName>" + typeName + "</wfs:TypeName>"
          + "</wfs:DescribeFeatureType>";
        Document doc = postAsDOM("wfs", xml);

        // with previous code missing namespace would have resulted in a service exception
        assertSchema(doc, MockData.PRIMITIVEGEOFEATURE);
    }

    public void testMultipleTypesImport() throws Exception {
        String typeName1 = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        String typeName2 = getLayerId(MockData.GENERICENTITY);
        String xml = "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
          + "xmlns:wfs='http://www.opengis.net/wfs/2.0'>" 
          + " <wfs:TypeName>" + typeName1 + "</wfs:TypeName>"
          + " <wfs:TypeName>" + typeName2 + "</wfs:TypeName>"
          + "</wfs:DescribeFeatureType>";

        Document doc = postAsDOM("wfs", xml);
        
        assertSchema(doc, MockData.PRIMITIVEGEOFEATURE, MockData.GENERICENTITY);
        
        NodeList nodes = doc.getDocumentElement().getChildNodes();
        boolean seenComplexType = false;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("xsd:complexType")) {
                seenComplexType = true;
            } else if (seenComplexType && node.getNodeName().equals("xsd:import")) {
                fail("All xsd:import must occur before all xsd:complexType");
            }
        }
    }

    /**
     * See http://jira.codehaus.org/browse/GEOS-3306
     * 
     * @throws Exception
     */
    public void testUserSuppliedTypeNameNamespace() throws Exception {
        final QName typeName = MockData.POLYGONS;
        String path = "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&" +
            "typeName=myPrefix:" + typeName.getLocalPart() + 
            "&namespace=xmlns(myPrefix%3D" + URLEncoder.encode(typeName.getNamespaceURI(), "UTF-8") + ")";
        
        Document doc = getAsDOM(path);
        assertSchema(doc, MockData.POLYGONS);
    }

    /**
     * See http://jira.codehaus.org/browse/GEOS-3306
     * 
     * @throws Exception
     */
    public void testUserSuppliedTypeNameDefaultNamespace() throws Exception {
        final QName typeName = MockData.POLYGONS;
        String path = "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&" + 
            "typeName="+ typeName.getLocalPart() + 
            "&namespace=xmlns(" + URLEncoder.encode(typeName.getNamespaceURI(), "UTF-8") + ")";
        
        Document doc = getAsDOM(path);
        assertSchema(doc, MockData.POLYGONS);
    }

    public void testMissingNameNamespacePrefix() throws Exception {
        final QName typeName = MockData.POLYGONS;
        String path = "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                + typeName.getLocalPart();
        Document doc = getAsDOM(path);
        assertSchema(doc, MockData.POLYGONS);
    }

    /**
     * Under cite compliance mode, even if the requested typeName is not qualified and it does exist
     * in the GeoServer's default namespace, the lookup should fail, since the request does not
     * addresses the typeName either by qualifying it as declared in the getcaps document, or
     * providing an alternate prefix with its corresponding prefix to namespace mapping.
     * 
     * @throws Exception
     */
    public void testCiteCompliance() throws Exception {
        final QName typeName = MockData.STREAMS;

        // make sure typeName _is_ in the default namespace
        Catalog catalog = getCatalog();
        catalog.setDefaultNamespace(catalog.getNamespaceByURI(typeName.getNamespaceURI()));
        FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());
        typeInfo.setEnabled(true);
        catalog.save(typeInfo);
        DataStoreInfo store = typeInfo.getStore();
        store.setEnabled(true);
        catalog.save(store);
        
        // and request typeName without prefix
        String path = "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
            + typeName.getLocalPart();
        Document doc;
    
        //first, non cite compliant mode should find the type even if namespace is not specified
        GeoServer geoServer = getGeoServer();
        WFSInfo service = geoServer.getService(WFSInfo.class);
        service.setCiteCompliant(false);
        geoServer.save(service);
        doc = getAsDOM(path);
        print(doc);
        assertSchema(doc, typeName);
        

        //then, in cite compliance more, it should not find the type name
        service.setCiteCompliant(true);
        geoServer.save(service);
        doc = getAsDOM(path);
        //print(doc);
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }
    
    /**
     * See http://jira.codehaus.org/browse/GEOS-3306
     * 
     * @throws Exception
     */
    public void testPrefixedGetStrictCite() throws Exception {
        GeoServer geoServer = getGeoServer();
        WFSInfo service = geoServer.getService(WFSInfo.class);
        service.setCiteCompliant(true);
        geoServer.save(service);
        
        final QName typeName = MockData.POLYGONS;
        String path = "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                + getLayerId(typeName);
        Document doc = getAsDOM(path);
        assertSchema(doc, MockData.POLYGONS);
    }

    public void testGML32OutputFormat() throws Exception {
        Document dom = getAsDOM("ows?service=WFS&version=2.0.0&request=DescribeFeatureType" +
            "&outputFormat=text/xml;+subtype%3Dgml/3.2&typename=" + getLayerId(MockData.POLYGONS));
        assertSchema(dom, MockData.POLYGONS);
    }
    
    public void testGMLAttributeMapping() throws Exception {
        WFSInfo wfs = getWFS();
        GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_11);
        gml.setOverrideGMLAttributes(false);
        getGeoServer().save(wfs);
        
        Document dom = getAsDOM("ows?service=WFS&version=2.0.0&request=DescribeFeatureType" +
                "&typename=" + getLayerId(MockData.PRIMITIVEGEOFEATURE));
        assertSchema(dom, MockData.PRIMITIVEGEOFEATURE);
        XMLAssert.assertXpathNotExists("//xsd:element[@name = 'name']", dom);
        XMLAssert.assertXpathNotExists("//xsd:element[@name = 'description']", dom);
        
        gml.setOverrideGMLAttributes(true);
        dom = getAsDOM("ows?service=WFS&version=2.0.0&request=DescribeFeatureType" +
                "&typename=" + getLayerId(MockData.PRIMITIVEGEOFEATURE));
        assertSchema(dom, MockData.PRIMITIVEGEOFEATURE);
        XMLAssert.assertXpathExists("//xsd:element[@name = 'name']", dom);
        XMLAssert.assertXpathExists("//xsd:element[@name = 'description']", dom);
    }

    public void testSOAP() throws Exception {
        String xml = 
           "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> " + 
                " <soap:Header/> " + 
                " <soap:Body>"
                + "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
                + "xmlns:wfs='http://www.opengis.net/wfs/2.0' " 
                + "xmlns:sf='" + MockData.PRIMITIVEGEOFEATURE.getNamespaceURI() + "'>" 
                + " <wfs:TypeName>" + getLayerId(MockData.PRIMITIVEGEOFEATURE) + "</wfs:TypeName>"
                + "</wfs:DescribeFeatureType>" + 
                " </soap:Body> " + 
            "</soap:Envelope> "; 
              
        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", resp.getContentType());
        
        Document dom = dom(new ByteArrayInputStream(resp.getOutputStreamContent().getBytes()));
        assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("xsd:base64", "//soap:Body/@type", dom);
        assertEquals(1, dom.getElementsByTagName("wfs:DescribeFeatureTypeResponse").getLength());
        
        String base64 = dom.getElementsByTagName("wfs:DescribeFeatureTypeResponse").item(0)
            .getFirstChild().getNodeValue();
        byte[] decoded = Base64.decodeBase64(base64.getBytes());
        dom = dom(new ByteArrayInputStream(decoded));
        assertEquals("xsd:schema", dom.getDocumentElement().getNodeName());
    }
}
