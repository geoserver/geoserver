package org.geoserver.wfsv;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import junit.framework.Test;

import org.geoserver.ows.util.ResponseUtils;
import org.w3c.dom.Document;

public class DescribeVersionedFeatureTypeTest extends WFSVTestSupport {

    @Override
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new DescribeVersionedFeatureTypeTest());
    }
    
    public void testValidateInvalidRequest() throws Exception {
        String request = "<DescribeVersionedFeatureType\r\n" + 
                "  version=\"1.0.0\"\r\n" + 
                "  service=\"WFSV\" versioned=\"true\"\r\n" + 
                "  xmlns=\"http://www.opengis.net/wfsv\"\r\n" + 
                "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n" + 
                "  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n" +
                "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n>\r\n" + 
                "    <wfsv:InvalidElement>topp:archsites</wfsv:InvalidElement>\r\n" + 
                "</DescribeVersionedFeatureType>";
        Document dom = postAsDOM(root() + "strict=true", request);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    public void testDescribeArcsitesPost10() throws Exception {
        String request = "<DescribeVersionedFeatureType\r\n" + 
        		"  version=\"1.0.0\"\r\n" + 
        		"  service=\"WFSV\" versioned=\"true\"\r\n" + 
        		"  xmlns=\"http://www.opengis.net/wfsv\"\r\n" + 
        		"  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n" + 
        		"  xmlns:wfsv=\"http://www.opengis.net/wfsv\"\r\n" +
        		"  xmlns:topp=\"http://www.openplans.org/topp\"\r\n>\r\n" + 
        		"    <wfsv:TypeName>topp:archsites</wfsv:TypeName>\r\n" + 
        		"</DescribeVersionedFeatureType>";
        Document dom = postAsDOM(root() + "strict=true", request);
        //print(dom);
        assertXpathEvaluatesTo("1", "count(//xs:schema)", dom);
        assertXpathEvaluatesTo("http://www.opengis.net/wfsv", "/xs:schema/xs:import/@namespace", dom);
        assertXpathEvaluatesTo("wfsv:AbstractVersionedFeatureType", "/xs:schema/xs:complexType/xs:complexContent/xs:extension/@base", dom);
    }
    
    public void testDescribeArcsitesGet10() throws Exception {
        String request = 
            ResponseUtils.appendQueryString(root() , "service=wfsv&version=1.0.0&request=DescribeVersionedFeatureType&typeName=topp:archsites");
        Document dom = getAsDOM(request);
        //print(dom);
        assertXpathEvaluatesTo("1", "count(//xs:schema)", dom);
        assertXpathEvaluatesTo("http://www.opengis.net/wfsv", "/xs:schema/xs:import/@namespace", dom);
        assertXpathEvaluatesTo("wfsv:AbstractVersionedFeatureType", "/xs:schema/xs:complexType/xs:complexContent/xs:extension/@base", dom);
    }
    
    public void testDescribeArcsitesPost11() throws Exception {
        String request = "<DescribeVersionedFeatureType\r\n" + 
                "  version=\"1.1.0\"\r\n" + 
                "  service=\"WFSV\" versioned=\"true\"\r\n" + 
                "  xmlns=\"http://www.opengis.net/wfsv\"\r\n" + 
                "  xmlns:wfs=\"http://www.opengis.net/wfs\"\r\n" + 
                "  xmlns:topp=\"http://www.openplans.org/topp\"\r\n" + 
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
                "  xsi:schemaLocation=\"http://www.opengis.net/wfsv http://localhost:8080/geoserver/schemas/wfs/1.1.0/wfsv.xsd\">\r\n" + 
                "    <wfs:TypeName>topp:archsites</wfs:TypeName>\r\n" + 
                "</DescribeVersionedFeatureType>";
        Document dom = postAsDOM(root(), request);
        //print(dom);
        assertXpathEvaluatesTo("http://www.opengis.net/wfsv", "/xs:schema/xs:import/@namespace", dom);
        assertXpathEvaluatesTo("wfsv:AbstractVersionedFeatureType", "/xs:schema/xs:complexType/xs:complexContent/xs:extension/@base", dom);
    }
    
    public void testDescribeArcsitesGet11() throws Exception {
        String request = ResponseUtils.appendQueryString( root(),"service=wfsv&version=1.1.0&request=DescribeVersionedFeatureType&typeName=topp:archsites" );
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("http://www.opengis.net/wfsv", "/xs:schema/xs:import/@namespace", dom);
        assertXpathEvaluatesTo("wfsv:AbstractVersionedFeatureType", "/xs:schema/xs:complexType/xs:complexContent/xs:extension/@base", dom);
    }
    
    
}
