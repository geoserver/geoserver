package org.geoserver.csw;

import junit.framework.Test;

import org.geoserver.platform.ServiceException;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetRepositoryItemTest extends CSWTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetRepositoryItemTest());
    }
 
    public void testGetMissingId() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetRepositoryItem");
        checkOws10Exception(dom, ServiceException.MISSING_PARAMETER_VALUE, "id");
    }

    public void testGetMissing() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(BASEPATH + "?service=csw&version=2.0.2&request=GetRepositoryItem&id=foo");
        assertEquals(404, response.getErrorCode());        
    }
    
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
