/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DescribeFeatureTest extends WFSTestSupport {

    @Before
    public void revert() throws Exception {
        revertLayer(CiteTestData.AGGREGATEGEOFEATURE);
    }

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {
        DataStoreInfo di = getCatalog().getDataStoreByName(CiteTestData.CITE_PREFIX);
        di.setEnabled(false);
        getCatalog().save(di);
    }

    @Test
    public void testGet() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=DescribeFeatureType&version=1.0.0");
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testSkipMisconfiguredLayers() throws Exception {
        // make sure AggregateGeoFeature is in the mock data set
        Document doc = getAsDOM("wfs?service=WFS&request=DescribeFeatureType&version=1.0.0");
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//xsd:import[contains(@schemaLocation, 'AggregateGeoFeature')])", doc);

        // enable skipping of misconfigured layers
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);

        // misconfigure a layer
        FeatureTypeInfo ftype =
                getCatalog().getFeatureTypeByName(CiteTestData.AGGREGATEGEOFEATURE.getLocalPart());
        ftype.setNativeName("NOT ACTUALLY THERE");
        getCatalog().save(ftype);

        // check the results again
        doc = getAsDOM("wfs?service=WFS&request=DescribeFeatureType&version=1.0.0");
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo(
                "0", "count(//xsd:import[contains(@schemaLocation, 'AggregateGeoFeature')])", doc);
    }

    @Test
    public void testPost() throws Exception {
        String xml =
                "<wfs:DescribeFeatureType "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" />";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testPostDummyFeature() throws Exception {

        String xml =
                "<wfs:DescribeFeatureType "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" >"
                        + " <wfs:TypeName>cgf:DummyFeature</wfs:TypeName>"
                        + "</wfs:DescribeFeatureType>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testWithoutExplicitMapping() throws Exception {
        String xml =
                "<DescribeFeatureType xmlns='http://www.opengis.net/wfs'"
                        + " xmlns:gml='http://www.opengis.net/gml'"
                        + " xmlns:ogc='http://www.opengis.net/ogc' version='1.0.0' service='WFS'>"
                        + " <TypeName>cdf:Locks</TypeName>"
                        + " </DescribeFeatureType>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());

        assertEquals(1, doc.getElementsByTagName("xsd:complexType").getLength());
    }

    @Test
    public void testWithoutTypeName() throws Exception {
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&version=1.0.0");
        NodeList nl = doc.getElementsByTagName("xsd:import");
        assertEquals(3, nl.getLength());

        HashMap<String, HashMap<String, String>> imprts = new HashMap();
        for (int i = 0; i < nl.getLength(); i++) {
            Element imprt = (Element) nl.item(i);
            String namespace = imprt.getAttribute("namespace");
            String schemaLocation = imprt.getAttribute("schemaLocation");
            int query = schemaLocation.indexOf("?");

            schemaLocation = schemaLocation.substring(query + 1);
            String[] sp = schemaLocation.split("&");
            HashMap params = new HashMap();
            for (int j = 0; j < sp.length; j++) {
                String[] sp1 = sp[j].split("=");
                params.put(sp1[0].toLowerCase(), sp1[1].toLowerCase());
            }

            imprts.put(namespace, params);
        }

        String[] expected =
                new String[] {CiteTestData.SF_URI, CiteTestData.CDF_URI, CiteTestData.CGF_URI};
        for (String namespace : expected) {
            assertNotNull(imprts.get(namespace));
            HashMap params = imprts.get(namespace);
            assertEquals("wfs", params.get("service"));
            assertEquals("1.0.0", params.get("version"));
            assertEquals("describefeaturetype", params.get("request"));

            String types = (String) params.get("typename");
            assertNotNull(types);

            Catalog cat = getCatalog();
            NamespaceInfo ns = cat.getNamespaceByURI(namespace);
            // System.out.println(ns.getPrefix());
            // %2c == , (url-encoded, comma is not considered a safe char)
            assertEquals(cat.getFeatureTypesByNamespace(ns).size(), types.split("%2c").length);
        }
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        Document doc = getAsDOM("sf/wfs?request=DescribeFeatureType&version=1.0.0");

        NodeList nl = doc.getElementsByTagName("xsd:complexType");
        assertEquals(3, nl.getLength());

        XMLAssert.assertXpathExists("//xsd:complexType[@name = 'AggregateGeoFeatureType']", doc);
        XMLAssert.assertXpathExists("//xsd:complexType[@name = 'PrimitiveGeoFeatureType']", doc);
        XMLAssert.assertXpathExists("//xsd:complexType[@name = 'GenericEntityType']", doc);

        doc = getAsDOM("sf/wfs?request=DescribeFeatureType&version=1.0.0&typename=cdf:Fifteen");
        XMLAssert.assertXpathExists("//ogc:ServiceException", doc);
    }

    @Test
    public void testMultipleNamespaceNoTargetNamespace() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=DescribeFeatureType&version=1.0.0&typeName=sf:PrimitiveGeoFeature,cgf:Points");
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        assertFalse(doc.getDocumentElement().hasAttribute("targetNamespace"));
    }

    @Test
    public void testMethodNameInjection() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&version=1.0.0"
                                + "&request=DescribeFeatureType%22%3E%3C/ServiceException%3E%3Cfoo%3EHello,%20World%3C/foo%3E%3CServiceException+foo=%22"
                                + "&typeName=sf:archsites");
        // print(dom);

        // check we have a valid exception
        XMLAssert.assertXpathExists("/ogc:ServiceExceptionReport/ogc:ServiceException", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "OperationNotSupported",
                "/ogc:ServiceExceptionReport/ogc:ServiceException/@code",
                dom);
        // the locator has been escaped
        XMLAssert.assertXpathEvaluatesTo(
                "DescribeFeatureType\"></ServiceException><foo>Hello, World</foo><ServiceException foo=\"",
                "/ogc:ServiceExceptionReport/ogc:ServiceException/@locator",
                dom);
        // the attack failed and the foo element is not there
        XMLAssert.assertXpathNotExists("//foo", dom);
    }
}
