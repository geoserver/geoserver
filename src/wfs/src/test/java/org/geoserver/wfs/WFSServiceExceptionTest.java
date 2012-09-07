/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import junit.framework.Test;
import net.sf.json.JSONObject;

import org.geoserver.wfs.json.JSONType;

import com.mockrunner.mock.web.MockHttpServletResponse;


public class WFSServiceExceptionTest extends WFSTestSupport {
	

    @Override
    protected void oneTimeSetUp() throws Exception {
    	super.oneTimeSetUp();
    	WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
    	getGeoServer().save(wfs);
    }
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new WFSServiceExceptionTest());
    }
	

    public void testJsonException() throws Exception {
    	
        String path="wfs/?service=wfs" +
        		"&version=1.1.0" +
        		"&request=DescribeFeatureType" +
        		"&typeName=foobar" +
        		"&format_options=callback:myMethod";
        
        // JSONP
        MockHttpServletResponse response = getAsServletResponse(path+"&EXCEPTIONS="+JSONType.jsonp);
        
        // MimeType
        assertEquals(JSONType.jsonp, response.getContentType());
        
        // Content
        String content = response.getOutputStreamContent();
        testJson(testJsonP(content));
        
        // JSON
        response = getAsServletResponse(path+"&EXCEPTIONS="+JSONType.json);
        
        // MimeType
        assertEquals(JSONType.json, response.getContentType());
        
        // Content        
        content = response.getOutputStreamContent();
        testJson(content);

    }
    
    /**
     * @param content Matches:
	 * 			myMethod( ... )
     * @return trimmed string
     */
    private static String testJsonP(String content){
        assertTrue(content.startsWith("myMethod("));
        assertTrue(content.endsWith(")"));
        content=content.substring("myMethod(".length(),content.length()-1);
        
        return content;
    }
    
    /**
	 * @param path
	 * @throws Exception
	 * 
	 * Matches:
	 * {"ExceptionReport": {
		"@version": "1.1.0",
		"Exception": {
		"@exceptionCode": "noApplicableCode",
		"@exceptionLocator": "noLocator",
		"ExceptionText": "Could not type name foobar"
		}
		}}
	 */
    private static void testJson(String content){
        
        JSONObject rootObject = JSONObject.fromObject(content);
        
        JSONObject subObject = rootObject.getJSONObject("ExceptionReport");
        assertEquals(subObject.getString("@version"), "1.1.0");
        JSONObject exception = subObject.getJSONObject("Exception");
        assertNotNull(exception);
        assertNotNull(exception.getString("@exceptionCode"));
        assertNotNull(exception.getString("@exceptionLocator"));
        String exceptionText = exception.getString("ExceptionText");
        assertNotNull(exceptionText);
        assertEquals(exceptionText, "Could not find type name foobar");

    }

}
