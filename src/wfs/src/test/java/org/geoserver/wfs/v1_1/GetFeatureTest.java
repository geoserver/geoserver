/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.xml.namespace.QName;
import net.sf.json.JSONObject;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.RunTestSetup;
import org.geoserver.util.XmlTestUtil;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.gml3.GML;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetFeatureTest extends WFSTestSupport {

    XmlTestUtil xmlUtil;

    @Before
    public void setupXmlUtil() {
        xmlUtil = new XmlTestUtil();
        xmlUtil.addNamespace("wfs", "http://www.opengis.net/wfs");
        xmlUtil.addNamespace("sf", "http://cite.opengeospatial.org/gmlsf");
        // xmlUtil.setShowXML(System.out); // Uncomment to display XML on failure
    }

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        data.addVectorLayer(
                new QName(SystemTestData.SF_URI, "WithGMLProperties", SystemTestData.SF_PREFIX),
                Collections.EMPTY_MAP,
                getClass(),
                getCatalog());
    }

    @Test
    public void testGet() throws Exception {
        testGetFifteenAll("wfs?request=GetFeature&typename=cdf:Fifteen&version=1.1.0&service=wfs");
    }

    @Test
    public void testGetPropertyNameEmpty() throws Exception {
        testGetFifteenAll(
                "wfs?request=GetFeature&typename=cdf:Fifteen&version=1.1.0&service=wfs&propertyname=");
    }

    @Test
    public void testGetPropertyNameStar() throws Exception {
        testGetFifteenAll(
                "wfs?request=GetFeature&typename=cdf:Fifteen&version=1.1.0&service=wfs&propertyname=*");
    }

    private void testGetFifteenAll(String request) throws Exception {
        Document doc = getAsDOM(request);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList features = doc.getElementsByTagName("cdf:Fifteen");
        assertFalse(features.getLength() == 0);

        for (int i = 0; i < features.getLength(); i++) {
            Element feature = (Element) features.item(i);
            assertTrue(feature.hasAttribute("gml:id"));
        }
    }

    // see GEOS-1287
    @Test
    @RunTestSetup
    public void testGetWithFeatureId() throws Exception {

        Document doc;
        doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName=cdf:Fifteen&version=1.1.0&service=wfs&featureid=Fifteen.2");

        // super.print(doc);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//wfs:FeatureCollection/gml:featureMembers/cdf:Fifteen)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "Fifteen.2", "//wfs:FeatureCollection/gml:featureMembers/cdf:Fifteen/@gml:id", doc);

        doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName=cite:NamedPlaces&version=1.1.0&service=wfs&featureId=NamedPlaces.1107531895891");

        // super.print(doc);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//wfs:FeatureCollection/gml:featureMembers/cite:NamedPlaces)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "NamedPlaces.1107531895891",
                "//wfs:FeatureCollection/gml:featureMembers/cite:NamedPlaces/@gml:id",
                doc);
    }

    @Test
    public void testGetWithTwoFeatureId() throws Exception {

        Document doc;
        doc =
                getAsDOM(
                        "wfs?request=GetFeature&&version=1.1.0&service=wfs&featureid=Fifteen.1,Fifteen.2");

        // print(doc);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo(
                "2", "count(//wfs:FeatureCollection/gml:featureMembers/cdf:Fifteen)", doc);

        assertXpathExists(
                "//wfs:FeatureCollection/gml:featureMembers/cdf:Fifteen[@gml:id='Fifteen.1']", doc);
        assertXpathExists(
                "//wfs:FeatureCollection/gml:featureMembers/cdf:Fifteen[@gml:id='Fifteen.2']", doc);
    }

    @Test
    public void testPost() throws Exception {

        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cdf:Other\"> "
                        + "<wfs:PropertyName>cdf:string2</wfs:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList features = doc.getElementsByTagName("cdf:Other");
        assertFalse(features.getLength() == 0);

        for (int i = 0; i < features.getLength(); i++) {
            Element feature = (Element) features.item(i);
            assertTrue(feature.hasAttribute("gml:id"));
        }
    }

    @Test
    public void testPostSOAP12() throws Exception {
        String xml =
                "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> "
                        + " <soap:Header/> "
                        + " <soap:Body>"
                        + "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cdf:Other\"> "
                        + "<wfs:PropertyName>cdf:string2</wfs:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>"
                        + "</soap:Body>"
                        + "</soap:Envelope>";

        MockHttpServletResponse response =
                postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", response.getContentType());

        Document doc = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        print(doc);

        assertXpathExists("/soap12:Envelope", doc);
        assertXpathExists("/soap12:Envelope/soap12:Body/wfs:FeatureCollection", doc);
        assertXpathEvaluatesTo(
                "1",
                "count(/soap12:Envelope/soap12:Body/wfs:FeatureCollection/gml:featureMembers/cdf:Other)",
                doc);
    }

    @Test
    public void testPostFormEncoded() throws Exception {
        String request =
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typename=sf:PrimitiveGeoFeature"
                        + "&namespace=xmlns("
                        + URLEncoder.encode("sf=http://cite.opengeospatial.org/gmlsf", "UTF-8")
                        + ")";

        Document doc = postAsDOM(request);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(5, doc.getElementsByTagName("sf:PrimitiveGeoFeature").getLength());
    }

    @Test
    public void testPostWithFilter() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "outputFormat=\"text/xml; subtype=gml/3.1.1\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" > "
                        + "<wfs:Query typeName=\"cdf:Other\"> "
                        + "<ogc:Filter> "
                        + "<ogc:PropertyIsEqualTo> "
                        + "<ogc:PropertyName>cdf:integers</ogc:PropertyName> "
                        + "<ogc:Add> "
                        + "<ogc:Literal>4</ogc:Literal> "
                        + "<ogc:Literal>3</ogc:Literal> "
                        + "</ogc:Add> "
                        + "</ogc:PropertyIsEqualTo> "
                        + "</ogc:Filter> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList features = doc.getElementsByTagName("cdf:Other");
        assertFalse(features.getLength() == 0);

        for (int i = 0; i < features.getLength(); i++) {
            Element feature = (Element) features.item(i);
            assertTrue(feature.hasAttribute("gml:id"));
        }
    }

    @Test
    public void testPostWithBboxFilter() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "outputFormat=\"text/xml; subtype=gml/3.1.1\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\" "
                        + "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" > "
                        + "<wfs:Query typeName=\"sf:PrimitiveGeoFeature\">"
                        + "<ogc:Filter>"
                        + "<ogc:BBOX>"
                        + "   <ogc:PropertyName>pointProperty</ogc:PropertyName>"
                        + "   <gml:Envelope srsName=\"EPSG:4326\">"
                        + "      <gml:lowerCorner>57.0 -4.5</gml:lowerCorner>"
                        + "      <gml:upperCorner>62.0 1.0</gml:upperCorner>"
                        + "   </gml:Envelope>"
                        + "</ogc:BBOX>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList features = doc.getElementsByTagName("sf:PrimitiveGeoFeature");
        assertEquals(1, features.getLength());
    }

    @Test
    public void testPostWithFailingUrnBboxFilter() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "outputFormat=\"text/xml; subtype=gml/3.1.1\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\" "
                        + "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" > "
                        + "<wfs:Query typeName=\"sf:PrimitiveGeoFeature\">"
                        + "<ogc:Filter>"
                        + "<ogc:BBOX>"
                        + "   <ogc:PropertyName>pointProperty</ogc:PropertyName>"
                        + "   <gml:Envelope srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
                        + "      <gml:lowerCorner>57.0 -4.5</gml:lowerCorner>"
                        + "      <gml:upperCorner>62.0 1.0</gml:upperCorner>"
                        + "   </gml:Envelope>"
                        + "</ogc:BBOX>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        NodeList features = doc.getElementsByTagName("sf:PrimitiveGeoFeature");
        assertEquals(0, features.getLength());
    }

    @Test
    public void testPostWithMatchingUrnBboxFilter() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "outputFormat=\"text/xml; subtype=gml/3.1.1\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\" "
                        + "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" > "
                        + "<wfs:Query typeName=\"sf:PrimitiveGeoFeature\">"
                        + "<ogc:Filter>"
                        + "<ogc:BBOX>"
                        + "   <ogc:PropertyName>pointProperty</ogc:PropertyName>"
                        + "   <gml:Envelope srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
                        + "      <gml:lowerCorner>-4.5 57.0</gml:lowerCorner>"
                        + "      <gml:upperCorner>1.0 62.0</gml:upperCorner>"
                        + "   </gml:Envelope>"
                        + "</ogc:BBOX>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        assertThat(doc, xmlUtil.hasNode("wfs:FeatureCollection"));
        assertThat(doc, xmlUtil.hasNode("//sf:PrimitiveGeoFeature"));
    }

    @Test
    public void testResultTypeHitsGet() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=cdf:Fifteen&version=1.1.0&resultType=hits&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList features = doc.getElementsByTagName("cdf:Fifteen");
        assertEquals(0, features.getLength());

        assertEquals("15", doc.getDocumentElement().getAttribute("numberOfFeatures"));
    }

    @Test
    public void testResultTypeHitsPost() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "outputFormat=\"text/xml; subtype=gml/3.1.1\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "resultType=\"hits\"> "
                        + "<wfs:Query typeName=\"cdf:Seven\"/> "
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList features = doc.getElementsByTagName("cdf:Fifteen");
        assertEquals(0, features.getLength());

        assertEquals("7", doc.getDocumentElement().getAttribute("numberOfFeatures"));
    }

    @Test
    public void testWithSRS() throws Exception {
        String xml =
                "<wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" version=\"1.1.0\" service=\"WFS\">"
                        + "<wfs:Query xmlns:cdf=\"http://www.opengis.net/cite/data\" typeName=\"cdf:Other\" srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\"/>"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals(1, dom.getElementsByTagName("cdf:Other").getLength());
    }

    @Test
    public void testWithSillyLiteral() throws Exception {
        String xml =
                "<wfs:GetFeature xmlns:cdf=\"http://www.opengis.net/cite/data\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" version=\"1.1.0\" service=\"WFS\">"
                        + "<wfs:Query  typeName=\"cdf:Other\" srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
                        + "<ogc:Filter>"
                        + "  <ogc:PropertyIsEqualTo>"
                        + "   <ogc:PropertyName>description</ogc:PropertyName>"
                        + "   <ogc:Literal>"
                        + "       <wfs:Native vendorId=\"foo\" safeToIgnore=\"true\"/>"
                        + "   </ogc:Literal>"
                        + "   </ogc:PropertyIsEqualTo>"
                        + " </ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals(0, dom.getElementsByTagName("cdf:Other").getLength());
    }

    @Test
    public void testWithGmlObjectId() throws Exception {
        String xml =
                "<wfs:GetFeature xmlns:cdf=\"http://www.opengis.net/cite/data\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" version=\"1.1.0\" service=\"WFS\">"
                        + "<wfs:Query  typeName=\"cdf:Seven\" srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", dom.getDocumentElement().getNodeName());
        assertEquals(7, dom.getElementsByTagName("cdf:Seven").getLength());

        NodeList others = dom.getElementsByTagName("cdf:Seven");
        String id = ((Element) others.item(0)).getAttributeNS(GML.NAMESPACE, "id");
        assertNotNull(id);

        xml =
                "<wfs:GetFeature xmlns:cdf=\"http://www.opengis.net/cite/data\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" version=\"1.1.0\" service=\"WFS\">"
                        + "<wfs:Query  typeName=\"cdf:Seven\" srsName=\"urn:x-ogc:def:crs:EPSG:6.11.2:4326\">"
                        + "<ogc:Filter>"
                        + "<ogc:GmlObjectId gml:id=\""
                        + id
                        + "\"/>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";
        dom = postAsDOM("wfs", xml);

        assertEquals(1, dom.getElementsByTagName("cdf:Seven").getLength());
    }

    @Test
    public void testPostWithBoundsEnabled() throws Exception {
        // enable feature bounds computation
        WFSInfo wfs = getWFS();
        boolean oldFeatureBounding = wfs.isFeatureBounding();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);

        try {
            String xml =
                    "<wfs:GetFeature "
                            + "service=\"WFS\" "
                            + "version=\"1.1.0\" "
                            + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                            + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                            + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                            + "> "
                            + "<wfs:Query typeName=\"cdf:Other\"> "
                            + "<wfs:PropertyName>cdf:string2</wfs:PropertyName> "
                            + "</wfs:Query> "
                            + "</wfs:GetFeature>";

            Document doc = postAsDOM("wfs", xml);
            assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

            NodeList features = doc.getElementsByTagName("cdf:Other");
            assertFalse(features.getLength() == 0);

            for (int i = 0; i < features.getLength(); i++) {
                Element feature = (Element) features.item(i);
                assertTrue(feature.hasAttribute("gml:id"));
                NodeList boundList = feature.getElementsByTagName("gml:boundedBy");
                assertEquals(1, boundList.getLength());
                Element boundedBy = (Element) boundList.item(0);
                NodeList boxList = boundedBy.getElementsByTagName("gml:Envelope");
                assertEquals(1, boxList.getLength());
                Element box = (Element) boxList.item(0);
                assertTrue(box.hasAttribute("srsName"));
            }
        } finally {
            wfs.setFeatureBounding(oldFeatureBounding);
            getGeoServer().save(wfs);
        }
    }

    @Test
    public void testAfterFeatureTypeAdded() throws Exception {
        Document dom = getAsDOM("wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:new");
        assertEquals("ExceptionReport", dom.getDocumentElement().getLocalName());

        getTestData()
                .addVectorLayer(
                        new QName(SystemTestData.SF_URI, "new", SystemTestData.SF_PREFIX),
                        Collections.EMPTY_MAP,
                        getClass(),
                        getCatalog());

        // reloadCatalogAndConfiguration();

        dom = getAsDOM("wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:new");
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
    }

    @Test
    public void testWithGMLProperties() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.1.0&typename=sf:WithGMLProperties");

        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());

        NodeList features = dom.getElementsByTagName("sf:WithGMLProperties");
        assertEquals(1, features.getLength());

        for (int i = 0; i < features.getLength(); i++) {
            Element feature = (Element) features.item(i);
            assertEquals(
                    "one",
                    getFirstElementByTagName(feature, "gml:name").getFirstChild().getNodeValue());
            assertEquals(
                    "1",
                    getFirstElementByTagName(feature, "sf:foo").getFirstChild().getNodeValue());

            Element location = getFirstElementByTagName(feature, "gml:location");
            assertNotNull(getFirstElementByTagName(location, "gml:Point"));
        }
    }

    @Test
    public void testLayerQualified() throws Exception {
        testGetFifteenAll(
                "cdf/Fifteen/wfs?request=GetFeature&typename=cdf:Fifteen&version=1.1.0&service=wfs");

        Document dom =
                getAsDOM(
                        "cdf/Seven/wfs?request=GetFeature&typename=cdf:Fifteen&version=1.1.0&service=wfs");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:ExceptionReport)", dom);
    }

    @Test
    public void testUserSuppliedNamespacePrefix() throws Exception {
        testGetFifteenAll(
                "wfs?request=GetFeature&typename=myPrefix:Fifteen&version=1.1.0&service=wfs&"
                        + "namespace=xmlns(myPrefix%3D" // the '=' sign shall be encoded, hence
                        // '%3D'
                        + URLEncoder.encode(SystemTestData.FIFTEEN.getNamespaceURI(), "UTF-8")
                        + ")");
    }

    @Test
    public void testUserSuppliedDefaultNamespace() throws Exception {
        testGetFifteenAll(
                "wfs?request=GetFeature&typename=Fifteen&version=1.1.0&service=wfs&"
                        + "namespace=xmlns("
                        + URLEncoder.encode(SystemTestData.FIFTEEN.getNamespaceURI(), "UTF-8")
                        + ")");
    }

    @Test
    public void testGML32OutputFormat() throws Exception {
        testGetFifteenAll(
                "wfs?request=getfeature&typename=cdf:Fifteen&version=1.1.0&service=wfs&outputFormat=gml32");
    }

    @Test
    public void testGMLAttributeMapping() throws Exception {
        WFSInfo wfs = getWFS();
        GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_11);
        gml.setOverrideGMLAttributes(false);
        getGeoServer().save(wfs);

        Document dom =
                getAsDOM(
                        "ows?service=WFS&version=1.1.0&request=GetFeature"
                                + "&typename="
                                + getLayerId(SystemTestData.PRIMITIVEGEOFEATURE));
        assertXpathExists("//gml:name", dom);
        assertXpathExists("//gml:description", dom);
        XMLAssert.assertXpathNotExists("//sf:name", dom);
        XMLAssert.assertXpathNotExists("//sf:description", dom);

        gml.setOverrideGMLAttributes(true);
        getGeoServer().save(wfs);

        dom =
                getAsDOM(
                        "ows?service=WFS&version=1.1.0&request=GetFeature"
                                + "&typename="
                                + getLayerId(SystemTestData.PRIMITIVEGEOFEATURE));
        XMLAssert.assertXpathNotExists("//gml:name", dom);
        XMLAssert.assertXpathNotExists("//gml:description", dom);
        assertXpathExists("//sf:name", dom);
        assertXpathExists("//sf:description", dom);

        gml.setOverrideGMLAttributes(false);
        getGeoServer().save(wfs);
    }

    @Test
    public void testSortedAscending() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&sortBy=ADDRESS");
        XMLAssert.assertXpathEvaluatesTo("2", "count(//cite:Buildings)", dom);
        XMLAssert.assertXpathEvaluatesTo("113", "//cite:Buildings[1]/cite:FID", dom);
        XMLAssert.assertXpathEvaluatesTo("114", "//cite:Buildings[2]/cite:FID", dom);

        // with max features
        dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&sortBy=ADDRESS&maxFeatures=1");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
        XMLAssert.assertXpathEvaluatesTo("113", "//cite:Buildings[1]/cite:FID", dom);

        // and with paging, first page
        dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&sortBy=ADDRESS&maxFeatures=1&startIndex=0");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
        XMLAssert.assertXpathEvaluatesTo("113", "//cite:Buildings[1]/cite:FID", dom);

        // second page
        dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&sortBy=ADDRESS&maxFeatures=1&startIndex=1");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
        XMLAssert.assertXpathEvaluatesTo("114", "//cite:Buildings[1]/cite:FID", dom);
    }

    @Test
    public void testSortedDescending() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&sortBy=ADDRESS D");
        XMLAssert.assertXpathEvaluatesTo("2", "count(//cite:Buildings)", dom);
        XMLAssert.assertXpathEvaluatesTo("114", "//cite:Buildings[1]/cite:FID", dom);
        XMLAssert.assertXpathEvaluatesTo("113", "//cite:Buildings[2]/cite:FID", dom);

        // with max features
        dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&sortBy=ADDRESS D&maxFeatures=1");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
        XMLAssert.assertXpathEvaluatesTo("114", "//cite:Buildings[1]/cite:FID", dom);

        // and with paging, first page
        dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&sortBy=ADDRESS D&maxFeatures=1&startIndex=0");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
        XMLAssert.assertXpathEvaluatesTo("114", "//cite:Buildings[1]/cite:FID", dom);

        // second page
        dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&sortBy=ADDRESS D&maxFeatures=1&startIndex=1");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//cite:Buildings)", dom);
        XMLAssert.assertXpathEvaluatesTo("113", "//cite:Buildings[1]/cite:FID", dom);
    }

    @Test
    public void testSortedInvalidAttribute() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=GetFeature&typename="
                                + getLayerId(SystemTestData.BUILDINGS)
                                + "&version=1.1.0&service=wfs&sortBy=GODOT");
        checkOws10Exception(dom, "InvalidParameterValue");
        XMLAssert.assertXpathEvaluatesTo(
                "Illegal property name: GODOT for feature type "
                        + getLayerId(SystemTestData.BUILDINGS),
                "//ows:ExceptionText",
                dom);
    }

    @Test
    public void testEncodeSrsDimension() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&service=wfs&typename="
                                + getLayerId(SystemTestData.PRIMITIVEGEOFEATURE));
        assertXpathExists("//gml:Point[@srsDimension = '2']", dom);

        setCiteCompliant(true);

        dom =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&service=wfs&typename="
                                + getLayerId(SystemTestData.PRIMITIVEGEOFEATURE));
        XMLAssert.assertXpathNotExists("//gml:Point[@srsDimension = '2']", dom);
    }

    @Test
    public void testWfs20AndGML31() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName=cdf:Fifteen&version=2.0.0&service=wfs&featureid=Fifteen.2&outputFormat=gml3");
        // print(doc);

        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//wfs:FeatureCollection/gml:featureMembers/cdf:Fifteen)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "Fifteen.2", "//wfs:FeatureCollection/gml:featureMembers/cdf:Fifteen/@gml:id", doc);
    }

    @Test
    public void testFeatureMembers() throws Exception {
        WFSInfo wfs = getWFS();
        GeoServer gs = getGeoServer();
        try {
            wfs.setEncodeFeatureMember(false);
            gs.save(wfs);

            Document dom =
                    getAsDOM(
                            "wfs?request=GetFeature&typename="
                                    + getLayerId(SystemTestData.BUILDINGS)
                                    + "&version=1.1.0&service=wfs&sortBy=ADDRESS");
            // print(dom);
            XMLAssert.assertXpathEvaluatesTo("1", "count(//gml:featureMembers)", dom);
            XMLAssert.assertXpathEvaluatesTo("0", "count(//gml:featureMember)", dom);

            wfs.setEncodeFeatureMember(true);
            gs.save(wfs);

            dom =
                    getAsDOM(
                            "wfs?request=GetFeature&typename="
                                    + getLayerId(SystemTestData.BUILDINGS)
                                    + "&version=1.1.0&service=wfs&sortBy=ADDRESS");
            // print(dom);
            XMLAssert.assertXpathEvaluatesTo("0", "count(//gml:featureMembers)", dom);
            XMLAssert.assertXpathEvaluatesTo("2", "count(//gml:featureMember)", dom);

        } finally {
            wfs.setEncodeFeatureMember(false);
            gs.save(wfs);
        }
    }

    /**
     * Test GetFeature operation invocations using virtual services end points. A common issue was
     * XSD caches created only with the types of a certain workspace and reuse the same cache for
     * other workspaces. The principal consequence was NULL namespaces in the GetFeature result, e.g
     * null:Buildings.
     */
    @Test
    public void testVirtualServicesInvocation() throws Exception {
        XpathEngine xpath = XMLUnit.newXpathEngine();
        // get rid of the current WFS schema
        org.geotools.wfs.v1_1.WFS.getInstance().dispose();
        // perform a GetFeature request using virtual service for workspace CGF
        String response =
                getAsServletResponse(
                                String.format(
                                        "%s/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=%s",
                                        SystemTestData.CGF_PREFIX,
                                        getLayerId(SystemTestData.MLINES)))
                        .getContentAsString();
        assertThat(response, notNullValue());
        assertThat(response.contains("null:MLines"), is(false));
        Document document = dom(new ByteArrayInputStream(response.getBytes()), true);
        String count = xpath.evaluate("count(//gml:featureMembers/cgf:MLines)", document);
        assertThat(Integer.parseInt(count), greaterThan(0));
        // now perform a GetFeature request using virtual service for workspace CITE
        response =
                getAsServletResponse(
                                String.format(
                                        "%s/ows?service=WFS&version=1.1.0&request=GetFeature&typeName=%s",
                                        SystemTestData.CITE_PREFIX,
                                        getLayerId(SystemTestData.BUILDINGS)))
                        .getContentAsString();
        assertThat(response, notNullValue());
        assertThat(response.contains("null:Buildings"), is(false));
        document = dom(new ByteArrayInputStream(response.getBytes()), true);
        count = xpath.evaluate("count(//gml:featureMembers/cite:Buildings)", document);
        assertThat(Integer.parseInt(count), greaterThan(0));
    }

    @Test
    public void testNPEOnPaginationLinks() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "outputFormat=\"text/xml; subtype=gml/3.1.1\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\" "
                        + "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" > "
                        + "<wfs:Query typeName=\"sf:PrimitiveGeoFeature\">"
                        + "<ogc:Filter>"
                        + "<ogc:BBOX>"
                        + "   <ogc:PropertyName>pointProperty</ogc:PropertyName>"
                        + "   <gml:Envelope srsName=\"EPSG:4326\">"
                        + "      <gml:lowerCorner>57.0 -4.5</gml:lowerCorner>"
                        + "      <gml:upperCorner>62.0 1.0</gml:upperCorner>"
                        + "   </gml:Envelope>"
                        + "</ogc:BBOX>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";
        ExecutorService executorService =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Future<Void> future =
                        executorService.submit(
                                () -> {
                                    Document doc = postAsDOM("wfs", xml);
                                    // print(doc);
                                    assertEquals(
                                            "wfs:FeatureCollection",
                                            doc.getDocumentElement().getNodeName());
                                    return (Void) null;
                                });
                futures.add(future);
            }
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void testGetWithCountAndStartIndex0() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                "wfs?request=GetFeature&typenames=cdf:Fifteen&version=1.1.0&service=wfs&maxFeatures=5&startIndex=0&outputFormat=JSON");
        assertEquals(5, json.getJSONArray("features").size());
        assertEquals(15, json.getInt("totalFeatures"));
    }

    @Test
    public void testGetWithCountAndStartIndexMiddle() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                "wfs?request=GetFeature&typenames=cdf:Fifteen&version=1.1.0&service=wfs&maxFeatures=5&startIndex=7&outputFormat=JSON");
        assertEquals(5, json.getJSONArray("features").size());
        assertEquals(15, json.getInt("totalFeatures"));
    }

    @Test
    public void testGetWithCountAndStartIndexEnd() throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                "wfs?request=GetFeature&typenames=cdf:Fifteen&version=1.1.0&service=wfs&maxFeatures=5&startIndex=11&outputFormat=JSON");
        assertEquals(4, json.getJSONArray("features").size());
        assertEquals(15, json.getInt("totalFeatures"));
    }

    /** gml:id should not be present on GML 3.1 geometry XML tag */
    @Test
    public void testNoGmlIdOnGeometry() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName=cite:NamedPlaces&version=1.1.0&service=wfs&featureId=NamedPlaces.1107531895891");

        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo(
                "0",
                "count(//wfs:FeatureCollection/gml:featureMembers/cite:NamedPlaces/cite:the_geom"
                        + "/gml:MultiSurface/gml:surfaceMember/gml:Polygon[@gml:id])",
                doc);
    }
}
