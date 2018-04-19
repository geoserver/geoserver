/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.MockTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.CreateStoredQuery;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geotools.wfs.v2_0.WFSConfiguration;
import org.geotools.xml.Parser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.ServletResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement()
                .getNodeName());
        assertEquals("2.0.0", doc.getDocumentElement().getAttribute("version"));
        
        XpathEngine xpath =  XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wfs:FeatureType", doc).getLength() > 0);
        
        // check GET/POST/SOAP are advertised
        assertXpathEvaluatesTo("TRUE", "//ows:OperationsMetadata/ows:Constraint[@name='KVPEncoding']/ows:DefaultValue", doc);
        assertXpathEvaluatesTo("TRUE", "//ows:OperationsMetadata/ows:Constraint[@name='XMLEncoding']/ows:DefaultValue", doc);
        assertXpathEvaluatesTo("TRUE", "//ows:OperationsMetadata/ows:Constraint[@name='SOAPEncoding']/ows:DefaultValue", doc);
    }
    
    @Test
    public void testPost() throws Exception {
        String xml = "<GetCapabilities service=\"WFS\" "
                + " xmlns=\"http://www.opengis.net/wfs/2.0\" "
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + " xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 "
                + " http://schemas.opengis.net/wfs/2.0/wfs.xsd\"/>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement()
                .getNodeName());
        assertEquals("2.0.0", doc.getDocumentElement().getAttribute("version"));
    }
    
    @Test
    public void testNamespaceFilter() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities&namespace=sf");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        XpathEngine xpath =  XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[starts-with(., sf)]", doc).getLength() > 0);
        assertEquals(0, xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[not(starts-with(., sf))]", doc).getLength());
        
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
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement()
                .getNodeName());
        assertEquals("2.0.0", doc.getDocumentElement().getAttribute("version"));
    }
    
    @Test
    public void testOutputFormats() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");
        
        // print(doc);

        // let's look for the outputFormat parameter values inside of the GetFeature operation metadata
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList formats = engine.getMatchingNodes(
                "//ows:Operation[@name=\"GetFeature\"]/ows:Parameter[@name=\"outputFormat\"]/ows:AllowedValues/ows:Value", doc);
        
        Set<String> s1 = new TreeSet<String>();
        for ( int i = 0; i < formats.getLength(); i++ ) {
            String format = formats.item(i).getFirstChild().getNodeValue();
            s1.add( format );
        }
        
        List<WFSGetFeatureOutputFormat> extensions = GeoServerExtensions.extensions( WFSGetFeatureOutputFormat.class );
        
        Set<String> s2 = new TreeSet<String>();
        for ( Iterator e = extensions.iterator(); e.hasNext(); ) {
            WFSGetFeatureOutputFormat extension = (WFSGetFeatureOutputFormat) e.next();
            s2.addAll( extension.getOutputFormats() );
        }
        
        assertEquals( s1, s2 );
    }

    /**
     * Minimum compliance for the resolve parameter
     * @throws Exception
     */
    @Test
    public void testResolveParameter() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

        // print(doc);
        
        String xpathTemplate = "//ows:Operation[@name=\"%s\"]/ows:Parameter[@name=\"resolve\"]/ows:AllowedValues[ows:Value='%s']";
        for (String op : new String[] { "GetFeature", "GetFeatureWithLock", "GetPropertyValue"}) {
            for (String value : new String[] { "none", "local"}) {
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
        NodeList spatialOperators = engine
                .getMatchingNodes(
                        "//fes:Spatial_Capabilities/fes:SpatialOperators/fes:SpatialOperator/@name",
                        doc);

        Set<String> ops = new TreeSet<String>();
        for (int i = 0; i < spatialOperators.getLength(); i++) {
            String format = spatialOperators.item(i).getFirstChild()
                    .getNodeValue();
            ops.add(format);
        }

        List<String> expectedSpatialOperators = getSupportedSpatialOperatorsList(false);
        assertEquals(expectedSpatialOperators.size(), ops.size());
        assertTrue(ops.containsAll(expectedSpatialOperators));
    }

    /**
     * See ISO 19142: Table 1, Table 13, A.1.2
     */
    @Test
    public void testBasicWFSFesConstraints() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=getCapabilities&version=2.0.0");

        String xpathTemplate = "//fes:Constraint[@name='%s']/ows:DefaultValue";
        for (String constraint : new String[]{"ImplementsAdHocQuery", "ImplementsResourceId", 
                "ImplementsMinStandardFilter", "ImplementsStandardFilter", "ImplementsMinSpatialFilter",
                "ImplementsSpatialFilter",
                "ImplementsSorting", "ImplementsMinimumXPath"}) {
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
        for (Iterator<FeatureTypeInfo> it = enabledTypes.iterator(); it.hasNext();) {
            FeatureTypeInfo ft = it.next();
            if (!ft.isEnabled()) {
                it.remove();
            }
        }
        final int enabledCount = enabledTypes.size();

        assertEquals(enabledCount, xpath.getMatchingNodes(
                "/wfs:WFS_Capabilities/wfs:FeatureTypeList/wfs:FeatureType", doc).getLength());
    }

    @Test
    public void testTypeNames() throws Exception {
        // filter on an existing namespace
        Document doc = getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());

        final List<FeatureTypeInfo> enabledTypes = getCatalog().getFeatureTypes();
        for (Iterator<FeatureTypeInfo> it = enabledTypes.iterator(); it.hasNext();) {
            FeatureTypeInfo ft = it.next();
            if (ft.isEnabled()) {
                String prefixedName = ft.getPrefixedName();

                String xpathExpr = "/wfs:WFS_Capabilities/wfs:FeatureTypeList/"
                        + "wfs:FeatureType/wfs:Name[text()=\"" + prefixedName + "\"]";

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
    public void testValidCapabilitiesDocument() throws Exception {
        print(getAsDOM("wfs?service=WFS&version=2.0.0&request=getCapabilities"));
        InputStream in = get("wfs?service=WFS&version=2.0.0&request=getCapabilities");
        Parser p = new Parser(new WFSConfiguration());
        p.setValidating(true);
        p.validate(in);
        
        for (Exception e : (List<Exception>)p.getValidationErrors()) {
            System.out.println(e.getLocalizedMessage());
        }
        assertTrue(p.getValidationErrors().isEmpty());
    }
    
    @Test
    public void testLayerQualified() throws Exception {
     // filter on an existing namespace
        Document doc = getAsDOM("sf/PrimitiveGeoFeature/wfs?service=WFS&version=2.0.0&request=getCapabilities");
        
        Element e = doc.getDocumentElement();
        assertEquals("WFS_Capabilities", e.getLocalName());
        
        XpathEngine xpath =  XMLUnit.newXpathEngine();
        assertEquals(1, xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[starts-with(., sf)]", doc).getLength());
        assertEquals(0, xpath.getMatchingNodes("//wfs:FeatureType/wfs:Name[not(starts-with(., sf))]", doc).getLength());

        //TODO: renable assertions when all operations implemented
        //assertEquals(7, xpath.getMatchingNodes("//ows:Get[contains(@xlink:href,'sf/PrimitiveGeoFeature/wfs')]", doc).getLength());
        //assertEquals(7, xpath.getMatchingNodes("//ows:Post[contains(@xlink:href,'sf/PrimitiveGeoFeature/wfs')]", doc).getLength());
        
        //TODO: test with a non existing workspace
    }
    
    @Test
    public void testSOAP() throws Exception {
        String xml = 
           "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'> " + 
                " <soap:Header/> " + 
                " <soap:Body>" +
                "<GetCapabilities service=\"WFS\" "
                + " xmlns=\"http://www.opengis.net/wfs/2.0\" "
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + " xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 "
                + " http://schemas.opengis.net/wfs/2.0/wfs.xsd\"/>" + 
                " </soap:Body> " + 
            "</soap:Envelope> "; 

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
        assertXpathEvaluatesTo("application/gml+xml; version=3.2", 
                "//ows:Operation[@name='GetPropertyValue']/ows:Parameter[@name='outputFormat']/ows:AllowedValues/ows:Value[1]", dom);
    }

    @Test
    public void testCreateStoredQuery() throws Exception {
        Document dom = getAsDOM("wfs?request=GetCapabilities&version=2.0.0&acceptformats=text/xml");
        assertXpathEvaluatesTo(CreateStoredQuery.DEFAULT_LANGUAGE,
                "//ows:Operation[@name='CreateStoredQuery']/ows:Parameter[@name='language']/ows:AllowedValues/ows:Value[1]", dom);
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
        assertEquals(1, xpath.getMatchingNodes("//wfs:FeatureType[wfs:Name='cgf:MPolygons']/wfs:MetadataURL", doc).getLength());
        assertEquals(1, xpath.getMatchingNodes("//wfs:FeatureType[wfs:Name='cgf:MPolygons']/wfs:MetadataURL[@xlink:href='http://www.geoserver.org']", doc).getLength());
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
        assertEquals(1, xpath.getMatchingNodes("//wfs:FeatureType[wfs:Name='cgf:MPolygons']/wfs:MetadataURL", doc).getLength());
        assertEquals(1, xpath.getMatchingNodes("//wfs:FeatureType[wfs:Name='cgf:MPolygons']/wfs:MetadataURL[@xlink:href='" + proxyBaseUrl + "/metadata?key=value']", doc).getLength());
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
            for (Iterator<FeatureTypeInfo> it = enabledTypes.iterator(); it.hasNext();) {
                FeatureTypeInfo ft = it.next();
                if (ft.enabled()) {
                    String prefixedName = ft.prefixedName();
    
                    String base = "//wfs:FeatureType[wfs:Name =\"" + prefixedName + "\"]";
                    XMLAssert.assertXpathExists(base, doc);
                    // we generate the other SRS only if it's not equal to native
                    boolean wgs84Native = "EPSG:4326".equals(ft.getSRS());
                    if(wgs84Native) {
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

    public void testSections(String sections, int serviceIdentification, int serviceProvider, int
            operationsMetadata, int featureTypeList, int filterCapabilities) throws Exception {
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

            assertXpathEvaluatesTo("TRUE", "//ows:Constraint[@name='ImplementsTransactionalWFS']/ows:DefaultValue", doc);
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

            assertXpathEvaluatesTo("FALSE", "//ows:Constraint[@name='ImplementsTransactionalWFS']/ows:DefaultValue", doc);
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
}
