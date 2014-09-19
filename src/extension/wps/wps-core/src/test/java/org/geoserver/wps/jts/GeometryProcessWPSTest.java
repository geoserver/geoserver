/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.jts;

import static org.custommonkey.xmlunit.XMLAssert.*;

import org.geoserver.wps.WPSTestSupport;
import org.geotools.geojson.geom.GeometryJSON;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;

public class GeometryProcessWPSTest extends WPSTestSupport {

    @Test
	public void testBufferCapabilities() throws Exception {
		// buffer uses an enumerated attribute, make sure it's not blacklisted because of that
		Document d = getAsDOM( "wps?service=wps&request=getcapabilities" );
        // print(d);
        checkValidationErrors(d);
        assertXpathEvaluatesTo("1", "count(//wps:Process/ows:Identifier[text() = 'JTS:buffer'])", d);
	}
	
    @Test
	public void testDescribeBuffer() throws Exception {
		Document d = getAsDOM( root() + "service=wps&request=describeprocess&identifier=JTS:buffer");
		// print(d);
		checkValidationErrors(d);
		
		// check we get the right type declarations for primitives
		assertXpathEvaluatesTo("xs:double", "//Input[ows:Identifier/text()='distance']/LiteralData/ows:DataType/text()", d);
		assertXpathEvaluatesTo("xs:int", "//Input[ows:Identifier/text()='quadrantSegments']/LiteralData/ows:DataType/text()", d);
		
		// check we have the list of possible values for enumerations
		assertXpathEvaluatesTo("3", "count(//Input[ows:Identifier/text()='capStyle']/LiteralData/ows:AllowedValues/ows:Value)", d);
		assertXpathEvaluatesTo("Round", "//Input[ows:Identifier/text()='capStyle']/LiteralData/ows:AllowedValues/ows:Value[1]/text()", d);
		assertXpathEvaluatesTo("Flat", "//Input[ows:Identifier/text()='capStyle']/LiteralData/ows:AllowedValues/ows:Value[2]/text()", d);
		assertXpathEvaluatesTo("Square", "//Input[ows:Identifier/text()='capStyle']/LiteralData/ows:AllowedValues/ows:Value[3]/text()", d);
	}                                        
	
    @Test
	public void testExecuteBuffer() throws Exception {
        String xml =  
            "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' " + 
                "xmlns:ows='http://www.opengis.net/ows/1.1'>" + 
              "<ows:Identifier>JTS:buffer</ows:Identifier>" + 
               "<wps:DataInputs>" + 
                  "<wps:Input>" + 
                      "<ows:Identifier>geom</ows:Identifier>" + 
                      "<wps:Data>" +
                        "<wps:ComplexData mimeType=\"application/wkt\">" +
                          "<![CDATA[POINT(0 0)]]>" +
                        "</wps:ComplexData>" + 
                      "</wps:Data>" +     
                  "</wps:Input>" + 
                  "<wps:Input>" + 
                     "<ows:Identifier>distance</ows:Identifier>" + 
                     "<wps:Data>" + 
                       "<wps:LiteralData>1</wps:LiteralData>" + 
                     "</wps:Data>" + 
                  "</wps:Input>" +
                  "<wps:Input>" + 
                    "<ows:Identifier>capStyle</ows:Identifier>" + 
                    "<wps:Data>" + 
                      "<wps:LiteralData>Round</wps:LiteralData>" + 
                    "</wps:Data>" + 
                 "</wps:Input>" +
             "</wps:DataInputs>" +
             "<wps:ResponseForm>" + 
             "    <wps:RawDataOutput mimeType=\"application/wkt\">" + 
             "        <ows:Identifier>result</ows:Identifier>" + 
             "    </wps:RawDataOutput>" + 
             "  </wps:ResponseForm>" +
             "</wps:Execute>";
          
          MockHttpServletResponse response = postAsServletResponse( "wps", xml );
          // System.out.println(response.getOutputStreamContent());
          assertEquals("application/wkt", response.getContentType());
          Geometry g = new WKTReader().read(response.getOutputStreamContent());
          assertTrue(g instanceof Polygon);

	}

    @Test
	public void testJsonResponse() throws Exception {
        String xml =  
            "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' " + 
                "xmlns:ows='http://www.opengis.net/ows/1.1'>" + 
              "<ows:Identifier>JTS:buffer</ows:Identifier>" + 
               "<wps:DataInputs>" + 
                  "<wps:Input>" + 
                      "<ows:Identifier>geom</ows:Identifier>" + 
                      "<wps:Data>" +
                        "<wps:ComplexData mimeType=\"application/wkt\">" +
                          "<![CDATA[POINT(0 0)]]>" +
                        "</wps:ComplexData>" + 
                      "</wps:Data>" +     
                  "</wps:Input>" + 
                  "<wps:Input>" + 
                     "<ows:Identifier>distance</ows:Identifier>" + 
                     "<wps:Data>" + 
                       "<wps:LiteralData>1</wps:LiteralData>" + 
                     "</wps:Data>" + 
                  "</wps:Input>" +
                  "<wps:Input>" + 
                    "<ows:Identifier>capStyle</ows:Identifier>" + 
                    "<wps:Data>" + 
                      "<wps:LiteralData>Round</wps:LiteralData>" + 
                    "</wps:Data>" + 
                 "</wps:Input>" +
             "</wps:DataInputs>" +
             "<wps:ResponseForm>" + 
             "    <wps:RawDataOutput mimeType=\"application/json\">" + 
             "        <ows:Identifier>result</ows:Identifier>" + 
             "    </wps:RawDataOutput>" + 
             "  </wps:ResponseForm>" +
             "</wps:Execute>";
          
          MockHttpServletResponse response = postAsServletResponse( "wps", xml );
          // System.out.println(response.getOutputStreamContent());
          assertEquals("application/json", response.getContentType());
          Geometry g = new GeometryJSON().read(response.getOutputStreamContent());
          assertTrue(g instanceof Polygon);

	}
}
