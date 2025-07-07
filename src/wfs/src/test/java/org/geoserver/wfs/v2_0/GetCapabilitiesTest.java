/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletResponse;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.MockTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.CreateStoredQuery;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geotools.util.GrowableInternationalString;
import org.geotools.wfs.v2_0.WFSConfiguration;
import org.geotools.xsd.Parser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetCapabilitiesTest extends WFS20TestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
    }

    @Before
    public void revert() throws Exception {
        revertLayer(MockData.MPOLYGONS);
    }

    @Test
    public void testGet() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");
        print(doc);

        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
        assertEquals("2.0.0", doc.getDocumentElement().getAttribute("version"));

        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wfs:FeatureType", doc).getLength() > 0);

        // check GET/POST/SOAP are advertised
        assertXpathEvaluatesTo(
                "TRUE", "//ows:OperationsMetadata/ows:Constraint[@name='KVPEncoding']/ows:DefaultValue", doc);
        assertXpathEvaluatesTo(
                "TRUE", "//ows:OperationsMetadata/ows:Constraint[@name='XMLEncoding']/ows:DefaultValue", doc);
        assertXpathEvaluatesTo(
                "TRUE", "//ows:OperationsMetadata/ows:Constraint[@name='SOAPEncoding']/ows:DefaultValue", doc);
    }

    @Test
    public void testPost() throws Exception {
        String xml = "<GetCapabilities service=\"WFS\" "
                + " xmlns=\"http://www.opengis.net/wfs/2.0\" "
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + " xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 "
                + " http://schemas.opengis.net/wfs/2.0/wfs.xsd\"/>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
        assertEquals("2.0.0", doc.getDocumentElement().getAttribute("version"));
    }

    @Test
    public void testNamespaceFilter() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities&namespace=sf");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[starts-with(., sf)]", doc)
                        .getLength()
                > 0);
        assertEquals(
                0,
                xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[not(starts-with(., sf))]", doc)
                        .getLength());

        // try again with a missing one
        doc = getAsDOM("wfs?service=WFS&request=getCapabilities&namespace=NotThere");
        e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        assertEquals(0, xpath.getMatchingNodes("//wfs:FeatureType", doc).getLength());
    }

    @Test
    public void testPostNoSchemaLocation() throws Exception {
        String xml = "<GetCapabilities service=\"WFS\" version='2.0.0' "
                + " xmlns=\"http://www.opengis.net/wfs/2.0\" "
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
        assertEquals("2.0.0", doc.getDocumentElement().getAttribute("version"));
    }

    @Test
    public void testOutputFormats() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

        // print(doc);

        // let's look for the outputFormat parameter values inside of the GetFeature operation
        // metadata
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList formats = engine.getMatchingNodes(
                "//ows:Operation[@name=\"GetFeature\"]/ows:Parameter[@name=\"outputFormat\"]/ows:AllowedValues/ows:Value",
                doc);

        Set<String> s1 = new TreeSet<>();
        for (int i = 0; i < formats.getLength(); i++) {
            String format = formats.item(i).getFirstChild().getNodeValue();
            s1.add(format);
        }

        List<WFSGetFeatureOutputFormat> extensions = GeoServerExtensions.extensions(WFSGetFeatureOutputFormat.class);

        Set<String> s2 = new TreeSet<>();
        for (WFSGetFeatureOutputFormat extension : extensions) {
            s2.addAll(extension.getOutputFormats());
        }

        assertEquals(s1, s2);
    }

    /** Minimum compliance for the resolve parameter */
    @Test
    public void testResolveParameter() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

        // print(doc);

        String xpathTemplate =
                "//ows:Operation[@name=\"%s\"]/ows:Parameter[@name=\"resolve\"]/ows:AllowedValues[ows:Value='%s']";
        for (String op : new String[] {"GetFeature", "GetFeatureWithLock", "GetPropertyValue"}) {
            for (String value : new String[] {"none", "local"}) {
                String xpath = String.format(xpathTemplate, op, value);
                assertXpathExists(xpath, doc);
            }
        }
    }

    @Test
    public void testSupportedSpatialOperators() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

        // let's look for the spatial capabilities, extract all the spatial operators
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList spatialOperators = engine.getMatchingNodes(
                "//fes:Spatial_Capabilities/fes:SpatialOperators/fes:SpatialOperator/@name", doc);

        Set<String> ops = new TreeSet<>();
        for (int i = 0; i < spatialOperators.getLength(); i++) {
            String format = spatialOperators.item(i).getFirstChild().getNodeValue();
            ops.add(format);
        }

        List<String> expectedSpatialOperators = getSupportedSpatialOperatorsList(false);
        assertEquals(expectedSpatialOperators.size(), ops.size());
        assertTrue(ops.containsAll(expectedSpatialOperators));
    }

    /** See ISO 19142: Table 1, Table 13, A.1.2 */
    @Test
    public void testBasicWFSFesConstraints() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

        String xpathTemplate = "//fes:Constraint[@name='%s']/ows:DefaultValue";
        for (String constraint : new String[] {
            "ImplementsAdHocQuery",
            "ImplementsResourceId",
            "ImplementsMinStandardFilter",
            "ImplementsStandardFilter",
            "ImplementsMinSpatialFilter",
            "ImplementsSpatialFilter",
            "ImplementsSorting",
            "ImplementsMinimumXPath"
        }) {
            String xpath = String.format(xpathTemplate, constraint);
            assertXpathEvaluatesTo("TRUE", xpath, doc);
        }
    }

    @Test
    public void testFunctionArgCount() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

        print(doc);

        // let's check the argument count of "abs" function
        assertXpathEvaluatesTo("1", "count(//fes:Function[@name=\"abs\"]/fes:Arguments/fes:Argument)", doc);
    }

    @Test
    public void testTypeNameCount() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());

        XpathEngine xpath = XMLUnit.newXpathEngine();

        final List<FeatureTypeInfo> enabledTypes = getCatalog().getFeatureTypes();
        for (Iterator<FeatureTypeInfo> it = enabledTypes.iterator(); it.hasNext(); ) {
            FeatureTypeInfo ft = it.next();
            if (!ft.isEnabled()) {
                it.remove();
            }
        }
        final int enabledCount = enabledTypes.size();

        assertEquals(
                enabledCount,
                xpath.getMatchingNodes("/wfs:WFS_Capabilities/wfs:FeatureTypeList/wfs:FeatureType", doc)
                        .getLength());
    }

    @Test
    public void testTypeNames() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());

        final List<FeatureTypeInfo> enabledTypes = getCatalog().getFeatureTypes();
        for (FeatureTypeInfo ft : enabledTypes) {
            if (ft.isEnabled()) {
                String prefixedName = ft.prefixedName();

                String xpathExpr = "/wfs:WFS_Capabilities/wfs:FeatureTypeList/"
                        + "wfs:FeatureType/wfs:Name[text()=\""
                        + prefixedName
                        + "\"]";

                XMLAssert.assertXpathExists(xpathExpr, doc);
            }
        }
    }

    @Test
    public void testOperationsMetadata() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        assertEquals("WFS_Capabilities", doc.getDocumentElement().getLocalName());

        XMLAssert.assertXpathExists("//ows:Operation[@name='GetCapabilities']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='DescribeFeatureType']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='GetFeature']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='LockFeature']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='GetFeatureWithLock']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='Transaction']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='ListStoredQueries']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='DescribeStoredQueries']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='CreateStoredQuery']", doc);
        XMLAssert.assertXpathExists("//ows:Operation[@name='DropStoredQuery']", doc);
    }

    @Test
    public void testOperationsMetadataWithDisabledStoredQueryManagement() throws Exception {
        GeoServer geoServer = getGeoServer();
        WFSInfo wfsInfo = geoServer.getService(WFSInfo.class);
        try {
            wfsInfo.setDisableStoredQueriesManagement(true);
            geoServer.save(wfsInfo);
            Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
            assertEquals("WFS_Capabilities", doc.getDocumentElement().getLocalName());

            XMLAssert.assertXpathExists("//ows:Operation[@name='ListStoredQueries']", doc);
            XMLAssert.assertXpathExists("//ows:Operation[@name='DescribeStoredQueries']", doc);
            XMLAssert.assertXpathNotExists("//ows:Operation[@name='CreateStoredQuery']", doc);
            XMLAssert.assertXpathNotExists("//ows:Operation[@name='DropStoredQuery']", doc);
        } finally {
            wfsInfo.setDisableStoredQueriesManagement(false);
            geoServer.save(wfsInfo);
        }
    }

    @Test
    public void testValidCapabilitiesDocument() throws Exception {
        print(getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities"));
        try (InputStream in = get("wfs?service=WFS&version=2.0.0&request=getCapabilities")) {
            Parser p = new Parser(new WFSConfiguration());
            p.setValidating(true);
            p.validate(in);

            for (Exception e : p.getValidationErrors()) {
                LOGGER.info(e.getLocalizedMessage());
            }
            assertTrue(p.getValidationErrors().isEmpty());
        }
    }

    @Test
    public void testLayerQualified() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("sf/PrimitiveGeoFeature/wfs?service=WFS&version=2.0.0&request=getCapabilities");

        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());

        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(
                1,
                xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[starts-with(., sf)]", doc)
                        .getLength());
        assertEquals(
                0,
                xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[not(starts-with(., sf))]", doc)
                        .getLength());

        // TODO: renable assertions when all operations implemented
        // assertEquals(7,
        // xpath.getMatchingNodes("//ows:Get[contains(@xlink:href,'sf/PrimitiveGeoFeature/wfs')]",
        // doc).getLength());
        // assertEquals(7,
        // xpath.getMatchingNodes("//ows:Post[contains(@xlink:href,'sf/PrimitiveGeoFeature/wfs')]",
        // doc).getLength());

        // TODO: test with a non existing workspace
    }

    @Test
    public void testSOAP() throws Exception {
        String xml = "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> "
                + " <soap:Header/> "
                + " <soap:Body>"
                + "<GetCapabilities service=\"WFS\" "
                + " xmlns=\"http://www.opengis.net/wfs/2.0\" "
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + " xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 "
                + " http://schemas.opengis.net/wfs/2.0/wfs.xsd\"/>"
                + " </soap:Body> "
                + "</soap:Envelope> ";

        MockHttpServletResponse resp = postAsServletResponse("wfs", xml, "application/soap+xml");
        assertEquals("application/soap+xml", resp.getContentType());

        Document dom = dom(new ByteArrayInputStream(resp.getContentAsString().getBytes()));

        assertEquals("soap:Envelope", dom.getDocumentElement().getNodeName());
        assertEquals(1, dom.getElementsByTagName("wfs:WFS_Capabilities").getLength());
    }

    @Test
    public void testAcceptVersions11() throws Exception {
        Document dom = getAsDOM("wfs?request=GetCapabilities&acceptversions=1.1.0,1.0.0");
        assertEquals("wfs:WFS_Capabilities", dom.getDocumentElement().getNodeName());
        assertEquals("1.1.0", dom.getDocumentElement().getAttribute("version"));
    }

    @Test
    public void testAcceptVersions11WithVersion() throws Exception {
        Document dom = getAsDOM("wfs?request=GetCapabilities&version=2.0.0&acceptversions=1.1.0,1.0.0");
        assertEquals("wfs:WFS_Capabilities", dom.getDocumentElement().getNodeName());
        assertEquals("1.1.0", dom.getDocumentElement().getAttribute("version"));
    }

    @Test
    public void testAcceptFormats() throws Exception {
        ServletResponse response = getAsServletResponse("wfs?request=GetCapabilities&version=2.0.0");
        assertEquals("application/xml", response.getContentType());

        response = getAsServletResponse("wfs?request=GetCapabilities&version=2.0.0&acceptformats=text/xml");
        assertEquals("text/xml", response.getContentType());
    }

    @Test
    public void testGetPropertyValueFormat() throws Exception {
        Document dom = getAsDOM("wfs?request=GetCapabilities&version=2.0.0&acceptformats=text/xml");
        assertXpathEvaluatesTo(
                "application/gml+xml; version=3.2",
                "//ows:Operation[@name='GetPropertyValue']/ows:Parameter[@name='outputFormat']/ows:AllowedValues/ows:Value[1]",
                dom);
    }

    @Test
    public void testCreateStoredQuery() throws Exception {
        Document dom = getAsDOM("wfs?request=GetCapabilities&version=2.0.0&acceptformats=text/xml");
        assertXpathEvaluatesTo(
                CreateStoredQuery.DEFAULT_LANGUAGE,
                "//ows:Operation[@name='CreateStoredQuery']/ows:Parameter[@name='language']/ows:AllowedValues/ows:Value[1]",
                dom);
    }

    @Test
    public void testMetadataLinks() throws Exception {
        FeatureTypeInfo mpolys = getCatalog().getFeatureTypeByName(getLayerId(MockTestData.MPOLYGONS));
        MetadataLinkInfo ml = getCatalog().getFactory().createMetadataLink();
        ml.setMetadataType("FGDC");
        ml.setType("text/html");
        ml.setContent("http://www.geoserver.org");
        mpolys.getMetadataLinks().add(ml);
        getCatalog().save(mpolys);

        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        // print(doc);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(
                1,
                xpath.getMatchingNodes("//wfs:FeatureType[wfs:Name='cgf:MPolygons']/wfs:MetadataURL", doc)
                        .getLength());
        assertEquals(
                1,
                xpath.getMatchingNodes(
                                "//wfs:FeatureType[wfs:Name='cgf:MPolygons']/wfs:MetadataURL[@xlink:href='http://www.geoserver.org']",
                                doc)
                        .getLength());
    }

    @Test
    public void testMetadataLinksTransormToProxyBaseURL() throws Exception {
        FeatureTypeInfo mpolys = getCatalog().getFeatureTypeByName(getLayerId(MockTestData.MPOLYGONS));
        MetadataLinkInfo ml = getCatalog().getFactory().createMetadataLink();
        ml.setMetadataType("FGDC");
        ml.setType("text/html");
        ml.setContent("/metadata?key=value");
        mpolys.getMetadataLinks().add(ml);
        getCatalog().save(mpolys);

        String proxyBaseUrl = getGeoServer().getGlobal().getSettings().getProxyBaseUrl();
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertEquals(
                1,
                xpath.getMatchingNodes("//wfs:FeatureType[wfs:Name='cgf:MPolygons']/wfs:MetadataURL", doc)
                        .getLength());
        assertEquals(
                1,
                xpath.getMatchingNodes(
                                "//wfs:FeatureType[wfs:Name='cgf:MPolygons']/wfs:MetadataURL[@xlink:href='"
                                        + proxyBaseUrl
                                        + "/metadata?key=value']",
                                doc)
                        .getLength());
    }

    @Test
    public void testOtherCRS() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getSRS().add("4326"); // this one corresponds to the native one, should not be generated
        wfs.getSRS().add("3857");
        wfs.getSRS().add("3003");
        try {
            getGeoServer().save(wfs);

            // perform get caps
            Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
            // for each enabled type, check we added the otherSRS
            final List<FeatureTypeInfo> enabledTypes = getCatalog().getFeatureTypes();
            for (FeatureTypeInfo ft : enabledTypes) {
                if (ft.enabled()) {
                    String prefixedName = ft.prefixedName();

                    String base = "//wfs:FeatureType[wfs:Name =\"" + prefixedName + "\"]";
                    XMLAssert.assertXpathExists(base, doc);
                    // we generate the other SRS only if it's not equal to native
                    boolean wgs84Native = "EPSG:4326".equals(ft.getSRS());
                    if (wgs84Native) {
                        assertXpathEvaluatesTo("2", "count(" + base + "/wfs:OtherCRS)", doc);
                    } else {
                        assertXpathEvaluatesTo("3", "count(" + base + "/wfs:OtherCRS)", doc);
                        XMLAssert.assertXpathExists(base + "[wfs:OtherCRS = 'urn:ogc:def:crs:EPSG::4326']", doc);
                    }
                    XMLAssert.assertXpathExists(base + "[wfs:OtherCRS = 'urn:ogc:def:crs:EPSG::3003']", doc);
                    XMLAssert.assertXpathExists(base + "[wfs:OtherCRS = 'urn:ogc:def:crs:EPSG::3857']", doc);
                }
            }
        } finally {
            wfs.getSRS().clear();
            getGeoServer().save(wfs);
        }
    }

    @Test
    public void testOtherSRSSingleTypeOverride() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.getSRS().add("4326"); // this one corresponds to the native one, should not be generated
        wfs.getSRS().add("3857");
        wfs.getSRS().add("3003");
        String polygonsName = getLayerId(MockData.POLYGONS);
        FeatureTypeInfo polygons = getCatalog().getFeatureTypeByName(polygonsName);
        polygons.getResponseSRS().add("32632");
        polygons.setOverridingServiceSRS(true);
        try {
            getGeoServer().save(wfs);
            getCatalog().save(polygons);

            // check for this layer we have a different list
            Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
            String base = "//wfs:FeatureType[wfs:Name =\"" + polygonsName + "\"]";
            XMLAssert.assertXpathExists(base, doc);
            assertXpathEvaluatesTo("1", "count(" + base + "/wfs:OtherCRS)", doc);
            XMLAssert.assertXpathExists(base + "[wfs:OtherCRS = 'urn:ogc:def:crs:EPSG::32632']", doc);
        } finally {
            wfs.getSRS().clear();
            getGeoServer().save(wfs);
            polygons.setOverridingServiceSRS(false);
            polygons.getResponseSRS().clear();
            getCatalog().save(polygons);
        }
    }

    @Test
    public void testGetSections() throws Exception {
        // do not specify sections
        testSections("", 1, 1, 1, 1, 1);
        // ask explicitly for all
        testSections("All", 1, 1, 1, 1, 1);
        // test sections one by one
        testSections("ServiceIdentification", 1, 0, 0, 0, 0);
        testSections("ServiceProvider", 0, 1, 0, 0, 0);
        testSections("OperationsMetadata", 0, 0, 1, 0, 0);
        testSections("FeatureTypeList", 0, 0, 0, 1, 0);
        testSections("Filter_Capabilities", 0, 0, 0, 0, 1);
        // try a group
        testSections("ServiceIdentification,Filter_Capabilities", 1, 0, 0, 0, 1);
        // mix All in the middle
        testSections("ServiceIdentification,Filter_Capabilities,All", 1, 1, 1, 1, 1);
        // try an invalid section
        Document dom = getAsDOM("wfs?service=WFS&version=2.0.0&request=GetCapabilities&sections=FooBar");
        checkOws11Exception(dom, "2.0.0", "InvalidParameterValue", "sections");
    }

    protected void testSections(
            String sections,
            int serviceIdentification,
            int serviceProvider,
            int operationsMetadata,
            int featureTypeList,
            int filterCapabilities)
            throws Exception {
        Document dom = getAsDOM("wfs?service=WFS&version=2.0.0&request=GetCapabilities&sections=" + sections);
        // print(dom);
        assertXpathEvaluatesTo("" + serviceIdentification, "count(//ows:ServiceIdentification)", dom);
        assertXpathEvaluatesTo("" + serviceProvider, "count(//ows:ServiceProvider)", dom);
        assertXpathEvaluatesTo("" + operationsMetadata, "count(//ows:OperationsMetadata)", dom);
        assertXpathEvaluatesTo("" + featureTypeList, "count(//wfs:FeatureTypeList)", dom);
        assertXpathEvaluatesTo("" + filterCapabilities, "count(//fes:Filter_Capabilities)", dom);
    }

    @Test
    public void testDisableLocking() throws Exception {
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.setServiceLevel(WFSInfo.ServiceLevel.TRANSACTIONAL);
        gs.save(wfs);
        try {
            Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

            assertXpathEvaluatesTo(
                    "TRUE", "//ows:Constraint[@name='ImplementsTransactionalWFS']/ows:DefaultValue", doc);
            assertXpathEvaluatesTo("FALSE", "//ows:Constraint[@name='ImplementsLockingWFS']/ows:DefaultValue", doc);

            // locking support is gone
            XMLAssert.assertXpathExists("//ows:Operation[@name='Transaction']", doc);
            XMLAssert.assertXpathNotExists("//ows:Operation[@name='LockFeature']", doc);
            XMLAssert.assertXpathNotExists("//ows:Operation[@name='GetFeatureWithLock']", doc);
        } finally {
            wfs.setServiceLevel(WFSInfo.ServiceLevel.COMPLETE);
            gs.save(wfs);
        }
    }

    @Test
    public void testDisableTransaction() throws Exception {
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.setServiceLevel(WFSInfo.ServiceLevel.BASIC);
        gs.save(wfs);
        try {
            Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

            assertXpathEvaluatesTo(
                    "FALSE", "//ows:Constraint[@name='ImplementsTransactionalWFS']/ows:DefaultValue", doc);
            assertXpathEvaluatesTo("FALSE", "//ows:Constraint[@name='ImplementsLockingWFS']/ows:DefaultValue", doc);

            // transaction support is gone
            XMLAssert.assertXpathNotExists("//ows:Operation[@name='Transaction']", doc);
            XMLAssert.assertXpathNotExists("//ows:Operation[@name='LockFeature']", doc);
            XMLAssert.assertXpathNotExists("//ows:Operation[@name='GetFeatureWithLock']", doc);
        } finally {
            wfs.setServiceLevel(WFSInfo.ServiceLevel.COMPLETE);
            gs.save(wfs);
        }
    }

    @Test
    public void testInternationalContent() throws Exception {
        GeoServer gs = getGeoServer();
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(MockData.FIFTEEN));
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for fti fifteen");
        title.add(Locale.ITALIAN, "titolo italiano");
        GrowableInternationalString _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for fti fifteen");
        _abstract.add(Locale.ITALIAN, "abstract italiano");
        fti.setInternationalTitle(title);
        fti.setInternationalAbstract(_abstract);
        KeywordInfo keywordInfo = new Keyword("english keyword");
        keywordInfo.setLanguage(Locale.ENGLISH.getLanguage());

        KeywordInfo keywordInfo2 = new Keyword("parola chiave");
        keywordInfo2.setLanguage(Locale.ITALIAN.getLanguage());
        fti.getKeywords().add(keywordInfo);
        fti.getKeywords().add(keywordInfo2);
        catalog.save(fti);
        WFSInfo wfs = gs.getService(WFSInfo.class);
        title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for WFS service");
        title.add(Locale.ITALIAN, "titolo italiano servizio WFS");
        _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for WFS service");
        _abstract.add(Locale.ITALIAN, "abstract italiano servizio WFS");
        wfs.setInternationalTitle(title);
        wfs.setInternationalAbstract(_abstract);
        gs.save(wfs);
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0&acceptLanguages=it");
        String service = "//ows:ServiceIdentification";
        assertXpathEvaluatesTo("titolo italiano servizio WFS", service + "/ows:Title", doc);
        assertXpathEvaluatesTo("abstract italiano servizio WFS", service + "/ows:Abstract", doc);
        String fifteenLayer = "/wfs:WFS_Capabilities/wfs:FeatureTypeList/wfs:FeatureType[wfs:Name='cdf:Fifteen']";
        assertXpathEvaluatesTo("titolo italiano", fifteenLayer + "/wfs:Title", doc);
        assertXpathEvaluatesTo("abstract italiano", fifteenLayer + "/wfs:Abstract", doc);
        assertXpathEvaluatesTo("parola chiave", fifteenLayer + "/ows:Keywords/ows:Keyword", doc);
    }

    @Test
    public void testAcceptLanguagesInvalid() throws Exception {
        GeoServer gs = getGeoServer();
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(MockData.FIFTEEN));
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for fti fifteen");
        title.add(Locale.ITALIAN, "titolo italiano");
        GrowableInternationalString _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for fti fifteen");
        _abstract.add(Locale.ITALIAN, "abstract italiano");
        fti.setInternationalTitle(title);
        fti.setInternationalAbstract(_abstract);
        KeywordInfo keywordInfo = new Keyword("english keyword");
        keywordInfo.setLanguage(Locale.ENGLISH.getLanguage());

        KeywordInfo keywordInfo2 = new Keyword("parola chiave");
        keywordInfo2.setLanguage(Locale.ITALIAN.getLanguage());
        fti.getKeywords().add(keywordInfo);
        fti.getKeywords().add(keywordInfo2);
        catalog.save(fti);
        WFSInfo wfs = gs.getService(WFSInfo.class);
        title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for WFS service");
        title.add(Locale.ITALIAN, "titolo italiano servizio WFS");
        _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for WFS service");
        _abstract.add(Locale.ITALIAN, "abstract italiano servizio WFS");
        wfs.setInternationalTitle(title);
        wfs.setInternationalAbstract(_abstract);
        gs.save(wfs);

        MockHttpServletResponse response =
                getAsServletResponse("wfs?service=WFS&request=getCapabilities&version=2.0.0&acceptLanguages=fre");
        String responseMsg = response.getContentAsString();
        assertTrue(
                responseMsg.contains(
                        "Content has been requested in one of the following languages: fre. But supported languages are: en,it"));
    }

    @Test
    public void testInternationalContentMultipleLanguages() throws Exception {
        // test request with AcceptLanguages with multiple value
        GeoServer gs = getGeoServer();
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(MockData.FIFTEEN));
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for fti fifteen");
        title.add(Locale.ITALIAN, "titolo italiano");
        GrowableInternationalString _abstract = new GrowableInternationalString();

        // not setting italian set french instead
        _abstract.add(Locale.ENGLISH, "a i18n abstract for fti fifteen");
        _abstract.add(Locale.FRENCH, "resumé");
        fti.setInternationalTitle(title);
        fti.setInternationalAbstract(_abstract);
        KeywordInfo keywordInfo = new Keyword("english keyword");
        keywordInfo.setLanguage(Locale.ENGLISH.getLanguage());

        KeywordInfo keywordInfo2 = new Keyword("parola chiave");
        keywordInfo2.setLanguage(Locale.ITALIAN.getLanguage());
        fti.getKeywords().add(keywordInfo);
        fti.getKeywords().add(keywordInfo2);
        catalog.save(fti);
        WFSInfo wfs = gs.getService(WFSInfo.class);
        title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for WFS service");
        title.add(Locale.ITALIAN, "titolo italiano servizio WFS");
        _abstract = new GrowableInternationalString();
        _abstract.add(Locale.ENGLISH, "a i18n abstract for WFS service");
        _abstract.add(Locale.ITALIAN, "abstract italiano servizio WFS");
        wfs.setInternationalTitle(title);
        wfs.setInternationalAbstract(_abstract);
        gs.save(wfs);

        // request for it or fr
        Document doc =
                getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0&acceptLanguages=it,fr", UTF_8.name());
        String service = "//ows:ServiceIdentification";
        assertXpathEvaluatesTo("titolo italiano servizio WFS", service + "/ows:Title", doc);
        assertXpathEvaluatesTo("abstract italiano servizio WFS", service + "/ows:Abstract", doc);
        String fifteenLayer = "/wfs:WFS_Capabilities/wfs:FeatureTypeList/wfs:FeatureType[wfs:Name='cdf:Fifteen']";
        assertXpathEvaluatesTo("titolo italiano", fifteenLayer + "/wfs:Title", doc);

        // it was not specified french should have been selected
        assertXpathEvaluatesTo("resumé", fifteenLayer + "/wfs:Abstract", doc);
        assertXpathEvaluatesTo("parola chiave", fifteenLayer + "/ows:Keywords/ows:Keyword", doc);
    }

    @Test
    public void testAcceptLanguagesParameter() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(MockData.FIFTEEN));
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for fti fifteen");
        title.add(Locale.ITALIAN, "titolo italiano");
        fti.setInternationalTitle(title);
        catalog.save(fti);

        Document dom = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0&acceptLanguages=it");

        assertXpathEvaluatesTo(
                "src/test/resources/geoserver/wfs?Language=it", "//ows:DCP/ows:HTTP/ows:Get/@xlink:href", dom);
    }

    @Test
    public void testNullLocale() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(getLayerId(MockData.FIFTEEN));
        GrowableInternationalString title = new GrowableInternationalString();
        title.add(Locale.ENGLISH, "a i18n title for fti fifteen");
        title.add(null, "null locale");
        fti.setInternationalTitle(title);
        catalog.save(fti);

        Document dom = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

        assertXpathEvaluatesTo("src/test/resources/geoserver/wfs", "//ows:DCP/ows:HTTP/ows:Get/@xlink:href", dom);
    }

    @Test
    public void testInternationalContentContact() throws Exception {
        ContactInfo old = getGeoServer().getSettings().getContact();
        try {
            GrowableInternationalString person = new GrowableInternationalString();
            person.add(Locale.ITALIAN, "I'm an italian person");
            person.add(Locale.ENGLISH, "I'm an english person");
            ContactInfo contactInfo = new ContactInfoImpl();
            contactInfo.setInternationalContactPerson(person);

            GrowableInternationalString org = new GrowableInternationalString();
            org.add(Locale.ITALIAN, "I'm an italian organization");
            org.add(Locale.ENGLISH, "I'm an english organization");
            contactInfo.setInternationalContactOrganization(org);

            GrowableInternationalString email = new GrowableInternationalString();
            email.add(Locale.ITALIAN, "italian@person.it");
            email.add(Locale.ENGLISH, "english@person.com");
            contactInfo.setInternationalContactEmail(email);

            GrowableInternationalString position = new GrowableInternationalString();
            position.add(Locale.ITALIAN, "Cartografo");
            position.add(Locale.ENGLISH, "Cartographer");
            contactInfo.setInternationalContactPosition(position);

            GrowableInternationalString tel = new GrowableInternationalString();
            tel.add(Locale.ITALIAN, "0558077333");
            tel.add(Locale.ENGLISH, "02304566607");
            contactInfo.setInternationalContactVoice(tel);

            GrowableInternationalString fax = new GrowableInternationalString();
            fax.add(Locale.ITALIAN, "0557777333");
            fax.add(Locale.ENGLISH, "0023030948");
            contactInfo.setInternationalContactFacsimile(fax);

            GrowableInternationalString address = new GrowableInternationalString();
            address.add(Locale.ITALIAN, "indirizzo");
            address.add(Locale.ENGLISH, "address");
            contactInfo.setInternationalAddressDeliveryPoint(address);

            GrowableInternationalString country = new GrowableInternationalString();
            country.add(Locale.ITALIAN, "Italia");
            country.add(Locale.ENGLISH, "England");
            contactInfo.setInternationalAddressCountry(country);

            GrowableInternationalString city = new GrowableInternationalString();
            city.add(Locale.ITALIAN, "Roma");
            city.add(Locale.ENGLISH, "London");
            contactInfo.setInternationalAddressCity(city);

            GrowableInternationalString postalCode = new GrowableInternationalString();
            postalCode.add(Locale.ITALIAN, "50021");
            postalCode.add(Locale.ENGLISH, "34234");
            contactInfo.setInternationalAddressPostalCode(postalCode);

            GeoServerInfo global = getGeoServer().getGlobal();
            global.getSettings().setContact(contactInfo);
            getGeoServer().save(global);

            Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0&AcceptLanguages=en");
            String serviceProv = "//ows:ServiceProvider";
            String pers = serviceProv + "/ows:ServiceContact";
            String contactInf = pers + "/ows:ContactInfo";
            String addrInfo = contactInf + "/ows:Address";
            assertXpathEvaluatesTo("I'm an english organization", serviceProv + "/ows:ProviderName", doc);
            assertXpathEvaluatesTo("I'm an english person", pers + "/ows:IndividualName", doc);
            assertXpathEvaluatesTo("Cartographer", pers + "/ows:PositionName", doc);
            assertXpathEvaluatesTo("02304566607", contactInf + "/ows:Phone/ows:Voice", doc);
            assertXpathEvaluatesTo("0023030948", contactInf + "/ows:Phone/ows:Facsimile", doc);
            assertXpathEvaluatesTo("english@person.com", addrInfo + "/ows:ElectronicMailAddress", doc);
            assertXpathEvaluatesTo("address", addrInfo + "/ows:DeliveryPoint", doc);
            assertXpathEvaluatesTo("London", addrInfo + "/ows:City", doc);
            assertXpathEvaluatesTo("England", addrInfo + "/ows:Country", doc);
            assertXpathEvaluatesTo("34234", addrInfo + "/ows:PostalCode", doc);
        } finally {
            GeoServerInfo global = getGeoServer().getGlobal();
            global.getSettings().setContact(old);
            getGeoServer().save(global);
        }
    }

    @Test
    public void testIauFeatureTypes() throws Exception {
        Document doc = getAsDOM("iau/wfs?service=WFS&version=2.0.0&request=getCapabilities");
        print(doc);

        String poiXPath = "//wfs:FeatureTypeList/wfs:FeatureType[wfs:Name = 'iau:MarsPoi']";
        assertXpathExists(poiXPath, doc);
        assertXpathEvaluatesTo("urn:ogc:def:crs:IAU::49900", poiXPath + "/wfs:DefaultCRS ", doc);
    }

    @Test
    public void testCiteCompliant() throws Exception {
        GeoServer gs = getGeoServer();
        WFSInfo wfs = gs.getService(WFSInfo.class);
        wfs.setCiteCompliant(true);
        gs.save(wfs);

        try {
            // version not required for GetCapabilities
            Document dom = getAsDOM("wfs?service=WFS&request=GetCapabilities");
            assertEquals("wfs:WFS_Capabilities", dom.getDocumentElement().getNodeName());

            MockHttpServletResponse response = getAsServletResponse("wfs?request=GetCapabilities&version=2.0.0");
            assertEquals("application/xml", response.getContentType());
            assertEquals(400, response.getStatus());

            // check the returned xml
            dom = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
            Element root = dom.getDocumentElement();
            assertEquals("ows:ExceptionReport", root.getNodeName());
            assertEquals("2.0.0", root.getAttribute("version"));

            // look into exception code and locator
            assertEquals(1, dom.getElementsByTagName("ows:Exception").getLength());
            Element ex = (Element) dom.getElementsByTagName("ows:Exception").item(0);
            assertEquals(ServiceException.MISSING_PARAMETER_VALUE, ex.getAttribute("exceptionCode"));
            assertEquals("service", ex.getAttribute("locator"));
            assertEquals(1, dom.getElementsByTagName("ows:ExceptionText").getLength());
        } finally {
            wfs.setCiteCompliant(false);
            gs.save(wfs);
        }
    }
}
