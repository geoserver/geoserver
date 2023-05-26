/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.geoserver.data.test.CiteTestData.LAKES;
import static org.geoserver.data.test.CiteTestData.PRIMITIVEGEOFEATURE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.Serializable;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import javax.xml.namespace.QName;
import org.apache.commons.codec.binary.Base64;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.gml3.v3_2.GML;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.SimpleInternationalString;
import org.geotools.wfs.v2_0.WFS;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DescribeFeatureTypeTest extends WFS20TestSupport {

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {
        DataStoreInfo di = getCatalog().getDataStoreByName(CiteTestData.CITE_PREFIX);
        di.setEnabled(false);
        getCatalog().save(di);

        // prepare to run a test against a real database
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("h2");
        WorkspaceInfo ws = cat.getDefaultWorkspace();
        ds.setWorkspace(ws);
        ds.setEnabled(true);

        Map<String, Serializable> params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        File dbFile = new File(getTestData().getDataDirectoryRoot().getAbsolutePath(), "data/h2");
        params.put("database", dbFile.getAbsolutePath());
        cat.add(ds);

        SimpleFeatureSource fsp = getFeatureSource(LAKES);

        DataStore store = (DataStore) ds.getDataStore(null);
        store.createSchema(fsp.getSchema());
        SimpleFeatureStore featureStore =
                (SimpleFeatureStore) store.getFeatureSource(LAKES.getLocalPart());
        featureStore.addFeatures(fsp.getFeatures());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);
        FeatureTypeInfo tft = cb.buildFeatureType(featureStore);
        cat.add(tft);

        // add the annotation
        JDBCDataStore jds = (JDBCDataStore) ds.getDataStore(null);
        try (Connection cx = jds.getConnection(Transaction.AUTO_COMMIT);
                Statement st = cx.createStatement()) {
            st.execute("COMMENT ON COLUMN \"Lakes\".\"NAME\" IS 'This is a text column'");
        }

        // force rebuilding the feature type cache for this store
        getCatalog().getResourcePool().clear(ds);
    }

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        super.setUpNamespaces(namespaces);
        namespaces.put("soap", "http://www.w3.org/2003/05/soap-envelope");
    }

    @Before
    public void resetLayers() throws Exception {
        revertLayer(PRIMITIVEGEOFEATURE);
    }

    @Test
    public void testGet() throws Exception {
        String typeName = getLayerId(PRIMITIVEGEOFEATURE);
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName="
                                + typeName);
        assertThat(response.getContentType(), is("application/gml+xml; version=3.2"));
        assertThat(
                response.getHeader(HttpHeaders.CONTENT_DISPOSITION),
                CoreMatchers.containsString("filename=sf-PrimitiveGeoFeature.xsd"));
        Document doc = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        assertSchema(doc, PRIMITIVEGEOFEATURE);
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
        String typeName = getLayerId(PRIMITIVEGEOFEATURE);
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeNames="
                                + typeName);
        assertThat(response.getContentType(), is("application/gml+xml; version=3.2"));
        Document doc = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        assertSchema(doc, PRIMITIVEGEOFEATURE);
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
        String typeName = getLayerId(PRIMITIVEGEOFEATURE);
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
                        assertSchema(doc, PRIMITIVEGEOFEATURE);
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
        String typeName = getLayerId(PRIMITIVEGEOFEATURE);
        String xml =
                "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
                        + "xmlns:wfs='http://www.opengis.net/wfs/2.0' "
                        + "xmlns:sf='"
                        + PRIMITIVEGEOFEATURE.getNamespaceURI()
                        + "'>"
                        + " <wfs:TypeName>"
                        + typeName
                        + "</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>";

        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        assertThat(response.getContentType(), is("application/gml+xml; version=3.2"));
        Document doc = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        assertSchema(doc, PRIMITIVEGEOFEATURE);
        // override GML 3.2 MIME type with text / xml
        setGmlMimeTypeOverride("text/xml");
        response = postAsServletResponse("wfs", xml);
        assertThat(response.getContentType(), is("text/xml"));
    }

    @Test
    public void testConcurrentPost() throws Exception {
        String typeName = getLayerId(PRIMITIVEGEOFEATURE);
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
                        assertSchema(doc, PRIMITIVEGEOFEATURE);
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

        String typeName = getLayerId(PRIMITIVEGEOFEATURE);
        String xml =
                "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
                        + "xmlns:wfs='http://www.opengis.net/wfs/2.0' "
                        + "xmlns:sf='"
                        + PRIMITIVEGEOFEATURE.getNamespaceURI()
                        + "'>"
                        + " <wfs:TypeName>"
                        + typeName
                        + "</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>";

        Document doc = postAsDOM("wfs", xml);
        assertSchema(doc, PRIMITIVEGEOFEATURE);

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
        String typeName = getLayerId(PRIMITIVEGEOFEATURE);
        String xml =
                "<wfs:DescribeFeatureType service='WFS' version='2.0.0' "
                        + "xmlns:wfs='http://www.opengis.net/wfs/2.0'>"
                        + " <wfs:TypeName>"
                        + typeName
                        + "</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>";
        Document doc = postAsDOM("wfs", xml);

        // with previous code missing namespace would have resulted in a service exception
        assertSchema(doc, PRIMITIVEGEOFEATURE);
    }

    @Test
    public void testMultipleTypesImport() throws Exception {
        String typeName1 = getLayerId(PRIMITIVEGEOFEATURE);
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

        assertSchema(doc, PRIMITIVEGEOFEATURE, CiteTestData.GENERICENTITY);

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

            // first, non cite compliant mode should find the type even if namespace is not
            // specified
            service.setCiteCompliant(false);
            geoServer.save(service);
            Document doc = getAsDOM(path);
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
                                + getLayerId(PRIMITIVEGEOFEATURE));
        assertSchema(dom, PRIMITIVEGEOFEATURE);
        XMLAssert.assertXpathNotExists("//xsd:element[@name = 'name']", dom);
        XMLAssert.assertXpathNotExists("//xsd:element[@name = 'description']", dom);

        gml.setOverrideGMLAttributes(true);
        dom =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&request=DescribeFeatureType"
                                + "&typename="
                                + getLayerId(PRIMITIVEGEOFEATURE));
        assertSchema(dom, PRIMITIVEGEOFEATURE);
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
                        + PRIMITIVEGEOFEATURE.getNamespaceURI()
                        + "'>"
                        + " <wfs:TypeName>"
                        + getLayerId(PRIMITIVEGEOFEATURE)
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
        String typeName = getLayerId(PRIMITIVEGEOFEATURE);

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wfs?service=WFS&version=2.0.0&request=DescribeFeatureType&typeNames="
                                + typeName);
        assertThat(response.getContentType(), is("application/gml+xml; version=3.2"));

        Document doc = dom(response, true);

        assertSchema(doc, PRIMITIVEGEOFEATURE);
        assertXpathNotExists("//xsd:import[@namespace='" + WFS.NAMESPACE + "']", doc);
    }

    @Test
    public void testCustomizeFeatureType() throws Exception {
        // customize feature type
        String layerId = getLayerId(PRIMITIVEGEOFEATURE);
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(layerId);
        // dynamically compute attributes
        List<AttributeTypeInfo> attributes = fti.attributes();

        // customize and set statically
        attributes.get(0).setName("abstract"); // rename
        attributes.get(0).setSource("description");
        attributes.get(0).setDescription(new SimpleInternationalString("attribute description"));
        attributes.remove(2); // remove
        AttributeTypeInfo att = getCatalog().getFactory().createAttribute();
        att.setName("new");
        att.setSource("Concatenate(name, 'abcd')");
        attributes.add(att);
        fti.getAttributes().addAll(attributes);
        getCatalog().save(fti);

        // check DFT
        String path =
                "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName=" + layerId;
        Document doc = getAsDOM(path);
        assertXpathEvaluatesTo("xsd:string", "//xsd:element[@name='abstract']/@type", doc);
        assertXpathNotExists("//xsd:element[@name='surfaceProperty']", doc);
        assertXpathEvaluatesTo("xsd:string", "//xsd:element[@name='new']/@type", doc);
        assertXpathEvaluatesTo(
                "attribute description",
                "//xsd:element[@name='abstract']/xsd:annotation/xsd:documentation",
                doc);
    }

    @Test
    public void describeH2Table() throws Exception {
        String layerId = getCatalog().getDefaultWorkspace().getName() + ":" + LAKES.getLocalPart();
        String path =
                "ows?service=WFS&version=2.0.0&request=DescribeFeatureType&typeName=" + layerId;
        Document doc = getAsDOM(path);

        // check the column description is setup as expected
        assertXpathEvaluatesTo("xsd:string", "//xsd:element[@name='NAME']/@type", doc);
        assertXpathEvaluatesTo(
                "This is a text column",
                "//xsd:element[@name='NAME']/xsd:annotation/xsd:documentation",
                doc);
    }
}
