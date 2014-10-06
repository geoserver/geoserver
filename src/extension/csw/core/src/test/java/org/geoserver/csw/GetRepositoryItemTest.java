/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import static junit.framework.Assert.*;

import org.geoserver.platform.ServiceException;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetRepositoryItemTest extends CSWSimpleTestSupport {

    @Test 
    public void testGetMissingId() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetRepositoryItem");
        checkOws10Exception(dom, ServiceException.MISSING_PARAMETER_VALUE, "id");
    }

    @Test 
    public void testGetMissing() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(BASEPATH + "?service=csw&version=2.0.2&request=GetRepositoryItem&id=foo");
        assertEquals(404, response.getErrorCode());        
    }
    
    @Test 
    public void testGetSingle() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(BASEPATH + "?service=csw&version=2.0.2&request=GetRepositoryItem&id=urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f");
        String content = response.getOutputStreamContent();
        // System.out.println(content);
        assertEquals(200, response.getStatusCode());
        assertEquals("application/xml", response.getContentType());
        
        String expected = "This is a random comment that will show up only when fetching the repository item";
        assertTrue(content.contains(expected));
    }
    
   
}
