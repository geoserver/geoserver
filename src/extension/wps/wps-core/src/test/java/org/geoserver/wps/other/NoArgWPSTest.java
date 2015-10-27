package org.geoserver.wps.other;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class NoArgWPSTest extends WPSTestSupport  {

	
	
	 @Override
	    protected void registerNamespaces(Map<String, String> namespaces) {
	        namespaces.put("feature", SystemTestData.BUILDINGS.getNamespaceURI());
	    }
	 
	/**
	 * This test runs a no-argument WPS Process and checks the result. 
	 * @throws Exception
	 */
	 @Test 
	public void NoArgumentProcessTest() throws Exception
	{
		 String xml = "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' " + 
	                "xmlns:ows='http://www.opengis.net/ows/1.1'>" + 
	                "<ows:Identifier>gs:NoArgWPS</ows:Identifier>" + 
	                 "<wps:DataInputs>" + 
	                    "</wps:DataInputs>" +
	                   "<wps:ResponseForm>" +  
	                     "<wps:RawDataOutput mimeType=\"text/xml\">" +
	                       "<ows:Identifier>result</ows:Identifier>" +
	                     "</wps:RawDataOutput>" +
	                   "</wps:ResponseForm>" + 
	                 "</wps:Execute>";
		 
		  //Document d = postAsDOM(root(), xml); // allows you to debug exception  
		 InputStream is = post(root(), xml );
		 String s = IOUtils.toString(is);
		 assertEquals(s,"Completed!");
	}
	 
}
