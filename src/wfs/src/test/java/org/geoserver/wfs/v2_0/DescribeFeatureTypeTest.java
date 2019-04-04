/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import javax.xml.namespace.QName;
import org.apache.commons.codec.binary.Base64;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geotools.gml3.v3_2.GML;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DescribeFeatureTypeTest extends WFS20TestSupport {

    //    @Override
    //    protected String getLogConfiguration() {
    //        return "/DEFAULT_LOGGING.properties";
    //    }

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {
        DataStoreInfo di = getCatalog().getDataStoreByName(CiteTestData.CITE_PREFIX);
        di.setEnabled(false);
        getCatalog().save(di);
    }

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        super.setUpNamespaces(namespaces);
        namespaces.put("soap", "http://www.w3.org/2003/05/soap-envelope");
    }

    @Test
    public void testGet() throws Exception {
        String typeName = getLayerId(CiteTestData.PRIMITIVEGEOFEATURE);
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                                + typeName);
        assertThat(response.getContentType(), is("application/gml+xml; version=3.2"));
        Document doc = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        assertSchema(doc, CiteTestData.PRIMITIVEGEOFEATURE);
        // override GML 3.2 MIME type with text / xml
        setGmlMimeTypeOverride("text/xml");
        response =
                getAsServletResponse(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                                + typeName);
        assertThat(response.getContentType(), is("text/xml"));
    }

    @Test
    public void testGetPluralKey() throws Exception {
        // the WFS 2.0 spec is contracting itself, says "typename" in a table and "typenames" just
        // below
        // current CITE tests typenames is used
        String typeName = getLayerId(CiteTestData.PRIMITIVEGEOFEATURE);
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeNames="
                                + typeName);
        assertThat(response.getContentType(), is("application/gml+xml; version=3.2"));
        Document doc = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        assertSchema(doc, CiteTestData.PRIMITIVEGEOFEATURE);
        // override GML 3.2 MIME type with text / xml
        setGmlMimeTypeOverride("text/xml");
        response =
                getAsServletResponse(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                                + typeName);
        assertThat(response.getContentType(), is("text/xml"));
    }

    @Test
    public void testConcurrentGet() throws Exception {
        String typeName = getLayerId(CiteTestData.PRIMITIVEGEOFEATURE);
        ExecutorCompletionService<Object> es =
                new ExecutorCompletionService<>(
                        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        final int REQUESTS = 200;
        for (int i = 0; i < REQUESTS; i++) {
            es.submit(
                    () -> {
                        Document doc =
                                getAsDOM(
                                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                                                + typeName);
                        assertSchema(doc, CiteTestData.PRIMITIVEGEOFEATURE);
                        return null;
                    });
        }
        // just check there are no exceptions
        for (int i = 0; i < REQUESTS; i++) {
            es.take().get();
        }
    }

    @Test
    public void testPost() throws Exception {
        String typeName = getLayerId(CiteTestData.PRIMITIVEGEOFEATURE);
        String xml =
                "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
                        + "xmlns:wfs='http://www.opengis.net/wfs/2.0' "
                        + "xmlns:sf='"
                        + CiteTestData.PRIMITIVEGEOFEATURE.getNamespaceURI()
                        + "'>"
                        + " <wfs:TypeName>"
                        + typeName
                        + "</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>";

        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        assertThat(response.getContentType(), is("application/gml+xml; version=3.2"));
        Document doc = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        assertSchema(doc, CiteTestData.PRIMITIVEGEOFEATURE);
        // override GML 3.2 MIME type with text / xml
        setGmlMimeTypeOverride("text/xml");
        response = postAsServletResponse("wfs", xml);
        assertThat(response.getContentType(), is("text/xml"));
    }

    @Test
    public void testConcurrentPost() throws Exception {
        String typeName = getLayerId(CiteTestData.PRIMITIVEGEOFEATURE);
        ExecutorCompletionService<Object> es =
                new ExecutorCompletionService<>(
                        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        final int REQUESTS = 200;
        for (int i = 0; i < REQUESTS; i++) {
            es.submit(
                    () -> {
                        Document doc =
                                getAsDOM(
                                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                                                + typeName);
                        assertSchema(doc, CiteTestData.PRIMITIVEGEOFEATURE);
                        return null;
                    });
        }
        // just check there are no exceptions
        for (int i = 0; i < REQUESTS; i++) {
            long start = System.currentTimeMillis();
            es.take().get();
            if (i % 100 == 0) {
                long curr = System.currentTimeMillis();
                LOGGER.info(i + " - " + (curr - start));
                start = curr;
            }
        }
    }

    void assertSchema(Document doc, QName... types) throws Exception {
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathExists("//xsd:import[@namespace='" + GML.NAMESPACE + "']", doc);

        for (QName type : types) {
            String eName = type.getLocalPart();
            String tName = eName + "Type";

            XMLAssert.assertXpathEvaluatesTo(
                    "1", "count(//xsd:complexType[@name='" + tName + "'])", doc);
            XMLAssert.assertXpathEvaluatesTo(
                    "1", "count(//xsd:element[@name='" + eName + "'])", doc);
        }
    }

    @Test
    public void testDateMappings() throws Exception {

        String typeName = getLayerId(CiteTestData.PRIMITIVEGEOFEATURE);
        String xml =
                "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
                        + "xmlns:wfs='http://www.opengis.net/wfs/2.0' "
                        + "xmlns:sf='"
                        + CiteTestData.PRIMITIVEGEOFEATURE.getNamespaceURI()
                        + "'>"
                        + " <wfs:TypeName>"
                        + typeName
                        + "</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>";

        Document doc = postAsDOM("wfs", xml);
        assertSchema(doc, CiteTestData.PRIMITIVEGEOFEATURE);

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

    @Test
    public void testNoNamespaceDeclaration() throws Exception {
        String typeName = getLayerId(CiteTestData.PRIMITIVEGEOFEATURE);
        String xml =
                "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
                        + "xmlns:wfs='http://www.opengis.net/wfs/2.0'>"
                        + " <wfs:TypeName>"
                        + typeName
                        + "</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>";
        Document doc = postAsDOM("wfs", xml);

        // with previous code missing namespace would have resulted in a service exception
        assertSchema(doc, CiteTestData.PRIMITIVEGEOFEATURE);
    }

    @Test
    public void testMultipleTypesImport() throws Exception {
        String typeName1 = getLayerId(CiteTestData.PRIMITIVEGEOFEATURE);
        String typeName2 = getLayerId(CiteTestData.GENERICENTITY);
        String xml =
                "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
                        + "xmlns:wfs='http://www.opengis.net/wfs/2.0'>"
                        + " <wfs:TypeName>"
                        + typeName1
                        + "</wfs:TypeName>"
                        + " <wfs:TypeName>"
                        + typeName2
                        + "</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>";

        Document doc = postAsDOM("wfs", xml);

        assertSchema(doc, CiteTestData.PRIMITIVEGEOFEATURE, CiteTestData.GENERICENTITY);

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

    /** See https://osgeo-org.atlassian.net/browse/GEOS-3306 */
    @Test
    public void testUserSuppliedTypeNameNamespace() throws Exception {
        final QName typeName = CiteTestData.POLYGONS;
        String path =
                "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&"
                        + "typeName=myPrefix:"
                        + typeName.getLocalPart()
                        + "&namespaces=xmlns(myPrefix,"
                        + URLEncoder.encode(typeName.getNamespaceURI(), "UTF-8")
                        + ")";

        Document doc = getAsDOM(path);
        assertSchema(doc, CiteTestData.POLYGONS);
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-3306 */
    @Test
    public void testUserSuppliedTypeNameDefaultNamespace() throws Exception {
        final QName typeName = CiteTestData.POLYGONS;
        String path =
                "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&"
                        + "typeName="
                        + typeName.getLocalPart()
                        + "&namespace=xmlns("
                        + URLEncoder.encode(typeName.getNamespaceURI(), "UTF-8")
                        + ")";

        Document doc = getAsDOM(path);
        assertSchema(doc, CiteTestData.POLYGONS);
    }

    @Test
    public void testMissingNameNamespacePrefix() throws Exception {
        final QName typeName = CiteTestData.POLYGONS;
        String path =
                "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                        + typeName.getLocalPart();
        Document doc = getAsDOM(path);
        assertSchema(doc, CiteTestData.POLYGONS);
    }

    /**
     * Under cite compliance mode, even if the requested typeName is not qualified and it does exist
     * in the GeoServer's default namespace, the lookup should fail, since the request does not
     * addresses the typeName either by qualifying it as declared in the getcaps document, or
     * providing an alternate prefix with its corresponding prefix to namespace mapping.
     */
    @Test
    public void testCiteCompliance() throws Exception {
        final QName typeName = CiteTestData.STREAMS;
        // make sure typeName _is_ in the default namespace
        Catalog catalog = getCatalog();
        NamespaceInfo defaultNs = catalog.getDefaultNamespace();

        GeoServer geoServer = getGeoServer();
        WFSInfo service = geoServer.getService(WFSInfo.class);
        try {
            // make sure typeName _is_ in the default namespace
            catalog.setDefaultNamespace(catalog.getNamespaceByURI(typeName.getNamespaceURI()));
            FeatureTypeInfo typeInfo =
                    catalog.getFeatureTypeByName(
                            typeName.getNamespaceURI(), typeName.getLocalPart());
            typeInfo.setEnabled(true);
            catalog.save(typeInfo);
            DataStoreInfo store = typeInfo.getStore();
            store.setEnabled(true);
            catalog.save(store);

            // and request typeName without prefix
            String path =
                    "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                            + typeName.getLocalPart();
            Document doc;

            // first, non cite compliant mode should find the type even if namespace is not
            // specified
            service.setCiteCompliant(false);
            geoServer.save(service);
            doc = getAsDOM(path);
            print(doc);
            assertSchema(doc, typeName);

            // then, in cite compliance more, it should not find the type name
            service.setCiteCompliant(true);
            geoServer.save(service);
            doc = getAsDOM(path, 400);
            // print(doc);
            assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
        } finally {
            catalog.setDefaultNamespace(defaultNs);
            service.setCiteCompliant(false);
            geoServer.save(service);
        }
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-3306 */
    @Test
    public void testPrefixedGetStrictCite() throws Exception {
        GeoServer geoServer = getGeoServer();
        WFSInfo service = geoServer.getService(WFSInfo.class);
        try {
            service.setCiteCompliant(true);
            geoServer.save(service);

            final QName typeName = CiteTestData.POLYGONS;
            String path =
                    "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                            + getLayerId(typeName);
            Document doc = getAsDOM(path);
            assertSchema(doc, CiteTestData.POLYGONS);
        } finally {
            service.setCiteCompliant(false);
            geoServer.save(service);
        }
    }

    @Test
    public void testGML32OutputFormat() throws Exception {
        Document dom =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&request=DescribeFeatureType"
                                + "&outputFormat=text/xml;+subtype%3Dgml/3.2&typename="
                                + getLayerId(CiteTestData.POLYGONS));
        assertSchema(dom, CiteTestData.POLYGONS);
    }

    @Test
    public void testGMLAttributeMapping() throws Exception {
        WFSInfo wfs = getWFS();
        GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_11);
        gml.setOverrideGMLAttributes(false);
        getGeoServer().save(wfs);

        Document dom =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&request=DescribeFeatureType"
                                + "&typename="
                                + getLayerId(CiteTestData.PRIMITIVEGEOFEATURE));
        assertSchema(dom, CiteTestData.PRIMITIVEGEOFEATURE);
        XMLAssert.assertXpathNotExists("//xsd:element[@name = 'name']", dom);
        XMLAssert.assertXpathNotExists("//xsd:element[@name = 'description']", dom);

        gml.setOverrideGMLAttributes(true);
        dom =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&request=DescribeFeatureType"
                                + "&typename="
                                + getLayerId(CiteTestData.PRIMITIVEGEOFEATURE));
        assertSchema(dom, CiteTestData.PRIMITIVEGEOFEATURE);
        XMLAssert.assertXpathExists("//xsd:element[@name = 'name']", dom);
        XMLAssert.assertXpathExists("//xsd:element[@name = 'description']", dom);
    }

    @Test
    public void testSOAP() throws Exception {
        String xml =
                "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> "
                        + " <soap:Header/> "
                        + " <soap:Body>"
                        + "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
                        + "xmlns:wfs='http://www.opengis.net/wfs/2.0' "
                        + "xmlns:sf='"
                        + CiteTestData.PRIMITIVEGEOFEATURE.getNamespaceURI()
                        + "'>"
                        + " <wfs:TypeName>"
                        + getLayerId(CiteTestData.PRIMITIVEGEOFEATURE)
                        + "</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>"
                        + " </soap:Body> "
                        + "</soap:Envelope> ";

        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", resp.getContentType());

        Document dom = dom(new ByteArrayInputStream(resp.getContentAsString().getBytes()));
        assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
        print(dom);
        XMLAssert.assertXpathEvaluatesTo("xsd:base64", "//soap:Body/@type", dom);
        assertEquals(1, dom.getElementsByTagName("wfs:DescribeFeatureTypeResponse").getLength());

        String base64 =
                dom.getElementsByTagName("wfs:DescribeFeatureTypeResponse")
                        .item(0)
                        .getFirstChild()
                        .getNodeValue();
        byte[] decoded = Base64.decodeBase64(base64.getBytes());
        dom = dom(new ByteArrayInputStream(decoded));
        assertEquals("xsd:schema", dom.getDocumentElement().getNodeName());
    }

    /** Tests that WFS schema is not imported in a DescribeFeatureType response. */
    @Test
    public void testNoWfsSchemaImport() throws Exception {
        String typeName = getLayerId(CiteTestData.PRIMITIVEGEOFEATURE);

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeNames="
                                + typeName);
        assertThat(response.getContentType(), is("application/gml+xml; version=3.2"));

        Document doc = dom(response, true);

        assertSchema(doc, CiteTestData.PRIMITIVEGEOFEATURE);
        assertXpathNotExists("//xsd:import[@namespace='" + WFS.NAMESPACE + "']", doc);
    }
}
