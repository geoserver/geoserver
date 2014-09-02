/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.wfs.json.JSONType;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;


public class WMSServiceExceptionTest extends WMSTestSupport {

    @Test
    public void testException111() throws Exception {
        assertResponse111("wms?version=1.1.1&request=getmap&layers=foobar");
    }
    
    @Test
    public void testException110() throws Exception {
        assertResponse111("wms?version=1.1.0&request=getmap&layers=foobar");
    }
    
    /**
     * Ask for png8 image and error in image, check that the content type of the response png,
     * see http://jira.codehaus.org/browse/GEOS-3018
     * @throws Exception
     */
    @Test
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
    @Test
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
    
    @Test
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

    @Test
    public void testJsonException130() throws Exception {
        String path = "wms?version=1.3.0&request=getmap&layers=foobar&EXCEPTIONS=" + JSONType.jsonp
                + "&format_options=callback:myMethod";
        JSONType.setJsonpEnabled(true);
        MockHttpServletResponse response = getAsServletResponse(path);
        JSONType.setJsonpEnabled(false);
        String content = response.getOutputStreamContent();
        testJson(testJsonP(content));
        
    }

    /**
     * @param content Matches: myMethod( ... )
     * @return trimmed string
     */
    private static String testJsonP(String content) {
        assertTrue(content.startsWith("myMethod("));
        assertTrue(content.endsWith(")"));
        content = content.substring("myMethod(".length(), content.length() - 1);
        return content;
    }

    /**
     * @param path
     * @throws Exception
     * 
     */
    private static void testJson(String content) {

        JSONObject jsonException = JSONObject.fromObject(content);
        assertEquals(jsonException.getString("version"), "1.3.0");
        JSONArray exceptions = jsonException.getJSONArray("exceptions");
        JSONObject exception = exceptions.getJSONObject(0);
        assertNotNull(exception);
        assertNotNull(exception.getString("code"));
        assertNotNull(exception.getString("locator"));
        String exceptionText = exception.getString("text");
        assertNotNull(exceptionText);
        assertEquals(exceptionText, "Could not find layer foobar");

    }

    /**
     * Test protection against cross-site scripting attack in exception response.
     */
    @Test
    public void testExceptionLocatorEscaped() throws Exception {
        // request contains cross-site scripting attack payload
        String path = "wms?request=%22%3E%3Ca%20xmlns:a=%27http://www.w3.org/1999/xhtml%27%3E%3C"
                + "a:body%20onload=%22alert%28%27xss%27%29%22/%3E%3C/a%3E%3C";
        MockHttpServletResponse response = getAsServletResponse(path);
        String content = response.getOutputStreamContent();
        // sanity
        assertTrue(content.contains("<ServiceExceptionReport "));
        assertTrue(content.contains("</ServiceExceptionReport>"));
        assertTrue(content.contains("<ServiceException "));
        assertTrue(content.contains("</ServiceException>"));
        // test that cross-site scripting attack payload is escaped
        assertFalse(content.contains("<a:body onload=\"alert('xss')\"/>"));
        assertTrue(content.contains("&lt;a:body onload=&quot;alert(&apos;xss&apos;)&quot;/&gt;"));
    }

}
