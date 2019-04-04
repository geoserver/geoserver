/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class GetFeatureTest extends WFSTestSupport {

    public static QName NULL_GEOMETRIES =
            new QName(SystemTestData.CITE_URI, "NullGeometries", SystemTestData.CITE_PREFIX);

    public static QName FIFTEEN_DUPLICATE =
            new QName(SystemTestData.CITE_URI, "Fifteen", SystemTestData.CITE_PREFIX);

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);

        data.addVectorLayer(NULL_GEOMETRIES, Collections.EMPTY_MAP, getClass(), getCatalog());
        data.addVectorLayer(FIFTEEN_DUPLICATE, Collections.EMPTY_MAP, getClass(), getCatalog());
    }

    @Before
    public void resetFifteen() throws IOException {
        revertLayer(MockData.FIFTEEN);
    }

    @Test
    public void testGet() throws Exception {
        testGetFifteenAll("wfs?request=GetFeature&typename=cdf:Fifteen&version=1.0.0&service=wfs");
    }

    @Test
    public void testPostForm() throws Exception {
        String contentType = "application/x-www-form-urlencoded; charset=UTF-8";
        String body = "request=GetFeature&typename=cdf:Fifteen&version=1.0.0&service=wfs";
        MockHttpServletRequest request = createRequest("wfs");
        request.setMethod("POST");
        request.setContent(body.getBytes("UTF-8"));
        // this is normally done by the servlet container, but the mock system won't do it
        request.addParameter("request", "GetFeature");
        request.addParameter("typename", "cdf:Fifteen");
        request.addParameter("version", "1.0.0");
        request.addParameter("service", "wfs");
        request.setContentType(contentType);
        MockHttpServletResponse response = dispatch(request);
        try (ByteArrayInputStream bis =
                new ByteArrayInputStream(response.getContentAsByteArray())) {
            Document doc = dom(bis);
            assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

            NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
            assertEquals(15, featureMembers.getLength());
        }
    }

    @Test
    public void testGetPropertyNameEmpty() throws Exception {
        testGetFifteenAll(
                "wfs?request=GetFeature&typename=cdf:Fifteen&version=1.0.0&service=wfs&propertyname=");
    }

    @Test
    public void testGetFilterEmpty() throws Exception {
        testGetFifteenAll(
                "wfs?request=GetFeature&typename=cdf:Fifteen&version=1.0.0&service=wfs&filter=");
    }

    @Test
    public void testGetPropertyNameStar() throws Exception {
        testGetFifteenAll(
                "wfs?request=GetFeature&typename=cdf:Fifteen&version=1.0.0&service=wfs&propertyname=*");
    }

    private void testGetFifteenAll(String request) throws Exception {
        Document doc = getAsDOM(request);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertEquals(15, featureMembers.getLength());
    }

    // see GEOS-1893
    @Test
    public void testGetMissingParams() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeNameWrongParam=cdf:Fifteen&version=1.0.0&service=wfs");
        // trick: the document specifies a namespace with schema reference, as a result xpath
        // expressions
        // do work only if fully qualified
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "MissingParameterValue", "//ogc:ServiceException/@code", doc);
    }

    @Test
    public void testAlienNamespace() throws Exception {
        // if the namespace is not known, complain with a service exception
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&typename=youdontknowme:Fifteen&version=1.0.0&service=wfs");
        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testGetNullGeometies() throws Exception {
        Document doc;
        doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName="
                                + getLayerId(NULL_GEOMETRIES)
                                + "&version=1.0.0&service=wfs");
        // print(doc);

        XMLAssert.assertXpathEvaluatesTo(
                "1",
                "count(//cite:NullGeometries[@fid=\"NullGeometries.1107531701010\"]/gml:boundedBy)",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "0",
                "count(//cite:NullGeometries[@fid=\"NullGeometries.1107531701011\"]/boundedBy)",
                doc);
    }

    // see GEOS-1287
    @Test
    public void testGetWithFeatureId() throws Exception {

        Document doc;
        doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName=cdf:Fifteen&version=1.0.0&service=wfs&featureid=Fifteen.2");

        // super.print(doc);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//wfs:FeatureCollection/gml:featureMember)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "Fifteen.2", "//wfs:FeatureCollection/gml:featureMember/cdf:Fifteen/@fid", doc);

        doc =
                getAsDOM(
                        "wfs?request=GetFeature&typeName=cite:NamedPlaces&version=1.0.0&service=wfs&featureId=NamedPlaces.1107531895891");

        // super.print(doc);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//wfs:FeatureCollection/gml:featureMember)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "NamedPlaces.1107531895891",
                "//wfs:FeatureCollection/gml:featureMember/cite:NamedPlaces/@fid",
                doc);
    }

    @Test
    public void testPost() throws Exception {

        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cdf:Other\"> "
                        + "<ogc:PropertyName>cdf:string2</ogc:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);

        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertFalse(featureMembers.getLength() == 0);
    }

    @Test
    public void testPostWithFilter() throws Exception {

        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "outputFormat=\"GML2\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" > "
                        + "<wfs:Query typeName=\"cdf:Other\"> "
                        + "<ogc:PropertyName>cdf:string2</ogc:PropertyName> "
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

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertFalse(featureMembers.getLength() == 0);
    }

    @Test
    public void testLax() throws Exception {
        String xml =
                "<GetFeature version='1.1.0' xmlns:gml=\"http://www.opengis.net/gml\">"
                        + " <Query typeName=\""
                        + SystemTestData.BUILDINGS.getLocalPart()
                        + "\">"
                        + "   <PropertyName>ADDRESS</PropertyName>"
                        + "   <Filter>"
                        + "     <PropertyIsEqualTo>"
                        + "       <PropertyName>ADDRESS</PropertyName>"
                        + "       <Literal>123 Main Street</Literal>"
                        + "     </PropertyIsEqualTo>"
                        + "   </Filter>"
                        + " </Query>"
                        + "</GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        // print( doc );
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("cite:Buildings");
        assertEquals(1, featureMembers.getLength());
    }

    @Test
    public void testMixed() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cdf:Other\"> "
                        + "<ogc:PropertyName>cdf:string2</ogc:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs?request=GetFeature", xml);

        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertFalse(featureMembers.getLength() == 0);
    }

    @Test
    public void testLikeMatchCase() throws Exception {
        // first run, without matching case, should match both buildings
        String xml =
                "<GetFeature version='1.1.0' xmlns:gml=\"http://www.opengis.net/gml\">"
                        + " <Query typeName=\""
                        + SystemTestData.BUILDINGS.getLocalPart()
                        + "\">"
                        + "   <PropertyName>ADDRESS</PropertyName>"
                        + "   <Filter>"
                        + "     <PropertyIsLike wildCard=\"*\" singleChar=\".\" escapeChar=\"\\\" matchCase=\"false\">"
                        + "       <PropertyName>ADDRESS</PropertyName>"
                        + "       <Literal>* MAIN STREET</Literal>"
                        + "     </PropertyIsLike>"
                        + "   </Filter>"
                        + " </Query>"
                        + "</GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("cite:Buildings");
        assertEquals(2, featureMembers.getLength());

        // second run, with match case, should match none
        xml =
                "<GetFeature version='1.1.0' xmlns:gml=\"http://www.opengis.net/gml\">"
                        + " <Query typeName=\""
                        + SystemTestData.BUILDINGS.getLocalPart()
                        + "\">"
                        + "   <PropertyName>ADDRESS</PropertyName>"
                        + "   <Filter>"
                        + "     <PropertyIsLike wildCard=\"*\" singleChar=\".\" escapeChar=\"\\\" matchCase=\"true\">"
                        + "       <PropertyName>ADDRESS</PropertyName>"
                        + "       <Literal>* MAIN STREET</Literal>"
                        + "     </PropertyIsLike>"
                        + "   </Filter>"
                        + " </Query>"
                        + "</GetFeature>";
        doc = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        featureMembers = doc.getElementsByTagName("cite:Buildings");
        assertEquals(0, featureMembers.getLength());
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        testGetFifteenAll(
                "cdf/wfs?request=GetFeature&typename=cdf:Fifteen&version=1.0.0&service=wfs");
        testGetFifteenAll("cdf/wfs?request=GetFeature&typename=Fifteen&version=1.0.0&service=wfs");

        Document doc =
                getAsDOM(
                        "sf/wfs?request=GetFeature&typename=cdf:Fifteen&version=1.0.0&service=wfs");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", doc);
    }

    @Test
    public void testLayerQualified() throws Exception {
        testGetFifteenAll(
                "cdf/Fifteen/wfs?request=GetFeature&typename=cdf:Fifteen&version=1.0.0&service=wfs");
        testGetFifteenAll(
                "cdf/Fifteen/wfs?request=GetFeature&typename=Fifteen&version=1.0.0&service=wfs");

        Document doc =
                getAsDOM(
                        "cdf/Fifteen/wfs?request=GetFeature&typename=cdf:Seven&version=1.0.0&service=wfs");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", doc);
    }

    @Test
    public void testMultiLayer() throws Exception {
        Document doc =
                getAsDOM(
                        "/wfs?request=GetFeature&typename="
                                + getLayerId(SystemTestData.BASIC_POLYGONS)
                                + ","
                                + getLayerId(SystemTestData.BRIDGES)
                                + "&version=1.0.0&service=wfs");
        // print(doc);

        XpathEngine engine = XMLUnit.newXpathEngine();
        String schemaLocation = engine.evaluate("wfs:FeatureCollection/@xsi:schemaLocation", doc);
        assertNotNull(schemaLocation);
        String[] parsedLocations = schemaLocation.split("\\s+");
        // System.out.println(Arrays.toString(parsedLocations));
        int i = 0;
        for (; i < parsedLocations.length; i += 2) {
            if (parsedLocations[i].equals("http://www.opengis.net/cite")) {
                assertEquals(
                        "http://localhost:8080/geoserver/wfs?service=WFS&version=1.0.0&request=DescribeFeatureType&typeName=cite%3ABasicPolygons,cite%3ABridges",
                        parsedLocations[i + 1]);
                break;
            }
        }
        if (i >= parsedLocations.length) {
            fail("Could not find the http://www.opengis.net/cite schema location!");
        }
    }

    @Test
    public void testStrictComplianceBBoxValidator() throws Exception {
        GeoServer geoServer = getGeoServer();
        WFSInfo service = geoServer.getService(WFSInfo.class);
        try {
            service.setCiteCompliant(true);
            geoServer.save(service);

            final QName typeName = MockData.FORESTS;
            // used to throw an error since it was not accounting for the bbox epsg code
            String path =
                    "ows?service=WFS&version=1.1.0&request=GetFeature&typeName="
                            + getLayerId(typeName)
                            + "&bbox=1818131,6142575,1818198,6142642,EPSG:3857&srsName=EPSG:4326";
            Document doc = getAsDOM(path);
            print(doc);
            XMLAssert.assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection)", doc);
        } finally {
            service.setCiteCompliant(false);
            geoServer.save(service);
        }
    }

    @Test
    public void testRequestDisabledResource() throws Exception {
        Catalog catalog = getCatalog();
        ResourceInfo fifteen =
                catalog.getResourceByName(getLayerId(MockData.FIFTEEN), ResourceInfo.class);
        fifteen.setEnabled(false);
        catalog.save(fifteen);

        Document doc =
                getAsDOM("wfs?request=GetFeature&typename=cdf:Fifteen&version=1.0.0&service=wfs");
        // print(doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "InvalidParameterValue", "//ogc:ServiceException/@code", doc);
        XMLAssert.assertXpathEvaluatesTo("typeName", "//ogc:ServiceException/@locator", doc);
    }

    /** Test that a request for a resource from a disabled store fails. */
    @Test
    public void testRequestDisabledStore() throws Exception {
        Catalog catalog = getCatalog();
        StoreInfo store = catalog.getStoreByName("cdf", DataStoreInfo.class);
        try {
            store.setEnabled(false);
            catalog.save(store);
            Document doc =
                    getAsDOM(
                            "wfs?service=WFS&version=1.0.0&request=GetFeature&typename=cdf:Fifteen");
            // print(doc);
            XMLAssert.assertXpathEvaluatesTo("1", "count(//ogc:ServiceException)", doc);
            XMLAssert.assertXpathEvaluatesTo(
                    "InvalidParameterValue", "//ogc:ServiceException/@code", doc);
            XMLAssert.assertXpathEvaluatesTo("typeName", "//ogc:ServiceException/@locator", doc);
        } finally {
            store.setEnabled(true);
            catalog.save(store);
        }
    }

    /** Tests CQL filter */
    @Test
    public void testCQLFilter() throws Exception {
        String layer = getLayerId(MockData.FORESTS);

        String request = "wfs?request=GetFeature&typename=" + layer + "&version=1.0.0&service=wfs";
        Document doc = getAsDOM(request);
        print(doc);
        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertTrue(featureMembers.getLength() > 0);

        // Add CQL filter
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(layer);
        info.setCqlFilter("NAME LIKE 'Red%'");
        getCatalog().save(info);

        doc = getAsDOM(request);
        featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertTrue(featureMembers.getLength() == 0);
    }
}
