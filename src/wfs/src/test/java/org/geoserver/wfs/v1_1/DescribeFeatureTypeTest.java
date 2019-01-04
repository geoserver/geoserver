/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URLEncoder;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.util.IOUtils;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.gml3.GML;
import org.geotools.wfs.v1_1.WFS;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DescribeFeatureTypeTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {
        DataStoreInfo di = getCatalog().getDataStoreByName(CiteTestData.CITE_PREFIX);
        di.setEnabled(false);
        getCatalog().save(di);

        File root = dataDirectory.getDataDirectoryRoot();
        File otherDir = new File(root, "workspaces/cdf/cdf/Other");
        otherDir.mkdirs();
        File otherSchema = new File(otherDir, "schema.xsd");
        IOUtils.copy(getClass().getResourceAsStream("others.xsd"), otherSchema);
    }

    @Test
    public void testDateMappings() throws Exception {

        String xml =
                "<wfs:DescribeFeatureType "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:sf=\""
                        + CiteTestData.PRIMITIVEGEOFEATURE.getNamespaceURI()
                        + "\">"
                        + " <wfs:TypeName>sf:"
                        + CiteTestData.PRIMITIVEGEOFEATURE.getLocalPart()
                        + "</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>";

        Document doc = postAsDOM("wfs", xml);
        // print( doc );
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());

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
        String xml =
                "<wfs:DescribeFeatureType "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\">"
                        + " <wfs:TypeName>sf:"
                        + CiteTestData.PRIMITIVEGEOFEATURE.getLocalPart()
                        + "</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>";

        Document doc = postAsDOM("wfs", xml);
        // print( doc );

        // with previous code missing namespace would have resulted in a service exception
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testMultipleTypesImport() throws Exception {
        String xml =
                "<wfs:DescribeFeatureType " //
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:sf=\""
                        + CiteTestData.PRIMITIVEGEOFEATURE.getNamespaceURI()
                        + "\">" //
                        + "<wfs:TypeName>sf:"
                        + CiteTestData.PRIMITIVEGEOFEATURE.getLocalPart() //
                        + "</wfs:TypeName>" //
                        + "<wfs:TypeName>sf:"
                        + CiteTestData.GENERICENTITY.getLocalPart() //
                        + "</wfs:TypeName>" //
                        + "</wfs:DescribeFeatureType>";
        Document doc = postAsDOM("wfs", xml);
        // print(doc);
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
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
    public void testUerSuppliedTypeNameNamespace() throws Exception {
        final QName typeName = CiteTestData.POLYGONS;
        String path =
                "ows?service=WFS&version=1.1.0&request=DescribeFeatureType&typeName=myPrefix:"
                        + typeName.getLocalPart()
                        + "&namespace=xmlns(myPrefix%3D"
                        + URLEncoder.encode(typeName.getNamespaceURI(), "UTF-8")
                        + ")";
        Document doc = getAsDOM(path);
        // print(doc);
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-3306 */
    @Test
    public void testUerSuppliedTypeNameDefaultNamespace() throws Exception {
        final QName typeName = CiteTestData.POLYGONS;
        String path =
                "ows?service=WFS&version=1.1.0&request=DescribeFeatureType&typeName="
                        + typeName.getLocalPart()
                        + "&namespace=xmlns("
                        + URLEncoder.encode(typeName.getNamespaceURI(), "UTF-8")
                        + ")";
        Document doc = getAsDOM(path);
        // print(doc);
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testMissingNameNamespacePrefix() throws Exception {
        final QName typeName = CiteTestData.POLYGONS;
        String path =
                "ows?service=WFS&version=1.1.0&request=DescribeFeatureType&typeName="
                        + typeName.getLocalPart();
        Document doc = getAsDOM(path);
        // print(doc);
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
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
                    "ows?service=WFS&version=1.1.0&request=DescribeFeatureType&typeName="
                            + typeName.getLocalPart();
            Document doc;

            // first, non cite compliant mode should find the type even if namespace is not
            // specified
            service.setCiteCompliant(false);
            geoServer.save(service);
            doc = getAsDOM(path);
            // print(doc);
            assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());

            // then, in cite compliance more, it should not find the type name
            service.setCiteCompliant(true);
            geoServer.save(service);
            doc = getAsDOM(path);
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
                    "ows?service=WFS&version=1.1.0&request=DescribeFeatureType&typeName="
                            + getLayerId(typeName);
            Document doc = getAsDOM(path);
            // print(doc);
            assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        } finally {
            service.setCiteCompliant(false);
            geoServer.save(service);
        }
    }

    @Test
    public void testGML32OutputFormat() throws Exception {
        Document dom =
                getAsDOM(
                        "ows?service=WFS&version=1.1.0&request=DescribeFeatureType"
                                + "&outputFormat=text/xml;+subtype%3Dgml/3.2&typename="
                                + getLayerId(CiteTestData.POLYGONS));
        print(dom);
    }

    @Test
    public void testGMLAttributeMapping() throws Exception {
        WFSInfo wfs = getWFS();
        GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_11);
        gml.setOverrideGMLAttributes(false);
        getGeoServer().save(wfs);

        Document dom =
                getAsDOM(
                        "ows?service=WFS&version=1.1.0&request=DescribeFeatureType"
                                + "&typename="
                                + getLayerId(CiteTestData.PRIMITIVEGEOFEATURE));
        XMLAssert.assertXpathNotExists("//xsd:element[@name = 'name']", dom);
        XMLAssert.assertXpathNotExists("//xsd:element[@name = 'description']", dom);

        wfs = getWFS();
        gml = wfs.getGML().get(WFSInfo.Version.V_11);
        gml.setOverrideGMLAttributes(true);
        getGeoServer().save(wfs);
        wfs = getWFS();
        gml = wfs.getGML().get(WFSInfo.Version.V_11);
        assertTrue(gml.getOverrideGMLAttributes());
        dom =
                getAsDOM(
                        "ows?service=WFS&version=1.1.0&request=DescribeFeatureType"
                                + "&typename="
                                + getLayerId(CiteTestData.PRIMITIVEGEOFEATURE));
        XMLAssert.assertXpathExists("//xsd:element[@name = 'name']", dom);
        XMLAssert.assertXpathExists("//xsd:element[@name = 'description']", dom);
    }

    @Test
    public void testCustomSchema() throws Exception {
        Document dom =
                getAsDOM(
                        "ows?request=DescribeFeatureType&version=1.1.0&service=WFS&typeName=cdf:Other");
        XMLAssert.assertXpathExists("//xsd:element[@name = 'pointProperty']", dom);
        XMLAssert.assertXpathExists("//xsd:element[@name = 'string1']", dom);
        XMLAssert.assertXpathExists("//xsd:element[@name = 'string2']", dom);
        XMLAssert.assertXpathNotExists("//xsd:element[@name = 'integers']", dom);
        XMLAssert.assertXpathNotExists("//xsd:element[@name = 'dataTime']", dom);
    }

    //    OUR CURRENT TEST HARNESS DOES NOT SUPPORT CONCURRENT TESTING...
    //    public void testConcurrentDescribe() throws Exception {
    //        ExecutorService es = Executors.newFixedThreadPool(8);
    //        List<Future<Void>> results = new ArrayList<Future<Void>>();
    //        for(int i = 0; i < 24; i++) {
    //            Future<Void> future = es.submit(new Callable<Void>() {
    //
    //                @Override
    //                public Void call() throws Exception {
    //                    Document dom =
    // getAsDOM("ows?request=DescribeFeatureType&version=1.1.0&service=WFS&typeName=cdf:Deletes,cdf:Seven,cdf:Locks,cdf:Nulls,cdf:Other,cdf:Inserts,cdf:Fifteen,cdf:Updates");
    //                    System.out.println(dom);
    //                    XMLAssert.assertXpathEvaluatesTo("8", "count(//xsd:ComplexType)", dom);
    //                    return null;
    //                }
    //
    //            });
    //            results.add(future);
    //        }
    //
    //        // make sure none threw an exception
    //        for (Future<Void> future : results) {
    //            future.get();
    //        }
    //    }

    /** Tests that WFS schema is not imported in a DescribeFeatureType response. */
    @Test
    public void testNoWfsSchemaImport() throws Exception {
        final String typeName = CiteTestData.POLYGONS.getLocalPart();
        String path =
                "ows?service=WFS&version=1.1.0&request=DescribeFeatureType&typeName=" + typeName;
        Document doc = getAsDOM(path);

        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        assertXpathExists("//xsd:complexType[@name='" + typeName + "Type']", doc);
        assertXpathExists("//xsd:element[@name='" + typeName + "']", doc);
        assertXpathExists("//xsd:import[@namespace='" + GML.NAMESPACE + "']", doc);
        assertXpathNotExists("//xsd:import[@namespace='" + WFS.NAMESPACE + "']", doc);
    }
}
