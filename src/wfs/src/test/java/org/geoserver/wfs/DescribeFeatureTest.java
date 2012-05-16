package org.geoserver.wfs;

import java.util.HashMap;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.data.test.MockData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DescribeFeatureTest extends WFSTestSupport {
    
//    /**
//     * This is a READ ONLY TEST so we can use one time setup
//     */
//    public static Test suite() {
//        return new OneTimeTestSetup(new DescribeFeatureTest());
//    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.disableDataStore(MockData.CITE_PREFIX);
    }

    public void testGet() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&request=DescribeFeatureType&version=1.0.0");
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }
    
    public void testSkipMisconfiguredLayers() throws Exception {
        // make sure AggregateGeoFeature is in the mock data set
        Document doc = getAsDOM("wfs?service=WFS&request=DescribeFeatureType&version=1.0.0");
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("1",
                "count(//xsd:import[contains(@schemaLocation, 'AggregateGeoFeature')])", doc);
        
        // enable skipping of misconfigured layers
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);
        
        // misconfigure a layer
        FeatureTypeInfo ftype =
                getCatalog().getFeatureTypeByName(MockData.AGGREGATEGEOFEATURE.getLocalPart());
        ftype.setNativeName("NOT ACTUALLY THERE");
        getCatalog().save(ftype);
        
        // check the results again
        doc = getAsDOM("wfs?service=WFS&request=DescribeFeatureType&version=1.0.0");
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo("0",
                "count(//xsd:import[contains(@schemaLocation, 'AggregateGeoFeature')])", doc);
    }

    public void testPost() throws Exception {
        String xml = "<wfs:DescribeFeatureType " + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" />";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    public void testPostDummyFeature() throws Exception {

        String xml = "<wfs:DescribeFeatureType " + "service=\"WFS\" "
                + "version=\"1.0.0\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" >"
                + " <wfs:TypeName>cgf:DummyFeature</wfs:TypeName>"
                + "</wfs:DescribeFeatureType>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("ServiceExceptionReport", doc.getDocumentElement()
                .getNodeName());

    }
    
    public void testWithoutExplicitMapping() throws Exception {
        String xml = "<DescribeFeatureType xmlns='http://www.opengis.net/wfs'"+
           " xmlns:gml='http://www.opengis.net/gml'"+
           " xmlns:ogc='http://www.opengis.net/ogc' version='1.0.0' service='WFS'>"+
           " <TypeName>cdf:Locks</TypeName>"+ 
           " </DescribeFeatureType>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        
        assertEquals( 1, doc.getElementsByTagName("xsd:complexType").getLength());
    }
    
    public void testWithoutTypeName() throws Exception {
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&version=1.0.0");
        NodeList nl = doc.getElementsByTagName("xsd:import");
        assertEquals( 3, nl.getLength());
            
        HashMap<String,HashMap<String,String>> imprts = new HashMap();
        for ( int i = 0; i < nl.getLength(); i++ ) {
            Element imprt = (Element) nl.item( i );
            String namespace = imprt.getAttribute("namespace");
            String schemaLocation = imprt.getAttribute( "schemaLocation");
            int query = schemaLocation.indexOf( "?" );
            
            schemaLocation = schemaLocation.substring(query+1);
            String[] sp = schemaLocation.split("&");
            HashMap params = new HashMap();
            for ( int j = 0; j < sp.length; j++ ) {
                String[] sp1 = sp[j].split("=");
                params.put(sp1[0].toLowerCase(),sp1[1].toLowerCase());
            }
            
            imprts.put(namespace,params);
        }
        
        String[] expected = new String[]{
            MockData.SF_URI, MockData.CDF_URI, MockData.CGF_URI
        };
        for ( String namespace : expected ) {
            assertNotNull( imprts.get( namespace ) );
            HashMap params = imprts.get( namespace );
            assertEquals( "wfs", params.get( "service") );
            assertEquals( "1.0.0", params.get( "version") );
            assertEquals( "describefeaturetype", params.get( "request") );
       
            String types = (String) params.get( "typename");
            assertNotNull(types);
                
            Catalog cat =  getCatalog();
            NamespaceInfo ns = cat.getNamespaceByURI(namespace);
            System.out.println(ns.getPrefix());
            // %2c == , (url-encoded, comma is not considered a safe char)
            assertEquals( cat.getFeatureTypesByNamespace(ns).size(), types.split("%2c").length);
        }
        
    }
    
    public void testWorkspaceQualified() throws Exception {
        Document doc = getAsDOM("sf/wfs?request=DescribeFeatureType&version=1.0.0");
        
        NodeList nl = doc.getElementsByTagName("xsd:complexType");
        assertEquals( 3, nl.getLength());
        
        XMLAssert.assertXpathExists("//xsd:complexType[@name = 'AggregateGeoFeatureType']", doc);
        XMLAssert.assertXpathExists("//xsd:complexType[@name = 'PrimitiveGeoFeatureType']", doc);
        XMLAssert.assertXpathExists("//xsd:complexType[@name = 'GenericEntityType']", doc);
        
        doc = getAsDOM("sf/wfs?request=DescribeFeatureType&version=1.0.0&typename=cdf:Fifteen");
        XMLAssert.assertXpathExists("//ogc:ServiceException", doc);
        
    }
}
