/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.ByteArrayInputStream;

import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;


public class WMSServiceExceptionTest extends WMSTestSupport {

    public void testException111() throws Exception {
        assertResponse111("wms?version=1.1.1&request=getmap&layers=foobar");
    }
    
    public void testException110() throws Exception {
        assertResponse111("wms?version=1.1.0&request=getmap&layers=foobar");
    }
    
    /**
     * Ask for png8 image and error in image, check that the content type of the response png,
     * see http://jira.codehaus.org/browse/GEOS-3018
     * @throws Exception
     */
    public void testPng8InImageFormat111() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=-130,24,-66,50&styles=I_DONT_EXIST"
                + "&layers=states&Format=image/png8&request=GetMap&width=550"
                + "&height=250&srs=EPSG:4326&version=1.1.1&service=WMS&EXCEPTIONS=application/vnd.ogc.se_inimage");
        
        assertEquals("image/png", response.getContentType());
    }
    
    /**
     * Ask for png8 image and error in image, check that the content type of the response png,
     * see http://jira.codehaus.org/browse/GEOS-3018
     * @throws Exception
     */
    public void testPng8InImageFormat130() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?bbox=-130,24,-66,50&styles=I_DONT_EXIST"
                + "&layers=states&Format=image/png8&request=GetMap&width=550"
                + "&height=250&srs=EPSG:4326&version=1.3.0&service=WMS&EXCEPTIONS=application/vnd.ogc.se_inimage");
        
        assertEquals("image/png", response.getContentType());
    }
    
    void assertResponse111(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        String content = response.getOutputStreamContent(); 
        assertTrue(content.contains(
            "<!DOCTYPE ServiceExceptionReport SYSTEM \"http://localhost:8080/geoserver/schemas/wms/1.1.1/WMS_exception_1_1_1.dtd\">"));
        
        assertEquals("application/vnd.ogc.se_xml", response.getContentType());
        Document dom = dom(new ByteArrayInputStream(content.getBytes()));
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        assertEquals("1.1.1", dom.getDocumentElement().getAttribute("version"));
    }
    
    public void testException130() throws Exception {
        assertResponse130("wms?version=1.3.0&request=getmap&layers=foobar");
    }
    
    void assertResponse130(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        String content = response.getOutputStreamContent();
        assertTrue(content.contains(
            "xsi:schemaLocation=\"http://www.opengis.net/ogc http://localhost:8080/geoserver/schemas/wms/1.3.0/exceptions_1_3_0.xsd\""));
        
        assertEquals("text/xml", response.getContentType());
        Document dom = dom(new ByteArrayInputStream(content.getBytes()));
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        assertEquals("1.3.0", dom.getDocumentElement().getAttribute("version"));
    }
}
